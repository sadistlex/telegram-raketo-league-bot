package com.raketo.league.controller;

import com.raketo.league.model.Player;
import com.raketo.league.model.Tour;
import com.raketo.league.service.PlayerService;
import com.raketo.league.repository.TourRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/webapp")
@RequiredArgsConstructor
public class WebAppController {
    private final PlayerService playerService;
    private final TourRepository tourRepository;

    @GetMapping("/calendar")
    public String calendar(@RequestParam(required = false) Long playerId,
                           @RequestParam(required = false) Long tourId,
                           Model model) {
        if (playerId != null) {
            Player player = playerService.findByTelegramId(playerId)
                    .orElseThrow(() -> new IllegalArgumentException("Player not found"));
            model.addAttribute("playerId", player.getId());
            model.addAttribute("playerName", player.getName());
        }
        if (tourId != null) {
            Tour tour = tourRepository.findById(tourId)
                    .orElseThrow(() -> new IllegalArgumentException("Tour not found"));
            model.addAttribute("tourId", tour.getId());
            model.addAttribute("tourStartDate", tour.getTourTemplate().getStartDate());
            model.addAttribute("tourEndDate", tour.getTourTemplate().getEndDate());
        }
        return "calendar";
    }
}
