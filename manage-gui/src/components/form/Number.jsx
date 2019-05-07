import React from "react";
import PropTypes from "prop-types";

const Number = ({ onChange, ...rest }) => (
  <input
    {...rest}
    onChange={e => onChange(parseInt(e.target.value, 10))}
    type="number"
  />
);

Boolean.propTypes = {
  autoFocus: PropTypes.bool,
  disabled: PropTypes.bool,
  enumValues: PropTypes.array.isRequired,
  onChange: PropTypes.func.isRequired,
  value: PropTypes.string.isRequired
};

export default Number;
