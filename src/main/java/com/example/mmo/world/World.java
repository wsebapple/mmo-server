package com.example.mmo.world;

import com.example.mmo.logic.AStar;
import com.example.mmo.logic.AStar.P;
import com.example.mmo.protocol.MsgType;
import com.example.mmo.protocol.WsMessage;
import com.example.mmo.protocol.dto.CombatPayload;
import com.example.mmo.protocol.dto.EntityView;
import com.example.mmo.protocol.dto.StateDeltaPayload;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

public class World {
    public final String mapId = "MVP-ISO-1";
    public final int tickRate = 20;
    public final int maxMonsterSpawnLimit = 25;
    public long tick = 0;

    private final ObjectMapper om;
    private final Random rnd = new Random();
    private final AtomicLong idGen = new AtomicLong(1000);

    public final GridMap map = GridMap.makeMvp();

    public final Map<Long, Player> players = new HashMap<>();
    public final Map<Long, Monster> monsters = new HashMap<>();
    public final Map<Long, Drop> drops = new HashMap<>();

    private final Map<Long, Float> respawnRemainSec = new HashMap<>();
    public final ConcurrentLinkedQueue<InputCommand> inputQueue = new ConcurrentLinkedQueue<>();

    private final Map<Long, Map<Long, EntityView>> lastSent = new HashMap<>();

    private final Map<Integer, Long> occ = new HashMap<>(); // tileKey -> entityId

    private int key(int x, int y) {
        return (x << 16) ^ (y & 0xFFFF);
    }

    public interface Outbound {
        void sendTo(long playerId, String json);
    }

    private final Outbound outbound;

    public World(Outbound outbound, ObjectMapper om) {
        this.outbound = outbound;
        this.om = om;
        spawnMonsters();
    }

    public Player addPlayer() {
        long id = idGen.incrementAndGet();
        int[] sp = map.randomWalkable(rnd, 10, 10, 18, 18);
        Player p = new Player(id, sp[0], sp[1]);
        players.put(id, p);
        lastSent.put(id, new HashMap<>());

        occ.put(key(Math.round(p.x), Math.round(p.y)), p.id);
        return p;
    }

    public void removePlayer(long playerId) {
        Player p = players.remove(playerId);
        lastSent.remove(playerId);

        if (p != null) {
            occ.remove(key(Math.round(p.x), Math.round(p.y)));
        }
    }

    public void enqueue(InputCommand cmd) {
        inputQueue.offer(cmd);
    }

    private void spawnMonsters() {
        for (long id = 1; id <= maxMonsterSpawnLimit; id++) {
            int[] pos = map.randomWalkable(rnd, 18, 12, 40, 30);
            int lvl = rollMonsterLevelForSpawn(pos[0], pos[1]);
            Monster m = new Monster(id, pos[0], pos[1], lvl);
            monsters.put(id, m);
            occ.put(key(Math.round(m.x), Math.round(m.y)), m.id);
        }
    }

