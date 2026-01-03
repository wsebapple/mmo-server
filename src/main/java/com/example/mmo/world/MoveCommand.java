package com.example.mmo.world;

public record MoveCommand(long playerId, float x, float y, long seq) implements InputCommand {}

