package kr.ac.ewha.catchMind.handler;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

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

    // 접속한 클라이언트 세션들을 모아두는 리스트, thread-safe 리스트로 세션관리
    private static final List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();

    //private final GameService gameService;

    private final Map<WebSocketSession, String> sessionRoomMap = new ConcurrentHashMap<WebSocketSession, String>();


    // JSON 문자열 <> java 객체 GameMessage 변환
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 생성자 주입
//    public GameSocketHandler(/*GameService gameService*/) {
//        //this.gameService = gameService;
//    }
    public GameSocketHandler() {

    }

    //    public static void broadcastStatic(String jsonMessage){
//        for (WebSocketSession session : sessions) {
//            if(session.isOpen()){
//                try {
//                    session.sendMessage(new TextMessage(jsonMessage));
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//    }
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

    // 👉 새로 추가: 특정 room 에 ROUND_START 같은 메시지 보낼 때 사용
    public void sendRoundStart(String roomId, GameMessage msg) {
        try {
            String json = objectMapper.writeValueAsString(msg);
            broadcastToRoom(roomId, json);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    // 소켓 연결 시
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        System.out.println("새 플레이어 접속: " + session.getId());
    }

    // 메시지 수신 시
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();    // 클라이언트가 보낸 JSON 문자열 꺼내기

        // JSON > GameMessage 객체 변환
        GameMessage gameMsg = objectMapper.readValue(payload, GameMessage.class);
        String type = gameMsg.getType();

        if (type == null) return;

        switch (type) {
            // 입장
            case "JOIN":
                System.out.println(gameMsg.getNickname() + "님이 입장했습니다.");
                handleJoin(session, gameMsg);

//                // 간단한 테스트용 응답 (실제 역할 배정은 HTTP 컨트롤러에서 했으므로 여기선 UI 표시용)
//                GameMessage syncMsg = new GameMessage();
//                syncMsg.setType("PLAYER_SYNC");
//                syncMsg.setDrawerName(gameMsg.getNickname()); // 일단 들어온 사람 이름 띄워주기
//                syncMsg.setGuesserName("상대방");
//                broadcast(objectMapper.writeValueAsString(syncMsg));
                break;

            // 그림 그리기
            case "DRAW":
                handleDraw(session, gameMsg);
                break;

//            // 정답 맞히기
//            case "GUESS":
//                handleGuess(gameMsg);
//                break;

//            // 시간 초과
//            case "TIME_OVER":
//                handleTimeOver();
//                break;

            default:
                System.out.println(type + ": 제대로 된 형식이 아닙니다.");
        }
    }

//    private void handleTimeOver() throws Exception {
//        System.out.println("시간 초과! 라운드 종료...");
//
//        gameService.isRoundOver(false);
//
//        // 종료 메시지 전송 (ROUND_END)
//        GameMessage endMsg = new GameMessage();
//        endMsg.setType("ROUND_END");
//        endMsg.setRoundSuccess(false);
//        endMsg.setText("시간 초과");
//
//        // 현재 정보 갱신
//        endMsg.setAnswer(gameService.getAnswer());
//        endMsg.setTotalScore(gameService.getScore());
//
//        // 라운드 번호 처리
//        endMsg.setRound(gameService.getCurrentRound() - 1);
//
//        broadcast(objectMapper.writeValueAsString(endMsg));
//    }

    // 소켓 종료 시
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        sessionRoomMap.remove(session);
        System.out.println("플레이어 접속 해제: " + session.getId());
    }

//    // 정답 처리 로직
//    private void handleGuess(GameMessage inputMsg) throws Exception {
//        // 정답 체크
//        boolean isCorrect = gameService.checkAnswer(inputMsg.getGuess());
//
//        // 라운드 종료 여부 체크
//        boolean isRoundOver = gameService.isRoundOver(isCorrect);
//
//        // GUESS_RESULT 메시지 생성
//        GameMessage result = new GameMessage();
//        result.setType("GUESS_RESULT");
//        result.setCorrect(isCorrect);
//        result.setAnswer(gameService.getAnswer());
//
//        // 현재 점수 및 상태 정보 불러오기
//        result.setTriesLeft(gameService.getTriesLeft());
//        result.setTotalScore(gameService.getScore());
//        result.setRoundScore(gameService.getCurrentRoundScore());
//
//        // 라운드 번호 처리(-1해야함)
//        int currentRound = gameService.getCurrentRound();
//        if (isRoundOver) {
//            result.setRound(currentRound - 1);
//        } else {
//            result.setRound(currentRound);
//        }
//
//        // 안내 텍스트
//        if (isCorrect) {
//            result.setText("정답입니다!");
//        } else {
//            result.setText("틀렸습니다. 다시 시도해보세요!");
//        }
//
//        // 결과 전송
//        broadcast(objectMapper.writeValueAsString(result));
//
//        // 라운드 종료 > ROUND_END 메시지 추가 전송
//        if (isRoundOver) {
//            GameMessage endMsg = new GameMessage();
//            endMsg.setType("ROUND_END");
//            endMsg.setRoundSuccess(isCorrect); // 맞혀서 끝난 건지, 5회 끝나서 종료인건지
//            endMsg.setText("라운드 종료!");
//
//            // 라운드 종료 시 점수/정답 확실하게 다시 보내주기
//            endMsg.setTotalScore(gameService.getScore());
//            endMsg.setAnswer(gameService.getAnswer());
//            endMsg.setRound(isRoundOver ? currentRound - 1 : currentRound);
//
//            broadcast(objectMapper.writeValueAsString(endMsg));
//        }
//    }

//    // 전체 방송
//    private void broadcast(String jsonMessage) {
//        for (WebSocketSession s : sessions) {
//            if (s.isOpen()) {
//                try {
//                    s.sendMessage(new TextMessage(jsonMessage));
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//    }
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

        // 입장 알림을 같은 방 사람들에게만 브로드캐스트
        String json = objectMapper.writeValueAsString(msg);
        broadcastToRoom(roomId, json);
    }

    private void handleDraw(WebSocketSession session, GameMessage msg) throws IOException {
        String roomId = sessionRoomMap.get(session);
        if (roomId == null) {
            System.out.println("roomId 없는 세션에서 DRAW 수신, 무시");
            return;
        }

        // 혹시 클라이언트에서 roomId 안 채워줬어도 서버에서 세팅
        msg.setRoomId(roomId);

        String json = objectMapper.writeValueAsString(msg);
        broadcastToRoom(roomId, json);
    }
}