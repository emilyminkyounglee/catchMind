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

    public GameController(GameService gameService, GameSocketHandler gameSocketHandler, GameRoomManager gameRoomManager) {
        this.gameService = gameService;
        this.gameSocketHandler = gameSocketHandler;
        this.gameRoomManager = gameRoomManager;
    }

    //공통으로 Model에 등록시킬 값들을 메서드로 처리
    private void addCommonAttributes(Model model, GameRoom room, Player me) {
        GameState gameState = new GameState();
        model.addAttribute("round", gameState.getRound());
        model.addAttribute("triesLeft", gameState.getTries());
        model.addAttribute("totalScore", gameState.getTotalScore());
        model.addAttribute("roomId", room.getRoomId());

        model.addAttribute("myName", me.getName());
        model.addAttribute("myRole", me.getRole());
    }

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

        if (gameService.isGameOver(room)) {
            System.out.println("[LOG] set new game");
            gameService.setupNewGame(room);
            gameService.startNewGameId(room);

            gameService.assignRoles(room.getPlayerList());
            room.getGameState().startNewRound();
            String answerWord = gameService.getWordForDrawer();
            gameService.setAnswer(room, answerWord);
        }
        addCommonAttributes(model, room, me);
        if(me.getRole().equals(Role.DRAWER)){
            model.addAttribute("wordForDrawer", gameService.getAnswer(room));
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
        addCommonAttributes(model, room, me);
        return "mainUI_Guesser";  // templates/mainUI_Guesser.html
    }
    @PostMapping("/answer")
    public String submitAnswer(@RequestParam String answer, Model model, @RequestParam String userId, HttpSession session) {

        String roomId = session.getAttribute("roomId").toString();

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

        boolean correct = gameService.checkAnswer(room, answer);
        boolean roundOver = gameService.isRoundOver(room, correct);

        try {
            int currentRoundForMsg = gameService.getCurrentRound(room);
            if(roundOver) {
                currentRoundForMsg -= 1;
            }
            gameSocketHandler.sendGuessResult(roomId, correct, gameService.getTriesLeft(room), gameService.getScore(room), room.getGameState().getRoundScore(), currentRoundForMsg);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!roundOver) {
            // 라운드 안 끝났으면 다시 Guesser 화면
            addCommonAttributes(model, room, me);
            model.addAttribute("lastResult", correct);
            if (me.getRole().equals(Role.GUESSER)) {
                return "mainUI_Guesser";
            } else  {
                return "mainUI_Drawer";
            }
        }

        GameState gameState = room.getGameState();
        boolean roundSuccess = gameState.getRoundScore() > 0;

        model.addAttribute("round", gameState.getRound() - 1); // 방금 끝난 라운드
        model.addAttribute("roundSuccess", roundSuccess);
        model.addAttribute("roundScore", gameState.getRoundScore());
        model.addAttribute("totalScore", gameState.getTotalScore());
        model.addAttribute("answerWord", gameState.getAnswer());
        model.addAttribute("myName", me.getName());

        try {
            gameSocketHandler.sendRoundEnd(roomId, gameState.getRound()-1, gameState.getAnswer(), correct);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 게임이 끝났으면 finalResult로 보내도 됨
        if (gameService.isGameOver(room)) {
            System.out.println("[LOG] game over! saving game history");
            for (Player p : room.getPlayerList()) {
                gameService.saveGameHistory(p, room);
                gameService.saveGameData(p, room);
            }
            return "finalResult";
        }

        return "midResult";    // templates/midResult.html
    }

    //  라운드가 타임아웃 / 기회 소진 등으로 끝났을 때 midResult 보여주는 GET
    @GetMapping("/answer")
    public String showMidResult(Model model, HttpSession session) {

        String roomId = session.getAttribute("roomId").toString();
        if (roomId == null) {
            return "redirect:/";
        }
        GameRoom room = gameRoomManager.getGameRoom(roomId);
        if (room == null) {
            return "redirect:/";
        }
        GameState gameState = room.getGameState();
        boolean roundSuccess = gameState.getRoundScore() > 0;

        model.addAttribute("round", gameState.getRound() - 1); // 방금 끝난 라운드
        model.addAttribute("roundSuccess", roundSuccess);
        model.addAttribute("roundScore", gameState.getRoundScore());
        model.addAttribute("totalScore", gameState.getTotalScore());
        model.addAttribute("answerWord", gameState.getAnswer());

        if (gameService.isGameOver(room)) {
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
        if (gameService.isGameOver(room)) {
            return "finalResult";
        }


        gameService.prepareNextRound(room);
        Player me = room.findPlayerByName(userId);
        if (me == null) {
            return "redirect:/";
        }

        addCommonAttributes(model, room, me);

        String viewName;
        // 7) 내 역할에 따라 Drawer / Guesser 화면 분기
        if (me.getRole() == Role.DRAWER) {
            model.addAttribute("wordForDrawer", gameService.getAnswer(room));
            viewName = "mainUI_Drawer";
        } else {
            // Guesser는 제시어 없이 추측 화면
            viewName = "mainUI_Guesser";
        }
        try {
            GameMessage start = new GameMessage();
            start.setType("ROUND_START");
            start.setRound(gameService.getCurrentRound(room));
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
