package kr.ac.ewha.catchMind.service;

import java.util.ArrayList;
import java.util.List;

import kr.ac.ewha.catchMind.model.GameHistory;
import kr.ac.ewha.catchMind.model.WordDictionary;
import kr.ac.ewha.catchMind.repository.GameHistoryRepository;
import kr.ac.ewha.catchMind.repository.PlayerRepository;
import kr.ac.ewha.catchMind.repository.WordDictionaryRepository;
import org.springframework.stereotype.Service;

import kr.ac.ewha.catchMind.model.Player;
import kr.ac.ewha.catchMind.model.Role;
import org.springframework.transaction.annotation.Transactional;

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
    private final PlayerRepository playerRepository;
    private final WordDictionaryRepository wordDictionaryRepository;
    private final GameHistoryRepository gameHistoryRepository;



    public GameService(PlayerRepository playerRepository, WordDictionaryRepository wordDictionaryRepository, GameHistoryRepository gameHistoryRepository) {
        this.playerRepository = playerRepository;
        this.wordDictionaryRepository = wordDictionaryRepository;
        this.gameHistoryRepository = gameHistoryRepository;
    }

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
    public void setupNewGame(Player p1, Player p2)//게임을 총체적으로 다시 실행해볼때
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
    public String getWordForDrawer() {
        WordDictionary wordDictionary = wordDictionaryRepository.getRandom();
        if ( wordDictionary == null)
        {
            return "고양이";
        }
        return wordDictionary.getWord();
    }
    public String getDrawerName(Player p1, Player p2) {
        if (p1 != null && p1.getRole().equals(Role.DRAWER)) {
            return p1.getName();
        }
        if (p2 != null && p2.getRole().equals(Role.DRAWER)) {
            return p2.getName();
        }
        return "Drawer"; // 기본값
    }

    public String getGuesserName(Player p1, Player p2) {
        if (p1 != null && p1.getRole().equals(Role.GUESSER)) {
            return p1.getName();
        }
        if (p2 != null && p2.getRole().equals(Role.GUESSER)) {
            return p2.getName();
        }
        return "Guesser"; // 기본값
    }

    @Transactional
    public void saveGameData(Player p) {
        p.setTotalScore(p.getTotalScore() + this.score);
        p.setGamesPlayed(p.getGamesPlayed() + 1);
        playerRepository.save(p);
    }

    @Transactional
    public Player loadPlayer(String name) {
        Player p = playerRepository.findByName(name);
        if (p == null) {
            p = new Player();
            p.setName(name);
            p.setTotalScore(0);
            playerRepository.save(p);
        }
        return p;
    }
    public String giveHint(){
        String hint = "";
        if (getTriesLeft()<3)
        {
            return hint;
        }
        else
        {
            return hint;
        }
    }

    // 새로 추가 메서드 - 조서연
    public String getAnswer() {
        // TODO Auto-generated method stub
        return this.answer;
    }

    public void saveGameHistory(Player p, char[] roundResult, int[] roundScore, int totalScore) {
        GameHistory history = new GameHistory();
        history.setPlayer(p);
        history.setRound1Result(roundResult[0]);
        history.setRound1Score(roundScore[0]);
        history.setRound2Result(roundResult[1]);
        history.setRound2Score(roundScore[1]);
        history.setRound3Result(roundResult[2]);
        history.setRound3Score(roundScore[2]);
        history.setRound4Result(roundResult[3]);
        history.setRound4Score(roundScore[3]);
        history.setRound5Result(roundResult[4]);
        history.setRound5Score(roundScore[4]);
        history.setRound6Result(roundResult[5]);
        history.setRound6Score(roundScore[5]);

        history.setTotalScore(totalScore);
        p.addGameHistory(history);
        gameHistoryRepository.save(history);
    }
}
