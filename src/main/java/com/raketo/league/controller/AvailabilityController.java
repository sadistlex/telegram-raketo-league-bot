package com.raketo.league.controller;

import com.raketo.league.model.AvailabilitySlot;
import com.raketo.league.service.AvailabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/availability")
@RequiredArgsConstructor
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    @GetMapping("/player/{playerId}")
    public ResponseEntity<List<AvailabilitySlot>> getPlayerAvailability(@PathVariable Long playerId) {
        return ResponseEntity.ok(availabilityService.getPlayerAvailability(playerId));
    }

    @GetMapping("/match/{matchId}")
    public ResponseEntity<List<AvailabilitySlot>> getMatchAvailability(@PathVariable Long matchId) {
        return ResponseEntity.ok(availabilityService.getMatchAvailability(matchId));
    }

    @PostMapping
    public ResponseEntity<AvailabilitySlot> saveAvailability(@RequestBody AvailabilitySlot slot) {
        return ResponseEntity.ok(availabilityService.saveAvailabilitySlot(slot));
    }

    @DeleteMapping("/{slotId}")
    public ResponseEntity<Void> deleteAvailability(@PathVariable Long slotId) {
        availabilityService.deleteAvailabilitySlot(slotId);
        return ResponseEntity.ok().build();
    }
}

