export const flattenObjectEntries = (inputEntries, keyPrefix) =>
    inputEntries.reduce((acc, curr) => {
        const [currKey, currValue] = curr;
        const currKeyWithPrefix = keyPrefix ? `${keyPrefix}.${currKey}` : currKey

        if (Array.isArray(currValue)) {
            console.error(`Arrays are currently not supported, skips processing the value of "${currKey}"`);
            acc.push([currKey, currValue]);
            return acc;
        }

        if (typeof currValue === "object") {
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
