import React from "react";
import PropTypes from "prop-types";
import {Select} from "./../../components";

export default class SelectOne extends React.PureComponent {
    valueToOption(value) {
        return {value: value, label: value};
    }

    valuesToOptions(values) {
        return values.map(value => this.valueToOption(value));
    }

    render() {
        const {enumValues, onChange, value, ...rest} = this.props;

        return (
            <Select
                {...rest}
                onChange={option => onChange(option.value)}
                optionRenderer={option => option.label}
                options={this.valuesToOptions(enumValues)}
                value={this.valueToOption(value)}
                isSearchable={false}
            />
        );
    }
}

SelectOne.propTypes = {
    enumValues: PropTypes.array.isRequired
};
