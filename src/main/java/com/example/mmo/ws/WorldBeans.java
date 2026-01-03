package com.example.mmo.ws;

import com.example.mmo.world.World;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class WorldBeans {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistry();
    }

    @Bean
    public World world(SessionRegistry reg, ObjectMapper om) {
        return new World((playerId, json) -> {
            WebSocketSession s = reg.playerToSession.get(playerId);
            if (s == null || !s.isOpen()) return;
            try { s.sendMessage(new TextMessage(json)); } catch (Exception ignore) {}
        }, om);
    }

    @Bean
    public GameWsHandler gameWsHandler(World world, SessionRegistry reg, ObjectMapper om) {
        return new GameWsHandler(world, reg, om);
    }
}
