package kr.ac.ewha.catchMind.model;

import java.util.Arrays;
import java.util.UUID;

public class GameState {
    private static final int MAX_ROUNDS = 6;
    public static final long ROUND_LIMIT_MS = 90000;

    private int tries = 5;
    private int round = 1;
    private long roundStartTime;

    private String answer;
    private int totalScore = 0;
    private int roundScore = 0;

    private final char[] roundResult = new char[MAX_ROUNDS];
    private final int[] roundScores = new int[MAX_ROUNDS];

    private boolean needInitNextRound = false;
    private String gameId;

    public GameState() {
        resetState();
    }
    public void resetState(){
        tries = 5;
        round = 1;
        roundStartTime = 0L;
        answer = null;
        totalScore = 0;
        roundScore = 0;
        Arrays.fill(roundResult, '-');
        Arrays.fill(roundScores, 0);
        needInitNextRound = false;
        gameId = null;
    }

    public boolean isGameOver() {
        return round > MAX_ROUNDS;
    }
    public boolean checkRoundOver(boolean correct) {
        boolean isOver = false;
        int idx = round -1;
        if (correct) {
            roundScore = tries;
            totalScore = totalScore + roundScore;
            roundResult[idx] = 'o';
            roundScores[idx] = roundScore;
            tries = 5;
            round++;
            isOver = true;
        } else if (tries < 1 || isTimeOver()) {
            roundScore = 0;
            roundResult[idx] = 'X';
            roundScores[idx] = roundScore;
            tries = 5;
            round++;
            isOver = true;
        }
        if (isOver) {
            needInitNextRound = true;
        }
        return isOver;
    }
    public boolean isTimeOver() {
        return System.currentTimeMillis() - roundStartTime >= ROUND_LIMIT_MS;
    }
    public void startNewRound() {
        tries = 5;
        roundStartTime = System.currentTimeMillis();
    }
    public void assignAnswer(String answer) {
        this.answer = answer;
    }
    public void initNewGameId(){
        gameId = UUID.randomUUID().toString();
    }

    public void decreaseTries() {
        if (tries > 0) {
            tries--;
        }
    }
    public void clearNeedInitNextRound() {
        this.needInitNextRound = false;
    }

    public int getRound() {
        return round;
    }

    public int getTries() {
        return tries;
    }

    public int getTotalScore() {
        return totalScore;
    }

    public int getRoundScore() {
        return roundScore;
    }

    public String getAnswer() {
        return answer;
    }

    public boolean isNeedInitNextRound() {
        return needInitNextRound;
    }

    public String getGameId() {
        return gameId;
    }

    public char[] getRoundResult() {
        return roundResult;
    }

    public int[] getRoundScores() {
        return roundScores;
    }

    public long getRoundStartTime() { return roundStartTime; }

    public void setRoundStartTime(long roundStartTime) { this.roundStartTime = roundStartTime; }
}
