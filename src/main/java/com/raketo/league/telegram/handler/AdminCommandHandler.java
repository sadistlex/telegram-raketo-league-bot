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
    private final LocalizationService localizationService;

    public void handleCommand(Update update, TelegramBot bot) {
        String text = update.getMessage().getText();
        Long chatId = update.getMessage().getChatId();
        Player player = playerService.findByTelegramId(chatId).orElse(null);
        if (BotCommand.START.matches(text)) {
            handleStartCommand(chatId, bot, player);
        } else if (BotCommand.ADMIN.matches(text)) {
            handleAdminCommand(chatId, bot, player);
        } else if (BotCommand.CREATE_TOURNAMENT.matches(text)) {
            handleCreateTournamentCommand(chatId, text, bot, player);
        } else if (BotCommand.ADD_PLAYER.matches(text)) {
            handleAddPlayerCommand(chatId, text, bot, player);
        } else if (BotCommand.VIEW_SCHEDULE.matches(text)) {
            handleViewScheduleCommand(chatId, text, bot, player);
        } else if (BotCommand.LIST_TOURNAMENTS.matches(text)) {
            handleListTournaments(chatId, bot, player);
        } else if (BotCommand.LIST_PLAYERS.matches(text)) {
            handleListPlayers(chatId, bot, player);
        } else if (BotCommand.LIST_DIVISIONS.matches(text)) {
            handleListDivisions(chatId, bot, player);
        } else if (BotCommand.ASSIGN_PLAYER.matches(text)) {
            handleAssignPlayer(chatId, text, bot, player);
        } else if (BotCommand.GENERATE_TOURS.matches(text)) {
            handleGenerateTours(chatId, text, bot, player);
        } else if (BotCommand.REGENERATE_TOURS.matches(text)) {
            handleRegenerateTours(chatId, text, bot, player);
        } else {
            bot.sendMessage(chatId, localizationService.msg(player, "admin.unknown.command"));
        }
    }

    public void handleCallback(Update update, TelegramBot bot) {
        String callbackData = update.getCallbackQuery().getData();
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        Player player = playerService.findByTelegramId(chatId).orElse(null);
        logger.info("Admin callback received: {}", callbackData);
        if ("ADMIN_MENU".equals(callbackData)) {
            showAdminMenu(chatId, bot, player);
        } else if ("ADMIN_HELP".equals(callbackData)) {
            handleAdminCommand(chatId, bot, player);
        } else if (callbackData.startsWith("ADMIN_CMD_")) {
            handleAdminCommandCallback(chatId, callbackData, bot, player);
        }
    }

    private void handleStartCommand(Long chatId, TelegramBot bot, Player player) {
        showAdminMenu(chatId, bot, player);
    }

    private void showAdminMenu(Long chatId, TelegramBot bot, Player player) {
        boolean isAlsoPlayer = player != null;
        StringBuilder message = new StringBuilder();
        message.append(localizationService.msg(player, "admin.panel.header"));
        SendMessage sendMessage = SendMessage.builder().chatId(chatId.toString()).text(message.toString()).replyMarkup(createAdminMenuKeyboard(isAlsoPlayer, player)).build();
        try {
            bot.execute(sendMessage);
        } catch (Exception e) {
            logger.error("Failed to send admin menu", e);
        }
    }

    private InlineKeyboardMarkup createAdminMenuKeyboard(boolean isAlsoPlayer, Player player) {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        InlineKeyboardButton listTournamentsBtn = InlineKeyboardButton.builder().text(localizationService.msg(player, "admin.menu.list_tournaments")).callbackData("ADMIN_CMD_LIST_TOURNAMENTS").build();
        InlineKeyboardButton listPlayersBtn = InlineKeyboardButton.builder().text(localizationService.msg(player, "admin.menu.list_players")).callbackData("ADMIN_CMD_LIST_PLAYERS").build();
        keyboard.add(List.of(listTournamentsBtn, listPlayersBtn));
        InlineKeyboardButton listDivisionsBtn = InlineKeyboardButton.builder().text(localizationService.msg(player, "admin.menu.list_divisions")).callbackData("ADMIN_CMD_LIST_DIVISIONS").build();
        InlineKeyboardButton createTournamentBtn = InlineKeyboardButton.builder().text(localizationService.msg(player, "admin.menu.create_tournament")).callbackData("ADMIN_CMD_CREATE_TOURNAMENT").build();
        keyboard.add(List.of(listDivisionsBtn, createTournamentBtn));
        InlineKeyboardButton addPlayerBtn = InlineKeyboardButton.builder().text(localizationService.msg(player, "admin.menu.add_player")).callbackData("ADMIN_CMD_ADD_PLAYER").build();
        InlineKeyboardButton assignPlayerBtn = InlineKeyboardButton.builder().text(localizationService.msg(player, "admin.menu.assign_player")).callbackData("ADMIN_CMD_ASSIGN_PLAYER").build();
        keyboard.add(List.of(addPlayerBtn, assignPlayerBtn));
        InlineKeyboardButton generateToursBtn = InlineKeyboardButton.builder().text(localizationService.msg(player, "admin.menu.generate_tours")).callbackData("ADMIN_CMD_GENERATE_TOURS").build();
        InlineKeyboardButton regenerateToursBtn = InlineKeyboardButton.builder().text(localizationService.msg(player, "admin.menu.regenerate_tours")).callbackData("ADMIN_CMD_REGENERATE_TOURS").build();
        keyboard.add(List.of(generateToursBtn, regenerateToursBtn));
        InlineKeyboardButton viewScheduleBtn = InlineKeyboardButton.builder().text(localizationService.msg(player, "admin.menu.view_schedule")).callbackData("ADMIN_CMD_VIEW_SCHEDULE").build();
        keyboard.add(List.of(viewScheduleBtn));
        InlineKeyboardButton helpBtn = InlineKeyboardButton.builder().text(localizationService.msg(player, "admin.menu.help")).callbackData("ADMIN_HELP").build();
        keyboard.add(List.of(helpBtn));
        if (isAlsoPlayer) {
            InlineKeyboardButton playerBtn = InlineKeyboardButton.builder().text(localizationService.msg(player, "admin.menu.player_schedule")).callbackData("PLAYER_SCHEDULE").build();
            keyboard.add(List.of(playerBtn));
        }
        return InlineKeyboardMarkup.builder().keyboard(keyboard).build();
    }

    private void handleAdminCommandCallback(Long chatId, String callbackData, TelegramBot bot, Player player) {
        switch (callbackData) {
            case "ADMIN_CMD_LIST_TOURNAMENTS":
                handleListTournaments(chatId, bot, player);
                break;
            case "ADMIN_CMD_LIST_PLAYERS":
                handleListPlayers(chatId, bot, player);
                break;
            case "ADMIN_CMD_LIST_DIVISIONS":
                handleListDivisions(chatId, bot, player);
                break;
            case "ADMIN_CMD_CREATE_TOURNAMENT":
                bot.sendMessage(chatId, localizationService.msg(player, "admin.cmd.create_tournament.usage", BotCommand.CREATE_TOURNAMENT.getCommand()));
                break;
            case "ADMIN_CMD_ADD_PLAYER":
                bot.sendMessage(chatId, localizationService.msg(player, "admin.cmd.add_player.usage", BotCommand.ADD_PLAYER.getCommand()));
                break;
            case "ADMIN_CMD_ASSIGN_PLAYER":
                bot.sendMessage(chatId, localizationService.msg(player, "admin.cmd.assign_player.usage", BotCommand.ASSIGN_PLAYER.getCommand()));
                break;
            case "ADMIN_CMD_GENERATE_TOURS":
                bot.sendMessage(chatId, localizationService.msg(player, "admin.cmd.generate_tours.usage", BotCommand.GENERATE_TOURS.getCommand()));
                break;
            case "ADMIN_CMD_REGENERATE_TOURS":
                bot.sendMessage(chatId, localizationService.msg(player, "admin.cmd.regenerate_tours.usage", BotCommand.REGENERATE_TOURS.getCommand()));
                break;
            case "ADMIN_CMD_VIEW_SCHEDULE":
                bot.sendMessage(chatId, localizationService.msg(player, "admin.cmd.view_schedule.usage", BotCommand.VIEW_SCHEDULE.getCommand()));
                break;
            default:
                bot.sendMessage(chatId, localizationService.msg(player, "admin.unknown.command"));
        }
    }

    private void handleAdminCommand(Long chatId, TelegramBot bot, Player player) {
        boolean isAlsoPlayer = player != null;
        StringBuilder helpMessage = new StringBuilder();
        helpMessage.append(localizationService.msg(player, "admin.help.header"));
        helpMessage.append(localizationService.msg(player, "admin.help.create_tournament.usage", BotCommand.CREATE_TOURNAMENT.getCommand())).append("\n");
        helpMessage.append(localizationService.msg(player, "admin.help.create_tournament.example"));
        helpMessage.append(localizationService.msg(player, "admin.help.add_player.usage", BotCommand.ADD_PLAYER.getCommand())).append("\n");
        helpMessage.append(localizationService.msg(player, "admin.help.add_player.example"));
        helpMessage.append(localizationService.msg(player, "admin.help.list_tournaments", BotCommand.LIST_TOURNAMENTS.getCommand())).append("\n");
        helpMessage.append(localizationService.msg(player, "admin.help.list_players", BotCommand.LIST_PLAYERS.getCommand())).append("\n");
        helpMessage.append(localizationService.msg(player, "admin.help.list_divisions", BotCommand.LIST_DIVISIONS.getCommand())).append("\n");
        helpMessage.append(localizationService.msg(player, "admin.help.assign_player", BotCommand.ASSIGN_PLAYER.getCommand())).append("\n");
        helpMessage.append(localizationService.msg(player, "admin.help.view_schedule", BotCommand.VIEW_SCHEDULE.getCommand())).append("\n");
        helpMessage.append(localizationService.msg(player, "admin.help.generate_tours", BotCommand.GENERATE_TOURS.getCommand())).append("\n");
        helpMessage.append(localizationService.msg(player, "admin.help.regenerate_tours", BotCommand.REGENERATE_TOURS.getCommand())).append("\n");
        if (isAlsoPlayer) {
            helpMessage.append(localizationService.msg(player, "admin.help.player.header"));
            helpMessage.append(localizationService.msg(player, "admin.help.player.schedule", BotCommand.SCHEDULE.getCommand())).append("\n");
            helpMessage.append(localizationService.msg(player, "admin.help.player.help", BotCommand.HELP.getCommand())).append("\n");
        }
        bot.sendMessage(chatId, helpMessage.toString());
    }

    private void handleCreateTournamentCommand(Long chatId, String text, TelegramBot bot, Player player) {
        try {
            String params = text.substring(BotCommand.CREATE_TOURNAMENT.getCommand().length()).trim();
            List<String> parsed = parseParameters(params);
            if (parsed.size() < 3) {
                bot.sendMessage(chatId, localizationService.msg(player, "admin.tournament.create.usage", BotCommand.CREATE_TOURNAMENT.getCommand()));
                return;
            }
            String name = parsed.get(0);
            String description = parsed.get(1);
            LocalDateTime startDate = LocalDateTime.parse(parsed.get(2) + "T00:00:00");
            Tournament tournament = tournamentService.createTournament(name, description, startDate);
            bot.sendMessage(chatId, localizationService.msg(player, "admin.tournament.created", tournament.getId(), tournament.getName()));
        } catch (Exception e) {
            logger.error("Error creating tournament", e);
            bot.sendMessage(chatId, localizationService.msg(player, "admin.tournament.create.failed", e.getMessage()));
        }
    }

    private void handleAddPlayerCommand(Long chatId, String text, TelegramBot bot, Player player) {
        try {
            String params = text.substring(BotCommand.ADD_PLAYER.getCommand().length()).trim();
            List<String> parsed = parseParameters(params);
            if (parsed.size() < 2) {
                bot.sendMessage(chatId, localizationService.msg(player, "admin.player.add.usage", BotCommand.ADD_PLAYER.getCommand()));
                return;
            }
            String username = parsed.get(0).replace("@", "");
            String name = parsed.get(1);
            if (playerService.isPlayerRegistered(username)) {
                bot.sendMessage(chatId, localizationService.msg(player, "admin.player.add.exists", username));
                return;
            }
            Player newPlayer = playerService.createPlayer(null, username, name);
            bot.sendMessage(chatId, localizationService.msg(player, "admin.player.add.success", newPlayer.getId(), newPlayer.getName(), newPlayer.getTelegramUsername()));
        } catch (Exception e) {
            logger.error("Error adding player", e);
            bot.sendMessage(chatId, localizationService.msg(player, "admin.player.add.failed", e.getMessage()));
        }
    }

    private void handleViewScheduleCommand(Long chatId, String text, TelegramBot bot, Player player) {
        try {
            String[] parts = text.split(" ");
            if (parts.length < 2) {
                bot.sendMessage(chatId, localizationService.msg(player, "admin.schedule.view.usage", BotCommand.VIEW_SCHEDULE.getCommand()));
                return;
            }
            Long divisionTournamentId = Long.parseLong(parts[1]);
            List<PlayerDivisionAssignment> assignments = divisionService.getPlayersByDivisionTournament(divisionTournamentId);
            if (assignments.isEmpty()) {
                bot.sendMessage(chatId, localizationService.msg(player, "admin.schedule.no.players"));
                return;
            }
            StringBuilder message = new StringBuilder();
            message.append(localizationService.msg(player, "admin.schedule.div.header", divisionTournamentId));
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM");
            for (PlayerDivisionAssignment assignment : assignments) {
                Player ap = assignment.getPlayer();
                message.append(localizationService.msg(player, "admin.schedule.player.line", ap.getName(), ap.getTelegramUsername())).append("\n");
                ScheduleService.PlayerSchedule ps = scheduleService.buildPlayerSchedule(ap);
                if (ps.tours().isEmpty()) {
                    message.append(localizationService.msg(player, "admin.schedule.no.tours")).append("\n");
                } else {
                    int tourNumber = 1;
                    for (ScheduleService.TourInfo ti : ps.tours()) {
                        message.append("  Tour ").append(tourNumber).append(" (").append(fmt.format(ti.startDate())).append("-").append(fmt.format(ti.endDate())).append(") - ");
                        if (ti.opponent() != null) {
                            message.append(ti.opponent().getName()).append(" [").append(ti.status()).append("]");
                        } else {
                            message.append(localizationService.msg(player, "admin.schedule.bye"));
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
            bot.sendMessage(chatId, localizationService.msg(player, "admin.schedule.view.failed", e.getMessage()));
        }
    }

    private void handleGenerateTours(Long chatId, String text, TelegramBot bot, Player player) {
        try {
            String[] parts = text.split(" ");
            if (parts.length < 4) {
                bot.sendMessage(chatId, localizationService.msg(player, "admin.tours.generate.usage", BotCommand.GENERATE_TOURS.getCommand()));
                return;
            }
            Long dtId = Long.parseLong(parts[1]);
            LocalDateTime start = LocalDateTime.parse(parts[2] + "T00:00:00");
            int interval = Integer.parseInt(parts[3]);
            int created = tourService.generateRoundRobinTours(dtId, start, interval);
            bot.sendMessage(chatId, localizationService.msg(player, "admin.tours.generate.success", created));
        } catch (Exception e) {
            logger.error("Error generating tours", e);
            bot.sendMessage(chatId, localizationService.msg(player, "admin.tours.generate.failed", e.getMessage()));
        }
    }

    private void handleRegenerateTours(Long chatId, String text, TelegramBot bot, Player player) {
        try {
            String[] parts = text.split(" ");
            if (parts.length < 2) {
                bot.sendMessage(chatId, localizationService.msg(player, "admin.tours.regenerate.usage", BotCommand.REGENERATE_TOURS.getCommand()));
                return;
            }
            Long dtId = Long.parseLong(parts[1]);
            int created = tourService.regenerateRoundRobinTours(dtId);
            bot.sendMessage(chatId, localizationService.msg(player, "admin.tours.regenerate.success", created));
        } catch (Exception e) {
            logger.error("Error regenerating tours", e);
            bot.sendMessage(chatId, localizationService.msg(player, "admin.tours.regenerate.failed", e.getMessage()));
        }
    }

    private void handleListTournaments(Long chatId, TelegramBot bot, Player player) {
        try {
            List<Tournament> tournaments = tournamentService.getAllTournaments();
            if (tournaments.isEmpty()) {
                bot.sendMessage(chatId, localizationService.msg(player, "admin.tournaments.none"));
                return;
            }
            StringBuilder message = new StringBuilder();
            message.append(localizationService.msg(player, "admin.tournaments.header"));
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy");
            for (Tournament t : tournaments) {
                String desc;
                if (t.getDescription() != null) {
                    desc = t.getDescription();
                } else {
                    desc = localizationService.msg(player, "generic.na");
                }
                String active;
                if (t.getIsActive()) {
                    active = localizationService.msg(player, "generic.yes");
                } else {
                    active = localizationService.msg(player, "generic.no");
                }
                message.append(localizationService.msg(player, "admin.tournaments.item", t.getId(), t.getName(), desc, t.getStartDate().format(fmt), active));
            }
            bot.sendMessage(chatId, message.toString());
        } catch (Exception e) {
            logger.error("Error listing tournaments", e);
            bot.sendMessage(chatId, localizationService.msg(player, "admin.tours.generate.failed", e.getMessage()));
        }
    }

    private void handleListPlayers(Long chatId, TelegramBot bot, Player player) {
        try {
            List<Player> players = playerService.getAllPlayers();
            if (players.isEmpty()) {
                bot.sendMessage(chatId, localizationService.msg(player, "admin.players.none"));
                return;
            }
            StringBuilder message = new StringBuilder();
            message.append(localizationService.msg(player, "admin.players.header"));
            for (Player p : players) {
                String tgId;
                if (p.getTelegramId() != null) {
                    tgId = p.getTelegramId().toString();
                } else {
                    tgId = localizationService.msg(player, "generic.not.linked");
                }
                String active;
                if (p.getIsActive()) {
                    active = localizationService.msg(player, "generic.yes");
                } else {
                    active = localizationService.msg(player, "generic.no");
                }
                message.append(localizationService.msg(player, "admin.players.item", p.getId(), p.getName(), p.getTelegramUsername(), tgId, active));
            }
            bot.sendMessage(chatId, message.toString());
        } catch (Exception e) {
            logger.error("Error listing players", e);
            bot.sendMessage(chatId, localizationService.msg(player, "admin.player.add.failed", e.getMessage()));
        }
    }

    private void handleListDivisions(Long chatId, TelegramBot bot, Player player) {
        try {
            List<Division> divisions = divisionService.getAllDivisions();
            if (divisions.isEmpty()) {
                bot.sendMessage(chatId, localizationService.msg(player, "admin.divisions.none"));
                return;
            }
            StringBuilder message = new StringBuilder();
            message.append(localizationService.msg(player, "admin.divisions.header"));
            for (Division d : divisions) {
                String level;
                if (d.getLevel() != null) {
                    level = d.getLevel().toString();
                } else {
                    level = localizationService.msg(player, "generic.na");
                }
                String active;
                if (d.getIsActive()) {
                    active = localizationService.msg(player, "generic.yes");
                } else {
                    active = localizationService.msg(player, "generic.no");
                }
                message.append(localizationService.msg(player, "admin.divisions.item", d.getId(), d.getName(), level, active));
            }
            bot.sendMessage(chatId, message.toString());
        } catch (Exception e) {
            logger.error("Error listing divisions", e);
            bot.sendMessage(chatId, localizationService.msg(player, "admin.divisions.none", e.getMessage()));
        }
    }

    private void handleAssignPlayer(Long chatId, String text, TelegramBot bot, Player player) {
        try {
            String[] parts = text.split(" ");
            if (parts.length < 3) {
                bot.sendMessage(chatId, localizationService.msg(player, "admin.assign.usage", BotCommand.ASSIGN_PLAYER.getCommand()));
                return;
            }
            Long playerId = Long.parseLong(parts[1]);
            Long divisionTournamentId = Long.parseLong(parts[2]);
            Player targetPlayer = playerService.findById(playerId).orElse(null);
            if (targetPlayer == null) {
                bot.sendMessage(chatId, localizationService.msg(player, "admin.assign.player.notfound", playerId));
                return;
            }
            DivisionTournament divisionTournament = divisionService.findDivisionTournamentById(divisionTournamentId).orElse(null);
            if (divisionTournament == null) {
                bot.sendMessage(chatId, localizationService.msg(player, "admin.assign.divTournament.notfound", divisionTournamentId));
                return;
            }
            PlayerDivisionAssignment assignment = divisionService.assignPlayerToDivision(targetPlayer, divisionTournament);
            bot.sendMessage(chatId, localizationService.msg(player, "admin.assign.success", targetPlayer.getName(), divisionTournament.getId()));
        } catch (Exception e) {
            logger.error("Error assigning player", e);
            bot.sendMessage(chatId, localizationService.msg(player, "admin.assign.failed", e.getMessage()));
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
