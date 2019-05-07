import React from "react";
import PropTypes from "prop-types";
import Creatable from "react-select/lib/Creatable";

export default class Strings extends React.PureComponent {
  render() {
    const { onChange, value, name, ...rest } = this.props;
    const options = value.map(val => ({ label: val, value: val }));

    return (
      <Creatable
        {...rest}
        inputId={`react-select-${name}`}
        isMulti={true}
        onChange={options => onChange(options.map(o => o.value))}
        placeholder="Enter value..."
        value={options}
      />
    );
  }
}

Strings.propTypes = {
  autoFocus: PropTypes.bool,
  disabled: PropTypes.bool,
  name: PropTypes.string.isRequired,
  onChange: PropTypes.func.isRequired,
  value: PropTypes.array.isRequired
};
