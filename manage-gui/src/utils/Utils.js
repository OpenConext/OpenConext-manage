import escape from "lodash.escape";
import React from "react";

export function stop(e) {
    if (e !== undefined && e !== null) {
        e.preventDefault();
        e.stopPropagation();
    }
}

export function isEmpty(obj) {
    if (obj === undefined || obj === null) {
        return true;
    }
    if (Array.isArray(obj)) {
        return obj.length === 0;
    }
    if (typeof obj === "string") {
        return obj.trim().length === 0;
    }
    if (typeof obj === "object") {
        return Object.keys(obj).length === 0;
    }
    return false;
}

export function escapeDeep(obj) {
    if (!isEmpty(obj)) {
        Object.keys(obj).forEach(key => {
            const val = obj[key];
            if (typeof (val) === "string" || val instanceof String) {
                obj[key] = escape(val);
            } else if (typeof (val) === "object" || val instanceof Object) {
                escapeDeep(val);
            }
        });

    }
}

export function copyToClip(elementId) {
    const listener = e => {
        const str = document.getElementById(elementId).innerHTML.replace(/&amp;/g, "&");
        e.clipboardData.setData("text/plain", str);
        e.preventDefault();
    };
    document.addEventListener("copy", listener);
    document.execCommand("copy");
    document.removeEventListener("copy", listener);
}

export function capitalize(str) {
    return str.charAt(0).toUpperCase() + str.substring(1);
}

export function validScope(scope) {
    return isEmpty(scope) || scope.indexOf(" ") === -1;
}

/*
 * Given input:
 * {
 *  "a.b.c": "val",
 *  "d.e.f": ["some"],
 *  "g": {a:1}
 * }
 *
 * Output:
 * {
 *  a: {b: {c: "val"}},
 *  d: {e: {f: ["some]}},
 *  g: {a: 1}
 * }
 */
export function collapseDotKeys(data) {
    return Object
        .entries(data)
        .reduce((acc, [path, value]) => {
            const parts = path.split(".").map(p => p.replace(/@/g, "."));
            const last = parts.pop();
            parts.reduce((o, k) => o[k] = o[k] || {}, acc)[last] = value;
            return acc;
        }, {});
}

const originalValue = (data, acc, key, value) => {
    const sourceValue = data[key];
    if (typeof sourceValue === "object" && !Array.isArray(sourceValue)) {
        acc[key] = {};
        Object.keys(sourceValue)
            .filter(sk => value[sk])
            .forEach(sk => originalValue(sourceValue, acc[key], sk, value))
    } else if (sourceValue !== undefined) {
        acc[key] = sourceValue
    }
    return acc;
}

/*
 * Given input:
 * data: {a:"b",c:{d: "val", ign: "x"},ign: [1,2,3]}
 * nestedChangeRequest: {a:"x",c:{d: "changed"},extra: [1]}
 *
 * Output:{a:"b",c:{d: "val"}}
 */
export function createDiffObject(data, nestedChangeRequest) {
    return Object
        .entries(nestedChangeRequest)
        .reduce((acc, [key, value]) => originalValue(data, acc, key, value), {})
}
