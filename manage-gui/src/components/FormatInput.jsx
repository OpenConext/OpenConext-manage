import React from "react";
import PropTypes from "prop-types";
import I18n from "i18n-js";

import {validation} from "./../api";
import {isEmpty} from "../utils/Utils";

import "./FormatInput.scss";

export default class FormatInput extends React.PureComponent {
    constructor(props) {
        super(props);

        this.state = {
            error: props.isError,
            errorMessage: I18n.t("metaDataFields.error", {format: props.format})
        };
    }

    componentDidMount() {
        if (this.props.autoFocus) this.input.focus();
    }

    onBlur = e => {
        const value = e.target.value;
        const {format, isRequired = true} = this.props;

        if (isEmpty(value)) {
            this.setState({error: isRequired});
            this.props.onError(isRequired);
        } else {
            validation(format, value).then(result => {
                this.setState({error: !result});
                this.props.onError(!result);
            })
        }
    };

    getInputType() {
        switch (this.props.format) {
            case 'password':
                return 'password';
            default:
                return 'text'
        }
    }

    render() {
        const {name, input, readOnly, onChange} = this.props;
        const {error, errorMessage} = this.state;

        return (
            <div className="format-input">
                <input
                    ref={ref => this.input = ref}
                    className={error ? "error" : ""}
                    type={this.getInputType()}
                    id={name}
                    name={name}
                    value={input}
                    onChange={e => onChange(e.target.value)}
                    onBlur={this.onBlur}
                    disabled={readOnly}
                />
                {error &&
                <span><i className="fas fa-warning"></i>{errorMessage}</span>
                }
            </div>
        )
    }
}

FormatInput.propTypes = {
    name: PropTypes.string.isRequired,
    input: PropTypes.string.isRequired,
    format: PropTypes.string.isRequired,
    onChange: PropTypes.func.isRequired,
    onError: PropTypes.func.isRequired,
    isError: PropTypes.bool.isRequired,
    autoFocus: PropTypes.bool,
    isRequired: PropTypes.bool,
    readOnly: PropTypes.bool
};
