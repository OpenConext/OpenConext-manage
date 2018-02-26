import escape from "lodash.escape";

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
            if (typeof(val) === "string" || val instanceof String) {
                obj[key] = escape(val);
            } else if (typeof(val) === "object" || val instanceof Object) {
                escapeDeep(val);
            }
        });

    }
}
