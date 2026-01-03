package com.example.mmo.ws;

import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SessionRegistry {
    public final Map<String, Long> sessionToPlayer = new ConcurrentHashMap<>();
    public final Map<Long, WebSocketSession> playerToSession = new ConcurrentHashMap<>();
}
