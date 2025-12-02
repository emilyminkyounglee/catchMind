package kr.ac.ewha.catchMind.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;

import kr.ac.ewha.catchMind.handler.GameSocketHandler;
import kr.ac.ewha.catchMind.model.*;
import kr.ac.ewha.catchMind.service.GameService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.boot.Banner;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;



@Controller
@RequestMapping("/game")
public class GameController {
    private final GameService gameService;
    private final GameSocketHandler gameSocketHandler;
    private final GameRoomManager gameRoomManager;
//    private Player p1;
//    private Player p2;

    public GameController(GameService gameService, GameSocketHandler gameSocketHandler, GameRoomManager gameRoomManager) {
        this.gameService = gameService;
        this.gameSocketHandler = gameSocketHandler;
        this.gameRoomManager = gameRoomManager;
    }

    //공통으로 Model에 등록시킬 값들을 메서드로 처리
    private void addCommonAttributes(Model model, Player me, String roomId) {
        model.addAttribute("round", gameService.getCurrentRound());
        model.addAttribute("triesLeft", gameService.getTriesLeft());
        model.addAttribute("totalScore", gameService.getScore());

        // 나 정보
        if (me != null) {
            model.addAttribute("myName", me.getName());
            model.addAttribute("myRole", me.getRole());
        } else {
            model.addAttribute("myName", "나");
            model.addAttribute("myRole", null);
        }

        model.addAttribute("roomId", roomId);
    }

//    @PostMapping("/start")
//    public String startGame(@RequestParam String userId, HttpSession session, Model model) {
//
//        // 이 세션의 유저 ID를 저장해두기 (나중에 next-round에서 사용)
//        session.setAttribute("userId", userId);
//
//        if ("wkvmtlf2MEJ".equals(userId)) {
//            return  "redirect:/admin/words";
//        }
//
//        // 현재 구조: p1, p2 둘 다 같은 userId 로딩
//        // 나중에 진짜 2인 구조로 바꾸려면 여기 로직을
//        // "첫 번째 접속자는 p1, 두 번째 접속자는 p2" 식으로 분리해야 함
////        p1 = gameService.loadPlayer(userId);
////        p2 = gameService.loadPlayer(userId);
////
////        gameService.setRoleRandomly(p1, p2);
////        gameService.setupNewGame(p1, p2);
////        gameService.newRound();
////
////        String answerWord = gameService.getWordForDrawer();
////        gameService.setAnswer(answerWord);
////
////        // 요청 보낸 나(me)를 p1/p2 중에서 찾기
////        Player me = p1;
////        if (p2 != null && userId.equals(p2.getName())) {
////            me = p2;
////        }
//        if(gameService.isGameOver()) {
//            System.out.println("[LOG] reset game");
//            p1 = null;
//            p2 = null;
//            gameService.setupNewGame(null, null);
//        }
//        Player me;
//        if (p1 == null) {
//            p1 = gameService.loadPlayer(userId);
//            me = p1;
//        } else if (p2 == null && !p1.getName().equals(userId)) {
//            p2 = gameService.loadPlayer(userId);
//            me = p2;
//            gameService.setRoleRandomly(p1, p2);
//            gameService.setupNewGame(p1, p2);
//            gameService.startNewGameId();
//            gameService.newRound();
//
//            String answerWord = gameService.getWordForDrawer();
//            gameService.setAnswer(answerWord);
//        }
//        else {
//            if(p1 != null && p1.getName().equals(userId)) {
//                me = p1;
//            } else if (p2 != null && p2.getName().equals(userId)) {
//                me = p2;
//            }
//            else {
//                return  "redirect:/";
//            }
//        }
//
//        addCommonAttributes(model, me);
//
//        if (me.getRole() == Role.DRAWER) {
//            model.addAttribute("wordForDrawer", gameService.getAnswer());
//            return "mainUI_Drawer";
//        } else {
//            // redirect 말고 바로 Guesser 템플릿을 리턴해도 됨
//            return "mainUI_Guesser";
//        }
//    }
    @PostMapping("/start")
    public String start(@RequestParam int capacity,@RequestParam String userId, HttpSession session, Model model) {
        if("wkvmtlf2MEJ".equals(userId)){
            return "redirect:/admin/words";
        }
        session.setAttribute("userId", userId);
        Player me = gameService.loadPlayer(userId);
        if (capacity < 2 || capacity > 6) {
            capacity = 2;
        }
        GameRoom room = gameRoomManager.getOrAddGameRoom(capacity);
        room.addPlayer(me);

        session.setAttribute("roomId", room.getRoomId());

        model.addAttribute("roomId", room.getRoomId());
        model.addAttribute("myName", me.getName());
        model.addAttribute("players", room.getPlayerList());
        model.addAttribute("capacity", room.getCapacity());
        return "waitingRoom";
    }
    @PostMapping("/begin")
    public String begin(@RequestParam String roomId,@RequestParam String userId, HttpSession session, Model model) {
        GameRoom room = gameRoomManager.getGameRoom(roomId);
        if (room == null) {
            return "redirect:/";
        }
        session.setAttribute("userId", userId);
        session.setAttribute("roomId", roomId);
        Player me = gameService.loadPlayer(userId);
        if (me == null) {
            me = gameService.loadPlayer(userId);
            room.addPlayer(me);
        }

        if (gameService.isGameOver()) {
            System.out.println("[LOG] set new game");
            gameService.setupNewGame(null, null);
            gameService.startNewGameId();

            gameService.assignRoles(room.getPlayerList());
            gameService.newRound();
            String answerWord = gameService.getWordForDrawer();
            gameService.setAnswer(answerWord);
        }
        addCommonAttributes(model, me, roomId);
        if(me.getRole().equals(Role.DRAWER)){
            model.addAttribute("wordForDrawer", gameService.getAnswer());
            return "mainUI_Drawer";
        } else {
            return "mainUI_Guesser";
        }
    }


