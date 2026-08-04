package com.plainphone.app;

/**
 * A little 8-bit-style animation for the home screen, expressed purely as "which pixels
 * are on at this tick" — PixelArtView owns the timer, canvas drawing, and shared ground
 * line; each constant here only fills its own content grid (COLS wide, CONTENT_ROWS tall).
 * Palette indices: 0 = background (not drawn), 1 = WHITE, 2 = LTGRAY, 3 = GRAY, 4 = DKGRAY.
 */
enum Scene {

    RAIN("Rain") {
        private final int[] columns = {1, 3, 5, 7, 9, 11, 13, 14};
        private static final int FALL_TOP = 6;
        private static final int FALL_ROWS = 8; // rows 6..13 — rows 14-17 are ground scenery

        @Override
        void fillGrid(int[][] grid, int tick) {
            mainCloud(grid);
            driftingCloud(grid, tick);
            ground(grid);

            for (int i = 0; i < columns.length; i++) {
                int head = FALL_TOP + ((tick + i * 2) % FALL_ROWS);
                set(grid, head, columns[i], 1);
                set(grid, head - 1, columns[i], 2); // dimmer trailing pixel, for a streak
            }
        }

        private void mainCloud(int[][] grid) {
            fillRow(grid, 0, 5, 10, 1);
            fillRow(grid, 1, 3, 12, 1);
            fillRow(grid, 2, 1, 14, 1);
            fillRow(grid, 3, 1, 14, 1);
            fillRow(grid, 4, 2, 13, 1);
            fillRow(grid, 5, 4, 11, 2); // shaded underside for a bit of volume
        }

        private void driftingCloud(int[][] grid, int tick) {
            int cyclePos = (tick / 8) % 6;
            int drift = cyclePos <= 3 ? cyclePos : 6 - cyclePos; // 0,1,2,3,2,1 — drifts and back
            int c0 = 12 - drift;
            fillRow(grid, 1, c0 + 1, c0 + 2, 1);
            fillRow(grid, 2, c0, c0 + 3, 1);
            fillRow(grid, 3, c0 + 1, c0 + 2, 2);
        }

        private void ground(int[][] grid) {
            // A small tree on the left.
            set(grid, 14, 2, 1);
            set(grid, 15, 1, 2); set(grid, 15, 2, 1); set(grid, 15, 3, 2);
            set(grid, 16, 1, 1); set(grid, 16, 2, 1); set(grid, 16, 3, 1);
            set(grid, 17, 2, 4);

            // Grass with a flower accent.
            set(grid, 16, 7, 2); // flower stem
            for (int c = 0; c <= 15; c++) {
                if (c == 2 || c == 7) continue;
                set(grid, 17, c, 3);
            }
            set(grid, 17, 7, 1); // flower bloom
        }
    },

