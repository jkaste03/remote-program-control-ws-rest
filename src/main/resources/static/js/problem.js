import { t } from "./i18n.js";

function isJsonContentType(contentType) {
    if (!contentType) return false;
    const lower = contentType.toLowerCase();
    return lower.includes("application/json") || lower.includes("application/problem+json") || lower.includes("+json");
}

export async function parseProblemResponse(response) {
    const contentType = response.headers.get("content-type") || "";
    const jsonLike = isJsonContentType(contentType);

    let body = null;
    if (jsonLike) {
        try {
            body = await response.json();
        } catch (_) {
            body = null;
        }
    } else {
        try {
            body = await response.text();
        } catch (_) {
            body = null;
        }
    }

    const problem = typeof body === "object" && body !== null ? { ...body } : { detail: body };
    problem.status = problem.status || response.status;
    problem.title = problem.title || response.statusText || t("common.error");
    return problem;
}

export function problemMessage(problem, fallbackMessage) {
    if (!problem) return fallbackMessage || t("common.genericError");
    const detail = typeof problem.detail === "string" ? problem.detail.trim() : problem.detail;
    const title = typeof problem.title === "string" ? problem.title.trim() : problem.title;

    if (detail && title && !detail.startsWith(title)) {
        return `${title}: ${detail}`;
    }
    if (detail) return detail;
    if (title) return title;
    return fallbackMessage || t("common.genericError");
}

export async function errorFromResponse(response, fallbackMessage) {
    const problem = await parseProblemResponse(response);
    const message = problemMessage(problem, fallbackMessage);
    const err = new Error(message);
    err.problem = problem;
    return err;
}
