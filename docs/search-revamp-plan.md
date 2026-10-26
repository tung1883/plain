# Search revamp: detailed implementation plan

Branch: `search-revamp` (already created from `main`; the older, shorter `docs/search-revamp-plan.md` on it is superseded by this plan and should be replaced by it in the first commit).

## Context
Home-screen search (`MainActivity`) matches names and titles, ranks nothing across result groups,
remembers nothing about what the user picks, offers one action per row, and cannot look inside files:
notes and to-dos are matched on their text, but files only by file name. This plan:
1. makes ranking and querying better (operators, learning from picks, a Best match group, highlights),
2. adds more actions (per result type, calculator, quick capture) that go through the app's gates,
3. adds a private content index so text inside files is searchable, including PDFs.

Decisions made with the user:
- **PDF:** read the text layer only; scanned / image-only PDFs are detected and skipped, no OCR.
- **Default content folders:** Documents + Download (user can change; "whole storage" is a toggle).
- **Ranking:** keep the existing groups and add a **Best match** group on top (top 1-3 across groups); the Go key fires the best match.
- Content search is **off until the user enables it** in Search settings (it needs all-files access and uses battery).

## What exists (code facts that shape the design)
- `MainActivity.filter()` (~1223) runs on every keystroke -> `renderRows()` (2665). Non-empty query path
  (2767-2774) is **synchronous on the UI thread**: `addGroup(APP, appResults(...))`, then loops
  `SearchResult.Kind.values()` calling `resultsFor(kind, needle)` (2783). `addGroup` (2799) sorts a group by
  `score` (lower = better) and adds a collapsible header (`collapsedSections`, persisted by Kind name).
  There is no cross-group ranking.
- Only files and contacts are async: `scheduleDeviceSearch` (3465): 180 ms debounce, thread, token to drop
  stale results, results stored in `deviceFiles` / `deviceContacts` / `deviceQuery`, then `renderRows()`.
- Click -> `result.activate()` (958-967); Go/Enter -> `firstResult()` = first `SearchResult` in `rows`
  unless `guarded` (1036-1042); long-press handlers per payload (970-1020); file options dialog
  `showFileOptions` (3674) and app options `showAppOptions` (3610) share one dialog pattern (`optionRow`).
- `SearchResult`: kind, title, subtitle (a `\n` gives extra lines), score, payload, `guarded()`,
  up to two inline buttons via `withAction(label, run)`. `SearchResultsAdapter.resultView` (155) draws it;
  it knows nothing about the query (no highlighting).
- `TextMatch`: `prepare` (64; handles `folder` / `file` qualifier words), `fold` (88: lowercase, strip
  accents, `đ ø ß æ œ ł`, non-alphanumerics to one space), `lexicalScore` 0..5, trigram fuzzy 100+. **No tests.**
- `FileIndex`: in-memory `static volatile List<Entry> entries`, filename only, 150k max, depth 15, skips
  dot-files and `Android/data|obb`, rebuilt by job `search.file-index` (`rebuildNow` 118, `walkAll`),
  every 6 h by `IndexScheduler.Receiver`, on demand by `ensureFresh`. `canWalk` = all-files access.
- `JobQueue`: file-based persistent queue (`Spec(type).label().priority().put()`), `JobService` is a
  foreground service (data-sync type) that dispatches `SearchJobs.isType(job.type)` to `runSearchJob`
  (~190). There is **no charging / battery constraint** anywhere. `allowBackup="false"` already.
- Opening: apps go through `launchApp` (3567: `ForegroundExtras.beforeOpen`, `TimeBlockRules`,
  `PinGateActivity`, `FlaggedGateActivity`). `DeviceSearch.open` / `openFile` / `openWithChooser`,
  `QuickActions`, `SearchTargets.system` start other apps **with no gate**.
- No SQLite, no zip/XML/PDF code, no usage history. `Todos.quickAdd(ctx, text)` exists. ML Kit OCR exists
  (not used here). Tests are JVM-only (JUnit 4, `unitTests.returnDefaultValues = true`, org.json).

## Phase S0: groundwork (no behaviour change)
1. **`TextMatchTest`**: fold (`Nguyễn` -> `nguyen`, `đ`, `ß`, punctuation), `lexicalScore` tiers 0..5, fuzzy
   returns >= 100 and rejects dissimilar text, `folder` / `file` qualifier handling, empty query.
