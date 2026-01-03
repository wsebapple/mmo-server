package com.example.mmo.protocol.dto;

public class CombatPayload {
    public long tick;
    public long attackerId;
    public long targetId;
    public int dmg;
    public boolean crit;
    public boolean miss;
    public int targetHp;
}
