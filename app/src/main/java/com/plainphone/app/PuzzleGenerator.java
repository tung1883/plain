package com.plainphone.app;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Java port of the real lichess puzzle-generator algorithm (lichess-org/lichess-puzzler,
 *  generator.py v50) adapted for on-device use: lichess reads a pre-existing engine eval off
 *  each move (from its own analysed-games database) — we don't have that, so {@link #walkScore}
 *  runs a shallow engine query at every ply to stand in for it. Everything past that point —
 *  the win-chance-swing trigger, the "only move" forced-line verification, the mate/advantage
 *  cooking recursion — is a direct port, just against {@link ChessRules} instead of
 *  python-chess and {@link StockfishEngine} instead of a local Stockfish subprocess. */
final class PuzzleGenerator {

    // Mobile-scaled down from upstream's depth 50 / 30s / 25M-node pair search and depth 15 /
    // 10s mate-defense search — all still capped by StockfishEngine's own 1.5s movetime cap.
    private static final int WALK_DEPTH = 10;     // per-ply "what's the eval here" walk
    private static final int PAIR_DEPTH = 14;     // investigating a candidate (multipv 2)
    private static final int DEFENSE_DEPTH = 10;  // opponent's forced defense in a mate line

    private static final double NON_MATE_WIN_THRESHOLD = 0.6;
    private static final Score MATE_SOON = Score.mate(15);

    private final StockfishEngine engine;
    private final java.util.function.BooleanSupplier cancelled;

    PuzzleGenerator(StockfishEngine engine, java.util.function.BooleanSupplier cancelled) {
        this.engine = engine;
        this.cancelled = cancelled;
    }

    // --- score: a tiny stand-in for python-chess's total-ordered Score -----------------

    static final class Score {
        final Integer cp;
        final Integer mate;

        private Score(Integer cp, Integer mate) { this.cp = cp; this.mate = mate; }

        static Score cp(int v) { return new Score(v, null); }
        static Score mate(int n) { return new Score(null, n); }

        Score negate() { return new Score(cp == null ? null : -cp, mate == null ? null : -mate); }

        /** A single comparable scalar: any positive mate beats any centipawn score, which
         *  beats any negative mate; among mates, a faster one is more extreme (Mate(1) is the
         *  best possible score, Mate(-1) the worst) — same ordering python-chess's Score
         *  implements natively. */
        long key() {
            if (mate != null) return mate > 0 ? 1_000_000L - mate : -1_000_000L - mate;
            return cp == null ? 0 : cp;
        }

        boolean gt(Score o) { return key() > o.key(); }
        boolean ge(Score o) { return key() >= o.key(); }
        boolean lt(Score o) { return key() < o.key(); }

        /** -1..1, https://github.com/lichess-org/lila/pull/11148's sigmoid. */
        double winChances() {
            if (mate != null) return mate > 0 ? 1.0 : -1.0;
            if (cp == null) return 0.0;
            return 2.0 / (1.0 + Math.exp(-0.00368208 * cp)) - 1.0;
        }
    }

    private static Score povOf(Score raw, boolean rawSideToMove, boolean want) {
        return rawSideToMove == want ? raw : raw.negate();
    }

    private static Score engineScore(StockfishEngine.Analysis a) { return new Score(a.scoreCp, a.mateIn); }

    private static int[] uciToMove(String uci) {
        int fromCol = uci.charAt(0) - 'a', fromRow = 8 - (uci.charAt(1) - '0');
        int toCol = uci.charAt(2) - 'a', toRow = 8 - (uci.charAt(3) - '0');
        return new int[]{fromRow, fromCol, toRow, toCol};
    }

    private static String uciOf(int[] m) {
        return "" + (char) ('a' + m[1]) + (8 - m[0]) + (char) ('a' + m[3]) + (8 - m[2]);
    }

    // --- next-move pair (multipv 2) + "only move" verification -------------------------

    private static final class NextPair {
        final ChessRules.Pos node;
        final int[] bestMove; final Score bestScore;
        final int[] secondMove; final Score secondScore;
        NextPair(ChessRules.Pos node, int[] bestMove, Score bestScore, int[] secondMove, Score secondScore) {
            this.node = node; this.bestMove = bestMove; this.bestScore = bestScore;
            this.secondMove = secondMove; this.secondScore = secondScore;
        }
    }

    private NextPair getNextPair(ChessRules.Pos node, boolean winner, boolean lookingForMate) throws IOException {
        List<StockfishEngine.Analysis> info = engine.analyzeMultiPv(ChessRules.toFen(node), PAIR_DEPTH, 2, null);
        if (info.isEmpty() || info.get(0).pvUci.isEmpty()) return null;
        StockfishEngine.Analysis bestA = info.get(0);
        int[] bestMove = uciToMove(bestA.pvUci.get(0));
        Score bestScore = povOf(engineScore(bestA), node.whiteTurn, winner);
        StockfishEngine.Analysis secondA = info.size() > 1 && !info.get(1).pvUci.isEmpty() ? info.get(1) : null;
        int[] secondMove = secondA != null ? uciToMove(secondA.pvUci.get(0)) : null;
        Score secondScore = secondA != null ? povOf(engineScore(secondA), node.whiteTurn, winner) : null;
        NextPair pair = new NextPair(node, bestMove, bestScore, secondMove, secondScore);
        if (node.whiteTurn == winner && !isValidAttack(pair, winner)) return null;
        return pair;
    }

