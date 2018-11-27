import React from "react";
import I18n from "i18n-js";
import PropTypes from "prop-types";
import ReactTooltip from "react-tooltip";
import scrollIntoView from "scroll-into-view";

import SelectEnum from "./SelectEnum";
import FormatInput from "./../FormatInput";
import CheckBox from "./../CheckBox";
import SelectNewMetaDataField from "./SelectNewMetaDataField";

import {isEmpty} from "../../utils/Utils";
import "./MetaData.css";

//PureComponent only does a shallow comparison and we use derived values from deeply nested objects
export default class MetaData extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            newMetaDataFieldKey: null,
            newAddedMetaData: []
        };
    }

    componentDidMount() {
        window.scrollTo(0, 0);
    }

    componentDidUpdate = () => {
        const newMetaDataFieldKey = this.state.newMetaDataFieldKey;
        if (!isEmpty(newMetaDataFieldKey) && !isEmpty(this.newMetaDataField)) {
            scrollIntoView(this.newMetaDataField);
            this.newMetaDataField.focus();
            this.newMetaDataField = null;
            this.setState({newMetaDataFieldKey: null})
        }
    };

    onChange = key => value => this.doChange(key, value);

    onError = key => value => this.props.onError(key, value);

    onChangeInputEvent = key => e => this.doChange(key, e.target.value);

    //Legacy issue with current metadata
    onChangeCheckEvent = key => e => this.doChange(key, e.target.checked ? "1" : "0");

    doChange = (key, value) => {
        this.props.onChange(`data.metaDataFields.${key}`, value);
        this.props.onError(key, this.isRequired(key, value));
    };

    newMetaDataFieldRendered = (ref, autoFocus) => {
        if (autoFocus) {
            this.newMetaDataField = ref;
        }
    };

    isRequired = (key, value) => isEmpty(value) && this.props.configuration.properties.metaDataFields.required.indexOf(key) > -1;

    renderMetaDataValue = (key, value, keyConfiguration, guest) => {
        const autoFocus = this.state.newMetaDataFieldKey === key;
        const isError = (this.props.errors[key] && !isEmpty(value)) || false;
        if (keyConfiguration.type === "string" && keyConfiguration.format !== "boolean") {
            if (!keyConfiguration.format && !keyConfiguration.enum) {
                return <input ref={ref => this.newMetaDataFieldRendered(ref, autoFocus)} type="text" name={key}
                              value={value} onChange={this.onChangeInputEvent(key)} disabled={guest}/>
            } else if (keyConfiguration.enum) {
                return <SelectEnum autofocus={autoFocus} onChange={this.onChange(key)} state={value}
                                   enumValues={keyConfiguration.enum} disabled={guest}/>
            } else if (keyConfiguration.format) {
                return <FormatInput autofocus={autoFocus}
                                    name={key} input={value} format={keyConfiguration.format}
                                    onChange={this.onChange(key)}
                                    onError={this.onError(key)}
                                    isError={isError}
                                    readOnly={guest}/>
            }
        } else if (keyConfiguration.format === "boolean") {
            return <CheckBox autofocus={autoFocus} onChange={this.onChangeCheckEvent(key)}
                             value={value === "1" ? true : false} name={key} readOnly={guest}/>
        }
        return <input ref={ref => this.newMetaDataFieldRendered(ref, false)} type="text" name={key}
                      value={value} onChange={this.onChangeInputEvent(key)} disabled={guest}/>
    };

    metaDataFieldConfiguration = (key, configuration) => {
        const patternProperties = Object.keys(configuration.properties.metaDataFields.patternProperties);
        let keyConf = configuration.properties.metaDataFields.properties[key];
        if (!keyConf) {
            const patternKey = patternProperties.find(property => new RegExp(property).test(key));
            keyConf = configuration.properties.metaDataFields.patternProperties[patternKey];
        }
        if (!keyConf) {
            return "unknown_key_conf";
        }
        const ref = keyConf["$ref"];
        if (ref) {
            //"#/definitions/AssertionConsumerServiceBinding"
            const keyConfRef = {...configuration.definitions[ref.substring(ref.lastIndexOf("/") + 1)]};
            if (keyConf.info) {
                keyConfRef.info = keyConf.info;
            }
            return keyConfRef;
        }
        return keyConf;
    };

    renderMetaDataRow = (key, metaDataFields, configuration, guest) => {
        const keyConfiguration = this.metaDataFieldConfiguration(key, configuration);
        const extraneous = keyConfiguration === "unknown_key_conf";
        const toolTip = keyConfiguration.info;
        const value = metaDataFields[key];
        const required = this.isRequired(key, value);
        return (<tr key={key}>
            <td className={`key ${extraneous ? "extraneous" : ""}`}>{key}
                {toolTip && <span>
                            <i className="fa fa-info-circle" data-for={key} data-tip></i>
                                <ReactTooltip id={key} type="info" class="tool-tip" effect="solid">
                                    <span>{toolTip}</span>
                                </ReactTooltip>
                        </span>}
            </td>
            <td colSpan={guest ? 2 : 1} className="value">
                {this.renderMetaDataValue(key, value, keyConfiguration, guest)}
                {required && <div className="error">{I18n.t("metadata.required", {name: key})}</div>}
                {extraneous && <div className="error">{I18n.t("metadata.extraneous", {name: key})}</div>}
            </td>
            {(!guest && configuration.properties.metaDataFields.required.indexOf(key) < 0)
            && <td className="trash">
                <span onClick={this.deleteMetaDataField(key)}><i className="fa fa-trash-o"></i></span>
            </td>}
        </tr>);
    };

    deleteMetaDataField = key => () => {
        const indexOf = this.state.newAddedMetaData.indexOf(key);
        if (indexOf > -1) {
            const newAddedMetaData = [...this.state.newAddedMetaData];
            newAddedMetaData.splice(indexOf, 1);
            this.setState({newAddedMetaData: newAddedMetaData});
        }
        this.props.onError(key, false);
        this.doChange(key, null);

    };

    renderMetaDataFields = (keys, metaDataFields, configuration, newAddedMetaData, guest) => {
        const existingKeys = keys.filter(key => newAddedMetaData.indexOf(key) === -1);
        return <table className="metadata-fields-table">
            <thead>
            <tr>
                <th className="key">{I18n.t("metaDataFields.key")}</th>
                <th className="value">{I18n.t("metaDataFields.value")}</th>
                <th></th>
            </tr>
            </thead>
            <tbody>
            {existingKeys.map(key => this.renderMetaDataRow(key, metaDataFields, configuration, guest))}
            {newAddedMetaData.map(key => this.renderMetaDataRow(key, metaDataFields, configuration, guest))}
            <tr>
                <td colSpan={2}>
                    {!guest && this.renderMetaDataSearch(metaDataFields, configuration)}
                </td>
                <td></td>
            </tr>
            </tbody>
        </table>
    };

    getDefaultValueForKey = (key, configuration) => {
        const keyConf = this.metaDataFieldConfiguration(key, configuration);
        if (keyConf.enum) {
            return keyConf.default || "";
        }
        if (keyConf.format === "boolean") {
            return "0";
        }
        if (keyConf.format === "date-time") {
            return new Date().toISOString();
        }
        return "";
    };

    renderMetaDataSearch = (metaDataFields, configuration) =>
        <SelectNewMetaDataField metaDataFields={metaDataFields}
                                configuration={configuration}
                                placeholder={I18n.t("metaDataFields.placeholder")}
                                onChange={value => {
                                    this.doChange(value, this.getDefaultValueForKey(value, configuration));
                                    const newAddedMetaDataState = [...this.state.newAddedMetaData];
                                    newAddedMetaDataState.push(value);
                                    this.setState({
                                        newMetaDataFieldKey: value,
                                        newAddedMetaData: newAddedMetaDataState
                                    });
                                }}/>;


    render() {
        const {metaDataFields, configuration, name, guest} = this.props;
        const {newAddedMetaData} = this.state;
        const keys = Object.keys(metaDataFields).sort((a, b) => a.toLowerCase().localeCompare(b.toLowerCase()));
        return (
            <div className="metadata-metadata">
                <div className="metadata-info">
                    <h2>
                        {I18n.t("metaDataFields.title", {name: name})}
                    </h2>
                </div>
                {this.renderMetaDataFields(keys, metaDataFields, configuration, newAddedMetaData, guest)}
            </div>
        );
    }
}

MetaData.propTypes = {
    onChange: PropTypes.func.isRequired,
    onError: PropTypes.func.isRequired,
    errors: PropTypes.object.isRequired,
    metaDataFields: PropTypes.object.isRequired,
    name: PropTypes.string.isRequired,
    configuration: PropTypes.object.isRequired,
    guest: PropTypes.bool.isRequired
};

