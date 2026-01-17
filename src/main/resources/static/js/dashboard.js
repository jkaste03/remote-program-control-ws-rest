import { t, i18nReady, applyTranslations } from "./i18n.js";
import { errorFromResponse } from "./problem.js";

await i18nReady;
applyTranslations();

const API_ROOT = "/remote-program-control/api/v1";
const DEVICES_URL = `${API_ROOT}/devices`;
const PROGRAMS_URL = `${API_ROOT}/programs`;
const DASHBOARD_EVENTS_URL = "/api/v1/dashboard/events";

const deviceContainer = document.getElementById("device-container");
const toastEl = document.getElementById("toast");
const deviceState = new Map();
const programState = new Map();

function getAccessToken() {
    const inline = typeof window !== "undefined" ? window.__ACCESS_TOKEN__ : null;
    if (inline) return inline;
    const meta = document.querySelector('meta[name="access-token"]');
    return meta?.content || null;
}

async function authFetch(url, options = {}) {
    const headers = new Headers(options.headers || {});
    headers.set("Accept", "application/hal+json");
    const token = getAccessToken();
    if (token) {
        headers.set("Authorization", `Bearer ${token}`);
    }
    if (options.body && !(options.body instanceof FormData)) {
        headers.set("Content-Type", "application/json");
    }
    const response = await fetch(url, { ...options, headers });
    return response;
}

function normalizeHref(href) {
    if (!href) return null;
    try {
        return new URL(href, window.location.origin).href;
    } catch (_) {
        return href;
    }
}

function getLink(resource, rel) {
    const links = resource?._links;
    const link = links?.[rel];
    if (!link) return null;
    return normalizeHref(typeof link === "string" ? link : link.href);
}

async function fetchJson(url, options, fallbackMessage) {
    const response = await authFetch(url, options);
    if (!response.ok) {
        throw await errorFromResponse(response, fallbackMessage);
    }
    return response.json();
}

function formatDate(value) {
    if (!value) return t("common.unknown");
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return t("common.unknown");
    return date.toLocaleString();
}

function extractDevices(payload) {
    if (!payload?._embedded) return [];
    if (payload._embedded.deviceResponseList) return payload._embedded.deviceResponseList;
    // fallback: pick the first embedded array if naming changes
    const firstKey = Object.keys(payload._embedded)[0];
    return firstKey ? payload._embedded[firstKey] : [];
}

function renderDevices(devices) {
    deviceState.clear();
    programState.clear();

    if (!Array.isArray(devices) || devices.length === 0) {
        deviceContainer.innerHTML = `<div class="empty-state">${t("dashboard.noDevices")}</div>`;
        return;
    }

    const html = devices
        .map((device) => {
            deviceState.set(device.id, device);
            const actions = renderDeviceActions(device);

            return `
            <article class="card device-card" data-device-id="${device.id}">
                <div class="card-header">
                    <div>
                        <p class="eyebrow">${t("dashboard.owner")}: ${device.username ?? t("common.unknown")}</p>
                        <h2 class="device-name">${device.name}</h2>
                        ${
                            device.status === "ONLINE"
                                ? ""
                                : `<p class="meta">${t("dashboard.lastSeen")}: ${formatDate(device.lastSeen)}</p>`
                        }
                    </div>
                    <span class="status-chip status-${device.status}" id="device-status-${device.id}">
                        ${device.status}
                    </span>
                </div>
                ${actions}
                <div class="program-list" id="programs-${device.id}">
                    ${renderPrograms(extractProgramsFromDevice(device), device.id)}
                </div>
            </article>`;
        })
        .join("");

    deviceContainer.innerHTML = html;
}

function renderDeviceActions(device) {
    const updateLink = getLink(device, "self");
    const deleteLink = getLink(device, "delete");
    if (!updateLink && !deleteLink) return "";

    return `
        <div class="card-actions">
            ${
                updateLink
                    ? `<button class="btn" data-device-action="update" data-device-id="${device.id}" data-href="${updateLink}">${t("dashboard.device.update")}</button>`
                    : ""
            }
            ${
                deleteLink
                    ? `<button class="btn btn-danger" data-device-action="delete" data-device-id="${device.id}" data-href="${deleteLink}">${t("dashboard.device.delete")}</button>`
                    : ""
            }
        </div>
    `;
}

