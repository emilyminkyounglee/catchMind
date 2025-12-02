package kr.ac.ewha.catchMind.model;

import com.fasterxml.jackson.annotation.JsonInclude;

// 값이 null인 필드는 JSON으로 보낼 때 아예 빼버리기
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GameMessage {

    // 공통
    private String type;// JOIN, DRAW, GUESS, GUESS_RESULT 등

    private String roomId;

    // 플레이어 정보
    private String nickname;
    private String role;        // DRAWER, GUESSER
    private String drawerName;
    private String guesserName;
    private String myRole;

    // 그림 정보
    private Double x;
    private Double y;
    private String color;       // #000000
    private Integer thickness;  // 선 굵기
    private Boolean dragging;   // 드래그 여부

    // 정답, 결과
    private String guess;       // 플레이어가 입력한 정답
    private Boolean correct;    // 정답 여부
    private Integer triesLeft;  // 남은 기회
    private Integer round;      // 현재 라운드
    private Integer roundScore; // 라운드 점수
    private Integer totalScore; // 총점
    private String answer;      // 실제 정답
    private Boolean roundSuccess; // 라운드 성공 여부

    private String text;        // 안내 메시지

    // 기본 생성자
    public GameMessage() {}

    // Getter Setter
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getDrawerName() {
        return drawerName;
    }

    public void setDrawerName(String drawerName) {
        this.drawerName = drawerName;
    }

    public String getGuesserName() {
        return guesserName;
    }

    public void setGuesserName(String guesserName) {
        this.guesserName = guesserName;
    }

    public String getMyRole() {
        return myRole;
    }

    public void setMyRole(String myRole) {
        this.myRole = myRole;
    }

    public Double getX() {
        return x;
    }

    public void setX(Double x) {
        this.x = x;
    }

    public Double getY() {
        return y;
    }

    public void setY(Double y) {
        this.y = y;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Integer getThickness() {
        return thickness;
    }

    public void setThickness(Integer thickness) {
        this.thickness = thickness;
    }

    public Boolean getDragging() {
        return dragging;
    }

    public void setDragging(Boolean dragging) {
        this.dragging = dragging;
    }

    public String getGuess() {
        return guess;
    }

    public void setGuess(String guess) {
        this.guess = guess;
    }

    public Boolean getCorrect() {
        return correct;
    }

    public void setCorrect(Boolean correct) {
        this.correct = correct;
    }

    public Integer getTriesLeft() {
        return triesLeft;
    }

    public void setTriesLeft(Integer triesLeft) {
        this.triesLeft = triesLeft;
    }

    public Integer getRound() {
        return round;
    }

    public void setRound(Integer round) {
        this.round = round;
    }

    public Integer getRoundScore() {
        return roundScore;
    }

    public void setRoundScore(Integer roundScore) {
        this.roundScore = roundScore;
    }

    public Integer getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(Integer totalScore) {
        this.totalScore = totalScore;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public Boolean getRoundSuccess() {
        return roundSuccess;
    }

    public void setRoundSuccess(Boolean roundSuccess) {
        this.roundSuccess = roundSuccess;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

}