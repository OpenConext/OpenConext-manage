import {EventEmitter} from "events";
import I18n from "i18n-js";

export const emitter = new EventEmitter();

//sneaky global...
let flash = {};

export function getFlash() {
    return {...flash};
}

export function setFlash(message, type) {
    flash = {message, type: type || "info"};
    emitter.emit("flash", flash);
}

export function clearFlash() {
    emitter.emit("flash", {});
}

export function pushFlash(isSuccess, currentUser) {
    if (currentUser.push.excludeOidcRP) {
        const msg = isSuccess ? "playground.pushedOk" : "playground.pushedNotOk";
        return I18n.t(msg, {name: currentUser.push.name});
    }
    const msg = isSuccess ? "playground.pushedOkWithOidc" : "playground.pushedNotOkWithOidc";
    return I18n.t(msg, {name: currentUser.push.name, oidcName: currentUser.push.oidcName});
}

export function pushConfirmationFlash(currentUser) {
    if (currentUser.push.excludeOidcRP) {
        return I18n.t("playground.pushConfirmation", {
            url: currentUser.push.url,
            name: currentUser.push.name
        });
    }
    return I18n.t("playground.pushConfirmationWithOidc", {
        url: currentUser.push.url,
        name: currentUser.push.name,
        oidcName: currentUser.push.oidcName,
        oidcUrl: currentUser.push.oidcUrl
    });
}