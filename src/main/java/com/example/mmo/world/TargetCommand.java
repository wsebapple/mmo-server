package com.example.mmo.world;

public record TargetCommand(long playerId, long targetId) implements InputCommand {}

