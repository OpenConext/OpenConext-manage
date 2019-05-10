import React from "react";
import PropTypes from "prop-types";
import Creatable from "react-select/lib/Creatable";
import I18n from "i18n-js";

import reactSelectStyles from "./../reactSelectStyles.js";

export default class Strings extends React.PureComponent {
  renderErrorMessage() {
    return I18n.t("metaDataFields.error", { format: this.props.format });
  }

  render() {
    const { onChange, value, name, hasFormatError, ...rest } = this.props;
    const options = value.map(val => ({ label: val, value: val }));

    return (
      <div className="format-input">
        <Creatable
          {...rest}
          styles={reactSelectStyles}
          inputId={`react-select-${name}`}
          isMulti={true}
          onChange={options => onChange(options.map(o => o.value))}
          placeholder="Enter value..."
          value={options}
        />
        {hasFormatError && (
          <span>
            <i className="fa fa-warning" />
            {this.renderErrorMessage()}
          </span>
        )}
      </div>
    );
  }
}

Strings.propTypes = {
  autoFocus: PropTypes.bool,
  disabled: PropTypes.bool,
  hasFormatError: PropTypes.bool,
  name: PropTypes.string.isRequired,
  onChange: PropTypes.func.isRequired,
  value: PropTypes.array.isRequired
};
