package com.github.jkaste03.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DeviceCreateRequest(
        @NotBlank(message = "{name.required}") @Size(max = 100, message = "{name.max-length}") String name,
        @NotBlank(message = "{device.public-ip.required}") @Pattern(regexp = "^(\\d{1,3}\\.){3}\\d{1,3}$|^([0-9a-fA-F]{0,4}:){2,7}[0-9a-fA-F]{0,4}$", message = "{device.public-ip.invalid}") String publicIp,
        @NotNull(message = "{device.wol-port.required}") @Min(value = 1, message = "{device.wol-port.min}") @Max(value = 65535, message = "{device.wol-port.max}") Integer wakeOnLanPort,
        @NotBlank(message = "{device.mac-address.required}") @Pattern(regexp = "^[0-9A-Fa-f]{2}(:[0-9A-Fa-f]{2}){5}$", message = "{device.mac-address.invalid}") String macAddress) {
}
