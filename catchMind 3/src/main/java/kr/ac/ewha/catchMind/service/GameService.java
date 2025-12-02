package kr.ac.ewha.catchMind.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import kr.ac.ewha.catchMind.model.GameHistory;
import kr.ac.ewha.catchMind.model.WordDictionary;
import kr.ac.ewha.catchMind.repository.GameHistoryRepository;
import kr.ac.ewha.catchMind.repository.PlayerRepository;
import kr.ac.ewha.catchMind.repository.WordDictionaryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import kr.ac.ewha.catchMind.model.Player;
import kr.ac.ewha.catchMind.model.Role;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GameService {
    private static final int MAX_ROUNDS = 6;
    //Controller에서 게임 새로 시작할때 resetGame 부르고 시작
    private int tries = 5;
    private int rounds = 1;
    private long time;
    private String answer;
    private int score = 0;
    private int roundScore = 0;
    private static final long ROUND_LIMIT_MS = 90_000;
    private final char[] roundResult = new char[MAX_ROUNDS];
    private final int[] roundScores = new int[MAX_ROUNDS];
    private final PlayerRepository playerRepository;
    private final WordDictionaryRepository wordDictionaryRepository;
    private final GameHistoryRepository gameHistoryRepository;

    private String gameId;

    private boolean needInitNextRound = false;




    public GameService(PlayerRepository playerRepository, WordDictionaryRepository wordDictionaryRepository, GameHistoryRepository gameHistoryRepository) {
        this.playerRepository = playerRepository;
        this.wordDictionaryRepository = wordDictionaryRepository;
        this.gameHistoryRepository = gameHistoryRepository;
    }

    public boolean isGameOver() // 현재 게임이 종료되었는지 확인 aka 6라운드까지 진행 완료 했는지
    {
        return rounds > MAX_ROUNDS;
    }
    public boolean isRoundOver(boolean correct)//현재 라운드가 종료되었는지 확인 + 여기서 점수계산 로직
    {
        boolean isOver = false;
        int idx = rounds - 1;

        if (correct) {
            roundScore = tries;
            score += tries;
            roundResult[idx] = 'O';
            roundScores[idx] = roundScore;
            tries = 5;
            rounds++;
            isOver = true;
        }
        else if (tries < 1 || isTimeOver()) {
            roundScore = 0;
            roundResult[idx] = 'X';
            roundScores[idx] = 0;
            tries = 5;
            rounds++;
            isOver = true;
        }
        if (isOver) {
            needInitNextRound = true;
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
        roundScore = 0;
        Arrays.fill(roundResult, '-');
        Arrays.fill(roundScores, 0);
        needInitNextRound = false;
    }
    public void newRound() // 새 라운드 시작할때 (얘도 무조건 처음에 부르기)
    {
        tries = 5;
        time = System.currentTimeMillis();
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

    public String getDrawerName(List<Player> players) {
        Player drawer = findDrawer(players);
        return drawer != null ? drawer.getName() : "Drawer";
    }

    public String getGuesserName(List<Player> players) {
        Player guesser = findGuesser(players);
        return guesser != null ? guesser.getName() : "Guesser";
    }

    public Player findDrawer(List<Player> players) {
        if (players == null) return null;
        for (Player p : players) {
            if (p.getRole() == Role.DRAWER) return p;
        }
        return null;
    }

    public Player findGuesser(List<Player> players) {
        if (players == null) return null;
        for (Player p : players) {
            if (p.getRole() == Role.GUESSER) return p;
        }
        return null;
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

    @Transactional
    public void saveGameHistory(Player p) {
        System.out.println("[LOG] save game history 호출 " + p.getName() + ", total score: " + score );
        saveGameHistory(p, roundResult, roundScores, score, this.gameId);
    }

    @Transactional
    public void saveGameHistory(Player p, char[] roundResult, int[] roundScore, int totalScore, String gameId) {
        GameHistory history = new GameHistory();
        history.setPlayer(p);
        history.setGameId(gameId);

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
       // p.addGameHistory(history);
        gameHistoryRepository.save(history);
    }

    public void setRoleRandomly(Player p1, Player p2) {
        if (Math.random() < 0.5) {
            p1.setRole(Role.DRAWER);
            p2.setRole(Role.GUESSER);
        } else {
            p1.setRole(Role.GUESSER);
            p2.setRole(Role.DRAWER);
        }
    }

    public synchronized void prepareNextRound(List<Player> players)
    {
        if (!needInitNextRound || isGameOver()) {
            return;
        }
        if (players == null || players.isEmpty()) return;
        int currentDrawerIndex = -1;
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).getRole() == Role.DRAWER) {
                currentDrawerIndex = i;
                break;
            }
        }
        int nextDrawerIndex;
        if (currentDrawerIndex == -1){
            nextDrawerIndex = 0;
        } else {
            nextDrawerIndex = (currentDrawerIndex + 1)%players.size();
        }
        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            if (i == nextDrawerIndex) {
                p.setRole(Role.DRAWER);
            } else {
                p.setRole(Role.GUESSER);
            }
        }
        newRound();
        String answerWord = getWordForDrawer();
        setAnswer(answerWord);
        needInitNextRound = false;
    }
    public void startNewGameId() {
        this.gameId = java.util.UUID.randomUUID().toString();
    }
    public String getGameId() {
        return this.gameId;
    }

    public void assignRoles(List<Player> players) {
        if (players == null || players.size() == 0) {
            return;
        }
        int drawerIndex = (int)(Math.random() * players.size());
        for(int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            if (i == drawerIndex) {
                player.setRole(Role.DRAWER);
            } else  {
                player.setRole(Role.GUESSER);
            }
        }
    }

}
