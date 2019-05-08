import React from "react";
import PropTypes from "prop-types";
import { Select } from "./../../components";

import I18n from "i18n-js";
import "./SelectMetaDataType.css";

export default class SelectMetaDataType extends React.PureComponent {

    render() {
        const {onChange, state, configuration, defaultToFirst} = this.props;
        const options = configuration.map(conf => ({value: conf.title, label: I18n.t(`metadata.${conf.title}_single`)}));

        return <Select className="select-metadata-type"
                       onChange={option => {
                           if (option) {
                               onChange(option.value);
                           }
                       }}
                       options={options.sort((a,b)=> a.label.localeCompare(b.label))}
                       value={state ? state : defaultToFirst ? options[0].value : state}
                       searchable={false}/>;
    }
}

SelectMetaDataType.propTypes = {
    onChange: PropTypes.func.isRequired,
    state: PropTypes.string.isRequired,
    configuration: PropTypes.array.isRequired,
    defaultToFirst: PropTypes.bool
};
