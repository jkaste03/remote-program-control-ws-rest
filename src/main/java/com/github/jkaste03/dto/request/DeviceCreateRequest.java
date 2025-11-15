package com.github.jkaste03.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DeviceCreateRequest(
        @NotBlank(message = "{device.name.required}") @Size(max = 100, message = "{device.name.max-length}") String name,
        @Pattern(regexp = "^(\\d{1,3}\\.){3}\\d{1,3}$|^([0-9a-fA-F]{0,4}:){2,7}[0-9a-fA-F]{0,4}$", message = "{device.public-ip.invalid}") String publicIp,
        @NotNull @Min(1) @Max(65535) Integer wakeOnLanPort,
        @Pattern(regexp = "^[0-9A-Fa-f]{2}(:[0-9A-Fa-f]{2}){5}$", message = "{device.mac-address.invalid}") String macAddress) {
}
