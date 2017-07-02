import React from "react";
import PropTypes from "prop-types";
import Select from "react-select";
import "react-select/dist/react-select.css";
import I18n from "i18n-js";
import "./SelectState.css";

const states = ["prodaccepted", "testaccepted"];

export default class SelectState extends React.PureComponent {

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
        const {onChange, state, disabled} = this.props;
        const options = states.map(s => {
            return {value: s, label: I18n.t(`metadata.${s}`)};
        });
        return <Select className="select-state"
                       onChange={option => onChange(option.value)}
                       optionRenderer={this.renderOption}
                       options={options}
                       value={state}
                       searchable={false}
                       valueRenderer={this.renderOption}
                       disabled={disabled || false}/>;
    }


}

SelectState.propTypes = {
    onChange: PropTypes.func.isRequired,
    state: PropTypes.string.isRequired,
    disabled: PropTypes.bool
};


