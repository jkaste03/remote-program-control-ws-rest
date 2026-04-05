const fallbackLocale = "en";

function normalizeLocale(raw) {
    if (!raw) return fallbackLocale;
    return raw.toLowerCase().split("-")[0];
}

async function loadMessages(locale) {
    try {
        const res = await fetch(`${contextPath}/i18n/${locale}.json`, { cache: "no-cache" });
        if (!res.ok) throw new Error("not found");
        return await res.json();
    } catch (_) {
        return null;
    }
}

const localeMeta = document.querySelector('meta[name="locale"]');
const ctxMeta = document.querySelector('meta[name="context-path"]');
const contextPath = (() => {
    const raw = ctxMeta?.content || "";
    if (raw === "/" || raw === "") return "";
    return raw.endsWith("/") ? raw.slice(0, -1) : raw;
})();
const detectedLocale = normalizeLocale(localeMeta?.content || navigator.language || fallbackLocale);

let messages = {};

export const i18nReady = (async () => {
    messages = (await loadMessages(detectedLocale)) || (await loadMessages(fallbackLocale)) || {};
})();

export function t(key, vars = {}) {
    const template = messages[key] ?? key;
    return template.replace(/\{(\w+)\}/g, (_, name) => (vars[name] ?? `{${name}}`));
}

export function applyTranslations(root = document) {
    root.querySelectorAll("[data-i18n]").forEach((el) => {
        const key = el.getAttribute("data-i18n");
        if (!key) return;
        const attr = el.getAttribute("data-i18n-attr");
        const value = t(key);
        if (attr) {
            el.setAttribute(attr, value);
        } else {
            el.textContent = value;
        }
    });
}
