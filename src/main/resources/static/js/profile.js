import { errorFromResponse } from "./problem.js";

const contextPath = (() => {
    const raw = document.querySelector('meta[name="context-path"]')?.content || "";
    if (raw === "/" || raw === "") return "";
    return raw.endsWith("/") ? raw.slice(0, -1) : raw;
})();
const form = document.getElementById("profile-form");
const input = document.getElementById("profile-username");
const toast = document.getElementById("toast");
const token = document.querySelector('meta[name="access-token"]')?.content;

form.addEventListener("submit", async (event) => {
    event.preventDefault();

    try {
        const response = await fetch(`${contextPath}/api/v1/users/me`, {
            method: "PUT",
            headers: {
                Accept: "application/json",
                "Content-Type": "application/json",
                ...(token ? { Authorization: `Bearer ${token}` } : {})
            },
            body: JSON.stringify({ username: input.value.trim() })
        });

        if (!response.ok) {
            throw await errorFromResponse(response, "Could not update username");
        }

        const updated = await response.json();
        input.value = updated.username;
        showToast("Username updated", "success");
        window.location.assign(`${contextPath}/`);
    } catch (error) {
        showToast(error.message || "Could not update username", "error");
    }
});

function showToast(message, type) {
    toast.textContent = message;
    toast.className = `toast toast-visible ${type === "error" ? "toast-error" : ""}`;
}
