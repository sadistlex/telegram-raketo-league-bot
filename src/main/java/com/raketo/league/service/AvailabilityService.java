package com.raketo.league.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.raketo.league.model.AvailabilitySlot;
import com.raketo.league.model.Player;
import com.raketo.league.model.Tour;
import com.raketo.league.repository.AvailabilitySlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AvailabilityService {
    private final AvailabilitySlotRepository availabilitySlotRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    @Transactional(readOnly = true)
    public List<AvailabilitySlot> getPlayerAvailability(Long playerId) {
        return availabilitySlotRepository.findByPlayerId(playerId);
    }

    @Transactional(readOnly = true)
    public Optional<AvailabilitySlot> getPlayerTourAvailability(Long playerId, Long tourId) {
        List<AvailabilitySlot> list = availabilitySlotRepository.findByPlayerIdAndTourId(playerId, tourId);
        return list.stream().findFirst();
    }

    @Transactional
    public AvailabilitySlot saveOrUpdatePlayerTourAvailability(Long tourId, Long playerId, String availableJson, String unavailableJson) {
        AvailabilitySlot existing = getPlayerTourAvailability(playerId, tourId).orElse(null);
        if (existing != null) {
            existing.setAvailableSlots(availableJson);
            existing.setUnavailableSlots(unavailableJson);
            existing.setUpdatedAt(LocalDateTime.now());
            return availabilitySlotRepository.save(existing);
        }
        AvailabilitySlot slot = AvailabilitySlot.builder()
                .tour(Tour.builder().id(tourId).build())
                .player(Player.builder().id(playerId).build())
                .availableSlots(availableJson)
                .unavailableSlots(unavailableJson)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return availabilitySlotRepository.save(slot);
    }

    @Transactional
    public void deletePlayerTourAvailability(Long tourId, Long playerId) {
        availabilitySlotRepository.findByPlayerIdAndTourId(playerId, tourId).forEach(availabilitySlotRepository::delete);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getTourIntersections(Long tourId, Long playerAId, Long playerBId) {
        AvailabilitySlot a = availabilitySlotRepository.findByPlayerIdAndTourId(playerAId, tourId).stream().findFirst().orElse(null);
        AvailabilitySlot b = availabilitySlotRepository.findByPlayerIdAndTourId(playerBId, tourId).stream().findFirst().orElse(null);

        Map<String, Object> result = new HashMap<>();
        result.put("tourId", tourId);
        result.put("playerAId", playerAId);
        result.put("playerBId", playerBId);
        result.put("playerAHasSubmitted", a != null);
        result.put("playerBHasSubmitted", b != null);

        List<TimeIntersection> greenIntersections = List.of();
        List<TimeIntersection> yellowIntersections = List.of();

        if (a != null && b != null) {
            greenIntersections = computeGreenIntersections(
                a.getAvailableSlots(), a.getUnavailableSlots(),
                b.getAvailableSlots(), b.getUnavailableSlots()
            );

            if (greenIntersections.isEmpty()) {
                yellowIntersections = computeYellowIntersections(
                    a.getAvailableSlots(), a.getUnavailableSlots(),
                    b.getAvailableSlots(), b.getUnavailableSlots(),
                    tourId
                );
            }
        }

        result.put("greenIntersections", greenIntersections);
        result.put("yellowIntersections", yellowIntersections);
        result.put("hasGreenMatches", !greenIntersections.isEmpty());
        result.put("hasYellowMatches", !yellowIntersections.isEmpty());

        return result;
    }

    private List<TimeIntersection> computeGreenIntersections(String availA, String unavailA, String availB, String unavailB) {
        Map<String, List<Integer>> greenA = parse(availA);
        Map<String, List<Integer>> greenB = parse(availB);

        Set<String> allDays = new HashSet<>();
        allDays.addAll(greenA.keySet());
        allDays.addAll(greenB.keySet());

        List<TimeIntersection> list = new ArrayList<>();
        for (String day : allDays) {
            List<Integer> ha = greenA.getOrDefault(day, List.of());
            List<Integer> hb = greenB.getOrDefault(day, List.of());
            Set<Integer> overlap = new TreeSet<>(ha);
            overlap.retainAll(hb);

            if (!overlap.isEmpty()) {
                for (Integer hour : overlap) {
                    LocalDateTime start = LocalDateTime.parse(day + " " + String.format("%02d:00:00", hour), DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));
                    LocalDateTime end = start.plusHours(1);
                    list.add(new TimeIntersection(start, end, "green"));
                }
            }
        }
        return merge(list);
    }

    private List<TimeIntersection> computeYellowIntersections(String availA, String unavailA, String availB, String unavailB, Long tourId) {
        Map<String, List<Integer>> greenA = parse(availA);
        Map<String, List<Integer>> redA = parse(unavailA);
        Map<String, List<Integer>> greenB = parse(availB);
        Map<String, List<Integer>> redB = parse(unavailB);

        Set<String> allPossibleDays = new HashSet<>();
        allPossibleDays.addAll(greenA.keySet());
        allPossibleDays.addAll(greenB.keySet());
        allPossibleDays.addAll(redA.keySet());
        allPossibleDays.addAll(redB.keySet());

        List<TimeIntersection> list = new ArrayList<>();

        for (String day : allPossibleDays) {
            List<Integer> greenHoursA = greenA.getOrDefault(day, List.of());
            List<Integer> redHoursA = redA.getOrDefault(day, List.of());
            List<Integer> greenHoursB = greenB.getOrDefault(day, List.of());
            List<Integer> redHoursB = redB.getOrDefault(day, List.of());

            for (int hour = 0; hour < 24; hour++) {
                boolean aRed = redHoursA.contains(hour);
                boolean bRed = redHoursB.contains(hour);
                boolean aGreen = greenHoursA.contains(hour);
                boolean bGreen = greenHoursB.contains(hour);

                if (!aRed && !bRed) {
                    boolean isYellow = (!aGreen || !bGreen);

                    if (isYellow) {
                        LocalDateTime start = LocalDateTime.parse(day + " " + String.format("%02d:00:00", hour), DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));
                        LocalDateTime end = start.plusHours(1);
                        list.add(new TimeIntersection(start, end, "yellow"));
                    }
                }
            }
        }

        return merge(list);
    }

    private Map<String, List<Integer>> parse(String json) {
        if (json == null || json.isBlank()) return Collections.emptyMap();
        try { return objectMapper.readValue(json, new TypeReference<Map<String, List<Integer>>>() {}); } catch (Exception e) { return Collections.emptyMap(); }
    }

    private List<TimeIntersection> merge(List<TimeIntersection> raw) {
        if (raw.isEmpty()) return raw;
        List<TimeIntersection> sorted = raw.stream().sorted(Comparator.comparing(TimeIntersection::start)).collect(Collectors.toList());
        List<TimeIntersection> merged = new ArrayList<>();
        TimeIntersection current = sorted.get(0);
        for (int i = 1; i < sorted.size(); i++) {
            TimeIntersection next = sorted.get(i);
            if (!current.end.isBefore(next.start) && current.type.equals(next.type)) {
                current = new TimeIntersection(current.start, current.end.isAfter(next.end) ? current.end : next.end, current.type);
            } else {
                merged.add(current);
                current = next;
            }
        }
        merged.add(current);
        return merged;
    }

    public record TimeIntersection(LocalDateTime start, LocalDateTime end, String type) {}
}
