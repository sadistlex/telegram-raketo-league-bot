package com.raketo.league.service;

import com.raketo.league.model.AvailabilitySlot;
import com.raketo.league.repository.AvailabilitySlotRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private static final Logger logger = LoggerFactory.getLogger(AvailabilityService.class);

    private final AvailabilitySlotRepository availabilitySlotRepository;

    @Transactional(readOnly = true)
    public List<AvailabilitySlot> getPlayerAvailability(Long playerId) {
        return availabilitySlotRepository.findByPlayerId(playerId);
    }

    @Transactional(readOnly = true)
    public List<AvailabilitySlot> getPlayerAvailabilityInRange(Long playerId, LocalDateTime start, LocalDateTime end) {
        return availabilitySlotRepository.findByPlayerIdAndDateRange(playerId, start, end);
    }

    @Transactional(readOnly = true)
    public List<AvailabilitySlot> getMatchAvailability(Long matchId) {
        return availabilitySlotRepository.findByMatchId(matchId);
    }

    @Transactional
    public AvailabilitySlot saveAvailabilitySlot(AvailabilitySlot slot) {
        return availabilitySlotRepository.save(slot);
    }

    @Transactional
    public void deleteAvailabilitySlot(Long slotId) {
        availabilitySlotRepository.deleteById(slotId);
    }
}