    public void step(float dtSec) {
        tick++;

        // 1) 입력 반영
        for (int i = 0; i < 4000; i++) {
            InputCommand cmd = inputQueue.poll();
            if (cmd == null) break;

            Player p = players.get(cmd.playerId());
            if (p == null) continue;

            if (cmd instanceof MoveCommand mc) {
                int sx = Math.round(p.x);
                int sy = Math.round(p.y);
                int gx = map.clampX(Math.round(mc.x()));
                int gy = map.clampY(Math.round(mc.y()));

                List<P> path = AStar.findPath(map, sx, sy, gx, gy);
                if (path == null || path.size() <= 1) {
                    p.pathTiles.clear();
                    continue;
                }

                p.pathTiles.clear();
                for (int pi = 1; pi < path.size(); pi++) {
                    P pt = path.get(pi);
                    p.pathTiles.addLast(new int[]{pt.x(), pt.y()});
                }
            } else if (cmd instanceof TargetCommand tc) {
                p.targetId = tc.targetId();
            } else if (cmd instanceof PickupCommand pc) {
                Drop d = drops.get(pc.dropId());
                if (d == null) continue;
                if (dist(p.x, p.y, d.x, d.y) <= 1.6f) {
                    drops.remove(d.id);

                    // 회복 및 경험치
                    p.hp = Math.min(p.maxHp, p.hp + 10);
                    Leveling.addExpAndLevelUp(p, 100);
                }
            }
        }

        // 2) 이동
        for (Player p : players.values()) {
            moveByPath(p, dtSec);
        }

        // 몬스터 배회(전투 중인 몹은 배회 금지)
        monsterWanderAI(dtSec);

        // 몬스터 이동
        for (Monster m : monsters.values()) {
            moveByPath(m, dtSec);
        }

        // 3) 전투(플레이어 -> 몬스터)
        List<CombatPayload> combats = new ArrayList<>();
        for (Player p : players.values()) {
            if (!p.isAlive()) continue;

            if (p.attackCd > 0) p.attackCd -= dtSec;
            if (p.targetId == 0) continue;

            Monster target = monsters.get(p.targetId);
            if (target == null || !target.isAlive()) {
                p.targetId = 0;
                continue;
            }

            float dist = dist(p.x, p.y, target.x, target.y);

            // 사거리 밖이면 A* 추적(쿨다운)
            if (dist > p.attackRange) {
                if (p.repathCd > 0) p.repathCd -= dtSec;
                if (p.repathCd <= 0f) {
                    p.repathCd = 0.35f;
                    int sx = Math.round(p.x), sy = Math.round(p.y);
                    int gx = Math.round(target.x), gy = Math.round(target.y);

                    List<P> path = AStar.findPath(map, sx, sy, gx, gy);
                    if (path == null || path.size() <= 1) {
                        p.pathTiles.clear();
                        continue;
                    }

                    p.pathTiles.clear();
                    for (int i = 1; i < path.size(); i++) {
                        P pt = path.get(i);
                        p.pathTiles.addLast(new int[]{pt.x(), pt.y()});
                    }
                }
                continue;
            }

            // 사거리 안이면 공격
            if (p.attackCd <= 0f) {
                p.attackCd = p.attackIntervalSec;

                boolean miss = Math.random() < 0.08;
                boolean crit = !miss && Math.random() < 0.15;

                int base = p.attackDamage;
                int dmg = miss ? 0 : (crit ? (int) (base * 1.8f) : base);

                // ✅ (중요) 몬스터 HP 감소
                if (!miss) {
                    target.hp = Math.max(0, target.hp - dmg);
                }

                // ✅ 맞은 몬스터는 공격자에게 반격(어그로 갱신)
                if (!miss && target.isAlive()) {
                    target.targetId = p.id;
                    target.aggroRemainSec = 6.0f;
                }

                CombatPayload cp = new CombatPayload();
                cp.tick = tick;
                cp.attackerId = p.id;
                cp.targetId = target.id;
                cp.dmg = dmg;
                cp.miss = miss;
                cp.crit = crit;
                cp.targetHp = target.hp;
                combats.add(cp);

                // 몬스터 사망 처리
                if (!target.isAlive()) {
                    // EXP 지급
                    long gain = Leveling.calcKillExp(p, target);
                    Leveling.addExpAndLevelUp(p, gain);

                    // 점유 해제
                    occ.remove(key(Math.round(target.x), Math.round(target.y)));

                    // 상태 초기화
                    target.pathTiles.clear();
                    target.stuckSec = 0f;
                    target.targetId = 0;
                    target.aggroRemainSec = 0f;
                    target.attackCd = 0f;
                    target.repathCd = 0f;

                    maybeSpawnDrop(target.x, target.y);
                    respawnRemainSec.put(target.id, 12f + (float) (Math.random() * 10f));
                }
            }
        }

        // 3.2) 전투(몬스터 반격: 몬스터 -> 플레이어)
        for (Monster m : monsters.values()) {
            if (!m.isAlive()) continue;

            // 어그로 시간 감소 (전투 상태 유지 핵심)
            if (m.aggroRemainSec > 0f) m.aggroRemainSec -= dtSec;

            if (m.attackCd > 0) m.attackCd -= dtSec;

            // 타겟 없거나 어그로 끝나면 전투 종료
            if (m.targetId == 0 || m.aggroRemainSec <= 0f) {
                m.targetId = 0;
                m.pathTiles.clear();
                continue;
            }

            Player tp = players.get(m.targetId);
            if (tp == null || !tp.isAlive()) {
                m.targetId = 0;
                m.aggroRemainSec = 0f;
                m.pathTiles.clear();
                continue;
            }

            float d = dist(m.x, m.y, tp.x, tp.y);

            // 너무 멀면 포기(즉시)
            float maxAggroRange = 10f;
            if (d > maxAggroRange) {
                m.targetId = 0;
                m.aggroRemainSec = 0f;
                m.pathTiles.clear();
                continue;
            }

            // 사거리 밖이면 추적(A*)
            if (d > m.attackRange) {
                if (m.repathCd > 0) m.repathCd -= dtSec;
                if (m.repathCd <= 0f) {
                    m.repathCd = 0.45f;

                    int sx = Math.round(m.x), sy = Math.round(m.y);
                    int gx = Math.round(tp.x), gy = Math.round(tp.y);

                    List<P> path = AStar.findPath(map, sx, sy, gx, gy);
                    if (path == null || path.size() <= 1) {
                        m.pathTiles.clear();
                    } else {
                        m.pathTiles.clear();
                        for (int i = 1; i < path.size(); i++) {
                            P pt = path.get(i);
                            m.pathTiles.addLast(new int[]{pt.x(), pt.y()});
                        }
                    }
                }
                continue;
            }

            // 사거리 안이면 공격
            if (m.attackCd <= 0f) {
                m.attackCd = m.attackIntervalSec;

                boolean miss = Math.random() < 0.10;
                boolean crit = !miss && Math.random() < 0.10;

                int base = m.attackDamage;
                int dmg = miss ? 0 : (crit ? (int) (base * 1.7f) : base);

                if (!miss) tp.hp = Math.max(0, tp.hp - dmg);

                CombatPayload cp = new CombatPayload();
                cp.tick = tick;
                cp.attackerId = m.id;
                cp.targetId = tp.id;
                cp.dmg = dmg;
                cp.miss = miss;
                cp.crit = crit;
                cp.targetHp = tp.hp;
                combats.add(cp);

                // 플레이어 사망 처리(간단)
                if (!tp.isAlive()) {
                    tp.targetId = 0;
                    m.targetId = 0;
                    m.aggroRemainSec = 0f;
                    m.pathTiles.clear();
                }
            }
        }

        // 3.5) 리스폰 카운트
        var it = respawnRemainSec.entrySet().iterator();
        while (it.hasNext()) {
            var e = it.next();
            float remain = e.getValue() - dtSec;
            if (remain > 0) {
                e.setValue(remain);
                continue;
            }

            long mid = e.getKey();
            Monster m = monsters.get(mid);
            if (m != null) {
                int[] pos = map.randomWalkable(rnd, 18, 12, 40, 30);
                m.x = pos[0];
                m.y = pos[1];

                int newLevel = rollMonsterLevelForSpawn(pos[0], pos[1]);
                m.resetForRespawn(newLevel);

                occ.put(key(Math.round(m.x), Math.round(m.y)), m.id);
            }
            it.remove();
        }

        // 4) AOI 델타 전송
        for (Player viewer : players.values()) {
            float viewRange = 18f;
            Map<Long, EntityView> prev = lastSent.get(viewer.id);
            if (prev == null) continue;

            Map<Long, EntityView> now = new HashMap<>();

            for (Player p : players.values()) {
                if (!Aoi.inRange(viewer, p, viewRange)) continue;
                now.put(p.id, toView(p, "P"));
            }

            for (Monster m : monsters.values()) {
                if (!m.isAlive()) continue;
                if (!Aoi.inRange(viewer, m, viewRange)) continue;
                now.put(m.id, toView(m, "M"));
            }

            for (Drop d : drops.values()) {
                if (!Aoi.inRange(viewer, d, viewRange)) continue;
                now.put(d.id, toView(d, "D"));
            }

            List<EntityView> updates = new ArrayList<>();
            List<Long> removes = new ArrayList<>();

            for (var en : now.entrySet()) {
                EntityView cur = en.getValue();
                EntityView old = prev.get(en.getKey());
                if (old == null || changed(old, cur)) updates.add(cur);
            }
            for (var oldId : prev.keySet()) {
                if (!now.containsKey(oldId)) removes.add(oldId);
            }

            prev.clear();
            prev.putAll(now);

            if (!updates.isEmpty() || !removes.isEmpty()) {
                StateDeltaPayload payload = new StateDeltaPayload();
                payload.tick = tick;
                payload.updates = updates;
                payload.removes = removes;
                send(viewer.id, new WsMessage<>(MsgType.STATE_DELTA, payload));
            }

            // 전투 이벤트: 근처면 전송(공격자/피격자 P/M 모두 지원)
            for (CombatPayload cp : combats) {
                Entity a = players.get(cp.attackerId);
                if (a == null) a = monsters.get(cp.attackerId);

                Entity t = players.get(cp.targetId);
                if (t == null) t = monsters.get(cp.targetId);

                if (a == null || t == null) continue;

                if (Aoi.inRange(viewer, a, viewRange) || Aoi.inRange(viewer, t, viewRange)) {
                    send(viewer.id, new WsMessage<>(MsgType.COMBAT, cp));
                }
            }
        }
    }

