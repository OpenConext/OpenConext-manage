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

export function pushFlash(isSuccess) {
    return I18n.t(isSuccess ? "playground.pushedOk" : "playground.pushedNotOk");
}