function renderPrograms(programs, deviceId) {
    if (!Array.isArray(programs) || programs.length === 0) {
        return `<div class="badge">${t("dashboard.programs.none")}</div>`;
    }

    return programs
        .map((program) => {
            programState.set(program.id, { program, deviceId });
            const status = program.status;
            const startLink = getLink(program, "start");
            const stopLink = getLink(program, "stop");
            const updateLink = getLink(program, "self");
            const action = startLink ? "start" : stopLink ? "stop" : null;
            const actionHref = startLink || stopLink || "";
            const busy = status === "TRYING_TO_START" || status === "TRYING_TO_STOP";
            const label = action === "start"
                ? t("dashboard.program.start")
                : action === "stop"
                    ? t("dashboard.program.stop")
                    : t("dashboard.program.busy");
            const disabled = !action || busy ? "disabled" : "";
            const actionClass = action === "stop" ? "btn btn-danger" : "btn btn-primary";

            return `
                <div class="program-row" data-program-id="${program.id}" data-device-id="${deviceId}">
                    <div class="program-info">
                        <p class="program-name">${program.name}</p>
                        <p class="program-meta">${t("dashboard.program.status")}: <span id="program-status-${program.id}">${status}</span></p>
                    </div>
                    <div class="actions">
                        <button class="${actionClass}" data-program-action="${action ?? ""}" data-program-id="${
                program.id
            }" data-href="${actionHref}" ${disabled}>${label}</button>
                        ${
                            updateLink
                                ? `<button class="btn" data-program-action="update" data-program-id="${program.id}" data-href="${updateLink}">${t("dashboard.program.update")}</button>`
                                : ""
                        }
                    </div>
                </div>
            `;
        })
        .join("");
}

function extractProgramsFromDevice(device) {
    const programs = device.programs;
    if (Array.isArray(programs)) return programs;
    if (programs && typeof programs === "object" && programs._embedded) {
        const firstKey = Object.keys(programs._embedded)[0];
        if (firstKey) return programs._embedded[firstKey] || [];
    }
    return [];
}

async function loadDevices() {
    deviceContainer.innerHTML = `<div class="loading">${t("dashboard.loading")}</div>`;
    try {
        const payload = await fetchJson(DEVICES_URL, undefined, t("dashboard.error.fetchDevices"));
        const devices = extractDevices(payload);
        renderDevices(devices);
    } catch (error) {
        const message = error.message || t("dashboard.error.fetchDevices");
        deviceContainer.innerHTML = `<div class="empty-state">${message}</div>`;
        showToast(message, "error");
    }
}

async function handleProgramAction(programId, rel, href, button) {
    button.disabled = true;
    button.dataset.loading = "true";

    try {
        if (rel !== "start" && rel !== "stop") {
            throw new Error(t("dashboard.program.unknownAction"));
        }

        const link = normalizeHref(href) || `${PROGRAMS_URL}/${programId}/${rel}`;
        await fetchJson(link, { method: "POST" }, t("dashboard.program.actionError"));
        updateProgramStatus(programId, rel === "start" ? "TRYING_TO_START" : "TRYING_TO_STOP");
        showToast(rel === "start" ? t("dashboard.program.starting") : t("dashboard.program.stopping"));
    } catch (error) {
        showToast(error.message || t("dashboard.program.actionError"), "error");
    } finally {
        button.disabled = false;
        delete button.dataset.loading;
    }
}

async function handleProgramUpdate(programId, href, button) {
    const target = `/programs/${programId}/edit`;
    window.location.href = target;
}

async function handleDeviceAction(deviceId, action, href, button) {
    const device = deviceState.get(deviceId) || {};
    const target = normalizeHref(href) || `${DEVICES_URL}/${deviceId}`;

    if (action === "update") {
        // Navigate to the edit page instead of the API endpoint
        window.location.href = `/devices/${deviceId}/edit`;
        return;
    }

    if (action === "delete") {
        const confirmed = confirm(t("dashboard.device.confirmDelete"));
        if (!confirmed) return;

        button.disabled = true;
        try {
            const response = await authFetch(target, { method: "DELETE" });
            if (!response.ok) {
                throw await errorFromResponse(response, t("dashboard.device.deleteError"));
            }
            showToast(t("dashboard.device.deleted"));
            await loadDevices();
        } catch (error) {
            showToast(error.message || t("dashboard.device.deleteError"), "error");
        } finally {
            button.disabled = false;
        }
        return;
    }
}

