const API_ROOT = "/remote-program-control/api/v1";
const DEVICES_URL = `${API_ROOT}/devices`;
const PROGRAMS_URL = `${API_ROOT}/programs`;
const DASHBOARD_EVENTS_URL = "/api/v1/dashboard/events";

const deviceContainer = document.getElementById("device-container");
const toastEl = document.getElementById("toast");

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

async function toProblem(response) {
    const contentType = response.headers.get("content-type") || "";
    let body;
    if (contentType.includes("json")) {
        try {
            body = await response.json();
        } catch (_) {
            body = null;
        }
    } else {
        body = await response.text();
    }

    const problem = typeof body === "object" && body !== null ? body : { detail: body };
    problem.status = problem.status || response.status;
    problem.title = problem.title || response.statusText || "Unknown error";

    const err = new Error(formatProblem(problem));
    err.problem = problem;
    return err;
}

async function fetchJson(url, options) {
    const response = await authFetch(url, options);
    if (!response.ok) {
        throw await toProblem(response);
    }
    return response.json();
}

function formatProblem(problem) {
    if (!problem) return "Something went wrong";
    const title = problem.title || "Error";
    const detail = problem.detail || problem.message || "";
    const status = problem.status ? ` (${problem.status})` : "";
    const code = problem.code ? ` [${problem.code}]` : "";
    return `${title}${status}${code}${detail ? ": " + detail : ""}`;
}

function formatDate(value) {
    if (!value) return "Unknown";
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return "Unknown";
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
    if (!Array.isArray(devices) || devices.length === 0) {
        deviceContainer.innerHTML = '<div class="empty-state">No devices available yet.</div>';
        return;
    }

    const html = devices
        .map(
            (device) => `
            <article class="card device-card" data-device-id="${device.id}">
                <div class="card-header">
                    <div>
                        <p class="eyebrow">Owner: ${device.username ?? "Unknown"}</p>
                        <h2 class="device-name">${device.name}</h2>
                        <p class="meta">Last seen: ${formatDate(device.lastSeen)}</p>
                    </div>
                    <span class="status-chip status-${device.status}" id="device-status-${device.id}">
                        ${device.status}
                    </span>
                </div>
                <div class="program-list" id="programs-${device.id}">
                    ${renderPrograms(extractProgramsFromDevice(device), device.id)}
                </div>
            </article>`
        )
        .join("");

    deviceContainer.innerHTML = html;
}

function renderPrograms(programs, deviceId) {
    if (!Array.isArray(programs) || programs.length === 0) {
        return '<div class="badge">No programs</div>';
    }

    return programs
        .map((program) => {
            const status = program.status;
            const action = status === "STOPPED" ? "start" : status === "STARTED" ? "stop" : null;
            const busy = status === "TRYING_TO_START" || status === "TRYING_TO_STOP";
            const label = action === "start" ? "Start" : action === "stop" ? "Stop" : "Busy";
            const disabled = !action || busy ? "disabled" : "";

            return `
                <div class="program-row" data-program-id="${program.id}" data-device-id="${deviceId}">
                    <div class="program-info">
                        <p class="program-name">${program.name}</p>
                        <p class="program-meta">${program.filename ?? ""}</p>
                        <p class="program-meta">Status: <span id="program-status-${program.id}">${status}</span></p>
                    </div>
                    <div class="actions">
                        <button class="btn btn-primary" data-program-action="${action ?? ""}" data-program-id="${
                program.id
            }" ${disabled}>${label}</button>
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
    deviceContainer.innerHTML = '<div class="loading">Loading devices...</div>';
    try {
        const payload = await fetchJson(DEVICES_URL);
        const devices = extractDevices(payload);
        renderDevices(devices);
    } catch (error) {
        deviceContainer.innerHTML = `<div class="empty-state">${error.message || "Could not fetch devices"}</div>`;
        showToast(error.message || "Could not fetch devices", "error");
    }
}

async function handleProgramAction(programId, rel, button) {
    button.disabled = true;
    button.dataset.loading = "true";

    try {
        if (rel !== "start" && rel !== "stop") {
            throw new Error("Unknown action.");
        }

        const link = `${PROGRAMS_URL}/${programId}/${rel}`;
        await fetchJson(link, { method: "POST" });
        showToast(rel === "start" ? "Starting program..." : "Stopping program...");
        await loadDevices();
    } catch (error) {
        showToast(error.message || "Could not perform action", "error");
    } finally {
        button.disabled = false;
        delete button.dataset.loading;
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
    }, 2600);
}

function updateDeviceStatus(deviceId, status) {
    const badge = document.getElementById(`device-status-${deviceId}`);
    if (badge) {
        badge.textContent = status;
        badge.className = `status-chip status-${status}`;
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
            button.textContent = action === "start" ? "Start" : action === "stop" ? "Stop" : "Busy";
            button.dataset.programAction = action ?? "";
            button.disabled = !action || busy;
        }
    }
}

function initSse() {
    const es = new EventSource(DASHBOARD_EVENTS_URL);

    es.addEventListener("DEVICE_STATUS", (evt) => {
        try {
            const data = JSON.parse(evt.data);
            updateDeviceStatus(data.deviceId, data.status);
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
        showToast("Lost SSE connection. Will retry on next reload.", "error");
    };
}

deviceContainer.addEventListener("click", (event) => {
    const button = event.target.closest("[data-program-action]");
    if (!button) return;
    const programId = Number(button.dataset.programId);
    const rel = button.dataset.programAction;
    if (!programId || !rel) return;
    handleProgramAction(programId, rel, button);
});

(async function bootstrap() {
    await loadDevices();
    initSse();
})();
