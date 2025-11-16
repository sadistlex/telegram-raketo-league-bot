package com.raketo.league.telegram;

public enum BotCommand {
    START("/start", CommandType.PLAYER),
    SCHEDULE("/schedule", CommandType.PLAYER),
    HELP("/help", CommandType.PLAYER),

    ADMIN("/admin", CommandType.ADMIN),
    CREATE_TOURNAMENT("/createtournament", CommandType.ADMIN),
    ADD_PLAYER("/addplayer", CommandType.ADMIN),
    VIEW_SCHEDULE("/viewschedule", CommandType.ADMIN),
    LIST_TOURNAMENTS("/listtournaments", CommandType.ADMIN),
    LIST_PLAYERS("/listplayers", CommandType.ADMIN),
    LIST_DIVISIONS("/listdivisions", CommandType.ADMIN),
    ASSIGN_PLAYER("/assignplayer", CommandType.ADMIN),
    GENERATE_TOURS("/gentours", CommandType.ADMIN),
    REGENERATE_TOURS("/regentours", CommandType.ADMIN);

    private final String command;
    private final CommandType type;

    BotCommand(String command, CommandType type) {
        this.command = command;
        this.type = type;
    }

    public String getCommand() {
        return command;
    }

    public CommandType getType() {
        return type;
    }

    public boolean matches(String text) {
        return text != null && text.startsWith(command);
    }

    public static boolean isAdminCommand(String text) {
        if (text == null) return false;
        for (BotCommand cmd : values()) {
            if (cmd.type == CommandType.ADMIN && text.startsWith(cmd.command)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isPlayerCommand(String text) {
        if (text == null) return false;
        for (BotCommand cmd : values()) {
            if (cmd.type == CommandType.PLAYER && text.startsWith(cmd.command)) {
                return true;
            }
        }
        return false;
    }

    public enum CommandType {
        ADMIN,
        PLAYER
    }
}

