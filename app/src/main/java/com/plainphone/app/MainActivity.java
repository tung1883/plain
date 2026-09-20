package com.plainphone.app;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainActivity extends Activity implements SelectionHost {

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
    private View homeFocusSink;
    private boolean searchImeVisible;

    /** Swipe-away header: 0 = all shown, 1 = menu section hidden, 2 = search hidden too. */
    private LinearLayout headerZone;
    private int collapseStage = 0;            // stage currently applied to the view
    private int keepStage = 0;                // stage the user last chose; restored after search / selection / stats force it open
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
    private View chessPanel;
    private ChessBoardView chessBoard;
    // Everything above the moves grid (status, engine lines, transport) — its own measured
    // height is what resizeChessBoardIfNeeded subtracts to size the board; the moves grid
    // itself scrolls independently below it now, so its height must never factor in.
    private LinearLayout chessFixedRows;
    private TextView chessTurnLine;
    private TextView[] chessEngineLines;
    private MovesGrid chessMovesGrid;
    private String chessSelectedBoard = "Grey";
    private String chessSelectedPieces = "alpha";
    // Manual override on top of resizeChessBoardIfNeeded's auto-fit width — 1f means "no
    // override, defer to auto-fit". Set by the corner drag handle / pinch, persisted in
    // Config so the size sticks across restarts.
    private float chessManualScale = 1f;
    private boolean chessAutoResize;
    private android.widget.ImageView chessResizeIcon;
    // The library record for whatever game is currently on the board, if it came from (or
    // has since been matched into) the library — null for a fresh/blank board or a game
    // loaded straight from a raw PGN file that hasn't been imported yet. Drives the
    // metadata block and the "Game N of M" PGN stepper; both quietly hide when this is null.
    private ChessLibrary.Entry chessCurrentEntry;
    private final Handler chessAutosaveHandler = new Handler(Looper.getMainLooper());
    private Runnable chessAutosaveTask;
    private TextView chessMetaPlayers, chessMetaEvent, chessMetaOpening, chessMetaResult;
    private TextView chessPgnStepper, chessPgnSourceChip;
    private LinearLayout chessMetaBlock, chessPgnRow;
    // The 3-tab restructure: Board / Puzzles / Library share one home section, swapped by
    // visibility (never a separate Activity transition) inside chessTabContainer.
    private View chessBoardTabContent;
    private ChessPuzzlesPanel chessPuzzlesPanel;
    private ChessLibraryPanel chessLibraryPanel;
    private FrameLayout chessTabContainer;
    private TextView[] chessTabButtons;
    private int chessActiveTab;

    private static final int REQUEST_NOTES_UNLOCK = 4301;
    private static final int REQUEST_PICK_NOTES_FOLDER = 4302;
    private static final int REQUEST_PICK_TODO_FILE = 4303;
    private static final int REQUEST_TODOS_UNLOCK = 4304;
    private static final int REQUEST_APPS_UNLOCK = 4305;
    private static final int REQUEST_SEARCH_UNLOCK = 4306;
    private static final int REQUEST_RECORDER_UNLOCK = 4307;
    private static final int REQUEST_IMPORT_NOTES = 4308;
    private static final int REQUEST_IMPORT_TODOS = 4309;
    private static final int REQUEST_IMPORT_RECORDINGS = 4310;
    private static final int REQUEST_VAULT_UNLOCK = 4311;
    private static final int REQUEST_DEV_UNLOCK = 4312;
    private static final int REQUEST_CHESS_IMPORT = 4313;
    private static final int REQUEST_CHESS_EXPORT = 4314;
    private static final int REQUEST_CHESS_LIBRARY = 4315;
    private static final int REQUEST_CHESS_EXPORT_SOURCE = 4316;
    private static final int REQUEST_CHESS_EXPORT_SELECTED = 4317;
    /** Which PGN source {@link #REQUEST_CHESS_EXPORT_SOURCE}'s file picker result is for —
     *  set right before {@code startActivityForResult}, read back in
     *  {@link #handleChessExportSource}. */
    private String chessExportSourceLabel;
    /** Same idea as {@link #chessExportSourceLabel}, for {@link #REQUEST_CHESS_EXPORT_SELECTED}
     *  (the Library tab's multi-select "Export"), read back in
     *  {@link #handleChessExportSelected}. */
    private java.util.Set<String> chessExportSelectedIds;
    /** Deferred action to run once the vault is unlocked (move-to-vault). */
    private Runnable afterVaultUnlock;
    private FrameLayout artFrame;
    private boolean showingHomeReminder = false;
    private boolean homeUiBuilt = false;
    // Home-list settings groups — collapsed on every entry, never persisted.
    private boolean notesSettingsOpen, todoSettingsOpen, vaultSettingsOpen, recorderSettingsOpen;

    /** Shared multi-select: null unless a section is in selection mode. */
    private HomeMode selectMode;
    private final java.util.LinkedHashSet<String> selection = new java.util.LinkedHashSet<>();
    private SelectionBar selectionBar;
    private FontChoice builtWithFont;

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        // Coming back from another app via the Home button: if a content screen
        // (To-do, Notes, a shell…) was open when Plain went to the background,
        // reopen it instead of dropping to the bare home grid.
        if (Nav.consumeRestore()) {
            startActivity(Nav.restoreIntent());
            return;
        }

        // Default-launcher Home button re-delivers ACTION_MAIN here. If the home UI
        // is already up, pressing Home must NOT rebuild it (that reloads the whole
        // screen and drops the swipe-away header stage). Just return to a clean
        // base view: exit selection, clear search — the header stage is kept.
        if (!homeUiBuilt) {
            recreate();
            return;
        }
        if (selectMode != null) exitSelection();
        clearSearchAndFocus();
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
        } else if (requestCode == REQUEST_DEV_UNLOCK && resultCode == RESULT_OK) {
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
        } else if (requestCode == REQUEST_IMPORT_NOTES && resultCode == RESULT_OK) {
            startImportJob(HomeMode.NOTES, urisFrom(data));
        } else if (requestCode == REQUEST_IMPORT_TODOS && resultCode == RESULT_OK) {
            startImportJob(HomeMode.TODOS, urisFrom(data));
        } else if (requestCode == REQUEST_IMPORT_RECORDINGS && resultCode == RESULT_OK) {
            startImportJob(HomeMode.RECORDER, urisFrom(data));
        } else if (requestCode == REQUEST_VAULT_UNLOCK) {
            Runnable action = afterVaultUnlock;
            afterVaultUnlock = null;
            if (resultCode == RESULT_OK && action != null && VaultSession.get().isUnlocked()) {
                action.run();
            }
        } else if (requestCode == REQUEST_CHESS_IMPORT) {
            handleChessImport(resultCode, data);
        } else if (requestCode == REQUEST_CHESS_EXPORT) {
            handleChessExport(resultCode, data);
        } else if (requestCode == REQUEST_CHESS_LIBRARY) {
            handleChessLibraryPick(resultCode, data);
        } else if (requestCode == REQUEST_CHESS_EXPORT_SOURCE) {
            handleChessExportSource(resultCode, data);
        } else if (requestCode == REQUEST_CHESS_EXPORT_SELECTED) {
            handleChessExportSelected(resultCode, data);
        }
    }

    /** Unlock the vault without leaving this screen, then run {@code action}. */
    private void unlockVaultThen(Runnable action) {
        afterVaultUnlock = action;
        startActivityForResult(new Intent(this, VaultActivity.class)
                .putExtra(VaultActivity.EXTRA_UNLOCK_ONLY, true), REQUEST_VAULT_UNLOCK);
    }

    /** Hand a picked file set to the global {@link JobService} and show its progress row. */
    private void startImportJob(HomeMode plugin, List<Uri> uris) {
        if (uris.isEmpty()) return;
        for (Uri u : uris) {
            try {
                getContentResolver().takePersistableUriPermission(
                        u, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (SecurityException ignored) {
            }
        }
        int n = uris.size();
        ImportJobs.start(this, plugin, uris, n + (n == 1 ? " file" : " files"));
        if (homeUiBuilt) filter(search.getText().toString());
    }

    private void toast(String message) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show();
    }

    private SearchResult inertRow(SearchResult.Kind kind, String title) {
        return new SearchResult(kind, title, null, -1, () -> {});
    }

    // --- SectionHost (shared with workspace PluginPanels) -------------

    @Override public Activity activity() { return this; }

    @Override public void refresh() { filter(search.getText().toString()); }

    @Override public boolean settingsOpen(HomeMode section) {
        switch (section) {
            case NOTES: return notesSettingsOpen;
            case TODOS: return todoSettingsOpen;
            case RECORDER: return recorderSettingsOpen;
            default: return false;
        }
    }

    @Override public void setSettingsOpen(HomeMode section, boolean open) {
        switch (section) {
            case NOTES: notesSettingsOpen = open; break;
            case TODOS: todoSettingsOpen = open; break;
            case RECORDER: recorderSettingsOpen = open; break;
            default: break;
        }
    }

    @Override public void pickImport(HomeMode section, String mimeType) {
        int code = section == HomeMode.NOTES ? REQUEST_IMPORT_NOTES
                : section == HomeMode.TODOS ? REQUEST_IMPORT_TODOS
                : REQUEST_IMPORT_RECORDINGS;
        pickImport(mimeType, code);
    }

    @Override public void pickTodoFile() {
        Todos.showFileOptions(this, REQUEST_PICK_TODO_FILE, this::refresh);
    }

    @Override public void pickNotesFolder() {
        Notes.showFolderOptions(this, REQUEST_PICK_NOTES_FOLDER, this::refresh);
    }

    @Override public void unlockVault() { unlockVaultThen(this::refresh); }

    /** Open a multi-select document picker for the given MIME type. */
    private void pickImport(String mime, int requestCode) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(mime);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        try {
            startActivityForResult(intent, requestCode);
        } catch (android.content.ActivityNotFoundException e) {
            toast("No file picker available");
        }
    }

    private static List<Uri> urisFrom(Intent data) {
        List<Uri> out = new ArrayList<>();
        if (data == null) return out;
        android.content.ClipData clip = data.getClipData();
        if (clip != null) {
            for (int i = 0; i < clip.getItemCount(); i++) {
                Uri u = clip.getItemAt(i).getUri();
                if (u != null) out.add(u);
            }
        } else if (data.getData() != null) {
            out.add(data.getData());
        }
        return out;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        IndexScheduler.schedule(this);
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
        ImportJobs.resumeIfPending(this);
        ImportJobs.addListener(importJobListener);
        importJobListener.onImportJobChanged();   // pick up a result from while we were away
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
            if (search.getText().length() == 0 && !searchImeVisible) releaseSearchFocus(false);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        searchImeVisible = false;
        tipHandler.removeCallbacks(tipRotate);
        artHandler.removeCallbacks(artRotate);
        VaultJobs.removeListener(vaultJobListener);
        ImportJobs.removeListener(importJobListener);
    }

    private final VaultJobs.Listener vaultJobListener = () -> runOnUiThread(() -> {
        if (isFinishing() || isDestroyed() || !homeUiBuilt) return;
        VaultJobs.Result done = VaultJobs.takeResult();
        if (done != null && done.message != null) {
            Toast.makeText(this, done.message, done.ok ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG).show();
        }
        filter(search.getText().toString());
    });

    private final ImportJobs.Listener importJobListener = () -> runOnUiThread(() -> {
        if (isFinishing() || isDestroyed() || !homeUiBuilt) return;
        ImportJobs.Result done = ImportJobs.takeResult();
        if (done != null) {
            int n = done.added;
            String noun = done.plugin == HomeMode.NOTES ? "note"
                    : done.plugin == HomeMode.TODOS ? "task" : "recording";
            toast(n == 0 ? "Nothing imported"
                    : "Imported " + n + " " + noun + (n == 1 ? "" : "s"));
        }
        SectionJobs.Result sectionDone = SectionJobs.takeResult();
        if (sectionDone != null && sectionDone.message != null) {
            Toast.makeText(this, sectionDone.message, Toast.LENGTH_SHORT).show();
        }
        filter(search.getText().toString());
    });

    @Override
    public void onBackPressed() {
        if (selectMode != null) {
            exitSelection();
            return;
        }
        if (search.getText().length() > 0) {
            clearSearchAndFocus();
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
        message.setSingleLine(true);
        message.setText("Set Plain as your Home app");
        root.addView(message);

        Button openHomeSettings = new Button(this);
        openHomeSettings.setText("Open Settings");
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

        int spSize = Math.round(28 * getResources().getDisplayMetrics().density);
        root.addView(UiKit.spinner(this), new LinearLayout.LayoutParams(spSize, spSize));

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
        root.setFocusableInTouchMode(true);
        homeFocusSink = root;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            root.setOnApplyWindowInsetsListener((v, insets) -> {
                searchImeVisible = insets.isVisible(WindowInsets.Type.ime());
                if (!searchImeVisible && search != null && search.hasFocus()) {
                    releaseSearchFocus(false);
                }
                return insets;
            });
        }

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
                    clearSearchTextOnly();
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

        artFrame.setForeground(UiKit.frameBorder(this));
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

        selectionBar = new SelectionBar(this);
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
        chessPanel = buildChessPanel();
        chessPanel.setVisibility(View.GONE);

        FrameLayout swipeContent = new FrameLayout(this);
        swipeContent.addView(listView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        swipeContent.addView(statsPanel.view(), new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        swipeContent.addView(chessPanel, new FrameLayout.LayoutParams(
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
                    clearSearchAndFocus();
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
                if (selectMode == HomeMode.NOTES) toggle(n.id);
                else enterSelection(HomeMode.NOTES, n.id);
                return true;
            }
            if (result.payload instanceof Recording) {
                Recording rec = (Recording) result.payload;
                if (selectMode == HomeMode.RECORDER) toggle(rec.id);
                else enterSelection(HomeMode.RECORDER, rec.id);
                return true;
            }
            if (result.payload instanceof Todos.Item && currentQuery.isEmpty()) {
                Todos.Item item = (Todos.Item) result.payload;
                String tid = "todo:" + item.index;
                if (selectMode == HomeMode.TODOS) toggle(tid);
                else enterSelection(HomeMode.TODOS, tid);
                return true;
            }
            if (result.payload instanceof Workspaces.Meta && currentQuery.isEmpty()) {
                String wid = ((Workspaces.Meta) result.payload).id;
                if (selectMode == HomeMode.WORKSPACE) toggle(wid);
                else enterSelection(HomeMode.WORKSPACE, wid);
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

    private void clearSearchTextOnly() {
        if (search == null) return;
        if (search.getText().length() > 0) {
            search.setText("");
        } else {
            filter("");
        }
    }

    private void clearSearchAndFocus() {
        if (search == null) return;
        clearSearchTextOnly();
        releaseSearchFocus(true);
    }

    private void releaseSearchFocus(boolean hideKeyboard) {
        if (search == null) return;
        search.clearFocus();
        View target = homeFocusSink != null ? homeFocusSink : getWindow().getDecorView();
        if (target != null) target.requestFocus();
        if (!hideKeyboard) return;
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(search.getWindowToken(), 0);
        }
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
        boolean chessShown = chessPanel != null && chessPanel.getVisibility() == View.VISIBLE;
        return homeUiBuilt && currentQuery.isEmpty() && selectMode == null
                && (listView.getVisibility() == View.VISIBLE || statsPanelShown || chessShown);
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
        resizeChessBoardIfNeeded();
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
        keepStage = target;               // deliberate choice — survives search / selection
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

    /** Keep the header open while it can't collapse (search / selection / stats);
     *  otherwise return it to the stage the user last chose. Called after render. */
    private void syncHeaderCollapse() {
        if (!collapseEligible()) {
            if (collapseStage != 0) setCollapseStage(0);
        } else if (collapseStage != keepStage) {
            // Snap, don't animate: while search was open the header was at stage 0
            // (menu section shown). Animating back from there flashes the full menu
            // for one frame before it folds away.
            setCollapseStage(keepStage);
        }
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
                    Config.isPinSet(this) ? "Locked" : "Locked — set a master PIN to take effect",
                    android.widget.Toast.LENGTH_SHORT).show();
        });
    }

    // Fixed height for the moves grid's own nested ScrollView — content's height must be
    // WRAP_CONTENT to sit inside the outer panel ScrollView, so the grid can't use
    // weight/0dp to flexibly fill leftover space the way it did without an outer scroll.
    private static final int CHESS_MOVES_GRID_HEIGHT_DP = 140;

    /** Plain wrapper around {@link #chessBoard} — resizing no longer happens by touching the
     *  live board at all; it's entirely inside {@link #chessShowResizeDialog}'s own preview,
     *  which pinches independently and only pushes the result into {@link #chessManualScale}
     *  once that dialog closes. */
    private View buildChessBoardWrap() {
        FrameLayout wrap = new FrameLayout(this);
        wrap.addView(chessBoard, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL));
        return wrap;
    }

    /** Players / event+round / opening, plus a result pill — from {@link #chessCurrentEntry}.
     *  Whole block hides when there's no known library record for the game on screen (a
     *  fresh board, or a game loaded straight from a raw PGN that was never imported).
     *  Any single line (opening especially — most PGNs don't carry an ECO/Opening tag)
     *  hides on its own when that field is blank, rather than showing an empty row. */
    private View buildChessMetaBlock() {
        chessMetaBlock = new LinearLayout(this);
        chessMetaBlock.setOrientation(LinearLayout.HORIZONTAL);
        chessMetaBlock.setGravity(Gravity.TOP);
        chessMetaBlock.setPadding(48, UiKit.dp(this, 12), 48, 0);

        LinearLayout lines = new LinearLayout(this);
        lines.setOrientation(LinearLayout.VERTICAL);
        chessMetaPlayers = chessText("", 15, Color.WHITE);
        chessMetaPlayers.setTypeface(Fonts.current(this), android.graphics.Typeface.BOLD);
        chessMetaEvent = chessText("", 12, Color.GRAY);
        chessMetaOpening = chessText("", 12, Color.GRAY);
        lines.addView(chessMetaPlayers);
        lines.addView(chessMetaEvent);
        lines.addView(chessMetaOpening);
        chessMetaBlock.addView(lines, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        chessMetaResult = chessText("", 12, Color.WHITE);
        chessMetaResult.setBackground(UiKit.rounded(this, Color.BLACK, 0xFF333333, 2f, UiKit.R_SM));
        chessMetaResult.setPadding(UiKit.dp(this, 10), UiKit.dp(this, 3), UiKit.dp(this, 10), UiKit.dp(this, 3));
        chessMetaBlock.addView(chessMetaResult);
        return chessMetaBlock;
    }

    /** "‹ Game N of M ›" (steps through the loaded PGN's own games) plus the source
     *  filename, tappable to browse that PGN's games. Hidden along with the meta block
     *  when there's no known source. */
    private View buildChessPgnRow() {
        chessPgnRow = new LinearLayout(this);
        chessPgnRow.setOrientation(LinearLayout.HORIZONTAL);
        chessPgnRow.setGravity(Gravity.CENTER_VERTICAL);
        // Bottom padding here (not a margin on the board wrap below) so the extra gap only
        // shows up when this row — and the meta block above it — are actually visible, i.e.
        // a game loaded from the library; a fresh/unloaded board stays as tight as before.
        chessPgnRow.setPadding(48, UiKit.dp(this, 10), 48, UiKit.dp(this, 18));

        chessPgnStepper = chessText("", 12, Color.WHITE);
        chessPgnRow.addView(chessPgnStepper, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        chessPgnSourceChip = chessText("", 12, Color.WHITE);
        chessPgnSourceChip.setBackground(UiKit.pressable(this, Color.BLACK, Color.DKGRAY, 0xFF262626, 2f, UiKit.R_SM));
        chessPgnSourceChip.setPadding(UiKit.dp(this, 10), UiKit.dp(this, 4), UiKit.dp(this, 10), UiKit.dp(this, 4));
        chessPgnSourceChip.setOnClickListener(v -> {
            if (chessCurrentEntry == null) return;
            Intent i = new Intent(this, ChessLibraryActivity.class);
            i.putExtra(ChessLibraryActivity.EXTRA_SOURCE_FILTER, chessCurrentEntry.src);
            i.putExtra(ChessLibraryActivity.EXTRA_CURRENT_ID, chessCurrentEntry.id);
            startActivityForResult(i, REQUEST_CHESS_LIBRARY);
        });
        chessPgnRow.addView(chessPgnSourceChip);
        return chessPgnRow;
    }

    /** Refreshes the meta block + PGN stepper from {@link #chessCurrentEntry} — called
     *  whenever a game is (re)loaded onto the board. Cheap enough to just re-run in full
     *  rather than diffing what changed. */
    /** Bumped every call — a background stepper computation (below) that finishes after a
     *  newer one started (rapid game-to-game taps) recognizes itself as stale and drops its
     *  result instead of overwriting the stepper with a wrong game's numbers. */
    private int chessMetaStepperGeneration;

    private void updateChessMetaUi() {
        if (chessMetaBlock == null) return;
        ChessLibrary.Entry e = chessCurrentEntry;
        boolean known = e != null;
        chessMetaBlock.setVisibility(known ? View.VISIBLE : View.GONE);
        chessPgnRow.setVisibility(known ? View.VISIBLE : View.GONE);
        if (!known) return;

        chessMetaPlayers.setText(ChessBoardView.shortName(e.white) + " vs " + ChessBoardView.shortName(e.black));
        String eventLine = e.event + (e.round.isEmpty() ? "" : " · Round " + e.round);
        chessMetaEvent.setText(eventLine);
        chessMetaEvent.setVisibility(eventLine.isEmpty() ? View.GONE : View.VISIBLE);
        chessMetaOpening.setText(e.eco);
        chessMetaOpening.setVisibility(e.eco.isEmpty() ? View.GONE : View.VISIBLE);
        chessMetaResult.setText(e.result);
        chessPgnSourceChip.setText(e.src + " ›");

        // "Game N of M" needs every sibling from the same source (ChessLibrary.loadBySource,
        // an O(library size) filter) just to find this one entry's position — on the UI
        // thread, that's a multi-second block on every single game load once the library
        // has thousands of games. Off-thread instead; the stepper just appears a moment
        // after everything else, rather than the board/toast waiting on it too.
        chessPgnStepper.setText("");
        int gen = ++chessMetaStepperGeneration;
        new Thread(() -> {
            List<ChessLibrary.Entry> siblings = ChessLibrary.loadBySource(this, e.src);
            int index = -1;
            for (int i = 0; i < siblings.size(); i++) if (siblings.get(i).id.equals(e.id)) { index = i; break; }
            // siblings is newest-first; show the PGN's own file order (oldest-first) to match
            // how a human reading the source file would count games in it.
            int total = siblings.size();
            int posFromEnd = index < 0 ? 0 : total - index;
            String stepperText = index < 0 ? "" : "‹ Game " + posFromEnd + " of " + total + " ›";
            runOnUiThread(() -> {
                if (gen != chessMetaStepperGeneration) return;
                chessPgnStepper.setText(stepperText);
            });
        }).start();
    }

    private static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    /** The whole Chess home section: a Board/Puzzles/Library segmented strip (same visual
     *  pattern as {@link StatsPanel#addToggle}'s Today/Week/Month toggle) over a
     *  {@link FrameLayout} holding all three tabs' content, swapped by visibility — never a
     *  separate Activity transition, and all three built once so switching tabs is instant
     *  and doesn't lose whatever state (an in-progress puzzle, a Library search) a tab had. */
    private View buildChessPanel() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);

        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.setPadding(UiKit.dp(this, 18), UiKit.dp(this, 14), UiKit.dp(this, 18), 0);
        String[] labels = {"Board", "Puzzles", "Library"};
        chessTabButtons = new TextView[labels.length];
        for (int i = 0; i < labels.length; i++) {
            final int idx = i;
            TextView tab = chessText(labels[i], 13, Color.WHITE);
            tab.setGravity(Gravity.CENTER);
            tab.setPadding(0, UiKit.dp(this, 12), 0, UiKit.dp(this, 12));
            tab.setOnClickListener(v -> chessSelectTab(idx));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            if (i > 0) lp.leftMargin = UiKit.dp(this, 8);
            tabs.addView(tab, lp);
            chessTabButtons[i] = tab;
        }
        root.addView(tabs);

        chessTabContainer = new FrameLayout(this);
        chessTabContainer.setPadding(0, UiKit.dp(this, 14), 0, 0);
        chessBoardTabContent = buildChessBoardTabContent();
        chessPuzzlesPanel = new ChessPuzzlesPanel(this, new ChessPuzzlesPanel.Listener() {
            @Override public void onOpenPuzzleList() { startActivity(new Intent(MainActivity.this, ChessPuzzleListActivity.class)); }
            @Override public void onResizeRequested() { chessShowResizeDialog(); }
            @Override public void onOpenSettings() { chessOpenSettings(); }
        });
        chessLibraryPanel = new ChessLibraryPanel(this, new ChessLibraryPanel.Listener() {
            @Override public void onGameChosen(ChessLibrary.Entry entry) { chessLoadEntryOntoBoard(entry); }
            @Override public void onOpenPgnFiles() { startActivity(new Intent(MainActivity.this, ChessPgnFilesActivity.class)); }
            @Override public void onImportPgn() { chessImportPgn(); }
            @Override public void onExportPgn() { chessExportPgn(); }
            @Override public void onExportSource(String source) { chessExportSource(source); }
            @Override public void onExportSelected(java.util.Set<String> ids) { chessExportSelected(ids); }
        });
        chessTabContainer.addView(chessBoardTabContent, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        chessTabContainer.addView(chessPuzzlesPanel.view(), new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        chessTabContainer.addView(chessLibraryPanel.view(), new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.addView(chessTabContainer, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        chessActiveTab = 0;
        chessRestyleTabs();
        chessPuzzlesPanel.view().setVisibility(View.GONE);
        chessLibraryPanel.view().setVisibility(View.GONE);
        return root;
    }

    private void chessSelectTab(int index) {
        if (index == chessActiveTab) return;
        chessActiveTab = index;
        chessBoardTabContent.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        chessPuzzlesPanel.view().setVisibility(index == 1 ? View.VISIBLE : View.GONE);
        chessLibraryPanel.view().setVisibility(index == 2 ? View.VISIBLE : View.GONE);
        chessRestyleTabs();
        if (index == 1) chessPuzzlesPanel.onShown();
        if (index == 2) chessLibraryPanel.refresh();
    }

    private void chessRestyleTabs() {
        for (int i = 0; i < chessTabButtons.length; i++) {
            boolean on = i == chessActiveTab;
            chessTabButtons[i].setBackground(on
                    ? UiKit.rounded(this, Color.WHITE, 0, 0f, UiKit.R_SM)
                    : UiKit.rounded(this, Color.BLACK, 0xFF2C2C2C, 2f, UiKit.R_SM));
            chessTabButtons[i].setTextColor(on ? Color.BLACK : Color.WHITE);
        }
    }

    /** Loads {@code entry} onto the Board tab's board and switches to it — the Library tab's
     *  row-tap path; {@link #handleChessLibraryPick} is the equivalent for the Intent-based
     *  "Games in a PGN" picker (still a separate {@link ChessLibraryActivity} screen). */
    private void chessLoadEntryOntoBoard(ChessLibrary.Entry entry) {
        chessCurrentEntry = entry;
        updateChessMetaUi();
        // Async — a long game's replay is real synchronous chess-move-generation work, and
        // running it inline here (this runs on the UI thread) used to freeze all touch input
        // for a second or more right after the tab switch below. The board fills in a moment
        // later instead of blocking the switch itself.
        chessBoard.loadSanMovesAsync(entry.sans(), entry.commentsForSans(), entry.result, null);
        chessSelectTab(0);
        toast("Loaded " + entry.white + " vs " + entry.black);
    }

    /** The "Board" tab's whole content — unchanged from before the 3-tab restructure (see
     *  {@link #buildChessPanel()}), just renamed since it's no longer the entire Chess home
     *  section on its own. */
    private View buildChessBoardTabContent() {
        ScrollView panel = new ScrollView(this);
        panel.setFillViewport(true);
        panel.setBackgroundColor(Color.BLACK);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER_HORIZONTAL);
        content.setPadding(0, UiKit.dp(this, 8), 0, 0);
        panel.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        chessManualScale = Config.getChessBoardScale(this);
        chessAutoResize = Config.getChessAutoResize(this);

        chessBoard = new ChessBoardView(this, this::updateChessHomeUi, this::updateChessEngineLinesOnly);
        chessBoard.setPieceTheme(chessSelectedPieces);
        chessBoard.setBoardTheme("grey");
        UiKit.clipRounded(this, chessBoard, UiKit.R_SM);

        content.addView(buildChessMetaBlock(), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        content.addView(buildChessPgnRow(), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        content.addView(buildChessBoardWrap(), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        chessFixedRows = new LinearLayout(this);
        chessFixedRows.setOrientation(LinearLayout.VERTICAL);
        content.addView(chessFixedRows, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout status = new LinearLayout(this);
        status.setGravity(Gravity.CENTER_VERTICAL);
        status.setPadding(48, UiKit.dp(this, 16), 48, UiKit.dp(this, 12));
        chessTurnLine = chessText("White to move", 17, Color.WHITE);
        status.addView(chessTurnLine, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        // 24dp — matches both icons' own 24x24 viewport exactly, so the drawn glyph fills the
        // box instead of leaving the extra centering slack a bigger box (36dp) would add on
        // top of the icon's own edge; that slack was why the gear used to sit visibly inset
        // from the transport row's "›|" (which corrects for glyph ink-bearing on its own).
        int iconBoxDp = 18;

        android.widget.ImageView chessFlipIcon = new android.widget.ImageView(this);
        chessFlipIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_chess_flip, getTheme()));
        chessFlipIcon.setScaleType(android.widget.ImageView.ScaleType.CENTER_INSIDE);
        chessFlipIcon.setContentDescription("Flip board");
        chessFlipIcon.setOnClickListener(v -> chessBoard.toggleFlip());
        LinearLayout.LayoutParams flipIconLp = new LinearLayout.LayoutParams(UiKit.dp(this, iconBoxDp), UiKit.dp(this, iconBoxDp));
        flipIconLp.rightMargin = UiKit.dp(this, 14);
        status.addView(chessFlipIcon, flipIconLp);

        chessResizeIcon = new android.widget.ImageView(this);
        chessResizeIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_chess_resize, getTheme()));
        chessResizeIcon.setScaleType(android.widget.ImageView.ScaleType.CENTER_INSIDE);
        chessResizeIcon.setContentDescription("Resize board");
        chessResizeIcon.setOnClickListener(v -> chessShowResizeDialog());
        LinearLayout.LayoutParams resizeIconLp = new LinearLayout.LayoutParams(UiKit.dp(this, iconBoxDp), UiKit.dp(this, iconBoxDp));
        resizeIconLp.rightMargin = UiKit.dp(this, 14);
        status.addView(chessResizeIcon, resizeIconLp);

        android.widget.ImageView chessSaveIcon = new android.widget.ImageView(this);
        chessSaveIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_chess_save, getTheme()));
        chessSaveIcon.setScaleType(android.widget.ImageView.ScaleType.CENTER_INSIDE);
        chessSaveIcon.setContentDescription("Save game to library");
        chessSaveIcon.setOnClickListener(v -> chessShowSaveSheet());
        LinearLayout.LayoutParams saveIconLp = new LinearLayout.LayoutParams(UiKit.dp(this, iconBoxDp), UiKit.dp(this, iconBoxDp));
        saveIconLp.rightMargin = UiKit.dp(this, 14);
        status.addView(chessSaveIcon, saveIconLp);

        android.widget.ImageView chessNewBoardIcon = new android.widget.ImageView(this);
        chessNewBoardIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_chess_new_board, getTheme()));
        chessNewBoardIcon.setScaleType(android.widget.ImageView.ScaleType.CENTER_INSIDE);
        chessNewBoardIcon.setContentDescription("New board");
        chessNewBoardIcon.setOnClickListener(v -> VaultUi.confirm(this, "Go to a new board?", null,
                "Yes", () -> { chessCurrentEntry = null; chessBoard.resetToStartPosition(); updateChessMetaUi(); },
                "Cancel", () -> { }));
        LinearLayout.LayoutParams newBoardIconLp = new LinearLayout.LayoutParams(UiKit.dp(this, iconBoxDp), UiKit.dp(this, iconBoxDp));
        newBoardIconLp.rightMargin = UiKit.dp(this, 14);
        status.addView(chessNewBoardIcon, newBoardIconLp);

        android.widget.ImageView chessSettingsIcon = new android.widget.ImageView(this);
        chessSettingsIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_chess_settings, getTheme()));
        chessSettingsIcon.setScaleType(android.widget.ImageView.ScaleType.CENTER_INSIDE);
        chessSettingsIcon.setContentDescription("Chess settings");
        chessSettingsIcon.setOnClickListener(v -> chessOpenSettings());
        status.addView(chessSettingsIcon, new LinearLayout.LayoutParams(UiKit.dp(this, iconBoxDp), UiKit.dp(this, iconBoxDp)));
        chessFixedRows.addView(status);

        chessEngineLines = new TextView[5]; // pool sized for the max "Variations shown" setting
        for (int i = 0; i < chessEngineLines.length; i++) {
            TextView line = chessText("", 13, 0xFF8FBF8F);
            line.setPadding(48, 0, 48, 0);
            line.setSingleLine(true);
            line.setEllipsize(android.text.TextUtils.TruncateAt.END);
            line.setVisibility(View.GONE);
            chessEngineLines[i] = line;
            chessFixedRows.addView(line);
        }
        // Fixed gap below the analysis block, regardless of how many lines are actually
        // visible — padding tied to a specific pool slot broke once "Variations shown"
        // could be less than the pool size.
        View chessEngineGap = new View(this);
        chessFixedRows.addView(chessEngineGap, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, UiKit.dp(this, 10)));

        chessFixedRows.addView(chessRule(), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));

        LinearLayout transport = new LinearLayout(this);
        transport.setGravity(Gravity.CENTER);
        transport.setPadding(48, UiKit.dp(this, 12), 48, UiKit.dp(this, 12));
        String[] labels = {"|‹", "‹", "›", "›|"};
        for (int i = 0; i < labels.length; i++) {
            final int action = i;
            TextView button = chessText(labels[i], 18, Color.WHITE);
            // First/last icons line up with the moves grid's own left/right margin;
            // the two middle ones stay centered in their share of the row.
            if (i == 0) {
                button.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
                button.setTranslationX(-chessInkLeadIn(labels[i], 18f));
            } else if (i == labels.length - 1) {
                button.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
                button.setTranslationX(chessInkTrailOut(labels[i], 18f));
            } else {
                button.setGravity(Gravity.CENTER);
            }
            button.setOnClickListener(v -> {
                if (action == 0) chessBoard.first();
                else if (action == 1) chessBoard.previous();
                else if (action == 2) chessBoard.next();
                else chessBoard.last();
            });
            transport.addView(button, new LinearLayout.LayoutParams(0, UiKit.dp(this, 34), 1f));
        }
        chessFixedRows.addView(transport);
        chessFixedRows.addView(chessRule(), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));

        chessMovesGrid = new MovesGrid(this, chessBoard, this::updateChessHomeUi);
        chessMovesGrid.setPadding(48, UiKit.dp(this, 12), 48, UiKit.dp(this, 12));
        // A plain ScrollView nested in another one doesn't claim drags on its own — and
        // setOnTouchListener doesn't fix it: the move cells are clickable, so they consume
        // ACTION_DOWN before it ever reaches this ScrollView's own touch handling, and the
        // listener never fires. onInterceptTouchEvent is the one hook the framework calls
        // on every ancestor for every event regardless of what a child does with it, so
        // that's where the claim has to happen — on the outer panel specifically (not on
        // this ScrollView itself, which needs its normal drag-to-scroll intact).
        ScrollView movesScroll = new ScrollView(this) {
            @Override
            public boolean onInterceptTouchEvent(android.view.MotionEvent ev) {
                if (ev.getActionMasked() == android.view.MotionEvent.ACTION_DOWN && getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                return super.onInterceptTouchEvent(ev);
            }
        };
        movesScroll.addView(chessMovesGrid, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        content.addView(movesScroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, UiKit.dp(this, CHESS_MOVES_GRID_HEIGHT_DP)));
        content.addView(chessRule(), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));

        // On first opening the panel, resizeChessBoardIfNeeded's very first attempt (via
        // syncHeaderCollapse right after this returns) almost always fires before
        // chessFixedRows has actually been measured (getHeight() still 0), so it silently
        // no-ops and the board is left at its full-width default — even if the header was
        // already collapsed from a previous visit. Nothing else was re-triggering it after
        // that, so it just stayed big until the user next dragged the header. This keeps
        // retrying on every layout pass instead (cheap and self-limiting: once the width
        // stops changing, resizeChessBoardIfNeeded stops calling setLayoutParams, so no
        // more layout passes fire from it).
        content.getViewTreeObserver().addOnGlobalLayoutListener(this::resizeChessBoardIfNeeded);
        updateChessMetaUi();
        return panel;
    }

    /** Shrinks/grows the square board to whatever's left of the header's own numbers once
     *  the fixed rows above the moves grid (status, engine lines, transport) and the moves
     *  grid's own reserved height are both accounted for. Piggybacks on
     *  {@link #applyHeaderOffset} so it re-runs on every header drag/animation frame. Uses
     *  {@code headerFullH}/{@code headerOffset} directly rather than re-reading the
     *  header's own {@code getHeight()} — that view's layout pass hasn't happened yet this
     *  frame, so reading it back here would always be one frame stale and the board would
     *  visibly lag a step behind the header instead of moving with it. */
    private void resizeChessBoardIfNeeded() {
        if (chessBoard == null || chessFixedRows == null) return;
        if (chessPanel == null || chessPanel.getVisibility() != View.VISIBLE) return;
        if (homeFocusSink == null || homeFocusSink.getHeight() <= 0) return;
        if (chessFixedRows.getHeight() <= 0) return;

        int target;
        if (chessAutoResize) {
            // Two independent sizes, one per header endpoint — the header's own drag
            // position (0 = fully open/"swiped down" .. headerFullH = fully
            // collapsed/"swiped up") interpolates between them, so the board still follows
            // the header smoothly instead of snapping between two fixed sizes.
            int naturalUpPx = chessAutoFitFor(headerFullH);
            int naturalDownPx = chessAutoFitFor(0f);
            // Both endpoints cap against the same fixed ceiling — the swiped-up NATURAL size
            // — never against whatever the other endpoint happens to be pinched to right now.
            int sizeUpPx = chessResolvedSizeUp(naturalUpPx);
            int sizeDownPx = chessResolvedSizeDown(naturalDownPx, naturalUpPx);
            // Two "swiped up" resting points exist — stage 1 (search bar still showing) and
            // stage 2 (search hidden too) — both should read as fully swiped-up for sizing,
            // or stage 1 lands short of the ceiling since its headerOffset is less than
            // headerFullH. Interpolate against stage 1's offset, not the full header height,
            // so the board is already at sizeUpPx by the time stage 1 is reached.
            float stage1Offset = Math.max(0f, headerFullH - headerSearchH);
            float t = stage1Offset <= 0f ? 1f : clamp(headerOffset / stage1Offset, 0f, 1f);
            target = Math.round(sizeDownPx + t * (sizeUpPx - sizeDownPx));
        } else {
            target = chessAutoFitFor(headerOffset);
            // The pinch override is relative to whatever auto-fit just picked for this
            // header state, so dragging bigger/smaller still respects the header-collapsed
            // vs. expanded baseline instead of fighting it.
            if (chessManualScale != 1f) {
                int maxW = getResources().getDisplayMetrics().widthPixels - 96;
                target = Math.round(target * chessManualScale);
                target = Math.max(UiKit.dp(this, 120), Math.min(maxW, target));
            }
        }
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) chessBoard.getLayoutParams();
        if (lp.width != target || lp.gravity != Gravity.CENTER_HORIZONTAL) {
            lp.width = target;
            lp.gravity = Gravity.CENTER_HORIZONTAL;
            chessBoard.setLayoutParams(lp);
        }
    }

    /** The auto-fit board width for a given (possibly simulated, not necessarily the header's
     *  actual current) {@code simulatedHeaderOffset} — used both for the real, continuously
     *  dragged header position and to work out the two fixed endpoints
     *  {@link #chessResolvedSizeUp}/{@link #chessResolvedSizeDown} scale from. */
    private int chessAutoFitFor(float simulatedHeaderOffset) {
        int otherRowsH = chessFixedRows.getHeight() + UiKit.dp(this, CHESS_MOVES_GRID_HEIGHT_DP);
        float headerNow = simulatedHeaderOffset <= 0f ? headerFullH : Math.max(0f, headerFullH - simulatedHeaderOffset);
        int viewportH = homeFocusSink.getHeight() - Math.round(headerNow);
        int available = viewportH - otherRowsH;
        // The fully-open header (menu swiped down) leaves the tightest fit — nudge the
        // board a little larger there; the outer panel ScrollView absorbs the rest.
        if (simulatedHeaderOffset <= 0f) available += UiKit.dp(this, 28);
        // Same side inset every other row in this panel already uses (status, engine lines,
        // transport all pad 48px each side) — the board used to run edge-to-edge instead of
        // matching that margin.
        int maxW = getResources().getDisplayMetrics().widthPixels - 96;
        int target = Math.min(maxW, available);
        return Math.max(UiKit.dp(this, 160), Math.min(maxW, target));
    }

    /** The swiped-up board's own size — a user override (dp) if one's been pinched in the
     *  resize dialog, else its natural auto-fit size — capped at itself (it IS the 100%
     *  ceiling both states are capped at, so this is really just clamping a stale override
     *  after e.g. a rotation changed what "natural" means). */
    private int chessResolvedSizeUp(int naturalUpPx) {
        int dp = Config.getChessSizeUpDp(this);
        int px = dp > 0 ? UiKit.dp(this, dp) : naturalUpPx;
        return Math.max(UiKit.dp(this, 120), Math.min(naturalUpPx, px));
    }

    /** The swiped-down board's own size — a user override (dp) if one's been pinched, else
     *  75% of {@code ceilingPx} (the swiped-up size) — either way capped at that same
     *  ceiling, so it can never grow past it. */
    private int chessResolvedSizeDown(int naturalDownPx, int ceilingPx) {
        int dp = Config.getChessSizeDownDp(this);
        int px = dp > 0 ? UiKit.dp(this, dp) : Math.round(ceilingPx * 0.75f);
        return Math.max(UiKit.dp(this, 120), Math.min(ceilingPx, px));
    }

    /** The resize icon opens this dialog — either a single pinchable preview board
     *  (Auto-resize off, today's {@link #chessManualScale} behavior) or two independent ones,
     *  one per header endpoint (Auto-resize on, {@link #chessResolvedSizeUp}/
     *  {@link #chessResolvedSizeDown}). The "Auto-resize" row and left-aligned "Reset" row
     *  stay put across both; only the board block beneath them is rebuilt when the toggle
     *  flips. Nothing touches the real board until the dialog closes (tap outside — there's
     *  no X), at which point whichever mode's values are current get applied and persisted.
     *  The body sits in a height-capped {@link ScrollView} so a tall two-board layout scrolls
     *  internally instead of pushing the dialog's own rounded bottom edge off-screen. */
    private void chessShowResizeDialog() {
        boolean[] autoResize = {chessAutoResize};
        float[] previewScale = {chessManualScale};

        int naturalUpPx = chessAutoFitFor(headerFullH);
        int naturalDownPx = chessAutoFitFor(0f);
        float[] upPx = {chessResolvedSizeUp(naturalUpPx)};
        float[] downPx = {chessResolvedSizeDown(naturalDownPx, naturalUpPx)};

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(UiKit.dialogBackground(this));
        UiKit.clipRounded(this, root, UiKit.R_MD);
        // Top/bottom padding must be at least the corner radius, or the first/last row's own
        // opaque, square-cornered background paints straight over the rounded corner's curve
        // — reads as the border vanishing right at the top (or bottom) edge.
        int edgeInset = UiKit.dp(this, UiKit.R_MD);
        root.setPadding(2, edgeInset, 2, edgeInset);

        android.widget.FrameLayout scrim = UiKit.wrapScrim(this, root, 0.92f);
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.Theme_PlainPhone_RoundedDialog)
                .setView(scrim).create();
        dialog.setOnDismissListener(d -> {
            chessAutoResize = autoResize[0];
            Config.setChessAutoResize(this, chessAutoResize);
            if (chessAutoResize) {
                Config.setChessSizeUpDp(this, pxToDp(upPx[0]));
                Config.setChessSizeDownDp(this, pxToDp(downPx[0]));
            } else {
                chessManualScale = previewScale[0];
                Config.setChessBoardScale(this, chessManualScale);
            }
            resizeChessBoardIfNeeded();
        });

        Runnable[] renderBody = new Runnable[1];
        TextView toggleRow = chessSettingsRow("", v -> { autoResize[0] = !autoResize[0]; renderBody[0].run(); });
        root.addView(toggleRow);
        TextView resetRow = chessSettingsRow("Reset", v -> {
            if (autoResize[0]) {
                upPx[0] = naturalUpPx;
                downPx[0] = Math.min(naturalUpPx, naturalDownPx * 1.5f);
            } else {
                previewScale[0] = 1f;
            }
            renderBody[0].run();
        });
        root.addView(resetRow);

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        // Auto-resize-on's two true-size boards can easily be taller than the screen, so the
        // body scrolls internally — but a plain ScrollView would also swallow a two-finger
        // pinch that starts on one of the preview boards (it claims any vertical drag past
        // touch slop, single- or multi-finger alike). Bailing out of interception the moment
        // a second pointer shows up hands the whole gesture back to the board's own
        // ScaleGestureDetector, same fix already used for the piece-drag/moves-grid cases
        // elsewhere in this file — only here it's conditional on pointer count instead of
        // unconditional, since single-finger drags on a board must still scroll normally.
        ScrollView scroller = new ScrollView(this) {
            @Override public boolean onInterceptTouchEvent(MotionEvent ev) {
                if (ev.getPointerCount() >= 2) return false;
                return super.onInterceptTouchEvent(ev);
            }
        };
        scroller.addView(body, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        // Capped only in auto-resize mode (renderBody below) — off-mode's single short
        // preview needs no scrolling and would otherwise sit in a tall box with dead space
        // under it.
        int maxBodyPx = Math.round(getResources().getDisplayMetrics().heightPixels * 0.55f);
        root.addView(scroller, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        renderBody[0] = () -> {
            toggleRow.setText("Auto-resize: " + (autoResize[0] ? "On" : "Off"));
            body.removeAllViews();
            LinearLayout.LayoutParams scrollerLp = (LinearLayout.LayoutParams) scroller.getLayoutParams();
            scrollerLp.height = autoResize[0] ? maxBodyPx : ViewGroup.LayoutParams.WRAP_CONTENT;
            scroller.setLayoutParams(scrollerLp);
            if (autoResize[0]) {
                body.addView(chessResizeBoardBlock("Swiped up", naturalUpPx, upPx,
                        UiKit.dp(this, 120), naturalUpPx));
                body.addView(chessResizeBoardBlock("Swiped down", naturalUpPx, downPx,
                        UiKit.dp(this, 120), naturalUpPx));
            } else {
                body.addView(chessResizeSinglePreview(previewScale));
            }
        };
        renderBody[0].run();

        UiKit.finishCentered(dialog, scrim);
    }

    /** How wide a preview board inside this dialog may actually draw at, in real px — the
     *  block's own 20dp side padding taken out of the dialog's own width above. Both preview
     *  helpers clamp their "100%" size to this, so a real board wider than the dialog itself
     *  still shows as large as it can rather than silently shrinking to an arbitrary demo size. */
    private int chessResizePreviewMaxPx() {
        int dialogWidthPx = Math.round(getResources().getDisplayMetrics().widthPixels * 0.92f);
        return dialogWidthPx - UiKit.dp(this, 40);
    }

    private int pxToDp(float px) {
        return Math.round(px / getResources().getDisplayMetrics().density);
    }

    /** A plain info row matching {@link #chessSettingsRow}'s own chrome (same padding, same
     *  20sp text) but non-interactive and two-part: {@code label} start-aligned, a live value
     *  end-aligned — {@code valueOut[0]} is stashed so the caller can update the value text
     *  later without rebuilding the row. */
    private View chessResizeInfoRow(String label, TextView[] valueOut) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(48, 32, 48, 32);
        TextView labelView = chessText(label, 20, Color.WHITE);
        row.addView(labelView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView value = chessText("", 20, 0xFF8FBF8F);
        row.addView(value, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        valueOut[0] = value;
        return row;
    }

    /** One pinchable preview board for {@link #chessShowResizeDialog}'s Auto-resize-on body —
     *  {@code sizePx[0]} is read/written in real device px (the same units
     *  {@link #resizeChessBoardIfNeeded} works in), clamped to {@code [minPx, ceilingPx]};
     *  {@code ceilingRefPx} is what the shown percentage and the preview's own max drawn size
     *  are relative to (always the swiped-up natural size, the fixed 100% reference). */
    private View chessResizeBoardBlock(String label, int ceilingRefPx, float[] sizePx,
                                       int minPx, int ceilingPx) {
        LinearLayout block = new LinearLayout(this);
        block.setOrientation(LinearLayout.VERTICAL);

        TextView[] pctOut = new TextView[1];
        block.addView(chessResizeInfoRow(label + ":", pctOut));
        TextView pct = pctOut[0];

        FrameLayout previewBox = new FrameLayout(this);
        View preview = chessResizePreviewBoard();
        FrameLayout.LayoutParams previewLp = new FrameLayout.LayoutParams(0, 0);
        previewLp.gravity = Gravity.CENTER;
        previewBox.addView(preview, previewLp);
        // The 100% reference draws at its true real size (ceilingRefPx), not a shrunk demo
        // square — only clamped down if it genuinely wouldn't fit the dialog's own width.
        int maxPreviewPx = Math.min(ceilingRefPx, chessResizePreviewMaxPx());
        LinearLayout.LayoutParams previewBoxLp = new LinearLayout.LayoutParams(maxPreviewPx, maxPreviewPx);
        previewBoxLp.gravity = Gravity.CENTER_HORIZONTAL;
        previewBoxLp.topMargin = UiKit.dp(this, 6);
        previewBoxLp.bottomMargin = UiKit.dp(this, 12);
        block.addView(previewBox, previewBoxLp);

        Runnable applySize = () -> {
            int side = Math.round(maxPreviewPx * (sizePx[0] / ceilingRefPx));
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) preview.getLayoutParams();
            lp.width = side;
            lp.height = side;
            preview.setLayoutParams(lp);
            pct.setText(Math.round(sizePx[0] / ceilingRefPx * 100) + "%");
        };
        applySize.run();

        ScaleGestureDetector detector = new ScaleGestureDetector(this,
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override public boolean onScale(ScaleGestureDetector d) {
                        sizePx[0] = clamp(sizePx[0] * d.getScaleFactor(), minPx, ceilingPx);
                        applySize.run();
                        return true;
                    }
                });
        previewBox.setOnTouchListener((v, ev) -> {
            detector.onTouchEvent(ev);
            return true;
        });
        return block;
    }

    /** The single pinchable preview for Auto-resize-off, {@link #chessManualScale}'s own
     *  55%–150% range — unchanged range, just factored out so the toggle can swap it in and
     *  brought in line with the auto-resize blocks: caption below the board, true real size. */
    private View chessResizeSinglePreview(float[] previewScale) {
        LinearLayout block = new LinearLayout(this);
        block.setOrientation(LinearLayout.VERTICAL);

        TextView[] pctOut = new TextView[1];
        block.addView(chessResizeInfoRow("Board size:", pctOut));
        TextView pct = pctOut[0];

        // 100% here is the board's own current natural auto-fit size (today's live target,
        // before chessManualScale) — the same real size the board is actually showing at
        // right now, not an arbitrary demo constant.
        int naturalPx = chessAutoFitFor(headerOffset);
        int maxPreviewPx = Math.min(naturalPx, chessResizePreviewMaxPx());

        FrameLayout previewBox = new FrameLayout(this);
        View preview = chessResizePreviewBoard();
        FrameLayout.LayoutParams previewLp = new FrameLayout.LayoutParams(0, 0);
        previewLp.gravity = Gravity.CENTER;
        previewBox.addView(preview, previewLp);
        LinearLayout.LayoutParams previewBoxLp = new LinearLayout.LayoutParams(maxPreviewPx, maxPreviewPx);
        previewBoxLp.gravity = Gravity.CENTER_HORIZONTAL;
        previewBoxLp.topMargin = UiKit.dp(this, 6);
        previewBoxLp.bottomMargin = UiKit.dp(this, 12);
        block.addView(previewBox, previewBoxLp);

        Runnable applyPreviewSize = () -> {
            int side = Math.round(maxPreviewPx * previewScale[0]);
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) preview.getLayoutParams();
            lp.width = side;
            lp.height = side;
            preview.setLayoutParams(lp);
            pct.setText(Math.round(previewScale[0] * 100) + "%");
        };
        applyPreviewSize.run();

        ScaleGestureDetector detector = new ScaleGestureDetector(this,
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override public boolean onScale(ScaleGestureDetector d) {
                        previewScale[0] = clamp(previewScale[0] * d.getScaleFactor(), 0.55f, 1f);
                        applyPreviewSize.run();
                        return true;
                    }
                });
        previewBox.setOnTouchListener((v, ev) -> {
            detector.onTouchEvent(ev);
            return true;
        });
        return block;
    }

    /** An empty (no pieces) checkerboard, same colors as the default board theme — purely a
     *  size reference inside {@link #chessShowResizeDialog}, never played on. */
    private View chessResizePreviewBoard() {
        View view = new View(this) {
            private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
            @Override protected void onDraw(Canvas c) {
                super.onDraw(c);
                float cell = getWidth() / 8f;
                // Same theme the real board is showing (bundled tile art if the theme has
                // one, else its flat light/dark fill) — not a hardcoded placeholder pattern.
                Bitmap lightTile = chessBoard.boardLightTile();
                Bitmap darkTile = chessBoard.boardDarkTile();
                int lightColor = chessBoard.boardLightColor();
                int darkColor = chessBoard.boardDarkColor();
                for (int r = 0; r < 8; r++) for (int col = 0; col < 8; col++) {
                    boolean isLight = ((r + col) & 1) == 0;
                    float l = col * cell, t = r * cell;
                    if (lightTile != null) {
                        Bitmap tile = isLight ? lightTile : darkTile;
                        c.drawBitmap(tile, null, new RectF(l, t, l + cell, t + cell), paint);
                    } else {
                        paint.setColor(isLight ? lightColor : darkColor);
                        c.drawRect(l, t, l + cell, t + cell, paint);
                    }
                }
            }
        };
        // Rounded corners on the preview too, matching the real board (see buildChessPanel).
        UiKit.clipRounded(this, view, UiKit.R_SM);
        return view;
    }

    private void chessOpenSettings() {
        RoundedBox root = new RoundedBox(this);
        root.setBackground(UiKit.dialogBackground(this));
        root.setRadiusDp(UiKit.R_MD);
        root.setPadding(2, 32, 2, UiKit.dp(this, UiKit.R_MD));

        android.widget.FrameLayout scrim = UiKit.wrapScrim(this, root, 0.85f);
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.Theme_PlainPhone_RoundedDialog)
                .setView(scrim).create();

        // Title and close share one top line — Import/Export/Imported games/Puzzles/Generate
        // all moved to the Library and Puzzles tabs, so there's no longer a bottom "Close"
        // row's worth of other rows to separate it from.
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = UiKit.dialogTitle(this, "Chess Settings");
        head.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView close = UiKit.dialogTitle(this, "✕");
        close.setOnClickListener(v -> dialog.dismiss());
        head.addView(close);
        root.addView(head);

        LinearLayout rows = new LinearLayout(this);
        rows.setOrientation(LinearLayout.VERTICAL);
        ScrollView scroller = new ScrollView(this);
        scroller.addView(rows, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(scroller);

        renderChessSettingsRows(rows, dialog);
        UiKit.finishCentered(dialog, scrim);
    }

    private void renderChessSettingsRows(LinearLayout rows, AlertDialog dialog) {
        rows.removeAllViews();
        rows.addView(chessSettingsRow("Board theme: " + chessSelectedBoard,
                v -> { dialog.dismiss(); chessChooseBoard(); }));
        rows.addView(chessSettingsRow("Piece theme: " + ChessBoardView.pretty(chessSelectedPieces),
                v -> { dialog.dismiss(); chessChoosePieces(); }));
        boolean coords = Config.getChessShowCoords(this);
        rows.addView(chessSettingsRow("Board coordinates: " + (coords ? "On" : "Off"), v -> {
            Config.setChessShowCoords(this, !coords);
            chessBoard.invalidate();
            renderChessSettingsRows(rows, dialog);
        }));
        rows.addView(UiKit.dialogTitle(this, "Engine"));
        int depth = Config.getChessEngineDepth(this);
        rows.addView(chessSettingsRow("Engine depth: " + depth, v -> {
            Config.setChessEngineDepth(this, nextChessDepth(depth));
            chessBoard.requestAnalysis();
            renderChessSettingsRows(rows, dialog);
        }));
        int lines = Config.getChessAnalysisLines(this);
        rows.addView(chessSettingsRow("Variations shown: " + lines, v -> {
            Config.setChessAnalysisLines(this, lines >= 5 ? 1 : lines + 1);
            chessBoard.requestAnalysis();
            renderChessSettingsRows(rows, dialog);
        }));
    }

    private static final int[] CHESS_DEPTH_STEPS = {8, 10, 12, 15, 18, 20, 24};

    private int nextChessDepth(int current) {
        for (int i = 0; i < CHESS_DEPTH_STEPS.length; i++) {
            if (CHESS_DEPTH_STEPS[i] == current) return CHESS_DEPTH_STEPS[(i + 1) % CHESS_DEPTH_STEPS.length];
        }
        return CHESS_DEPTH_STEPS[0];
    }

    /** The status row's save icon — writes the board's current moves/comments/result back to
     *  the library: either overwriting {@link #chessCurrentEntry} (if this game was loaded
     *  from one) or appending it as a new game into an existing or brand-new PGN. */
    private void chessShowSaveSheet() {
        if (chessBoard.mainlineSans().isEmpty()) { toast("No moves to save"); return; }
        new Thread(() -> {
            List<ChessLibrary.SourceSummary> sources = ChessLibrary.listSources(this);
            runOnUiThread(() -> chessShowSaveSheetWith(new ArrayList<>(sources), new LinkedHashSet<>(), 0, null, null));
        }).start();
    }

    /** One dialog, two screens swapped in place (never a second stacked dialog — that used to
     *  make "+ New PGN" flash the sheet behind it for a moment while the old dialog dismissed
     *  and the new one hadn't shown yet): MAIN (Update / "Save to…") and PICKER (multi-select
     *  which PGN(s) to save into, "+ New PGN"). "+ New PGN" itself is the one exception — it
     *  dismisses this dialog for {@link UiKit#textPrompt}'s own proven-working keyboard
     *  handling (an EditText added to an already-shown dialog window, the pattern the other
     *  two screens use, never reliably got the IME to actually open — focus and a forced
     *  show() call both landed, the keyboard itself just didn't), then reopens this same
     *  picker via {@code onDismiss} either way (Cancel or Create). */
    private void chessShowSaveSheetWith(List<ChessLibrary.SourceSummary> sourceList,
                                        Set<String> selectedTargets, int initialMode,
                                        Map<String, String> pendingTags, String pendingResult) {
        int[] mode = {initialMode};
        Map<String, String>[] tagsHolder = new Map[]{pendingTags};
        String[] resultHolder = {pendingResult};
        Runnable[] render = new Runnable[1];

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(UiKit.dialogBackground(this));
        UiKit.clipRounded(this, root, UiKit.R_MD);
        root.setPadding(2, 32, 2, UiKit.dp(this, UiKit.R_MD));

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        root.addView(body);

        android.widget.FrameLayout scrim = UiKit.wrapScrim(this, root, 0.85f);
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.Theme_PlainPhone_RoundedDialog)
                .setView(scrim).create();

        render[0] = () -> {
            body.removeAllViews();
            if (mode[0] == 0) {
                body.addView(UiKit.dialogTitle(this, "Save Game"));
                if (chessCurrentEntry != null) {
                    body.addView(chessSettingsRow("Update this game", v -> {
                        dialog.dismiss();
                        chessUpdateCurrentEntry(tagsHolder[0], resultHolder[0]);
                    }));
                }
                body.addView(chessSettingsRow("Edit metadata", v -> {
                    dialog.dismiss();
                    String w = tagsHolder[0] != null ? tagsHolder[0].get("White") : chessCurrentEntry != null ? chessCurrentEntry.white : "";
                    String b = tagsHolder[0] != null ? tagsHolder[0].get("Black") : chessCurrentEntry != null ? chessCurrentEntry.black : "";
                    String ev = tagsHolder[0] != null ? tagsHolder[0].get("Event") : chessCurrentEntry != null ? chessCurrentEntry.event : "";
                    String rd = tagsHolder[0] != null ? tagsHolder[0].get("Round") : chessCurrentEntry != null ? chessCurrentEntry.round : "";
                    String dt = tagsHolder[0] != null ? tagsHolder[0].get("Date") : chessCurrentEntry != null ? chessCurrentEntry.date
                            : new java.text.SimpleDateFormat("yyyy.MM.dd", java.util.Locale.US).format(new java.util.Date());
                    String ec = tagsHolder[0] != null ? tagsHolder[0].get("ECO") : chessCurrentEntry != null ? chessCurrentEntry.eco : "";
                    String res = resultHolder[0] != null ? resultHolder[0]
                            : chessCurrentEntry != null ? chessCurrentEntry.result : chessBoard.pgnResult();
                    // Save here closes for good — no return to the Save Game sheet. A loaded
                    // game's tags are committed straight to its library entry, same as the
                    // Library's own "Edit metadata"; a still-unsaved game has nothing to
                    // commit to yet, so there's nothing more to do either way.
                    ChessMetadataDialog.show(this, w, b, ev, rd, dt, ec, res,
                            (tags, result) -> {
                                if (chessCurrentEntry == null) return;
                                ChessLibrary.Entry entry = chessCurrentEntry;
                                new Thread(() -> ChessLibrary.updateMetadata(this, entry.id, tags, result)).start();
                                chessCurrentEntry = new ChessLibrary.Entry(entry.id, entry.src,
                                        tags.get("White"), tags.get("Black"), tags.get("Event"), tags.get("Round"),
                                        tags.get("ECO"), tags.get("Date"), result, entry.sansJoined, entry.importedAt,
                                        entry.comments);
                                updateChessMetaUi();
                                toast("Metadata updated");
                            }, null);
                }));
                body.addView(chessSettingsRow("Save to…", v -> { mode[0] = 1; render[0].run(); }));
            } else {
                body.addView(chessPickerHeader("Save to…", () -> { mode[0] = 0; render[0].run(); },
                        "Save", () -> {
                            if (selectedTargets.isEmpty()) { toast("Choose at least one PGN"); return; }
                            dialog.dismiss();
                            chessSaveAsNewGameMulti(new LinkedHashSet<>(selectedTargets), tagsHolder[0], resultHolder[0]);
                        }));
                body.addView(chessColoredRow("+ New PGN", Color.WHITE, v -> {
                    dialog.dismiss();
                    UiKit.textPrompt(this, "New PGN", "", "Create", true, name -> {
                        new Thread(() -> ChessLibrary.createSource(this, name)).start();
                        String normalized = ChessLibrary.normalizeSourceName(name);
                        sourceList.add(new ChessLibrary.SourceSummary(normalized, 0, System.currentTimeMillis()));
                        selectedTargets.add(normalized);
                    }, () -> chessShowSaveSheetWith(sourceList, selectedTargets, 1, tagsHolder[0], resultHolder[0]));
                }));
                LinearLayout rows = new LinearLayout(this);
                rows.setOrientation(LinearLayout.VERTICAL);
                for (ChessLibrary.SourceSummary s : sourceList) {
                    boolean sel = selectedTargets.contains(s.label);
                    rows.addView(chessColoredRow((sel ? "[x] " : "[ ] ") + s.label, Color.WHITE, v -> {
                        if (!selectedTargets.remove(s.label)) selectedTargets.add(s.label);
                        render[0].run();
                    }));
                }
                ScrollView scroller = new ScrollView(this);
                scroller.addView(rows, new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                int maxScrollerPx = (int) (getResources().getDisplayMetrics().heightPixels * 0.5f);
                body.addView(scroller, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, maxScrollerPx));
            }
        };
        render[0].run();

        UiKit.finishCentered(dialog, scrim);
    }

    /** [← back]  title  .......  [action], all one row — the picker/new-PGN screens' shared
     *  header inside {@link #chessShowSaveSheetWith}'s single dialog. */
    private LinearLayout chessPickerHeader(String title, Runnable onBack, String actionLabel, Runnable onAction) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(48, 20, 48, 20);
        TextView back = chessText("←", 22, Color.WHITE);
        back.setOnClickListener(v -> onBack.run());
        row.addView(back);
        TextView titleView = chessText(title, 17, Color.WHITE);
        titleView.setTypeface(Fonts.current(this), android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        titleLp.leftMargin = UiKit.dp(this, 16);
        row.addView(titleView, titleLp);
        TextView action = chessText(actionLabel, 17, Color.WHITE);
        action.setTypeface(Fonts.current(this), android.graphics.Typeface.BOLD);
        action.setOnClickListener(v -> onAction.run());
        row.addView(action);
        return row;
    }

    private TextView chessColoredRow(String label, int color, View.OnClickListener listener) {
        TextView row = chessText(label, 20, color);
        row.setPadding(48, 32, 48, 32);
        StateListDrawable bg = new StateListDrawable();
        bg.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Color.DKGRAY));
        bg.addState(new int[]{}, new ColorDrawable(Color.BLACK));
        row.setBackground(bg);
        row.setOnClickListener(listener);
        return row;
    }

    /** {@code tagOverride}/{@code resultOverride} come from the Save sheet's "Edit metadata"
     *  step, when the user went through it — {@code null} for either leaves the corresponding
     *  half of the game (moves/comments/result vs. tags) untouched, same as before metadata
     *  editing existed. */
    private void chessUpdateCurrentEntry(Map<String, String> tagOverride, String resultOverride) {
        ChessLibrary.Entry entry = chessCurrentEntry;
        if (entry == null) return;
        List<String> sans = chessBoard.mainlineSans();
        List<String> comments = chessBoard.mainlineComments();
        String result = resultOverride != null ? resultOverride : chessBoard.pgnResult();
        new Thread(() -> {
            boolean ok = ChessLibrary.updateEntry(this, entry.id, sans, comments, result);
            if (ok && tagOverride != null) ChessLibrary.updateMetadata(this, entry.id, tagOverride, null);
            runOnUiThread(() -> toast(ok ? "Game updated" : "Could not update game"));
        }).start();
    }

    /** Appends the board's current game as a brand-new library entry into every PGN in
     *  {@code sources} — {@code tagOverride}/{@code resultOverride} from "Edit metadata" if the
     *  user went through it, otherwise {@link #chessCurrentEntry}'s own player/event tags when
     *  this game started as a copy of one (a natural "save as" for a variation explored from a
     *  loaded game), plain defaults otherwise. */
    private void chessSaveAsNewGameMulti(Set<String> sources, Map<String, String> tagOverride, String resultOverride) {
        List<String> sans = chessBoard.mainlineSans();
        List<String> comments = chessBoard.mainlineComments();
        String result = resultOverride != null ? resultOverride : chessBoard.pgnResult();
        Map<String, String> tags;
        if (tagOverride != null) {
            tags = tagOverride;
        } else {
            tags = new LinkedHashMap<>();
            if (chessCurrentEntry != null) {
                tags.put("White", chessCurrentEntry.white);
                tags.put("Black", chessCurrentEntry.black);
                tags.put("Event", chessCurrentEntry.event);
                tags.put("Round", chessCurrentEntry.round);
                tags.put("ECO", chessCurrentEntry.eco);
                tags.put("Date", chessCurrentEntry.date);
            } else {
                tags.put("Event", "Plainphone study");
                tags.put("Date", new java.text.SimpleDateFormat("yyyy.MM.dd", java.util.Locale.US).format(new java.util.Date()));
            }
        }
        new Thread(() -> {
            String lastId = null;
            for (String source : sources) {
                String id = ChessLibrary.saveGame(this, source, tags, sans, comments, result);
                if (id != null) lastId = id;
            }
            String finalId = lastId;
            runOnUiThread(() -> {
                if (finalId != null) {
                    chessCurrentEntry = ChessLibrary.findById(this, finalId);
                    updateChessMetaUi();
                    toast("Saved to " + sources.size() + (sources.size() == 1 ? " PGN" : " PGNs"));
                } else {
                    toast("Could not save game");
                }
            });
        }).start();
    }

    /** Matches {@code UiKit.promptRow}'s look — the app's one standard popup-row style. */
    private TextView chessSettingsRow(String label, View.OnClickListener listener) {
        TextView row = chessText(label, 20, Color.WHITE);
        row.setPadding(48, 32, 48, 32);
        StateListDrawable bg = new StateListDrawable();
        bg.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Color.DKGRAY));
        bg.addState(new int[]{}, new ColorDrawable(Color.BLACK));
        row.setBackground(bg);
        row.setOnClickListener(listener);
        return row;
    }

    /** Same rounded-sheet chrome as {@link #chessOpenSettings}, for a plain pick-one list. */
    private void chessOptionSheet(String title, String[] labels, java.util.function.IntConsumer onPick) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(UiKit.dialogBackground(this));
        UiKit.clipRounded(this, root, UiKit.R_MD);
        root.setPadding(2, 32, 2, UiKit.dp(this, UiKit.R_MD));
        root.addView(UiKit.dialogTitle(this, title));

        LinearLayout rows = new LinearLayout(this);
        rows.setOrientation(LinearLayout.VERTICAL);
        ScrollView scroller = new ScrollView(this);
        scroller.addView(rows, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        int maxScrollerPx = (int) (getResources().getDisplayMetrics().heightPixels * 0.6f);
        root.addView(scroller, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, maxScrollerPx));

        android.widget.FrameLayout scrim = UiKit.wrapScrim(this, root, 0.85f);
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.Theme_PlainPhone_RoundedDialog)
                .setView(scrim).create();
        for (int i = 0; i < labels.length; i++) {
            final int index = i;
            rows.addView(chessSettingsRow(labels[i], v -> { dialog.dismiss(); onPick.accept(index); }));
        }
        UiKit.finishCentered(dialog, scrim);
    }

    private void chessChooseBoard() {
        List<String[]> options = new ArrayList<>(); // {slug, label}
        try {
            for (String name : getAssets().list("chess_theme/board")) {
                if (!name.endsWith(".png")) continue;
                String slug = name.substring(6, name.length() - 4);
                options.add(new String[]{slug, ChessBoardView.pretty(slug)});
            }
        } catch (Exception ignored) { }
        Collections.sort(options, (a, b) -> a[1].compareTo(b[1]));
        options.add(0, new String[]{"default", "Slate Study"});
        String[] labels = new String[options.size()];
        for (int i = 0; i < labels.length; i++) labels[i] = options.get(i)[1];
        chessOptionSheet("Board theme", labels, which -> {
            chessSelectedBoard = options.get(which)[1];
            chessBoard.setBoardTheme(options.get(which)[0]);
        });
    }

    private void chessChoosePieces() {
        List<String> folders = new ArrayList<>();
        try { folders.addAll(Arrays.asList(getAssets().list("chess_theme/pieces"))); } catch (Exception ignored) { }
        Collections.sort(folders);
        String[] labels = new String[folders.size()];
        for (int i = 0; i < labels.length; i++) labels[i] = ChessBoardView.pretty(folders.get(i));
        chessOptionSheet("Piece theme", labels, which -> {
            chessSelectedPieces = folders.get(which);
            chessBoard.setPieceTheme(chessSelectedPieces);
        });
    }

    private void chessImportPgn() {
        // .pgn has no MIME type Android recognizes, so a "text/*" filter hides it from the
        // picker entirely (it isn't registered as text/anything) — "*/*" shows everything,
        // which is the standard workaround for a custom extension with no registered type.
        Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        pick.addCategory(Intent.CATEGORY_OPENABLE);
        pick.setType("*/*");
        startActivityForResult(pick, REQUEST_CHESS_IMPORT);
    }

    private void chessExportPgn() {
        Intent save = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        save.addCategory(Intent.CATEGORY_OPENABLE);
        save.setType("application/x-chess-pgn");
        save.putExtra(Intent.EXTRA_TITLE, "plainphone-study.pgn");
        startActivityForResult(save, REQUEST_CHESS_EXPORT);
    }

    /** "Export this PGN" from a Library-tab source group's options menu — every game under
     *  {@code source}, not just the one on the board. */
    private void chessExportSource(String source) {
        chessExportSourceLabel = source;
        Intent save = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        save.addCategory(Intent.CATEGORY_OPENABLE);
        save.setType("application/x-chess-pgn");
        String fileName = source.toLowerCase(java.util.Locale.US).endsWith(".pgn") ? source : source + ".pgn";
        save.putExtra(Intent.EXTRA_TITLE, fileName);
        startActivityForResult(save, REQUEST_CHESS_EXPORT_SOURCE);
    }

    /** Kicks off a background {@link ChessImportJobs} run instead of reading/parsing the file
     *  right here — a real downloaded PGN collection (a player's whole career, an opening
     *  database) is routinely thousands of games, and streaming that off a foreground-service
     *  job (survives navigating away, even the app dying and JobService restarting it) beats
     *  an inline background {@code Thread} that dies with the Activity. Import is now silent —
     *  no "choose a game" picker or auto-load onto the board once it finishes, since the app
     *  may not even be on this screen (or open at all) by then; the imported games just show
     *  up in the Library tab like any other, same as picking one out afterward always worked. */
    private void handleChessImport(int resultCode, Intent data) {
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        String sourceLabel = chessDisplayNameOf(uri);
        try {
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (SecurityException ignored) {
            // Some providers don't support persistable permissions — the job still works as
            // long as this process stays alive long enough to finish reading the file.
        }
        ChessImportJobs.start(this, uri, sourceLabel);
        toast("Importing " + sourceLabel + "…");
    }

    /** The file's display name, for the "Imported games" library's source label — a content
     *  {@link Uri} carries no filename of its own, so this is a best-effort lookup via the
     *  {@code OpenableColumns} projection every document provider is required to answer. */
    private String chessDisplayNameOf(Uri uri) {
        try (android.database.Cursor c = getContentResolver().query(uri, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) {
                    String name = c.getString(idx);
                    if (name != null && !name.isEmpty()) return name;
                }
            }
        } catch (Exception ignored) { }
        return "Imported PGN";
    }

    private void handleChessLibraryPick(int resultCode, Intent data) {
        if (resultCode != RESULT_OK || data == null) return;
        String id = data.getStringExtra(ChessLibraryActivity.EXTRA_ID);
        String white = data.getStringExtra(ChessLibraryActivity.EXTRA_WHITE);
        String black = data.getStringExtra(ChessLibraryActivity.EXTRA_BLACK);
        String sans = data.getStringExtra(ChessLibraryActivity.EXTRA_SANS);
        if (sans == null) return;
        List<String> moves = new ArrayList<>();
        if (!sans.isEmpty()) for (String s : sans.split(" ")) if (!s.isEmpty()) moves.add(s);
        chessCurrentEntry = id == null ? null : ChessLibrary.findById(this, id);
        updateChessMetaUi();
        List<String> comments = chessCurrentEntry != null ? chessCurrentEntry.commentsForSans() : null;
        chessBoard.loadSanMovesAsync(moves, comments, chessCurrentEntry != null ? chessCurrentEntry.result : null, null);
        toast(moves.isEmpty() ? "PGN loaded (no moves found)" : "Loaded " + white + " vs " + black);
    }

    private void handleChessExport(int resultCode, Intent data) {
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Map<String, String> tags = new LinkedHashMap<>();
        tags.put("Event", "Plainphone study");
        tags.put("Date", new java.text.SimpleDateFormat("yyyy.MM.dd", java.util.Locale.US).format(new java.util.Date()));
        String pgn = Pgn.write(tags, chessBoard.mainlineSans(), chessBoard.mainlineComments(), chessBoard.pgnResult());
        try (OutputStream out = getContentResolver().openOutputStream(data.getData())) {
            if (out != null) out.write(pgn.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            toast("PGN exported");
        } catch (Exception e) {
            toast("Could not export PGN");
        }
    }

    /** Writes every game under {@link #chessExportSourceLabel} to the picked file, one after
     *  another — {@link ChessLibrary#loadBySourceWithSans} (not the lightweight
     *  {@code loadBySource} every other Library-tab read uses) since this actually needs each
     *  game's moves, not just white/black/event/date. Off the UI thread: a big PGN is
     *  thousands of games. */
    private void handleChessExportSource(int resultCode, Intent data) {
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        String source = chessExportSourceLabel;
        if (source == null) return;
        toast("Exporting " + source + "…");
        new Thread(() -> {
            List<ChessLibrary.Entry> games = ChessLibrary.loadBySourceWithSans(this, source);
            Collections.reverse(games); // newest-first -> oldest-first, the file's own original order
            boolean ok = false;
            try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                if (out != null) {
                    for (ChessLibrary.Entry e : games) {
                        Map<String, String> tags = new LinkedHashMap<>();
                        tags.put("Event", e.event);
                        tags.put("Date", e.date);
                        tags.put("Round", e.round);
                        tags.put("White", e.white);
                        tags.put("Black", e.black);
                        if (!e.eco.isEmpty()) tags.put("ECO", e.eco);
                        String pgn = Pgn.write(tags, e.sans(), e.commentsForSans(), e.result);
                        out.write(pgn.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                        out.write('\n');
                    }
                    ok = true;
                }
            } catch (Exception ignored) { }
            boolean success = ok;
            int count = games.size();
            runOnUiThread(() -> toast(success ? ("Exported " + count + " games") : "Could not export PGN"));
        }).start();
    }

    /** "Export" from the Library tab's multi-select toolbar — an arbitrary set of games,
     *  possibly spanning several PGNs, as opposed to {@link #chessExportSource}'s whole-PGN
     *  export. */
    private void chessExportSelected(java.util.Set<String> ids) {
        chessExportSelectedIds = ids;
        Intent save = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        save.addCategory(Intent.CATEGORY_OPENABLE);
        save.setType("application/x-chess-pgn");
        save.putExtra(Intent.EXTRA_TITLE, ids.size() + "-games.pgn");
        startActivityForResult(save, REQUEST_CHESS_EXPORT_SELECTED);
    }

    private void handleChessExportSelected(int resultCode, Intent data) {
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        java.util.Set<String> ids = chessExportSelectedIds;
        if (ids == null) return;
        toast("Exporting " + ids.size() + " games…");
        new Thread(() -> {
            String pgn = ChessLibrary.writePgn(this, ids);
            boolean ok = false;
            try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                if (out != null) { out.write(pgn.getBytes(java.nio.charset.StandardCharsets.UTF_8)); ok = true; }
            } catch (Exception ignored) { }
            boolean success = ok;
            runOnUiThread(() -> toast(success ? ("Exported " + ids.size() + " games") : "Could not export PGN"));
        }).start();
    }

    private TextView chessText(String value, float size, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setTypeface(Fonts.current(this));
        view.setIncludeFontPadding(false);
        return view;
    }

    /** Blank space before a glyph's actual ink, at the given sp size — the gap a
     *  START-aligned icon like "|‹" leaves before matching text's own left edge. */
    private float chessInkLeadIn(String text, float sp) {
        android.graphics.Paint p = new android.graphics.Paint();
        p.setTypeface(Fonts.current(this));
        p.setTextSize(android.util.TypedValue.applyDimension(
                android.util.TypedValue.COMPLEX_UNIT_SP, sp, getResources().getDisplayMetrics()));
        android.graphics.Rect bounds = new android.graphics.Rect();
        p.getTextBounds(text, 0, text.length(), bounds);
        return bounds.left;
    }

    /** Blank space after a glyph's actual ink, at the given sp size — the gap an
     *  END-aligned icon like "›|" leaves before matching text's own right edge. */
    private float chessInkTrailOut(String text, float sp) {
        android.graphics.Paint p = new android.graphics.Paint();
        p.setTypeface(Fonts.current(this));
        p.setTextSize(android.util.TypedValue.applyDimension(
                android.util.TypedValue.COMPLEX_UNIT_SP, sp, getResources().getDisplayMetrics()));
        android.graphics.Rect bounds = new android.graphics.Rect();
        p.getTextBounds(text, 0, text.length(), bounds);
        return p.measureText(text) - bounds.right;
    }

    private View chessRule() {
        View rule = new View(this);
        rule.setBackgroundColor(0xFF303030);
        return rule;
    }

    private void updateChessHomeUi() {
        if (chessBoard == null || chessTurnLine == null) return;
        String puzzleStatus = chessBoard.puzzleStatusText();
        String gameOver = chessBoard.gameOverText();
        chessTurnLine.setText(puzzleStatus != null ? puzzleStatus
                : gameOver != null ? gameOver
                : chessBoard.whiteToMove() ? "White to move" : "Black to move");
        List<String> engineLines = chessBoard.engineSummary();
        for (int i = 0; i < chessEngineLines.length; i++) {
            boolean has = i < engineLines.size();
            chessEngineLines[i].setVisibility(has ? View.VISIBLE : View.GONE);
            if (has) chessEngineLines[i].setText(engineLines.get(i));
        }
        chessMovesGrid.refresh(); // comments render inline in the grid, right under their move
        scheduleChessAutosave();
    }

    /** Debounced autosave for a game already loaded from the library — every move/comment/
     *  variation edit lands here via {@link #updateChessHomeUi} instead of requiring a manual
     *  "Update" tap. Debounced (1.2s of quiet) so a burst of edits (replaying moves, typing a
     *  comment) writes once, not on every keystroke/move. Silent — no toast, unlike the manual
     *  Update action — an autosave firing constantly would just be noise. */
    private void scheduleChessAutosave() {
        if (chessCurrentEntry == null) return;
        if (chessAutosaveTask != null) chessAutosaveHandler.removeCallbacks(chessAutosaveTask);
        ChessLibrary.Entry entry = chessCurrentEntry;
        chessAutosaveTask = () -> {
            List<String> sans = chessBoard.mainlineSans();
            List<String> comments = chessBoard.mainlineComments();
            String result = chessBoard.pgnResult();
            new Thread(() -> ChessLibrary.updateEntry(this, entry.id, sans, comments, result)).start();
        };
        chessAutosaveHandler.postDelayed(chessAutosaveTask, 1200);
    }

    /** Just the engine-eval lines — {@link ChessBoardView}'s dedicated callback for a pure
     *  analysis-progress tick (Stockfish's iterative deepening streams several a second),
     *  as opposed to {@link #updateChessHomeUi}'s full refresh (status line + moves grid)
     *  for when the position/move tree itself actually changed. Rebuilding the whole moves
     *  grid on every one of those ticks — genuinely expensive for a long game — is what used
     *  to keep the UI thread (and so all scrolling) busy for a second or more right after a
     *  big game's first real analysis started streaming in. */
    private void updateChessEngineLinesOnly() {
        if (chessBoard == null) return;
        List<String> engineLines = chessBoard.engineSummary();
        for (int i = 0; i < chessEngineLines.length; i++) {
            boolean has = i < engineLines.size();
            chessEngineLines[i].setVisibility(has ? View.VISIBLE : View.GONE);
            if (has) chessEngineLines[i].setText(engineLines.get(i));
        }
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

        // Stats sits behind the app-list lock — same PIN, same grace window.
        boolean statsLocked = homeMode == HomeMode.STATS && Lock.APPS.gateActive(this);
        boolean showStats = homeMode == HomeMode.STATS && needle.isEmpty() && !statsLocked;
        boolean showChess = homeMode == HomeMode.CHESS && needle.isEmpty() && !selecting;
        if (statsPanel != null) {
            statsPanel.view().setVisibility(showStats ? View.VISIBLE : View.GONE);
            if (chessPanel != null) chessPanel.setVisibility(showChess ? View.VISIBLE : View.GONE);
            listView.setVisibility(showStats || showChess ? View.GONE : View.VISIBLE);
            if (showStats && !statsPanelShown) {
                statsPanelShown = true;
                statsPanel.render();
            } else if (!showStats) {
                statsPanelShown = false;
            }
        }
        if (showStats) {
            if (Lock.APPS.isLocked(this)) Lock.APPS.keepUnlocked(this);
            syncHeaderCollapse();
            adapter.notifyDataSetChanged();
            return;
        }
        if (showChess) {
            updateChessHomeUi();
            syncHeaderCollapse();
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
                    NotesSection.renderSelection(this, rows);
                } else {
                    if (Lock.NOTES.isLocked(this)) Lock.NOTES.keepUnlocked(this);
                    NotesSection.render(this, this::openNote, rows);
                }
            } else if (homeMode == HomeMode.TODOS) {
                renderTodoSection();
            } else if (homeMode == HomeMode.RECORDER) {
                renderRecorderSection();
            } else if (homeMode == HomeMode.VAULT) {
                renderVaultSection();
            } else if (homeMode == HomeMode.DEV) {
                renderDevSection();
            } else if (homeMode == HomeMode.WORKSPACE) {
                renderWorkspaceSection();
            } else if (homeMode == HomeMode.STATS) {
                rows.add(new SearchResult(SearchResult.Kind.APP, "App list is locked",
                        "Tap to unlock", -1, () -> startActivityForResult(
                        Lock.APPS.pinGate(this), REQUEST_APPS_UNLOCK)));
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

        // Searching / selecting / stats force the header open; otherwise restore
        // the user's chosen stage.
        syncHeaderCollapse();
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
        return Recorder.orderedAll(this);
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
            RecorderSection.renderSelection(this, rows);
            return;
        }
        RecorderSection.render(this, rows);
    }

    // --- shared multi-select ------------------------------------------------

    @Override public java.util.Set<String> selection() { return selection; }

    @Override public SelectionBar selectionBar() { return selectionBar; }

    @Override public void toggle(String id) {
        if (!selection.remove(id)) selection.add(id);
        filter(search.getText().toString());
    }

    @Override public void setSelected(java.util.Collection<String> ids) {
        selection.clear();
        selection.addAll(ids);
        filter(search.getText().toString());
    }

    private void enterSelection(HomeMode mode, String firstId) {
        selectMode = mode;
        selection.clear();
        if (firstId != null) selection.add(firstId);
        filter(search.getText().toString());
    }

    @Override public void exitSelection() {
        selectMode = null;
        selection.clear();
        filter(search.getText().toString());
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

    private void renderWorkspaceSection() {
        java.util.List<Workspaces.Meta> all = Workspaces.list(this);

        if (selectMode == HomeMode.WORKSPACE) {
            java.util.List<Workspaces.Meta> sel = new ArrayList<>();
            for (Workspaces.Meta m : all) if (selection.contains(m.id)) sel.add(m);
            java.util.List<String> ids = new ArrayList<>();
            for (Workspaces.Meta m : all) ids.add(m.id);

            java.util.List<BarAction> actions = new ArrayList<>();
            if (sel.size() == 1) {
                Workspaces.Meta one = sel.get(0);
                actions.add(new BarAction("Rename", () -> UiKit.textPrompt(this, "Rename workspace",
                        one.name, "Save", name -> { Workspaces.rename(this, one.id, name); refresh(); })));
            }
            actions.add(new BarAction("Delete", () -> VaultUi.confirm(this,
                    "Delete " + sel.size() + " workspace" + (sel.size() == 1 ? "" : "s") + "?",
                    null, "Delete", () -> {
                        for (Workspaces.Meta m : sel) Workspaces.delete(this, m.id);
                        exitSelection();
                    }, "Cancel", null)));
            selectionBar.bind(this, ids, actions);

            for (Workspaces.Meta m : all) {
            final String id = m.id;
                rows.add(new SearchResult(SearchResult.Kind.WORKSPACE, m.name,
                        null, -1,
                        () -> toggle(id), m).check(selection.contains(id)));
            }
            return;
        }

        for (Workspaces.Meta m : all) {
            final String id = m.id;
            rows.add(new SearchResult(SearchResult.Kind.WORKSPACE, m.name,
                    null, -1,
                    () -> startActivity(new Intent(this, WorkspaceActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            .putExtra(WorkspaceActivity.EXTRA_ID, id)), m));
        }
        rows.add(new SearchResult(SearchResult.Kind.WORKSPACE, "+ New workspace", null, -1, () -> {
            Workspaces.Meta m = Workspaces.create(this, null);
            startActivity(new Intent(this, WorkspaceActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .putExtra(WorkspaceActivity.EXTRA_ID, m.id));
        }));
    }

    private void renderDevSection() {
        if (Lock.DEV.gateActive(this)) {
            rows.add(new SearchResult(SearchResult.Kind.DEV, "Dev is locked",
                    "Tap to unlock", -1, () -> startActivityForResult(
                    Lock.DEV.pinGate(this), REQUEST_DEV_UNLOCK)));
            return;
        }
        if (Lock.DEV.isLocked(this)) Lock.DEV.keepUnlocked(this);

        rows.add(new SearchResult(SearchResult.Kind.DEV, "Dev settings", null, -1,
                () -> startActivity(new Intent(this, DevSettingsActivity.class))));

        rows.add(new SearchResultsAdapter.Header("Devices"));
        java.util.List<DevHost> devHosts = DevHost.all(this);
        for (DevHost host : devHosts) {
            boolean live = DevService.isConnected(host.id);
            rows.add(new SearchResult(SearchResult.Kind.DEV, host.label,
                    live ? "connected — tap to open" : host.address(), -1, () -> {
                DevService.connect(this, host.id);
                startActivity(new Intent(this, DevHostActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .putExtra(DevHostActivity.EXTRA_HOST_ID, host.id));
            }));
        }

        rows.add(new SearchResult(SearchResult.Kind.DEV, "+ Add device", null, -1,
                () -> startActivity(new Intent(this, DevPairActivity.class))));

        java.util.List<DevAccount> cloud = DevAccount.all(this);
        if (!cloud.isEmpty()) {
            rows.add(new SearchResultsAdapter.Header("Cloud"));
            for (DevAccount a : cloud) {
                String label = (a.label == null || a.label.isEmpty())
                        ? a.displayKind() : a.displayKind() + " · " + a.label;
                rows.add(new SearchResult(SearchResult.Kind.DEV, label, null, -1, () ->
                        startActivity(new Intent(this, CloudPanelActivity.class)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                .putExtra(CloudPanelActivity.EXTRA_KIND, a.kind)
                                .putExtra(CloudPanelActivity.EXTRA_ACCOUNT_ID, a.id))));
            }
        }
        rows.add(new SearchResult(SearchResult.Kind.DEV, "+ Add cloud account", null, -1,
                () -> startActivity(new Intent(this, DevAccountsActivity.class))));
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
            TodoSection.renderSelection(this, rows);
            return;
        }

        TodoSection.render(this, rows);
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

        if (Config.isLocksEnabled(this)
                && Config.isApplockEnabled(this)
                && Config.getLockedPackages(this).contains(pkg)
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

        root.addView(UiKit.dialogTitle(this, label.toString()));

        android.widget.FrameLayout scrim = UiKit.wrapScrim(this, root, 0.85f);
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(
                this, R.style.Theme_PlainPhone_RoundedDialog)
                .setView(scrim)
                .create();

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

        UiKit.finishCentered(dialog, scrim);
    }

    private void showFileOptions(String title, Runnable openWith, Runnable revealInFileManager) {
        Typeface georgia = Fonts.current(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(popupBackground());
        root.setPadding(0, 8, 0, 8);

        root.addView(UiKit.dialogTitle(this, title));

        android.widget.FrameLayout scrim = UiKit.wrapScrim(this, root, 0.85f);
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(
                this, R.style.Theme_PlainPhone_RoundedDialog)
                .setView(scrim)
                .create();

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

        UiKit.finishCentered(dialog, scrim);
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

