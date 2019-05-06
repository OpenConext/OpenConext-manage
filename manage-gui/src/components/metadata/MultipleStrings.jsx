import React from "react";
import Creatable from "react-select/lib/Creatable";

import "./SelectEnum.css";

export default class MultipleStrings extends React.PureComponent {
  render() {
    const { onChange, values, ...rest } = this.props;
    const options = values.map(val => ({ label: val, value: val }));

    return (
      <Creatable
        {...rest}
        isMulti={true}
        placeholder="Enter value..."
        onChange={options => onChange(options.map(o => o.value))}
        value={options}
      />
    );
  }
}
