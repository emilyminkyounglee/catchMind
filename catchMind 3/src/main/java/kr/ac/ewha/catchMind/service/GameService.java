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
    private final PlayerRepository playerRepository;
    private final WordDictionaryRepository wordDictionaryRepository;

    public GameService(PlayerRepository playerRepository, WordDictionaryRepository wordDictionaryRepository) {
        this.playerRepository = playerRepository;
        this.wordDictionaryRepository = wordDictionaryRepository;
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

    public void prepareNextRound(GameRoom room)
    {
        GameState gameState = room.getGameState();

        if (!gameState.isNeedInitNextRound() || gameState.isGameOver()) {
            return;
        }

        List<Player> players = room.getPlayerList();
        if (players == null || players.isEmpty())
        {
            return;
        }

        gameState.startNewRound();

        int currentRound = gameState.getRound();
        assignRolesForRound(players, currentRound);

        String answerWord = getWordForDrawer();
        gameState.assignAnswer(answerWord);

        gameState.clearNeedInitNextRound();
    }

    private void assignRolesForRound(List<Player> players, int round) {
        if (players == null || players.isEmpty()) {
            return;
        }

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

    public void assignRolesForNewGame(GameRoom room) {
        GameState gameState = room.getGameState();
        List<Player> players = room.getPlayerList();
        assignRolesForRound(players, gameState.getRound());
    }
}
