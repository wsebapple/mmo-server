package com.example.mmo.world;

public sealed interface InputCommand permits MoveCommand, TargetCommand, PickupCommand {
    long playerId();
}
