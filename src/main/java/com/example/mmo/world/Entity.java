package com.example.mmo.world;

import java.util.ArrayDeque;
import java.util.Deque;

public abstract class Entity {
    public final long id;
    public float x, y;
    public int hp, maxHp;

    // ===== 이동 공통 =====
    public float moveSpeed;          // 타일/초
    public boolean moving = false;   // 이동 중인지
    public int toX, toY;             // 목표 타일
    public final Deque<int[]> pathTiles = new ArrayDeque<>();

    public int fromX, fromY;

    protected Entity(long id, float x, float y, int maxHp) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.maxHp = maxHp;
        this.hp = maxHp;
    }

    public boolean isAlive() { return hp > 0; }
}