function showToast(message, variant = "info") {
    if (!toastEl) return;
    toastEl.textContent = message;
    toastEl.classList.remove("toast-hidden");
    toastEl.style.borderColor = variant === "error" ? "rgba(239,68,68,0.7)" : "var(--border)";
    toastEl.style.color = variant === "error" ? "#fecdd3" : "var(--text)";

    clearTimeout(showToast._t);
    showToast._t = setTimeout(() => {
        toastEl.classList.add("toast-hidden");
    }, 2600); // TODO: Set longer duration (possibly based on message length/variant)
}

function updateDeviceStatus(deviceId, status, lastSeen) {
    const badge = document.getElementById(`device-status-${deviceId}`);
    if (badge) {
        badge.textContent = status;
        badge.className = `status-chip status-${status}`;
    }

    const card = document.querySelector(`.device-card[data-device-id="${deviceId}"]`);
    if (!card) return;

    const headerCtx = card.querySelector(".card-header > div");
    if (!headerCtx) return;

    let metaEl = headerCtx.querySelector(".meta");
    if (status === "ONLINE") {
        if (metaEl) metaEl.remove();
    } else if (lastSeen) {
        const text = `Last seen: ${formatDate(lastSeen)}`;
        if (metaEl) {
            metaEl.textContent = text;
        } else {
            metaEl = document.createElement("p");
            metaEl.className = "meta";
            metaEl.textContent = text;
            headerCtx.appendChild(metaEl);
        }
    }
}

function updateProgramStatus(programId, status) {
    const badge = document.getElementById(`program-status-${programId}`);
    if (badge) {
        badge.textContent = status;
    }

    const row = document.querySelector(`.program-row[data-program-id="${programId}"]`);
    if (row) {
        const button = row.querySelector("[data-program-action]");
        const action = status === "STOPPED" ? "start" : status === "STARTED" ? "stop" : null;
        const busy = status === "TRYING_TO_START" || status === "TRYING_TO_STOP";
        if (button) {
            button.textContent = action === "start"
                ? t("dashboard.program.start")
                : action === "stop"
                    ? t("dashboard.program.stop")
                    : t("dashboard.program.busy");
            button.dataset.programAction = action ?? "";
            if (action === "stop") {
                button.className = "btn btn-danger";
            } else {
                button.className = "btn btn-primary";
            }

            // Update href based on action to ensure the correct endpoint is called
            if (action) {
                button.dataset.href = `${PROGRAMS_URL}/${programId}/${action}`;
            }

            button.disabled = !action || busy;
        }
    }
}

function initSse() {
    const es = new EventSource(DASHBOARD_EVENTS_URL);

    es.addEventListener("DEVICE_STATUS", (evt) => {
        try {
            const data = JSON.parse(evt.data);
            updateDeviceStatus(data.deviceId, data.status, data.lastSeen);
        } catch (error) {
            console.error("Kunne ikke parse DEVICE_STATUS", error);
        }
    });

    es.addEventListener("PROGRAM_STATUS", (evt) => {
        try {
            const data = JSON.parse(evt.data);
            updateProgramStatus(data.programId, data.status);
        } catch (error) {
            console.error("Kunne ikke parse PROGRAM_STATUS", error);
        }
    });

    es.onerror = () => {
        showToast(t("dashboard.sse.lost"), "error");
    };
}

deviceContainer.addEventListener("click", (event) => {
    const programButton = event.target.closest("[data-program-action]");
    if (programButton) {
        const programId = Number(programButton.dataset.programId);
        const rel = programButton.dataset.programAction;
        if (!programId || !rel) return;
        if (rel === "update") {
            handleProgramUpdate(programId, programButton.dataset.href, programButton);
        } else {
            handleProgramAction(programId, rel, programButton.dataset.href, programButton);
        }
        return;
    }

    const deviceButton = event.target.closest("[data-device-action]");
    if (deviceButton) {
        const deviceId = Number(deviceButton.dataset.deviceId);
        const action = deviceButton.dataset.deviceAction;
        if (!deviceId || !action) return;
        handleDeviceAction(deviceId, action, deviceButton.dataset.href, deviceButton);
    }
});

(async function bootstrap() {
    await loadDevices();
    initSse();
})();
