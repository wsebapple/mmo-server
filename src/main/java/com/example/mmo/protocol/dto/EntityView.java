package com.example.mmo.protocol.dto;

public class EntityView {
    public long id;
    public String kind; // "P" | "M" | "D"
    public float x;
    public float y;
    public int hp;
    public int maxHp;
    public long targetId; // player only
}
