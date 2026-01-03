package com.example.mmo.ws;

import com.example.mmo.protocol.*;
import com.example.mmo.protocol.dto.*;
import com.example.mmo.world.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

public class GameWsHandler extends TextWebSocketHandler {
    private final World world;
    private final SessionRegistry reg;
    private final ObjectMapper om;

    public GameWsHandler(World world, SessionRegistry reg, ObjectMapper om) {
        this.world = world;
        this.reg = reg;
        this.om = om;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Player p = world.addPlayer();
        reg.sessionToPlayer.put(session.getId(), p.id);
        reg.playerToSession.put(p.id, session);

        WelcomePayload wp = new WelcomePayload();
        wp.playerId = p.id;
        wp.mapId = world.mapId;
        wp.tickRate = world.tickRate;

        session.sendMessage(new TextMessage(om.writeValueAsString(new WsMessage<>(MsgType.WELCOME, wp))));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long playerId = reg.sessionToPlayer.get(session.getId());
        if (playerId == null) return;

        JsonNode root = om.readTree(message.getPayload());
        MsgType type = MsgType.valueOf(root.get("type").asText());
        JsonNode payload = root.get("payload");

        switch (type) {
            case MOVE_REQ -> {
                MoveReqPayload p = om.treeToValue(payload, MoveReqPayload.class);
                world.enqueue(new MoveCommand(playerId, p.x, p.y, p.seq));
            }
            case TARGET_REQ -> {
                TargetReqPayload p = om.treeToValue(payload, TargetReqPayload.class);
                world.enqueue(new TargetCommand(playerId, p.targetId));
            }
            case PICKUP_REQ -> {
                PickupReqPayload p = om.treeToValue(payload, PickupReqPayload.class);
                world.enqueue(new PickupCommand(playerId, p.dropId));
            }
            default -> {}
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long playerId = reg.sessionToPlayer.remove(session.getId());
        if (playerId != null) {
            reg.playerToSession.remove(playerId);
            world.removePlayer(playerId);
        }
    }
}
