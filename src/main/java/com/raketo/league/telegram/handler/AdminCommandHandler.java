package com.raketo.league.telegram.handler;

import com.raketo.league.model.*;
import com.raketo.league.service.*;
import com.raketo.league.telegram.BotCommand;
import com.raketo.league.telegram.TelegramBot;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
        } else {
            bot.sendMessage(chatId, "Unknown admin command. Type /admin for help.");
        }
    }

    public void handleCallback(Update update, TelegramBot bot) {
        String callbackData = update.getCallbackQuery().getData();
        logger.info("Admin callback received: {}", callbackData);
    }

    private void handleStartCommand(Long chatId, TelegramBot bot) {
        bot.sendMessage(chatId, "Welcome Admin! Use /admin to see available commands.");
    }

    private void handleAdminCommand(Long chatId, TelegramBot bot) {
        Long userId = chatId;
        boolean isAlsoPlayer = playerService.findByTelegramId(userId).isPresent();

        StringBuilder helpMessage = new StringBuilder();
        helpMessage.append("Admin Commands:\n");
        helpMessage.append(BotCommand.CREATE_TOURNAMENT.getCommand()).append(" <name> <desc> <startDate:yyyy-MM-dd> - Create tournament\n");
        helpMessage.append(BotCommand.ADD_PLAYER.getCommand()).append(" <@username> <name> - Add a player\n");
        helpMessage.append(BotCommand.LIST_TOURNAMENTS.getCommand()).append(" - List all tournaments\n");
        helpMessage.append(BotCommand.LIST_PLAYERS.getCommand()).append(" - List all players\n");
        helpMessage.append(BotCommand.LIST_DIVISIONS.getCommand()).append(" - List all divisions\n");
        helpMessage.append(BotCommand.ASSIGN_PLAYER.getCommand()).append(" <playerId> <divisionTournamentId> - Assign player to division\n");
        helpMessage.append(BotCommand.VIEW_SCHEDULE.getCommand()).append(" <divisionTournamentId> - View schedule\n");
        helpMessage.append(BotCommand.GENERATE_TOURS.getCommand()).append(" <divisionTournamentId> <start:yyyy-MM-dd> <intervalDays> - Generate tours\n");

        if (isAlsoPlayer) {
            helpMessage.append("\nPlayer Commands:\n");
            helpMessage.append(BotCommand.SCHEDULE.getCommand()).append(" - View your schedule\n");
            helpMessage.append(BotCommand.HELP.getCommand()).append(" - Show help\n");
        }

        bot.sendMessage(chatId, helpMessage.toString());
    }

    private void handleCreateTournamentCommand(Long chatId, String text, TelegramBot bot) {
        try {
            String[] parts = text.split(" ", 4);
            if (parts.length < 4) {
                bot.sendMessage(chatId, "Usage: " + BotCommand.CREATE_TOURNAMENT.getCommand() + " <name> <description> <startDate:yyyy-MM-dd>");
                return;
            }
            String name = parts[1];
            String description = parts[2];
            LocalDateTime startDate = LocalDateTime.parse(parts[3] + "T00:00:00");

            Tournament tournament = tournamentService.createTournament(name, description, startDate);
            bot.sendMessage(chatId, "Tournament created successfully!\nID: " + tournament.getId() + "\nName: " + tournament.getName());
        } catch (Exception e) {
            logger.error("Error creating tournament", e);
            bot.sendMessage(chatId, "Failed to create tournament: " + e.getMessage());
        }
    }

    private void handleAddPlayerCommand(Long chatId, String text, TelegramBot bot) {
        try {
            String[] parts = text.split(" ", 3);
            if (parts.length < 3) {
                bot.sendMessage(chatId, "Usage: " + BotCommand.ADD_PLAYER.getCommand() + " <@username> <name>");
                return;
            }
            String username = parts[1].replace("@", "");
            String name = parts[2];

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
                    for (ScheduleService.TourInfo ti : ps.tours()) {
                        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM HH:mm");
                        message.append("  Tour ").append(ti.tourId())
                               .append(": ").append(ti.startDate().format(fmt))
                               .append(" - ").append(ti.endDate().format(fmt))
                               .append(" [").append(ti.status()).append("]");
                        if (ti.opponent() != null) {
                            message.append(" vs ").append(ti.opponent().getName());
                        }
                        message.append("\n");
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
}
