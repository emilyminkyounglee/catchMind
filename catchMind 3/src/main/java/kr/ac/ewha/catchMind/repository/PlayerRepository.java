package kr.ac.ewha.catchMind.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import kr.ac.ewha.catchMind.model.*;

public interface PlayerRepository extends JpaRepository<Player, Long> {
    Player findByName(String name);
}
