package kr.ac.ewha.catchMind.model;


import kr.ac.ewha.catchMind.service.GameService;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GameRoomManager {
    private final Map<String, GameRoom> rooms = new ConcurrentHashMap<>();
    private final GameService gameService;

    public GameRoomManager(GameService gameService) {
        this.gameService = gameService;
    }

    public GameRoom getGameRoom(String roomId) {
        return rooms.get(roomId);
    }
    public void removeGameRoom(String roomId) {
        GameRoom gameRoom = rooms.get(roomId);
        if (gameRoom != null && rooms.isEmpty()) {
            rooms.remove(roomId);
        }
    }
    public synchronized GameRoom getOrAddGameRoom(int capacity) {
        for (GameRoom gameRoom : rooms.values()) {
            if (gameRoom.getCapacity() == capacity && !gameRoom.isFull()) {
                return gameRoom;
            }
        }
        String roomId = UUID.randomUUID().toString();
        GameRoom newRoom = new GameRoom(roomId, gameService, capacity);
        rooms.put(roomId, newRoom);
        return newRoom;
    }

}
