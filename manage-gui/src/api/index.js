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

function postPutJson(path, body, method, showErrorDialog = true) {
    return fetchJson(path, {method: method, body: JSON.stringify(body)}, showErrorDialog);
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

export function getAllEntities() {
    return fetchJson(`metadata/allentities`);
}

export function remove(metaData, revisionNote) {
    return postPutJson(`metadata/${metaData.type}/${metaData.id}`, {revisionNote}, "put");
}

export function update(metaData) {
    return postPutJson("metadata", metaData, "put");
}

export function validateUniqueField(type, fieldName, value) {
    return fetchJson(`metadata/validate-unique-field/${type}/${fieldName}/${value}`, {}, {}, false);
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

export function stats() {
    return fetchJson("metadata/stats");
}

export function revisions(type, parentId) {
    return fetchJson(`revisions/${type}/${parentId}`);
}

export function validation(format, value) {
    return postPutJson("validation", {type: format, value: value}, "post");
}

export function ipInfo(ipAddress, networkPrefix) {
    const networkPrefixParam = isEmpty(networkPrefix) ? "" : "&networkPrefix=" + networkPrefix;
    return fetchJson("/ipinfo?ipAddress=" + encodeURIComponent(ipAddress) + networkPrefixParam);
}

export function fetchEnumValues(fetchValue) {
    return fetchJson(`fetch/${fetchValue}`);
}

export function secret() {
    return fetchJson("secret");
}

export function whiteListing(type, state) {
    return fetchJson(`whiteListing/${type}?state=${encodeURIComponent(state)}`);
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

export function allResourceServers(state) {
    return search({"state": state}, "oauth20_rs");
}

export function relyingPartiesByResourceServer(resourceServerEntityID) {
    return fetchJson(`relyingParties?resourceServerEntityID=${encodeURIComponent(resourceServerEntityID)}`);
}

export function provisioningById(id) {
    return postPutJson("provisioning", [id], "POST");
}

export function search(options, type) {
    return postPutJson(`search/${type}`, options, "post");
}

export function allChangeRequests() {
    return fetchJson("change-requests/all");
}

export function hasOpenChangeRequests() {
    return fetchJson("change-requests/count");
}

export function changeRequests(type, metaDataId) {
    return fetchJson(`change-requests/${type}/${metaDataId}`);
}

export function newChangeRequest(changeRequest) {
    return postPutJson("change-requests", changeRequest, "post");
}

export function acceptChangeRequest(changeRequest) {
    return postPutJson("change-requests/accept", changeRequest, "put");
}

export function rejectChangeRequest(changeRequest) {
    return postPutJson("change-requests/reject", changeRequest, "put");
}

export function removeChangeRequests(type, metaDataId) {
    return fetchDelete(`change-requests/remove/${type}/${metaDataId}`);
}

export function uniqueEntityId(entityid, type) {
    return postPutJson(`uniqueEntityId/${type}`, {entityid}, "post");
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

export function push(includeEB, includeOIDC, includePdP) {
    return postPutJson("playground/push", {includeEB:includeEB, includeOIDC:includeOIDC, includePdP:includePdP}, "PUT");
}

export function pushPreview() {
    return fetchJson("playground/pushPreview");
}

export function pushPreviewOIDC() {
    return fetchJson("playground/pushPreviewOIDC");
}

export function pushPreviewPdP() {
    return fetchJson("playground/pushPreviewPdP");
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

export function includeInPush(id, type) {
    const path = `includeInPush/${type}/${id}`;
    return postPutJson(path, {}, "put");
}

//Scopes
export function allScopes() {
    return fetchJson("scopes")
}

export function scopeSupportedLanguagers() {
    return fetchJson("scopes_languages")
}

export function saveScope(scope) {
    return fetchJson("scopes", {method: scope.id ? "put" : "post", body: JSON.stringify(scope)}, {}, false);
}

export function deleteScope(id) {
    return validFetch(`scopes/${id}`, {method: "delete"}, {}, false);
}

export function scopeInUse(scopes) {
    return fetchJson(`inuse/scopes?scopes=${encodeURIComponent(scopes.join(","))}`);
}

//Activity
export function recentActivity(types, limit) {
    return postPutJson("recent-activity", {types, limit}, "POST")
}

//Policies
export function policyAttributes() {
    return fetchJson("attributes")
}

export function policySAMLAttributes() {
    return fetchJson("saml-attributes")
}

export function getAllowedLoas() {
    return fetchJson("loas");
}

export function getPlaygroundPolicies() {
    return search({ALL_ATTRIBUTES: true}, "policy")
}

export function getPlaygroundServiceProviders() {
    return search({}, "saml20_sp")
}

export function getPlaygroundRelyingParties() {
    return search({}, "oidc10_rp")
}

export function getPlaygroundIdentityProviders() {
    return search({}, "saml20_idp")
}
//Policies
export function idpPolicies(idpEntityID) {
    return fetchJson(`idpPolicies?entityId=${encodeURIComponent(idpEntityID)}`);
}

export function spPolicies(spEntityID) {
    return fetchJson(`spPolicies?entityId=${encodeURIComponent(spEntityID)}`);
}

export function missingEnforcementPolicies() {
    return fetchJson("pdp/missing-enforcements");
}

export function policyConflicts() {
    return fetchJson("pdp/conflicts");
}

export function parsePolicyXML(data) {
    return postPutJson("pdp/parse", data, "POST")
}

export function parsePolicyJSON(data) {
    return postPutJson("pdp/parse-json", data, "POST")
}

export function playGroundPolicyDecision(pdpRequest) {
    return postPutJson("pdp/decide", pdpRequest, "POST", false);
}

