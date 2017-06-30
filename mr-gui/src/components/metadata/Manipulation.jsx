import React from "react";
import I18n from "i18n-js";
import PropTypes from "prop-types";
import InlineEditable from "./InlineEditable";

import "./Manipulation.css";

export default class Manipulation extends React.PureComponent {

    constructor(props) {
        super(props);
        this.state = {};
    }

    componentDidMount() {
        window.scrollTo(0, 0);
    }

    onChange = name => value => {
        this.props.onChange(name, value);
    };

    render() {
        //const {whiteListing, allowedEntities} = this.props;
        return (
            <div className="metadata-manipulation">
                <p>Manipulation</p>
            </div>
        );
    }
}

Manipulation.propTypes = {
    content: PropTypes.array.isRequired,
    onChange: PropTypes.func.isRequired
};

