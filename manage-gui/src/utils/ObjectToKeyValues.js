export const objectToKeyValues = (inputEntries, keyPrefix) =>
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
            acc.push(...objectToKeyValues(nestedInputEntries, currKeyWithPrefix));
            return acc;
        }

        acc.push([currKeyWithPrefix, currValue]);
        return acc;
    }, []);
