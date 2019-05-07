import React from "react";
import PropTypes from "prop-types";
import Select from "react-select";

export default class SelectOne extends React.PureComponent {
  static defaultProps = {
    disabled: false
  };

  valueToOption(value) {
    return { value: value, label: value };
  }

  valuesToOptions(values) {
    return values.map(value => this.valueToOption(value));
  }

  render() {
    const { enumValues, onChange, value, name, ...rest } = this.props;

    return (
      <Select
        {...rest}
        inputId={`react-select-${name}`}
        onChange={option => onChange(option.value)}
        optionRenderer={option => option.label}
        options={this.valuesToOptions(enumValues)}
        value={this.valueToOption(value)}
      />
    );
  }
}

SelectOne.propTypes = {
  autoFocus: PropTypes.bool,
  disabled: PropTypes.bool,
  enumValues: PropTypes.array.isRequired,
  name: PropTypes.string.isRequired,
  onChange: PropTypes.func.isRequired,
  value: PropTypes.string.isRequired
};
