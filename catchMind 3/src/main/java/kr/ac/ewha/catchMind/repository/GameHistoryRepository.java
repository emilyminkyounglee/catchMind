package kr.ac.ewha.catchMind.repository;

import kr.ac.ewha.catchMind.model.GameHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameHistoryRepository extends JpaRepository<GameHistory, String> {
}
