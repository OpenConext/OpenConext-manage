import React from "react";
import I18n from "i18n-js";
import PropTypes from "prop-types";
import InlineEditable from "./InlineEditable";

import "./Revisions.css";

export default class Revisions extends React.PureComponent {

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
        //const {revisions, allowedEntities} = this.props;
        return (
            <div className="metadata-revisions">
                <p>Revisions</p>
            </div>
        );
    }
}

Revisions.propTypes = {
    metaData: PropTypes.object.isRequired,
    revisions: PropTypes.array.isRequired
};

