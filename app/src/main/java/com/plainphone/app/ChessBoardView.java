package com.plainphone.app;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Shared board surface: the Home chess panel and the Workspace chess panel use the same
 *  rules and gestures — this view owns the whole game (position, history, legality, SAN,
 *  the bundled Stockfish integration) and is otherwise a plain custom {@link View}. */
final class ChessBoardView extends View {
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
    private final android.view.GestureDetector doubleTap;
    private String boardTheme = "default";
    private String pieceTheme = "fritz";
    private int light = 0xFF393A3C, dark = 0xFF1D1E20;
    private Bitmap boardLightTile, boardDarkTile;
    private String gameOverText; // null while the game (at the displayed position) is ongoing
    // Latest background-analysis result as display lines ("+0.34 d14  Nf3 Nc6 Bb5 Bb4 O-O"),
    // one per MultiPV candidate — kept as separate strings rather than one joined blob so the
    // UI can ellipsize each line on its own instead of letting a long one wrap to a second
    // line — and a generation counter so a slow analysis for a position the user has since
    // moved past can recognize itself as stale and get thrown away instead of overwriting a
    // newer one.
    private List<String> engineSummary = new ArrayList<>();
    private int analysisGeneration;

    ChessBoardView(Activity host, Runnable onChanged) {
        super(host);
        this.host = host;
        this.onChanged = onChanged;
        setFocusable(true);
        textPaint.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.NORMAL));
        coordPaint.setTypeface(Fonts.current(host));
        setBoardTheme(boardTheme);
        history.add(new GameState(position, whiteTurn, castleRights));
        doubleTap = new android.view.GestureDetector(host, new android.view.GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onDoubleTap(android.view.MotionEvent e) {
                return handleDoubleTap(e);
            }
        });
        requestAnalysis();
    }

    List<String> engineSummary() { return engineSummary; }

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
        doubleTap.onTouchEvent(event);
        if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
            downX = event.getX(); downY = event.getY();
            dragCol = (int) ((downX - left) / cell); dragRow = (int) (downY / cell);
            boolean grabbedPiece = gameOverText == null && in(dragRow, dragCol) && position[dragRow][dragCol] != 0
                    && Character.isUpperCase(position[dragRow][dragCol]) == whiteTurn;
            if (grabbedPiece) {
                selectedRow = dragRow; selectedCol = dragCol;
                invalidate(); // Selection never tints pieces; it only reveals move dots.
            }
            // Only steal the gesture from the enclosing scroll/swiper when there's an actual
            // piece to drag — a plain tap (destination square, empty square) stays small
            // enough that neither ScrollView nor the section swiper would intercept it anyway.
            if (grabbedPiece) getParent().requestDisallowInterceptTouchEvent(true);
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
        requestAnalysis();
        invalidate();
    }

    // Guards against a second double-tap firing while Stockfish is still answering the
    // first one — the board state a stale answer was computed against may no longer exist.
    private boolean thinkingAboutDoubleTap;

    /** Double-tapping an empty square or an enemy piece (no piece selected first) auto-plays
     *  it: find every piece of the side to move that can legally reach that square, and if
     *  there's only one, just make the move — mirrors how a drag-drop never needs to search
     *  candidates because the dragged piece already pins the origin; here the double-tap
     *  itself pins the destination, so the search runs backwards from it instead. When more
     *  than one piece qualifies, this is ChessBase's real "Heumas" move: it hands the bundled
     *  Stockfish exactly those candidates as {@code searchmoves} and plays whichever one the
     *  engine judges best, off the UI thread so a deep search never freezes the board; the
     *  local material-only search only ever runs as a fallback if the engine can't answer. */
    private boolean handleDoubleTap(android.view.MotionEvent e) {
        if (gameOverText != null || thinkingAboutDoubleTap) return false;
        float left = UiKit.dp(host, 20), cell = (getWidth() - left) / 8f;
        int col = (int) ((e.getX() - left) / cell), row = (int) (e.getY() / cell);
        if (!in(row, col)) return false;
        char occupant = position[row][col];
        if (occupant != 0 && Character.isUpperCase(occupant) == whiteTurn) return false;
        List<int[]> candidates = candidatesFor(row, col);
        if (candidates.isEmpty()) return false;
        if (candidates.size() == 1) {
            selectedRow = selectedCol = -1;
            commitMove(candidates.get(0)[0], candidates.get(0)[1], row, col);
            invalidate();
            return true;
        }
        askStockfish(candidates, row, col);
        return true;
    }

    // A fixed time budget keeps a double-tap's latency predictable no matter how
    // tactically messy the position is, unlike a fixed depth which can balloon on sharp
    // positions with many checks/captures to look through.
    private static final int STOCKFISH_MOVETIME_MS = 80;

    private void askStockfish(List<int[]> candidates, int toRow, int toCol) {
        thinkingAboutDoubleTap = true;
        String fen = toFen();
        List<String> uciMoves = new ArrayList<>();
        for (int[] from : candidates) uciMoves.add(uciMove(from[0], from[1], toRow, toCol));
        new Thread(() -> {
            int[] chosen = null;
            try {
                String best = StockfishEngine.get(host).bestOf(fen, uciMoves, STOCKFISH_MOVETIME_MS);
                if (best != null) chosen = candidates.get(uciMoves.indexOf(best));
            } catch (Exception ignored) {
                // No engine available (process failed to start, wrong ABI, etc.) — the
                // local heuristic below still gives a real, defensible answer.
            }
            int[] fallback = chosen != null ? chosen : heumasPick(candidates, toRow, toCol);
            host.runOnUiThread(() -> {
                thinkingAboutDoubleTap = false;
                selectedRow = selectedCol = -1;
                commitMove(fallback[0], fallback[1], toRow, toCol);
                invalidate();
            });
        }).start();
    }

    /** Current position as a FEN piece-placement + side-to-move + castling string. En
     *  passant is always "-" since this board never tracks it as a move option, and the
     *  half-move/full-move counters are rough — neither affects which move Stockfish
     *  prefers at these shallow, searchmoves-restricted depths. */
    private String toFen() {
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < 8; r++) {
            int empty = 0;
            for (int c = 0; c < 8; c++) {
                char p = position[r][c];
                if (p == 0) { empty++; continue; }
                if (empty > 0) { sb.append(empty); empty = 0; }
                sb.append(p);
            }
            if (empty > 0) sb.append(empty);
            if (r < 7) sb.append('/');
        }
        sb.append(whiteTurn ? " w " : " b ");
        String castling = "" + ((castleRights & 1) != 0 ? "K" : "") + ((castleRights & 2) != 0 ? "Q" : "")
                + ((castleRights & 4) != 0 ? "k" : "") + ((castleRights & 8) != 0 ? "q" : "");
        sb.append(castling.isEmpty() ? "-" : castling);
        sb.append(" - 0 ").append((moves.size() / 2) + 1);
        return sb.toString();
    }

    private String uciMove(int fromRow, int fromCol, int toRow, int toCol) {
        char moving = position[fromRow][fromCol];
        boolean promotes = Character.toUpperCase(moving) == 'P' && (toRow == 0 || toRow == 7);
        return coordinate(fromRow, fromCol) + coordinate(toRow, toCol) + (promotes ? "q" : "");
    }

    // Longer than the double-tap's own search budget since nothing is waiting on this —
    // it just needs to land before the *next* move, not before the current gesture ends.
    private static final int ANALYSIS_MOVETIME_MS = 700;
    private static final int ANALYSIS_LINES = 3;
    private static final int ANALYSIS_PLIES = 10;

    /** Kicks off a background evaluation of the position now on screen and, once it lands,
     *  updates {@link #engineSummary} and re-runs {@code onChanged} so the host redraws its
     *  status line — unless the position has since moved on again, in which case this
     *  generation's result is simply dropped rather than showing stale analysis. */
    private void requestAnalysis() {
        int generation = ++analysisGeneration;
        String fen = toFen();
        new Thread(() -> {
            List<String> summary;
            try {
                List<StockfishEngine.Analysis> lines =
                        StockfishEngine.get(host).analyzeMultiPv(fen, ANALYSIS_MOVETIME_MS, ANALYSIS_LINES);
                summary = formatAnalysis(lines);
            } catch (Exception e) {
                summary = new ArrayList<>();
            }
            List<String> finalSummary = summary;
            host.runOnUiThread(() -> {
                if (generation != analysisGeneration) return;
                engineSummary = finalSummary;
                onChanged.run();
            });
        }).start();
    }

    private List<String> formatAnalysis(List<StockfishEngine.Analysis> lines) {
        List<String> out = new ArrayList<>();
        if (lines == null) return out;
        for (StockfishEngine.Analysis a : lines) {
            if (a.pvUci.isEmpty()) continue;
            String evalText;
            if (a.mateIn != null) {
                // UCI's mate count is relative to the side to move; flip it to White's
                // perspective the same way the centipawn score is flipped below.
                evalText = "#" + (whiteTurn ? a.mateIn : -a.mateIn);
            } else if (a.scoreCp != null) {
                int cp = whiteTurn ? a.scoreCp : -a.scoreCp;
                evalText = (cp >= 0 ? "+" : "") + String.format("%.2f", cp / 100.0);
            } else {
                continue;
            }
            out.add(evalText + " d" + a.depth + "  " + pvToSan(a.pvUci));
        }
        return out;
    }

    /** Renders a UCI principal variation ("e2e4 e7e5 g1f3") as SAN ("e4 e5 Nf3") by actually
     *  replaying it move by move on the live board — reusing the same {@link #sanFor} that
     *  real moves use, so disambiguation, captures and check aren't reimplemented a second
     *  time for the analysis panel — then undoing every move so the displayed board is never
     *  affected by a line that was only ever meant to be read, not played. Capped at
     *  {@link #ANALYSIS_PLIES}; Stockfish's mainline can run far longer than that. */
    private String pvToSan(List<String> pvUci) {
        List<UndoInfo> undos = new ArrayList<>();
        List<int[]> squares = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        int plies = Math.min(pvUci.size(), ANALYSIS_PLIES);
        for (int i = 0; i < plies; i++) {
            String mv = pvUci.get(i);
            if (mv.length() < 4) break;
            int fromCol = mv.charAt(0) - 'a', fromRow = 8 - (mv.charAt(1) - '0');
            int toCol = mv.charAt(2) - 'a', toRow = 8 - (mv.charAt(3) - '0');
            if (!in(fromRow, fromCol) || !in(toRow, toCol)) break;
            char moving = position[fromRow][fromCol];
            if (moving == 0) break;
            char captured = position[toRow][toCol];
            boolean castle = Character.toUpperCase(moving) == 'K' && Math.abs(toCol - fromCol) == 2;
            boolean promotes = Character.toUpperCase(moving) == 'P' && (toRow == 0 || toRow == 7);
            String san = castle ? (toCol > fromCol ? "O-O" : "O-O-O")
                    : sanFor(moving, fromRow, fromCol, toRow, toCol, captured != 0, promotes);
            if (sb.length() > 0) sb.append(' ');
            sb.append(san);
            undos.add(rawApply(fromRow, fromCol, toRow, toCol));
            squares.add(new int[]{fromRow, fromCol, toRow, toCol});
        }
        for (int i = undos.size() - 1; i >= 0; i--) {
            int[] sq = squares.get(i);
            rawUndo(sq[0], sq[1], sq[2], sq[3], undos.get(i));
        }
        return sb.toString();
    }

    /** How many plies deep a candidate is actually searched before picking a winner — our
     *  move plus this many more replies, mirroring ChessBase's own "Heumas Ply" search-depth
     *  setting rather than a fixed, unadjustable heuristic. Only used as a fallback when the
     *  bundled Stockfish process itself couldn't be reached. */
    private static final int HEUMAS_PLY = 2;
    private static final int MATE_SCORE = 100000;

    /** Picks a single winner among two or more legal candidates for the same destination by
     *  actually playing each one out and searching {@link #HEUMAS_PLY} plies of replies —
     *  whichever leaves the best material result for the mover after the opponent answers
     *  as strongly as possible wins. Board order (top-left to bottom-right) breaks any
     *  leftover tie, so a double-tap always commits to exactly one piece. */
    private int[] heumasPick(List<int[]> candidates, int toRow, int toCol) {
        int[] best = null;
        int bestScore = Integer.MIN_VALUE;
        for (int[] from : candidates) {
            boolean movingWhite = Character.isUpperCase(position[from[0]][from[1]]);
            UndoInfo u = rawApply(from[0], from[1], toRow, toCol);
            int score = -negamax(HEUMAS_PLY - 1, !movingWhite);
            rawUndo(from[0], from[1], toRow, toCol, u);
            if (score > bestScore) { bestScore = score; best = from; }
        }
        return best;
    }

    /** Classic negamax: the side to move always picks the reply that's best *for it*, which
     *  is why the recursive call is negated — a good result for the opponent is a bad one
     *  for us. Depth 0 just returns the material count from the mover's own point of view. */
    private int negamax(int depth, boolean white) {
        if (depth <= 0) return (white ? 1 : -1) * materialBalance();
        List<int[]> moves = allLegalMoves(white);
        if (moves.isEmpty()) return isKingInCheck(white) ? -MATE_SCORE : 0;
        int best = Integer.MIN_VALUE;
        for (int[] mv : moves) {
            UndoInfo u = rawApply(mv[0], mv[1], mv[2], mv[3]);
            int score = -negamax(depth - 1, !white);
            rawUndo(mv[0], mv[1], mv[2], mv[3], u);
            if (score > best) best = score;
        }
        return best;
    }

    /** Every legal (from, to) pair for one side, flattened out of the per-piece lookup this
     *  class already does for move dots and SAN disambiguation. */
    private List<int[]> allLegalMoves(boolean white) {
        List<int[]> out = new ArrayList<>();
        for (int r = 0; r < 8; r++) for (int c = 0; c < 8; c++) {
            char p = position[r][c];
            if (p == 0 || Character.isUpperCase(p) != white) continue;
            for (int[] mv : legalMoves(r, c)) out.add(new int[]{r, c, mv[0], mv[1]});
        }
        return out;
    }

    private int materialBalance() {
        int score = 0;
        for (int r = 0; r < 8; r++) for (int c = 0; c < 8; c++) {
            char p = position[r][c];
            if (p == 0) continue;
            score += Character.isUpperCase(p) ? pieceValue(p) : -pieceValue(p);
        }
        return score;
    }

    /** Everything {@link #rawUndo} needs to put the board back exactly as it was — separate
     *  from {@link GameState} because search needs this thousands of times per double-tap
     *  and can't afford a full board copy each time. */
    private static final class UndoInfo {
        char moving, captured, rookPiece;
        boolean wasCastle;
        int rookFromCol, rookToCol, prevCastleRights;
    }

    /** Applies a move directly to the live board for search purposes only — same mechanics
     *  as {@link #commitMove} (castling moves the rook too, pawns auto-queen) but skipping
     *  everything about SAN notation and move history that a throwaway search branch never
     *  needs and must instead be undone with {@link #rawUndo}. */
    private UndoInfo rawApply(int fromRow, int fromCol, int toRow, int toCol) {
        UndoInfo u = new UndoInfo();
        u.moving = position[fromRow][fromCol];
        u.captured = position[toRow][toCol];
        u.prevCastleRights = castleRights;
        u.wasCastle = Character.toUpperCase(u.moving) == 'K' && Math.abs(toCol - fromCol) == 2;
        updateCastleRights(u.moving, fromRow, fromCol, u.captured, toRow, toCol);
        position[toRow][toCol] = u.moving;
        position[fromRow][fromCol] = 0;
        if (u.wasCastle) {
            u.rookFromCol = toCol > fromCol ? 7 : 0;
            u.rookToCol = toCol > fromCol ? 5 : 3;
            u.rookPiece = position[toRow][u.rookFromCol];
            position[toRow][u.rookToCol] = u.rookPiece;
            position[toRow][u.rookFromCol] = 0;
        }
        if (Character.toUpperCase(u.moving) == 'P' && (toRow == 0 || toRow == 7)) {
            position[toRow][toCol] = Character.isUpperCase(u.moving) ? 'Q' : 'q';
        }
        return u;
    }

    private void rawUndo(int fromRow, int fromCol, int toRow, int toCol, UndoInfo u) {
        position[fromRow][fromCol] = u.moving;
        position[toRow][toCol] = u.captured;
        if (u.wasCastle) {
            position[toRow][u.rookFromCol] = u.rookPiece;
            position[toRow][u.rookToCol] = 0;
        }
        castleRights = u.prevCastleRights;
    }

    private int pieceValue(char p) {
        switch (Character.toUpperCase(p)) {
            case 'P': return 1;
            case 'N': case 'B': return 3;
            case 'R': return 5;
            case 'Q': return 9;
            default: return 0;
        }
    }

    /** Every square holding a side-to-move piece whose legal moves include {@code (toRow,toCol)}. */
    private List<int[]> candidatesFor(int toRow, int toCol) {
        List<int[]> out = new ArrayList<>();
        for (int r = 0; r < 8; r++) for (int c = 0; c < 8; c++) {
            char p = position[r][c];
            if (p == 0 || Character.isUpperCase(p) != whiteTurn) continue;
            for (int[] mv : legalMoves(r, c)) {
                if (mv[0] == toRow && mv[1] == toCol) { out.add(new int[]{r, c}); break; }
            }
        }
        return out;
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
        requestAnalysis();
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
