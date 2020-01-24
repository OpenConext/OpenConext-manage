import React from "react";
import I18n from "i18n-js";
import PropTypes from "prop-types";
import ReactTooltip from "react-tooltip";

import CheckBox from "./../../components/CheckBox";
import SelectSource from "./SelectSource";
import {copyToClip, isEmpty} from "../../utils/Utils";

import "./ARP.css";

//PureComponent only does a shallow comparison and we use derived values from deeply nested objects
export default class ARP extends React.Component {

  constructor(props) {
    super(props);
    this.state = {
      addInput: false,
      keyForNewInput: undefined,
      value: "",
      newArpAttributeAddedKey: undefined,
      copiedToClipboardClassName: ""
    };
  }

  componentDidMount() {
    window.scrollTo(0, 0);
  }

  componentDidUpdate = () => {
    const {addInput, keyForNewInput, value} = this.state;
    if (addInput && keyForNewInput && this.newAttributeValue && value === "") {
      this.newAttributeValue.focus();
    }
    if (this.state.newArpAttributeAddedKey) {
      setTimeout(() => this.setState({newArpAttributeAddedKey: undefined}), 150);
    }
  };

  onChange = (name, value) => {
    const cleansedName = `data.arp.attributes.${name.replace(/\./g, "@")}`;
    if (Array.isArray(value) && value.length > 0) {
      this.props.onChange(["data.arp.enabled", cleansedName], [true, value], true);
    } else {
      this.props.onChange(cleansedName, value, true);
    }

  };

  nameOfKey = (display, key) => (display || key.substring(key.lastIndexOf(":") + 1)).toLocaleLowerCase();

  copyToClipboard = () => {
    copyToClip("arp-attributes-printable");
    this.setState({copiedToClipboardClassName: "copied"});
    setTimeout(() => this.setState({copiedToClipboardClassName: ""}), 5000);
  };

  arpEnabled = e => {
    const noArp = e.target.checked;
    this.props.onChange(["data.arp.enabled", "data.arp.attributes"], [!noArp, {}]);
  };

  sortAttributeConfigurationKeys = (arpConfAttributes, attributes) => (aKey, bKey) => {
    const a = arpConfAttributes[aKey];
    const b = arpConfAttributes[bKey];
    const aEnabled = !isEmpty(attributes[aKey]);
    const bEnabled = !isEmpty(attributes[bKey]);

    const nameOfA = this.nameOfKey(a.display, aKey);
    const nameOfB = this.nameOfKey(b.display, bKey);

    if (aEnabled && !bEnabled) {
      return -1;
    }
    if (!aEnabled && bEnabled) {
      return 1;
    }
    if (a.deprecated && b.deprecated) {
      return nameOfA.localeCompare(nameOfB);
    }
    if (a.deprecated && !b.deprecated) {
      return 1;
    }
    if (!a.deprecated && b.deprecated) {
      return -1;
    }
    return nameOfA.localeCompare(nameOfB);
  };

  onChangeArp = (property, key, index) => value => {
    const currentArpValues = [...this.props.arp.attributes[key]];
    currentArpValues[index][property] = value;
    this.onChange(key, currentArpValues);
  };

  enableArpKey = key => () => {
    const arpConf = this.props.arpConfiguration.properties.attributes.properties[key];
    if (arpConf.multiplicity) {
      this.setState({addInput: true, keyForNewInput: key});
    } else {
      this.onChange(key, [{value: "*", source: "idp", motivation: ""}]);
      this.setState({newArpAttributeAddedKey: key});
    }
  };

  attributeInputValueBlur = key => () => {
    const value = this.state.value;
    if (isEmpty(value)) {
      this.setState({addInput: false, keyForNewInput: undefined, value: ""});
    } else {
      const currentArpValues = [...this.props.arp.attributes[key] || []];
      currentArpValues.push({value: value, source: "idp", motivation: ""});
      this.setState({addInput: false, keyForNewInput: undefined, value: "", newArpAttributeAddedKey: key});
      this.onChange(key, currentArpValues);
    }
  };

  motivationChange = key => e => {
    const value = e.target.value;
    const currentArpValues = [...this.props.arp.attributes[key] || []];
    const newArpValues = currentArpValues.map(arpValue => (
      {value: arpValue.value, source: arpValue.source, motivation: value}
    ));
    this.onChange(key, newArpValues);
  };


