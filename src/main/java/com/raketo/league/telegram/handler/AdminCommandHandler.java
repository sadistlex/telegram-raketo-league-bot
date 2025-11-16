package com.raketo.league.telegram.handler;

import com.raketo.league.model.*;
import com.raketo.league.service.*;
import com.raketo.league.telegram.BotCommand;
import com.raketo.league.telegram.TelegramBot;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AdminCommandHandler {
    private static final Logger logger = LoggerFactory.getLogger(AdminCommandHandler.class);
    private final TourService tourService;
    private final PlayerService playerService;
    private final TournamentService tournamentService;
    private final DivisionService divisionService;
    private final ScheduleService scheduleService;

    public void handleCommand(Update update, TelegramBot bot) {
        String text = update.getMessage().getText();
        Long chatId = update.getMessage().getChatId();

        if (BotCommand.START.matches(text)) {
            handleStartCommand(chatId, bot);
        } else if (BotCommand.ADMIN.matches(text)) {
            handleAdminCommand(chatId, bot);
        } else if (BotCommand.CREATE_TOURNAMENT.matches(text)) {
            handleCreateTournamentCommand(chatId, text, bot);
        } else if (BotCommand.ADD_PLAYER.matches(text)) {
            handleAddPlayerCommand(chatId, text, bot);
        } else if (BotCommand.VIEW_SCHEDULE.matches(text)) {
            handleViewScheduleCommand(chatId, text, bot);
        } else if (BotCommand.LIST_TOURNAMENTS.matches(text)) {
            handleListTournaments(chatId, bot);
        } else if (BotCommand.LIST_PLAYERS.matches(text)) {
            handleListPlayers(chatId, bot);
        } else if (BotCommand.LIST_DIVISIONS.matches(text)) {
            handleListDivisions(chatId, bot);
        } else if (BotCommand.ASSIGN_PLAYER.matches(text)) {
            handleAssignPlayer(chatId, text, bot);
        } else if (BotCommand.GENERATE_TOURS.matches(text)) {
            handleGenerateTours(chatId, text, bot);
        } else if (BotCommand.REGENERATE_TOURS.matches(text)) {
            handleRegenerateTours(chatId, text, bot);
        } else {
            bot.sendMessage(chatId, "Unknown admin command. Type /admin for help.");
        }
    }

    public void handleCallback(Update update, TelegramBot bot) {
        String callbackData = update.getCallbackQuery().getData();
        Long chatId = update.getCallbackQuery().getMessage().getChatId();

        logger.info("Admin callback received: {}", callbackData);

        if ("ADMIN_MENU".equals(callbackData)) {
            showAdminMenu(chatId, bot);
        } else if ("ADMIN_HELP".equals(callbackData)) {
            handleAdminCommand(chatId, bot);
        } else if (callbackData.startsWith("ADMIN_CMD_")) {
            handleAdminCommandCallback(chatId, callbackData, bot);
        }
    }

    private void handleStartCommand(Long chatId, TelegramBot bot) {
        showAdminMenu(chatId, bot);
    }

    private void showAdminMenu(Long chatId, TelegramBot bot) {
        boolean isAlsoPlayer = playerService.findByTelegramId(chatId).isPresent();

        StringBuilder message = new StringBuilder();
        message.append("🛡️ Admin Panel\n\n");
        message.append("Select an action from the menu below:");

        SendMessage sendMessage = SendMessage.builder()
                .chatId(chatId.toString())
                .text(message.toString())
                .replyMarkup(createAdminMenuKeyboard(isAlsoPlayer))
                .build();

        try {
            bot.execute(sendMessage);
        } catch (Exception e) {
            logger.error("Failed to send admin menu", e);
        }
    }

    private InlineKeyboardMarkup createAdminMenuKeyboard(boolean isAlsoPlayer) {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        InlineKeyboardButton listTournamentsBtn = InlineKeyboardButton.builder()
                .text("🏆 List Tournaments")
                .callbackData("ADMIN_CMD_LIST_TOURNAMENTS")
                .build();

        InlineKeyboardButton listPlayersBtn = InlineKeyboardButton.builder()
                .text("👥 List Players")
                .callbackData("ADMIN_CMD_LIST_PLAYERS")
                .build();
        keyboard.add(List.of(listTournamentsBtn, listPlayersBtn));

        InlineKeyboardButton listDivisionsBtn = InlineKeyboardButton.builder()
                .text("📊 List Divisions")
                .callbackData("ADMIN_CMD_LIST_DIVISIONS")
                .build();

        InlineKeyboardButton createTournamentBtn = InlineKeyboardButton.builder()
                .text("➕ Create Tournament")
                .callbackData("ADMIN_CMD_CREATE_TOURNAMENT")
                .build();
        keyboard.add(List.of(listDivisionsBtn, createTournamentBtn));

        InlineKeyboardButton addPlayerBtn = InlineKeyboardButton.builder()
                .text("👤➕ Add Player")
                .callbackData("ADMIN_CMD_ADD_PLAYER")
                .build();

        InlineKeyboardButton assignPlayerBtn = InlineKeyboardButton.builder()
                .text("📝 Assign Player")
                .callbackData("ADMIN_CMD_ASSIGN_PLAYER")
                .build();
        keyboard.add(List.of(addPlayerBtn, assignPlayerBtn));

        InlineKeyboardButton generateToursBtn = InlineKeyboardButton.builder()
                .text("🗓️ Generate Tours")
                .callbackData("ADMIN_CMD_GENERATE_TOURS")
                .build();

        InlineKeyboardButton regenerateToursBtn = InlineKeyboardButton.builder()
                .text("🔄 Regenerate Tours")
                .callbackData("ADMIN_CMD_REGENERATE_TOURS")
                .build();
        keyboard.add(List.of(generateToursBtn, regenerateToursBtn));

        InlineKeyboardButton viewScheduleBtn = InlineKeyboardButton.builder()
                .text("📅 View Schedule")
                .callbackData("ADMIN_CMD_VIEW_SCHEDULE")
                .build();
        keyboard.add(List.of(viewScheduleBtn));

        InlineKeyboardButton helpBtn = InlineKeyboardButton.builder()
                .text("📖 Command Help")
                .callbackData("ADMIN_HELP")
                .build();
        keyboard.add(List.of(helpBtn));

        if (isAlsoPlayer) {
            InlineKeyboardButton playerBtn = InlineKeyboardButton.builder()
                    .text("👤 My Schedule")
                    .callbackData("PLAYER_SCHEDULE")
                    .build();
            keyboard.add(List.of(playerBtn));
        }

        return InlineKeyboardMarkup.builder().keyboard(keyboard).build();
    }

    private void handleAdminCommandCallback(Long chatId, String callbackData, TelegramBot bot) {
        switch (callbackData) {
            case "ADMIN_CMD_LIST_TOURNAMENTS":
                handleListTournaments(chatId, bot);
                break;
            case "ADMIN_CMD_LIST_PLAYERS":
                handleListPlayers(chatId, bot);
                break;
            case "ADMIN_CMD_LIST_DIVISIONS":
                handleListDivisions(chatId, bot);
                break;
            case "ADMIN_CMD_CREATE_TOURNAMENT":
                bot.sendMessage(chatId, "Use command: " + BotCommand.CREATE_TOURNAMENT.getCommand() + " <name> <description> <startDate:yyyy-MM-dd>");
                break;
            case "ADMIN_CMD_ADD_PLAYER":
                bot.sendMessage(chatId, "Use command: " + BotCommand.ADD_PLAYER.getCommand() + " <@username> <name>");
                break;
            case "ADMIN_CMD_ASSIGN_PLAYER":
                bot.sendMessage(chatId, "Use command: " + BotCommand.ASSIGN_PLAYER.getCommand() + " <playerId> <divisionTournamentId>");
                break;
            case "ADMIN_CMD_GENERATE_TOURS":
                bot.sendMessage(chatId, "Use command: " + BotCommand.GENERATE_TOURS.getCommand() + " <divisionTournamentId> <start:yyyy-MM-dd> <intervalDays>");
                break;
            case "ADMIN_CMD_REGENERATE_TOURS":
                bot.sendMessage(chatId, "Use command: " + BotCommand.REGENERATE_TOURS.getCommand() + " <divisionTournamentId>");
                break;
            case "ADMIN_CMD_VIEW_SCHEDULE":
                bot.sendMessage(chatId, "Use command: " + BotCommand.VIEW_SCHEDULE.getCommand() + " <divisionTournamentId>");
                break;
            default:
                bot.sendMessage(chatId, "Unknown command");
        }
    }

    private void handleAdminCommand(Long chatId, TelegramBot bot) {
        Long userId = chatId;
        boolean isAlsoPlayer = playerService.findByTelegramId(userId).isPresent();

        StringBuilder helpMessage = new StringBuilder();
        helpMessage.append("Admin Commands:\n\n");
        helpMessage.append("Note: Use <> for parameters with spaces\n\n");
        helpMessage.append(BotCommand.CREATE_TOURNAMENT.getCommand()).append(" <name> <desc> <yyyy-MM-dd>\n");
        helpMessage.append("  Example: /createtournament <Summer 2025> <Summer League> <2025-06-01>\n\n");
        helpMessage.append(BotCommand.ADD_PLAYER.getCommand()).append(" <@username> <name>\n");
        helpMessage.append("  Example: /addplayer <@john> <John Doe>\n\n");
        helpMessage.append(BotCommand.LIST_TOURNAMENTS.getCommand()).append(" - List all tournaments\n");
        helpMessage.append(BotCommand.LIST_PLAYERS.getCommand()).append(" - List all players\n");
        helpMessage.append(BotCommand.LIST_DIVISIONS.getCommand()).append(" - List all divisions\n");
        helpMessage.append(BotCommand.ASSIGN_PLAYER.getCommand()).append(" <playerId> <divTournamentId>\n");
        helpMessage.append(BotCommand.VIEW_SCHEDULE.getCommand()).append(" <divisionTournamentId>\n");
        helpMessage.append(BotCommand.GENERATE_TOURS.getCommand()).append(" <divTournamentId> <yyyy-MM-dd> <days>\n");
        helpMessage.append(BotCommand.REGENERATE_TOURS.getCommand()).append(" <divTournamentId> - Regenerate tours (preserves availability)\n");

        if (isAlsoPlayer) {
            helpMessage.append("\nPlayer Commands:\n");
            helpMessage.append(BotCommand.SCHEDULE.getCommand()).append(" - View your schedule\n");
            helpMessage.append(BotCommand.HELP.getCommand()).append(" - Show help\n");
        }

        bot.sendMessage(chatId, helpMessage.toString());
    }

    private void handleCreateTournamentCommand(Long chatId, String text, TelegramBot bot) {
        try {
            String params = text.substring(BotCommand.CREATE_TOURNAMENT.getCommand().length()).trim();

            List<String> parsed = parseParameters(params);
            if (parsed.size() < 3) {
                bot.sendMessage(chatId, "Usage: " + BotCommand.CREATE_TOURNAMENT.getCommand() + " <name> <description> <startDate:yyyy-MM-dd>");
                return;
            }

            String name = parsed.get(0);
            String description = parsed.get(1);
            LocalDateTime startDate = LocalDateTime.parse(parsed.get(2) + "T00:00:00");

            Tournament tournament = tournamentService.createTournament(name, description, startDate);
            bot.sendMessage(chatId, "Tournament created successfully!\nID: " + tournament.getId() + "\nName: " + tournament.getName());
        } catch (Exception e) {
            logger.error("Error creating tournament", e);
            bot.sendMessage(chatId, "Failed to create tournament: " + e.getMessage());
        }
    }

    private void handleAddPlayerCommand(Long chatId, String text, TelegramBot bot) {
        try {
            String params = text.substring(BotCommand.ADD_PLAYER.getCommand().length()).trim();

            List<String> parsed = parseParameters(params);
            if (parsed.size() < 2) {
                bot.sendMessage(chatId, "Usage: " + BotCommand.ADD_PLAYER.getCommand() + " <@username> <name>");
                return;
            }

            String username = parsed.get(0).replace("@", "");
            String name = parsed.get(1);

            if (playerService.isPlayerRegistered(username)) {
                bot.sendMessage(chatId, "Player @" + username + " is already registered.");
                return;
            }

            Player player = playerService.createPlayer(null, username, name);
            bot.sendMessage(chatId, "Player added successfully!\nID: " + player.getId() + "\nName: " + player.getName() + "\nUsername: @" + player.getTelegramUsername());
        } catch (Exception e) {
            logger.error("Error adding player", e);
            bot.sendMessage(chatId, "Failed to add player: " + e.getMessage());
        }
    }

    private void handleViewScheduleCommand(Long chatId, String text, TelegramBot bot) {
        try {
            String[] parts = text.split(" ");
            if (parts.length < 2) {
                bot.sendMessage(chatId, "Usage: " + BotCommand.VIEW_SCHEDULE.getCommand() + " <divisionTournamentId>");
                return;
            }
            Long divisionTournamentId = Long.parseLong(parts[1]);

            List<PlayerDivisionAssignment> assignments = divisionService.getPlayersByDivisionTournament(divisionTournamentId);
            if (assignments.isEmpty()) {
                bot.sendMessage(chatId, "No players found in this division tournament.");
                return;
            }

            StringBuilder message = new StringBuilder();
            message.append("Schedule for Division Tournament ID: ").append(divisionTournamentId).append("\n\n");

            for (PlayerDivisionAssignment assignment : assignments) {
                Player player = assignment.getPlayer();
                ScheduleService.PlayerSchedule ps = scheduleService.buildPlayerSchedule(player);
                message.append("Player: ").append(player.getName()).append(" (@").append(player.getTelegramUsername()).append(")\n");

                if (ps.tours().isEmpty()) {
                    message.append("  No tours assigned\n");
                } else {
                    int tourNumber = 1;
                    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM");
                    for (ScheduleService.TourInfo ti : ps.tours()) {
                        message.append("  Tour ").append(tourNumber)
                               .append(" (").append(ti.startDate().format(fmt))
                               .append("-").append(ti.endDate().format(fmt))
                               .append(") - ");
                        if (ti.opponent() != null) {
                            message.append(ti.opponent().getName())
                                   .append(" [").append(ti.status()).append("]");
                        } else {
                            message.append("Bye");
                        }
                        message.append("\n");
                        tourNumber++;
                    }
                }
                message.append("\n");
            }

            bot.sendMessage(chatId, message.toString());
        } catch (Exception e) {
            logger.error("Error viewing schedule", e);
            bot.sendMessage(chatId, "Failed to view schedule: " + e.getMessage());
        }
    }

    private void handleGenerateTours(Long chatId, String text, TelegramBot bot) {
        try {
            String[] parts = text.split(" ");
            if (parts.length < 4) {
                bot.sendMessage(chatId, "Usage: " + BotCommand.GENERATE_TOURS.getCommand() + " <divisionTournamentId> <start:yyyy-MM-dd> <intervalDays>");
                return;
            }
            Long dtId = Long.parseLong(parts[1]);
            LocalDateTime start = LocalDateTime.parse(parts[2] + "T00:00:00");
            int interval = Integer.parseInt(parts[3]);
            int created = tourService.generateRoundRobinTours(dtId, start, interval);
            bot.sendMessage(chatId, "Generated " + created + " tours.");
        } catch (Exception e) {
            logger.error("Error generating tours", e);
            bot.sendMessage(chatId, "Failed to generate tours: " + e.getMessage());
        }
    }

    private void handleRegenerateTours(Long chatId, String text, TelegramBot bot) {
        try {
            String[] parts = text.split(" ");
            if (parts.length < 2) {
                bot.sendMessage(chatId, "Usage: " + BotCommand.REGENERATE_TOURS.getCommand() + " <divisionTournamentId>");
                return;
            }
            Long dtId = Long.parseLong(parts[1]);
            int created = tourService.regenerateRoundRobinTours(dtId);
            bot.sendMessage(chatId, "Regenerated " + created + " tours. Availability data preserved where possible. Schedule requests deleted.");
        } catch (Exception e) {
            logger.error("Error regenerating tours", e);
            bot.sendMessage(chatId, "Failed to regenerate tours: " + e.getMessage());
        }
    }

    private void handleListTournaments(Long chatId, TelegramBot bot) {
        try {
            List<Tournament> tournaments = tournamentService.getAllTournaments();
            if (tournaments.isEmpty()) {
                bot.sendMessage(chatId, "No tournaments found.");
                return;
            }

            StringBuilder message = new StringBuilder();
            message.append("Tournaments:\n\n");
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            for (Tournament t : tournaments) {
                message.append("ID: ").append(t.getId()).append("\n");
                message.append("Name: ").append(t.getName()).append("\n");
                message.append("Description: ").append(t.getDescription() != null ? t.getDescription() : "N/A").append("\n");
                message.append("Start Date: ").append(t.getStartDate().format(fmt)).append("\n");
                message.append("Active: ").append(t.getIsActive() ? "Yes" : "No").append("\n\n");
            }

            bot.sendMessage(chatId, message.toString());
        } catch (Exception e) {
            logger.error("Error listing tournaments", e);
            bot.sendMessage(chatId, "Failed to list tournaments: " + e.getMessage());
        }
    }

    private void handleListPlayers(Long chatId, TelegramBot bot) {
        try {
            List<Player> players = playerService.getAllPlayers();
            if (players.isEmpty()) {
                bot.sendMessage(chatId, "No players found.");
                return;
            }

            StringBuilder message = new StringBuilder();
            message.append("Players:\n\n");

            for (Player p : players) {
                message.append("ID: ").append(p.getId()).append("\n");
                message.append("Name: ").append(p.getName()).append("\n");
                message.append("Username: @").append(p.getTelegramUsername()).append("\n");
                message.append("Telegram ID: ").append(p.getTelegramId() != null ? p.getTelegramId() : "Not linked").append("\n");
                message.append("Active: ").append(p.getIsActive() ? "Yes" : "No").append("\n\n");
            }

            bot.sendMessage(chatId, message.toString());
        } catch (Exception e) {
            logger.error("Error listing players", e);
            bot.sendMessage(chatId, "Failed to list players: " + e.getMessage());
        }
    }

    private void handleListDivisions(Long chatId, TelegramBot bot) {
        try {
            List<Division> divisions = divisionService.getAllDivisions();
            if (divisions.isEmpty()) {
                bot.sendMessage(chatId, "No divisions found.");
                return;
            }

            StringBuilder message = new StringBuilder();
            message.append("Divisions:\n\n");

            for (Division d : divisions) {
                message.append("ID: ").append(d.getId()).append("\n");
                message.append("Name: ").append(d.getName()).append("\n");
                message.append("Level: ").append(d.getLevel() != null ? d.getLevel() : "N/A").append("\n");
                message.append("Active: ").append(d.getIsActive() ? "Yes" : "No").append("\n\n");
            }

            bot.sendMessage(chatId, message.toString());
        } catch (Exception e) {
            logger.error("Error listing divisions", e);
            bot.sendMessage(chatId, "Failed to list divisions: " + e.getMessage());
        }
    }

    private void handleAssignPlayer(Long chatId, String text, TelegramBot bot) {
        try {
            String[] parts = text.split(" ");
            if (parts.length < 3) {
                bot.sendMessage(chatId, "Usage: " + BotCommand.ASSIGN_PLAYER.getCommand() + " <playerId> <divisionTournamentId>");
                return;
            }

            Long playerId = Long.parseLong(parts[1]);
            Long divisionTournamentId = Long.parseLong(parts[2]);

            Player player = playerService.findById(playerId).orElse(null);
            if (player == null) {
                bot.sendMessage(chatId, "Player not found with ID: " + playerId);
                return;
            }

            DivisionTournament divisionTournament = divisionService.findDivisionTournamentById(divisionTournamentId).orElse(null);
            if (divisionTournament == null) {
                bot.sendMessage(chatId, "Division Tournament not found with ID: " + divisionTournamentId);
                return;
            }

            PlayerDivisionAssignment assignment = divisionService.assignPlayerToDivision(player, divisionTournament);
            bot.sendMessage(chatId, "Player assigned successfully!\nPlayer: " + player.getName() + "\nDivision Tournament ID: " + divisionTournament.getId());
        } catch (Exception e) {
            logger.error("Error assigning player", e);
            bot.sendMessage(chatId, "Failed to assign player: " + e.getMessage());
        }
    }

    private List<String> parseParameters(String params) {
        List<String> result = new ArrayList<>();
        if (params == null || params.trim().isEmpty()) {
            return result;
        }

        int i = 0;
        while (i < params.length()) {
            char c = params.charAt(i);

            if (c == '<') {
                int closingBracket = params.indexOf('>', i);
                if (closingBracket == -1) {
                    result.add(params.substring(i + 1).trim());
                    break;
                }
                result.add(params.substring(i + 1, closingBracket).trim());
                i = closingBracket + 1;
            } else if (!Character.isWhitespace(c)) {
                int nextSpace = params.indexOf(' ', i);
                if (nextSpace == -1) {
                    result.add(params.substring(i).trim());
                    break;
                }
                result.add(params.substring(i, nextSpace).trim());
                i = nextSpace + 1;
            } else {
                i++;
            }
        }

        return result;
    }
}
