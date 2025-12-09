package kr.ac.ewha.catchMind.service;

import kr.ac.ewha.catchMind.model.GameHistory;
import kr.ac.ewha.catchMind.model.GameRoom;
import kr.ac.ewha.catchMind.model.GameState;
import kr.ac.ewha.catchMind.model.Player;
import kr.ac.ewha.catchMind.repository.GameHistoryRepository;
import kr.ac.ewha.catchMind.repository.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GameSaveService {
    private GameHistoryRepository gameHistoryRepository;
    private PlayerRepository playerRepository;

    public GameSaveService(GameHistoryRepository gameHistoryRepository, PlayerRepository playerRepository) {
        this.gameHistoryRepository = gameHistoryRepository;
        this.playerRepository = playerRepository;
    }

    @Transactional
    public void saveGameData(Player p, GameRoom room) {
        GameState gameState = room.getGameState();
        p.setTotalScore(p.getTotalScore() + gameState.getTotalScore());
        p.setGamesPlayed(p.getGamesPlayed() + 1);
        playerRepository.save(p);
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

}
