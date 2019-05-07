import React from "react";
import PropTypes from "prop-types";
import Select from "react-select";

export default class SelectMulti extends React.PureComponent {
  static defaultProps = {
    disabled: false
  };

  valuesToOptions(values) {
    return values.map(value => ({ value: value, label: value }));
  }

  optionsToValues(options) {
    return options.map(option => option.value);
  }

  render() {
    const { enumValues, onChange, value, name, ...rest } = this.props;

    const selectedOptions = this.valuesToOptions(value);
    const options = this.valuesToOptions(enumValues);

    return (
      <Select
        {...rest}
        inputId={`react-select-${name}`}
        isMulti={true}
        onChange={options => onChange(this.optionsToValues(options))}
        optionRenderer={option => option.label}
        options={options}
        value={selectedOptions}
      />
    );
  }
}

SelectMulti.propTypes = {
  autoFocus: PropTypes.bool,
  disabled: PropTypes.bool,
  enumValues: PropTypes.array.isRequired,
  name: PropTypes.string.isRequired,
  onChange: PropTypes.func.isRequired,
  value: PropTypes.array.isRequired
};
