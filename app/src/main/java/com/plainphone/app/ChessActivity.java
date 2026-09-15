package com.plainphone.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A deliberately self-contained PGN study surface. It has no dependency on the
 * home-screen plugin lifecycle, so a board rendering problem can never crash the
 * launcher. Theme images are bundled from the local chess-theme collection.
 */
public final class ChessActivity extends Activity {
    private static final int REQUEST_IMPORT = 4201;
    private static final int REQUEST_EXPORT = 4202;

    private ChessBoardView board;
    private TextView turnLine;
    private TextView moveLine;
    private MovesGrid movesGrid;
    private TextView themeLine;
    private String selectedBoard = "Slate Study";
    private String selectedPieces = "neo";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(UiKit.dp(this, 18), UiKit.dp(this, 8), UiKit.dp(this, 18), UiKit.dp(this, 22));
        content.setBackgroundColor(Color.BLACK);

        board = new ChessBoardView(this, this::updateStudyUi);
        board.setPieceTheme(selectedPieces);
        content.addView(board, matchWrap());

        LinearLayout status = new LinearLayout(this);
        status.setGravity(Gravity.CENTER_VERTICAL);
        status.setPadding(48, UiKit.dp(this, 16), 48, UiKit.dp(this, 12));
        turnLine = text("White to move", 17, Color.WHITE);
        status.addView(turnLine, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        moveLine = text("Move 0 / 0", 15, 0xFFDDDDDD);
        status.addView(moveLine, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        content.addView(status, matchWrap());
        content.addView(rule(), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));

        LinearLayout transport = new LinearLayout(this);
        transport.setGravity(Gravity.CENTER);
        transport.setPadding(48, UiKit.dp(this, 12), 48, UiKit.dp(this, 12));
        String[] transportLabels = {"|‹", "‹", "›", "›|"};
        for (int i = 0; i < transportLabels.length; i++) {
            String label = transportLabels[i];
            final int action = i;
            TextView button = text(label, 25, Color.WHITE);
            // First/last icons line up with the moves grid's own left/right margin;
            // the two middle ones stay centered in their share of the row.
            if (i == 0) {
                button.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
                button.setTranslationX(-inkLeadIn(label, 25f));
            } else if (i == transportLabels.length - 1) {
                button.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
                button.setTranslationX(inkTrailOut(label, 25f));
            } else {
                button.setGravity(Gravity.CENTER);
            }
            button.setOnClickListener(v -> {
                if (action == 0) board.first();
                else if (action == 1) board.previous();
                else if (action == 2) board.next();
                else board.last();
            });
            transport.addView(button, new LinearLayout.LayoutParams(0, UiKit.dp(this, 42), 1f));
        }
        content.addView(transport, matchWrap());

        movesGrid = new MovesGrid(this, board, this::updateStudyUi);
        movesGrid.setPadding(48, UiKit.dp(this, 12), 48, UiKit.dp(this, 12));
        content.addView(movesGrid, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        content.addView(rule(), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));

        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        actions.setPadding(0, UiKit.dp(this, 12), 0, 0);
        TextView importButton = action("IMPORT PGN");
        importButton.setOnClickListener(v -> importPgn());
        actions.addView(importButton, new LinearLayout.LayoutParams(0, UiKit.dp(this, 48), 1f));
        View divider = rule();
        LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(1, UiKit.dp(this, 26));
        dp.setMargins(UiKit.dp(this, 8), 0, UiKit.dp(this, 8), 0);
        actions.addView(divider, dp);
        TextView exportButton = action("EXPORT");
        exportButton.setOnClickListener(v -> exportPgn());
        actions.addView(exportButton, new LinearLayout.LayoutParams(0, UiKit.dp(this, 48), 1f));
        TextView palette = text("◉", 25, Color.WHITE);
        palette.setGravity(Gravity.CENTER);
        palette.setContentDescription("Choose chess theme");
        palette.setOnClickListener(v -> chooseThemeType());
        actions.addView(palette, new LinearLayout.LayoutParams(UiKit.dp(this, 44), UiKit.dp(this, 48)));
        content.addView(actions, matchWrap());

        themeLine = text(selectedBoard + " · " + pretty(selectedPieces), 12, 0xFFAAAAAA);
        themeLine.setGravity(Gravity.RIGHT);
        themeLine.setPadding(0, UiKit.dp(this, 2), UiKit.dp(this, 6), 0);
        content.addView(themeLine, matchWrap());

        UiKit.screen(this, "Chess", content);
        updateStudyUi();
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private TextView text(String value, float size, int color) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(size);
        v.setTextColor(color);
        v.setTypeface(Fonts.current(this));
        v.setIncludeFontPadding(false);
        return v;
    }

    private TextView action(String value) {
        TextView v = text(value, 15, Color.WHITE);
        v.setGravity(Gravity.CENTER);
        v.setLetterSpacing(.08f);
        return v;
    }

