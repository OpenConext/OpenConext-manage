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
    if (!isEmpty(sourceValue) && typeof sourceValue === "object" && !Array.isArray(sourceValue)) {
        acc[key] = {};
        Object.keys(sourceValue)
            .filter(sk => value && (value[sk] ||
                key === "attributes" ||
                key.indexOf("urn:mace") > -1 ||
                key.indexOf("attribute-def") > -1 ))
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

export function sortDict(data) {
    Object.values(data).forEach(value => {
        if (Array.isArray(value)) {
            value.sort((o1, o2) => {
                if (o1 && o1.name && o2 && o2.name) {
                    return o1.name.localeCompare(o2.name);
                }
                if (o1 && o2) {
                    return o1.toString().localeCompare(o2.toString());
                }
                return 0;
            })
        } else if (value && value.constructor === Object) {
            sortDict(value);
        }
    })
}

export function groupBy(arr, property) {
    return arr.reduce((acc, item) => {
        const value = item[property];
        acc[value] = acc[value] ?? [];
        acc[value].push(item);
        return acc;
    }, {});
}

export function groupPolicyAttributes(attributes) {
    return attributes.reduce((acc, item) => {
        const attributeName = item.name;
        const key = `${attributeName}#${item.groupID || 0}`;
        acc[key] = acc[key] || [];
        acc[key].push(item);
        return acc;
    }, {});
}

export function determineStatus(decision) {
    switch (decision) {
        case "Permit":
            return "check";
        case "Indeterminate":
        case "Deny":
            return "remove";
        case "NotApplicable":
            return "question";
        default:
            throw "Unknown decision" + decision;
    }
}




