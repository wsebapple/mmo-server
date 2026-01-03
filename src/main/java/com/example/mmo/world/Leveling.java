package com.example.mmo.world;

public final class Leveling {
    private Leveling() {}

    // 레벨업에 필요한 경험치(현재 레벨 -> 다음 레벨)
    public static long requiredExp(int level) {
        // 샘플: 초반은 빠르게, 후반은 점점 무겁게
        // level=1 -> 120, level=10 -> 1200+...
        return 80L + (long) level * level * 40L;
    }

    // 몬스터 레벨차 보정 배율
    public static float levelDiffMultiplier(int playerLevel, int monsterLevel) {
        int diff = monsterLevel - playerLevel;

        if (diff >= 5) return 1.50f;
        if (diff == 4) return 1.35f;
        if (diff == 3) return 1.25f;
        if (diff == 2) return 1.15f;
        if (diff == 1) return 1.07f;
        if (diff == 0) return 1.00f;
        if (diff == -1) return 0.90f;
        if (diff == -2) return 0.80f;
        if (diff == -3) return 0.70f;
        if (diff == -4) return 0.55f;

        // 너무 약한 몹(회색몹) 처리 정책
        // 0으로 하면 “경험치 파밍 방지”가 강해지고,
        // 0.40이면 “조금은 주되 의미는 없음” 정도가 됩니다.
        return 0.40f;
    }

    // 킬 경험치 계산(지금은 몬스터 baseExp 기반)
    public static long calcKillExp(Player p, Monster m) {
        // 예: 플레이어가 몹보다 10 이상 높으면 0 처리하고 싶으면 여기서 컷
//        if (p.level >= m.level + 10) return 0;

        float mul = levelDiffMultiplier(p.level, m.level);
        long raw = Math.round(m.baseExp * mul);
        return Math.max(0, raw);
    }

    public static void addExpAndLevelUp(Player p, long gainExp) {
        if (gainExp <= 0) return;

        p.exp += gainExp;

        // 여러 레벨업 한번에 처리
        while (true) {
            long need = requiredExp(p.level);
            if (p.exp < need) break;
            p.exp -= need;
            p.level++;
            p.onLevelUp();
        }
    }
}
