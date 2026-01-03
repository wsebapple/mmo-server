package com.example.mmo.logic;

import com.example.mmo.world.GridMap;

import java.util.*;

public class AStar {
    public record P(int x, int y) {}
    private record Node(int x, int y, int g, int f, Node parent) {}

    public static List<P> findPath(GridMap map, int sx, int sy, int gx, int gy) {
        if (!map.isWalkable(sx, sy)) {
            int[] n = map.findNearestWalkable(sx, sy);
            sx = n[0]; sy = n[1];
        }
        if (!map.isWalkable(gx, gy)) {
            int[] n = map.findNearestWalkable(gx, gy);
            gx = n[0]; gy = n[1];
        }
        if (sx == gx && sy == gy) return List.of(new P(sx, sy));

        PriorityQueue<Node> open = new PriorityQueue<>(Comparator.comparingInt(n -> n.f));
        int[][] bestG = new int[map.w][map.h];
        for (int x=0; x<map.w; x++) Arrays.fill(bestG[x], Integer.MAX_VALUE);

        Node start = new Node(sx, sy, 0, h(sx, sy, gx, gy), null);
        open.add(start);
        bestG[sx][sy] = 0;

        int[] dx = {1, -1, 0, 0};
        int[] dy = {0, 0, 1, -1};

        while (!open.isEmpty()) {
            Node cur = open.poll();
            if (cur.x == gx && cur.y == gy) return reconstruct(cur);

            for (int i=0;i<4;i++) {
                int nx = cur.x + dx[i];
                int ny = cur.y + dy[i];
                if (!map.isWalkable(nx, ny)) continue;

                int ng = cur.g + 1;
                if (ng >= bestG[nx][ny]) continue;

                bestG[nx][ny] = ng;
                int nf = ng + h(nx, ny, gx, gy);
                open.add(new Node(nx, ny, ng, nf, cur));
            }
        }

        return List.of(new P(sx, sy));
    }

    private static int h(int x, int y, int gx, int gy) {
        return Math.abs(x - gx) + Math.abs(y - gy);
    }

    private static List<P> reconstruct(Node goal) {
        ArrayList<P> path = new ArrayList<>();
        Node n = goal;
        while (n != null) { path.add(new P(n.x, n.y)); n = n.parent; }
        Collections.reverse(path);
        return path;
    }
}

