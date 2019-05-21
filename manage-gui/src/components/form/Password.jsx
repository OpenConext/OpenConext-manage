import React from "react";
import PropTypes from "prop-types";
import "./Password.css";

export default class Password extends React.PureComponent {
  state = {
    value: this.props.value,
    disabled: true,
    type: "password",
    showSaveWarning: false
  };

  handleEdit() {
    this.setState(
      {
        disabled: false,
        value: "",
        type: "text"
      },
      () => this.passwordInput.focus()
    );
  }

  handleUndo() {
    this.setState({
      value: this.props.value,
      disabled: true,
      type: "password",
      showSaveWarning: false
    });
  }

  handleSave() {
    this.props.onChange(this.state.value);

    this.setState({
      disabled: true,
      type: "password",
      showSaveWarning: false
    });
  }

  renderDisabledIcon() {
    return (
      <div className="password-icon" onClick={() => this.handleEdit()}>
        <i className="fa fa-pencil edit"/>
      </div>
    );
  }

  renderEnabledIcons() {
    return (
      <div className="password-icon-container">
        <div className="password-icon" onClick={() => this.handleUndo()}>
          <i className="fa fa-undo undo"/>
        </div>

        <span className="separator"/>

        <div className="password-icon" onClick={() => this.handleSave()}>
          <i className="fa fa-save save"/>
        </div>
      </div>
    );
  }

  render() {
    const {value, type, showSaveWarning} = this.state;
    const {hasFormatError, onChange, ...rest} = this.props;

    const disabled = this.state.disabled || this.props.disabled;

    return (
      <div>
        <div
          className={"password-field " + (disabled ? "disabled" : "")}
          onBlur={() => this.setState({showSaveWarning: true})}
          onFocus={() => this.setState({showSaveWarning: true})}
        >
          <input
            {...rest}
            {...{value, type, disabled}}
            className="password-input"
            onChange={e => this.setState({value: e.target.value})}
            ref={el => {
              this.passwordInput = el;
            }}
          />

          <span className="separator"/>
          {disabled ? this.renderDisabledIcon() : this.renderEnabledIcons()}
        </div>

        {showSaveWarning && (
          <span className="error">
            <i className="fa fa-warning"/>
            Save the new submission inline for it to be saved on submit
          </span>
        )}
      </div>
    );
  }
}

Password.propTypes = {
  name: PropTypes.string.isRequired,
  value: PropTypes.string.isRequired,
  format: PropTypes.string.isRequired,
  onChange: PropTypes.func.isRequired,
  autoFocus: PropTypes.bool,
  isRequired: PropTypes.bool,
  disabled: PropTypes.bool
};
