import React from "react";
import PropTypes from "prop-types";
import "./Password.css";
import {secret} from "../../api";
import CopyToClipboard from "react-copy-to-clipboard";

export default class Password extends React.PureComponent {
  state = {
    value: this.props.value,
    disabled: true,
    showSaveWarning: false,
    copied: false
  };

  handleEdit() {
    this.setState(
      {
        disabled: false,
        value: "",
      },
      () => this.passwordInput.focus()
    );
  }

  handleCopy = () => this.setState({"copied": true},
    () => setTimeout(() => this.setState({"copied": false}), 1500));

  handleUndo() {
    this.setState({
      value: this.props.value,
      disabled: true,
      showSaveWarning: false
    });
  }

  handleGenerate() {
    secret().then(json => this.setState({
      value: json.secret,
      disabled: true,
      showSaveWarning: false
    }));

  }

  handleSave() {
    this.props.onChange(this.state.value);

    this.setState({
      disabled: true,
      showSaveWarning: false
    });
  }

  renderDisabledIcon(copied) {
    const classNameCopy = copied ? "copy copied" : "copy";
    return (
      <div className="password-icon-container">
        <div className="password-icon">
          <CopyToClipboard text={this.state.value} onCopy={this.handleCopy}>
            <i className={`fa fa-copy ${classNameCopy}`}/>
          </CopyToClipboard>
        </div>

        <span className="separator"/>

        <div className="password-icon" onClick={() => this.handleEdit()}>
          <i className="fa fa-pencil edit"/>
        </div>
      </div>
    );
  }

  renderEnabledIcons() {
    return (
      <div className="password-icon-container">
        <div className="password-icon" onClick={() => this.handleGenerate()}>
          <i className="fa fa-key key"/>
        </div>

        <span className="separator"/>

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
    const {value, showSaveWarning, copied} = this.state;
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
            {...{value, disabled}}
            type="text"
            className="password-input"
            onChange={e => this.setState({value: e.target.value})}
            ref={el => {
              this.passwordInput = el;
            }}
          />

          <span className="separator"/>
          {disabled ? this.renderDisabledIcon(copied) : this.renderEnabledIcons()}
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
