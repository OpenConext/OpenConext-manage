import React from "react";
import PropTypes from "prop-types";
import I18n from "i18n-js";
import {validation} from "./../api";

import "./FormatInput.css";

export default class FormatInput extends React.PureComponent {

    constructor(props) {
        super(props);
        this.state = {
            error: props.isError,
            errorMessage: I18n.t("metaDataFields.error", {format: props.format})
        };
    }

    componentDidMount () {
        if (this.props.autofocus && this.input !== null) {
            this.input.focus();
        }
    }

    onChange = e => this.props.onChange(e.target.value);

    onBlur = format => e => {
        const value = e.target.value;
        validation(format, value).then(result => {
            this.setState({
                error: !result
            });
            this.props.onError(!result);
        })
    };

    render() {
        const {name, input, format, readOnly} = this.props;
        const {error, errorMessage} = this.state;
        const className = error ? "error" : "";
        return <div className="format-input">
            <input ref={ref => this.input = ref} className={className} type="text" id={name} name={name} value={input}
                   onChange={this.onChange} onBlur={this.onBlur(format)} disabled={readOnly}/>
            {error && <span><i className="fa fa-warning"></i>{errorMessage}</span>}
        </div>

    }

}

FormatInput.propTypes = {
    name: PropTypes.string.isRequired,
    input: PropTypes.string.isRequired,
    format: PropTypes.string.isRequired,
    onChange: PropTypes.func.isRequired,
    onError: PropTypes.func.isRequired,
    isError: PropTypes.bool.isRequired,
    autofocus: PropTypes.bool,
    readOnly: PropTypes.bool
};


