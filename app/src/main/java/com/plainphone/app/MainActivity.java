package com.plainphone.app;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends Activity {

    private static final long DEVICE_SEARCH_DEBOUNCE_MS = 180;

    private PackageManager pm;
    private List<ResolveInfo> allApps;

    private List<Object> rows;
    private SearchResultsAdapter adapter;
    private EditText search;

    private Set<String> collapsedSections;

    private String currentQuery = "";

    private TextMatch.Query currentSearch = TextMatch.prepare("");

    private String deviceQuery = "";
    private List<SearchResult> deviceFiles = new ArrayList<>();
    private List<SearchResult> deviceContacts = new ArrayList<>();
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingDeviceSearch;
    private int deviceSearchToken;
    private TextView timeBlockRow;
    private ListView listView;
    private SwipeSwitcher swipeSwitcher;
    private LinearLayout modeToggle;
    private HomeMode homeMode = HomeMode.APPS;
    private FrameLayout artFrame;
    private boolean showingHomeReminder = false;
    private boolean homeUiBuilt = false;
    private FontChoice builtWithFont;

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        recreate();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pm = getPackageManager();

        SharedPreferences onboardingPrefs = getSharedPreferences("plain", Context.MODE_PRIVATE);
        if (!onboardingPrefs.getBoolean("onboarding_complete", false)) {

            Intent intent = new Intent(this, OnboardingActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            return;
        }

        if (!isDefaultHomeApp()) {

            showSetHomeReminder();
            return;
        }

        startLoadingHomeUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (showingHomeReminder && isDefaultHomeApp()) {
            showingHomeReminder = false;
            startLoadingHomeUi();
        } else if (homeUiBuilt) {
            if (Config.getFontChoice(this) != builtWithFont) {

                recreate();
                return;
            }

            listView.setTranslationX(0f);
            listView.setAlpha(1f);
            refreshApps();

            WebSearch.forget();
            applyPixelArtSelection();
            refreshTimeBlockRow();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        searchHandler.removeCallbacksAndMessages(null);
        FileIndex.setListener(null);
    }

    private void refreshTimeBlockRow() {
        List<TimeBlock> active = TimeBlockRules.getActiveBlocks(this);
        if (active.isEmpty()) {
            timeBlockRow.setVisibility(View.GONE);
            return;
        }
        TimeBlock first = active.get(0);
        StringBuilder text = new StringBuilder("Block: ").append(first.name)
                .append(" until ").append(TimeBlockRules.formatEndTime(this, first));
        if (active.size() > 1) {
            text.append(" (+").append(active.size() - 1).append(')');
        }
        timeBlockRow.setText(text.toString());
        timeBlockRow.setVisibility(View.VISIBLE);
    }

    private void applyPixelArtSelection() {

        artFrame.removeAllViews();
        View art = Config.isPhotoArtSelected(this) && Config.getArtPhotoUri(this) != null
                ? new PhotoArtView(this, Uri.parse(Config.getArtPhotoUri(this)),
                        Config.getArtPhotoFocusX(this), Config.getArtPhotoFocusY(this),
                        Config.getArtPhotoZoom(this))
                : new GifArtView(this, Config.getGifScene(this));
        artFrame.addView(art, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        artFrame.setVisibility(Config.isPixelArtEnabled(this) ? View.VISIBLE : View.GONE);
    }

    private void refreshApps() {
        new Thread(() -> {
            List<ResolveInfo> loaded = loadLaunchableApps();
            runOnUiThread(() -> {
                allApps = loaded;
                filter(search.getText().toString());
            });
        }).start();
    }

    private boolean isDefaultHomeApp() {
        Intent homeIntent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        ResolveInfo resolveInfo = pm.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY);
        return resolveInfo != null && getPackageName().equals(resolveInfo.activityInfo.packageName);
    }

    private void showSetHomeReminder() {
        showingHomeReminder = true;
        Typeface georgia = Fonts.current(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(Color.BLACK);
        root.setPadding(48, 48, 48, 48);

        TextView message = new TextView(this);
        message.setTextColor(Color.WHITE);
        message.setTextSize(18);
        message.setTypeface(georgia);
        message.setGravity(Gravity.CENTER);
        message.setText("Plain isn't set as your Home app right now.");
        root.addView(message);

        Button openHomeSettings = new Button(this);
        openHomeSettings.setText("Open Home app settings");
        UiKit.style(this, openHomeSettings);
        openHomeSettings.setOnClickListener(v ->
                startActivity(new Intent(android.provider.Settings.ACTION_HOME_SETTINGS)));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = 32;
        root.addView(openHomeSettings, params);

        setContentView(root, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private void requestNotificationPermissionIfNeeded() {
        if (android.os.Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1001);
        }
    }

    private void startLoadingHomeUi() {
        requestNotificationPermissionIfNeeded();
        setBlackWallpaperOnce();

        showLoadingSpinner();
        new Thread(() -> {
            List<ResolveInfo> loaded = loadLaunchableApps();
            runOnUiThread(() -> buildHomeUi(loaded));
        }).start();
    }

    private void showLoadingSpinner() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(Color.BLACK);

        ProgressBar spinner = new ProgressBar(this);
        spinner.getIndeterminateDrawable().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
        root.addView(spinner);

        TextView label = new TextView(this);
        label.setText("Loading...");
        label.setTextColor(Color.WHITE);
        label.setTypeface(Fonts.current(this));
        label.setTextSize(16);
        label.setGravity(Gravity.CENTER);
        label.setPadding(0, 32, 0, 0);
        root.addView(label, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        setContentView(root, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private void buildHomeUi(List<ResolveInfo> loaded) {
        allApps = loaded;
        rows = new ArrayList<>();
        collapsedSections = Config.getCollapsedSections(this);
        homeMode = Config.getHomeMode(this);
        homeUiBuilt = true;
        builtWithFont = Config.getFontChoice(this);

        Typeface georgia = Fonts.current(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);

        search = new EditText(this);
        search.setHint("Search");
        search.setHintTextColor(Color.GRAY);
        search.setTextColor(Color.WHITE);
        search.setBackgroundColor(Color.BLACK);
        search.setSingleLine(true);
        search.setImeOptions(EditorInfo.IME_ACTION_GO);
        search.setPadding(48, 32, 48, 32);
        search.setTypeface(georgia);
        root.addView(search, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        LinearLayout menuColumn = new LinearLayout(this);
        menuColumn.setOrientation(LinearLayout.VERTICAL);

        TextView screenOffRow = buildRow(georgia, "Screen off");
        screenOffRow.setOnClickListener(v -> AppMonitorService.lockScreen());
        menuColumn.addView(screenOffRow, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView statsRow = buildRow(georgia, "Stats");
        statsRow.setOnClickListener(v -> startActivity(new Intent(this, StatsActivity.class)));
        menuColumn.addView(statsRow, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView settingsRow = buildRow(georgia, "Settings");
        settingsRow.setOnClickListener(v -> startActivity(new Intent(this, SettingsGateActivity.class)));
        menuColumn.addView(settingsRow, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        LinearLayout menuRow = new LinearLayout(this);
        menuRow.setOrientation(LinearLayout.HORIZONTAL);
        menuRow.addView(menuColumn, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        artFrame = new FrameLayout(this);

        artFrame.setForeground(UiKit.frameBorder());
        artFrame.setOnClickListener(v -> startActivity(new Intent(this, ArtViewerActivity.class)));

        LinearLayout.LayoutParams artFrameParams = new LinearLayout.LayoutParams(320, 0);
        artFrameParams.rightMargin = 48;
        menuRow.addView(artFrame, artFrameParams);

        menuColumn.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                menuColumn.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                artFrameParams.height = menuColumn.getHeight();
                artFrame.setLayoutParams(artFrameParams);
            }
        });

        applyPixelArtSelection();

        root.addView(menuRow, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        timeBlockRow = buildRow(georgia, "");
        timeBlockRow.setVisibility(View.GONE);
        timeBlockRow.setOnClickListener(v -> startActivity(new Intent(this, TimeBlocksActivity.class)));
        root.addView(timeBlockRow, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        root.addView(divider());

        modeToggle = buildModeToggle(georgia);
        root.addView(modeToggle, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        listView = new ListView(this);
        listView.setBackgroundColor(Color.BLACK);
        listView.setDivider(null);
        listView.setDividerHeight(0);
        listView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        listView.setSelector(new ColorDrawable(Color.TRANSPARENT));
        listView.setCacheColorHint(Color.BLACK);
        listView.setScrollingCacheEnabled(false);

        swipeSwitcher = new SwipeSwitcher(this);
        swipeSwitcher.addView(listView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        swipeSwitcher.setHandler(new SwipeSwitcher.Handler() {
            @Override
            public boolean canGo(int dir) {
                return sectionAt(dir) != null;
            }

            @Override
            public void switchSection(int dir) {
                HomeMode target = sectionAt(dir);
                if (target == null) return;
                homeMode = target;
                Config.setHomeMode(MainActivity.this, target);
                refreshModeToggle();
                if (search.getText().length() > 0) {
                    search.setText("");
                } else {
                    renderRows();
                }
                listView.setSelectionAfterHeaderView();
            }
        });
        root.addView(swipeSwitcher, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);

        adapter = new SearchResultsAdapter(this, rows, georgia);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                SearchResultsAdapter.Header header = adapter.headerAt(position);
                if (header != null) {
                    toggleSection(header.kind);
                    return;
                }
                SearchResult result = adapter.resultAt(position);
                if (result != null) result.activate();
            }
        });

        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            SearchResult result = adapter.resultAt(position);
            if (result == null) return false;

            if (result.payload instanceof ResolveInfo) {
                showAppOptions((ResolveInfo) result.payload);
                return true;
            }
            if (result.payload instanceof Note) {
                confirmDeleteNote((Note) result.payload);
                return true;
            }
            if (result.payload instanceof FileIndex.Entry) {
                FileIndex.Entry entry = (FileIndex.Entry) result.payload;

                if (entry.directory) return false;
                showFileOptions(entry.name,
                        () -> DeviceSearch.openWithChooser(this, entry.file),
                        () -> DeviceSearch.revealInFileManager(this, entry.file));
                return true;
            }
            if (result.payload instanceof DeviceSearch.MediaFile) {

                DeviceSearch.MediaFile media = (DeviceSearch.MediaFile) result.payload;
                showFileOptions(result.title,
                        () -> DeviceSearch.openWithChooser(this, media.uri, media.mime), null);
                return true;
            }

            return false;
        });

        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        search.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId != EditorInfo.IME_ACTION_GO) return false;
            SearchResult first = firstResult();
            if (first == null) return false;
            first.activate();
            return true;
        });

        FileIndex.setListener(() -> {

            if (!currentQuery.isEmpty()) {
                deviceQuery = "";
                filter(search.getText().toString());
            }
        });

        filter("");
        refreshTimeBlockRow();
    }

    private View divider() {
        View line = new View(this);
        line.setBackgroundColor(Color.DKGRAY);
        line.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 2));
        return line;
    }

    private void filter(String query) {
        currentSearch = TextMatch.prepare(query);
        currentQuery = currentSearch.folded;

        renderRows();
        scheduleDeviceSearch(currentQuery);
    }

    private void renderRows() {
        rows.clear();
        String needle = currentQuery;

        if (needle.isEmpty()) {
            if (homeMode == HomeMode.NOTES) {
                rows.add(newNoteRow());
                rows.addAll(noteBrowseResults());
            } else {
                addGroup(SearchResult.Kind.APP, appResults(currentSearch), false);
            }
        } else {
            addGroup(SearchResult.Kind.APP, appResults(currentSearch), true);
            for (SearchResult.Kind kind : SearchResult.Kind.values()) {
                if (kind == SearchResult.Kind.APP) continue;
                addGroup(kind, resultsFor(kind, needle), true);
            }
        }

        adapter.notifyDataSetChanged();
    }

    private List<SearchResult> resultsFor(SearchResult.Kind kind, String needle) {
        switch (kind) {
            case NOTE: return noteResults(needle);
            case PLAIN: return SearchTargets.plain(this, currentSearch);
            case SYSTEM: return SearchTargets.system(this, currentSearch);
            case WEB: return webResults();
            case FILE: return fileResults(needle);
            case CONTACT: return contactResults(needle);
            default: return new ArrayList<>();
        }
    }

    private void addGroup(SearchResult.Kind kind, List<SearchResult> results, boolean showHeader) {
        if (results.isEmpty()) return;
        Collections.sort(results, new Comparator<SearchResult>() {
            @Override
            public int compare(SearchResult a, SearchResult b) {
                return Integer.compare(a.score, b.score);
            }
        });

        if (!showHeader) {
            rows.addAll(results);
            return;
        }

        boolean collapsed = collapsedSections.contains(kind.name());
        rows.add(new SearchResultsAdapter.Header(kind, collapsed, results.size()));
        if (!collapsed) rows.addAll(results);
    }

    private void toggleSection(SearchResult.Kind kind) {
        if (!collapsedSections.remove(kind.name())) {
            collapsedSections.add(kind.name());
        }
        Config.setCollapsedSections(this, collapsedSections);
        renderRows();
    }

    private List<SearchResult> appResults(TextMatch.Query query) {
        List<String> pinnedOrder = Config.getPinnedPackages(this);
        List<SearchResult> results = new ArrayList<>();

        for (ResolveInfo info : allApps) {
            String label = labelFor(info).toString();
            int score = TextMatch.score(label, query);
            if (score == TextMatch.NO_MATCH) continue;

            int pinnedAt = pinnedOrder.indexOf(info.activityInfo.packageName);
            int rank = query.empty
                    ? (pinnedAt >= 0 ? pinnedAt : pinnedOrder.size())
                    : score;

            results.add(new SearchResult(SearchResult.Kind.APP, label, null, rank,
                    () -> launchApp(info), info));
        }
        return results;
    }

    private List<SearchResult> noteBrowseResults() {
        List<Note> notes = Config.getNotes(this);
        Collections.sort(notes, (a, b) -> Long.compare(b.updatedAt, a.updatedAt));
        List<SearchResult> results = new ArrayList<>();
        for (int i = 0; i < notes.size(); i++) {
            Note note = notes.get(i);
            results.add(new SearchResult(SearchResult.Kind.NOTE, note.title(), note.preview(), i,
                    () -> openNote(note.id), note));
        }
        return results;
    }

    private List<SearchResult> noteResults(String needle) {
        List<SearchResult> results = new ArrayList<>();
        for (Note note : Config.getNotes(this)) {
            int score = TextMatch.score(note.text, currentSearch);
            if (score == TextMatch.NO_MATCH) continue;
            results.add(new SearchResult(SearchResult.Kind.NOTE, note.title(), note.preview(), score,
                    () -> openNote(note.id), note));
        }
        return results;
    }

    private SearchResult newNoteRow() {
        return new SearchResult(SearchResult.Kind.NOTE, "+ New note", null, -1, () -> {
            Note note = Note.create();
            List<Note> all = Config.getNotes(this);
            all.add(note);
            Config.setNotes(this, all);
            openNote(note.id);
        });
    }

    private void openNote(String id) {
        Intent intent = new Intent(this, NoteEditActivity.class);
        intent.putExtra("noteId", id);
        startActivity(intent);
    }

    private void confirmDeleteNote(Note note) {
        Typeface georgia = Fonts.current(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(popupBackground());
        root.setPadding(48, 40, 48, 24);

        TextView title = new TextView(this);
        title.setText("Delete this note?");
        title.setTextColor(Color.WHITE);
        title.setTextSize(18);
        title.setTypeface(georgia);
        root.addView(title);

        TextView body = new TextView(this);
        body.setText(note.title());
        body.setTextColor(Color.GRAY);
        body.setTextSize(14);
        body.setTypeface(georgia);
        body.setPadding(0, 16, 0, 8);
        root.addView(body);

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setView(root)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        root.addView(optionRow(georgia, "Cancel", v -> dialog.dismiss()));
        root.addView(optionRow(georgia, "Delete", v -> {
            dialog.dismiss();
            List<Note> notes = Config.getNotes(this);
            java.util.Iterator<Note> it = notes.iterator();
            while (it.hasNext()) {
                if (it.next().id.equals(note.id)) it.remove();
            }
            Config.setNotes(this, notes);
            filter(search.getText().toString());
        }));

        dialog.show();
        if (dialog.getWindow() != null) {
            android.view.WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
            params.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.85);
            dialog.getWindow().setAttributes(params);
        }
    }

    private LinearLayout buildModeToggle(Typeface georgia) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(48, 28, 48, 20);

        HomeMode[] modes = HomeMode.values();
        for (HomeMode mode : modes) {
            TextView tab = new TextView(this);
            tab.setText(mode.label.toUpperCase());
            tab.setTypeface(georgia);
            tab.setTextSize(13);
            tab.setLetterSpacing(0.15f);
            tab.setPadding(0, 8, 56, 8);
            tab.setOnClickListener(v -> switchMode(mode));
            row.addView(tab, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        }
        refreshTabColors(row);
        return row;
    }

    private void refreshModeToggle() {
        if (modeToggle != null) refreshTabColors(modeToggle);
    }

    private HomeMode sectionAt(int dir) {
        int next = homeMode.ordinal() + dir;
        HomeMode[] modes = HomeMode.values();
        return (next >= 0 && next < modes.length) ? modes[next] : null;
    }

    /** Tab tap: animated slide to the target section. */
    private void switchMode(HomeMode target) {
        if (homeMode == target) return;
        swipeSwitcher.performSwitch(target.ordinal() > homeMode.ordinal() ? 1 : -1);
    }

    private void refreshTabColors(LinearLayout row) {
        HomeMode[] modes = HomeMode.values();
        for (int i = 0; i < row.getChildCount() && i < modes.length; i++) {
            TextView tab = (TextView) row.getChildAt(i);
            tab.setTextColor(modes[i] == homeMode ? Color.WHITE : Color.DKGRAY);
        }
    }

    private List<SearchResult> fileResults(String needle) {
        if (!Config.isFileSearchEnabled(this)) return new ArrayList<>();

        boolean fullAccess = FileIndex.canWalk(this);
        if (!fullAccess && !DeviceSearch.canSearchFiles(this)) {

            SearchResult ask = new SearchResult(SearchResult.Kind.FILE, "Search files on this phone",
                    "Tap to allow access", 0, () -> DeviceSearch.requestFullFileAccess(this));
            return singleRow(ask);
        }

        List<SearchResult> results = needle.equals(deviceQuery)
                ? new ArrayList<>(deviceFiles)
                : new ArrayList<>();

        if (fullAccess) {

            if (results.isEmpty() && FileIndex.isScanning()) {
                results.add(new SearchResult(SearchResult.Kind.FILE, "Indexing files…",
                        "Searching again in a moment will find them", 0, () -> {}));
            }
        } else {

            results.add(new SearchResult(SearchResult.Kind.FILE,
                    "Search folders and all files", "Tap to allow full file access",
                    Integer.MAX_VALUE, () -> DeviceSearch.requestAllFilesAccess(this)));
        }
        return results;
    }

    private List<SearchResult> contactResults(String needle) {
        if (!Config.isContactSearchEnabled(this)) return new ArrayList<>();
        if (!DeviceSearch.canSearchContacts(this)) {
            return singleRow(permissionRow(SearchResult.Kind.CONTACT, "Search your contacts",
                    new String[]{android.Manifest.permission.READ_CONTACTS},
                    DeviceSearch.REQUEST_CONTACTS));
        }
        return needle.equals(deviceQuery) ? deviceContacts : new ArrayList<>();
    }

    private List<SearchResult> webResults() {
        if (!Config.isWebSearchEnabled(this)) return new ArrayList<>();
        return WebSearch.results(this, currentSearch);
    }

    private List<SearchResult> singleRow(SearchResult result) {
        List<SearchResult> only = new ArrayList<>();
        only.add(result);
        return only;
    }

    private SearchResult permissionRow(SearchResult.Kind kind, String title,
                                       String[] permissions, int requestCode) {
        return new SearchResult(kind, title, "Tap to allow access", 0,
                () -> requestPermissions(permissions, requestCode));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != DeviceSearch.REQUEST_FILES && requestCode != DeviceSearch.REQUEST_CONTACTS) {
            return;
        }

        if (grantResults.length == 0) return;

        boolean granted = false;
        for (int result : grantResults) {
            if (result == PackageManager.PERMISSION_GRANTED) granted = true;
        }
        if (!granted) {
            if (requestCode == DeviceSearch.REQUEST_FILES) {
                Config.setFileSearchEnabled(this, false);
            } else {
                Config.setContactSearchEnabled(this, false);
            }
        }

        deviceQuery = "";
        filter(search.getText().toString());
    }

    private void scheduleDeviceSearch(String needle) {
        if (pendingDeviceSearch != null) searchHandler.removeCallbacks(pendingDeviceSearch);
        if (needle.isEmpty() || needle.equals(deviceQuery)) return;

        final int token = ++deviceSearchToken;
        TextMatch.Query query = currentSearch;
        pendingDeviceSearch = () -> new Thread(() -> {
            List<SearchResult> files = DeviceSearch.files(this, query);
            List<SearchResult> contacts = DeviceSearch.contacts(this, query);
            runOnUiThread(() -> {
                if (token != deviceSearchToken) return;
                deviceQuery = needle;
                deviceFiles = files;
                deviceContacts = contacts;
                renderRows();
            });
        }).start();
        searchHandler.postDelayed(pendingDeviceSearch, DEVICE_SEARCH_DEBOUNCE_MS);
    }

    private SearchResult firstResult() {
        for (Object row : rows) {
            if (row instanceof SearchResult) return (SearchResult) row;
        }
        return null;
    }

    private TextView buildRow(Typeface georgia, String label) {
        TextView row = new TextView(this);
        row.setText(label);
        row.setBackground(rowBackground());
        row.setTextColor(Color.WHITE);
        row.setTextSize(20);
        row.setPadding(48, 40, 48, 40);
        row.setGravity(Gravity.START);
        row.setTypeface(georgia);
        return row;
    }

    private Drawable rowBackground() {
        StateListDrawable drawable = new StateListDrawable();
        drawable.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Color.DKGRAY));
        drawable.addState(new int[]{}, new ColorDrawable(Color.BLACK));
        return drawable;
    }

    private void launchApp(ResolveInfo info) {
        String pkg = info.activityInfo.packageName;

        TimeBlock blockingBlock = TimeBlockRules.getBlockingBlock(this, pkg);
        if (blockingBlock != null) {
            Intent gate = new Intent(this, TimeBlockGateActivity.class);
            gate.putExtra("package", pkg);
            gate.putExtra("blockId", blockingBlock.id);
            startActivity(gate);
            return;
        }

        if (Config.getLockedPackages(this).contains(pkg)) {
            Intent gate = new Intent(this, PinGateActivity.class);
            gate.putExtra("package", pkg);
            gate.putExtra("label", labelFor(info).toString());
            startActivity(gate);
            return;
        }

        if (Config.getFlaggedPackages(this).contains(pkg)) {
            Intent gate = new Intent(this, FlaggedGateActivity.class);
            gate.putExtra("package", pkg);
            gate.putExtra("label", labelFor(info).toString());
            startActivity(gate);
            return;
        }

        Intent launchIntent = pm.getLaunchIntentForPackage(pkg);
        if (launchIntent != null) {
            startActivity(launchIntent);
        }
    }

    private void showAppOptions(ResolveInfo info) {
        String pkg = info.activityInfo.packageName;
        CharSequence label = labelFor(info);
        Typeface georgia = Fonts.current(this);

        int appFlags = info.activityInfo.applicationInfo.flags;
        boolean uninstallable = (appFlags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0
                || (appFlags & android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0;

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(popupBackground());
        root.setPadding(0, 8, 0, 8);

        TextView title = new TextView(this);
        title.setText(label);
        title.setTextColor(Color.WHITE);
        title.setTextSize(18);
        title.setTypeface(georgia);
        title.setPadding(48, 32, 48, 24);
        root.addView(title);

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setView(root)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        boolean pinned = Config.getPinnedPackages(this).contains(pkg);
        root.addView(optionRow(georgia, pinned ? "Unpin app" : "Pin app", v -> {
            dialog.dismiss();
            List<String> pinnedPackages = Config.getPinnedPackages(this);
            if (pinned) {
                pinnedPackages.remove(pkg);
            } else {
                pinnedPackages.add(pkg);
            }
            Config.setPinnedPackages(this, pinnedPackages);
            filter(search.getText().toString());
        }));

        root.addView(optionRow(georgia, "App info", v -> {
            dialog.dismiss();
            startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    android.net.Uri.parse("package:" + pkg)));
        }));

        if (uninstallable) {
            root.addView(optionRow(georgia, "Uninstall", v -> {
                dialog.dismiss();
                startActivity(new Intent(Intent.ACTION_DELETE,
                        android.net.Uri.parse("package:" + pkg)));
            }));
        }

        root.addView(optionRow(georgia, "Flag app", v -> {
            dialog.dismiss();
            Set<String> flagged = Config.getFlaggedPackages(this);
            flagged.add(pkg);
            Config.setFlaggedPackages(this, flagged);
            android.widget.Toast.makeText(this, label + " flagged",
                    android.widget.Toast.LENGTH_SHORT).show();
        }));

        dialog.show();
        if (dialog.getWindow() != null) {
            android.view.WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
            params.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.85);
            dialog.getWindow().setAttributes(params);
        }
    }

    private void showFileOptions(String title, Runnable openWith, Runnable revealInFileManager) {
        Typeface georgia = Fonts.current(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(popupBackground());
        root.setPadding(0, 8, 0, 8);

        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextColor(Color.WHITE);
        titleView.setTextSize(18);
        titleView.setTypeface(georgia);
        titleView.setPadding(48, 32, 48, 24);
        root.addView(titleView);

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setView(root)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        root.addView(optionRow(georgia, "Open with…", v -> {
            dialog.dismiss();
            openWith.run();
        }));

        if (revealInFileManager != null) {
            root.addView(optionRow(georgia, "Show in file manager", v -> {
                dialog.dismiss();
                revealInFileManager.run();
            }));
        }

        dialog.show();
        if (dialog.getWindow() != null) {
            android.view.WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
            params.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.85);
            dialog.getWindow().setAttributes(params);
        }
    }

    private TextView optionRow(Typeface georgia, String label, View.OnClickListener listener) {
        TextView row = new TextView(this);
        row.setText(label);
        row.setTextColor(Color.WHITE);
        row.setTextSize(20);
        row.setTypeface(georgia);
        row.setPadding(48, 32, 48, 32);
        row.setGravity(Gravity.START);
        row.setBackground(rowBackground());
        row.setOnClickListener(listener);
        return row;
    }

    private Drawable popupBackground() {
        android.graphics.drawable.GradientDrawable box = new android.graphics.drawable.GradientDrawable();
        box.setColor(Color.BLACK);
        return box;
    }

    private void setBlackWallpaperOnce() {
        SharedPreferences prefs = getSharedPreferences("plain", Context.MODE_PRIVATE);
        if (prefs.getBoolean("wallpaper_set", false)) return;
        try {
            Bitmap black = Bitmap.createBitmap(2, 2, Bitmap.Config.RGB_565);
            black.eraseColor(Color.BLACK);
            WallpaperManager.getInstance(this).setBitmap(black);
            prefs.edit().putBoolean("wallpaper_set", true).apply();
        } catch (Exception ignored) {

        }
    }

    private List<ResolveInfo> loadLaunchableApps() {
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> resolved = pm.queryIntentActivities(intent, 0);

        Set<String> hiddenPackages = Config.getHiddenPackages(this);
        List<ResolveInfo> deduped = new ArrayList<>();
        Set<String> seenPackages = new HashSet<>();
        for (ResolveInfo info : resolved) {
            if (info.activityInfo.packageName.equals(getPackageName())) continue;
            if (hiddenPackages.contains(info.activityInfo.packageName)) continue;
            if (seenPackages.add(info.activityInfo.packageName)) {
                deduped.add(info);
            }
        }

        Collections.sort(deduped, new Comparator<ResolveInfo>() {
            @Override
            public int compare(ResolveInfo a, ResolveInfo b) {
                return labelFor(a).toString().compareToIgnoreCase(labelFor(b).toString());
            }
        });
        return deduped;
    }

    private CharSequence labelFor(ResolveInfo info) {
        return info.activityInfo.applicationInfo.loadLabel(pm);
    }
}

