package kr.ac.ewha.catchMind.model;

import jakarta.persistence.*;

@Entity
@Table(name = "game_history")
public class GameHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    @Column(name = "game_id", nullable = false, length = 36)
    private String gameId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @Column(length = 1)
    private Character round1Result;
    private Integer round1Score;

    @Column(length = 1)
    private Character round2Result;
    private Integer round2Score;

    @Column(length = 1)
    private Character round3Result;
    private Integer round3Score;

    @Column(length = 1)
    private Character round4Result;
    private Integer round4Score;

    @Column(length = 1)
    private Character round5Result;
    private Integer round5Score;

    @Column(length = 1)
    private Character round6Result;
    private Integer round6Score;

    private Integer totalScore;

    public Character getRound1Result() {
        return round1Result;
    }

    public void setRound1Result(Character round1Result) {
        this.round1Result = round1Result;
    }

    public Integer getRound1Score() {
        return round1Score;
    }

    public void setRound1Score(Integer round1Score) {
        this.round1Score = round1Score;
    }

    public Character getRound2Result() {
        return round2Result;
    }

    public void setRound2Result(Character round2Result) {
        this.round2Result = round2Result;
    }

    public Integer getRound2Score() {
        return round2Score;
    }

    public void setRound2Score(Integer round2Score) {
        this.round2Score = round2Score;
    }

    public Character getRound3Result() {
        return round3Result;
    }

    public void setRound3Result(Character round3Result) {
        this.round3Result = round3Result;
    }

    public Integer getRound3Score() {
        return round3Score;
    }

    public void setRound3Score(Integer round3Score) {
        this.round3Score = round3Score;
    }

    public Character getRound4Result() {
        return round4Result;
    }

    public void setRound4Result(Character round4Result) {
        this.round4Result = round4Result;
    }

    public Integer getRound4Score() {
        return round4Score;
    }

    public void setRound4Score(Integer round4Score) {
        this.round4Score = round4Score;
    }

    public Character getRound5Result() {
        return round5Result;
    }

    public void setRound5Result(Character round5Result) {
        this.round5Result = round5Result;
    }

    public Character getRound6Result() {
        return round6Result;
    }

    public void setRound6Result(Character round6Result) {
        this.round6Result = round6Result;
    }

    public Integer getRound5Score() {
        return round5Score;
    }

    public void setRound5Score(Integer round5Score) {
        this.round5Score = round5Score;
    }

    public Integer getRound6Score() {
        return round6Score;
    }

    public void setRound6Score(Integer round6Score) {
        this.round6Score = round6Score;
    }

    public Integer getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(Integer totalScore) {
        this.totalScore = totalScore;
    }
    public String getId() {
        return id;
    }
    public Player getPlayer() {
        return player;
    }
    public void setPlayer(Player player) {
        this.player = player;
    }
    public void setGameId(String gameId) {
        this.gameId = gameId;
    }
    public String getGameId() {
        return gameId;
    }
}
