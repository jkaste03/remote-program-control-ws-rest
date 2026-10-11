package com.github.jkaste03.controller.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.github.jkaste03.security.UserPrincipal;
import com.github.jkaste03.security.JwtService;
import com.github.jkaste03.service.SseService;

@Controller
public class DashboardController {

    private final SseService sseService;
    private final JwtService jwtService;

    public DashboardController(SseService sseService, JwtService jwtService) {
        this.sseService = sseService;
        this.jwtService = jwtService;
    }

    @GetMapping({"/", "/dashboard"})
    public String index(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(required = false) String logout,
            Model model) {
        if (user != null && logout == null) {
            model.addAttribute("accessToken", jwtService.createAccessToken(user));
        }
        return "index";
    }

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal UserPrincipal user, Model model) {
        model.addAttribute("accessToken", jwtService.createAccessToken(user));
        return "profile";
    }

    @GetMapping("/api/v1/dashboard/events")
    public SseEmitter subscribeToDashboardEvents() {
        SseEmitter emitter = new SseEmitter(0L); // Infinite timeout
        sseService.addDashboardEmitter(emitter);
        return emitter;
    }
}
