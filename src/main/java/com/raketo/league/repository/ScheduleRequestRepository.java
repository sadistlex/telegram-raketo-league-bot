package com.raketo.league.repository;

import com.raketo.league.model.ScheduleRequest;
import com.raketo.league.model.ScheduleRequest.ScheduleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ScheduleRequestRepository extends JpaRepository<ScheduleRequest, Long> {
    List<ScheduleRequest> findByTourId(Long tourId);
    List<ScheduleRequest> findByTourIdAndStatus(Long tourId, ScheduleStatus status);
    List<ScheduleRequest> findByInitiatorPlayerId(Long initiatorPlayerId);
    List<ScheduleRequest> findByRecipientPlayerId(Long recipientPlayerId);
    List<ScheduleRequest> findByProposedTimeBetween(LocalDateTime start, LocalDateTime end);
}
