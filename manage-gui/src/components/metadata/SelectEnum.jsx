import React from "react";
import PropTypes from "prop-types";
import Select from "react-select";
import "react-select/dist/react-select.css";
import "./SelectEnum.css";

export default class SelectEnum extends React.PureComponent {

    renderOption = option => {
        return (
            <span className="select-option">
                <span className="select-label">
                    {option.label}
                </span>
            </span>
        );
    };

    render() {
        const {onChange, state, enumValues, disabled, autofocus} = this.props;
        const options = enumValues.map(s => {
            return {value: s, label: s};
        });
        return <Select className="select-state"
                       onChange={option => {
                           if (option) {
                               onChange(option.value);
                           }
                       }}
                       optionRenderer={this.renderOption}
                       options={options}
                       value={state}
                       autoFocus={autofocus}
                       searchable={false}
                       valueRenderer={this.renderOption}
                       disabled={disabled || false}/>;
    }
}

SelectEnum.propTypes = {
    onChange: PropTypes.func.isRequired,
    state: PropTypes.string.isRequired,
    enumValues: PropTypes.array.isRequired,
    disabled: PropTypes.bool,
    autofocus: PropTypes.bool
};


