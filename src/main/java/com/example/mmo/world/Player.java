package com.example.mmo.world;

public class Player extends Entity {

    // ===== 레벨/경험치 =====
    public int level = 1;
    public long exp = 0; // 현재 레벨 내 경험치

    // 전투
    public long targetId = 0;
    public int attackDamage = 10;
    public float attackRange = 1.6f;
    public float attackIntervalSec = 0.8f;
    public float attackCd = 0f;
    public float repathCd = 0f;

    public Player(long id, float x, float y) {
        super(id, x, y, 100);
        this.moveSpeed = 4.0f;
    }

    public void onLevelUp() {
        // 예시: 레벨업 때마다 최대 체력 증가 + 현재 체력 일부 회복
        this.maxHp = 100 + (level - 1) * 10;
        this.hp = Math.min(this.maxHp, this.hp + 15);
        this.attackDamage = this.attackDamage + 2;
        this.attackRange = this.attackRange + 0.05f;
        this.attackIntervalSec = Math.max(0.25f, this.attackIntervalSec - 0.01f);
    }
}
