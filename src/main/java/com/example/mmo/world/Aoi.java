package com.example.mmo.world;

public class Aoi {
    public static boolean inRange(Entity center, Entity other, float range) {
        float dx = center.x - other.x;
        float dy = center.y - other.y;
        return (dx*dx + dy*dy) <= range*range;
    }
}

