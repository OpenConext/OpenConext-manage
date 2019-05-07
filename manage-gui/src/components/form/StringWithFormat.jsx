import React from "react";
import PropTypes from "prop-types";
import I18n from "i18n-js";

export default class StringWithFormat extends React.PureComponent {
  getInputType() {
    switch (this.props.format) {
      case "password":
        return "password";
      default:
        return "text";
    }
  }

  renderErrorMessage() {
    return I18n.t("metaDataFields.error", { format: this.props.format });
  }

  render() {
    const { onChange, hasFormatError, ...rest } = this.props;

    return (
      <div className="format-input">
        <input
          {...rest}
          className={hasFormatError && "error"}
          type={this.getInputType()}
          onChange={e => onChange(e.target.value)}
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
