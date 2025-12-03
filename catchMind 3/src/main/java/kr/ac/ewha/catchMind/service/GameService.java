package kr.ac.ewha.catchMind.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import kr.ac.ewha.catchMind.model.*;
import kr.ac.ewha.catchMind.repository.GameHistoryRepository;
import kr.ac.ewha.catchMind.repository.PlayerRepository;
import kr.ac.ewha.catchMind.repository.WordDictionaryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

@Service
public class GameService {
    private static final int MAX_ROUNDS = 6;
    //Controller에서 게임 새로 시작할때 resetGame 부르고 시작
    private final PlayerRepository playerRepository;
    private final WordDictionaryRepository wordDictionaryRepository;
    private final GameHistoryRepository gameHistoryRepository;

    public GameService(PlayerRepository playerRepository, WordDictionaryRepository wordDictionaryRepository, GameHistoryRepository gameHistoryRepository) {
        this.playerRepository = playerRepository;
        this.wordDictionaryRepository = wordDictionaryRepository;
        this.gameHistoryRepository = gameHistoryRepository;
    }

    public boolean isGameOver(GameRoom room) // 현재 게임이 종료되었는지 확인 aka 6라운드까지 진행 완료 했는지
    {
        return room.getGameState().isGameOver();
    }

    public boolean checkAnswer(GameRoom room, String guess)//정답 체크
    {
        GameState gameState = room.getGameState();
        String answer = gameState.getAnswer();
        if (answer != null && answer.equals(guess))
        {
            return true;
        }
        else {
            gameState.decreaseTries();
            return false;
        }
    }

    public boolean isRoundOver(GameRoom room, boolean correct)
    {
        return room.getGameState().checkRoundOver(correct);
    }
    public int getTriesLeft(GameRoom room)
    {
        return room.getGameState().getTries();
    }
    public int getCurrentRound(GameRoom room)
    {
        return room.getGameState().getRound();
    }
    public int getScore(GameRoom room)
    {
        return room.getGameState().getTotalScore();
    }
    public void setupNewGame(GameRoom room)
    {
        room.getGameState().resetState();
    }
    public void setAnswer(GameRoom room, String answer)
    {
        room.getGameState().assignAnswer(answer);
    }
    public String getAnswer(GameRoom room)
    {
        return room.getGameState().getAnswer();
    }
    public void startNewGameId(GameRoom room) {
        room.getGameState().initNewGameId();
    }

    public String getGameId(GameRoom room) {
        return room.getGameState().getGameId();
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
    public void saveGameData(Player p, GameRoom room) {
        GameState gameState = room.getGameState();
        p.setTotalScore(p.getTotalScore() + gameState.getTotalScore());
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

    @Transactional
    public void saveGameHistory(Player p,
                                char[] roundResult,
                                int[] roundScore,
                                int totalScore,
                                String gameId) {
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
        gameHistoryRepository.save(history);
    }
    @Transactional
    public void saveGameHistory(Player p, GameRoom room) {
        GameState state = room.getGameState();
        saveGameHistory(
                p,
                state.getRoundResult(),
                state.getRoundScores(),
                state.getTotalScore(),
                state.getGameId()
        );
    }

    public void prepareNextRound(GameRoom room)
    {
        GameState gameState = room.getGameState();

        // 다음 라운드 초기화가 필요한 상태가 아니거나, 이미 게임이 끝났으면 그냥 리턴
        if (!gameState.isNeedInitNextRound() || gameState.isGameOver()) {
            return;
        }

        List<Player> players = room.getPlayerList();
        if (players == null || players.isEmpty())
        {
            return;
        }

        // 1) 라운드 수 올리기 (1,2,3,...)
        gameState.startNewRound();

        // 2) 이번 라운드 번호 기준으로 Drawer를 순환 배정
        int currentRound = gameState.getRound();
        assignRolesForRound(players, currentRound);

        // 3) 새 문제 세팅
        String answerWord = getWordForDrawer();
        gameState.assignAnswer(answerWord);

        // 4) 다음 라운드 초기화 플래그 내려주기
        gameState.clearNeedInitNextRound();
    }
//
//    public void assignRoles(List<Player> players) {
//        if (players == null || players.size() == 0) {
//            return;
//        }
//        int drawerIndex = (int)(Math.random() * players.size());
//        for(int i = 0; i < players.size(); i++) {
//            Player player = players.get(i);
//            if (i == drawerIndex) {
//                player.setRole(Role.DRAWER);
//            } else  {
//                player.setRole(Role.GUESSER);
//            }
//        }
//    }

    //3명 이상일때 역할 분배를 위해 추가한 메서드
    // round 번호(1,2,3,...)에 따라 Drawer를 순환시키는 공통 메서드
    private void assignRolesForRound(List<Player> players, int round) {
        if (players == null || players.isEmpty()) {
            return;
        }

        // 0-based 인덱스로 변환
        int drawerIndex = (round - 1) % players.size();

        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            if (i == drawerIndex) {
                player.setRole(Role.DRAWER);
            } else {
                player.setRole(Role.GUESSER);
            }
        }
    }
    
    //추가 메서드) 새 게임 시작 시에 쓸 메서드 추가
    // 새 게임 시작 시, 현재 GameState의 round 값에 맞춰 역할 배정
    public void assignRolesForNewGame(GameRoom room) {
        GameState gameState = room.getGameState();
        List<Player> players = room.getPlayerList();
        assignRolesForRound(players, gameState.getRound());
    }



}
