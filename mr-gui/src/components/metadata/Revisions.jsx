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
        const {revisions, metaData} = this.props;
        debugger;
        return (
            <div className="metadata-revisions">
                <div className="revisions-info">
                    <h2>{I18n.t("revisions.info")}</h2>
                </div>
                <div className="revisions">

                </div>

            </div>
        );
    }
}

Revisions.propTypes = {
    metaData: PropTypes.object.isRequired,
    revisions: PropTypes.array.isRequired,
    guest: PropTypes.bool.isRequired
};

