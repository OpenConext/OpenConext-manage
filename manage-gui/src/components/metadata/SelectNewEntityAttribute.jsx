import React from "react";
import PropTypes from "prop-types";
import Select from "react-select";
import "react-select/dist/react-select.css";
import "./SelectNewEntityAttribute.css";

export default class SelectNewEntityAttribute extends React.PureComponent {

    options = (configuration, attributes) => {
        const attr = configuration.title === "saml20_idp" ? ["disableConsent.name", "disableConsent.type"] :
            ["arp.enabled"].concat(Object.keys(configuration.properties.arp.properties.attributes.properties)
                .map(arpAttr => "arp.attributes." + arpAttr));
        const choosenKeys = Object.keys(attributes);
        return attr.concat(["active", "allowedEntities.name", "entityid", "manipulation", "manipulationNotes", "metadataurl", "allowedall", "notes"])
            .filter(a => choosenKeys.indexOf(a) < 0)
            .map(a => ({value: a, label: a}))
            .sort((a, b) => a.value.toLowerCase().localeCompare(b.value.toLowerCase()));
    };

    render() {
        const {onChange, configuration, attributes, placeholder} = this.props;
        return <Select className="select-new-metadata"
                       onChange={option => {
                           if (option) {
                               onChange(option.value);
                           }
                       }}
                       options={this.options(configuration, attributes)}
                       value={null}
                       searchable={true}
                       placeholder={placeholder || "Select..."}/>;
    }

}

SelectNewEntityAttribute.propTypes = {
    onChange: PropTypes.func.isRequired,
    configuration: PropTypes.object.isRequired,
    attributes: PropTypes.object.isRequired,
    placeholder: PropTypes.string
};


