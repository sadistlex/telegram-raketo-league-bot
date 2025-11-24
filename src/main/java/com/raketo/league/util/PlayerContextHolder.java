package com.raketo.league.util;

public class PlayerContextHolder {
    private static final ThreadLocal<Long> currentPlayer = new ThreadLocal<>();

    public static void setCurrentPlayerId(Long playerId) {
        currentPlayer.set(playerId);
    }

    public static Long getCurrentPlayerId() {
        return currentPlayer.get();
    }

    public static void clear() {
        currentPlayer.remove();
    }
}

