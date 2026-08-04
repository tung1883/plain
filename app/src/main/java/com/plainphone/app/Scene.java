package com.plainphone.app;

/**
 * A little 8-bit-style animation for the home screen, expressed purely as "which pixels
 * are on at this tick" — PixelArtView owns the timer, canvas drawing, and shared ground
 * line; each constant here only fills its own content grid (COLS wide, CONTENT_ROWS tall).
 * Palette indices: 0 = background (not drawn), 1 = WHITE, 2 = LTGRAY, 3 = GRAY, 4 = DKGRAY.
 */
enum Scene {

    RAIN("Rain") {
        @Override
        void fillGrid(int[][] grid, int tick) {
            cloud(grid, 1);
            int[] columns = {1, 3, 5, 6};
            for (int i = 0; i < columns.length; i++) {
                int row = 3 + ((tick + i * 2) % 6);
                set(grid, row, columns[i], 2);
            }
        }
    },

    UFO("UFO") {
        @Override
        void fillGrid(int[][] grid, int tick) {
            int[] bob = {0, 1, 1, 0};
            int base = 2 + bob[tick % bob.length];

            set(grid, base, 3, 1);
            set(grid, base, 4, 1);
            for (int c = 1; c <= 6; c++) {
                set(grid, base + 1, c, 2);
            }
            for (int c = 0; c <= 7; c++) {
                set(grid, base + 2, c, (c % 2 == tick % 2) ? 1 : 4);
            }
            if (tick % 3 < 2) {
                for (int r = base + 3; r <= 8; r++) {
                    set(grid, r, 3, 3);
                    set(grid, r, 4, 3);
                }
            }
        }
    },

    HEARTBEAT("Heartbeat") {
        private final int[][] big = {
                {0, 1}, {0, 2}, {0, 4}, {0, 5},
                {1, 0}, {1, 1}, {1, 2}, {1, 3}, {1, 4}, {1, 5}, {1, 6},
                {2, 0}, {2, 1}, {2, 2}, {2, 3}, {2, 4}, {2, 5}, {2, 6},
                {3, 1}, {3, 2}, {3, 3}, {3, 4}, {3, 5},
                {4, 2}, {4, 3}, {4, 4},
        };
        private final int[][] small = {
                {0, 2}, {0, 4},
                {1, 1}, {1, 2}, {1, 3}, {1, 4}, {1, 5},
                {2, 1}, {2, 2}, {2, 3}, {2, 4}, {2, 5},
                {3, 2}, {3, 3}, {3, 4},
        };

        @Override
        void fillGrid(int[][] grid, int tick) {
            int[][] shape = (tick % 2 == 0) ? big : small;
            for (int[] p : shape) {
                set(grid, 2 + p[0], p[1], 1);
            }
        }
    };

    final String label;

    Scene(String label) {
        this.label = label;
    }

    abstract void fillGrid(int[][] grid, int tick);

    private static void cloud(int[][] grid, int color) {
        set(grid, 0, 2, color); set(grid, 0, 3, color); set(grid, 0, 4, color); set(grid, 0, 5, color);
        for (int c = 1; c <= 6; c++) set(grid, 1, c, color);
        for (int c = 0; c <= 7; c++) set(grid, 2, c, color);
    }

    static void set(int[][] grid, int row, int col, int value) {
        if (row >= 0 && row < grid.length && col >= 0 && col < grid[0].length) {
            grid[row][col] = value;
        }
    }
}
