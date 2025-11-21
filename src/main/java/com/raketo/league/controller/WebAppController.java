package com.raketo.league.controller;

import com.raketo.league.model.Language;
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
import org.springframework.web.servlet.support.RequestContextUtils;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;

@Controller
@RequestMapping("/webapp")
@RequiredArgsConstructor
public class WebAppController {
    private final PlayerService playerService;
    private final TourRepository tourRepository;

    @GetMapping("/calendar")
    public String calendar(@RequestParam(required = false) Long playerId,
                           @RequestParam(required = false) Long tourId,
                           HttpServletRequest request,
                           Model model) {
        if (playerId != null) {
            Player player = playerService.findByTelegramId(playerId)
                    .orElseThrow(() -> new IllegalArgumentException("Player not found"));
            model.addAttribute("playerId", player.getId());
            model.addAttribute("playerName", player.getName());

            Locale locale = player.getLanguage() == Language.EN ? Locale.ENGLISH : new Locale("ru");
            RequestContextUtils.getLocaleResolver(request).setLocale(request, null, locale);
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

    @GetMapping("/compatible")
    public String compatibleTimes(@RequestParam Long playerId,
                                   @RequestParam Long opponentId,
                                   @RequestParam Long tourId,
                                   HttpServletRequest request,
                                   Model model) {
        System.out.println("Compatible endpoint called with playerId=" + playerId + ", opponentId=" + opponentId + ", tourId=" + tourId);

        Player player = playerService.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Player not found"));
        Player opponent = playerService.findById(opponentId)
                .orElseThrow(() -> new IllegalArgumentException("Opponent not found"));
        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new IllegalArgumentException("Tour not found"));

        model.addAttribute("playerId", player.getId());
        model.addAttribute("playerName", player.getName());
        model.addAttribute("opponentId", opponent.getId());
        model.addAttribute("opponentName", opponent.getName());
        model.addAttribute("tourId", tour.getId());
        model.addAttribute("tourStartDate", tour.getTourTemplate().getStartDate());
        model.addAttribute("tourEndDate", tour.getTourTemplate().getEndDate());

        Locale locale = player.getLanguage() == Language.EN ? Locale.ENGLISH : new Locale("ru");
        RequestContextUtils.getLocaleResolver(request).setLocale(request, null, locale);

        System.out.println("Compatible model attributes: " + model.asMap());
        return "compatible";
    }
}
