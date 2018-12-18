import spinner from "../lib/Spin";
import {isEmpty} from "../utils/Utils";

const apiPath = "/manage/api/client/";
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
                    throw error;
                }, 250);
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
    return ((isEmpty(query) || query.length < 3) && "*" !== query.trim()) ?
        Promise.resolve([]) : fetchJson(`autocomplete/${type}?query=${encodeURIComponent(query.trim())}`);
}

export function detail(type, id) {
    return fetchJson(`metadata/${type}/${id}`, {}, {}, false);
}

export function template(type) {
    return fetchJson(`template/${type}`);
}

export function ping() {
    return fetchJson("users/ping");
}

export function save(metaData) {
    return postPutJson("metadata", metaData, "post");
}

export function remove(metaData) {
    return fetchDelete(`metadata/${metaData.type}/${metaData.id}`, metaData);
}

export function update(metaData) {
    return postPutJson("metadata", metaData, "put");
}

export function restoreRevision(id, type, parentType) {
    return postPutJson("restoreRevision", {id: id, type: type, parentType: parentType}, "put");
}

export function restoreDeletedRevision(id, type, parentType) {
    return postPutJson("restoreDeleted", {id: id, type: type, parentType: parentType}, "put");
}

export function configuration() {
    return fetchJson("metadata/configuration");
}

export function revisions(type, parentId) {
    return fetchJson(`revisions/${type}/${parentId}`);
}

export function validation(format, value) {
    return postPutJson("validation", {type: format, value: value}, "post");
}

export function whiteListing(type) {
    return fetchJson(`whiteListing/${type}`);
}

export function exportMetaData(metaData) {
    return postPutJson("export", metaData, "post");
}

export function importMetaDataXmlUrl(url, type, entityId) {
    const body = {url: url};
    if (!isEmpty(entityId)) {
        body.entityId = entityId;
    }
    return postPutJson(`import/endpoint/xml/${type}`, body, "post");
}

export function importMetaDataJsonUrl(url, type) {
    const body = {url: url};
    return postPutJson(`import/endpoint/json/${type}`, body, "post");
}

export function importMetaDataXML(xml, type) {
    return postPutJson(`import/xml/${type}`, {xml: xml}, "post");
}


export function importMetaDataJSON(type, metaData) {
    return postPutJson(`import/json/${type}`, JSON.parse(metaData), "post");
}

export function importFeed(url) {
    return postPutJson("import/feed", {url: url}, "post");
}

export function deleteFeed() {
    return fetchDelete("delete/feed").then(res => res.json());
}

export function countFeed() {
    return fetchJson("count/feed");
}

export function search(options, type) {
    return postPutJson(`search/${type}`, options, "post");
}

export function rawSearch(query, type) {
    return fetchJson(`rawSearch/${type}?query=${encodeURIComponent(query)}`)
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

export function push() {
    return fetchJson("playground/push");
}

export function pushPreview() {
    return fetchJson("playground/pushPreview");
}

export function validate() {
    return fetchJson("playground/validate");
}

export function orphans() {
    return fetchJson("playground/orphans");
}

export function deleteOrphanedReferences() {
    return fetchDelete("playground/deleteOrphans");
}

export function includeInPush(id) {
    const path = `includeInPush/saml20_sp/${id}`;
    return postPutJson(path, {}, "put");
}