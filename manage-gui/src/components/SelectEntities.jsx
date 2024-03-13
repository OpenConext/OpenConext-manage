import React from "react";
import PropTypes from "prop-types";
import {Select} from "../components";

import "./SelectEntities.scss";
import {getLanguage} from "../utils/Language";

export default class SelectEntities extends React.PureComponent {

    options() {
        const {whiteListing, allowedEntities} = this.props;
        const allowedEntityNames = allowedEntities.map(entity => entity.name);

        return whiteListing
            .map(entry => {
                const {
                    metaDataFields: {"name:en": eng, "name:nl": nl},
                    entityid: value,
                    state,
                    _id
                } = entry.data;

                return {
                    label: `${getLanguage() === "en" ? (eng || nl || value) : (nl || eng || value)} - ${value} (${state})`,
                    state,
                    value,
                    _id
                };
            })
            .filter(entry => !allowedEntityNames.includes(entry.value));
    }

    render() {
        const {onChange, placeholder} = this.props;

        return (
            <Select
                onChange={option => onChange(option.value)}
                options={this.options()}
                placeholder={placeholder || "Select..."}
                searchable={true}
                value={null}
            />
        );
    }
}

SelectEntities.defaultProps = {
    allowedEntities: []
};

SelectEntities.propTypes = {
    onChange: PropTypes.func.isRequired,
    whiteListing: PropTypes.array.isRequired,
    allowedEntities: PropTypes.array.isRequired,
    placeholder: PropTypes.string
};
