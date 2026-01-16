package com.github.jkaste03.controller.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.github.jkaste03.security.UserPrincipal;

@Controller
public class DeviceWebController {

    @GetMapping("/devices/{id}/edit")
    public String editDevicePage(
            @PathVariable long id,
            @AuthenticationPrincipal UserPrincipal user,
            @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient authorizedClient,
            Model model) {

        if (user != null && authorizedClient != null) {
            model.addAttribute("accessToken", authorizedClient.getAccessToken().getTokenValue());
        }
        model.addAttribute("deviceId", id);
        return "edit-device";
    }
}
