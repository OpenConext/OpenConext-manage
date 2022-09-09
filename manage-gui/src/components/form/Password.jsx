import React from "react";
import PropTypes from "prop-types";
import I18n from "i18n-js";
import "./Password.scss";
import {secret} from "../../api";
import CopyToClipboard from "react-copy-to-clipboard";
import ReactTooltip from "react-tooltip";

export default class Password extends React.PureComponent {

    state = {
        value: this.props.value,
        disabled: true,
        copied: false
    };

    componentDidMount() {
        if (!this.props.value) {
            this.handleGenerate();
        }
    }

    handleCopy = () => this.setState({"copied": true},
        () => setTimeout(() => this.setState({"copied": false}), 1500));

    handleGenerate() {
        secret().then(json => this.setState({
            value: json.secret,
        }, () => {
            this.props.hasError(this.props.name, false);
            this.props.onChange(this.state.value);
        }));

    }

    renderIcon = (id, className, tooltipKey) =>
        <span>
          <i className={className} data-for={`${this.props.name}-${id}`} data-tip/>
          <ReactTooltip
              id={`${this.props.name}-${id}`}
              type="info"
              class="tool-tip"
              effect="solid">
            <span>{I18n.t(`password.${tooltipKey}`)}</span>
          </ReactTooltip>
      </span>;

    renderDisabledIcon(copied) {
        const classNameCopy = copied ? "copy copied" : "copy";
        const tooltipKey = copied ? "copied" : "copy";
        return (
            <div className="password-icon-container">
                <div className="password-icon">
                    <CopyToClipboard text={this.state.value} onCopy={this.handleCopy}>
                        {this.renderIcon("copy-icon", `fa fa-copy ${classNameCopy}`, tooltipKey)}
                    </CopyToClipboard>
                </div>

                <span className="separator"/>

                <div className="password-icon" onClick={() => this.handleGenerate()}>
                    {this.renderIcon("key-icon", "fa fa-key key", "key")}
                </div>

            </div>
        );
    }

    render() {
        const {value, copied} = this.state;
        const {hasFormatError, onChange, hasError, ...rest} = this.props;

        const disabled = this.state.disabled || this.props.disabled;

        return (
            <div>
                <div className={"password-field " + (disabled ? "disabled" : "")}>
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
                    {this.renderDisabledIcon(copied)}
                </div>

            </div>
        );
    }
}

Password.propTypes = {
    name: PropTypes.string.isRequired,
    value: PropTypes.string.isRequired,
    format: PropTypes.string.isRequired,
    onChange: PropTypes.func.isRequired,
    hasError: PropTypes.func.isRequired,
    autoFocus: PropTypes.bool,
    isRequired: PropTypes.bool,
    disabled: PropTypes.bool
};
