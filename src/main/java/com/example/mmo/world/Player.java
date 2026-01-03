package com.example.mmo.world;

public class Player extends Entity {
    public long targetId = 0;
    public float attackRange = 1.6f;
    public float attackIntervalSec = 0.8f;
    public float attackCd = 0f;

    public float repathCd = 0f; // 추적 재계산용 (전투 로직에서만 사용)

    public Player(long id, float x, float y) {
        super(id, x, y, 100);
        this.moveSpeed = 4.0f;   // ⭐ 반드시 필요
    }
}
