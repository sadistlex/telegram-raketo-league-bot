package com.raketo.league.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/webapp")
public class WebAppController {

    @GetMapping("/calendar")
    public String calendar() {
        return "calendar";
    }
}

