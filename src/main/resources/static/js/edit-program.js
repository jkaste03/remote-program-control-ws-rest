import { t, i18nReady, applyTranslations } from "./i18n.js";
import { errorFromResponse } from "./problem.js";

await i18nReady;
applyTranslations();

const programId = document.querySelector('meta[name="program-id"]').content;
const accessToken = document.querySelector('meta[name="access-token"]').content;

const form = document.getElementById("edit-program-form");
const toast = document.getElementById("toast");
const filenameInput = document.getElementById("filename");
const nameInput = document.getElementById("name");
const isPublicInput = document.getElementById("isPublic");
const allowedSection = document.getElementById("allowed-users-section");
const selectedUsersEl = document.getElementById("selected-users");
const searchInput = document.getElementById("userSearch");
const searchBtn = document.getElementById("userSearchBtn");
const searchResultsEl = document.getElementById("user-search-results");

// Maintain selected users as a Map for quick lookups/removal
const selectedUsers = new Map();
let searchTimer;

async function authFetch(url, options = {}) {
    const headers = new Headers(options.headers || {});
    headers.set("Accept", "application/json");
    if (accessToken) {
        headers.set("Authorization", `Bearer ${accessToken}`);
    }
    if (options.body && !(options.body instanceof FormData)) {
        headers.set("Content-Type", "application/json");
    }
    const response = await fetch(url, { ...options, headers });
    return response;
}

function showToast(message, type = "info") {
    if (!toast) return;
    toast.textContent = message;
    toast.className = `toast toast-visible ${type === "error" ? "toast-error" : ""}`;

    clearTimeout(showToast._t);
    showToast._t = setTimeout(() => {
        toast.className = "toast toast-hidden";
    }, 3000);
}

function toggleAllowedSection(isPublic) {
    if (isPublic) {
        allowedSection.classList.add("hidden");
    } else {
        allowedSection.classList.remove("hidden");
    }
}

function renderSelectedUsers() {
    if (!selectedUsersEl) return;

    if (selectedUsers.size === 0) {
        selectedUsersEl.innerHTML = `<span class="chip">${t("program.form.allowedUsers.none")}</span>`;
        return;
    }

    const chips = Array.from(selectedUsers.values())
        .map(
            (u) => `
            <span class="chip" data-user-id="${u.id}">
                ${u.label}
                <button type="button" aria-label="${t("program.form.allowedUsers.remove", { user: u.label })}" data-remove-user="${u.id}">&times;</button>
            </span>
        `,
        )
        .join("");

    selectedUsersEl.innerHTML = chips;
}

function escapeHtml(value) {
    return value
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#39;");
}

function renderSearchResults(users) {
    if (!searchResultsEl) return;

    if (!Array.isArray(users) || users.length === 0) {
        searchResultsEl.innerHTML = `<div>${t("program.search.noResults")}</div>`;
    } else {
        searchResultsEl.innerHTML = users
            .map((u) => {
                const alreadySelected = selectedUsers.has(u.id);
                const labelRaw = (u.username || "").trim() || u.id;
                const labelSafe = escapeHtml(labelRaw);
                const labelEncoded = encodeURIComponent(labelRaw);
                return `
                    <div class="result-row">
                        <div>
                            <strong>${labelSafe}</strong>
                        </div>
                        <button type="button" data-user-id="${u.id}" data-user-label="${labelEncoded}" ${
                            alreadySelected ? "disabled" : ""
                        }>
                            ${alreadySelected ? t("program.search.added") : t("program.search.add")}
                        </button>
                    </div>
                `;
            })
            .join("");
    }

    searchResultsEl.classList.remove("hidden");
}

async function searchUsers(query) {
    const trimmed = query.trim();
    if (trimmed.length < 2) {
        searchResultsEl.innerHTML = `<div>${t("program.search.tooShort")}</div>`;
        searchResultsEl.classList.remove("hidden");
        return;
    }

    try {
        const url = `/remote-program-control/api/v1/users/search?q=${encodeURIComponent(trimmed)}&limit=10`;
        const response = await authFetch(url);
        if (!response.ok) {
            throw await errorFromResponse(response, t("program.search.failed"));
        }
        const users = await response.json();
        renderSearchResults(users);
    } catch (error) {
        console.error("Search failed", error);
        showToast(error.message || t("program.search.couldNotSearch"), "error");
    }
}

function hydrateSelectedUsers(program) {
    const ids = Array.isArray(program.allowedUserIds) ? program.allowedUserIds : [];
    const names = Array.isArray(program.allowedUsers) ? program.allowedUsers : [];
    ids.forEach((id, idx) => {
        const label = names[idx] ?? id;
        selectedUsers.set(id, { id, label });
    });
    renderSelectedUsers();
}

async function fetchProgramDetails() {
    try {
        const response = await authFetch(`/remote-program-control/api/v1/programs/${programId}`);
        if (!response.ok) {
            throw await errorFromResponse(response, t("program.error.fetch"));
        }

        const program = await response.json();
        filenameInput.value = program.filename;
        nameInput.value = program.name;
        isPublicInput.checked = program.isPublic;
        toggleAllowedSection(program.isPublic);
        hydrateSelectedUsers(program);
    } catch (error) {
        showToast(error?.message || t("program.error.fetch"), "error");
    }
}

function getPayload() {
    const isPublic = isPublicInput.checked;
    return {
        filename: filenameInput.value,
        name: nameInput.value,
        isPublic,
        allowedUserIds: isPublic ? [] : Array.from(selectedUsers.keys()),
    };
}

form.addEventListener("submit", async (e) => {
    e.preventDefault();
    if (!form.reportValidity()) {
        return;
    }

    try {
        const response = await authFetch(`/remote-program-control/api/v1/programs/${programId}`, {
            method: "PUT",
            body: JSON.stringify(getPayload()),
        });

        if (!response.ok) {
            const err = await errorFromResponse(response, t("program.error.update"));
            const validationError =
                Array.isArray(err.problem?.errors) && err.problem.errors.length > 0
                    ? err.problem.errors[0].message
                    : null;
            throw new Error(validationError || err.message);
        }

        showToast(t("program.toast.updateSuccess"), "success");
        setTimeout(() => {
            window.location.href = "/";
        }, 1200);
    } catch (error) {
        showToast(error.message || t("program.error.update"), "error");
    }
});

isPublicInput.addEventListener("change", () => {
    toggleAllowedSection(isPublicInput.checked);
});

searchBtn.addEventListener("click", () => {
    searchUsers(searchInput.value || "");
});

searchInput.addEventListener("input", () => {
    clearTimeout(searchTimer);
    searchTimer = setTimeout(() => {
        searchUsers(searchInput.value || "");
    }, 350);
});

searchResultsEl.addEventListener("click", (event) => {
    const addBtn = event.target.closest("[data-user-id]");
    if (!addBtn) return;
    const id = addBtn.getAttribute("data-user-id");
    const label = decodeURIComponent(addBtn.getAttribute("data-user-label") || "") || id;
    if (!id) return;
    selectedUsers.set(id, { id, label });
    renderSelectedUsers();
    addBtn.disabled = true;
    addBtn.textContent = t("program.search.added");
});

selectedUsersEl.addEventListener("click", (event) => {
    const removeBtn = event.target.closest("[data-remove-user]");
    if (!removeBtn) return;
    const id = removeBtn.getAttribute("data-remove-user");
    if (id) {
        selectedUsers.delete(id);
        renderSelectedUsers();
    }
});

// Initialize
fetchProgramDetails();
