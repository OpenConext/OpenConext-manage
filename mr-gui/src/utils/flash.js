import {EventEmitter} from "events";

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