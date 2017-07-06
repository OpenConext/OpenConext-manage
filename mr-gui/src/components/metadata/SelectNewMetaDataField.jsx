import React from "react";
import PropTypes from "prop-types";
import Select from "react-select";
import "react-select/dist/react-select.css";
import I18n from "i18n-js";
import "./SelectNewMetaDataField.css";

const patternPropertyRegex = /(.*)(\(.*?\))(.*)/g;
const enumPropertyRegex = /([a-z]{2})\|([a-z]{2})/;

export default class SelectNewMetaDataField extends React.PureComponent {

    options = (configuration, metaDataFields) => {
       /*
        First get all keys from the configuration where there is no metaDataField
         */
       const metaDataKeys = Object.keys(metaDataFields);
       const properties = configuration.properties.metaDataFields.properties;
       const missingProperties = properties.filter(property => metaDataKeys.indexOf(property) === -1);

       const patternProperties = configuration.properties.metaDataFields.patternProperties;
       const missingPatternProperties = patternProperties.filter()

    };

    isMissingProperty = (patternProperty, metaDataKeys) => {
        patternPropertyRegex.lastIndex = 0;
        enumPropertyRegex.lastIndex = 0;

        if (patternProperty.multiplicity) {
            const patternResults = patternPropertyRegex.exec(patternProperty.substring(1, patternProperty.length - 1));
            metaDataKeys.filter(key => key.startsWith(patternResults[1] && key.endsWith(patternResults[3])))
        }
    };

    metaDataFieldConfiguration = (key, configuration) => {
        const patternProperties = Object.keys(configuration.properties.metaDataFields.patternProperties);
        let keyConf = configuration.properties.metaDataFields.properties[key];
        if (!keyConf) {
            const patternKey = patternProperties.find(property => {
                patternPropertyRegex.lastIndex = 0;
                const patternResults = patternPropertyRegex.exec(property.substring(1, property.length - 1));
                return patternResults && key.startsWith(patternResults[1]) && key.endsWith(patternResults[3]);
            });
            keyConf = configuration.properties.metaDataFields.patternProperties[patternKey];
        }
        if (!keyConf) {
            throw new Error("Unsupported metaData key " + key);
        }
        const ref = keyConf["$ref"];
        if (ref) {
            //"#/definitions/AssertionConsumerServiceBinding"
            keyConf = configuration.definitions[ref.substring(ref.lastIndexOf("/") + 1)]
        }
        return keyConf;
    };

    render() {
        const {onChange, configuration, metaDataFields, placeholder} = this.props;
        return <Select className="select-state"
                       onChange={option => onChange(option.value)}
                       options={this.options(configuration, metaDataFields)}
                       value={null}
                       searchable={true}
                       placeholder={placeholder || "Select..."}/>;
    }


}

SelectNewMetaDataField.propTypes = {
    onChange: PropTypes.func.isRequired,
    configuration: PropTypes.object.isRequired,
    metaDataFields: PropTypes.object.isRequired,
    placeholder: PropTypes.string
};