    private boolean isValidMateInOne(NextPair pair, boolean winner) throws IOException {
        if (pair.bestScore.mate == null || pair.bestScore.mate != 1) return false;
        if (pair.secondScore == null || pair.secondScore.winChances() <= NON_MATE_WIN_THRESHOLD) return true;
        if (pair.secondScore.mate != null && pair.secondScore.mate == 1) {
            int mates = ChessRules.countImmediateMates(pair.node);
            List<StockfishEngine.Analysis> info = engine.analyzeMultiPv(ChessRules.toFen(pair.node), PAIR_DEPTH, mates + 1, null);
            if (info.isEmpty()) return true;
            StockfishEngine.Analysis lastA = info.get(info.size() - 1);
            Score last = povOf(engineScore(lastA), pair.node.whiteTurn, winner);
            if (last.lt(Score.mate(1)) && last.winChances() > NON_MATE_WIN_THRESHOLD) return false;
            return true;
        }
        return false;
    }

    private boolean isValidAttack(NextPair pair, boolean winner) throws IOException {
        if (pair.secondScore == null) return true;
        if (isValidMateInOne(pair, winner)) return true;
        return pair.bestScore.winChances() > pair.secondScore.winChances() + 0.7;
    }

    // --- cooking: verifying a forced line exists ---------------------------------------

    /** Winner's plies must pass {@link #isValidAttack}; the opponent's plies just play the
     *  engine's single best defense (shallower search — mirrors upstream's
     *  {@code mate_defense_limit}). Returns null if no forced mate could be verified. */
    private List<int[]> cookMate(ChessRules.Pos node, boolean winner) throws IOException {
        if (ChessRules.isGameOver(node)) return new ArrayList<>();
        int[] move;
        if (node.whiteTurn == winner) {
            NextPair pair = getNextPair(node, winner, true);
            if (pair == null || pair.bestScore.lt(MATE_SOON)) return null;
            move = pair.bestMove;
        } else {
            List<StockfishEngine.Analysis> info = engine.analyzeMultiPv(ChessRules.toFen(node), DEFENSE_DEPTH, 1, null);
            if (info.isEmpty() || info.get(0).pvUci.isEmpty()) return null;
            move = uciToMove(info.get(0).pvUci.get(0));
        }
        List<int[]> followUp = cookMate(ChessRules.apply(node, move[0], move[1], move[2], move[3]), winner);
        if (followUp == null) return null;
        List<int[]> out = new ArrayList<>();
        out.add(move);
        out.addAll(followUp);
        return out;
    }

    /** Every ply (winner's and the opponent's alike) goes through {@link #getNextPair} at the
     *  same depth — matching upstream, which doesn't special-case the defender here the way
     *  {@link #cookMate} does. Stops on repetition or the advantage dropping back under
     *  {@code Cp(200)}. Returns null on "not winning enough, abort" (never a genuine puzzle). */
    private List<NextPair> cookAdvantage(ChessRules.Pos node, boolean winner, Set<String> seenKeys) throws IOException {
        String key = ChessRules.positionKey(node);
        if (seenKeys.contains(key)) return null; // repetition
        Set<String> nextSeen = new HashSet<>(seenKeys);
        nextSeen.add(key);

        NextPair pair = getNextPair(node, winner, false);
        if (pair == null) return new ArrayList<>();
        if (pair.bestScore.lt(Score.cp(200))) return null;

        List<NextPair> followUp = cookAdvantage(
                ChessRules.apply(node, pair.bestMove[0], pair.bestMove[1], pair.bestMove[2], pair.bestMove[3]),
                winner, nextSeen);
        if (followUp == null) return null;
        List<NextPair> out = new ArrayList<>();
        out.add(pair);
        out.addAll(followUp);
        return out;
    }

    // --- per-ply walk + trigger ----------------------------------------------------------

    /** The shallow "what's the eval here" query that stands in for lichess's pre-existing
     *  {@code %eval} annotation — already in the side-to-move's own POV, same UCI convention
     *  {@link StockfishEngine} documents elsewhere. */
    private Score walkScore(ChessRules.Pos board) throws IOException {
        List<StockfishEngine.Analysis> info = engine.analyzeMultiPv(ChessRules.toFen(board), WALK_DEPTH, 1, null);
        if (info.isEmpty()) return null;
        StockfishEngine.Analysis a = info.get(0);
        return new Score(a.scoreCp, a.mateIn);
    }

    private static final class StepResult {
        final ChessPuzzles.Puzzle puzzle;
        final Score score;
        private StepResult(ChessPuzzles.Puzzle puzzle, Score score) { this.puzzle = puzzle; this.score = score; }
        static StepResult puzzle(ChessPuzzles.Puzzle p) { return new StepResult(p, null); }
        static StepResult score(Score s) { return new StepResult(null, s); }
    }

