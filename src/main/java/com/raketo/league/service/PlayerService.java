package com.raketo.league.service;

import com.raketo.league.model.Player;
import com.raketo.league.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PlayerService {

    private static final Logger logger = LoggerFactory.getLogger(PlayerService.class);

    private final PlayerRepository playerRepository;

    @Transactional(readOnly = true)
    public Optional<Player> findByTelegramId(Long telegramId) {
        return playerRepository.findByTelegramId(telegramId);
    }

    @Transactional(readOnly = true)
    public Optional<Player> findByTelegramUsername(String username) {
        return playerRepository.findByTelegramUsername(username);
    }

    @Transactional
    public Player createPlayer(Long telegramId, String username, String firstName, String lastName) {
        Player player = Player.builder()
                .telegramId(telegramId)
                .telegramUsername(username)
                .firstName(firstName)
                .lastName(lastName)
                .isActive(true)
                .postponementsUsed(0)
                .createdAt(LocalDateTime.now())
                .lastActiveAt(LocalDateTime.now())
                .build();
        return playerRepository.save(player);
    }

    @Transactional
    public Player updateLastActive(Long playerId) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Player not found: " + playerId));
        player.setLastActiveAt(LocalDateTime.now());
        return playerRepository.save(player);
    }

    @Transactional(readOnly = true)
    public boolean isPlayerRegistered(String username) {
        return playerRepository.existsByTelegramUsername(username);
    }
}

