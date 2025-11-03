package com.raketo.league.repository;

import com.raketo.league.model.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Long> {

    Optional<Player> findByTelegramId(Long telegramId);

    Optional<Player> findByTelegramUsername(String telegramUsername);

    boolean existsByTelegramUsername(String telegramUsername);
}

