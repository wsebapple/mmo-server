package com.example.mmo.world;

public class Monster extends Entity {

    // ===== 레벨/경험치 =====
    public int level;
    public int baseExp;

    // ===== 전투 =====
    public long targetId = 0;
    public int attackDamage;
    public float attackRange = 1.4f;
    public float attackIntervalSec = 1.2f;
    public float attackCd = 0f;
    public float repathCd = 0f;
    public float aggroRemainSec = 0f;

    // ===== AI =====
    public float wanderCd = 0f;
    public float stuckSec = 0f;

    public Monster(long id, float x, float y, int level) {
        super(id, x, y, calcMaxHp(level));
        this.level = level;
        this.baseExp = calcBaseExp(level);
        this.attackDamage = calcAttack(level);
        this.moveSpeed = 2.2f;
    }

    public void resetForRespawn(int newLevel) {
        this.level = newLevel;
        this.maxHp = calcMaxHp(newLevel);
        this.hp = this.maxHp;
        this.baseExp = calcBaseExp(newLevel);
        this.attackDamage = calcAttack(newLevel);

        this.targetId = 0;
        this.aggroRemainSec = 0f;
        this.attackCd = 0f;
        this.repathCd = 0f;

        this.pathTiles.clear();
        this.stuckSec = 0f;
        this.wanderCd = 0.5f + (float) Math.random();
    }

    private static int calcMaxHp(int level) {
        // 예: 레벨당 HP 증가 (원하시면 공격력/방어력도 같이 스케일링 가능)
        return 50 + level * 12;
    }

    private static int calcBaseExp(int level) {
        // 예: 레벨당 기본 경험치
        return 12 + level * 6;
    }

    private static int calcAttack(int level) {
        return 4 + level * 2;
    }
}