    UFO("UFO") {
        private final int[][] stars = {
                {0, 1}, {0, 13}, {1, 4}, {2, 11}, {3, 0}, {3, 15}, {4, 2}, {14, 1}, {15, 14},
        };
        private static final int BEAM_COL = 7;
        private static final int WALK_TICKS = 9;
        private static final int RISE_TICKS = 7;
        private static final int PAUSE_TICKS = 3;
        private static final int CYCLE_LENGTH = WALK_TICKS + RISE_TICKS + PAUSE_TICKS;

        @Override
        void fillGrid(int[][] grid, int tick) {
            for (int i = 0; i < stars.length; i++) {
                if ((tick + i) % 4 != 0) {
                    set(grid, stars[i][0], stars[i][1], (i % 2 == 0) ? 1 : 2);
                }
            }

            int[] bob = {0, 1, 2, 1};
            int base = 4 + bob[tick % bob.length];

            // Dome with a small alien silhouette peeking out (dark cutout eyes).
            fillRow(grid, base, 6, 9, 1);
            set(grid, base, 7, 4);
            set(grid, base, 8, 4);

            fillRow(grid, base + 1, 4, 11, 2); // body

            // Rim, narrowed from the ship's earlier full-width version, with chasing lights.
            for (int c = 3; c <= 12; c++) {
                set(grid, base + 2, c, ((c + tick) % 4 == 0) ? 1 : 4);
            }

            if (tick % 3 < 2) {
                for (int r = base + 3; r <= 16; r++) {
                    set(grid, r, BEAM_COL, 3);
                    set(grid, r, BEAM_COL + 1, 3);
                }
            }

            abduction(grid, tick, base);
        }

        private void abduction(int[][] grid, int tick, int base) {
            int cycle = tick / CYCLE_LENGTH;
            int phase = tick % CYCLE_LENGTH;
            // Deterministic per-cycle "random" spawn column — every PixelArtView showing
            // this scene derives it purely from tick, since Scene constants are shared
            // singletons and can't hold mutable per-instance state.
            int startCol = (cycle * 37 + 5) % 16;

            if (phase < WALK_TICKS) {
                int distance = BEAM_COL - startCol;
                int step = Math.min(phase, Math.abs(distance));
                int personCol = startCol + step * Integer.signum(distance);
                set(grid, 17, personCol, 1);
            } else if (phase < WALK_TICKS + RISE_TICKS) {
                int riseStep = phase - WALK_TICKS;
                int personRow = 17 - riseStep;
                if (personRow > base + 2) {
                    set(grid, personRow, BEAM_COL, 1);
                }
            }
        }
    },

    HEARTBEAT("Heartbeat") {
        // A short cardiac-cycle-like curve: quick expansion, fast partial relax, a smaller
        // secondary bump, then a longer rest — instead of a flat 2-shape toggle.
        private final double[] beatScale = {0.90, 1.15, 1.05, 0.95, 1.08, 1.0, 0.92, 0.90, 0.90, 0.90};

        @Override
        void fillGrid(int[][] grid, int tick) {
            int phase = tick % beatScale.length;
            double scale = beatScale[phase];

            double centerCol = 7.5;
            double centerRow = 9.0;
            double halfWidth = 7.0;
            double halfHeight = 7.0;

            for (int r = 1; r <= 17; r++) {
                for (int c = 0; c <= 15; c++) {
                    double nx = (c - centerCol) / (halfWidth * scale);
                    double ny = ((centerRow - r) / (halfHeight * scale)) * 1.25 + 0.15;
                    double eq = Math.pow(nx * nx + ny * ny - 1, 3) - nx * nx * ny * ny * ny;
                    if (eq <= 0) {
                        // Shade by position for a volumetric look: highlight upper-left,
                        // shadow lower-right.
                        double shade = nx - ny;
                        int color = shade < -0.4 ? 1 : shade < 0.4 ? 2 : 4;
                        set(grid, r, c, color);
                    }
                }
            }

            if (phase == 1 || phase == 4) {
                ring(grid, 1, 0, 17, 15, 2); // faint pulse ring right at each beat
            }
        }

        private void ring(int[][] grid, int top, int left, int bottom, int right, int color) {
            for (int c = left; c <= right; c++) {
                set(grid, top, c, color);
                set(grid, bottom, c, color);
            }
            for (int r = top; r <= bottom; r++) {
                set(grid, r, left, color);
                set(grid, r, right, color);
            }
        }
    };

    final String label;

    Scene(String label) {
        this.label = label;
    }

    abstract void fillGrid(int[][] grid, int tick);

    private static void fillRow(int[][] grid, int row, int fromCol, int toCol, int value) {
        for (int c = fromCol; c <= toCol; c++) {
            set(grid, row, c, value);
        }
    }

    static void set(int[][] grid, int row, int col, int value) {
        if (row >= 0 && row < grid.length && col >= 0 && col < grid[0].length) {
            grid[row][col] = value;
        }
    }
}
