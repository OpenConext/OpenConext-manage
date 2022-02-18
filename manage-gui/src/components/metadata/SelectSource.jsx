import React from "react";
import PropTypes from "prop-types";
import {Select} from "./../../components";

import "./SelectSource.scss";

export default class SelectSource extends React.PureComponent {

  render() {
    const {onChange, source, sources, disabled, autoFocus = false} = this.props;
    const options = sources.map(s => {
      return {value: s, label: s};
    });
    return <Select className="select-state"
                   onChange={option => {
                     if (option) {
                       onChange(option.value);
                     }
                   }}
                   options={options}
                   value={source}
                   autoFocus={autoFocus}
                   isSearchable={false}
                   disabled={disabled || false}/>;
  }
}

SelectSource.propTypes = {
  onChange: PropTypes.func.isRequired,
  source: PropTypes.string.isRequired,
  sources: PropTypes.array.isRequired,
  disabled: PropTypes.bool,
  autoFocus: PropTypes.bool
};


