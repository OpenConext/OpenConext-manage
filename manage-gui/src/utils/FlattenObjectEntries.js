import {isEmpty} from "./Utils";

// Todo remove before PR

export const flattenObjectEntries = (inputEntries, keyPrefix) =>
    inputEntries.reduce((acc, curr) => {
        const [currKey, currValue] = curr;
        const currKeyWithPrefix = keyPrefix ? `${keyPrefix}.${currKey}` : currKey

        if (Array.isArray(currValue)) {
            acc.push([currKeyWithPrefix, JSON.stringify(currValue)]);
            return acc;
        }

        if (typeof currValue === "object" && !isEmpty(currValue)) {
            const nestedInputEntries = Object.entries(currValue);
            acc.push(...flattenObjectEntries(nestedInputEntries, currKeyWithPrefix));
            return acc;
        }

        acc.push([currKeyWithPrefix, currValue]);
        return acc;
    }, []);

export const flattenObject = (inputObject) =>
    Object.fromEntries(flattenObjectEntries(Object.entries(inputObject)))

export const flattenArrayOfObjects = (arrayOfObjects) =>
    arrayOfObjects.map((arrayItem) => flattenObject(arrayItem))
