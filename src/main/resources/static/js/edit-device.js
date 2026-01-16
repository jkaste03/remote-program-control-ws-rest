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
            throw new Error("Failed to fetch device details");
        }

        const device = await response.json();

        document.getElementById("name").value = device.name;
        document.getElementById("publicIp").value = device.publicIp;
        document.getElementById("wakeOnLanPort").value = device.wakeOnLanPort;
        document.getElementById("macAddress").value = device.macAddress;
    } catch (error) {
        showToast("Error loading device details: " + error.message, "error");
    }
}

// Handle form submission
form.addEventListener("submit", async (e) => {
    e.preventDefault();

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
            let errorMessage = "Failed to update device";
            try {
                const errorData = await response.json();
                const validationError = Array.isArray(errorData.errors) && errorData.errors.length > 0
                    ? errorData.errors[0].message
                    : null;
                errorMessage = validationError || errorData.detail || errorMessage;
            } catch (parseError) {
                console.error("Failed to parse error response", parseError);
            }
            throw new Error(errorMessage);
        }

        showToast("Device updated successfully!", "success");

        // Optional: Redirect back after delay
        setTimeout(() => {
            window.location.href = "/";
        }, 1500);
    } catch (error) {
        showToast("Error updating device: " + error.message, "error");
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
