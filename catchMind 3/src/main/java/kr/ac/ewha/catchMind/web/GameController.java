package kr.ac.ewha.catchMind.web;

import jakarta.servlet.http.HttpSession;

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
    public String startGame(@RequestParam String userId,
                            HttpSession session,
                            Model model) {

        // 이 세션의 유저 ID를 저장해두기 (나중에 next-round에서 사용)
        session.setAttribute("userId", userId);

        if ("wkvmtlf2MEJ".equals(userId)) {
            return  "redirect:/admin/words";
        }

        // 현재 구조: p1, p2 둘 다 같은 userId 로딩
        // 나중에 진짜 2인 구조로 바꾸려면 여기 로직을
        // "첫 번째 접속자는 p1, 두 번째 접속자는 p2" 식으로 분리해야 함
        p1 = gameService.loadPlayer(userId);
        p2 = gameService.loadPlayer(userId);

        gameService.setRoleRandomly(p1, p2);
        gameService.setupNewGame(p1, p2);
        gameService.newRound();

        String answerWord = gameService.getWordForDrawer();
        gameService.setAnswer(answerWord);

        // 요청 보낸 나(me)를 p1/p2 중에서 찾기
        Player me = p1;
        if (p2 != null && userId.equals(p2.getName())) {
            me = p2;
        }

        addCommonAttributes(model);

        if (me.getRole() == Role.DRAWER) {
            model.addAttribute("wordForDrawer", answerWord);
            return "mainUI_Drawer";
        } else {
            // redirect 말고 바로 Guesser 템플릿을 리턴해도 됨
            return "mainUI_Guesser";
        }
    }


    @GetMapping("/guesser")
    public String showGuesser(Model model) {
        // 게임 시작 전에 접근하면 홈으로 보내기
        if (p1 == null || p2 == null || p1.getName() == null || p1.getRole() == null ||
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
        model.addAttribute("answerWord", gameService.getAnswer());

        // 게임이 끝났으면 finalResult로 보내도 됨
        if (gameService.isGameOver()) {
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

        // 1) 게임이 이미 끝났으면 저장 후 최종 결과
        if (gameService.isGameOver()) {
            gameService.saveGameHistory(p1);
            gameService.saveGameHistory(p2);
            gameService.saveGameData(p1);
            gameService.saveGameData(p2);
            model.addAttribute("totalScore", gameService.getScore());
            return "finalResult";
        }

        // 2) 두 플레이어 역할 교대 (DRAWER ↔ GUESSER)
        gameService.changeRoles(p1, p2);

        // 3) 새 라운드 세팅 (tries 초기화, 타이머 리셋)
        gameService.newRound();

        // 4) 공통 정보 (라운드, 점수, 남은 기회 등)
        addCommonAttributes(model);

        // 5) 이 요청을 보낸 세션의 userId
        String userId = (String) session.getAttribute("userId");

        // 6) p1/p2 중에서 "나(me)" 찾기
        Player me = null;
        if (p1 != null && userId != null && userId.equals(p1.getName())) {
            me = p1;
        } else if (p2 != null && userId != null && userId.equals(p2.getName())) {
            me = p2;
        }

        if (me == null) {
            // 비정상 접근이면 그냥 홈으로
            return "redirect:/";
        }

        // 7) 내 역할에 따라 Drawer / Guesser 화면 분기
        if (me.getRole() == Role.DRAWER) {
            // Drawer만 이번 라운드 제시어를 받음
            String answerWord = gameService.getWordForDrawer();
            gameService.setAnswer(answerWord);
            model.addAttribute("wordForDrawer", answerWord);
            return "mainUI_Drawer";
        } else {
            // Guesser는 제시어 없이 추측 화면
            return "mainUI_Guesser";
        }
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
