import React from "react";
import PropTypes from "prop-types";
import Select from "react-select";

import "./SelectEnum.css";

export default class SelectEnum extends React.PureComponent {
  renderOption = option => {
    return (
      <span className="select-option">
        <span className="select-label">{option.label}</span>
      </span>
    );
  };

  render() {
    const { autofocus, disabled, enumValues, onChange, state } = this.props;
    const options = enumValues.map(s => ({ value: s, label: s }));

    return (
      <Select
        autoFocus={autofocus}
        className="select-state"
        disabled={disabled || false}
        onChange={option => option && onChange(option.value)}
        optionRenderer={this.renderOption}
        options={options}
        searchable={false}
        value={state}
        valueRenderer={this.renderOption}
      />
    );
  }
}

SelectEnum.propTypes = {
  autofocus: PropTypes.bool,
  disabled: PropTypes.bool,
  enumValues: PropTypes.array.isRequired,
  onChange: PropTypes.func.isRequired,
  state: PropTypes.string.isRequired
};
