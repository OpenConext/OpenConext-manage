import React from "react";
import I18n from "i18n-js";
import PropTypes from "prop-types";
import CheckBox from "./../../components/CheckBox";
import SelectSource from "./SelectSource";
import "./ARP.css";

export default class ARP extends React.PureComponent {

    constructor(props) {
        super(props);
        this.state = {};
    }

    componentDidMount() {
        window.scrollTo(0, 0);
    }

    onChange = name => value => {
        this.props.onChange(name, value);
    };

    sortAttributeConfigurationKeys = arpConfiguration => (aKey, bKey) => {
        const a = arpConfiguration.properties.attributes[aKey];
        const b = arpConfiguration.properties.attributes[bKey];
        if (a.deprecated && b.deprecated) {
            return a.localeCompare(b);
        }
        if (a.deprecated && !b.deprecated) {
            return -1;
        }
        if (!a.deprecated && b.deprecated) {
            return 1;
        }
        return a.localeCompare(b);
    };

    renderSourceCell = (sources, key, attributeValue, guest) =>
        <ul>
            {attributeValue.map((source, index) =>
            <li key={`${source}-${index}`}>
                <SelectSource onChange={this.onChange(`data.arp.${key}.`)} sources={sources} source={source} disabled={guest} />
            </li>)}
        </ul>;

    renderAttributeRow = (sources, key, attributeValues, guest) => {
        return <tr>
            <td className="name">{key}</td>
            <td className="source">{attributeValues.map(attributeValue => this.renderSourceCell(sources, key, attributeValue, guest))}</td>
        </tr>
    };

    renderArpAttributesTable = (arp, onChange, arpConfiguration, guest) => {
        const attributeKeys = Object.keys(arp.attributes);
        attributeKeys.sort();
        const configurationAttributes = Object.keys(arpConfiguration.properties.attributes)
            .filter(attr => attributeKeys.indexOf(attr) === -1);
        configurationAttributes.sort(this.sortAttributeConfigurationKeys(arpConfiguration));

        const sources = arpConfiguration.sources;
        const headers = ["name", "source", "value", "matching_rule"];

        return <table className="arp-attributes">
            <thead>
            <tr>
                {headers.map(td => <td className={td} key={td}>{I18n.t(`arp.${td}`)}</td>)}
            </tr>
            </thead>
            <tbody>
            {attributeKeys.map(key => this.renderAttributeRow(sources, key, arp.attributes[key], guest))}
            </tbody>
        </table>
    };


    render() {
        const {arp, onChange, arpConfiguration, guest} = this.props;
        return (
            <div className="metadata-arp">
                <section className="arp-info">
                    <h2>
                        <a href="https://github.com/OpenConext/OpenConext-engineblock/wiki/Attribute-Release-Policy" target="_blank" rel="noopener noreferrer">
                            {I18n.t("arp.description")}
                        </a>
                    </h2>
                </section>
                <section className="enabled">
                    <CheckBox name="arp-enabled" value={arp.enabled}
                              onChange={this.onChange("data.arp.enabled")} readOnly={guest}
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

