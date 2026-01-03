package com.example.mmo.protocol.dto;

import java.util.List;

public class StateDeltaPayload {
    public long tick;
    public List<EntityView> updates;
    public List<Long> removes;
}