  renderEnabledCell = (sources, attributeKey, attributeValues, guest) => {
    const {addInput, keyForNewInput} = this.state;
    const doAddInput = (addInput && keyForNewInput === attributeKey);
    return (
      <ul className="values">
        {attributeValues.map((attributeValue, index) =>
          <li key={`${attributeValue.value}-${index}`}>
            <CheckBox name={`${attributeKey}_${attributeValue.value}_${index}`} value={true}
                      onChange={() => {
                        const currentArpValues = [...this.props.arp.attributes[attributeKey]];
                        currentArpValues.splice(index, 1);
                        this.onChange(attributeKey,
                          currentArpValues.length === 0 ? null : currentArpValues)
                      }} readOnly={guest}
                      info={attributeValue.value}/>
          </li>)}
        {(attributeValues.length === 0 && !doAddInput) &&
        <li>
          <CheckBox name={attributeKey} value={false}
                    onChange={this.enableArpKey(attributeKey)} readOnly={guest}/>
        </li>}
      </ul>);
  };

  onKeyUp = key => e => {
    if (e.keyCode === 13) {//enter
      this.attributeInputValueBlur(key)();
    }
    if (e.keyCode === 27) {//esc
      this.setState({addInput: false, keyForNewInput: undefined, value: ""});
    }
    return true;
  };

  renderValueCellWithInput = (key, index, display) => {
    const {value} = this.state;
    return (<tr className={index % 2 === 0 ? "even" : "odd"}>
      <td className="new-attribute-value"
          colSpan={2}>{I18n.t("arp.new_attribute_value", {key: this.nameOfKey(key, display)})}</td>
      <td><input ref={ref => this.newAttributeValue = ref}
                 type="text" onKeyUp={this.onKeyUp(key)}
                 onChange={e => this.setState({value: e.target.value})}
                 value={value} onBlur={this.attributeInputValueBlur(key)}/></td>
      <td colSpan={2}></td>
    </tr>);
  };

  matchingRule = value => {
    if (!isEmpty(value) && value.trim() === "*") {
      return I18n.t("arp.wildcard");
    }
    if (!isEmpty(value) && value.trim().endsWith("*")) {
      return I18n.t("arp.prefix");
    }
    return I18n.t("arp.exact");
  };

  renderMatchingRulesCell = (sources, key, attributeValues, guest) =>
    <ul className="matching_rules">
      {attributeValues.map((attributeValue, index) =>
        <li key={`${attributeValue.value}-${index}`}>
          <span>{this.matchingRule(attributeValue.value)}</span>
        </li>)}
    </ul>;

  renderSourceCell = (sources, key, attributeValues, guest) => {
    return <ul className="sources">
      {attributeValues.map((attributeValue, index) =>
        <li key={`${attributeValue.source}-${index}`}>
          <SelectSource onChange={this.onChangeArp("source", key, index)} sources={sources}
                        source={attributeValue.source} disabled={guest}/>
        </li>)}
    </ul>;
  };

  addArpAttributeValue = key => () => this.setState({addInput: true, keyForNewInput: key});

  renderActionsCell = (key, guest) =>
    <span onClick={this.addArpAttributeValue(key)}><i className="fa fa-plus"></i></span>;