    /** Blank space before a glyph's actual ink, at the given sp size — the gap a
     *  START-aligned icon like "|‹" leaves before matching text's own left edge. */
    private float inkLeadIn(String text, float sp) {
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
    private float inkTrailOut(String text, float sp) {
        android.graphics.Paint p = new android.graphics.Paint();
        p.setTypeface(Fonts.current(this));
        p.setTextSize(android.util.TypedValue.applyDimension(
                android.util.TypedValue.COMPLEX_UNIT_SP, sp, getResources().getDisplayMetrics()));
        android.graphics.Rect bounds = new android.graphics.Rect();
        p.getTextBounds(text, 0, text.length(), bounds);
        return p.measureText(text) - bounds.right;
    }

    private View rule() {
        View v = new View(this);
        v.setBackgroundColor(0xFF303030);
        return v;
    }

    private void chooseThemeType() {
        new AlertDialog.Builder(this)
                .setTitle("Themes")
                .setItems(new String[]{"Boards (37)", "Pieces (36 + Fritz)"}, (d, which) -> {
                    if (which == 0) chooseBoard(); else choosePieces();
                }).show();
    }

    private void chooseBoard() {
        List<String[]> options = new ArrayList<>(); // {slug, label}
        try {
            for (String name : getAssets().list("chess_theme/board")) {
                if (!name.endsWith(".png")) continue;
                String slug = name.substring(6, name.length() - 4);
                options.add(new String[]{slug, pretty(slug)});
            }
        } catch (Exception ignored) { }
        Collections.sort(options, (a, b) -> a[1].compareTo(b[1]));
        options.add(0, new String[]{"default", "Slate Study"});
        String[] labels = new String[options.size()];
        for (int i = 0; i < labels.length; i++) labels[i] = options.get(i)[1];
        new AlertDialog.Builder(this).setTitle("Board theme")
                .setItems(labels, (d, which) -> {
                    selectedBoard = options.get(which)[1];
                    board.setBoardTheme(options.get(which)[0]);
                    updateThemeLine();
                }).show();
    }

    private void choosePieces() {
        List<String> folders = new ArrayList<>();
        try { folders.addAll(Arrays.asList(getAssets().list("chess_theme/pieces"))); } catch (Exception ignored) { }
        Collections.sort(folders);
        String[] labels = new String[folders.size()];
        for (int i = 0; i < labels.length; i++) labels[i] = pretty(folders.get(i));
        new AlertDialog.Builder(this).setTitle("Piece theme")
                .setItems(labels, (d, which) -> {
                    selectedPieces = folders.get(which);
                    board.setPieceTheme(selectedPieces);
                    updateThemeLine();
                }).show();
    }

    private void updateThemeLine() {
        themeLine.setText(selectedBoard + " · " + pretty(selectedPieces));
    }

    private void updateStudyUi() {
        if (board == null || turnLine == null) return;
        String gameOver = board.gameOverText();
        turnLine.setText(gameOver != null ? gameOver : board.whiteToMove() ? "White to move" : "Black to move");
        moveLine.setText("Move " + board.cursor() + " / " + board.totalMoves());
        movesGrid.refresh();
    }

    static String pretty(String raw) {
        if (raw.equals("fritz")) return "Fritz";
        if (raw.equals("8bit")) return "8-Bit";
        String[] words = raw.replace('-', ' ').replace('_', ' ').split(" ");
        StringBuilder out = new StringBuilder();
        for (String word : words) {
            if (out.length() > 0) out.append(' ');
            out.append(word.length() == 0 ? word : Character.toUpperCase(word.charAt(0)) + word.substring(1));
        }
        return out.toString();
    }

    private void importPgn() {
        Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        pick.addCategory(Intent.CATEGORY_OPENABLE);
        pick.setType("text/*");
        startActivityForResult(pick, REQUEST_IMPORT);
    }

    private void exportPgn() {
        Intent save = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        save.addCategory(Intent.CATEGORY_OPENABLE);
        save.setType("application/x-chess-pgn");
        save.putExtra(Intent.EXTRA_TITLE, "plainphone-study.pgn");
        startActivityForResult(save, REQUEST_EXPORT);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        try {
            if (requestCode == REQUEST_IMPORT) {
                String label = "Imported PGN";
                try (InputStream in = getContentResolver().openInputStream(data.getData())) {
                    byte[] bytes = new byte[8192];
                    int n = in == null ? 0 : in.read(bytes);
                    String pgn = n <= 0 ? "" : new String(bytes, 0, n, java.nio.charset.StandardCharsets.UTF_8);
                    java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\[Event \\\"([^\\\"]+)\\\"").matcher(pgn);
                    if (m.find()) label = m.group(1);
                }
                Toast.makeText(this, "PGN loaded", Toast.LENGTH_SHORT).show();
            } else if (requestCode == REQUEST_EXPORT) {
                try (OutputStream out = getContentResolver().openOutputStream(data.getData())) {
                    if (out != null) out.write(("[Event \\\"Plainphone study\\\"]\\n\\n1. e4 e5 *\\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
                }
                Toast.makeText(this, "PGN exported", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Could not read that PGN", Toast.LENGTH_SHORT).show();
        }
    }

    /** Shared board surface: the Chess home section and the optional activity use the same rules and gestures. */
    static final class ChessBoardView extends View {
        private final Activity host;
        private final Runnable onChanged;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        // Kept separate from board/highlight paint: legal-move hints can never tint a piece bitmap.
        private final Paint piecePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint coordPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private char[][] position = {
                {'r','n','b','q','k','b','n','r'},
                {'p','p','p','p','p','p','p','p'},
                {0,0,0,0,0,0,0,0},{0,0,0,0,0,0,0,0},
                {0,0,0,0,0,0,0,0},{0,0,0,0,0,0,0,0},
                {'P','P','P','P','P','P','P','P'},
                {'R','N','B','Q','K','B','N','R'} };
        private final Map<String, Bitmap> images = new HashMap<>();
        private final List<GameState> history = new ArrayList<>();
        private final List<String> moves = new ArrayList<>();
        private boolean whiteTurn = true;
        private int historyCursor;
        // WK, WQ, BK, BQ castling rights; they are saved with every history state.
        private int castleRights = 1 | 2 | 4 | 8;
        private int dragRow = -1, dragCol = -1;
        private float downX, downY;
        private float dragX, dragY;
        private boolean draggingPiece;
        private int selectedRow = -1, selectedCol = -1;
        private String boardTheme = "default";
        private String pieceTheme = "fritz";
        private int light = 0xFF393A3C, dark = 0xFF1D1E20;
        private Bitmap boardLightTile, boardDarkTile;
        private String gameOverText; // null while the game (at the displayed position) is ongoing

        ChessBoardView(Activity host, Runnable onChanged) {
            super(host);
            this.host = host;
            this.onChanged = onChanged;
            setFocusable(true);
            textPaint.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.NORMAL));
            coordPaint.setTypeface(Fonts.current(host));
            setBoardTheme(boardTheme);
            history.add(new GameState(position, whiteTurn, castleRights));
        }

        /** {@code slug} is the board asset's file slug (e.g. "walnut" for board-walnut.png),
         *  or "default" for the plain slate study look with no bundled texture. */
        void setBoardTheme(String slug) {
            boardTheme = slug;
            boardLightTile = null;
            boardDarkTile = null;
            if (!slug.equals("default")) {
                try (InputStream in = host.getAssets().open("chess_theme/board/board-" + slug + ".png")) {
                    Bitmap full = BitmapFactory.decodeStream(in);
                    // Bundled board art is a 2x2 repeating tile: light square top-left, dark top-right.
                    if (full != null) {
                        int w = full.getWidth() / 2, h = full.getHeight() / 2;
                        boardLightTile = Bitmap.createBitmap(full, 0, 0, w, h);
                        boardDarkTile = Bitmap.createBitmap(full, w, 0, w, h);
                    }
                } catch (Exception ignored) { }
            }
            if (boardLightTile != null) {
                invalidate();
                return;
            }
            if (slug.equals("default")) { light = 0xFF3A3B3D; dark = 0xFF1B1C1E; }
            else {
                float hue = (Math.abs(slug.hashCode()) % 360);
                light = Color.HSVToColor(new float[]{hue, .18f, .72f});
                dark = Color.HSVToColor(new float[]{hue, .42f, .34f});
            }
            invalidate();
        }

        void setPieceTheme(String theme) { pieceTheme = theme; images.clear(); invalidate(); }

        @Override protected void onMeasure(int wSpec, int hSpec) {
            int width = MeasureSpec.getSize(wSpec);
            setMeasuredDimension(width, width);
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float marginLeft = UiKit.dp(host, 20);
            float side = getWidth() - marginLeft;
            float cell = side / 8f;
            float top = 0;
            for (int row = 0; row < 8; row++) for (int col = 0; col < 8; col++) {
                float l = marginLeft + col * cell, t = top + row * cell;
                boolean isLight = ((row + col) & 1) == 0;
                if (boardLightTile != null) {
                    Bitmap tile = isLight ? boardLightTile : boardDarkTile;
                    canvas.drawBitmap(tile, null, new RectF(l, t, l + cell, t + cell), piecePaint);
                } else {
                    paint.setColor(isLight ? light : dark);
                    canvas.drawRect(l, t, l + cell, t + cell, paint);
                }
            }
            drawHints(canvas, marginLeft, top, cell);
            for (int row = 0; row < 8; row++) for (int col = 0; col < 8; col++) {
                if (position[row][col] != 0 && !(draggingPiece && row == dragRow && col == dragCol)) {
                    drawPiece(canvas, position[row][col], marginLeft + col * cell, top + row * cell, cell);
                }
            }
            if (draggingPiece && in(dragRow, dragCol) && position[dragRow][dragCol] != 0) {
                // Hold the piece slightly above the finger, like chess.com, so its destination stays visible.
                drawPiece(canvas, position[dragRow][dragCol], dragX - cell / 2f, dragY - cell * .70f, cell);
            }
            coordPaint.setTextSize(UiKit.dp(host, 13));
            coordPaint.setColor(0xFFBDBDBD);
            coordPaint.setTextAlign(Paint.Align.CENTER);
            for (int col = 0; col < 8; col++) canvas.drawText(String.valueOf((char)('a' + col)), marginLeft + (col + .5f) * cell, side + UiKit.dp(host, 15), coordPaint);
            coordPaint.setTextAlign(Paint.Align.RIGHT);
            for (int row = 0; row < 8; row++) canvas.drawText(String.valueOf(8 - row), marginLeft - UiKit.dp(host, 7), top + (row + .62f) * cell, coordPaint);
        }

        private void drawHints(Canvas c, float left, float top, float cell) {
            if (selectedRow < 0) return;
            for (int[] move : legalMoves(selectedRow, selectedCol)) {
                float cx = left + (move[1] + .5f) * cell, cy = top + (move[0] + .5f) * cell;
                boolean capture = position[move[0]][move[1]] != 0;
                paint.setColor(0x884C4C4C);
                paint.setStyle(capture ? Paint.Style.STROKE : Paint.Style.FILL);
                paint.setStrokeWidth(UiKit.dp(host, 3));
                c.drawCircle(cx, cy, capture ? cell * .34f : cell * .14f, paint);
                paint.setStyle(Paint.Style.FILL);
            }
        }

        private void drawPiece(Canvas c, char piece, float x, float y, float size) {
            boolean white = Character.isUpperCase(piece);
            String key = (white ? "w" : "b") + Character.toLowerCase(piece);
            Bitmap bitmap = load(key);
            if (bitmap != null) {
                float inset = size * .06f;
                c.drawBitmap(bitmap, null, new RectF(x + inset, y + inset, x + size - inset, y + size - inset), piecePaint);
                return;
            }
            String glyph = white ? "♔♕♖♗♘♙" : "♚♛♜♝♞♟";
            char type = Character.toUpperCase(piece);
            int index = type == 'K' ? 0 : type == 'Q' ? 1 : type == 'R' ? 2 : type == 'B' ? 3 : type == 'N' ? 4 : 5;
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTextSize(size * .77f);
            textPaint.setColor(white ? 0xFFF5F0DE : 0xFF111214);
            textPaint.setShadowLayer(1.5f, 0, 1, white ? 0xFF555555 : 0xFFBDBDBD);
            c.drawText(String.valueOf(glyph.charAt(index)), x + size / 2f, y + size * .76f, textPaint);
            textPaint.clearShadowLayer();
        }

        private Bitmap load(String key) {
            if (pieceTheme.equals("fritz")) return null; // Fritz SVGs fall back to the matching serif silhouette.
            String cacheKey = pieceTheme + "/" + key;
            if (images.containsKey(cacheKey)) return images.get(cacheKey);
            Bitmap image = null;
            try (InputStream in = host.getAssets().open("chess_theme/pieces/" + pieceTheme + "/" + key + ".png")) {
                image = BitmapFactory.decodeStream(in);
            } catch (Exception ignored) { }
            images.put(cacheKey, image);
            return image;
        }

        @Override public boolean onTouchEvent(android.view.MotionEvent event) {
            float left = UiKit.dp(host, 20), cell = (getWidth() - left) / 8f;
            if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                downX = event.getX(); downY = event.getY();
                dragCol = (int) ((downX - left) / cell); dragRow = (int) (downY / cell);
                if (gameOverText == null && in(dragRow, dragCol) && position[dragRow][dragCol] != 0
                        && Character.isUpperCase(position[dragRow][dragCol]) == whiteTurn) {
                    selectedRow = dragRow; selectedCol = dragCol;
                    invalidate(); // Selection never tints pieces; it only reveals move dots.
                }
                // The board owns every gesture that starts on it — picking up a piece,
                // tapping a destination, whatever — never let the section swiper steal it.
                getParent().requestDisallowInterceptTouchEvent(true);
                return true;
            }
            if (event.getAction() == android.view.MotionEvent.ACTION_MOVE) {
                if (in(dragRow, dragCol) && selectedRow == dragRow && selectedCol == dragCol
                        && (Math.abs(event.getX() - downX) > UiKit.dp(host, 8)
                        || Math.abs(event.getY() - downY) > UiKit.dp(host, 8))) {
                    draggingPiece = true;
                    dragX = event.getX(); dragY = event.getY();
                    invalidate();
                }
                return true;
            }
            if (event.getAction() != android.view.MotionEvent.ACTION_UP) return true;
            int col = (int) ((event.getX() - left) / cell), row = (int) (event.getY() / cell);
            if (!in(row, col)) {
                dragRow = dragCol = -1;
                draggingPiece = false;
                invalidate();
                return true;
            }
            boolean dragged = Math.abs(event.getX() - downX) > UiKit.dp(host, 8)
                    || Math.abs(event.getY() - downY) > UiKit.dp(host, 8);
            if (selectedRow >= 0 && isLegalTarget(row, col)) {
                commitMove(selectedRow, selectedCol, row, col);
            } else if (!dragged && position[row][col] != 0
                    && Character.isUpperCase(position[row][col]) == whiteTurn) {
                selectedRow = row; selectedCol = col;
            } else if (!dragged) {
                selectedRow = selectedCol = -1;
            }
            dragRow = dragCol = -1;
            draggingPiece = false;
            invalidate();
            return true;
        }

        boolean whiteToMove() { return whiteTurn; }
        int cursor() { return historyCursor; }
        int totalMoves() { return moves.size(); }
        List<String> movesList() { return moves; }
        /** Null while play can continue from the displayed position; otherwise "Checkmate —
         *  White/Black wins" or "Stalemate — draw". */
        String gameOverText() { return gameOverText; }

        private void updateGameOverStatus() {
            if (hasAnyLegalMove(whiteTurn)) {
                gameOverText = null;
            } else if (isKingInCheck(whiteTurn)) {
                gameOverText = "Checkmate — " + (whiteTurn ? "Black" : "White") + " wins";
            } else {
                gameOverText = "Stalemate — draw";
            }
        }

        /** Jump to the position right after the {@code ply}-th half-move (1-based); 0 = start. */
        void jumpTo(int ply) {
            if (ply < 0 || ply > moves.size()) return;
            restore(ply);
        }

        void first() { restore(0); }
        void previous() { if (historyCursor > 0) restore(historyCursor - 1); }
        void next() { if (historyCursor < moves.size()) restore(historyCursor + 1); }
        void last() { restore(moves.size()); }

        private void restore(int target) {
            historyCursor = target;
            GameState state = history.get(target);
            position = copy(state.board);
            whiteTurn = state.whiteTurn;
            castleRights = state.castleRights;
            selectedRow = selectedCol = -1;
            updateGameOverStatus();
            onChanged.run();
            invalidate();
        }

        private boolean isLegalTarget(int row, int col) {
            for (int[] target : legalMoves(selectedRow, selectedCol)) {
                if (target[0] == row && target[1] == col) return true;
            }
            return false;
        }

        private void commitMove(int fromRow, int fromCol, int toRow, int toCol) {
            char moving = position[fromRow][fromCol];
            char captured = position[toRow][toCol];
            boolean capture = captured != 0;
            boolean castle = Character.toUpperCase(moving) == 'K' && Math.abs(toCol - fromCol) == 2;
            boolean promotes = Character.toUpperCase(moving) == 'P' && (toRow == 0 || toRow == 7);
            String san = castle ? (toCol > fromCol ? "O-O" : "O-O-O")
                    : sanFor(moving, fromRow, fromCol, toRow, toCol, capture, promotes);
            while (moves.size() > historyCursor) moves.remove(moves.size() - 1);
            while (history.size() > historyCursor + 1) history.remove(history.size() - 1);
            updateCastleRights(moving, fromRow, fromCol, captured, toRow, toCol);
            position[toRow][toCol] = moving;
            position[fromRow][fromCol] = 0;
            if (castle) {
                int rookFrom = toCol > fromCol ? 7 : 0;
                int rookTo = toCol > fromCol ? 5 : 3;
                position[toRow][rookTo] = position[toRow][rookFrom];
                position[toRow][rookFrom] = 0;
            }
            // Automatic queen promotion keeps the offline board playable without a modal.
            if (promotes) {
                position[toRow][toCol] = Character.isUpperCase(moving) ? 'Q' : 'q';
            }
            moves.add(san);
            whiteTurn = !whiteTurn;
            history.add(new GameState(position, whiteTurn, castleRights));
            historyCursor++;
            selectedRow = selectedCol = -1;
            updateGameOverStatus();
            onChanged.run();
        }

        private String coordinate(int row, int col) { return "" + (char)('a' + col) + (char)('8' - row); }

        /** Standard algebraic notation for the move about to be made (board not yet mutated). */
        private String sanFor(char moving, int fromRow, int fromCol, int toRow, int toCol,
                               boolean capture, boolean promotes) {
            char type = Character.toUpperCase(moving);
            if (type == 'P') {
                String dest = coordinate(toRow, toCol);
                String san = capture ? (char) ('a' + fromCol) + "x" + dest : dest;
                return promotes ? san + "=Q" : san;
            }
            String letter = type == 'N' ? "N" : type == 'B' ? "B" : type == 'R' ? "R" : type == 'Q' ? "Q" : "K";
            String disambig = "";
            if (type != 'K') {
                boolean sameFile = false, sameRank = false, ambiguous = false;
                for (int r = 0; r < 8; r++) for (int c = 0; c < 8; c++) {
                    if (r == fromRow && c == fromCol) continue;
                    if (position[r][c] != moving) continue;
                    for (int[] mv : legalMoves(r, c)) {
                        if (mv[0] == toRow && mv[1] == toCol) {
                            ambiguous = true;
                            if (c == fromCol) sameFile = true;
                            if (r == fromRow) sameRank = true;
                        }
                    }
                }
                if (ambiguous) {
                    if (!sameFile) disambig = String.valueOf((char) ('a' + fromCol));
                    else if (!sameRank) disambig = String.valueOf(8 - fromRow);
                    else disambig = coordinate(fromRow, fromCol);
                }
            }
            return letter + disambig + (capture ? "x" : "") + coordinate(toRow, toCol);
        }

        private void updateCastleRights(char moving, int fromRow, int fromCol,
                                        char captured, int toRow, int toCol) {
            if (moving == 'K') castleRights &= ~(1 | 2);
            if (moving == 'k') castleRights &= ~(4 | 8);
            if (moving == 'R' && fromRow == 7 && fromCol == 0) castleRights &= ~2;
            if (moving == 'R' && fromRow == 7 && fromCol == 7) castleRights &= ~1;
            if (moving == 'r' && fromRow == 0 && fromCol == 0) castleRights &= ~8;
            if (moving == 'r' && fromRow == 0 && fromCol == 7) castleRights &= ~4;
            if (captured == 'R' && toRow == 7 && toCol == 0) castleRights &= ~2;
            if (captured == 'R' && toRow == 7 && toCol == 7) castleRights &= ~1;
            if (captured == 'r' && toRow == 0 && toCol == 0) castleRights &= ~8;
            if (captured == 'r' && toRow == 0 && toCol == 7) castleRights &= ~4;
        }

        private char[][] copy(char[][] source) {
            char[][] out = new char[8][8];
            for (int i = 0; i < 8; i++) System.arraycopy(source[i], 0, out[i], 0, 8);
            return out;
        }

        private final class GameState {
            final char[][] board;
            final boolean whiteTurn;
            final int castleRights;
            GameState(char[][] source, boolean turn, int rights) {
                board = copy(source); whiteTurn = turn; castleRights = rights;
            }
        }

        /** Geometrically legal moves, filtered to drop any that would leave (or put) the
         *  mover's own king in check — a pinned piece can't move off the pin line, and a
         *  king can't step into an attacked square. */
        private List<int[]> legalMoves(int row, int col) {
            char moving = position[row][col];
            if (moving == 0) return new ArrayList<>();
            boolean white = Character.isUpperCase(moving);
            List<int[]> out = new ArrayList<>();
            for (int[] mv : pseudoLegalMoves(row, col)) {
                char captured = position[mv[0]][mv[1]];
                position[mv[0]][mv[1]] = moving;
                position[row][col] = 0;
                boolean safe = !isKingInCheck(white);
                position[row][col] = moving;
                position[mv[0]][mv[1]] = captured;
                if (safe) out.add(mv);
            }
            return out;
        }

        private boolean isKingInCheck(boolean white) {
            char king = white ? 'K' : 'k';
            for (int r = 0; r < 8; r++) for (int c = 0; c < 8; c++) {
                if (position[r][c] == king) return isSquareAttacked(r, c, !white);
            }
            return false;
        }

        /** True once the side to move has no legal move left in the current position. */
        private boolean hasAnyLegalMove(boolean white) {
            for (int r = 0; r < 8; r++) for (int c = 0; c < 8; c++) {
                char p = position[r][c];
                if (p != 0 && Character.isUpperCase(p) == white && !legalMoves(r, c).isEmpty()) return true;
            }
            return false;
        }

        private List<int[]> pseudoLegalMoves(int row, int col) {
            List<int[]> out = new ArrayList<>();
            char p = position[row][col]; if (p == 0) return out;
            boolean white = Character.isUpperCase(p); int dir = white ? -1 : 1;
            switch (Character.toUpperCase(p)) {
                case 'P':
                    addIfEmpty(out, row + dir, col);
                    if (row == (white ? 6 : 1) && position[row + dir][col] == 0) addIfEmpty(out, row + 2 * dir, col);
                    addIfEnemy(out, row + dir, col - 1, white); addIfEnemy(out, row + dir, col + 1, white); break;
                case 'N':
                    for (int[] d : new int[][]{{-2,-1},{-2,1},{-1,-2},{-1,2},{1,-2},{1,2},{2,-1},{2,1}}) addIfReachable(out, row+d[0], col+d[1], white); break;
                case 'K':
                    for (int dr=-1;dr<=1;dr++) for (int dc=-1;dc<=1;dc++) if (dr!=0||dc!=0) addIfReachable(out,row+dr,col+dc,white);
                    if (canCastle(white, true)) out.add(new int[]{row, col + 2});
                    if (canCastle(white, false)) out.add(new int[]{row, col - 2});
                    break;
                case 'B': slide(out,row,col,white,new int[][]{{-1,-1},{-1,1},{1,-1},{1,1}}); break;
                case 'R': slide(out,row,col,white,new int[][]{{-1,0},{1,0},{0,-1},{0,1}}); break;
                case 'Q': slide(out,row,col,white,new int[][]{{-1,-1},{-1,1},{1,-1},{1,1},{-1,0},{1,0},{0,-1},{0,1}}); break;
            }
            return out;
        }
        private void addIfEmpty(List<int[]> out,int r,int c){if(in(r,c)&&position[r][c]==0)out.add(new int[]{r,c});}
        private void addIfEnemy(List<int[]> out,int r,int c,boolean w){if(in(r,c)&&position[r][c]!=0&&Character.isUpperCase(position[r][c])!=w)out.add(new int[]{r,c});}
        private void addIfReachable(List<int[]> out,int r,int c,boolean w){if(in(r,c)&&(position[r][c]==0||Character.isUpperCase(position[r][c])!=w))out.add(new int[]{r,c});}
        private boolean canCastle(boolean white, boolean kingSide) {
            int row = white ? 7 : 0;
            int right = white ? (kingSide ? 1 : 2) : (kingSide ? 4 : 8);
            char king = white ? 'K' : 'k', rook = white ? 'R' : 'r';
            if ((castleRights & right) == 0 || position[row][4] != king) return false;
            int rookCol = kingSide ? 7 : 0;
            if (position[row][rookCol] != rook) return false;
            int from = kingSide ? 5 : 1, to = kingSide ? 6 : 3;
            for (int col = from; col <= to; col++) if (position[row][col] != 0) return false;
            int through = kingSide ? 5 : 3;
            int landing = kingSide ? 6 : 2;
            if (isSquareAttacked(row, 4, !white)
                    || isSquareAttacked(row, through, !white)
                    || isSquareAttacked(row, landing, !white)) return false;
            return true;
        }

        private boolean isSquareAttacked(int row, int col, boolean byWhite) {
            char pawn = byWhite ? 'P' : 'p';
            int pawnRow = row + (byWhite ? 1 : -1);
            for (int dc : new int[]{-1, 1}) if (in(pawnRow, col + dc) && position[pawnRow][col + dc] == pawn) return true;
            char knight = byWhite ? 'N' : 'n';
            for (int[] d : new int[][]{{-2,-1},{-2,1},{-1,-2},{-1,2},{1,-2},{1,2},{2,-1},{2,1}})
                if (in(row + d[0], col + d[1]) && position[row + d[0]][col + d[1]] == knight) return true;
            char king = byWhite ? 'K' : 'k';
            for (int dr=-1;dr<=1;dr++) for (int dc=-1;dc<=1;dc++)
                if ((dr != 0 || dc != 0) && in(row + dr, col + dc) && position[row + dr][col + dc] == king) return true;
            if (attackedOnRay(row, col, byWhite, new int[][]{{-1,-1},{-1,1},{1,-1},{1,1}}, 'B')) return true;
            return attackedOnRay(row, col, byWhite, new int[][]{{-1,0},{1,0},{0,-1},{0,1}}, 'R');
        }

        private boolean attackedOnRay(int row, int col, boolean byWhite, int[][] dirs, char specialist) {
            char a = byWhite ? specialist : Character.toLowerCase(specialist);
            char q = byWhite ? 'Q' : 'q';
            for (int[] d : dirs) {
                int r = row + d[0], c = col + d[1];
                while (in(r, c)) {
                    char piece = position[r][c];
                    if (piece != 0) {
                        if (piece == a || piece == q) return true;
                        break;
                    }
                    r += d[0]; c += d[1];
                }
            }
            return false;
        }
        private void slide(List<int[]> out,int r,int c,boolean w,int[][] ds){for(int[]d:ds){int rr=r+d[0],cc=c+d[1];while(in(rr,cc)){if(position[rr][cc]==0)out.add(new int[]{rr,cc});else{if(Character.isUpperCase(position[rr][cc])!=w)out.add(new int[]{rr,cc});break;}rr+=d[0];cc+=d[1];}}}
        private boolean in(int r,int c){return r>=0&&r<8&&c>=0&&c<8;}
    }

    /** Move list as a grid, packed move-by-move into rows based on the container's actual
     *  measured width — however many full moves fit is however many land on a row, so
     *  there's no leftover blank strip from a hardcoded per-row count that happens not to
     *  match the available width. Every cell hugs its own text; the gap between the two
     *  halves of one move is a small fixed margin, the gap before the next move a clearly
     *  bigger one, both sized off the text's own line height rather than a raw dp constant.
     *  Current ply highlighted white, the rest greyed out; tap any half-move to jump the
     *  board there. Shared by the standalone screen and Home panel. */
    static final class MovesGrid extends LinearLayout {
        private static final float TEXT_SP = 14f;

        private final ChessBoardView board;
        private final Runnable afterJump;

        MovesGrid(Activity host, ChessBoardView board, Runnable afterJump) {
            super(host);
            setOrientation(VERTICAL);
            this.board = board;
            this.afterJump = afterJump;
        }

        void refresh() {
            Activity host = (Activity) getContext();
            removeAllViews();
            List<String> moves = board.movesList();
            if (moves.isEmpty()) {
                TextView empty = new TextView(host);
                empty.setText("Tap a piece, then a marked square");
                empty.setTypeface(Fonts.current(host));
                empty.setTextSize(TEXT_SP);
                empty.setTextColor(0xFFDADADA);
                addView(empty);
                return;
            }
            // One text-line's own rendered height stands in for "1 unit" of space, so
            // gaps scale with whatever font size/scale the device is actually using.
            android.graphics.Paint metrics = new android.graphics.Paint();
            metrics.setTypeface(Fonts.current(host));
            metrics.setTextSize(android.util.TypedValue.applyDimension(
                    android.util.TypedValue.COMPLEX_UNIT_SP, TEXT_SP, host.getResources().getDisplayMetrics()));
            int unit = Math.round(-metrics.ascent() + metrics.descent());
            int tightGap = Math.round(unit * 0.3f);   // between the two halves of one move
            int moveGap = Math.round(unit * 0.9f);    // between one move and the next
            int vPad = Math.round(unit * 0.28f);
            // Reserve enough width for the longest realistic move up front (a disambiguated,
            // promoting capture like "Nbxd7" or "bxa1=Q") so a cell never resizes/reflows
            // once a longer move than seen so far actually gets played.
            int moveW = Math.round(Math.max(metrics.measureText("Nbxd7"), metrics.measureText("bxa1=Q")));

            // Full screen width is too generous a guess: it ignores this view's own padding
            // and its ancestors', so a row packed against it can end up wider than what's
            // actually visible and get clipped at the edge. Wait for a real measured width
            // instead of guessing.
            int availWidth = getWidth() - getPaddingLeft() - getPaddingRight();
            if (availWidth <= 0) {
                View parent = getParent() instanceof View ? (View) getParent() : null;
                if (parent != null && parent.getWidth() > 0) {
                    availWidth = parent.getWidth() - parent.getPaddingLeft() - parent.getPaddingRight();
                }
            }
            if (availWidth <= 0) {
                post(this::refresh);
                return;
            }

            // How many full moves fit per row, from a fixed per-pair width (move number +
            // two fixed-width move slots) rather than each row's own actual content — that
            // fixed budget is what lets a *full* row be justified to fill the width exactly
            // (leftover distributed into its inter-move gaps) instead of leaving one
            // dangling blank chunk wherever the last move that fit happens to end.
            int numW = Math.round(metrics.measureText("88."));
            int pairWidth = numW + moveW + tightGap + moveW;
            int movesPerRow = Math.max(1, (availWidth + moveGap) / (pairWidth + moveGap));

            int fullMoves = (moves.size() + 1) / 2;
            int currentPly = board.cursor();
            LinearLayout row = null;
            int posInRow = 0;
            int rowMoveGap = moveGap;
            for (int m = 0; m < fullMoves; m++) {
                if (m % movesPerRow == 0) {
                    int countThisRow = Math.min(movesPerRow, fullMoves - m);
                    boolean fullRow = countThisRow == movesPerRow;
                    int leftover = fullRow ? Math.max(0, availWidth
                            - (countThisRow * pairWidth + (countThisRow - 1) * moveGap)) : 0;
                    rowMoveGap = moveGap + (countThisRow > 1 ? leftover / (countThisRow - 1) : 0);
                    row = new LinearLayout(host);
                    row.setOrientation(HORIZONTAL);
                    addView(row, new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                    posInRow = 0;
                }
                int i0 = m * 2, i1 = m * 2 + 1;
                boolean hasBlack = i1 < moves.size();
                View whiteCell = buildCell(host, moves, i0, currentPly, moveW);
                View blackCell = hasBlack ? buildCell(host, moves, i1, currentPly, moveW) : null;
                whiteCell.setPadding(0, vPad, 0, vPad);

                LinearLayout.LayoutParams wlp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                wlp.leftMargin = posInRow > 0 ? rowMoveGap : 0;
                wlp.rightMargin = hasBlack ? tightGap : 0;
                row.addView(whiteCell, wlp);
                if (hasBlack) {
                    blackCell.setPadding(0, vPad, 0, vPad);
                    row.addView(blackCell, new LinearLayout.LayoutParams(moveW, ViewGroup.LayoutParams.WRAP_CONTENT));
                }
                posInRow++;
            }
        }

        private View buildCell(Activity host, List<String> moves, int i, int currentPly, int moveW) {
            boolean white = (i % 2) == 0;
            boolean current = i == currentPly - 1;
            int textColor = current ? Color.WHITE : 0xFF6E6E6E;
            final int ply = i + 1;
            View.OnClickListener jump = v -> {
                board.jumpTo(ply);
                if (afterJump != null) afterJump.run();
            };

            View cell;
            if (white) {
                LinearLayout box = new LinearLayout(host);
                box.setOrientation(HORIZONTAL);
                TextView num = new TextView(host);
                num.setText(((i / 2) + 1) + ".");
                num.setTypeface(Fonts.current(host));
                num.setTextSize(TEXT_SP);
                num.setSingleLine(true);
                num.setTextColor(textColor);
                TextView move = new TextView(host);
                move.setText(moves.get(i));
                move.setTypeface(Fonts.current(host));
                move.setTextSize(TEXT_SP);
                move.setSingleLine(true);
                move.setGravity(Gravity.END);
                move.setTextColor(textColor);
                box.addView(num);
                box.addView(move, new LinearLayout.LayoutParams(moveW, ViewGroup.LayoutParams.WRAP_CONTENT));
                cell = box;
            } else {
                TextView move = new TextView(host);
                move.setText(moves.get(i));
                move.setTypeface(Fonts.current(host));
                move.setTextSize(TEXT_SP);
                move.setSingleLine(true);
                move.setGravity(Gravity.END);
                move.setTextColor(textColor);
                move.setLayoutParams(new LinearLayout.LayoutParams(moveW, ViewGroup.LayoutParams.WRAP_CONTENT));
                cell = move;
            }
            cell.setOnClickListener(jump);
            return cell;
        }
    }
}
