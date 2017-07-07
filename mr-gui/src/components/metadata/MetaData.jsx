import React from "react";
import I18n from "i18n-js";
import PropTypes from "prop-types";
import ReactTooltip from "react-tooltip";

import SelectEnum from "./SelectEnum";
import FormatInput from "./../FormatInput";
import CheckBox from "./../CheckBox";
import SelectNewMetaDataField from "./SelectNewMetaDataField";


import "./MetaData.css";

const patternPropertyRegex = /\^(.*)(\(.*?\))(.*)\$/g

export default class MetaData extends React.PureComponent {

    constructor(props) {
        super(props);
        this.state = {};
    }

    componentDidMount() {
        window.scrollTo(0, 0);
    }

    //React decides not to -re-render on metadata value changes due to derived values, but the props are consistently different
    shouldComponentUpdate = (nextProps, nextState) => true;

    onChange = key => value => this.doChange(key, value);

    onChangeInputEvent = key => e => this.doChange(key, e.target.value);

    //Legacy issue with current metadata
    onChangeCheckEvent = key => e => this.doChange(key, e.target.checked ? "1" : "0");

    doChange = (key, value) => this.props.onChange(`data.metaDataFields.${key}`, value);

    renderMetaDataValue = (key, value, keyConfiguration) => {
        if (keyConfiguration.type === "string") {
            if (!keyConfiguration.format && !keyConfiguration.enum) {
                return <input type="text" name={key} value={value} onChange={this.onChangeInputEvent(key)}/>
            } else if (keyConfiguration.enum) {
                return <SelectEnum onChange={this.onChange(key)} state={value}
                                            enumValues={keyConfiguration.enum} disabled={false}/>
            } else if (keyConfiguration.format) {
                return <FormatInput name={key} input={value} format={keyConfiguration.format}
                                    onChange={this.onChange(key)}/>
            }
        } else if (keyConfiguration.type === "boolean") {
            return <CheckBox onChange={this.onChangeCheckEvent(key)} value={value === "1" ? true : false} name={key}/>
        }
        throw new Error("Unsupported metaData key configuration " + JSON.stringify(keyConfiguration));
    };

    metaDataFieldConfiguration = (key, configuration) => {
        const patternProperties = Object.keys(configuration.properties.metaDataFields.patternProperties);
        let keyConf = configuration.properties.metaDataFields.properties[key];
        if (!keyConf) {
            const patternKey = patternProperties.find(property => {
                patternPropertyRegex.lastIndex = 0;
                const patternResults = patternPropertyRegex.exec(property);
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

    renderMetaDataRow = (key, metaDataFields, configuration) => {
        const keyConfiguration = this.metaDataFieldConfiguration(key, configuration);
        const toolTip = keyConfiguration.info;
        return <tr key={key}>
            <td className="key">{key}
                {toolTip && <span><i className="fa fa-info-circle" data-for={key} data-tip></i>
                <ReactTooltip id={key} type="light" class="tool-tip" effect="solid">
                    <span>{toolTip}</span>
                </ReactTooltip>
                    </span>}
            </td>
            <td className="value">{this.renderMetaDataValue(key, metaDataFields[key], keyConfiguration)}</td>
        </tr>;
    };


    renderMetaDataFields = (keys, metaDataFields, configuration) => {
        return <table className="metadata-fields-table">
            <thead>
            <tr>
                <th className="key">{I18n.t("metaDataFields.key")}</th>
                <th className="value">{I18n.t("metaDataFields.value")}</th>
            </tr>
            </thead>
            <tbody>
            {keys.map(key => this.renderMetaDataRow(key, metaDataFields, configuration))}

            </tbody>
        </table>
    };

    render() {
        const {metaDataFields, configuration, name} = this.props;
        const keys = Object.keys(metaDataFields).sort();
        return (
            <div className="metadata-metadata">
                <div className="metadata-info">
                    <h2>
                        {I18n.t("metaDataFields.title", {name: name})}
                    </h2>
                </div>
                {this.renderMetaDataFields(keys, metaDataFields, configuration)}
                <SelectNewMetaDataField metaDataFields={metaDataFields} configuration={configuration}
                                        placeholder={I18n.t("metaDataFields.placeholder")}
                                        onChange={value => {
                    debugger;
                }}/>
            </div>
        );
    }
}

MetaData.propTypes = {
    onChange: PropTypes.func.isRequired,
    metaDataFields: PropTypes.object.isRequired,
    name: PropTypes.string.isRequired,
    configuration: PropTypes.object.isRequired
};

