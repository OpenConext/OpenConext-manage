import React from "react";
import I18n from "i18n-js";
import PropTypes from "prop-types";
import ReactTooltip from "react-tooltip";

import CheckBox from "./../../components/CheckBox";
import SelectSource from "./SelectSource";
import {isEmpty} from "../../utils/Utils";

import "./ARP.css";

//PureComponent only does a shallow comparison and we use derived values from deeply nested objects
export default class ARP extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            addInput: false,
            keyForNewInput: undefined,
            value: "",
            newArpAttributeAddedKey: undefined
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
            this.props.onChange(["data.arp.enabled",cleansedName], [true, value], true);
        } else {
            this.props.onChange(cleansedName, value, true);
        }

    };

    nameOfKey = key => key.substring(key.lastIndexOf(":") + 1);

    arpEnabled = e => {
        const noArp = e.target.checked;
        this.props.onChange(["data.arp.enabled", "data.arp.attributes"], [!noArp, {}]);
    };

    sortAttributeConfigurationKeys = (arpConfAttributes, attributes) => (aKey, bKey) => {
        const a = arpConfAttributes[aKey];
        const b = arpConfAttributes[bKey];
        const aEnabled = !isEmpty(attributes[aKey]);
        const bEnabled = !isEmpty(attributes[bKey]);

        if (aEnabled && !bEnabled) {
            return -1;
        }
        if (!aEnabled && bEnabled) {
            return 1;
        }
        if (a.deprecated && b.deprecated) {
            return this.nameOfKey(aKey).localeCompare(this.nameOfKey(bKey));
        }
        if (a.deprecated && !b.deprecated) {
            return 1;
        }
        if (!a.deprecated && b.deprecated) {
            return -1;
        }
        return this.nameOfKey(aKey).localeCompare(this.nameOfKey(bKey));
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
            this.onChange(key, [{value: "*", source: "idp"}]);
            this.setState({newArpAttributeAddedKey: key});
        }
    };

    attributeInputValueBlur = key => () => {
        const value = this.state.value;
        if (isEmpty(value)) {
            this.setState({addInput: false, keyForNewInput: undefined, value: ""});
        } else {
            const currentArpValues = [...this.props.arp.attributes[key] || []];
            currentArpValues.push({value: value, source: "idp"});
            this.setState({addInput: false, keyForNewInput: undefined, value: "", newArpAttributeAddedKey: key});
            this.onChange(key, currentArpValues);
        }
    };

    renderEnabledCell = (sources, attributeKey, attributeValues, guest) => {
        const {addInput, keyForNewInput} = this.state;
        const doAddInput = (addInput && keyForNewInput === attributeKey);
        // const key = attributeKey;//
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

    renderValueCellWithInput = (key, index) => {
        const {value} = this.state;
        return (<tr className={index % 2 === 0 ? "even" : "odd"}>
            <td className="new-attribute-value"
                colSpan={2}>{I18n.t("arp.new_attribute_value", {key: this.nameOfKey(key)})}</td>
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
        const autoFocus = this.state.newArpAttributeAddedKey === key;
        return <ul className="sources">
            {attributeValues.map((attributeValue, index) =>
                <li key={`${attributeValue.source}-${index}`}>
                    <SelectSource onChange={this.onChangeArp("source", key, index)} sources={sources}
                                  source={attributeValue.source} disabled={guest}
                                  autofocus={autoFocus}/>
                </li>)}
        </ul>;
    };

    addArpAttributeValue = key => () => this.setState({addInput: true, keyForNewInput: key});

    renderActionsCell = (key, guest) =>
        <span onClick={this.addArpAttributeValue(key)}><i className="fa fa-plus"></i></span>;


    renderAttributeRow = (sources, attributeKey, attributeValues, configurationAttributes, arpAttributes, guest) => {
        let displayKey = attributeKey.substring(attributeKey.lastIndexOf(":") + 1);
        const arpAttribute = arpAttributes[attributeKey];
        const deprecated = arpAttribute.deprecated;
        const description = arpAttribute.description;

        displayKey += description ? ` (${description})` : "";
        displayKey += deprecated ? " - deprecated" : "";

        const renderAction = arpAttribute.multiplicity && !guest;

        return <tr key={attributeKey}>
            <td className={`name ${deprecated ? "deprecated" : ""}`}><span
                className="display-name">{displayKey}</span><i className="fa fa-info-circle"
                                                               data-for={attributeKey} data-tip></i>
                <ReactTooltip id={attributeKey} type="info" class="tool-tip" effect="solid">
                    <span>{attributeKey}</span>
                </ReactTooltip>
            </td>
            <td className="source">{this.renderSourceCell(sources, attributeKey, attributeValues, guest)}</td>
            <td className="enabled">{this.renderEnabledCell(sources, attributeKey, attributeValues, guest)}</td>
            <td className="matching_rule">{this.renderMatchingRulesCell(sources, attributeKey, attributeValues, guest)}</td>
            <td className="action">{renderAction && arpAttribute.multiplicity && this.renderActionsCell(attributeKey, guest)}</td>
        </tr>
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
            <tbody>
            {configurationAttributes.map(attrKey => {
                const {addInput, keyForNewInput} = this.state;
                const doAddInput = addInput && keyForNewInput === attrKey;
                const rows = [this.renderAttributeRow(
                    sources, attrKey, arp.attributes[attrKey] || [],
                    configurationAttributes, arpConfAttributes, guest)];
                if (doAddInput) {
                    rows.push(this.renderValueCellWithInput(attrKey))
                }
                return rows;
            })}
            </tbody>
        </table>
    };

    render() {
        const {arp, onChange, arpConfiguration, guest} = this.props;
        return (
            <div className="metadata-arp">
                <section className="arp-info">
                    <h2>
                        <a href="https://github.com/OpenConext/OpenConext-engineblock/wiki/Attribute-Release-Policy"
                           target="_blank" rel="noopener noreferrer">
                            {I18n.t("arp.description")}
                        </a>
                    </h2>
                </section>
                <section className="enabled">
                    <CheckBox name="arp-enabled" value={!arp.enabled}
                              onChange={this.arpEnabled} readOnly={guest}
                              info={I18n.t("arp.arp_enabled")}/>
                </section>
                <section className="attributes">
                    <h2>{I18n.t("arp.attributes")}</h2>
                    {this.renderArpAttributesTable(arp, onChange, arpConfiguration, guest)}
                </section>
            </div>
        );
    }

}

ARP.propTypes = {
    arp: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
    arpConfiguration: PropTypes.object.isRequired,
    guest: PropTypes.bool.isRequired
};

