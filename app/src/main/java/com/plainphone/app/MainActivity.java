package com.plainphone.app;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
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
import android.widget.Toast;

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
    private LinearLayout menuRow;
    private View menuDivider;
    private android.widget.HorizontalScrollView modeToggleScroller;
    private VDragStrip headerStrip;
    private TextView chevLeft, chevRight;
    private LinearLayout tipRow;
    private TextView tipKicker;
    private TextView tipBody;

    /** Swipe-away header: 0 = all shown, 1 = menu section hidden, 2 = search hidden too. */
    private LinearLayout headerZone;
    private int collapseStage = 0;
    private int headerFullH, headerSearchH;
    private float headerOffset;               // px currently folded away (0 .. headerFullH)
    private boolean headerDragging;
    private ValueAnimator headerAnim;
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
    private static final int REQUEST_RECORDER_UNLOCK = 4307;
    private FrameLayout artFrame;
    private boolean showingHomeReminder = false;
    private boolean homeUiBuilt = false;
    // Home-list settings groups — collapsed on every entry, never persisted.
    private boolean notesSettingsOpen, todoSettingsOpen, vaultSettingsOpen, recorderSettingsOpen;

    /** Shared multi-select: null unless a section is in selection mode. */
    private HomeMode selectMode;
    private final java.util.LinkedHashSet<String> selection = new java.util.LinkedHashSet<>();
    private LinearLayout selectionBar;
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
        } else if (requestCode == REQUEST_RECORDER_UNLOCK && resultCode == RESULT_OK) {
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
        Config.migrateArt(this);

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
        VaultJobs.resumeIfPending(this);
        VaultJobs.addListener(vaultJobListener);
        if (showingHomeReminder && isDefaultHomeApp()) {
            showingHomeReminder = false;
            startLoadingHomeUi();
        } else if (homeUiBuilt) {
            if (Config.getFontChoice(this) != builtWithFont
                    || !modeOrder.equals(Config.getHomeModeOrder(this))) {
                recreate();   // font changed, or a section was shown/hidden
                return;
            }

            listView.setTranslationX(0f);
            listView.setAlpha(1f);
            statsPanelShown = false;
            refreshApps();

            WebSearch.forget();
            ArtGallery.maybeAdvance(this);
            applyPixelArtSelection();
            scheduleArtRotation();
            refreshTimeBlockRow();
            Tips.maybeAutoAdvance(this);
            refreshTipRow();
            scheduleTipRotation();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        tipHandler.removeCallbacks(tipRotate);
        artHandler.removeCallbacks(artRotate);
        VaultJobs.removeListener(vaultJobListener);
    }

    private final VaultJobs.Listener vaultJobListener = () -> runOnUiThread(() -> {
        if (!isFinishing() && !isDestroyed() && homeUiBuilt) filter(search.getText().toString());
    });

    @Override
    public void onBackPressed() {
        if (selectMode != null) {
            exitSelection();
            return;
        }
        if (search.getText().length() > 0) {
            search.setText("");
            return;
        }
        // Nothing behind the home screen — swallow the press. Calling super here
        // finishes this activity, which drops the user onto whatever app was last
        // open; a launcher's Home is meant to be the floor.
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        searchHandler.removeCallbacksAndMessages(null);
        tipHandler.removeCallbacks(tipRotate);
        artHandler.removeCallbacks(artRotate);
        FileIndex.setListener(null);
        if (statsPanel != null) statsPanel.shutdown();
    }

    private final Handler tipHandler = new Handler(Looper.getMainLooper());
    private final Runnable tipRotate = new Runnable() {
        @Override
        public void run() {
            int minutes = Config.getTipRotateMinutes(MainActivity.this);
            if (minutes <= 0) return;
            Tips.advance(MainActivity.this);
            refreshTipRow();
            tipHandler.postDelayed(this, minutes * 60_000L);
        }
    };

    private void scheduleTipRotation() {
        tipHandler.removeCallbacks(tipRotate);
        int minutes = Config.getTipRotateMinutes(this);
        if (minutes > 0) tipHandler.postDelayed(tipRotate, minutes * 60_000L);
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
        View art = ArtKit.homeArt(this);
        artFrame.setVisibility(art == null ? View.GONE : View.VISIBLE);
        if (art != null) {
            artFrame.addView(art, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        }
    }

    private final Handler artHandler = new Handler(Looper.getMainLooper());
    private final Runnable artRotate = new Runnable() {
        @Override
        public void run() {
            if (!slideshowActive()) return;
            ArtGallery.advance(MainActivity.this);
            crossfadeArt();
            artHandler.postDelayed(this, Config.getArtSlideshowMinutes(MainActivity.this) * 60_000L);
        }
    };

    private boolean slideshowActive() {
        return !"off".equals(Config.getArtMode(this))
                && ArtGallery.selectedIds(this).size() >= 2
                && Config.getArtSlideshowMinutes(this) > 0;
    }

    private void scheduleArtRotation() {
        artHandler.removeCallbacks(artRotate);
        if (slideshowActive()) {
            artHandler.postDelayed(artRotate, Config.getArtSlideshowMinutes(this) * 60_000L);
        }
    }

    private void crossfadeArt() {
        if (artFrame == null) return;
        View next = ArtKit.homeArt(this);
        if (next == null) {
            applyPixelArtSelection();
            return;
        }
        next.setAlpha(0f);
        artFrame.addView(next, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        long dur = animatorsOff() ? 0 : 400;
        next.animate().alpha(1f).setDuration(dur).withEndAction(() -> {
            for (int i = artFrame.getChildCount() - 1; i >= 0; i--) {
                if (artFrame.getChildAt(i) != next) artFrame.removeViewAt(i);
            }
        }).start();
    }

    private boolean animatorsOff() {
        try {
            return android.provider.Settings.Global.getFloat(getContentResolver(),
                    android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f;
        } catch (Exception e) {
            return false;
        }
    }

    private void refreshApps() {
        new Thread(() -> {
            List<ResolveInfo> loaded = loadLaunchableApps();
            runOnUiThread(() -> {
                if (isDestroyed() || isFinishing() || !homeUiBuilt) return;
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

        // Collapsible header: search on top, then the menu section. A vertical
        // swipe on the tab strip folds it away, clipping from the bottom.
        headerZone = new LinearLayout(this);
        headerZone.setOrientation(LinearLayout.VERTICAL);
        headerZone.setBackgroundColor(Color.BLACK);
        headerZone.setClipChildren(true);
        root.addView(headerZone, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

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

        headerZone.addView(search, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        tipRow = buildTipRow(georgia);
        headerZone.addView(tipRow, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        Tips.advance(this);
        refreshTipRow();
        scheduleTipRotation();

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

        menuRow = new LinearLayout(this);
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

        ArtGallery.maybeAdvance(this);
        applyPixelArtSelection();
        scheduleArtRotation();

        headerZone.addView(menuRow, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        timeBlockRow = buildRow(georgia, "");
        timeBlockRow.setVisibility(View.GONE);
        timeBlockRow.setOnClickListener(v -> startActivity(new Intent(this, TimeBlocksActivity.class)));
        headerZone.addView(timeBlockRow, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        menuDivider = divider();
        headerZone.addView(menuDivider);

        modeToggle = buildModeToggle(georgia);
        modeToggleScroller = new EdgeSnapScrollView(this);
        modeToggleScroller.setHorizontalScrollBarEnabled(false);
        modeToggleScroller.setOverScrollMode(View.OVER_SCROLL_ALWAYS);
        modeToggleScroller.addView(modeToggle, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        modeToggleScroller.setOnScrollChangeListener(
                (v, sx, sy, osx, osy) -> updateHeaderChevrons());

        chevLeft = headerChevron("‹");
        chevRight = headerChevron("›");
        headerStrip = new VDragStrip(this);
        headerStrip.setOrientation(LinearLayout.HORIZONTAL);
        headerStrip.setGravity(Gravity.CENTER_VERTICAL);
        headerStrip.setBackgroundColor(Color.BLACK);
        headerStrip.setDragListener(new VDragStrip.Listener() {
            @Override public void onDragStart() { onHeaderDragStart(); }
            @Override public void onDragBy(float dy) { onHeaderDragBy(dy); }
            @Override public void onDragEnd(float velocityY) { onHeaderDragEnd(velocityY); }
        });
        headerStrip.addView(chevLeft);
        headerStrip.addView(modeToggleScroller, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        headerStrip.addView(chevRight);
        root.addView(headerStrip, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        modeToggleScroller.post(this::scrollActiveTabIntoView);

        selectionBar = new LinearLayout(this);
        selectionBar.setOrientation(LinearLayout.VERTICAL);
        selectionBar.setBackgroundColor(Color.BLACK);
        selectionBar.setVisibility(View.GONE);
        root.addView(selectionBar, new LinearLayout.LayoutParams(
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
            if (result.payload instanceof Note && currentQuery.isEmpty()) {
                Note n = (Note) result.payload;
                if (selectMode == HomeMode.NOTES) toggleSelected(n.id);
                else enterSelection(HomeMode.NOTES, n.id);
                return true;
            }
            if (result.payload instanceof Recording) {
                Recording rec = (Recording) result.payload;
                if (selectMode == HomeMode.RECORDER) toggleSelected(rec.id);
                else enterSelection(HomeMode.RECORDER, rec.id);
                return true;
            }
            if (result.payload instanceof Todos.Item && currentQuery.isEmpty()) {
                Todos.Item item = (Todos.Item) result.payload;
                String tid = "todo:" + item.index;
                if (selectMode == HomeMode.TODOS) toggleSelected(tid);
                else enterSelection(HomeMode.TODOS, tid);
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
            if (first == null || first.guarded) return false;
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

    // --- swipe-away header ------------------------------------------------

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private boolean collapseEligible() {
        return homeUiBuilt && currentQuery.isEmpty() && selectMode == null
                && (listView.getVisibility() == View.VISIBLE || statsPanelShown);
    }

    /** Re-measure the header's natural full height and the search band height. */
    private void measureHeader() {
        ViewGroup.LayoutParams lp = headerZone.getLayoutParams();
        lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        headerZone.setLayoutParams(lp);
        int w = headerZone.getWidth() > 0 ? headerZone.getWidth()
                : getResources().getDisplayMetrics().widthPixels;
        headerZone.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        headerFullH = headerZone.getMeasuredHeight();
        headerSearchH = search.getMeasuredHeight() > 0 ? search.getMeasuredHeight()
                : search.getHeight();
        if (headerSearchH <= 0) headerSearchH = dp(56);
        applyHeaderOffset();
    }

    private void applyHeaderOffset() {
        if (headerZone == null) return;
        ViewGroup.LayoutParams lp = headerZone.getLayoutParams();
        if (headerOffset <= 0f) {
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        } else {
            lp.height = Math.max(0, Math.round(headerFullH - headerOffset));
        }
        headerZone.setLayoutParams(lp);
    }

    private float snapForStage(int stage) {
        if (stage <= 0) return 0f;
        if (stage == 1) return Math.max(0, headerFullH - headerSearchH);
        return headerFullH;
    }

    private int stageForOffset(float offset) {
        float s1 = snapForStage(1);
        if (offset < s1 / 2f) return 0;
        if (offset < (s1 + headerFullH) / 2f) return 1;
        return 2;
    }


    private void onHeaderDragStart() {
        if (!collapseEligible()) { headerDragging = false; return; }
        if (headerAnim != null) { headerAnim.cancel(); headerAnim = null; }
        measureHeader();
        headerDragging = true;
    }

    private void onHeaderDragBy(float dy) {
        if (!headerDragging) return;
        float next = headerOffset - dy;                    // drag up (dy < 0) folds more
        if (next < 0f) next *= 0.3f;                       // rubber-band past the ends
        else if (next > headerFullH) next = headerFullH + (next - headerFullH) * 0.3f;
        headerOffset = next;
        applyHeaderOffset();
    }

    private void onHeaderDragEnd(float velocityY) {
        if (!headerDragging) return;
        headerDragging = false;
        float bounded = Math.max(0f, Math.min(headerFullH, headerOffset));
        int target = stageForOffset(bounded);
        if (velocityY < -dp(700)) target = Math.min(2, target + 1);
        else if (velocityY > dp(700)) target = Math.max(0, target - 1);
        animateHeaderToStage(target);
    }

    private void animateHeaderToStage(int stage) {
        collapseStage = stage;
        final float to = snapForStage(stage);
        if (headerAnim != null) headerAnim.cancel();
        headerAnim = ValueAnimator.ofFloat(headerOffset, to);
        headerAnim.setDuration(160);
        headerAnim.setInterpolator(new android.view.animation.DecelerateInterpolator());
        headerAnim.addUpdateListener(a -> {
            headerOffset = (float) a.getAnimatedValue();
            applyHeaderOffset();
        });
        headerAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator anim) {
                headerAnim = null;
                headerOffset = to;
                applyHeaderOffset();
            }
        });
        headerAnim.start();
    }

    /** Force the header fully open (searching / selecting / stats). */
    private void setCollapseStage(int stage) {
        if (headerAnim != null) { headerAnim.cancel(); headerAnim = null; }
        collapseStage = Math.max(0, Math.min(2, stage));
        headerDragging = false;
        if (collapseStage == 0) {
            headerOffset = 0f;
            applyHeaderOffset();
            return;
        }
        measureHeader();
        headerOffset = snapForStage(collapseStage);
        applyHeaderOffset();
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
        PluginLock.requestLockAll(this, () -> Lock.lockAllSections(this), () -> {
            search.setText("");
            filter("");
            android.widget.Toast.makeText(this,
                    Config.isPinSet(this) ? "Locked" : "Locked — set an App-lock PIN to take effect",
                    android.widget.Toast.LENGTH_SHORT).show();
        });
    }

    private void renderRows() {
        rows.clear();
        String needle = currentQuery;

        if (selectMode != null && (selectMode != homeMode || !needle.isEmpty())) {
            selectMode = null;
            selection.clear();
        }
        boolean selecting = selectMode != null;

        if (headerStrip != null) {
            boolean showStrip = needle.isEmpty() && !selecting;
            headerStrip.setVisibility(showStrip ? View.VISIBLE : View.GONE);
            if (showStrip) modeToggleScroller.post(this::updateHeaderChevrons);
        }
        if (menuRow != null) {
            menuRow.setVisibility(needle.isEmpty() && !selecting ? View.VISIBLE : View.GONE);
        }
        if (menuDivider != null) {
            menuDivider.setVisibility(needle.isEmpty() && !selecting ? View.VISIBLE : View.GONE);
        }
        selectionBar.setVisibility(selecting ? View.VISIBLE : View.GONE);
        refreshTipRow();
        if (tipRow != null && selecting) tipRow.setVisibility(View.GONE);

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
            if (!collapseEligible()) setCollapseStage(0);
            adapter.notifyDataSetChanged();
            return;
        }

        if (needle.isEmpty()) {
            if (homeMode == HomeMode.NOTES) {
                if (Lock.NOTES.gateActive(this)) {
                    rows.add(new SearchResult(SearchResult.Kind.NOTE, "Notes are locked",
                            "Tap to unlock", -1, () -> startActivityForResult(
                            Lock.NOTES.pinGate(this), REQUEST_NOTES_UNLOCK)));
                } else if (selecting) {
                    renderNoteSelection();
                } else {
                    if (Lock.NOTES.isLocked(this)) Lock.NOTES.keepUnlocked(this);
                    renderNoteSettingsGroup();
                    rows.add(newNoteRow());
                    rows.addAll(noteBrowseResults());
                }
            } else if (homeMode == HomeMode.TODOS) {
                renderTodoSection();
            } else if (homeMode == HomeMode.RECORDER) {
                renderRecorderSection();
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

        // Searching / selecting / stats: the header must be fully visible again.
        if (!collapseEligible()) setCollapseStage(0);
    }

    private List<SearchResult> resultsFor(SearchResult.Kind kind, String needle) {
        switch (kind) {
            case ACTION: return QuickActions.results(this, currentSearch);
            case NOTE: return noteResults(needle);
            case TODO: return todoResults(needle);
            case RECORDING: return recordingResults(needle);
            case PLAIN: return SearchTargets.plain(this, currentSearch);
            case SYSTEM: return SearchTargets.system(this, currentSearch);
            case WEB: return webResults();
            case FILE: return fileResults(needle);
            case CONTACT: return contactResults(needle);
            case VAULT: return vaultResults(needle);
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
        List<Note> notes = new ArrayList<>(Config.getNotes(this));
        notes.addAll(Notes.vaultNotes(this));
        Collections.sort(notes, (a, b) -> Long.compare(b.updatedAt, a.updatedAt));
        List<SearchResult> results = new ArrayList<>();
        for (int i = 0; i < notes.size(); i++) {
            Note note = notes.get(i);
            results.add(new SearchResult(SearchResult.Kind.NOTE, note.title(), note.preview(), i,
                    () -> openNote(note.id), note));
        }
        if (!VaultSession.get().isUnlocked() && VaultFormat.exists(VaultSession.vaultRoot(this))
                && Config.getVaultNoteCount(this) > 0) {
            int n = Config.getVaultNoteCount(this);
            results.add(new SearchResult(SearchResult.Kind.NOTE,
                    n + (n == 1 ? " note in the vault" : " notes in the vault"), "Unlock to read", -1,
                    () -> startActivity(new Intent(this, VaultActivity.class))));
        }
        return results;
    }

    private List<SearchResult> noteResults(String needle) {
        if (Lock.NOTES.gateActive(this)) return new ArrayList<>();
        List<SearchResult> results = new ArrayList<>();
        List<Note> all = new ArrayList<>(Config.getNotes(this));
        all.addAll(Notes.vaultNotes(this));
        for (Note note : all) {
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
        boolean expanded = notesSettingsOpen;
        rows.add(new SearchResult(SearchResult.Kind.NOTE,
                (expanded ? "▾  " : "▸  ") + "Notes settings", null, -1, () -> {
            notesSettingsOpen = !expanded;
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

        int plainCount = Config.getNotes(this).size();
        if (plainCount > 0 && VaultFormat.exists(VaultSession.vaultRoot(this))) {
            rows.add(new SearchResult(SearchResult.Kind.NOTE,
                    "Move all notes to vault", null, -1, () -> {
                if (!VaultSession.get().isUnlocked()) {
                    Toast.makeText(this, "Unlock the vault first", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, VaultActivity.class));
                    return;
                }
                VaultUi.confirm(this, "Move " + plainCount + " note"
                                + (plainCount == 1 ? "" : "s") + " to the vault?",
                        "They'll be encrypted and only readable while the vault is unlocked.",
                        "Move", () -> {
                            int moved = Notes.moveAllToVault(this);
                            Toast.makeText(this, "Moved " + moved + " to the vault",
                                    Toast.LENGTH_SHORT).show();
                            filter(search.getText().toString());
                        }, "Cancel", null);
            }));
        }
    }

    private void openNote(String id) {
        if (Notes.isVaulted(id)) {
            startActivity(new Intent(this, VaultTextViewerActivity.class)
                    .putExtra("docId", Notes.docIdOf(id))
                    .putExtra("name", Notes.vaultNoteName(this, id)));
            return;
        }
        if (Lock.NOTES.isLocked(this)) Lock.NOTES.keepUnlocked(this);
        Intent intent = new Intent(this, NoteEditActivity.class);
        intent.putExtra("noteId", id);
        startActivity(intent);
    }

    // --- voice recorder ---------------------------------------------------

    private List<Recording> recorderAll() {
        List<Recording> list = new ArrayList<>(Recorder.all(this));
        list.addAll(Recorder.vaultRecordings(this));
        Collections.sort(list, (a, b) -> Long.compare(b.createdAt, a.createdAt));
        return list;
    }

    private void renderRecorderSection() {
        if (Lock.RECORDER.gateActive(this)) {
            rows.add(new SearchResult(SearchResult.Kind.RECORDING, "Recorder is locked",
                    "Tap to unlock", -1, () -> startActivityForResult(
                    Lock.RECORDER.pinGate(this), REQUEST_RECORDER_UNLOCK)));
            return;
        }
        if (Lock.RECORDER.isLocked(this)) Lock.RECORDER.keepUnlocked(this);

        if (selectMode == HomeMode.RECORDER) {
            renderRecorderSelection(recorderAll());
            return;
        }
        boolean expanded = recorderSettingsOpen;
        rows.add(new SearchResult(SearchResult.Kind.RECORDING,
                (expanded ? "▾  " : "▸  ") + "Recorder settings", null, -1, () -> {
            recorderSettingsOpen = !expanded;
            filter(search.getText().toString());
        }));
        if (expanded) {
            rows.add(new SearchResult(SearchResult.Kind.RECORDING,
                    "Format: " + Config.getRecorderFormat(this).toUpperCase(java.util.Locale.US),
                    null, -1, () -> startActivity(new Intent(this, RecorderSettingsActivity.class))));
            rows.add(new SearchResult(SearchResult.Kind.RECORDING,
                    "Sample rate: " + Config.getRecorderSampleRate(this) + " Hz", null, -1,
                    () -> startActivity(new Intent(this, RecorderSettingsActivity.class))));
            rows.add(new SearchResult(SearchResult.Kind.RECORDING,
                    "Locked: " + (Lock.RECORDER.isLocked(this) ? "On" : "Off"), null, -1,
                    () -> Lock.RECORDER.toggleLock(this, () -> filter(search.getText().toString()))));
            int local = Recorder.all(this).size();
            if (local > 0 && VaultFormat.exists(VaultSession.vaultRoot(this))) {
                rows.add(new SearchResult(SearchResult.Kind.RECORDING,
                        "Move all recordings to vault", null, -1, () -> {
                    if (!VaultSession.get().isUnlocked()) {
                        Toast.makeText(this, "Unlock the vault first", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, VaultActivity.class));
                        return;
                    }
                    VaultUi.confirm(this, "Move " + local + " recording"
                                    + (local == 1 ? "" : "s") + " to the vault?",
                            "They'll be encrypted and only playable while the vault is unlocked.",
                            "Move", () -> {
                                int moved = Recorder.moveAllToVault(this);
                                Toast.makeText(this, "Moved " + moved + " to the vault",
                                        Toast.LENGTH_SHORT).show();
                                filter(search.getText().toString());
                            }, "Cancel", null);
                }));
            }
        }

        rows.add(new SearchResult(SearchResult.Kind.RECORDING, "+ New recording", null, -1,
                () -> startActivity(new Intent(this, RecordActivity.class))));
        rows.addAll(recorderBrowseResults());
    }

    private List<SearchResult> recorderBrowseResults() {
        List<Recording> list = recorderAll();
        List<SearchResult> results = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            Recording r = list.get(i);
            results.add(new SearchResult(SearchResult.Kind.RECORDING, r.displayName(),
                    r.subtitle(), i, () -> openRecording(r.id), r));
        }
        if (!VaultSession.get().isUnlocked() && VaultFormat.exists(VaultSession.vaultRoot(this))
                && Config.getRecordingCount(this) > 0) {
            int n = Config.getRecordingCount(this);
            results.add(new SearchResult(SearchResult.Kind.RECORDING,
                    n + (n == 1 ? " recording in the vault" : " recordings in the vault"),
                    "Unlock to play", -1,
                    () -> startActivity(new Intent(this, VaultActivity.class))));
        }
        return results;
    }

    // --- shared multi-select ------------------------------------------------

    private static final class BarAction {
        final String label;
        final Runnable run;
        BarAction(String label, Runnable run) { this.label = label; this.run = run; }
    }

    private void enterSelection(HomeMode mode, String firstId) {
        selectMode = mode;
        selection.clear();
        if (firstId != null) selection.add(firstId);
        filter(search.getText().toString());
    }

    private void toggleSelected(String id) {
        if (!selection.remove(id)) selection.add(id);
        // Empty selection stays in selection mode — only ✕ (or a tab switch / search) leaves.
        filter(search.getText().toString());
    }

    private void exitSelection() {
        selectMode = null;
        selection.clear();
        filter(search.getText().toString());
    }

    private void selectAllOrNone(List<String> allIds) {
        boolean all = !allIds.isEmpty() && selection.size() == allIds.size();
        selection.clear();
        if (!all) selection.addAll(allIds);
        // "Select none" keeps you in selection mode — only unchecking the last row leaves.
        filter(search.getText().toString());
    }

    /** Rebuild the two-row selection bar: ✕ / "N selected" / select-all, then actions. */
    private void buildSelectionBar(List<String> allIds, List<BarAction> actions) {
        Typeface font = Fonts.current(this);
        selectionBar.removeAllViews();
        boolean all = !allIds.isEmpty() && selection.size() == allIds.size();

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(40, 28, 40, 14);

        TextView close = new TextView(this);
        close.setText("✕");
        close.setTextColor(Color.WHITE);
        close.setTextSize(16);
        close.setTypeface(font);
        close.setPadding(8, 8, 28, 8);
        close.setOnClickListener(v -> exitSelection());
        top.addView(close);

        TextView count = new TextView(this);
        count.setText(selection.size() + " selected");
        count.setTextColor(Color.WHITE);
        count.setTextSize(14);
        count.setTypeface(font);
        top.addView(count, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView selAll = new TextView(this);
        selAll.setText(all ? "SELECT NONE" : "SELECT ALL");
        selAll.setTextColor(Color.GRAY);
        selAll.setTextSize(12);
        selAll.setLetterSpacing(0.1f);
        selAll.setTypeface(font);
        selAll.setPadding(16, 8, 8, 8);
        selAll.setOnClickListener(v -> selectAllOrNone(allIds));
        top.addView(selAll);
        selectionBar.addView(top);

        LinearLayout actionRow = new LinearLayout(this);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        actionRow.setPadding(40, 6, 40, 22);
        boolean enabled = !selection.isEmpty();
        for (BarAction a : actions) {
            TextView t = new TextView(this);
            t.setText(a.label.toUpperCase(java.util.Locale.US));
            t.setTextColor(enabled ? Color.WHITE : 0xFF555555);
            t.setTextSize(12);
            t.setLetterSpacing(0.08f);
            t.setTypeface(font);
            t.setPadding(0, 10, 44, 10);
            if (enabled) t.setOnClickListener(v -> a.run.run());
            actionRow.addView(t);
        }
        android.widget.HorizontalScrollView scroller = new android.widget.HorizontalScrollView(this);
        scroller.setHorizontalScrollBarEnabled(false);
        scroller.addView(actionRow);
        selectionBar.addView(scroller);

        selectionBar.addView(divider());
    }

    private List<Recording> selectedRecordings(List<Recording> all) {
        List<Recording> out = new ArrayList<>();
        for (Recording r : all) if (selection.contains(r.id)) out.add(r);
        return out;
    }

    private void renderRecorderSelection(List<Recording> list) {
        List<String> ids = new ArrayList<>();
        for (Recording r : list) ids.add(r.id);
        List<Recording> sel = selectedRecordings(list);
        int locals = 0, vaulted = 0;
        for (Recording r : sel) {
            if (Recorder.isVaulted(r.id)) vaulted++; else locals++;
        }

        List<BarAction> actions = new ArrayList<>();
        if (locals > 0 && VaultFormat.exists(VaultSession.vaultRoot(this))) {
            actions.add(new BarAction("Move to vault", () -> recorderMoveToVault(sel)));
        }
        if (vaulted > 0) {
            actions.add(new BarAction("Move out", () -> {
                int moved = 0;
                for (Recording r : sel) {
                    if (Recorder.isVaulted(r.id) && Recorder.moveOutOfVault(this, r.id)) moved++;
                }
                Toast.makeText(this, "Moved " + moved + " out of the vault",
                        Toast.LENGTH_SHORT).show();
                exitSelection();
            }));
        }
        if (locals > 0) {
            actions.add(new BarAction("Export", () -> recorderExport(sel)));
        }
        if (sel.size() == 1 && locals == 1) {
            Recording one = sel.get(0);
            actions.add(new BarAction("Rename", () -> promptRenameRecording(one)));
        }
        actions.add(new BarAction("Delete", () -> VaultUi.confirm(this,
                "Delete " + sel.size() + " recording" + (sel.size() == 1 ? "" : "s") + "?",
                null, "Delete", () -> {
                    for (Recording r : sel) {
                        if (Recorder.isVaulted(r.id)) Recorder.deleteVaultRecording(this, r.id);
                        else Recorder.deleteLocal(this, r);
                    }
                    exitSelection();
                }, "Cancel", null)));
        buildSelectionBar(ids, actions);

        for (Recording r : list) {
            rows.add(new SearchResult(SearchResult.Kind.RECORDING, r.displayName(), r.subtitle(),
                    -1, () -> toggleSelected(r.id), r).check(selection.contains(r.id)));
        }
    }

    private void recorderMoveToVault(List<Recording> sel) {
        if (!VaultFormat.exists(VaultSession.vaultRoot(this))) {
            VaultUi.confirm(this, "Set up the vault?",
                    "Recordings you move in are encrypted with your vault password.",
                    "Set up", () -> startActivity(new Intent(this, VaultActivity.class)),
                    "Cancel", null);
            return;
        }
        if (!VaultSession.get().isUnlocked()) {
            Toast.makeText(this, "Unlock the vault first", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, VaultActivity.class));
            return;
        }
        int moved = 0;
        for (Recording r : sel) {
            if (!Recorder.isVaulted(r.id) && Recorder.moveToVault(this, r)) moved++;
        }
        Toast.makeText(this, "Moved " + moved + " to the vault", Toast.LENGTH_SHORT).show();
        exitSelection();
    }

    private void recorderExport(List<Recording> sel) {
        java.util.ArrayList<Uri> uris = new java.util.ArrayList<>();
        for (Recording r : sel) {
            if (Recorder.isVaulted(r.id)) continue;
            java.io.File f = Recorder.fileFor(this, r);
            if (f.isFile()) uris.add(PlainFileProvider.uriFor(getPackageName() + ".files", f));
        }
        sendFiles(uris, "audio/*", "Send recordings");
    }

    private void sendFiles(java.util.ArrayList<Uri> uris, String mime, String chooser) {
        if (uris.isEmpty()) {
            Toast.makeText(this, "Nothing to export", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Intent send = new Intent(uris.size() == 1
                    ? Intent.ACTION_SEND : Intent.ACTION_SEND_MULTIPLE);
            send.setType(mime);
            if (uris.size() == 1) send.putExtra(Intent.EXTRA_STREAM, uris.get(0));
            else send.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
            send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(send, chooser));
        } catch (Exception e) {
            Toast.makeText(this, "Export failed", Toast.LENGTH_SHORT).show();
        }
    }

    // --- notes multi-select ---

    private void renderNoteSelection() {
        List<Note> list = new ArrayList<>(Config.getNotes(this));
        list.addAll(Notes.vaultNotes(this));
        Collections.sort(list, (a, b) -> Long.compare(b.updatedAt, a.updatedAt));

        List<String> ids = new ArrayList<>();
        for (Note n : list) ids.add(n.id);
        List<Note> sel = new ArrayList<>();
        for (Note n : list) if (selection.contains(n.id)) sel.add(n);
        int locals = 0, vaulted = 0;
        for (Note n : sel) {
            if (Notes.isVaulted(n.id)) vaulted++; else locals++;
        }

        List<BarAction> actions = new ArrayList<>();
        if (locals > 0 && VaultFormat.exists(VaultSession.vaultRoot(this))) {
            actions.add(new BarAction("Move to vault", () -> {
                if (!VaultSession.get().isUnlocked()) {
                    Toast.makeText(this, "Unlock the vault first", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, VaultActivity.class));
                    return;
                }
                int moved = 0;
                for (Note n : sel) if (!Notes.isVaulted(n.id) && Notes.moveToVault(this, n)) moved++;
                Toast.makeText(this, "Moved " + moved + " to the vault", Toast.LENGTH_SHORT).show();
                exitSelection();
            }));
        }
        if (vaulted > 0) {
            actions.add(new BarAction("Move out", () -> {
                int moved = 0;
                for (Note n : sel) if (Notes.isVaulted(n.id) && Notes.moveOutOfVault(this, n.id)) moved++;
                Toast.makeText(this, "Moved " + moved + " out of the vault", Toast.LENGTH_SHORT).show();
                exitSelection();
            }));
        }
        if (locals > 0) {
            actions.add(new BarAction("Export", () -> {
                java.util.ArrayList<Uri> uris = new java.util.ArrayList<>();
                for (Note n : sel) {
                    if (Notes.isVaulted(n.id)) continue;
                    try {
                        java.io.File f = new java.io.File(getCacheDir(), Notes.exportFileName(n));
                        try (java.io.OutputStream os = new java.io.FileOutputStream(f)) {
                            os.write(Notes.exportText(n).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                        }
                        uris.add(PlainFileProvider.uriFor(getPackageName() + ".files", f));
                    } catch (Exception ignored) {
                    }
                }
                sendFiles(uris, Notes.EXPORT_MIME, "Send notes");
            }));
        }
        actions.add(new BarAction("Delete", () -> VaultUi.confirm(this,
                "Delete " + sel.size() + " note" + (sel.size() == 1 ? "" : "s") + "?",
                null, "Delete", () -> {
                    List<Note> keep = Config.getNotes(this);
                    keep.removeIf(n -> selection.contains(n.id));
                    Config.setNotes(this, keep);
                    for (Note n : sel) if (Notes.isVaulted(n.id)) Notes.deleteVaultNote(this, n.id);
                    exitSelection();
                }, "Cancel", null)));
        buildSelectionBar(ids, actions);

        for (Note n : list) {
            rows.add(new SearchResult(SearchResult.Kind.NOTE, n.title(), n.preview(), -1,
                    () -> toggleSelected(n.id), n).check(selection.contains(n.id)));
        }
    }

    // --- to-do multi-select ---

    private void renderTodoSelection() {
        List<Todos.Item> items = Todos.sortedForView(Todos.load(this),
                Config.isTodosShowCompleted(this));
        List<String> ids = new ArrayList<>();
        for (Todos.Item it : items) ids.add("todo:" + it.index);

        java.util.List<Integer> selIdx = new ArrayList<>();
        int done = 0, open = 0;
        for (Todos.Item it : items) {
            if (selection.contains("todo:" + it.index)) {
                selIdx.add(it.index);
                if (it.todo.done) done++; else open++;
            }
        }

        List<BarAction> actions = new ArrayList<>();
        if (open > 0) {
            actions.add(new BarAction("Complete", () -> {
                Todos.setDone(this, selIdx, true);
                exitSelection();
            }));
        }
        if (done > 0) {
            actions.add(new BarAction("Reopen", () -> {
                Todos.setDone(this, selIdx, false);
                exitSelection();
            }));
        }
        if (selIdx.size() == 1) {
            char cur = 0;
            for (Todos.Item it : items) if (it.index == selIdx.get(0)) cur = it.todo.priority;
            char nextP = nextPriority(cur);
            int only = selIdx.get(0);
            actions.add(new BarAction("Priority " + nextPriorityLabel(cur), () -> {
                Todos.setPriority(this, only, nextP);
                exitSelection();
            }));
        }
        actions.add(new BarAction("Delete", () -> VaultUi.confirm(this,
                "Delete " + selIdx.size() + " task" + (selIdx.size() == 1 ? "" : "s") + "?",
                null, "Delete", () -> {
                    Todos.deleteAll(this, selIdx);
                    exitSelection();
                }, "Cancel", null)));
        buildSelectionBar(ids, actions);

        for (Todos.Item it : items) {
            String id = "todo:" + it.index;
            rows.add(new SearchResult(SearchResult.Kind.TODO, todoTitle(it.todo),
                    todoSubtitle(it.todo), -1, () -> toggleSelected(id), it)
                    .withStrike(it.todo.done).check(selection.contains(id)));
        }
    }

    private List<SearchResult> recordingResults(String needle) {
        List<SearchResult> results = new ArrayList<>();
        if (Lock.RECORDER.gateActive(this)) return results;
        for (Recording r : recorderAll()) {
            int score = TextMatch.score(r.displayName(), currentSearch);
            if (score == TextMatch.NO_MATCH) continue;
            results.add(new SearchResult(SearchResult.Kind.RECORDING, r.displayName(),
                    r.subtitle(), score, () -> openRecording(r.id), r));
        }
        return results;
    }

    private void openRecording(String id) {
        if (Lock.RECORDER.isLocked(this)) Lock.RECORDER.keepUnlocked(this);
        Intent intent = new Intent(this, RecordingPlayerActivity.class);
        if (Recorder.isVaulted(id)) {
            String name = Recorder.vaultRecordingName(this, id);
            int dot = name.lastIndexOf('.');
            intent.putExtra("docId", Recorder.docIdOf(id));
            intent.putExtra("name", dot > 0 ? name.substring(0, dot) : name);
            intent.putExtra("format", dot >= 0 ? name.substring(dot + 1).toLowerCase() : "m4a");
        } else {
            intent.putExtra("recId", id);
        }
        startActivity(intent);
    }

    private void promptRenameRecording(Recording rec) {
        Typeface georgia = Fonts.current(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(popupBackground());
        root.setPadding(0, 32, 0, 8);

        TextView title = new TextView(this);
        title.setText("Rename recording");
        title.setTextColor(Color.WHITE);
        title.setTextSize(18);
        title.setTypeface(georgia);
        title.setPadding(48, 0, 48, 16);
        root.addView(title);

        EditText input = new EditText(this);
        input.setText(rec.displayName());
        input.setSelectAllOnFocus(true);
        input.setBackground(null);
        input.setTextColor(Color.WHITE);
        input.setTypeface(georgia);
        input.setTextSize(18);
        input.setSingleLine(true);
        input.setPadding(48, 8, 48, 16);
        root.addView(input);

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setView(root).create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        root.addView(optionRow(georgia, "Save", v -> {
            String name = input.getText().toString().trim();
            if (!name.isEmpty()) Recorder.rename(this, rec.id, name);
            dialog.dismiss();
            exitSelection();
        }));
        root.addView(optionRow(georgia, "Cancel", v -> dialog.dismiss()));
        dialog.show();
        if (dialog.getWindow() != null) {
            android.view.WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
            params.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.85);
            dialog.getWindow().setAttributes(params);
        }
    }

    private void renderVaultSection() {
        if (VaultJobs.resetPending(this)) {
            rows.add(new SearchResult(SearchResult.Kind.PLAIN, "Vault",
                    "Erases all data stored in vault", -1, () -> {}));
            return;
        }
        boolean created = VaultFormat.exists(VaultSession.vaultRoot(this));
        boolean unlocked = VaultSession.get().isUnlocked();

        if (created && unlocked) {
            boolean expanded = vaultSettingsOpen;
            rows.add(new SearchResult(SearchResult.Kind.PLAIN,
                    (expanded ? "▾  " : "▸  ") + "Vault settings", null, -1, () -> {
                vaultSettingsOpen = !expanded;
                filter(search.getText().toString());
            }));
            if (expanded) {
                rows.add(new SearchResult(SearchResult.Kind.PLAIN, "Show on home screen: "
                        + (Config.isVaultHiddenFromHome(this) ? "Off" : "On"), null, -1, () -> {
                    Config.setVaultHiddenFromHome(this, !Config.isVaultHiddenFromHome(this));
                    recreate();
                }));
                rows.add(new SearchResult(SearchResult.Kind.PLAIN, "Auto-lock: "
                        + formatVaultTimeout(Config.getVaultAutoLockSeconds(this)), null, -1,
                        () -> startActivity(new Intent(this, VaultAutoLockActivity.class))));
                rows.add(new SearchResult(SearchResult.Kind.PLAIN, "More vault settings", null, -1,
                        () -> startActivity(new Intent(this, VaultSettingsActivity.class))));
            }
        }

        String title = created && !unlocked ? "Vault is locked" : "Vault";
        String state = !created ? "Tap to set up"
                : unlocked ? "Unlocked — tap to browse" : "Tap to unlock";
        rows.add(new SearchResult(SearchResult.Kind.PLAIN, title, state, -1,
                () -> startActivity(new Intent(this, VaultActivity.class))));
        if (created && unlocked) {
            rows.add(new SearchResult(SearchResult.Kind.PLAIN, "Lock vault now", null, -1, () ->
                    PluginLock.requestLock(this, java.util.EnumSet.of(HomeMode.VAULT),
                            () -> filter(search.getText().toString()))));
        }
    }

    private static String formatVaultTimeout(int seconds) {
        if (seconds % 60 == 0) return (seconds / 60) + " min";
        return seconds + " s";
    }

    private void renderTodoSection() {
        if (Lock.TODOS.gateActive(this)) {
            rows.add(new SearchResult(SearchResult.Kind.TODO, "To-do is locked",
                    "Tap to unlock", -1, () -> startActivityForResult(
                    Lock.TODOS.pinGate(this), REQUEST_TODOS_UNLOCK)));
            return;
        }
        if (Lock.TODOS.isLocked(this)) Lock.TODOS.keepUnlocked(this);

        if (selectMode == HomeMode.TODOS) {
            renderTodoSelection();
            return;
        }

        boolean expanded = todoSettingsOpen;
        rows.add(new SearchResult(SearchResult.Kind.TODO,
                (expanded ? "▾  " : "▸  ") + "To-do settings", null, -1, () -> {
            todoSettingsOpen = !expanded;
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

    /**
     * Tab-strip scroller: a little rubber-band past either edge that springs back in
     * one motion. The natural scroll extent already ends at the last tab (see
     * {@link #trimTrailingTab}), so there's no second snap after the spring.
     */
    private static class EdgeSnapScrollView extends android.widget.HorizontalScrollView {
        EdgeSnapScrollView(Context context) {
            super(context);
        }

        @Override
        protected boolean overScrollBy(int dx, int dy, int sx, int sy, int rangeX, int rangeY,
                                       int maxOverX, int maxOverY, boolean isTouch) {
            int over = (int) (40 * getResources().getDisplayMetrics().density);
            return super.overScrollBy(dx, dy, sx, sy, rangeX, rangeY, over, 0, isTouch);
        }
    }

    /** The visually-last tab carries no inter-tab spacing on its right — keep that after reorders. */
    private void trimTrailingTab(LinearLayout row) {
        for (int i = 0; i < row.getChildCount(); i++) {
            boolean last = i == row.getChildCount() - 1;
            row.getChildAt(i).setPadding(0, 8, last ? 0 : 56, 8);
        }
    }

    private TextView headerChevron(String glyph) {
        TextView t = new TextView(this);
        t.setText(glyph);
        t.setTextColor(0xFF5C5C5C);
        t.setTextSize(15);
        t.setTypeface(Fonts.current(this));
        t.setGravity(Gravity.CENTER);
        t.setPadding(14, 0, 14, 0);
        t.setVisibility(View.INVISIBLE);   // space reserved; the strip never re-flows
        return t;
    }

    /**
     * Show a chevron only while the section strip can still scroll that way, and
     * dim any tab the viewport edge is cutting through so the clip looks intentional.
     */
    private void updateHeaderChevrons() {
        if (modeToggleScroller == null || chevLeft == null) return;
        chevLeft.setVisibility(modeToggleScroller.canScrollHorizontally(-1)
                ? View.VISIBLE : View.INVISIBLE);
        chevRight.setVisibility(modeToggleScroller.canScrollHorizontally(1)
                ? View.VISIBLE : View.INVISIBLE);

        if (modeToggle == null || draggingTab != null) return;
        int start = modeToggleScroller.getScrollX();
        int end = start + modeToggleScroller.getWidth();
        for (int i = 0; i < modeToggle.getChildCount(); i++) {
            View tab = modeToggle.getChildAt(i);
            boolean clipped = tab.getLeft() < start || tab.getRight() > end;
            tab.setAlpha(clipped ? 0.35f : 1f);
        }
    }

    private LinearLayout buildModeToggle(Typeface georgia) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(20, 28, 20, 20);   // the flanking chevrons carry the edge inset now

        for (HomeMode mode : modeOrder) {
            TextView tab = new TextView(this);
            tab.setText(mode.label.toUpperCase());
            tab.setTag(mode);
            tab.setTypeface(georgia);
            tab.setTextSize(13);
            tab.setLetterSpacing(0.15f);
            tab.setOnClickListener(v -> {
                if (draggingTab == null) switchMode((HomeMode) v.getTag());
            });
            tab.setOnLongClickListener(v -> {
                draggingTab = (TextView) v;
                if (modeToggleScroller != null) {
                    modeToggleScroller.requestDisallowInterceptTouchEvent(true);
                }
                v.setAlpha(0.55f);
                v.setTranslationZ(12f);
                ((TextView) v).setTextColor(Color.WHITE);
                return true;
            });
            tab.setOnTouchListener(this::onTabTouch);
            row.addView(tab, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        }
        trimTrailingTab(row);
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
                if (modeToggleScroller != null) {
                    modeToggleScroller.requestDisallowInterceptTouchEvent(false);
                }
                v.animate().translationX(0f).setDuration(140).start();
                v.setAlpha(1f);
                v.setTranslationZ(0f);
                Config.setHomeModeOrder(this, modeOrder);
                trimTrailingTab(modeToggle);
                refreshTabColors(modeToggle);
                modeToggleScroller.post(this::updateHeaderChevrons);
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
        if (modeToggle == null) return;
        refreshTabColors(modeToggle);
        modeToggle.post(this::scrollActiveTabIntoView);
    }

    /** Bring the active section's tab fully into the strip (past the dimmed clip edge). */
    private void scrollActiveTabIntoView() {
        if (modeToggle == null || modeToggleScroller == null) return;
        int pad = (int) (24 * getResources().getDisplayMetrics().density);
        int viewStart = modeToggleScroller.getScrollX();
        int viewW = modeToggleScroller.getWidth();
        for (int i = 0; i < modeToggle.getChildCount(); i++) {
            View tab = modeToggle.getChildAt(i);
            if (tab.getTag() != homeMode) continue;
            int target = viewStart;
            if (tab.getLeft() - pad < viewStart) {
                target = tab.getLeft() - pad;
            } else if (tab.getRight() + pad > viewStart + viewW) {
                target = tab.getRight() + pad - viewW;
            }
            if (target != viewStart) modeToggleScroller.smoothScrollTo(Math.max(0, target), 0);
            break;
        }
        modeToggleScroller.post(this::updateHeaderChevrons);
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
        Typeface base = Fonts.current(this);
        for (int i = 0; i < row.getChildCount(); i++) {
            TextView tab = (TextView) row.getChildAt(i);
            boolean active = tab.getTag() == homeMode;
            tab.setTextColor(active ? Color.WHITE : Color.DKGRAY);
            tab.setTypeface(Typeface.create(base, active ? Typeface.BOLD : Typeface.NORMAL));
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

    private List<SearchResult> vaultResults(String needle) {
        List<SearchResult> results = new ArrayList<>();
        if (needle.isEmpty() || Config.isVaultHiddenFromHome(this)
                || !VaultSession.get().isUnlocked()
                || !VaultFormat.exists(VaultSession.vaultRoot(this))) {
            return results;
        }
        for (VaultStore.Entry entry : VaultStore.searchAll(this, needle)) {
            int score = TextMatch.score(entry.name, currentSearch);
            if (score == TextMatch.NO_MATCH) continue;
            String docId = entry.docId;
            boolean isDir = entry.isDir;
            results.add(new SearchResult(SearchResult.Kind.VAULT, entry.name,
                    isDir ? "folder" : entry.mimeType, score, () -> {
                Intent intent = new Intent(this, VaultActivity.class);
                intent.putExtra("startDocId", isDir ? docId
                        : docId.substring(0, docId.lastIndexOf('/')));
                startActivity(intent);
            }));
        }
        return results;
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

        // A denied prompt no longer disables the toggle — the "Tap to allow"
        // row stays so search keeps offering contacts / files.
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
            if (currentTipEntry != null && currentTipEntry.kind == Tips.Kind.WARNING) {
                startActivity(new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS));
                return;
            }
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

    private Tips.Entry currentTipEntry;

    private void refreshTipRow() {
        if (tipRow == null) return;
        Tips.Entry entry = currentQuery.isEmpty() ? Tips.current(this) : null;
        currentTipEntry = entry;
        if (entry == null) {
            tipRow.setVisibility(View.GONE);
            return;
        }
        boolean warn = entry.kind == Tips.Kind.WARNING;
        tipKicker.setTextColor(Color.parseColor(warn ? "#B05A50" : "#5C5C5C"));
        tipBody.setTextColor(Color.parseColor(warn ? "#C88F87" : "#9A9A9A"));
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