    @GetMapping("/guesser")
    public String showGuesser(Model model, HttpSession session) {
        // 게임 시작 전에 접근하면 홈으로 보내기
        String roomId = (String) session.getAttribute("roomId");
        String userId = session.getAttribute("userId").toString();
        if (roomId == null || userId == null) {
            return "redirect:/";
        }
        GameRoom room = gameRoomManager.getGameRoom(roomId);
        if (room == null) {
            return "redirect:/";
        }
        Player me = room.findPlayerByName(userId);
        if (me == null) {
            return "redirect:/";
        }
        addCommonAttributes(model, me, roomId);
        return "mainUI_Guesser";  // templates/mainUI_Guesser.html
    }
    @PostMapping("/answer")
    public String submitAnswer(@RequestParam String answer, Model model, @RequestParam String userId, HttpSession session) {

        String roomId = session.getAttribute("roomId").toString();

//        Player me = null;
//        if (p1 !=null && userId != null && userId.equals(p1.getName())) {
//            me = p1;
//        }
//        else if (p2 !=null && userId != null && userId.equals(p2.getName())) {
//            me = p2;
//        }
        if (roomId == null) {
            return "redirect:/";
        }
        GameRoom room = gameRoomManager.getGameRoom(roomId);
        if (room == null) {
            return "redirect:/";
        }
        Player me = room.findPlayerByName(userId);
        if (me == null) {
            return "redirect:/";
        }

        boolean correct = gameService.checkAnswer(answer);
        boolean roundOver = gameService.isRoundOver(correct);

        try {
            int currentRoundForMsg = gameService.getCurrentRound();
            if(roundOver) {
                currentRoundForMsg -= 1;
            }
            gameSocketHandler.sendGuessResult(roomId, correct, gameService.getTriesLeft(), gameService.getScore(),gameService.getCurrentRoundScore(), currentRoundForMsg);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!roundOver) {
            // 라운드 안 끝났으면 다시 Guesser 화면
            addCommonAttributes(model, me, roomId);
            model.addAttribute("lastResult", correct);
            if (me.getRole().equals(Role.GUESSER)) {
                return "mainUI_Guesser";
            } else  {
                return "mainUI_Drawer";
            }
        }
//        if (!gameService.isGameOver()) {
//            gameService.changeRoles(p1, p2);
//        }


        boolean roundSuccess = gameService.getCurrentRoundScore() > 0;

        model.addAttribute("round", gameService.getCurrentRound() - 1); // 방금 끝난 라운드
        model.addAttribute("roundSuccess", roundSuccess);
        model.addAttribute("roundScore", gameService.getCurrentRoundScore());
        model.addAttribute("totalScore", gameService.getScore());
        model.addAttribute("answerWord", gameService.getAnswer());
        model.addAttribute("myName", me.getName());

        try {
            gameSocketHandler.sendRoundEnd(roomId, gameService.getCurrentRound()-1, gameService.getAnswer(), correct);
        } catch (Exception e) {
            e.printStackTrace();
        }
//        Player other = (me == p1 ? p2 : p1);
//        if(other != null) {
//            model.addAttribute("otherName", other.getName());
//        }
////        try {
//            GameMessage end = new GameMessage();
//            end.setType("ROUND_END");
//            end.setRound(gameService.getCurrentRound()-1);
//            end.setAnswer(gameService.getAnswer());
//            end.setRoundSuccess(correct);
//
//            String json = new ObjectMapper().writeValueAsString(end);
//            gameSocketHandler.broadcast(json);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        try {
//            gameSocketHandler.sendRoundEnd(roomId, gameService.getCurrentRound()-1, gameService.getAnswer(), correct);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
        // 게임이 끝났으면 finalResult로 보내도 됨
        if (gameService.isGameOver()) {
            System.out.println("[LOG] game over! saving game history");
            for (Player p : room.getPlayerList()) {
                gameService.saveGameHistory(p);
                gameService.saveGameData(p);
            }
            return "finalResult";
        }

        return "midResult";    // templates/midResult.html
    }

