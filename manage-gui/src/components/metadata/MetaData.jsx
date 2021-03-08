import React from "react";
import I18n from "i18n-js";
import PropTypes from "prop-types";
import ReactTooltip from "react-tooltip";
import scrollIntoView from "scroll-into-view";

import {validation} from "./../../api";
import {Boolean, Number, SelectMulti, SelectOne, String, Strings, StringWithFormat} from "../form";
import {SelectNewMetaDataField} from "../metadata";
import {isEmpty} from "../../utils/Utils";

import "./MetaData.css";
import ScopeSelection from "../form/ScopeSelection";

export default class MetaData extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      newMetaDataFieldKey: null,
      newAddedMetaData: [],
      requiredFields: []
    };
  }

  componentDidMount() {
    window.scrollTo(0, 0);

    this.setState({
      requiredFields: this.props.configuration.properties.metaDataFields
        .required
    });
  }

  componentDidUpdate = () => {
    const newMetaDataFieldKey = this.state.newMetaDataFieldKey;

    if (!isEmpty(newMetaDataFieldKey) && !isEmpty(this.newMetaDataField)) {
      scrollIntoView(this.newMetaDataField);
      this.newMetaDataField.focus();
      this.newMetaDataField = null;
      this.setState({newMetaDataFieldKey: null});
    }
  };

  hasError = (key, value = true) => {
    this.props.onError(key, value || undefined);
  }

  validPresence = (key, value) => {
    const isRequired = this.state.requiredFields.includes(key);

    return !isRequired || (isRequired && !isEmpty(value));
  }

  doChange = (key, value) => {
    const valid = this.validPresence(key, value);

    this.hasError(key, !valid);
    this.props.onChange(`data.metaDataFields.${key}`, value);
  }

  async validateFormat(key, value, format) {
    if (isEmpty(value)) {
      return true;
    }

    if (typeof value === "string") {
      const valid = await validation(format, value);

      this.hasError(key, !valid);
    }

    if (Array.isArray(value)) {
      const validationValues = await Promise.all(
        value.map(val => validation(format, val))
      );
      const valid = isEmpty(validationValues.filter(val => !val));

      this.hasError(key, !valid);
    }
  }

  renderMetaDataValue = (key, value, keyConfiguration, guest) => {
    let valueToUse = isEmpty(value) ? "" : value;
    if (valueToUse === "") {
      if (keyConfiguration.type === "array") {
        valueToUse = [];
      } else if (keyConfiguration.type === "boolean") {
        valueToUse = false;
      }
    }

    const defaultProps = {
      autoFocus: this.state.newMetaDataFieldKey === key,
      disabled: guest,
      name: key,
      onChange: value => this.doChange(key, value),
      value: valueToUse
    };

    const hasFormatError = !isEmpty(value) && !isEmpty(this.props.errors[key]);

    switch (keyConfiguration.type) {
      case "boolean":
        return <Boolean {...defaultProps} />;
      case "number":
        return <Number {...defaultProps} />;
      case "array":
        if (key === "scopes") {
          const {isNewEntity, originalEntityId} = this.props;
          return <ScopeSelection {...defaultProps} isNewEntity={isNewEntity} originalEntityId={originalEntityId}/>;
        }

        const options = keyConfiguration.items.enum;
        if (options) {
          return <SelectMulti {...defaultProps} enumValues={options}/>;
        }

        const itemFormat = keyConfiguration.items.format;

        return (
          <Strings
            {...defaultProps}
            format={itemFormat}
            hasFormatError={hasFormatError}
            onChange={value => this.doChange(key, value)}
            onBlur={value => this.validateFormat(key, value, itemFormat)}
          />
        );
      case "string":
        if (keyConfiguration.enum) {
          return (
            <SelectOne {...defaultProps} enumValues={keyConfiguration.enum}/>
          );
        }
        if (keyConfiguration.format) {
          return (
            <StringWithFormat
              {...defaultProps}
              format={keyConfiguration.format}
              minLength={keyConfiguration.minLength}
              hasFormatError={hasFormatError}
              hasError={this.hasError}
              onChange={value => this.doChange(key, value)}
              onBlur={value =>
                this.validateFormat(key, value, keyConfiguration.format)
              }
            />
          );
        }
        return <String {...defaultProps} />;
      default:
        return <String {...defaultProps} />;
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
    //id's need to be unique for our checkboxes to work
    const reactTooltipId = `${key}_tooltip`;
    return (
      <tr key={key}>
        <td className={`key ${extraneous ? "extraneous" : ""}`}>
          {key}
          {toolTip && (
            <span>
              <i
                className="fa fa-info-circle"
                data-for={reactTooltipId}
                data-tip
              />
              <ReactTooltip
                id={reactTooltipId}
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
          {!this.validPresence(key, value) && (
            <div className="error">
              {I18n.t("metadata.required", {name: key})}
            </div>
          )}
          {extraneous && (
            <div className="error">
              {I18n.t("metadata.extraneous", {name: key})}
            </div>
          )}
        </td>
        {!guest &&
        configuration.properties.metaDataFields.required.indexOf(key) < 0 && (
          <td className="trash">
              <span onClick={this.deleteMetaDataField(key)}>
                <i className="fa fa-trash-o"/>
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
      this.setState({newAddedMetaData: newAddedMetaData});
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
          <th/>
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
          <td/>
        </tr>
        </tbody>
      </table>
    );
  };

  getDefaultValueForKey = (key, configuration) => {
    const keyConf = this.metaDataFieldConfiguration(key, configuration);

    if (keyConf.type === "boolean") {
      //https://www.pivotaltracker.com/story/show/175998120
      return true;
    }
    if (keyConf.format === "date-time") {
      return new Date().toISOString();
    }

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
    const {metaDataFields, configuration, name, guest} = this.props;
    const {newAddedMetaData} = this.state;
    const keys = Object.keys(metaDataFields).sort((a, b) =>
      a.toLowerCase().localeCompare(b.toLowerCase())
    );
    return (
      <div className="metadata-metadata">
        <div className="metadata-info">
          <h2>{I18n.t("metaDataFields.title", {name: name})}</h2>
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
  guest: PropTypes.bool.isRequired,
  isNewEntity: PropTypes.bool.isRequired
};
