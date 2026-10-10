package com.github.jkaste03.controller.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.github.jkaste03.security.JwtService;
import com.github.jkaste03.security.UserPrincipal;

@Controller
public class DeviceWebController {

    private final JwtService jwtService;

    public DeviceWebController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @GetMapping("/devices/{id}/edit")
    public String editDevicePage(
            @PathVariable long id,
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(required = false) String logout,
            Model model) {

        if (user != null && logout == null) {
            model.addAttribute("accessToken", jwtService.createAccessToken(user));
        }
        model.addAttribute("deviceId", id);
        return "edit-device";
    }
}
