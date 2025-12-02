package kr.ac.ewha.catchMind.model;

import kr.ac.ewha.catchMind.service.GameService;

import java.util.ArrayList;
import java.util.List;

public class GameRoom {
    private String roomId;
    private GameService gameService;
    private int capacity;
    private List<Player> playerList = new ArrayList<>();

    public GameRoom(String roomId, GameService gameService, int capacity) {
        this.roomId = roomId;
        this.gameService = gameService;
        this.capacity = capacity;
    }
    public String getRoomId() {
        return roomId;
    }
    public GameService getGameService() {
        return gameService;
    }
    public List<Player> getPlayerList() {
        return playerList;
    }
    public int getCapacity() {
        return capacity;
    }
    public boolean isEmpty() {
        return playerList.isEmpty();
    }
    public boolean addPlayer(Player p) {
        if (p == null) {
            return false;
        }
        if (isFull()) {
            return false;
        }
        if (playerList.contains(p)) {
            return false;
        }
        playerList.add(p);
        return true;
    }
    public void removePlayer(Player p) {
        playerList.remove(p);
    }
    public Player findPlayerByName(String name) {
        if (name == null) {
            return null;
        }
        for (Player p : playerList) {
            if (p.getName().equals(name)) {
                return p;
            }
        }
        return null;
    }
    public int totalPeople() {
        return playerList.size();
    }
    public boolean isFull() {
        return playerList.size() >= capacity;
    }

}
