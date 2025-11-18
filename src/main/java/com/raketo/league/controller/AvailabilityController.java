package com.raketo.league.controller;

import com.raketo.league.model.AvailabilitySlot;
import com.raketo.league.service.AvailabilityService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/availability")
@RequiredArgsConstructor
public class AvailabilityController {
    private final AvailabilityService availabilityService;

    @GetMapping("/player/{playerId}")
    public ResponseEntity<List<AvailabilitySlot>> getPlayerAvailability(@PathVariable Long playerId) {
        return ResponseEntity.ok(availabilityService.getPlayerAvailability(playerId));
    }

    @GetMapping("/tour/{tourId}/players/{playerAId}/{playerBId}/intersections")
    public ResponseEntity<Map<String, Object>> getTourIntersections(@PathVariable Long tourId, @PathVariable Long playerAId, @PathVariable Long playerBId) {
        return ResponseEntity.ok(availabilityService.getTourIntersections(tourId, playerAId, playerBId));
    }

    @GetMapping("/tour/{tourId}/player/{playerId}")
    public ResponseEntity<AvailabilitySlot> getPlayerTourAvailability(@PathVariable Long tourId, @PathVariable Long playerId) {
        return ResponseEntity.ok(availabilityService.getPlayerTourAvailability(playerId, tourId).orElse(null));
    }

    @PostMapping("/tour/{tourId}/player/{playerId}")
    public ResponseEntity<AvailabilitySlot> savePlayerTourAvailability(@PathVariable Long tourId, @PathVariable Long playerId, @RequestBody AvailabilityPayload payload) {
        AvailabilitySlot slot = availabilityService.saveOrUpdatePlayerTourAvailability(tourId, playerId, payload.getAvailableSlots(), payload.getUnavailableSlots());
        return ResponseEntity.ok(slot);
    }

    @DeleteMapping("/tour/{tourId}/player/{playerId}")
    public ResponseEntity<Void> deletePlayerTourAvailability(@PathVariable Long tourId, @PathVariable Long playerId) {
        availabilityService.deletePlayerTourAvailability(tourId, playerId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/compatible/{tourId}/{playerId}/{opponentId}")
    public ResponseEntity<Map<String, Object>> getCompatibleTimes(@PathVariable Long tourId, @PathVariable Long playerId, @PathVariable Long opponentId) {
        return ResponseEntity.ok(availabilityService.getCompatibleTimes(tourId, playerId, opponentId));
    }

    @Data
    public static class AvailabilityPayload {
        private String availableSlots;
        private String unavailableSlots;
    }
}
