package com.github.jkaste03.controller.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.github.jkaste03.security.UserPrincipal;
import com.github.jkaste03.service.SseService;

@Controller
public class DashboardController {

    private final SseService sseService;

    public DashboardController(SseService sseService) {
        this.sseService = sseService;
    }

    @GetMapping("/")
    public String index(
            @AuthenticationPrincipal UserPrincipal user,
            @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient authorizedClient,
            Model model) {
        if (user != null && authorizedClient != null) {
            model.addAttribute("accessToken", authorizedClient.getAccessToken().getTokenValue());
        }
        return "index";
    }

    @GetMapping("/api/v1/dashboard/events")
    public SseEmitter subscribeToDashboardEvents() {
        SseEmitter emitter = new SseEmitter(0L); // Infinite timeout
        sseService.addDashboardEmitter(emitter);
        return emitter;
    }
}
