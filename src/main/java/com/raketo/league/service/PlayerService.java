package com.raketo.league.service;

import com.raketo.league.model.Language;
import com.raketo.league.model.Player;
import com.raketo.league.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
    public Optional<Player> findById(Long id) {
        return playerRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Player> findByTelegramUsername(String username) {
        return playerRepository.findByTelegramUsername(username);
    }

    @Transactional
    public Player findOrLinkPlayer(Long telegramId, String telegramUsername) {
        Optional<Player> byId = findByTelegramId(telegramId);
        if (byId.isPresent()) {
            return byId.get();
        }
        Optional<Player> byUsername = findByTelegramUsername(telegramUsername);
        if (byUsername.isPresent()) {
            Player player = byUsername.get();
            if (player.getTelegramId() == null) {
                player.setTelegramId(telegramId);
                logger.info("Linked telegram ID {} to player {}", telegramId, telegramUsername);
                return playerRepository.save(player);
            }
            return player;
        }
        return null;
    }

    @Transactional
    public Player createPlayer(Long telegramId, String username, String name) {
        Player player = Player.builder()
                .telegramId(telegramId)
                .telegramUsername(username)
                .name(name)
                .isActive(true)
                .language(Language.RU)
                .build();
        return playerRepository.save(player);
    }

    @Transactional(readOnly = true)
    public boolean isPlayerRegistered(String username) {
        return playerRepository.existsByTelegramUsername(username);
    }

    @Transactional(readOnly = true)
    public List<Player> getAllPlayers() {
        return playerRepository.findAll();
    }

    @Transactional
    public void updateLanguage(Player player, Language language) {
        player.setLanguage(language);
        playerRepository.save(player);
    }
}
