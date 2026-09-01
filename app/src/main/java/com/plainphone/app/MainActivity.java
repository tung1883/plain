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
    private Drawable searchIcon;
    private Drawable clearIcon;

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
    private android.widget.HorizontalScrollView modeToggleScroller;
    private LinearLayout tipRow;
    private TextView tipKicker;
    private TextView tipBody;
    private HomeMode homeMode = HomeMode.APPS;
    private List<HomeMode> modeOrder = new ArrayList<>();
    private TextView draggingTab;
    private float dragLastRawX;

    private StatsPanel statsPanel;
    private boolean statsPanelShown;

    private static final int REQUEST_NOTES_UNLOCK = 4301;
    private static final int REQUEST_PICK_NOTES_FOLDER = 4302;
    private static final int REQUEST_PICK_TODO_FILE = 4303;
    private static final int REQUEST_TODOS_UNLOCK = 4304;
    private static final int REQUEST_APPS_UNLOCK = 4305;
    private static final int REQUEST_SEARCH_UNLOCK = 4306;
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (!homeUiBuilt) return;
        if (requestCode == REQUEST_NOTES_UNLOCK && resultCode == RESULT_OK) {
            filter(search.getText().toString());
        } else if (requestCode == REQUEST_TODOS_UNLOCK && resultCode == RESULT_OK) {
            filter(search.getText().toString());
        } else if (requestCode == REQUEST_APPS_UNLOCK && resultCode == RESULT_OK) {
            filter(search.getText().toString());
        } else if (requestCode == REQUEST_SEARCH_UNLOCK && resultCode == RESULT_OK) {
            filter(search.getText().toString());
        } else if (requestCode == REQUEST_PICK_NOTES_FOLDER && resultCode == RESULT_OK) {
            Notes.saveFolderPick(this, data);
            filter(search.getText().toString());
        } else if (requestCode == REQUEST_PICK_TODO_FILE && resultCode == RESULT_OK) {
            String message = Todos.handleFilePick(this, data);
            if (message != null) {
                android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show();
            }
            filter(search.getText().toString());
        }
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
            statsPanelShown = false;
            refreshApps();

            WebSearch.forget();
            applyPixelArtSelection();
            refreshTimeBlockRow();
            refreshTipRow();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        searchHandler.removeCallbacksAndMessages(null);
        FileIndex.setListener(null);
        if (statsPanel != null) statsPanel.shutdown();
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

        ProgressBar spinner = UiKit.spinner(this);
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
        modeOrder = Config.getHomeModeOrder(this);
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
        search.setCompoundDrawablePadding(24);

        int iconPx = (int) (16 * getResources().getDisplayMetrics().density);
        searchIcon = getResources().getDrawable(R.drawable.ic_search, getTheme());
        clearIcon = MiniIcons.cross(iconPx, Color.WHITE);
        updateSearchAffordance();
        search.setOnTouchListener((v, ev) -> {
            if (ev.getAction() == android.view.MotionEvent.ACTION_UP
                    && search.getText().length() > 0) {
                int hit = clearIcon.getBounds().width() + search.getPaddingRight();
                if (ev.getX() >= search.getWidth() - hit) {
                    search.setText("");
                    return true;
                }
            }
            return false;
        });

        root.addView(search, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        tipRow = buildTipRow(georgia);
        root.addView(tipRow, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        Tips.advance(this);
        refreshTipRow();

        LinearLayout menuColumn = new LinearLayout(this);
        menuColumn.setOrientation(LinearLayout.VERTICAL);

        TextView screenOffRow = buildRow(georgia, "Screen off");
        screenOffRow.setOnClickListener(v -> AppMonitorService.lockScreen());
        menuColumn.addView(screenOffRow, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView lockAllRow = buildRow(georgia, "Lock all");
        lockAllRow.setOnClickListener(v -> lockAll());
        menuColumn.addView(lockAllRow, new LinearLayout.LayoutParams(
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
        modeToggleScroller = new android.widget.HorizontalScrollView(this);
        modeToggleScroller.setHorizontalScrollBarEnabled(false);
        modeToggleScroller.setOverScrollMode(View.OVER_SCROLL_NEVER);
        modeToggleScroller.addView(modeToggle, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(modeToggleScroller, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        listView = new ListView(this);
        listView.setBackgroundColor(Color.BLACK);
        listView.setDivider(null);
        listView.setDividerHeight(0);
        listView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        listView.setSelector(new ColorDrawable(Color.TRANSPARENT));
        listView.setCacheColorHint(Color.BLACK);
        listView.setScrollingCacheEnabled(false);

        statsPanel = new StatsPanel(this);
        statsPanel.view().setVisibility(View.GONE);

        FrameLayout swipeContent = new FrameLayout(this);
        swipeContent.addView(listView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        swipeContent.addView(statsPanel.view(), new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        swipeSwitcher = new SwipeSwitcher(this);
        swipeSwitcher.addView(swipeContent, new FrameLayout.LayoutParams(
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
                showNoteOptions((Note) result.payload);
                return true;
            }
            if (result.payload instanceof Todos.Item) {
                Todos.Item item = (Todos.Item) result.payload;
                showTodoOptions(item.index, item.todo);
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
                updateSearchAffordance();
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
        if (!Lock.SEARCH.gateActive(this)) scheduleDeviceSearch(currentQuery);
    }

    private void updateSearchAffordance() {
        Drawable end = search.getText().length() == 0 ? searchIcon : clearIcon;
        search.setCompoundDrawablesWithIntrinsicBounds(null, null, end, null);
    }

    private void lockAll() {
        Lock.lockAll(this);
        search.setText("");
        filter("");
        android.widget.Toast.makeText(this,
                Config.isPinSet(this) ? "Locked" : "Locked — set an App-lock PIN to take effect",
                android.widget.Toast.LENGTH_SHORT).show();
    }

    private void renderRows() {
        rows.clear();
        String needle = currentQuery;

        if (modeToggleScroller != null) {
            modeToggleScroller.setVisibility(needle.isEmpty() ? View.VISIBLE : View.GONE);
        }
        refreshTipRow();

        boolean showStats = homeMode == HomeMode.STATS && needle.isEmpty();
        if (statsPanel != null) {
            statsPanel.view().setVisibility(showStats ? View.VISIBLE : View.GONE);
            listView.setVisibility(showStats ? View.GONE : View.VISIBLE);
            if (showStats && !statsPanelShown) {
                statsPanelShown = true;
                statsPanel.render();
            } else if (!showStats) {
                statsPanelShown = false;
            }
        }
        if (showStats) {
            adapter.notifyDataSetChanged();
            return;
        }

        if (needle.isEmpty()) {
            if (homeMode == HomeMode.NOTES) {
                if (Lock.NOTES.gateActive(this)) {
                    rows.add(new SearchResult(SearchResult.Kind.NOTE, "Notes are locked",
                            "Tap to unlock", -1, () -> startActivityForResult(
                            Lock.NOTES.pinGate(this), REQUEST_NOTES_UNLOCK)));
                } else {
                    if (Lock.NOTES.isLocked(this)) Lock.NOTES.keepUnlocked(this);
                    renderNoteSettingsGroup();
                    rows.add(newNoteRow());
                    rows.addAll(noteBrowseResults());
                }
            } else if (homeMode == HomeMode.TODOS) {
                renderTodoSection();
            } else if (homeMode == HomeMode.VAULT) {
                renderVaultSection();
            } else if (Lock.APPS.gateActive(this)) {
                rows.add(new SearchResult(SearchResult.Kind.APP, "App list is locked",
                        "Tap to unlock", -1, () -> startActivityForResult(
                        Lock.APPS.pinGate(this), REQUEST_APPS_UNLOCK)));
            } else {
                if (Lock.APPS.isLocked(this)) Lock.APPS.keepUnlocked(this);
                addGroup(SearchResult.Kind.APP, appResults(currentSearch), false);
            }
        } else if (Lock.SEARCH.gateActive(this)) {
            rows.add(new SearchResult(SearchResult.Kind.APP, "Search is locked",
                    "Tap to unlock", -1, () -> startActivityForResult(
                    Lock.SEARCH.pinGate(this), REQUEST_SEARCH_UNLOCK)));
        } else {
            if (Lock.SEARCH.isLocked(this)) Lock.SEARCH.keepUnlocked(this);
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
            case TODO: return todoResults(needle);
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
        List<SearchResult> results = new ArrayList<>();
        if (Lock.APPS.gateActive(this)) return results;

        List<String> pinnedOrder = Config.getPinnedPackages(this);

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
        if (Lock.NOTES.gateActive(this)) return new ArrayList<>();
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

    private void renderNoteSettingsGroup() {
        boolean expanded = Config.isNotesHomeSettingsExpanded(this);
        rows.add(new SearchResult(SearchResult.Kind.NOTE,
                (expanded ? "▾  " : "▸  ") + "Notes settings", null, -1, () -> {
            Config.setNotesHomeSettingsExpanded(this, !expanded);
            filter(search.getText().toString());
        }));
        if (!expanded) return;

        rows.add(new SearchResult(SearchResult.Kind.NOTE,
                "Export folder: " + Notes.exportFolderLabel(this), null, -1,
                () -> Notes.showFolderOptions(this, REQUEST_PICK_NOTES_FOLDER,
                        () -> filter(search.getText().toString()))));
        rows.add(new SearchResult(SearchResult.Kind.NOTE,
                "Locked: " + (Lock.NOTES.isLocked(this) ? "On" : "Off"), null, -1,
                () -> Lock.NOTES.toggleLock(this, () -> filter(search.getText().toString()))));
    }

    private void openNote(String id) {
        if (Lock.NOTES.isLocked(this)) Lock.NOTES.keepUnlocked(this);
        Intent intent = new Intent(this, NoteEditActivity.class);
        intent.putExtra("noteId", id);
        startActivity(intent);
    }

    private void openNoteExport(String id) {
        Intent intent = new Intent(this, NoteEditActivity.class);
        intent.putExtra("noteId", id);
        intent.putExtra("autoExport", true);
        startActivity(intent);
    }

    private void showNoteOptions(Note note) {
        Typeface georgia = Fonts.current(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(popupBackground());
        root.setPadding(0, 8, 0, 8);

        TextView title = new TextView(this);
        title.setText(note.title());
        title.setTextColor(Color.WHITE);
        title.setTextSize(18);
        title.setTypeface(georgia);
        title.setSingleLine(true);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        title.setPadding(48, 32, 48, 24);
        root.addView(title);

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setView(root)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        root.addView(optionRow(georgia, "Export", v -> {
            dialog.dismiss();
            openNoteExport(note.id);
        }));
        root.addView(optionRow(georgia, "Delete", v -> {
            dialog.dismiss();
            Notes.confirmDelete(this, note, () -> filter(search.getText().toString()));
        }));

        dialog.show();
        if (dialog.getWindow() != null) {
            android.view.WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
            params.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.85);
            dialog.getWindow().setAttributes(params);
        }
    }

    private void renderVaultSection() {
        boolean created = VaultFormat.exists(VaultSession.vaultRoot(this));
        boolean unlocked = VaultSession.get().isUnlocked();
        String state = !created ? "Tap to set up"
                : unlocked ? "Unlocked — tap to browse" : "Locked — tap to unlock";
        rows.add(new SearchResult(SearchResult.Kind.PLAIN, "File vault", state, -1,
                () -> startActivity(new Intent(this, VaultActivity.class))));
        if (created && unlocked) {
            rows.add(new SearchResult(SearchResult.Kind.PLAIN, "Lock vault now", null, -1, () -> {
                VaultUnlockService.stop(this);
                VaultSession.get().lock(this);
                filter(search.getText().toString());
            }));
        }
    }

    private void renderTodoSection() {
        if (Lock.TODOS.gateActive(this)) {
            rows.add(new SearchResult(SearchResult.Kind.TODO, "To-do is locked",
                    "Tap to unlock", -1, () -> startActivityForResult(
                    Lock.TODOS.pinGate(this), REQUEST_TODOS_UNLOCK)));
            return;
        }
        if (Lock.TODOS.isLocked(this)) Lock.TODOS.keepUnlocked(this);

        boolean expanded = Config.isTodoHomeSettingsExpanded(this);
        rows.add(new SearchResult(SearchResult.Kind.TODO,
                (expanded ? "▾  " : "▸  ") + "To-do settings", null, -1, () -> {
            Config.setTodoHomeSettingsExpanded(this, !expanded);
            filter(search.getText().toString());
        }));
        if (expanded) {
            rows.add(new SearchResult(SearchResult.Kind.TODO,
                    "Locked: " + (Lock.TODOS.isLocked(this) ? "On" : "Off"), null, -1,
                    () -> Lock.TODOS.toggleLock(this, () -> filter(search.getText().toString()))));
            rows.add(new SearchResult(SearchResult.Kind.TODO,
                    "Show completed: " + (Config.isTodosShowCompleted(this) ? "On" : "Off"), null, -1,
                    () -> {
                        Config.setTodosShowCompleted(this, !Config.isTodosShowCompleted(this));
                        filter(search.getText().toString());
                    }));
            int done = Todos.completedCount(this);
            rows.add(new SearchResult(SearchResult.Kind.TODO,
                    "Archive completed (" + done + ")", null, -1, () -> {
                if (done == 0) {
                    android.widget.Toast.makeText(this, "Nothing to archive",
                            android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }
                confirmArchiveTodos(done);
            }));
            rows.add(new SearchResult(SearchResult.Kind.TODO, "Edit as text", null, -1,
                    () -> startActivity(new Intent(this, TodoListEditActivity.class))));
            rows.add(new SearchResult(SearchResult.Kind.TODO,
                    "Todo file: " + Todos.fileLabel(this), null, -1,
                    () -> Todos.showFileOptions(this, REQUEST_PICK_TODO_FILE,
                            () -> filter(search.getText().toString()))));
            rows.add(new SearchResult(SearchResult.Kind.TODO, "Guide", null, -1,
                    () -> startActivity(new Intent(this, TodoGuideActivity.class))));
        }

        rows.add(new SearchResult(SearchResult.Kind.TODO, "+ New task", null, -1,
                this::promptNewTask));

        List<Todos.Item> items = Todos.sortedForView(Todos.load(this),
                Config.isTodosShowCompleted(this));
        for (int i = 0; i < items.size(); i++) {
            rows.add(todoRow(items.get(i), i));
        }
    }

    private void confirmArchiveTodos(int done) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Archive " + done + " completed task" + (done == 1 ? "" : "s") + "?")
                .setMessage("They move out of the list into the done archive.")
                .setPositiveButton("Archive", (d, w) -> {
                    Todos.archiveCompleted(this);
                    filter(search.getText().toString());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private SearchResult todoRow(Todos.Item item, int rank) {
        Todo todo = item.todo;
        int index = item.index;
        SearchResult result = new SearchResult(SearchResult.Kind.TODO,
                todoTitle(todo), todoSubtitle(todo), rank, () -> {
            Todos.toggleDone(this, index);
            filter(search.getText().toString());
        }, item);
        return result.withStrike(todo.done);
    }

    private String todoTitle(Todo todo) {
        String text = todo.displayText();
        if (todo.priority != 0 && !todo.done) text = "(" + todo.priority + ") " + text;
        return text;
    }

    private String todoSubtitle(Todo todo) {
        List<String> tags = new ArrayList<>();
        for (String p : todo.projects()) tags.add("+" + p);
        for (String c : todo.contexts()) tags.add("@" + c);
        if (!tags.isEmpty()) return android.text.TextUtils.join(" ", tags);
        String due = todo.dueDate();
        return due != null ? "due " + due : null;
    }

    private List<SearchResult> todoResults(String needle) {
        List<SearchResult> results = new ArrayList<>();
        if (Lock.TODOS.gateActive(this)) return results;
        List<Todo> todos = Todos.load(this);
        boolean showCompleted = Config.isTodosShowCompleted(this);
        for (int i = 0; i < todos.size(); i++) {
            Todo todo = todos.get(i);
            if (todo.done && !showCompleted) continue;
            int score = TextMatch.score(todo.description, currentSearch);
            if (score == TextMatch.NO_MATCH) continue;
            results.add(todoRow(new Todos.Item(todo, i), score));
        }
        return results;
    }

    private void promptNewTask() {
        Typeface georgia = Fonts.current(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(popupBackground());
        root.setPadding(0, 32, 0, 8);

        TextView title = new TextView(this);
        title.setText("New task");
        title.setTextColor(Color.WHITE);
        title.setTextSize(18);
        title.setTypeface(georgia);
        title.setPadding(48, 0, 48, 16);
        root.addView(title);

        EditText input = new EditText(this);
        input.setBackground(null);
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(Color.GRAY);
        input.setHint("Buy milk +groceries");
        input.setTypeface(georgia);
        input.setTextSize(18);
        input.setSingleLine(true);
        input.setImeOptions(EditorInfo.IME_ACTION_DONE);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setPadding(48, 8, 48, 24);
        root.addView(input);

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setView(root)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        Runnable add = () -> {
            String text = input.getText().toString().trim();
            if (text.isEmpty()) {
                dialog.dismiss();
                return;
            }
            Todos.quickAdd(this, text);
            dialog.dismiss();
            filter(search.getText().toString());
        };

        input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId != EditorInfo.IME_ACTION_DONE) return false;
            add.run();
            return true;
        });

        root.addView(optionRow(georgia, "Add", v -> add.run()));
        root.addView(optionRow(georgia, "Cancel", v -> dialog.dismiss()));

        dialog.show();
        if (dialog.getWindow() != null) {
            android.view.WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
            params.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.85);
            dialog.getWindow().setAttributes(params);
        }
    }

    private void showTodoOptions(int index, Todo todo) {
        Typeface georgia = Fonts.current(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(popupBackground());
        root.setPadding(0, 8, 0, 8);

        TextView title = new TextView(this);
        title.setText(todo.displayText());
        title.setTextColor(Color.WHITE);
        title.setTextSize(18);
        title.setTypeface(georgia);
        title.setSingleLine(true);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        title.setPadding(48, 32, 48, 24);
        root.addView(title);

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setView(root)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        root.addView(optionRow(georgia, "Edit as text", v -> {
            dialog.dismiss();
            startActivity(new Intent(this, TodoListEditActivity.class));
        }));
        root.addView(optionRow(georgia, "Priority: " + (todo.priority == 0 ? "none" : todo.priority)
                + " → " + nextPriorityLabel(todo.priority), v -> {
            dialog.dismiss();
            Todos.setPriority(this, index, nextPriority(todo.priority));
            filter(search.getText().toString());
        }));
        root.addView(optionRow(georgia, "Delete", v -> {
            dialog.dismiss();
            Todos.delete(this, index);
            filter(search.getText().toString());
        }));
        if (Todos.completedCount(this) > 0) {
            root.addView(optionRow(georgia, "Archive completed", v -> {
                dialog.dismiss();
                Todos.archiveCompleted(this);
                filter(search.getText().toString());
            }));
        }

        dialog.show();
        if (dialog.getWindow() != null) {
            android.view.WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
            params.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.85);
            dialog.getWindow().setAttributes(params);
        }
    }

    private static char nextPriority(char current) {
        switch (current) {
            case 0: return 'A';
            case 'A': return 'B';
            case 'B': return 'C';
            default: return 0;
        }
    }

    private static String nextPriorityLabel(char current) {
        char next = nextPriority(current);
        return next == 0 ? "none" : String.valueOf(next);
    }

    private LinearLayout buildModeToggle(Typeface georgia) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(48, 28, 48, 20);

        for (HomeMode mode : modeOrder) {
            TextView tab = new TextView(this);
            tab.setText(mode.label.toUpperCase());
            tab.setTag(mode);
            tab.setTypeface(georgia);
            tab.setTextSize(13);
            tab.setLetterSpacing(0.15f);
            tab.setPadding(0, 8, 56, 8);
            tab.setOnClickListener(v -> {
                if (draggingTab == null) switchMode((HomeMode) v.getTag());
            });
            tab.setOnLongClickListener(v -> {
                draggingTab = (TextView) v;
                v.setAlpha(0.55f);
                v.setTranslationZ(12f);
                ((TextView) v).setTextColor(Color.WHITE);
                return true;
            });
            tab.setOnTouchListener(this::onTabTouch);
            row.addView(tab, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        }
        refreshTabColors(row);
        return row;
    }

    private boolean onTabTouch(View v, android.view.MotionEvent e) {
        switch (e.getActionMasked()) {
            case android.view.MotionEvent.ACTION_DOWN:
                dragLastRawX = e.getRawX();
                return false;   // let long-press detection run
            case android.view.MotionEvent.ACTION_MOVE:
                if (draggingTab != v) return false;
                float dx = e.getRawX() - dragLastRawX;
                dragLastRawX = e.getRawX();
                v.setTranslationX(v.getTranslationX() + dx);
                maybeReorderTab((TextView) v);
                return true;
            case android.view.MotionEvent.ACTION_UP:
            case android.view.MotionEvent.ACTION_CANCEL:
                if (draggingTab != v) return false;
                draggingTab = null;
                v.animate().translationX(0f).setDuration(140).start();
                v.setAlpha(1f);
                v.setTranslationZ(0f);
                Config.setHomeModeOrder(this, modeOrder);
                refreshTabColors(modeToggle);
                return true;
        }
        return false;
    }

    private boolean reorderPending;

    private void maybeReorderTab(TextView tab) {
        if (reorderPending) return;
        int idx = modeToggle.indexOfChild(tab);
        float center = tab.getLeft() + tab.getWidth() / 2f + tab.getTranslationX();

        View neighbour = null;
        int neighbourIdx = -1;
        if (idx > 0) {
            View left = modeToggle.getChildAt(idx - 1);
            if (center < left.getLeft() + left.getWidth() / 2f) {
                neighbour = left;
                neighbourIdx = idx - 1;
            }
        }
        if (neighbour == null && idx < modeToggle.getChildCount() - 1) {
            View right = modeToggle.getChildAt(idx + 1);
            if (center > right.getLeft() + right.getWidth() / 2f) {
                neighbour = right;
                neighbourIdx = idx + 1;
            }
        }
        if (neighbour == null) return;

        // Move the NEIGHBOUR across the dragged tab. The dragged view is never
        // detached, so its touch stream (the ongoing drag) is not interrupted.
        float beforeLeft = tab.getLeft();
        modeToggle.removeViewAt(neighbourIdx);
        modeToggle.addView(neighbour, idx);
        java.util.Collections.swap(modeOrder, idx, neighbourIdx);
        reorderPending = true;
        modeToggle.post(() -> {
            tab.setTranslationX(tab.getTranslationX() + beforeLeft - tab.getLeft());
            reorderPending = false;
        });
    }

    private void refreshModeToggle() {
        if (modeToggle != null) refreshTabColors(modeToggle);
    }

    private HomeMode sectionAt(int dir) {
        int next = modeOrder.indexOf(homeMode) + dir;
        return (next >= 0 && next < modeOrder.size()) ? modeOrder.get(next) : null;
    }

    /** Tab tap: animated slide to the target section. */
    private void switchMode(HomeMode target) {
        if (homeMode == target) return;
        int diff = modeOrder.indexOf(target) - modeOrder.indexOf(homeMode);
        swipeSwitcher.performJump(diff > 0 ? 1 : -1, Math.abs(diff));
    }

    private void refreshTabColors(LinearLayout row) {
        for (int i = 0; i < row.getChildCount(); i++) {
            TextView tab = (TextView) row.getChildAt(i);
            tab.setTextColor(tab.getTag() == homeMode ? Color.WHITE : Color.DKGRAY);
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

    private LinearLayout buildTipRow(Typeface georgia) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setBackground(rowBackground());
        row.setPadding(48, 8, 48, 36);
        row.setOnClickListener(v -> {
            Tips.advance(this);
            refreshTipRow();
        });

        tipKicker = new TextView(this);
        tipKicker.setTextColor(Color.parseColor("#5C5C5C"));
        tipKicker.setTextSize(11);
        tipKicker.setLetterSpacing(0.2f);
        tipKicker.setTypeface(georgia);
        tipKicker.setPadding(0, 0, 0, 12);
        row.addView(tipKicker);

        tipBody = new TextView(this);
        tipBody.setTextColor(Color.parseColor("#9A9A9A"));
        tipBody.setTextSize(15);
        tipBody.setLineSpacing(6f, 1f);
        tipBody.setTypeface(georgia);
        tipBody.setLines(2);
        tipBody.setEllipsize(android.text.TextUtils.TruncateAt.END);
        row.addView(tipBody);

        return row;
    }

    private void refreshTipRow() {
        if (tipRow == null) return;
        Tips.Entry entry = currentQuery.isEmpty() ? Tips.current(this) : null;
        if (entry == null) {
            tipRow.setVisibility(View.GONE);
            return;
        }
        tipKicker.setText(entry.kicker());
        tipBody.setText(entry.text);
        tipRow.setVisibility(View.VISIBLE);
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

        if (Config.getLockedPackages(this).contains(pkg)
                && !Config.isAppRecentlyUnlocked(this, pkg)) {
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

        if (Config.getLockedPackages(this).contains(pkg)) {
            Config.markAppUnlocked(this, pkg);
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

