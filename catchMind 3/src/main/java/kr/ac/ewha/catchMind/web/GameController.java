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

    private final Player p1 = new Player();
    private final Player p2 = new Player();

    private final List<String> wordList = Arrays.asList("사과", "바나나", "딸기", "배", "고양이", "강아지");
    public GameController(GameService gameService) {
        this.gameService = gameService;
    }
    @PostMapping("/start")
    public String startGame(@RequestParam String userId, Model model) {

        // 간단히 p1 = DRAWER, p2 = GUESSER로 고정 (임시)
        p1.setName(userId);
        p1.setRole(Role.DRAWER);

        p2.setName("상대방");
        p2.setRole(Role.GUESSER);

        // 게임 전체 초기화
        gameService.setupNewGame(wordList, p1, p2);

        // 새 라운드 시작
        gameService.newRound();

        // 이번 라운드 제시어 뽑기 & 정답 세팅
        String answerWord = pickRandomWord();
        gameService.setAnswer(answerWord);

        // drawer 화면에 넘길 정보들
        model.addAttribute("round", gameService.getCurrentRound());
        model.addAttribute("drawerName", gameService.getDrawerName(p1, p2));
        model.addAttribute("guesserName", gameService.getGuesserName(p1, p2));
        model.addAttribute("wordForDrawer", gameService.getWordForDrawer());

        return "mainUI_Drawer";   // templates/mainUI_Drawer.html
    }
    @GetMapping("/guesser")
    public String showGuesser(Model model) {

        model.addAttribute("round", gameService.getCurrentRound());
        model.addAttribute("drawerName", gameService.getDrawerName(p1, p2));
        model.addAttribute("guesserName", gameService.getGuesserName(p1, p2));
        model.addAttribute("triesLeft", gameService.getTriesLeft());

        return "mainUI_Guesser";  // templates/mainUI_Guesser.html
    }
    @PostMapping("/answer")
    public String submitAnswer(@RequestParam String answer, Model model) {

        boolean correct = gameService.checkAnswer(answer);

        // 라운드가 끝났는지 확인 → 점수 계산도 여기서 이미 됨
        boolean roundOver = gameService.isRoundOver(correct);

        if (!roundOver) {
            // 라운드 안 끝났으면 다시 Guesser 화면으로
            model.addAttribute("round", gameService.getCurrentRound());
            model.addAttribute("drawerName", gameService.getDrawerName(p1, p2));
            model.addAttribute("guesserName", gameService.getGuesserName(p1, p2));
            model.addAttribute("triesLeft", gameService.getTriesLeft());
            model.addAttribute("lastResult", correct);

            return "mainUI_Guesser";
        }

        // 라운드가 끝났다면 → midResult 화면
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
            // 6라운드 다 끝났으면 최종 결과로
            model.addAttribute("totalScore", gameService.getScore());
            return "finalResult";
        }

        // 역할 바꾸기
        gameService.changeRoles(p1, p2);

        // 새 라운드 시작
        gameService.newRound();

        String answerWord = pickRandomWord();
        gameService.setAnswer(answerWord);

        model.addAttribute("round", gameService.getCurrentRound());
        model.addAttribute("drawerName", gameService.getDrawerName(p1, p2));
        model.addAttribute("guesserName", gameService.getGuesserName(p1, p2));
        model.addAttribute("wordForDrawer", gameService.getWordForDrawer());

        return "mainUI_Drawer";
    }

    private String pickRandomWord() {
        int idx = (int) (Math.random() * wordList.size());
        return wordList.get(idx);
    }

}
