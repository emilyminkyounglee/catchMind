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

        gameService.setRoleRandomly(p1, p2);
//        p1.setRole(Role.DRAWER);
//        p2.setRole(Role.GUESSER);

        gameService.setupNewGame(p1, p2);

        gameService.newRound();

        String answerWord = gameService.getWordForDrawer();
        gameService.setAnswer(answerWord);

        // 새로 수정한 부분!
        if (p1.getRole() == Role.DRAWER) {
            // 1. 내가 그리는 사람(DRAWER)일 때
            addCommonAttributes(model);
            model.addAttribute("wordForDrawer", gameService.getWordForDrawer());
            return "mainUI_Drawer"; // 그림판 화면

        } else {
            // 2. 내가 맞히는 사람(GUESSER)일 때
            // guesser 화면을 띄워주는 @GetMapping("/guesser") 경로로 토스!
            return "redirect:/game/guesser";
        }
        // 여기까지

//        // drawer 화면에 넘길 정보들
//        addCommonAttributes(model);
//        model.addAttribute("wordForDrawer", gameService.getWordForDrawer());
//
//        return "mainUI_Drawer";
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


        if (p1.getRole() == Role.DRAWER) {
            addCommonAttributes(model);
            model.addAttribute("wordForDrawer", gameService.getWordForDrawer());
            return "mainUI_Drawer";
        } else {
            return "redirect:/game/guesser";
        }

    }


}