  renderAttributeRow = (sources, attributeKey, attributeValues, configurationAttributes, arpAttributes, guest) => {
    const display = (arpAttributes[attributeKey] || {}).display
    let displayKey = display || attributeKey.substring(attributeKey.lastIndexOf(":") + 1);
    const arpAttribute = arpAttributes[attributeKey];
    const deprecated = arpAttribute.deprecated;
    const description = arpAttribute.description;

    displayKey += description ? ` (${description})` : "";
    displayKey += deprecated ? " - deprecated" : "";

    const renderAction = arpAttribute.multiplicity && !guest;
    const {addInput, keyForNewInput} = this.state;
    const doAddInput = (addInput && keyForNewInput === attributeKey);
    //id's need to be unique for our checkboxes to work
    const reactTooltipId = `${attributeKey}_tooltip`;

    return <tbody key={attributeKey}>
    <tr>
      <td className={`name ${deprecated ? "deprecated" : ""}`}><span
        className="display-name">{displayKey}</span><i className="fa fa-info-circle"
                                                       data-for={reactTooltipId} data-tip></i>
        <ReactTooltip id={reactTooltipId} type="info" class="tool-tip" effect="solid">
          <span>{attributeKey}</span>
        </ReactTooltip>
      </td>
      <td className="source">{this.renderSourceCell(sources, attributeKey, attributeValues, guest)}</td>
      <td className="enabled">{this.renderEnabledCell(sources, attributeKey, attributeValues, guest)}</td>
      <td className="matching_rule">{this.renderMatchingRulesCell(sources, attributeKey, attributeValues, guest)}</td>
      <td
        className="action">{renderAction && arpAttribute.multiplicity && this.renderActionsCell(attributeKey, guest)}</td>
    </tr>
    {(!doAddInput && attributeValues.length > 0) &&
    <tr>
      <td className="new-attribute-value"
          colSpan={2}>{I18n.t("arp.new_attribute_motivation", {key: this.nameOfKey(display, attributeKey)})}</td>
      <td colSpan={3}><input
        ref={ref => {
          if (this.state.newArpAttributeAddedKey === attributeKey) {
            setTimeout(() => ref.focus(), 75);
          }
        }}
        type="text" value={attributeValues[0].motivation} className="motivation"
        onChange={this.motivationChange(attributeKey)}
        placeholder={I18n.t("arp.new_attribute_motivation_placeholder")}/></td>
    </tr>}
    </tbody>
  };

  renderArpAttributesTable = (arp, onChange, arpConfiguration, guest) => {
    const arpConfAttributes = arpConfiguration.properties.attributes.properties;
    const configurationAttributes = Object.keys(arpConfAttributes);
    configurationAttributes.sort(this.sortAttributeConfigurationKeys(arpConfAttributes, arp.attributes));

    const sources = arpConfiguration.sources;
    const headers = ["name", "source", "enabled", "matching_rule", "action"];

    return <table className="arp-attributes">
      <thead>
      <tr>
        {headers.map(td => <th className={td} key={td}>{I18n.t(`arp.${td}`)}</th>)}
      </tr>
      </thead>
      {configurationAttributes.map((attrKey, index) => {
        const {addInput, keyForNewInput} = this.state;
        const doAddInput = addInput && keyForNewInput === attrKey;
        const rows = [this.renderAttributeRow(
            sources, attrKey, arp.attributes[attrKey] || [],
            configurationAttributes, arpConfAttributes, guest)];
        if (doAddInput) {
          rows.push(this.renderValueCellWithInput(attrKey, index, arpConfAttributes[attrKey].display));
        }
        return rows;
      })}
    </table>
  };

  renderArpAttributesTablePrintable = (arp) =>
    <section id="arp-attributes-printable"
             className="arp-attributes-printable">
      {
        Object.keys(arp.attributes)
          .map(attr => `${attr}\t${arp.attributes[attr].filter(val => val.value !== "*").map(val => val.value).join(",")}`).join("\n")
      }
    </section>;

  render() {
    const {arp, onChange, arpConfiguration, guest} = this.props;
    const {copiedToClipboardClassName} = this.state;
    const sanitizedArp = isEmpty(arp) ? {attributes: {}} : arp;
    return (
      <div className="metadata-arp">
        <section className="options">
          <CheckBox name="arp-enabled" value={!sanitizedArp.enabled}
                    onChange={this.arpEnabled} readOnly={guest}
                    info={I18n.t("arp.arp_enabled")}/>
          <span className={`button green ${copiedToClipboardClassName}`} onClick={this.copyToClipboard}>
                        {I18n.t("clipboard.copy")}<i className="fa fa-clone"></i>
                    </span>
        </section>
        <section className="attributes">
          <h2>{I18n.t("arp.attributes")}</h2>
          {this.renderArpAttributesTable(sanitizedArp, onChange, arpConfiguration, guest)}
          {this.renderArpAttributesTablePrintable(sanitizedArp)}
        </section>
      </div>
    );
  }

}

ARP.defaultProps = {
  arp: {}
}

ARP.propTypes = {
  arp: PropTypes.object,
  onChange: PropTypes.func.isRequired,
  arpConfiguration: PropTypes.object.isRequired,
  guest: PropTypes.bool.isRequired
};
