package com.github.jkaste03.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.github.jkaste03.service.SseService;

@Controller
public class DashboardController {

    private final SseService sseService;

    public DashboardController(SseService sseService) {
        this.sseService = sseService;
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/api/v1/dashboard/events")
    public SseEmitter subscribeToDashboardEvents() {
        SseEmitter emitter = new SseEmitter(0L); // Infinite timeout
        sseService.addDashboardEmitter(emitter);
        return emitter;
    }
}
