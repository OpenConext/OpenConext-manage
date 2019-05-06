import React from "react";
import I18n from "i18n-js";
import PropTypes from "prop-types";
import ReactTooltip from "react-tooltip";
import scrollIntoView from "scroll-into-view";

import {
  MultipleStrings,
  SelectEnum,
  SelectNewMetaDataField
} from "../metadata";

import FormatInput from "./../FormatInput";
import CheckBox from "./../CheckBox";

import { isEmpty } from "../../utils/Utils";
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
      this.setState({ newMetaDataFieldKey: null });
    }
  };

  onError = key => value => this.props.onError(key, value);

  doChange = (key, value) => {
    this.props.onChange(`data.metaDataFields.${key}`, value);
    this.props.onError(key, this.isRequired(key, value));
  };

  newMetaDataFieldRendered = (ref, autoFocus) => {
    if (autoFocus) {
      this.newMetaDataField = ref;
    }
  };

  isRequired = (key, value) =>
    isEmpty(value) &&
    this.props.configuration.properties.metaDataFields.required.indexOf(key) >
      -1;

  renderMetaDataValue = (key, value, keyConfiguration, guest) => {
    const autoFocus = this.state.newMetaDataFieldKey === key;

    const stringInput = () => (
      <input
        ref={ref => this.newMetaDataFieldRendered(ref, autoFocus)}
        type="text"
        name={key}
        value={value}
        onChange={e => this.doChange(key, e.target.value)}
        disabled={guest}
      />
    );

    const multipleStringsInput = () => (
      <MultipleStrings
        autofocus={autoFocus}
        onChange={values => this.doChange(key, values)}
        values={value}
        disabled={guest}
      />
    );

    const selectInput = () => (options, multiple = false) => (
      <SelectEnum
        multiple={multiple}
        autofocus={autoFocus}
        onChange={value => this.doChange(key, value)}
        state={value}
        enumValues={options}
        disabled={guest}
      />
    );

    const formatInput = () => (
      <FormatInput
        autofocus={autoFocus}
        name={key}
        input={value}
        format={keyConfiguration.format}
        onChange={value => this.doChange(key, value)}
        onError={this.onError(key)}
        isError={(this.props.errors[key] && !isEmpty(value)) || false}
        readOnly={guest}
      />
    );

    const booleanInput = () => (
      <CheckBox
        autofocus={autoFocus}
        onChange={e => this.doChange(key, e.target.checked)}
        value={value}
        name={key}
        readOnly={guest}
      />
    );

    switch (keyConfiguration.type) {
      case "boolean":
        return booleanInput();
      case "array":
        return keyConfiguration.items.enum
          ? selectInput(keyConfiguration.items.enum, true)
          : multipleStringsInput();
      default:
        if (keyConfiguration.enum) return selectInput(keyConfiguration.enum);
        if (keyConfiguration.format) return formatInput();
        return stringInput();
    }
  };

  metaDataFieldConfiguration = (key, configuration) => {
    const patternProperties = Object.keys(
      configuration.properties.metaDataFields.patternProperties
    );
    let keyConf = configuration.properties.metaDataFields.properties[key];
    if (!keyConf) {
      const patternKey = patternProperties.find(property =>
        new RegExp(property).test(key)
      );
      keyConf =
        configuration.properties.metaDataFields.patternProperties[patternKey];
    }
    if (!keyConf) {
      return "unknown_key_conf";
    }
    const ref = keyConf["$ref"];
    if (ref) {
      //"#/definitions/AssertionConsumerServiceBinding"
      const keyConfRef = {
        ...configuration.definitions[ref.substring(ref.lastIndexOf("/") + 1)]
      };
      if (keyConf.info) {
        keyConfRef.info = keyConf.info;
      }
      return keyConfRef;
    }
    return keyConf;
  };

  renderMetaDataRow = (key, metaDataFields, configuration, guest) => {
    const keyConfiguration = this.metaDataFieldConfiguration(
      key,
      configuration
    );
    const extraneous = keyConfiguration === "unknown_key_conf";
    const toolTip = keyConfiguration.info;
    const value = metaDataFields[key];
    const required = this.isRequired(key, value);
    return (
      <tr key={key}>
        <td className={`key ${extraneous ? "extraneous" : ""}`}>
          {key}
          {toolTip && (
            <span>
              <i className="fa fa-info-circle" data-for={key} data-tip />
              <ReactTooltip
                id={key}
                type="info"
                class="tool-tip"
                effect="solid"
              >
                <span>{toolTip}</span>
              </ReactTooltip>
            </span>
          )}
        </td>
        <td colSpan={guest ? 2 : 1} className="value">
          {this.renderMetaDataValue(key, value, keyConfiguration, guest)}
          {required && (
            <div className="error">
              {I18n.t("metadata.required", { name: key })}
            </div>
          )}
          {extraneous && (
            <div className="error">
              {I18n.t("metadata.extraneous", { name: key })}
            </div>
          )}
        </td>
        {!guest &&
          configuration.properties.metaDataFields.required.indexOf(key) < 0 && (
            <td className="trash">
              <span onClick={this.deleteMetaDataField(key)}>
                <i className="fa fa-trash-o" />
              </span>
            </td>
          )}
      </tr>
    );
  };

  deleteMetaDataField = key => () => {
    const indexOf = this.state.newAddedMetaData.indexOf(key);
    if (indexOf > -1) {
      const newAddedMetaData = [...this.state.newAddedMetaData];
      newAddedMetaData.splice(indexOf, 1);
      this.setState({ newAddedMetaData: newAddedMetaData });
    }
    this.props.onError(key, false);
    this.doChange(key, null);
  };

  renderMetaDataFields = (
    keys,
    metaDataFields,
    configuration,
    newAddedMetaData,
    guest
  ) => {
    const existingKeys = keys.filter(
      key => newAddedMetaData.indexOf(key) === -1
    );
    return (
      <table className="metadata-fields-table">
        <thead>
          <tr>
            <th className="key">{I18n.t("metaDataFields.key")}</th>
            <th className="value">{I18n.t("metaDataFields.value")}</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {existingKeys.map(key =>
            this.renderMetaDataRow(key, metaDataFields, configuration, guest)
          )}
          {newAddedMetaData.map(key =>
            this.renderMetaDataRow(key, metaDataFields, configuration, guest)
          )}
          <tr>
            <td colSpan={2}>
              {!guest &&
                this.renderMetaDataSearch(metaDataFields, configuration)}
            </td>
            <td />
          </tr>
        </tbody>
      </table>
    );
  };

  getDefaultValueForKey = (key, configuration) => {
    const keyConf = this.metaDataFieldConfiguration(key, configuration);

    if (keyConf.type === "boolean") return keyConf.default;
    if (keyConf.format === "date-time") return new Date().toISOString();

    return keyConf.default || "";
  };

  renderMetaDataSearch = (metaDataFields, configuration) => (
    <SelectNewMetaDataField
      metaDataFields={metaDataFields}
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
      }}
    />
  );

  render() {
    const { metaDataFields, configuration, name, guest } = this.props;
    const { newAddedMetaData } = this.state;
    const keys = Object.keys(metaDataFields).sort((a, b) =>
      a.toLowerCase().localeCompare(b.toLowerCase())
    );
    return (
      <div className="metadata-metadata">
        <div className="metadata-info">
          <h2>{I18n.t("metaDataFields.title", { name: name })}</h2>
        </div>
        {this.renderMetaDataFields(
          keys,
          metaDataFields,
          configuration,
          newAddedMetaData,
          guest
        )}
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
