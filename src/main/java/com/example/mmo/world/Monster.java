package com.example.mmo.world;

public class Monster extends Entity {
    // ===== AI 전용 =====
    public float wanderCd = 0f;
    public float stuckSec = 0f;

    public Monster(long id, float x, float y) {
        super(id, x, y, 60);
        this.moveSpeed = 2.2f;   // 몬스터는 느리게
    }
}
