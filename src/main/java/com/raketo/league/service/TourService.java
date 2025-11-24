package com.raketo.league.service;

import com.raketo.league.model.*;
import com.raketo.league.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TourService {
    private static final Logger logger = LoggerFactory.getLogger(TourService.class);
    private final TourTemplateRepository tourTemplateRepository;
    private final TourRepository tourRepository;
    private final TourPlayerRepository tourPlayerRepository;
    private final PlayerDivisionAssignmentRepository playerDivisionAssignmentRepository;
    private final DivisionTournamentRepository divisionTournamentRepository;
    private final AvailabilitySlotRepository availabilitySlotRepository;
    private final ScheduleRequestRepository scheduleRequestRepository;

    @Transactional
    public int generateRoundRobinTours(Long divisionTournamentId, LocalDateTime tournamentStartDate, int tourDurationDays) {
        logger.info("Generating round-robin tours divisionTournamentId={}, startDate={}, durationDays={}", divisionTournamentId, tournamentStartDate, tourDurationDays);
        return createOrRecreateRoundRobin(divisionTournamentId, tournamentStartDate, tourDurationDays, false);
    }

    @Transactional
    public int regenerateRoundRobinTours(Long divisionTournamentId) {
        logger.info("Regenerating round-robin tours divisionTournamentId={}", divisionTournamentId);
        return createOrRecreateRoundRobin(divisionTournamentId, null, null, true);
    }

    private int createOrRecreateRoundRobin(Long divisionTournamentId, LocalDateTime startDateArg, Integer tourDurationDaysArg, boolean preserveAvailability) {
        DivisionTournament divisionTournament = loadTournament(divisionTournamentId);
        List<Player> players = loadPlayers(divisionTournamentId);
        int numberOfTours = computeNumberOfTours(players.size());
        List<TourTemplate> existingTemplates = tourTemplateRepository.findByDivisionTournamentId(divisionTournamentId);
        GenerationParams params = prepareGenerationParams(preserveAvailability, existingTemplates, startDateArg, tourDurationDaysArg, divisionTournamentId);
        if (!existingTemplates.isEmpty()) {
            deleteExistingCascade(existingTemplates, params.oldTours);
        }
        List<TourTemplate> newTemplates = buildTemplates(divisionTournament, params.startDate, params.durationDays, numberOfTours);
        List<List<PlayerPair>> schedule = generateRoundRobinSchedule(players);
        if (schedule.size() != newTemplates.size()) {
            throw new IllegalStateException("Mismatch templates=" + newTemplates.size() + " rounds=" + schedule.size());
        }
        Map<String, Long> tourIdByKey = persistTours(newTemplates, schedule);
        int preserved = 0;
        if (preserveAvailability) {
            preserved = preserveAvailability(params.oldAvailabilities, params.oldTours, newTemplates, schedule, tourIdByKey, players);
        }
        logger.info("Round-robin created templates={} tours={} divisionTournamentId={} preservedAvailability={}", numberOfTours, tourIdByKey.size(), divisionTournamentId, preserved);
        return tourIdByKey.size();
    }

    private GenerationParams prepareGenerationParams(boolean preserveAvailability, List<TourTemplate> existingTemplates, LocalDateTime startDateArg, Integer tourDurationDaysArg, Long divisionTournamentId) {
        if (preserveAvailability) {
            return preparePreserveParams(existingTemplates);
        }
        return prepareNewParams(existingTemplates, startDateArg, tourDurationDaysArg, divisionTournamentId);
    }

    private GenerationParams preparePreserveParams(List<TourTemplate> existingTemplates) {
        if (existingTemplates.isEmpty()) {
            throw new IllegalArgumentException("No existing tour templates found. Use /gentours first.");
        }
        LocalDateTime startDate = existingTemplates.get(0).getStartDate();
        int durationDays = (int) Duration.between(existingTemplates.get(0).getStartDate(), existingTemplates.get(0).getEndDate()).toDays();
        List<Tour> oldTours = collectOldTours(existingTemplates);
        Map<Long, List<AvailabilitySlot>> oldAvailabilities = collectOldAvailability(oldTours);
        return new GenerationParams(startDate, durationDays, oldAvailabilities, oldTours);
    }

    private GenerationParams prepareNewParams(List<TourTemplate> existingTemplates, LocalDateTime startDateArg, Integer tourDurationDaysArg, Long divisionTournamentId) {
        if (startDateArg == null) {
            throw new IllegalArgumentException("Start date is required");
        }
        if (tourDurationDaysArg == null || tourDurationDaysArg <= 0) {
            throw new IllegalArgumentException("tourDurationDays must be > 0");
        }
        if (!existingTemplates.isEmpty()) {
            logger.warn("Existing templates found divisionTournamentId={}, deleting before creation", divisionTournamentId);
        }
        List<Tour> oldTours = collectOldTours(existingTemplates);
        return new GenerationParams(startDateArg, tourDurationDaysArg, Collections.emptyMap(), oldTours);
    }

    private List<Tour> collectOldTours(List<TourTemplate> templates) {
        if (templates.isEmpty()) {
            return Collections.emptyList();
        }
        return templates.stream().flatMap(t -> tourRepository.findByTourTemplateId(t.getId()).stream()).toList();
    }

    private Map<Long, List<AvailabilitySlot>> collectOldAvailability(List<Tour> oldTours) {
        Map<Long, List<AvailabilitySlot>> map = new HashMap<>();
        for (Tour tour : oldTours) {
            List<AvailabilitySlot> slots = availabilitySlotRepository.findByTourId(tour.getId());
            if (!slots.isEmpty()) {
                map.put(tour.getId(), slots);
            }
        }
        return map;
    }

    private DivisionTournament loadTournament(Long id) {
        return divisionTournamentRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("DivisionTournament not found id=" + id));
    }

    private List<Player> loadPlayers(Long divisionTournamentId) {
        List<PlayerDivisionAssignment> assignments = playerDivisionAssignmentRepository.findByDivisionTournamentId(divisionTournamentId);
        if (assignments.size() < 2) {
            throw new IllegalArgumentException("Need at least 2 players to generate tours");
        }
        return assignments.stream().map(PlayerDivisionAssignment::getPlayer).toList();
    }

    private int computeNumberOfTours(int playerCount) {
        return playerCount % 2 == 0 ? playerCount - 1 : playerCount;
    }

    private void deleteExistingCascade(List<TourTemplate> templates, List<Tour> tours) {
        for (Tour tour : tours) {
            scheduleRequestRepository.deleteAll(scheduleRequestRepository.findByTourId(tour.getId()));
            tourPlayerRepository.deleteAll(tourPlayerRepository.findByTourId(tour.getId()));
        }
        tourRepository.deleteAll(tours);
        tourTemplateRepository.deleteAll(templates);
    }

    private List<TourTemplate> buildTemplates(DivisionTournament divisionTournament, LocalDateTime startDate, int durationDays, int count) {
        List<TourTemplate> list = new ArrayList<>();
        LocalDateTime current = startDate;
        for (int i = 0; i < count; i++) {
            LocalDateTime end = current.plusDays(durationDays);
            TourTemplate template = TourTemplate.builder().divisionTournament(divisionTournament).startDate(current).endDate(end).build();
            list.add(template);
            current = end;
        }
        tourTemplateRepository.saveAll(list);
        return list;
    }

    private Map<String, Long> persistTours(List<TourTemplate> templates, List<List<PlayerPair>> schedule) {
        Map<String, Long> map = new HashMap<>();
        Map<Long, Integer> responsibilityCount = new HashMap<>();

        for (int i = 0; i < schedule.size(); i++) {
            TourTemplate template = templates.get(i);
            for (PlayerPair pair : schedule.get(i)) {
                Player responsible = selectResponsiblePlayer(pair.player1, pair.player2, responsibilityCount);

                Tour tour = Tour.builder()
                        .tourTemplate(template)
                        .status(Tour.TourStatus.Active)
                        .responsiblePlayer(responsible)
                        .updatedAt(LocalDateTime.now())
                        .build();
                Tour saved = tourRepository.save(tour);
                tourPlayerRepository.save(TourPlayer.builder().tour(saved).player(pair.player1).build());
                tourPlayerRepository.save(TourPlayer.builder().tour(saved).player(pair.player2).build());
                map.put(buildKey(template.getId(), pair.player1.getId(), pair.player2.getId()), saved.getId());
            }
        }
        return map;
    }

    private Player selectResponsiblePlayer(Player p1, Player p2, Map<Long, Integer> responsibilityCount) {
        int p1Count = responsibilityCount.getOrDefault(p1.getId(), 0);
        int p2Count = responsibilityCount.getOrDefault(p2.getId(), 0);

        Player responsible;
        if (p1Count < p2Count) {
            responsible = p1;
        } else if (p2Count < p1Count) {
            responsible = p2;
        } else {
            responsible = new Random().nextBoolean() ? p1 : p2;
        }

        responsibilityCount.put(responsible.getId(), responsibilityCount.getOrDefault(responsible.getId(), 0) + 1);
        return responsible;
    }

    private int preserveAvailability(Map<Long, List<AvailabilitySlot>> oldAvailabilities, List<Tour> oldTours, List<TourTemplate> newTemplates, List<List<PlayerPair>> schedule, Map<String, Long> tourIdByKey, List<Player> newPlayers) {
        if (oldAvailabilities.isEmpty()) {
            return 0;
        }
        Set<Long> newPlayerIds = newPlayers.stream().map(Player::getId).collect(Collectors.toSet());
        Map<String, Integer> templateIndexByDateRange = new HashMap<>();
        for (int i = 0; i < newTemplates.size(); i++) {
            TourTemplate t = newTemplates.get(i);
            templateIndexByDateRange.put(t.getStartDate() + "_" + t.getEndDate(), i);
        }
        Map<Long, Tour> oldTourById = oldTours.stream().collect(Collectors.toMap(Tour::getId, t -> t));
        int preserved = 0;
        for (Map.Entry<Long, List<AvailabilitySlot>> entry : oldAvailabilities.entrySet()) {
            Tour oldTour = oldTourById.get(entry.getKey());
            if (oldTour == null) {
                continue;
            }
            TourTemplate oldTemplate = oldTour.getTourTemplate();
            Integer index = templateIndexByDateRange.get(oldTemplate.getStartDate() + "_" + oldTemplate.getEndDate());
            if (index == null) {
                for (AvailabilitySlot slot : entry.getValue()) {
                    logger.info("Dropping availability playerId={} reason=template_mismatch", slot.getPlayer().getId());
                }
                continue;
            }
            List<PlayerPair> pairs = schedule.get(index);
            for (AvailabilitySlot oldSlot : entry.getValue()) {
                Long playerId = oldSlot.getPlayer().getId();
                if (!newPlayerIds.contains(playerId)) {
                    logger.info("Dropping availability playerId={} reason=player_removed", playerId);
                    continue;
                }
                boolean saved = false;
                for (PlayerPair pair : pairs) {
                    if (pair.player1.getId().equals(playerId) || pair.player2.getId().equals(playerId)) {
                        Long newTourId = tourIdByKey.get(buildKey(newTemplates.get(index).getId(), pair.player1.getId(), pair.player2.getId()));
                        if (newTourId != null) {
                            AvailabilitySlot newSlot = AvailabilitySlot.builder().tour(Tour.builder().id(newTourId).build()).player(oldSlot.getPlayer()).availableSlots(oldSlot.getAvailableSlots()).unavailableSlots(oldSlot.getUnavailableSlots()).createdAt(oldSlot.getCreatedAt()).updatedAt(LocalDateTime.now()).build();
                            availabilitySlotRepository.save(newSlot);
                            preserved++;
                            saved = true;
                        }
                        break;
                    }
                }
                if (!saved) {
                    logger.info("Dropping availability playerId={} reason=no_matching_pair", playerId);
                }
            }
        }
        return preserved;
    }

    private String buildKey(Long templateId, Long p1, Long p2) {
        return templateId + "_" + p1 + "_" + p2;
    }

    private List<List<PlayerPair>> generateRoundRobinSchedule(List<Player> players) {
        int n = players.size();
        boolean isOdd = n % 2 == 1;
        List<Player> list = new ArrayList<>(players);
        if (isOdd) {
            list.add(null);
            n++;
        }
        List<List<PlayerPair>> schedule = new ArrayList<>();
        for (int round = 0; round < n - 1; round++) {
            List<PlayerPair> roundPairs = new ArrayList<>();
            for (int i = 0; i < n / 2; i++) {
                int home = i;
                int away = n - 1 - i;
                Player p1 = list.get(home);
                Player p2 = list.get(away);
                if (p1 != null && p2 != null) {
                    roundPairs.add(new PlayerPair(p1, p2));
                }
            }
            schedule.add(roundPairs);
            Player fixed = list.get(0);
            list.remove(0);
            Collections.rotate(list, 1);
            list.add(0, fixed);
        }
        return schedule;
    }

    private static class PlayerPair {
        Player player1;
        Player player2;
        PlayerPair(Player player1, Player player2) { this.player1 = player1; this.player2 = player2; }
    }

    private static class GenerationParams {
        final LocalDateTime startDate;
        final int durationDays;
        final Map<Long, List<AvailabilitySlot>> oldAvailabilities;
        final List<Tour> oldTours;
        GenerationParams(LocalDateTime startDate, int durationDays, Map<Long, List<AvailabilitySlot>> oldAvailabilities, List<Tour> oldTours) {
            this.startDate = startDate;
            this.durationDays = durationDays;
            this.oldAvailabilities = oldAvailabilities;
            this.oldTours = oldTours;
        }
    }
}
