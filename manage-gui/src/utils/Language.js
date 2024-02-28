// DetermineLanguage based on parameter, cookie and finally navigator
import {isEmpty} from "./Utils";
import {getParameterByName, replaceQueryParameter} from "./QueryParameters";
import Cookies from "js-cookie";

let language = "en"

export function setLanguage(lang) {
    Cookies.set("lang", lang, {expires: 356, secure: document.location.protocol.endsWith("https")});
    window.location.search = replaceQueryParameter(window.location.search, "lang", lang);
}

export function getLanguage() {
    return language;
}

export function getInitialLanguage() {
    language = getParameterByName("lang", window.location.search);

    if (isEmpty(language)) {
        language = Cookies.get("lang");
    }
    if (isEmpty(language)) {
        language = navigator.language.toLowerCase().substring(0, 2);
    }
    if (["nl", "en"].indexOf(language) === -1) {
        language = "en";
    }
    return language;
}

export function getNameForLanguage(metaDataFields) {
    return language === "en" ? (metaDataFields["name:en"] || metaDataFields["name:nl"]) : (metaDataFields["name:nl"] || metaDataFields["name:en"]);
}

export function getOrganisationForLanguage(metaDataFields) {
    return language === "en" ? (metaDataFields["OrganizationName:en"] || metaDataFields["OrganizationName:nl"]) : (metaDataFields["OrganizationName:nl"] || metaDataFields["OrganizationName:en"]);
}
