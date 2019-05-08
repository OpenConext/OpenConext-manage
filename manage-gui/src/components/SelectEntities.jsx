import React from "react";
import PropTypes from "prop-types";
import { Select } from "../components";

import "./SelectEntities.css";

export default class SelectEntities extends React.PureComponent {

    renderOption = option => {
        return (
            <span className="select-option">
                <span className="select-label">
                    {`${option.label}  -  ${option.value} (${option.state})`}
                </span>
            </span>
        );
    };

    options = (whiteListing, allowedEntities =[]) => whiteListing
        .map(entry => {
            const metaDataFields = entry.data.metaDataFields;
            const value = entry.data.entityid;
            return {
                value: value,
                label: (metaDataFields["name:en"] || metaDataFields["name:nl"] || value),
                state: entry.data.state
            };
        })
        .filter(entry => allowedEntities.indexOf(entry.value) === -1);

    render() {
        const {onChange, whiteListing, allowedEntities = [], placeholder} = this.props;
        return <Select className="select-state"
                       onChange={option => {
                           if (option) {
                               onChange(option.value);
                           }
                       }}
                       optionRenderer={this.renderOption}
                       options={this.options(whiteListing, allowedEntities.map(entity => entity.name))}
                       value={null}
                       searchable={true}
                       valueRenderer={this.renderOption}
                       placeholder={placeholder || "Select..."}/>;
    }


}

SelectEntities.propTypes = {
    onChange: PropTypes.func.isRequired,
    whiteListing: PropTypes.array.isRequired,
    allowedEntities: PropTypes.array.isRequired,
    placeholder: PropTypes.string
};
