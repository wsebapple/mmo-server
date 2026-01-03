package com.example.mmo.world;

public class Drop extends Entity {
    public final int itemId;

    public Drop(long id, float x, float y, int itemId) {
        super(id, x, y, 1);
        this.itemId = itemId;
        this.hp = 1;
    }
}
