package kr.ac.ewha.catchMind.handler;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import kr.ac.ewha.catchMind.model.GameRoom;
import kr.ac.ewha.catchMind.model.GameRoomManager;
import kr.ac.ewha.catchMind.model.Player;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.databind.ObjectMapper;

import kr.ac.ewha.catchMind.model.GameMessage;
import kr.ac.ewha.catchMind.service.GameService;

@Component
public class GameSocketHandler extends TextWebSocketHandler {

    private static final List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();

    private final GameService gameService;
    private final GameRoomManager gameRoomManager;

    private final Map<WebSocketSession, String> sessionRoomMap = new ConcurrentHashMap<WebSocketSession, String>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    public GameSocketHandler(GameService gameService, GameRoomManager gameRoomManager) {
        this.gameService = gameService;
        this.gameRoomManager = gameRoomManager;
    }

    private void broadcastToRoom(String roomId, String jsonMessage) {
        for (WebSocketSession session : sessions) {
            if (!session.isOpen()) {
                continue;
            }
            String sessionRoomId = sessionRoomMap.get(session);
            if (roomId != null && roomId.equals(sessionRoomId)) {
                try {
                    session.sendMessage(new TextMessage(jsonMessage));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    public void broadcast(String jsonMessage) {
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(jsonMessage));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void sendRoundStart(String roomId, GameMessage msg) {
        try {
            String json = objectMapper.writeValueAsString(msg);
            broadcastToRoom(roomId, json);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleNextRound(WebSocketSession session, GameMessage msg) throws IOException {
        String roomId = sessionRoomMap.get(session);
        if (roomId == null) {
            System.out.println("roomId 없는 세션에서 NEXT_ROUND, 무시");
            return;
        }

        msg.setRoomId(roomId);
        msg.setType("NEXT_ROUND");

        String json = objectMapper.writeValueAsString(msg);
        broadcastToRoom(roomId, json);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        System.out.println("새 플레이어 접속: " + session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();    // 클라이언트가 보낸 JSON 문자열 꺼내기

        GameMessage gameMsg = objectMapper.readValue(payload, GameMessage.class);
        String type = gameMsg.getType();

        if (type == null) return;

        switch (type) {
            // 입장
            case "JOIN":
                System.out.println(gameMsg.getNickname() + "님이 입장했습니다.");
                handleJoin(session, gameMsg);
                break;

            // 그림 그리기
            case "DRAW":
                handleDraw(session, gameMsg);
                break;
            case "CLEAR_CANVAS":
                handleClearCanvas(session, gameMsg);
                break;

            case "GAME_START":
                handleGameStart(session, gameMsg);
                break;
            case "NEXT_ROUND":
                handleNextRound(session, gameMsg);
                break;
            default:
                System.out.println(type + ": 제대로 된 형식이 아닙니다.");
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        sessionRoomMap.remove(session);
        System.out.println("플레이어 접속 해제: " + session.getId());
    }

    public void sendGuessResult(String roomId, boolean correct, int triesLeft, int totalScore, int roundScore, int currentRound) {
        try {
            GameMessage msg = new GameMessage();
            msg.setType("GUESS_RESULT");
            msg.setRoomId(roomId);
            msg.setCorrect(correct);
            msg.setTriesLeft(triesLeft);
            msg.setTotalScore(totalScore);
            msg.setRoundScore(roundScore);
            msg.setRound(currentRound);

            String json = objectMapper.writeValueAsString(msg);
            broadcastToRoom(roomId, json);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendRoundEnd(String roomId,
                             int round,
                             String answer,
                             boolean roundSuccess) {
        try {
            GameMessage end = new GameMessage();
            end.setType("ROUND_END");
            end.setRoomId(roomId);
            end.setRound(round);
            end.setAnswer(answer);
            end.setRoundSuccess(roundSuccess);

            String json = objectMapper.writeValueAsString(end);
            broadcastToRoom(roomId, json);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleJoin(WebSocketSession session, GameMessage msg) throws IOException {
        String roomId = msg.getRoomId();
        if (roomId == null) {
            System.out.println("JOIN 메시지에 roomId 없음");
            return;
        }

        sessionRoomMap.put(session, roomId);
        System.out.println("세션 " + session.getId() + " 이(가) 방 " + roomId + " 에 참여");

        String json = objectMapper.writeValueAsString(msg);
        broadcastToRoom(roomId, json);

        GameRoom room = gameRoomManager.getGameRoom(roomId);
        if (room != null && room.getPlayerList().size() == room.getCapacity()) {
            GameMessage start = new GameMessage();
            start.setType("ROUND_START");
            start.setRoomId(roomId);
            start.setRound(gameService.getCurrentRound(room));
            start.setDrawerName(gameService.getDrawerName(room.getPlayerList()));
            start.setGuesserName(gameService.getGuesserName(room.getPlayerList()));

            sendRoundStart(roomId, start);
        }
    }

    private void handleDraw(WebSocketSession session, GameMessage msg) throws IOException {
        String roomId = sessionRoomMap.get(session);
        if (roomId == null) {
            System.out.println("roomId 없는 세션에서 DRAW 수신, 무시");
            return;
        }

        msg.setRoomId(roomId);

        String json = objectMapper.writeValueAsString(msg);
        broadcastToRoom(roomId, json);
    }

    public void sendPlayerList(String roomId, List<Player> players) {
        try {
            GameMessage msg = new GameMessage();
            msg.setType("PLAYER_LIST");
            msg.setRoomId(roomId);
            List<String> names = players.stream().map(Player::getName).toList();
            msg.setPlayers(names);
            String json = objectMapper.writeValueAsString(msg);
            broadcastToRoom(roomId, json);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleClearCanvas(WebSocketSession session, GameMessage msg) throws IOException {
        String roomId = sessionRoomMap.get(session);
        System.out.println("[WS] CLEAR_CANVAS 받음, roomId=" + roomId );
        if (roomId == null) {
            System.out.println("roomId 없는 세션에서 CLEAR_CANVAS, 무시");
            return;
        }

        // 혹시 클라이언트에서 roomId 안 채워서 보냈어도 서버에서 강제로 세팅
        msg.setRoomId(roomId);
        msg.setType("CLEAR_CANVAS");

        String json = objectMapper.writeValueAsString(msg);
        broadcastToRoom(roomId, json);
    }

    private void handleGameStart(WebSocketSession session, GameMessage msg) throws IOException {
        String roomId = sessionRoomMap.get(session);
        if (roomId == null) {
            System.out.println("roomId 없는 세션에서 GAME_START, 무시");
            return;
        }

        msg.setRoomId(roomId);
        msg.setType("GAME_START");

        String json = objectMapper.writeValueAsString(msg);
        broadcastToRoom(roomId, json);
    }
}