    private StepResult analyzePosition(ChessRules.Pos board, Score prevScore, Score score,
                                       ChessLibrary.Entry meta, int ply) throws IOException {
        boolean winner = board.whiteTurn;
        if (ChessRules.allLegalMoves(board).size() < 2) return StepResult.score(score);
        if (prevScore.gt(Score.cp(300)) && score.lt(MATE_SOON)) return StepResult.score(score);
        if (ChessRules.materialDiff(board, winner) > 0) return StepResult.score(score);
        if (score.ge(Score.mate(1))) return StepResult.score(score); // mate-in-1: always too easy
        if (score.gt(MATE_SOON)) {
            ChessPuzzles.Puzzle p = tryMate(board, winner, meta, ply);
            return p != null ? StepResult.puzzle(p) : StepResult.score(score);
        }
        if (score.ge(Score.cp(200)) && score.winChances() > prevScore.winChances() + 0.6) {
            if (score.lt(Score.cp(400)) && ChessRules.materialDiff(board, winner) > -1) return StepResult.score(score);
            ChessPuzzles.Puzzle p = tryAdvantage(board, winner, meta, ply);
            return p != null ? StepResult.puzzle(p) : StepResult.score(score);
        }
        return StepResult.score(score);
    }

    private ChessPuzzles.Puzzle tryMate(ChessRules.Pos board, boolean winner, ChessLibrary.Entry meta, int ply) throws IOException {
        List<int[]> solution = cookMate(board, winner);
        if (solution == null || solution.isEmpty()) return null;
        List<String> uci = new ArrayList<>();
        for (int[] m : solution) uci.add(uciOf(m));
        return buildPuzzle(board, meta, ply, uci, winner, "Mate", Integer.MAX_VALUE - 1);
    }

    private ChessPuzzles.Puzzle tryAdvantage(ChessRules.Pos board, boolean winner, ChessLibrary.Entry meta, int ply) throws IOException {
        List<NextPair> solution = cookAdvantage(board, winner, new HashSet<>());
        if (solution == null) return null;
        while (!solution.isEmpty()
                && (solution.size() % 2 == 0 || solution.get(solution.size() - 1).secondMove == null)) {
            solution = solution.subList(0, solution.size() - 1);
        }
        if (solution.size() <= 1) return null;   // discard a one-mover
        if (solution.size() == 3) return null;   // discard a two-mover (no tiering to allow it)
        List<String> uci = new ArrayList<>();
        for (NextPair p : solution) uci.add(uciOf(p.bestMove));
        Score last = solution.get(solution.size() - 1).bestScore;
        int cp = last.cp != null ? last.cp : Integer.MAX_VALUE - 2;
        return buildPuzzle(board, meta, ply, uci, winner, "Advantage", cp);
    }

    private ChessPuzzles.Puzzle buildPuzzle(ChessRules.Pos board, ChessLibrary.Entry meta, int ply,
                                            List<String> solutionUci, boolean winnerWhite,
                                            String category, int cp) {
        return new ChessPuzzles.Puzzle(meta.src, meta.white, meta.black, meta.event, meta.date,
                ChessRules.toFen(board), ply, solutionUci, winnerWhite, category, cp);
    }

    // --- walking one game -----------------------------------------------------------------

    /** Walks {@code entry}'s mainline ply by ply looking for the first puzzle-worthy position
     *  (at most one puzzle per game, same as upstream). Skips (rather than aborting the whole
     *  game, unlike upstream) a ply whose walk-eval query fails — one flaky engine call
     *  shouldn't cost the rest of a game's scan. */
    Optional<ChessPuzzles.Puzzle> analyzeGame(ChessLibrary.Entry entry) throws IOException {
        List<String> sans = entry.sans();
        ChessRules.Pos pos = ChessRules.start();
        Score prevScore = Score.cp(20);
        Set<String> seenKeys = new HashSet<>();
        boolean skipUntilIrreversible = false;

        for (int i = 0; i < sans.size(); i++) {
            if (cancelled.getAsBoolean()) return Optional.empty();
            String san = ChessRules.normalizeSan(sans.get(i));
            int[] move = ChessRules.findMoveBySan(pos, san);
            if (move == null) break; // corrupt/unsupported token — same bail-out as loadSanMoves

            boolean irreversible = ChessRules.isIrreversible(pos, move[0], move[1], move[2], move[3]);
            pos = ChessRules.apply(pos, move[0], move[1], move[2], move[3]);

            if (skipUntilIrreversible) {
                if (irreversible) { skipUntilIrreversible = false; seenKeys.clear(); }
                continue;
            }
            String key = ChessRules.positionKey(pos);
            if (seenKeys.contains(key)) { skipUntilIrreversible = true; continue; }
            seenKeys.add(key);

            Score currentEval = walkScore(pos);
            if (currentEval == null) continue;

            StepResult result = analyzePosition(pos, prevScore, currentEval, entry, i + 1);
            if (result.puzzle != null) return Optional.of(result.puzzle);
            prevScore = result.score.negate();
        }
        return Optional.empty();
    }
}
