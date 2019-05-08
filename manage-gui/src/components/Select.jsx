import React from "react";
import PropTypes from "prop-types";
import { default as ReactSelect } from "react-select";

import reactSelectStyles from "./reactSelectStyles.js";

export default class Select extends React.PureComponent {
  static defaultProps = {
    disabled: false
  };

  render() {
    const { name, ...rest } = this.props;

    return (
      <ReactSelect
        {...rest}
        styles={reactSelectStyles}
        inputId={`react-select-${name}`}
      />
    );
  }
}

Select.propTypes = {
  autoFocus: PropTypes.bool,
  disabled: PropTypes.bool,
  searchable: PropTypes.bool,
  options: PropTypes.array.isRequired,
  name: PropTypes.string.isRequired,
  onChange: PropTypes.func.isRequired,
  value: PropTypes.oneOfType([PropTypes.string, PropTypes.array])
};
