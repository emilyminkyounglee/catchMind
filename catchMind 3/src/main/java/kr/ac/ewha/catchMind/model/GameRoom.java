package kr.ac.ewha.catchMind.model;

import kr.ac.ewha.catchMind.service.GameService;

import java.util.ArrayList;
import java.util.List;

public class GameRoom {
    private final String roomId;
    private final int capacity;
    private final List<Player> playerList = new ArrayList<>();

    private final GameState gameState = new GameState();

    public GameRoom(String roomId, int capacity) {
        this.roomId = roomId;
        this.capacity = capacity;
    }
    public GameState getGameState() {
        return gameState;
    }

    public String getRoomId() {
        return roomId;
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
        if (p == null || isFull()) {
            return false;
        }
        for (Player player : playerList) {
            if (player.getName() != null && player.getName().equals(p.getName())) {
                return false;
            }
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
