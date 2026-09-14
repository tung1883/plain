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
    private TextView movesLine;
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
        status.setPadding(0, UiKit.dp(this, 16), 0, UiKit.dp(this, 12));
        turnLine = text("White to move", 17, Color.WHITE);
        status.addView(turnLine, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        moveLine = text("Move 0 / 0", 15, 0xFFDDDDDD);
        status.addView(moveLine, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        content.addView(status, matchWrap());
        content.addView(rule(), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));

        LinearLayout transport = new LinearLayout(this);
        transport.setGravity(Gravity.CENTER);
        transport.setPadding(0, UiKit.dp(this, 12), 0, UiKit.dp(this, 12));
        String[] transportLabels = {"|‹", "‹", "›", "›|"};
        for (int i = 0; i < transportLabels.length; i++) {
            String label = transportLabels[i];
            final int action = i;
            TextView button = text(label, 25, Color.WHITE);
            button.setGravity(Gravity.CENTER);
            button.setOnClickListener(v -> {
                if (action == 0) board.first();
                else if (action == 1) board.previous();
                else if (action == 2) board.next();
                else board.last();
            });
            transport.addView(button, new LinearLayout.LayoutParams(0, UiKit.dp(this, 42), 1f));
        }
        content.addView(transport, matchWrap());

        movesLine = text("Tap a white piece to begin", 14, 0xFFDADADA);
        movesLine.setSingleLine(true);
        movesLine.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
        movesLine.setPadding(UiKit.dp(this, 4), UiKit.dp(this, 12), UiKit.dp(this, 4), UiKit.dp(this, 12));
        content.addView(movesLine, matchWrap());
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
        List<String> items = new ArrayList<>();
        items.add("Slate Study");
        try {
            for (String name : getAssets().list("chess_theme/board")) {
                if (name.endsWith(".png")) items.add(pretty(name.substring(6, name.length() - 4)));
            }
        } catch (Exception ignored) { }
        Collections.sort(items.subList(1, items.size()));
        new AlertDialog.Builder(this).setTitle("Board theme")
                .setItems(items.toArray(new String[0]), (d, which) -> {
                    selectedBoard = items.get(which);
                    board.setBoardTheme(selectedBoard);
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
        turnLine.setText(board.whiteToMove() ? "White to move" : "Black to move");
        moveLine.setText("Move " + board.cursor() + " / " + board.totalMoves());
        String text = board.moveText();
        movesLine.setText(text.length() == 0 ? "Tap a piece, then a marked square" : text);
    }

    private static String pretty(String raw) {
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
        private String boardTheme = "Slate Study";
        private String pieceTheme = "fritz";
        private int light = 0xFF393A3C, dark = 0xFF1D1E20;

        ChessBoardView(Activity host, Runnable onChanged) {
            super(host);
            this.host = host;
            this.onChanged = onChanged;
            setFocusable(true);
            textPaint.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.NORMAL));
            setBoardTheme(boardTheme);
            history.add(new GameState(position, whiteTurn, castleRights));
        }

        void setBoardTheme(String theme) {
            boardTheme = theme;
            if (theme.equals("Slate Study")) { light = 0xFF3A3B3D; dark = 0xFF1B1C1E; }
            else if (theme.equalsIgnoreCase("Walnut")) { light = 0xFFDBB58C; dark = 0xFF805137; }
            else if (theme.equalsIgnoreCase("Green")) { light = 0xFFF0E4C3; dark = 0xFF779556; }
            else if (theme.equalsIgnoreCase("Marble")) { light = 0xFFE6E0D5; dark = 0xFF8B8D91; }
            else {
                float hue = (Math.abs(theme.hashCode()) % 360);
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
            float marginBottom = UiKit.dp(host, 20);
            float side = getWidth() - marginLeft;
            float cell = side / 8f;
            float top = 0;
            for (int row = 0; row < 8; row++) for (int col = 0; col < 8; col++) {
                paint.setColor(((row + col) & 1) == 0 ? light : dark);
                canvas.drawRect(marginLeft + col * cell, top + row * cell,
                        marginLeft + (col + 1) * cell, top + (row + 1) * cell, paint);
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
            textPaint.setTextSize(UiKit.dp(host, 13));
            textPaint.setColor(0xFFBDBDBD);
            textPaint.setTextAlign(Paint.Align.CENTER);
            for (int col = 0; col < 8; col++) canvas.drawText(String.valueOf((char)('a' + col)), marginLeft + (col + .5f) * cell, side + UiKit.dp(host, 15), textPaint);
            textPaint.setTextAlign(Paint.Align.RIGHT);
            for (int row = 0; row < 8; row++) canvas.drawText(String.valueOf(8 - row), marginLeft - UiKit.dp(host, 7), top + (row + .62f) * cell, textPaint);
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
                if (in(dragRow, dragCol) && position[dragRow][dragCol] != 0
                        && Character.isUpperCase(position[dragRow][dragCol]) == whiteTurn) {
                    selectedRow = dragRow; selectedCol = dragCol;
                    invalidate(); // Selection never tints pieces; it only reveals move dots.
                }
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
            if (in(dragRow, dragCol) && selectedRow == dragRow && selectedCol == dragCol
                    && isLegalTarget(row, col)) {
                commitMove(dragRow, dragCol, row, col);
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

        String moveText() {
            if (moves.isEmpty()) return "";
            StringBuilder out = new StringBuilder();
            int end = historyCursor;
            int start = Math.max(0, end - 7);
            for (int i = start; i < end; i++) {
                if (i > start) out.append("   ");
                if ((i & 1) == 0) out.append((i / 2 + 1)).append(". ");
                out.append(moves.get(i));
            }
            return out.toString();
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
            if (Character.toUpperCase(moving) == 'P' && (toRow == 0 || toRow == 7)) {
                position[toRow][toCol] = Character.isUpperCase(moving) ? 'Q' : 'q';
            }
            moves.add(castle ? (toCol > fromCol ? "O-O" : "O-O-O")
                    : coordinate(fromRow, fromCol) + (capture ? "x" : "-") + coordinate(toRow, toCol));
            whiteTurn = !whiteTurn;
            history.add(new GameState(position, whiteTurn, castleRights));
            historyCursor++;
            selectedRow = selectedCol = -1;
            onChanged.run();
        }

        private String coordinate(int row, int col) { return "" + (char)('a' + col) + (char)('8' - row); }

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

        private List<int[]> legalMoves(int row, int col) {
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
}
