const patternPropertyRegex = /\^(.*)(\(.*?\))(.*)\$/g;
const languagePropertyRegex = /\^(.*):\(([a-z|]{0,24})\)\$/g;
const multiplicityRegex = /.*:(\d{1,2})[:]{0,1}.*/;

const addMissingProperty = (accumulator, patternPropertyKey, patternProperty, metaDataKeys) => {
    patternPropertyRegex.lastIndex = 0;
    languagePropertyRegex.lastIndex = 0;
    multiplicityRegex.lastIndex = 0;

    const regExp = new RegExp(patternPropertyKey);
    const existingMetaDataKeys = metaDataKeys.filter(metaDataKey => regExp.test(metaDataKey));
    const languagePropertyExec = languagePropertyRegex.exec(patternPropertyKey);
    if (existingMetaDataKeys.length === 0) {
        //translate the patternPropertyKey to a metaDataKey and add it to the accumulator
        if (patternProperty.multiplicity) {
            //for some we want to add only one - acs locations 0 - and for some we need to add them all - contacts telephone
            if (patternProperty.sibblingIndependent && patternProperty.multiplicity) {
                for (let count = (patternProperty.startIndex || 0); count < patternProperty.multiplicity; count++) {
                    const newKey = patternPropertyKey.replace(patternPropertyRegex, `$1${count}$3`);
                    if (existingMetaDataKeys.indexOf(newKey) === -1) {
                        accumulator.push(newKey);
                    }
                }
            } else {
                const newMetaDataKey = patternPropertyKey.replace(patternPropertyRegex, `$1${patternProperty.startIndex || 0}$3`);
                accumulator.push(newMetaDataKey);
            }
        } else if (languagePropertyExec) {
            const languages = languagePropertyExec[2].split("|");
            languages.forEach(lang => accumulator.push(`${languagePropertyExec[1]}:${lang}`))
        } else {
            throw new Error("Not supported patternProperty " + patternPropertyKey);
        }
    } else {
        //find the highest cardinal
        if (patternProperty.multiplicity) {
            if (existingMetaDataKeys.length < patternProperty.multiplicity) {
                if (patternProperty.sibblingIndependent) {
                    for (let count = (patternProperty.startIndex || 0); count < patternProperty.multiplicity; count++) {
                        const newKey = patternPropertyKey.replace(patternPropertyRegex, `$1${count}$3`);
                        if (existingMetaDataKeys.indexOf(newKey) === -1) {
                            accumulator.push(newKey);
                        }
                    }
                } else {
                    //add the missing in-between one's and the highest new one - 'Raoul's theorem'
                    let highestMetaDataKey;
                    if (existingMetaDataKeys.length > 9) {
                        const sortedMetaDataKeys = existingMetaDataKeys.sort((a, b) => {
                            const aParsed = multiplicityRegex.exec(a);
                            const bParsed = multiplicityRegex.exec(b);
                            const aInt = parseInt(aParsed[1], 10);
                            const bInt = parseInt(bParsed[1], 10);
                            return aInt - bInt;
                        });
                        highestMetaDataKey = sortedMetaDataKeys[existingMetaDataKeys.length - 1];
                    } else {
                        highestMetaDataKey = existingMetaDataKeys.sort()[existingMetaDataKeys.length - 1];
                    }
                    const multiplicityParsed = multiplicityRegex.exec(highestMetaDataKey);
                    const currentlyHighest = parseInt(multiplicityParsed[1], 10);

                    const highestMultiplicity = (patternProperty.multiplicity - 1 + (patternProperty.startIndex || 0));
                    const highestAllowed = Math.min(highestMultiplicity, (currentlyHighest + 1 + (patternProperty.startIndex || 0)));

                    for (let count = (patternProperty.startIndex || 0); count <= highestAllowed; count++) {
                        const newKey = patternPropertyKey.replace(patternPropertyRegex, `$1${count}$3`);
                        if (existingMetaDataKeys.indexOf(newKey) === -1) {
                            accumulator.push(newKey);
                        }
                    }
                }
            }
        } else if (languagePropertyExec) {
            const languages = languagePropertyExec[2].split("|");
            languages
                .filter(lang => !existingMetaDataKeys.includes(`${languagePropertyExec[1]}:${lang}`))
                .map(lang => accumulator.push(`${languagePropertyExec[1]}:${lang}`));
        } else {
            throw new Error("Not supported patternProperty " + patternPropertyKey);
        }
    }
    return accumulator;
};

export const options = (configuration, metaDataFields) => {
    /*
     * First get all the keys from the configuration for which there is no metaDataField
     */
    const metaDataKeys = Object.keys(metaDataFields);
    const properties = Object.keys(configuration.properties.metaDataFields.properties);
    const missingProperties = properties.filter(property => metaDataKeys.indexOf(property) === -1);

    /*
     * Do the same for the patternProperties
     */
    const patternProperties = configuration.properties.metaDataFields.patternProperties;
    const patternPropertiesKeys = Object.keys(patternProperties);
    const missingPatternProperties = patternPropertiesKeys
        .reduce((accumulator, patternPropertyKey) => {
            let patternProperty = patternProperties[patternPropertyKey];
            const ref = patternProperty["$ref"];
            if (ref) {
                //"#/definitions/AssertionConsumerServiceBinding"
                const refDefinition = configuration.definitions[ref.substring(ref.lastIndexOf("/") + 1)];
                patternProperty = {...refDefinition, ...patternProperty};
            }
            return addMissingProperty(accumulator, patternPropertyKey, patternProperty, metaDataKeys);
        }, []);
    return missingProperties.concat(missingPatternProperties).map(prop => {
        return {value: prop, label: prop};
    }).sort((a, b) => a.value.toLowerCase().localeCompare(b.value.toLowerCase()));
};

