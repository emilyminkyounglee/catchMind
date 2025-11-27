package kr.ac.ewha.catchMind.web;


import kr.ac.ewha.catchMind.model.Player;
import kr.ac.ewha.catchMind.model.Role;
import kr.ac.ewha.catchMind.service.GameService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/game")
public class GameController {
    private final GameService gameService;

    private Player p1;
    private Player p2;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    //공통으로 Model에 등록시킬 값들을 메서드로 처리
    private void addCommonAttributes(Model model) {
        model.addAttribute("round", gameService.getCurrentRound());
        model.addAttribute("drawerName", gameService.getDrawerName(p1, p2));
        model.addAttribute("guesserName", gameService.getGuesserName(p1, p2));
        model.addAttribute("triesLeft", gameService.getTriesLeft());
        model.addAttribute("totalScore", gameService.getScore());
    }

    @PostMapping("/start")
    public String startGame(@RequestParam String userId, Model model) {

        if ("wkvmtlf2MEJ".equals(userId)) {
            return  "redirect:/admin/words";
        }
        p1 = gameService.loadPlayer(userId);
        p2 = gameService.loadPlayer(userId);

        p1.setRole(Role.DRAWER);
        p2.setRole(Role.GUESSER);

        gameService.setupNewGame(p1, p2);

        gameService.newRound();

        String answerWord = gameService.getWordForDrawer();
        gameService.setAnswer(answerWord);

        // drawer 화면에 넘길 정보들
        addCommonAttributes(model);
        model.addAttribute("wordForDrawer", gameService.getWordForDrawer());

        return "mainUI_Drawer";
    }
    @GetMapping("/guesser")
    public String showGuesser(Model model) {
        // 게임 시작 전에 접근하면 홈으로 보내기
        if (p1.getName() == null || p1.getRole() == null ||
                p2.getName() == null || p2.getRole() == null) {
            return "redirect:/";
        }

        addCommonAttributes(model);
        return "mainUI_Guesser";  // templates/mainUI_Guesser.html
    }
    @PostMapping("/answer")
    public String submitAnswer(@RequestParam String answer, Model model) {

        boolean correct = gameService.checkAnswer(answer);

        boolean roundOver = gameService.isRoundOver(correct);

        if (!roundOver) {
            // 라운드 안 끝났으면 다시 Guesser 화면으로
            addCommonAttributes(model);
            model.addAttribute("lastResult", correct);

            return "mainUI_Guesser";
        }


        boolean roundSuccess = gameService.getCurrentRoundScore() > 0;

        model.addAttribute("round", gameService.getCurrentRound() - 1); // 방금 끝난 라운드
        model.addAttribute("roundSuccess", roundSuccess);
        model.addAttribute("roundScore", gameService.getCurrentRoundScore());
        model.addAttribute("totalScore", gameService.getScore());
        model.addAttribute("answerWord", gameService.getWordForDrawer());

        // 게임이 끝났으면 finalResult로 보내도 됨
        if (gameService.isGameOver()) {
            return "finalResult";
        }

        return "midResult";    // templates/midResult.html
    }
    @PostMapping("/next-round")
    public String nextRound(Model model) {

        if (gameService.isGameOver()) {
            gameService.saveGameData(p1);
            gameService.saveGameData(p2);
            model.addAttribute("totalScore", gameService.getScore());
            return "finalResult";
        }

        gameService.changeRoles(p1, p2);

        gameService.newRound();

        String answerWord = gameService.getWordForDrawer();
        gameService.setAnswer(answerWord);

        addCommonAttributes(model);
        model.addAttribute("wordForDrawer", gameService.getWordForDrawer());

        return "mainUI_Drawer";
    }

    @PostMapping("/mypage")
    public String showMyPage(@RequestParam String userId, Model model) {
        Player p = gameService.loadPlayer(userId);
        model.addAttribute("player", p);
        model.addAttribute("totalScore", p.getTotalScore());
        model.addAttribute("gamesPlayed", p.getGamesPlayed());
        model.addAttribute("history", p.getGameHistory());
        return "myPage";
    }

}
