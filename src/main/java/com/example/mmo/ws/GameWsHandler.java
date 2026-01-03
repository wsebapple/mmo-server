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
        // ✅ 여기서 Player 만들지 않음
        // 클라가 AUTH를 보내면 그때 addOrResumePlayer로 확정
    }

    private void sendJson(WebSocketSession session, Object msg) {
        try {
            session.sendMessage(new TextMessage(om.writeValueAsString(msg)));
        } catch (Exception ignore) {}
    }

    private void sendWelcome(WebSocketSession session, Player p) {
        WelcomePayload wp = new WelcomePayload();
        wp.playerId = p.id;
        wp.mapId = world.mapId;
        wp.tickRate = world.tickRate;

        sendJson(session, new WsMessage<>(MsgType.WELCOME, wp));
    }

    private void sendError(WebSocketSession session, String message) {
        ErrorPayload ep = new ErrorPayload();
        ep.message = message;
        sendJson(session, new WsMessage<>(MsgType.ERROR, ep));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JsonNode root = om.readTree(message.getPayload());
        MsgType type = MsgType.valueOf(root.get("type").asText());
        JsonNode payload = root.get("payload");

        // ✅ AUTH 처리
        if (type == MsgType.AUTH) {
            // 이미 인증된 세션이면 무시 (중복 AUTH 방지)
            Long already = reg.sessionToPlayer.get(session.getId());
            if (already != null) {
                return;
            }

            AuthPayload ap = om.treeToValue(payload, AuthPayload.class);
            Player p = world.addOrResumePlayer(ap.playerId);

            // 같은 playerId로 기존 세션이 붙어있으면 정리
            WebSocketSession old = reg.playerToSession.get(p.id);
            if (old != null && old.isOpen() && !old.getId().equals(session.getId())) {
                try { old.close(CloseStatus.NORMAL); } catch (Exception ignore) {}
            }

            reg.sessionToPlayer.put(session.getId(), p.id);
            reg.playerToSession.put(p.id, session);

            sendWelcome(session, p);
            return;
        }

        // ✅ AUTH 이전 메시지면 에러 내려주기(디버깅 편의)
        Long playerId = reg.sessionToPlayer.get(session.getId());
        if (playerId == null) {
            sendError(session, "AUTH first");
            return;
        }

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
            default -> {
                // 무시
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long playerId = reg.sessionToPlayer.remove(session.getId());
        if (playerId != null) {
            reg.playerToSession.remove(playerId);

            // ✅ 즉시 삭제하지 말고 유예 처리 (새로고침 대응)
            world.onDisconnect(playerId);
        }
    }
}
