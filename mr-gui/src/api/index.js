import spinner from "../lib/Spin";
import {isEmpty} from "../utils/Utils";

const apiPath = "/mr/api/client/";
let csrfToken = null;

function apiUrl(path) {
    return apiPath + path;
}

function validateResponse(showErrorDialog) {
    return res => {
        spinner.stop();

        if (!res.ok) {
            if (res.type === "opaqueredirect") {
                setTimeout(() => window.location.reload(true), 100);
                return res;
            }
            const error = new Error(res.statusText);
            error.response = res;
            if (showErrorDialog) {
                setTimeout(() => {
                    const error = new Error(res.statusText);
                    error.response = res;
                    throw error;
                }, 100);
            }
            throw error;
        }
        csrfToken = res.headers.get("x-csrf-token");

        const sessionAlive = res.headers.get("x-session-alive");

        if (sessionAlive !== "true") {
            window.location.reload(true);
        }
        return res;

    };
}

function validFetch(path, options, headers = {}, showErrorDialog = true) {
    const contentHeaders = {
        "Accept": "application/json",
        "Content-Type": "application/json",
        "X-CSRF-TOKEN": csrfToken,
        ...headers
    };

    const fetchOptions = Object.assign({}, {headers: contentHeaders}, options, {
        credentials: "same-origin",
        redirect: "manual"
    });
    spinner.start();
    return fetch(apiUrl(path), fetchOptions)
        .catch(err => {
            spinner.stop();
            throw err;
        })
        .then(validateResponse(showErrorDialog));
}

function fetchJson(path, options = {}, headers = {}, showErrorDialog = true) {
    return validFetch(path, options, headers, showErrorDialog)
        .then(res => res.json());
}

function postPutJson(path, body, method) {
    return fetchJson(path, {method: method, body: JSON.stringify(body)});
}

function fetchDelete(path) {
    return validFetch(path, {method: "delete"});
}

//API
export function autocomplete(type, query) {
    return isEmpty(query) || query.length < 3 ? Promise.resolve([]) : fetchJson(`autocomplete/${type}?query=${encodeURIComponent(query)}`);
}

export function detail(type, id) {
    return fetchJson(`metadata/${type}/${id}`);
}

export function ping() {
    return fetchJson("users/ping");
}

export function save(metaData) {
    return postPutJson("metadata", metaData, "post");
}

export function update(metaData) {
    return postPutJson("metadata", metaData, "put");
}

export function configuration() {
    return fetchJson("metadata/configuration");
}

export function revisions(type, parentId) {
    return fetchJson(`revisions/${type}/${parentId}`);
}

export function validation(format, value) {
    return fetchJson(`validation/${format}?value=${encodeURIComponent(value)}`);
}

export function whiteListing(type) {
    return fetchJson(`whiteListing/${type}`);
}

export function search(options, type) {
    return postPutJson(`search/${type}`, options, "post");
}

export function me() {
    return fetchJson("users/me", {}, {}, false);
}

export function reportError(error) {
    return postPutJson("users/error", error, "post");
}

export function logOut() {
    return fetchDelete("users/logout");
}
