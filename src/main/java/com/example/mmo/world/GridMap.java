package com.example.mmo.world;

import java.util.Random;

public class GridMap {
    public final int w;
    public final int h;
    private final boolean[][] blocked;

    public GridMap(int w, int h) {
        this.w = w;
        this.h = h;
        this.blocked = new boolean[w][h];
    }

    public boolean inBounds(int x, int y) {
        return x >= 0 && y >= 0 && x < w && y < h;
    }

    public boolean isWalkable(int x, int y) {
        return inBounds(x, y) && !blocked[x][y];
    }

    public void setBlockedRect(int x1, int y1, int x2, int y2, boolean value) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
                if (inBounds(x, y)) blocked[x][y] = value;
            }
        }
    }

    public static GridMap makeMvp() {
        GridMap m = new GridMap(200, 200);

        m.setBlockedRect(20, 20, 35, 35, true);
        m.setBlockedRect(40, 25, 55, 33, true);

        m.setBlockedRect(70, 40, 85, 55, true);
        m.setBlockedRect(90, 60, 110, 65, true);

        // 테두리
        m.setBlockedRect(0, 0, 199, 0, true);
        m.setBlockedRect(0, 0, 0, 199, true);
        m.setBlockedRect(199, 0, 199, 199, true);
        m.setBlockedRect(0, 199, 199, 199, true);

        return m;
    }

    public int clampX(int x){ return Math.max(1, Math.min(w-2, x)); }
    public int clampY(int y){ return Math.max(1, Math.min(h-2, y)); }

    public int[] findNearestWalkable(int x, int y) {
        x = clampX(x);
        y = clampY(y);
        if (isWalkable(x, y)) return new int[]{x, y};

        for (int r = 1; r < 20; r++) {
            for (int dx = -r; dx <= r; dx++) {
                int nx1 = x + dx, ny1 = y - r;
                int nx2 = x + dx, ny2 = y + r;
                if (isWalkable(nx1, ny1)) return new int[]{nx1, ny1};
                if (isWalkable(nx2, ny2)) return new int[]{nx2, ny2};
            }
            for (int dy = -r; dy <= r; dy++) {
                int nx1 = x - r, ny1 = y + dy;
                int nx2 = x + r, ny2 = y + dy;
                if (isWalkable(nx1, ny1)) return new int[]{nx1, ny1};
                if (isWalkable(nx2, ny2)) return new int[]{nx2, ny2};
            }
        }
        return new int[]{10, 10};
    }

    public int[] randomWalkable(Random rnd, int x1, int y1, int x2, int y2) {
        for (int i = 0; i < 2000; i++) {
            int x = x1 + rnd.nextInt(Math.max(1, x2 - x1 + 1));
            int y = y1 + rnd.nextInt(Math.max(1, y2 - y1 + 1));
            if (isWalkable(x, y)) return new int[]{x, y};
        }
        return findNearestWalkable((x1 + x2) / 2, (y1 + y2) / 2);
    }
}

