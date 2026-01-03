package com.example.mmo.protocol;

public enum MsgType {
    AUTH,
    MOVE_REQ,
    TARGET_REQ,
    PICKUP_REQ,

    WELCOME,
    STATE_DELTA,
    COMBAT,
    DROP_SPAWN,
    DROP_REMOVE,
    ERROR
}