    //  라운드가 타임아웃 / 기회 소진 등으로 끝났을 때 midResult 보여주는 GET
    @GetMapping("/answer")
    public String showMidResult(Model model) {

        boolean roundSuccess = gameService.getCurrentRoundScore() > 0;

        model.addAttribute("round", gameService.getCurrentRound() - 1); // 방금 끝난 라운드
        model.addAttribute("roundSuccess", roundSuccess);
        model.addAttribute("roundScore", gameService.getCurrentRoundScore());
        model.addAttribute("totalScore", gameService.getScore());
        model.addAttribute("answerWord", gameService.getAnswer());

        if (gameService.isGameOver()) {
            return "finalResult";
        }

        return "midResult";
    }

    @PostMapping("/next-round")
    public String nextRound(HttpSession session, Model model) {

        String userId = session.getAttribute("userId").toString();
        String roomId = session.getAttribute("roomId").toString();

        if (roomId == null || userId == null) {
            return "redirect:/";
        }
        GameRoom room = gameRoomManager.getGameRoom(roomId);
        if (room == null) {
            return "redirect:/";
        }
        // 1) 게임이 이미 끝났으면 저장 후 최종 결과
        if (gameService.isGameOver()) {
//            gameService.saveGameHistory(p1);
//            gameService.saveGameHistory(p2);
//            gameService.saveGameData(p1);
//            gameService.saveGameData(p2);
//            model.addAttribute("totalScore", gameService.getScore());
            return "finalResult";
        }

        // 2) 두 플레이어 역할 교대 (DRAWER ↔ GUESSER)
        //gameService.changeRoles(p1, p2);

        // 3) 새 라운드 세팅 (tries 초기화, 타이머 리셋)
//        gameService.newRound();
//        List<Player> players = room.getPlayerList();
        //gameService.prepareNextRound(p1, p2);
        //String userId = session.getAttribute("userId").toString();
//        Player me = null;
//        if (p1 != null && userId != null && userId.equals(p1.getName())) {
//            me = p1;
//        }  else if (p2 != null && userId != null && userId.equals(p2.getName())) {
//            me = p2;
//        }
        List<Player> players = room.getPlayerList();
        gameService.prepareNextRound(players);
        Player me = room.findPlayerByName(userId);
        if (me == null) {
            return "redirect:/";
        }
//        if (userId != null) {
//            if(p1 != null && p1.getName().equals(userId)) {
//                me = p1;
//            }
//            else if(p2 != null && p2.getName().equals(userId)) {
//                me = p2;
//            }
//        }
        // 5) 이 요청을 보낸 세션의 userId

//        if (p1 != null && userId != null && userId.equals(p1.getName())) {
//            me = p1;
//        } else if (p2 != null && userId != null && userId.equals(p2.getName())) {
//            me = p2;
//        }
//        if (me == null) {
//            return "redirect:/";
//        }

//
//        if (me == null) {
//            // 비정상 접근이면 그냥 홈으로
//            return "redirect:/";
//        }

        addCommonAttributes(model, me, roomId);

        String viewName;
        // 7) 내 역할에 따라 Drawer / Guesser 화면 분기
        if (me.getRole() == Role.DRAWER) {
            // Drawer만 이번 라운드 제시어를 받음
//            String answerWord = gameService.getWordForDrawer();
//            gameService.setAnswer(answerWord);
            model.addAttribute("wordForDrawer", gameService.getAnswer());
            viewName = "mainUI_Drawer";
        } else {
            // Guesser는 제시어 없이 추측 화면
            viewName = "mainUI_Guesser";
        }
        try {
            GameMessage start = new GameMessage();
            start.setType("ROUND_START");
            start.setRound(gameService.getCurrentRound());
            start.setDrawerName(gameService.getDrawerName(room.getPlayerList()));
            start.setGuesserName(gameService.getGuesserName(room.getPlayerList()));
            start.setRoomId(roomId);
            gameSocketHandler.sendRoundStart(roomId, start);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return viewName;
    }




    @PostMapping("/mypage")
    public String showMyPage(@RequestParam String userId, Model model) {
        Player p = gameService.loadPlayer(userId);
        model.addAttribute("player", p);
        model.addAttribute("totalScore", p.getTotalScore());
        model.addAttribute("gamesPlayed", p.getGamesPlayed());
        model.addAttribute("histories", p.getGameHistory());
        return "myPage";
    }

}
