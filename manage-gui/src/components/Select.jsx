import React from "react";
import PropTypes from "prop-types";
import {default as ReactSelect} from "react-select";

import reactSelectStyles from "./reactSelectStyles.js";

export default class Select extends React.PureComponent {
  static defaultProps = {
    disabled: false
  };

  valueToOption = value => ({value: value, label: value});

  randomName = () => Math.random().toString(36).substring(2, 15) + Math.random().toString(36).substring(2, 15);

  render() {
    const {name = this.randomName(), value, className = "", ...rest} = this.props;

    const valueAsOption = typeof value === "string" ? this.valueToOption(value) : value;

    return (
      <ReactSelect
        {...rest}
        className={className}
        styles={reactSelectStyles}
        inputId={`react-select-${name}`}
        value={valueAsOption}
      />
    );
  }
}

Select.propTypes = {
  autoFocus: PropTypes.bool,
  disabled: PropTypes.bool,
  searchable: PropTypes.bool,
  options: PropTypes.array.isRequired,
  name: PropTypes.string,
  onChange: PropTypes.func.isRequired,
  value: PropTypes.oneOfType([PropTypes.object, PropTypes.string, PropTypes.array])
};
