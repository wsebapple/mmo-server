package com.example.mmo.protocol;

public class WsMessage<T> {
    public MsgType type;
    public T payload;

    public WsMessage() {}
    public WsMessage(MsgType type, T payload) {
        this.type = type;
        this.payload = payload;
    }
}
