import React from "react";
import PropTypes from "prop-types";
import {Select} from "./../../components";

import I18n from "i18n-js";
import "./SelectState.scss";

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
    const {onChange, state, states, disabled} = this.props;
    const options = states.map(s => {
      return {value: s, label: I18n.t(`metadata.${s}`)};
    });
    return <Select className="select-state"
                   name="react-select-state"
                   onChange={option => {
                     if (option) {
                       onChange(option.value);
                     }
                   }}
                   optionRenderer={this.renderOption}
                   options={options}
                   value={state}
                   isSearchable={false}
                   valueRenderer={this.renderOption}
                   disabled={disabled || false}/>;
  }


}

SelectState.propTypes = {
  onChange: PropTypes.func.isRequired,
  state: PropTypes.string.isRequired,
  states: PropTypes.array.isRequired,
  disabled: PropTypes.bool
};