2. **`GatedLaunch`** (new, extracted from `launchApp` 3567-3608): `static boolean gate(Activity, String pkg, CharSequence label)`
   runs the same chain (blocking time block -> PIN gate -> flagged gate) and returns true when it
   showed a gate. `launchApp` becomes: `if (!GatedLaunch.gate(...)) launch`. Add
   `GatedLaunch.forIntent(Activity, Intent)`: resolves the handler package (`resolveActivity`), gates it,
   else starts the intent. Add `GatedLaunch.excludedComponents(Activity, Intent)` = components of gated
   packages (via `queryIntentActivities`) for `Intent.EXTRA_EXCLUDE_COMPONENTS` on chooser intents, so
   "Open with..." cannot hand a file to a blocked / flagged app.
3. Route `DeviceSearch.open`, `openFile`, `openWithChooser` and `QuickActions` intents through it.
   (Known limitation, documented: after a gate is passed the target app's launcher opens, not the file.)
4. **`AsyncSource`** (small helper): debounce + token + background thread + "post to UI if still current",
   copied from `scheduleDeviceSearch`; used by content search only (files / contacts keep their code).

## Phase S1: ranking and querying
- **`QueryParser`** (pure) -> `ParsedQuery`: `text`, `phrases`, `excluded`, `kinds` (`app: note: todo: file:
  web: contact: rec: vault:`), `exts` (`pdf: md: docx:` or `ext:pdf`), `since` (`in:today|week|month`),
  `command` (leading `todo ` / `note `). Unknown `x:` stays text; `-word` only after whitespace; quotes
  group a phrase. `core` = `TextMatch.prepare(text + phrases)`. Wire in `filter()` (store `parsed`,
  keep `currentSearch = parsed.core`); `renderRows` skips Kinds outside `parsed.kinds`; sources call
  `parsed.rejects(foldedText)` (exclusions) and apply ext / date filters (files: `File.lastModified`).
  Tests: `QueryParserTest` (quotes, exclusion, filters, unknown prefixes, empty, command words).
- **`PickHistory`** (pure core, injectable clock) + `PickStore` (JSON file in `filesDir`, atomic write):
  per result key -> `{count, last, perPrefix}`; `boost(key, foldedQuery, now)` = small log of picks for
  that query prefix + recency (half-life 14 days), capped; `adjust(score, boost)` moves lexical scores
  (0..5) by at most 2 and fuzzy (>= 100) by at most 40 so tiers never cross. `ResultKey.of(SearchResult)`
  maps payloads: `ResolveInfo` -> package, `Note` -> id, file entry -> path, recording -> name,
  plain / system target -> title; others have no key (no learning). Record on tap (958-967) and on Go (1038).
  "Learn from my picks" toggle + "Clear history" in Search settings. Tests: `PickHistoryTest`.
- **Best match group**: add `Kind.BEST("Best match")`. In `renderRows` build all groups into a map first,
  apply history, then pick up to 3 results across Kinds (exclude `ACTION`, `WEB`, any `guarded` row, and
  fuzzy results): a result qualifies with lexical tier <= 2 or a history boost; take the best per Kind,
  ordered by adjusted score. Render BEST first and **remove** those rows from their own groups (no
  duplicates). `firstResult()` then naturally returns the best match. Collapse state works through the
  existing `collapsed_sections`.
- **Highlighting**: `Highlight.foldWithMap(text)` (folded string + index map back to the original),
  `Highlight.ranges(text, TextMatch.Query)`; `SearchResultsAdapter` gets `setQuery(...)` and bolds matched
  ranges in title and subtitle (`SpannableString`). Tests: `HighlightTest` (accents, `ß`, Vietnamese, none).

## Phase S2: more actions
- **`ResultActions.options(Activity, SearchResult)`** -> list of (label, Runnable); `showFileOptions` becomes
  a generic `showOptions(title, options)` using the same dialog code. Existing long-press behaviour for
  notes / to-dos / recordings / workspaces (multi-select) is **unchanged**. New menus:
  - file: Open, Open with..., Share (`ACTION_SEND` via `PlainFileProvider`), Copy path, Copy name, Show in file manager;
    folder: Open, Copy path.
  - contact: Call, Message, Copy number (phone lookup via `ContactsContract.CommonDataKinds.Phone`).
  - web row: Open, Copy link. Content hit: Open, Open with..., Share, Copy snippet, Copy path, Show in file manager.
  - app: add "Share app" is out of scope; existing app menu stays.
- **`Calc`** (pure; recursive-descent): `+ - * / ^ %`, parentheses, `pi`, `e`, `sqrt`; unit conversion
  `10 km in mi`, `5 kg to lb`, temperatures (length / mass / volume / temperature tables, no currency).
  Result row in the Actions group: title `= 7`, subtitle the expression, tap copies and toasts; not guarded
  (copy is safe) but never in Best match. Hook: `QuickActions.results`. Tests: `CalcTest`.
- **Quick capture**: when `parsed.command` is `todo` / `note`, show a **guarded** Actions row
  "Add to-do: <text>" (`Todos.quickAdd`) and "Add note: <text>" (confirm the note-creation API in `Notes` /
  `Config` before implementing; if none is public, ship to-do only).
- All file / URL / contact launches use `GatedLaunch`.

## Phase S3: content index (text, markdown, Office, ODF, EPUB)
**Storage** - `ContentDb extends SQLiteOpenHelper` (`filesDir/content_index.db`, FTS4 for portability):
```
docs(id INTEGER PRIMARY KEY, path TEXT UNIQUE, size INTEGER, mtime INTEGER, ext TEXT,
     title TEXT, status TEXT, error TEXT, indexed_at INTEGER)
  status: ok | empty | scanned | locked | failed | toobig
chunk_text(id INTEGER PRIMARY KEY, doc INTEGER, locator TEXT, body TEXT)      -- raw text for snippets
chunks USING fts4(folded, tokenize=unicode61)  -- rowid == chunk_text.id, folded = TextMatch.fold(body)
```
`Fold.forIndex(text)` = `TextMatch.fold` + a space between CJK ideographs / kana so unigrams tokenise;
the query side does the same. Caps: 2 MB of text per file, ~1000-character chunks split at paragraph /
sentence breaks (`Chunker`), total DB size cap (setting, default 150 MB; stop with status `toobig`).
Ranking: BM25 from `matchinfo(chunks, 'pcnalx')` computed in Java (`Bm25`), top ~300 chunks, grouped per
document.

**Extractors** (`TextExtractor` interface: `supports(ext)`, `extract(File, Sink)`; `Sink.chunk(locator, text)`,
`Sink.title()`, `Sink.cancelled()`; zip entries capped at 20 MB each / 100 MB total against zip bombs; XML via
`org.xmlpull.v1.XmlPullParserFactory` so JVM tests can use `kxml2`):
- `PlainTextExtractor`: txt md markdown log csv tsv json xml yaml yml ini conf toml tex rst org srt vtt and
  common code extensions, html/htm (tags stripped). Binary sniff on the first 4 KB; UTF-8 then Latin-1 fallback,
  UTF-16 BOM. Locator: nearest markdown heading (`§ Heading`) or `L<line>` for code / text.
- `OoxmlExtractor`: docx (`word/document.xml`, `w:t` per `w:p`, heading styles become locators),
  xlsx (shared strings + sheet cells, locator = sheet name, cell cap), pptx (`slideN.xml` `a:t`, locator `slide N`).
- `OdfExtractor`: odt / ods / odp (`content.xml` `text:p` / `text:h`).
- `EpubExtractor`: `container.xml` -> OPF spine -> xhtml, locator = chapter title or `ch. N`.
- `RtfExtractor`: strip control words (basic).

**Indexer** - `ContentIndexer.run(Context, Control)`:
1. roots = configured top-level folders under shared storage (default `Documents`, `Download`).
2. walk (same exclusions as `FileIndex`: dot-files, `Android/data|obb`, depth 15, no symlinks) keeping
   supported extensions within per-type size limits (text 20 MB, Office 50 MB, PDF 100 MB).
3. diff against `docs` by (path, size, mtime): delete rows of vanished / out-of-scope files; todo = new, changed,
   or `failed` older than 24 h. Order: small text first, Office next, PDFs last.
4. per file: extract -> `Chunker` -> one transaction (delete old chunks, insert docs / chunk_text / chunks).
   Stop between files when the job is cleared / paused, the policy says stop, or the DB cap is hit.
5. The DB is the resume state; running again continues the diff. Progress: notification text `n / N` (extend `runSearchJob`).
- `IndexPolicy.shouldRun(charging, batteryPct, onlyCharging)` (pure) + `BatteryManager` sticky intent read.
- Triggers: `SearchJobs.startContentIndex` (type `search.content-index`, priority -30, listed in `isType`,
  branch in `runSearchJob`), called after a file-index rebuild, from `IndexScheduler.Receiver`, from the
  Settings "Index now" button, and from `MainActivity` start when the last run is older than 1 h and the
  feature is on. Not run when the policy says no; it simply waits for the next trigger.

**Search** - `ContentSearch.search(Context, ParsedQuery, limitFiles)` (background, via `AsyncSource`, min 2
characters): `FtsQuery.build(parsed)` (folded terms ANDed, last term as prefix `term*`, phrases quoted,
exclusions `-term`; ext / date filters applied on `docs`), score = BM25 rank + filename-match boost
(`TextMatch.score` on the name) + small recency boost. Up to 10 files; one snippet per file (`Snippets`:
window of ~60 characters either side of the first hit found with `Highlight.foldWithMap` on the raw text).
Row: `Kind.CONTENT("Inside files")` (enum placed after FILE), title = file name, subtitle =
`<folder label> · <locator>\n…snippet…` (+ `· N matches`), payload `ContentHit`, tap = open through
`GatedLaunch`, long-press = the file menu from S2. Wire like files: new `deviceContent` list + token, and a
`case CONTENT` in `resultsFor`.

**Settings** (`SearchSettingsActivity`, `Config` keys `content_search_enabled` (false), `content_folders`,
`content_types`, `content_only_charging` (true), `content_max_mb` (150)): toggle "Search inside files"
(asks for all-files access, then starts indexing), "Folders" dialog (checklist of top-level storage folders +
whole-storage toggle), "File types" checklist, "Only while charging", "Max index size", a status row
(indexed N files, skipped: scanned PDFs / locked / failed / too big, last run) and buttons "Index now",
"Clear index". The encrypted vault is **not** indexed. Notes and to-dos stay matched in memory.

## Phase S4: PDF (text layer only)
- Dependency: `implementation("com.tom-roush:pdfbox-android:2.0.27.0")` (Apache-2.0, a few MB, app is
  arm64-only); `PDFBoxResourceLoader.init(app)` once per process on the job thread; add
  `packaging { resources { excludes += ... } }` if duplicate-file errors appear.
- `PdfExtractor implements TextExtractor` behind `PdfTextSource` (interface: `open(File)` -> pages, so rules
  are testable with a fake). Real source: `PDDocument.load(file, MemoryUsageSetting.setupTempFileOnly())`
  (never from a stream), a `PDFTextStripper` subclass overriding `endPage` to emit one chunk per page
  (locator `p. N`) in a **single pass**, cooperative cancel + wall-clock limit checked at page boundaries,
  `setSortByPosition(false)` for speed.
- `PdfPolicy.decide(pageCharCounts)` (pure): a page under ~20 characters is empty; if the document
  averages under ~20 characters per page (or over 80% empty pages) -> status `scanned` and **nothing is
  stored**; mixed documents are indexed from their text pages. `InvalidPasswordException` -> `locked`;
  more than 3000 pages -> index the first 3000; `OutOfMemoryError` / exception / timeout (60 s) -> `failed`
  (retried after 24 h). One PDF at a time; PDFs only run when the policy allows (charging by default).
- Tests: `PdfPolicyTest` (text, scanned, mixed, empty, huge), extractor test with a fake `PdfTextSource`.
  Real PdfBox runs only on the device; a node script `tools/search/make_test_pdfs.js` writes a minimal valid
  text PDF for the manual check.

## Phase S5: polish
- Settings wording, permission / empty states, headers for the new groups, README line.
- Optional if time allows: recent queries under the box when focused and empty (touches the home layout, so
  only if it fits without moving existing home modes), "did you mean" when only Web / Actions rows remain.
- Measure typing latency with the full index and large notes / to-do sets on the Galaxy A22; move any slow
  source behind `AsyncSource`.

## Files
New (all in `app/src/main/java/com/plainphone/app/`): `GatedLaunch`, `AsyncSource`, `QueryParser`, `ParsedQuery`,
`PickHistory`, `PickStore`, `ResultKey`, `Highlight`, `ResultActions`, `Calc`, `ContentDb`, `ContentIndexer`,
`ContentSearch`, `ContentHit`, `FtsQuery`, `Bm25`, `Chunker`, `Snippets`, `Fold`, `IndexPolicy`, `TextExtractor`,
`PlainTextExtractor`, `OoxmlExtractor`, `OdfExtractor`, `EpubExtractor`, `RtfExtractor`, `PdfExtractor`,
`PdfTextSource`, `PdfPolicy`; tests for each pure class under `app/src/test/.../`; `tools/search/make_test_pdfs.js`.
Modified: `MainActivity` (hooks listed above, kept small), `SearchResult` (`BEST`, `CONTENT`), `SearchResultsAdapter`
(highlight, query), `SearchSettingsActivity`, `Config`, `SearchJobs`, `JobService` (`runSearchJob`), `FileIndex`
(expose a snapshot of entries), `IndexScheduler`, `DeviceSearch`, `QuickActions`, `app/build.gradle`
(`pdfbox-android`; test `net.sf.kxml:kxml2:2.3.0`), `README.md`.
Reuse: `TextMatch` (fold / score), `FileIndex` walk exclusions, `JobQueue` / `JobService` / `SearchJobs`,
`PlainFileProvider`, `DeviceSearch` open helpers, `Todos.quickAdd`, the `optionRow` dialog code, `Config`.

## Risks and mitigations
- `MainActivity` is ~3500 lines and other branches change it: hooks only, logic in new classes, one commit per phase.
- Index size and battery: opt-in, folder-limited, size caps, charging gate, one file at a time, resumable.
- PdfBox speed / memory on a mid-range phone: temp-file memory mode, per-file limits, last in the queue; tune on the A22 first.
- FTS4 and `unicode61` on API 26-29: fold ourselves so the tokenizer only splits; fall back to `simple` if `unicode61` is missing.
- `Xml.newPullParser()` is not available in JVM tests: use `XmlPullParserFactory` + `kxml2` test dependency.
- Best match can feel wrong early (no history): require lexical tier <= 2 so it only shows strong matches.
- Gates: file opens cannot show the file after a gate passes (documented); chooser exclusion keeps blocked apps out of "Open with...".

## Verification
- `./gradlew testDebugUnitTest` (use the cached Gradle 8.14.3 + JDK 17; no wrapper in the repo): all new pure-class tests
  plus the existing suite.
- Install on the Galaxy A22 (`adb -s 100.94.65.4:5555 install -r --user 0`). Push a corpus to
  `/sdcard/Documents/search-test/`: `.md`, `.txt`, code file, `.docx`, `.xlsx`, `.pptx`, `.odt`, `.epub`, a text
  PDF, a scanned PDF (photo to PDF), a password-protected PDF, a Vietnamese file, a CJK file, a 30 MB log.
- Per phase checks:
  - S1: `"exact phrase"`, `-exclude`, `pdf:`, `note:` filters; pick a result 3 times and see it move up; Best match shows at
    most 3 strong results, no duplicates, Go key opens the first; matched words bold; clear history works.
  - S2: file / contact / web menus; Share sends the file; `2+2*3` and `10 km in mi` answers copy; `todo buy milk` adds a to-do
    and is never fired by Go; opening a file whose viewer is a flagged app shows the gate; "Open with..." omits blocked apps.
  - S3/S4: enable the feature (permission prompt), "Index now": status counts match the corpus (scanned PDF counted as
    scanned, protected PDF as locked); search words from inside each file type and check snippet, locator and ranking;
    `nguyen` finds `Nguyễn`; CJK substring found; edit a file (re-indexed), delete a file (disappears), force-stop mid-index
    (resumes); unplug with "only while charging" on (does not run); disable permission (feature shows the allow row).
  - Latency: logcat tags `PlainContentIndex` / `PlainSearch` print query timings; typing stays smooth with the full index.
- Per project memory: UI changes are confirmed by the user on the device, not by my own screenshots.

## UI revamp

The search screen and Search settings were restyled afterwards (switches, filter chips, bottom sheets, status card, storage choice incl. SD card and any folder). Content index now runs when charging or battery is 20% or more, and below that only if "Allow when low battery" is on.
