import React from "react";
import PropTypes from "prop-types";
import {Select} from "./../../components";

import "./SelectNewEntityAttribute.scss";

export default class SelectNewEntityAttribute extends React.PureComponent {

  options = (configuration, attributes) => {
    let attr = ["entityid", "notes", "state"];
    if (configuration.title === "saml20_idp") {
      attr = attr.concat(["disableConsent.name", "disableConsent.type", "stepupEntities.name", "mfaEntities.name"])
    } else if (configuration.title === "saml20_sp" || configuration.title === "oidc10_rp") {
      attr = attr.concat(["arp.enabled"].concat(Object.keys(configuration.properties.arp.properties.attributes.properties)
        .map(arpAttr => "arp.attributes." + arpAttr)));
    }
    if (configuration.title === "oidc10_rp") {
      attr = attr.concat(["allowedResourceServers.name"]);
    }
    if (configuration.title !== "oauth20_rs") {
      attr = attr.concat(["allowedEntities.name", "manipulation", "manipulationNotes", "metadataurl", "allowedall"])
    }
    const choosenKeys = Object.keys(attributes);
    return attr.filter(a => choosenKeys.indexOf(a) < 0)
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


