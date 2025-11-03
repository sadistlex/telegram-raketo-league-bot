package com.raketo.league.service;

import com.raketo.league.model.Player;
import com.raketo.league.repository.PlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlayerServiceTest {

    @Mock
    private PlayerRepository playerRepository;

    @InjectMocks
    private PlayerService playerService;

    private Player testPlayer;

    @BeforeEach
    void setUp() {
        testPlayer = Player.builder()
                .id(1L)
                .telegramId(123456789L)
                .telegramUsername("testuser")
                .firstName("Test")
                .lastName("User")
                .isActive(true)
                .build();
    }

    @Test
    void findByTelegramId_shouldReturnPlayer() {
        when(playerRepository.findByTelegramId(123456789L))
                .thenReturn(Optional.of(testPlayer));

        Optional<Player> result = playerService.findByTelegramId(123456789L);

        assertTrue(result.isPresent());
        assertEquals("testuser", result.get().getTelegramUsername());
        verify(playerRepository).findByTelegramId(123456789L);
    }

    @Test
    void createPlayer_shouldSaveAndReturnPlayer() {
        when(playerRepository.save(any(Player.class)))
                .thenReturn(testPlayer);

        Player result = playerService.createPlayer(
                123456789L,
                "testuser",
                "Test",
                "User"
        );

        assertNotNull(result);
        assertEquals("testuser", result.getTelegramUsername());
        verify(playerRepository).save(any(Player.class));
    }

    @Test
    void isPlayerRegistered_shouldReturnTrue_whenPlayerExists() {
        when(playerRepository.existsByTelegramUsername("testuser"))
                .thenReturn(true);

        boolean result = playerService.isPlayerRegistered("testuser");

        assertTrue(result);
        verify(playerRepository).existsByTelegramUsername("testuser");
    }
}

