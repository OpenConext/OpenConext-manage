import React from "react";
import PropTypes from "prop-types";
import CheckBox from "./../CheckBox";

export default class Boolean extends React.PureComponent {
  render() {
    const {value, onChange, disabled, ...rest} = this.props;

    const backwardCompatibleValue = ["1", 1, "true", true].includes(value);

    return (
      <CheckBox
        {...rest}
        onChange={e => onChange(e.target.checked)}
        readOnly={disabled}
        value={backwardCompatibleValue}
      />
    );
  }
}

Boolean.propTypes = {
  autoFocus: PropTypes.bool,
  disabled: PropTypes.bool,
  onChange: PropTypes.func.isRequired,
  value: PropTypes.oneOfType([PropTypes.string, PropTypes.bool]).isRequired
};
