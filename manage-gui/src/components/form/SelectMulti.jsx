import React from "react";
import PropTypes from "prop-types";
import {Select} from "./../../components";

export default class SelectMulti extends React.PureComponent {

  valuesToOptions(values) {
    return values.map(value => ({value: value, label: value}));
  }

  optionsToValues(options) {
    return options.map(option => option.value);
  }

  render() {
    const {enumValues, onChange, value, ...rest} = this.props;

    const selectedOptions = this.valuesToOptions(value);
    const options = this.valuesToOptions(enumValues);

    return (
      <Select
        {...rest}
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
  enumValues: PropTypes.array.isRequired
};
