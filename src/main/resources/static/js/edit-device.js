import { t, i18nReady, applyTranslations } from "./i18n.js";
import { errorFromResponse } from "./problem.js";

await i18nReady;
applyTranslations();

const deviceId = document.querySelector('meta[name="device-id"]').content;
const accessToken = document.querySelector('meta[name="access-token"]').content;

const form = document.getElementById("edit-device-form");
const toast = document.getElementById("toast");

// Fetch device details on load
async function fetchDeviceDetails() {
    try {
        const response = await fetch(`/remote-program-control/api/v1/devices/${deviceId}`, {
            headers: {
                Authorization: `Bearer ${accessToken}`,
            },
        });

        if (!response.ok) {
            throw await errorFromResponse(response, t("device.error.fetch"));
        }

        const device = await response.json();

        document.getElementById("name").value = device.name;
        document.getElementById("publicIp").value = device.publicIp;
        document.getElementById("wakeOnLanPort").value = device.wakeOnLanPort;
        document.getElementById("macAddress").value = device.macAddress;
    } catch (error) {
        showToast(error.message || t("device.error.fetch"), "error");
    }
}

// Handle form submission
form.addEventListener("submit", async (e) => {
    e.preventDefault();

    if (!form.reportValidity()) {
        return;
    }

    const formData = {
        name: document.getElementById("name").value,
        publicIp: document.getElementById("publicIp").value,
        wakeOnLanPort: parseInt(document.getElementById("wakeOnLanPort").value),
        macAddress: document.getElementById("macAddress").value,
    };

    try {
        const response = await fetch(`/remote-program-control/api/v1/devices/${deviceId}`, {
            method: "PUT",
            headers: {
                Authorization: `Bearer ${accessToken}`,
                "Content-Type": "application/json",
            },
            body: JSON.stringify(formData),
        });

        if (!response.ok) {
            const err = await errorFromResponse(response, t("device.error.update"));
            const validationError = Array.isArray(err.problem?.errors) && err.problem.errors.length > 0
                ? err.problem.errors[0].message
                : null;
            throw new Error(validationError || err.message);
        }

        showToast(t("device.toast.updateSuccess"), "success");

        // Optional: Redirect back after delay
        setTimeout(() => {
            window.location.href = "/";
        }, 1500);
    } catch (error) {
        showToast(error.message || t("device.error.update"), "error");
    }
});

function showToast(message, type = "info") {
    toast.textContent = message;
    toast.className = `toast toast-visible ${type === "error" ? "toast-error" : ""}`;

    setTimeout(() => {
        toast.className = "toast toast-hidden";
    }, 3000);
}

// Initialize
fetchDeviceDetails();
