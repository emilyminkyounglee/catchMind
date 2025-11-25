package kr.ac.ewha.catchMind.service;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

import kr.ac.ewha.catchMind.model.Player;
import kr.ac.ewha.catchMind.model.Role;

@Service
public class GameService {
    //Controller에서 게임 새로 시작할때 resetGame 부르고 시작
    private int tries = 5;
    private int rounds = 1;
    private long time;
    private String answer;
    private int score = 0;
    private int roundScore = 0;
    private static final long ROUND_LIMIT_MS = 90_000;
    public boolean isGameOver() // 현재 게임이 종료되었는지 확인 aka 6라운드까지 진행 완료 했는지
    {
        boolean isOver = false;
        if (rounds > 6)
        {
            isOver = true;
        }
        return isOver;
    }
    public boolean isRoundOver(boolean correct)//현재 라운드가 종료되었는지 확인 + 여기서 점수계산 로직
    {
        boolean isOver = false;

        if (correct) {
            roundScore = tries;
            score += tries;
            tries = 5;
            rounds++;
            isOver = true;
        }
        else if (tries < 1 || isTimeOver()) {
            roundScore = 0;
            tries = 5;
            rounds++;
            isOver = true;
        }
        return isOver;
    }
    public boolean isTimeOver() { // 시간 체크 (1분 30초짜리 게임으로
        long now = System.currentTimeMillis();
        return now - time >= ROUND_LIMIT_MS;
    }
    public int minuteLeft() { // 남은 분
        long now = System.currentTimeMillis();
        long remain = ROUND_LIMIT_MS - (now - time);

        if (remain < 0) remain = 0;

        return (int)(remain / 1000) / 60;
    }

    public int secondsLeft() { // 남은 초
        long now = System.currentTimeMillis();
        long remain = ROUND_LIMIT_MS - (now - time);

        if (remain < 0) remain = 0;

        return (int)(remain / 1000) % 60;
    }
    public boolean checkAnswer(String guess)//정답 체크
    {
        if (answer != null && guess.equals(answer))
        {
            return true;
        }
        else {
            tries--;
            return false;
        }
    }
    public int getTriesLeft() // 정답 시도 기회 알려줌
    {
        return tries;
    }
    public int getCurrentRound() // 현재 라운드 몇번째인지 알려줌
    {
        return rounds;
    }
    public void setupNewGame(List<String> wordList, Player p1, Player p2)//게임을 총체적으로 다시 실행해볼때
    {
        tries = 5;
        rounds = 1;
        score = 0;
    }
    public void newRound() // 새 라운드 시작할때 (얘도 무조건 처음에 부르기)
    {
        tries = 5;
        time = System.currentTimeMillis();
    }
    public void setPlayerInfo(Player player, int i, String name)
    {
        //view에서 받은 플레이어 정보 (이름)
        player.setName(name);
        if  (i%2 == 0)
        {
            player.setRole(Role.DRAWER);
        }
        else
        {
            player.setRole(Role.GUESSER);
        }
    }
    public void changeRoles(Player p1, Player p2) //역할 바꿔주기
    {
        Role p2Role = p1.getRole();
        Role p1Role = p2.getRole();
        p1.setRole(p1Role);
        p2.setRole(p2Role);
    }
    public int getScore()
    {
        return score;
    }
    public void setAnswer(String answer)
    {
        this.answer = answer;
    }
    public int getCurrentRoundScore()
    {
        return this.roundScore;
    }
    public String getWordForDrawer() { // 정답 알려주는 애
        return this.answer;
    }
    public String getDrawerName(Player p1, Player p2) {
        if (p1 != null && p1.getRole() == Role.DRAWER) {
            return p1.getName();
        }
        if (p2 != null && p2.getRole() == Role.DRAWER) {
            return p2.getName();
        }
        return "Drawer"; // 기본값
    }

    public String getGuesserName(Player p1, Player p2) {
        if (p1 != null && p1.getRole() == Role.GUESSER) {
            return p1.getName();
        }
        if (p2 != null && p2.getRole() == Role.GUESSER) {
            return p2.getName();
        }
        return "Guesser"; // 기본값
    }


}