    private void moveByPath(Entity e, float dt) {
        if (!e.isAlive()) return;

        if (e.moving) {
            float dx = e.toX - e.x;
            float dy = e.toY - e.y;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);

            float maxMove = e.moveSpeed * dt;
            if (maxMove <= 0f) return;

            if (dist <= maxMove) {
                e.x = e.toX;
                e.y = e.toY;
                e.moving = false;
            } else {
                float t = maxMove / dist;
                e.x += dx * t;
                e.y += dy * t;
            }
            return;
        }

        if (e.pathTiles.isEmpty()) return;

        int[] next = e.pathTiles.peekFirst();
        int nx = next[0];
        int ny = next[1];

        int cx = Math.round(e.x);
        int cy = Math.round(e.y);

        if (cx == nx && cy == ny) {
            e.pathTiles.pollFirst();
            return;
        }

        if (!map.isWalkable(nx, ny)) {
            e.pathTiles.clear();
            return;
        }

        int nKey = key(nx, ny);
        Long blocker = occ.get(nKey);
        if (blocker != null && blocker != e.id) {
            return;
        }

        occ.remove(key(cx, cy));
        occ.put(nKey, e.id);

        e.toX = nx;
        e.toY = ny;
        e.moving = true;

        e.pathTiles.pollFirst();
    }

    private void monsterWanderAI(float dt) {
        for (Monster m : monsters.values()) {
            if (!m.isAlive()) continue;

            // ✅ 전투 중이면 배회 금지
            if (m.targetId != 0 && m.aggroRemainSec > 0f) continue;

            if (!m.pathTiles.isEmpty()) continue;

            if (m.wanderCd > 0) {
                m.wanderCd -= dt;
                continue;
            }

            m.wanderCd = 1.2f + (float) (Math.random() * 1.6f);

            int cx = Math.round(m.x);
            int cy = Math.round(m.y);

            int r = 8 + (int) (Math.random() * 7);
            int gx = cx + (int) (Math.random() * (2 * r + 1)) - r;
            int gy = cy + (int) (Math.random() * (2 * r + 1)) - r;

            gx = map.clampX(gx);
            gy = map.clampY(gy);

            int[] g = map.findNearestWalkable(gx, gy);
            gx = g[0];
            gy = g[1];

            Long occId = occ.get(key(gx, gy));
            if (occId != null && occId != m.id) continue;

            List<P> path = AStar.findPath(map, cx, cy, gx, gy);
            if (path == null || path.size() <= 1) {
                m.pathTiles.clear();
                continue;
            }

            m.pathTiles.clear();
            for (int i = 1; i < path.size(); i++) {
                P pt = path.get(i);
                m.pathTiles.addLast(new int[]{pt.x(), pt.y()});
            }
        }
    }

    private void maybeSpawnDrop(float x, float y) {
        if (Math.random() < 0.65) {
            long id = idGen.incrementAndGet();
            int itemId = (int) (1 + Math.random() * 5);
            Drop d = new Drop(id, x + 0.3f, y + 0.3f, itemId);
            drops.put(d.id, d);
        }
    }

    private float dist(float x1, float y1, float x2, float y2) {
        float dx = x1 - x2, dy = y1 - y2;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private boolean changed(EntityView a, EntityView b) {
        if (a.hp != b.hp) return true;
        if (a.targetId != b.targetId) return true;
        return Math.abs(a.x - b.x) > 0.02f || Math.abs(a.y - b.y) > 0.02f;
    }

    private EntityView toView(Entity e, String kind) {
        EntityView v = new EntityView();
        v.id = e.id;
        v.kind = kind;
        v.x = e.x;
        v.y = e.y;
        v.hp = e.hp;
        v.maxHp = e.maxHp;

        if (e instanceof Player p) {
            v.targetId = p.targetId;
            v.level = p.level;
            v.exp = p.exp;
            v.expNeed = Leveling.requiredExp(p.level);
        } else if (e instanceof Monster m) {
            v.targetId = 0;
            v.level = m.level;
        } else {
            v.targetId = 0;
        }
        return v;
    }

    private void send(long playerId, Object msg) {
        try {
            outbound.sendTo(playerId, om.writeValueAsString(msg));
        } catch (Exception ignore) {
        }
    }

    private int rollMonsterLevelForSpawn(int x, int y) {
        int base = 1;
        base += (x / 15);
        base += (y / 15);

        int jitter = rnd.nextInt(3) - 1; // -1,0,1
        int lvl = base + jitter;

        if (lvl < 1) lvl = 1;
        if (lvl > 30) lvl = 30;
        return lvl;
    }
}
