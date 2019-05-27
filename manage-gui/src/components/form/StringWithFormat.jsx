import React from "react";
import PropTypes from "prop-types";
import I18n from "i18n-js";
import { Password } from "../form";

export default class StringWithFormat extends React.PureComponent {
  renderInput() {
    const { onChange, hasFormatError, format, onBlur, ...rest } = this.props;

    return (
      <div className="format-input">
        <input
          {...rest}
          className={hasFormatError ? "error" : ""}
          type="text"
          onChange={e => onChange(e.target.value)}
          onBlur={e => onBlur(e.target.value)}
        />
        {hasFormatError && (
          <span>
            <i className="fa fa-warning" />
            {I18n.t("metaDataFields.error", { format })}
          </span>
        )}
      </div>
    );
  }

  render() {
    switch (this.props.format) {
      case "password":
        return <Password {...this.props} />;
      default:
        return this.renderInput();
    }
  }
}

StringWithFormat.propTypes = {
  name: PropTypes.string.isRequired,
  value: PropTypes.string.isRequired,
  format: PropTypes.string.isRequired,
  onChange: PropTypes.func.isRequired,
  hasFormatError: PropTypes.bool.isRequired,
  autoFocus: PropTypes.bool,
  isRequired: PropTypes.bool,
  disabled: PropTypes.bool
};
