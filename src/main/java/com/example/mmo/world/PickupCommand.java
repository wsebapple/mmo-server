package com.example.mmo.world;

public record PickupCommand(long playerId, long dropId) implements InputCommand {}
