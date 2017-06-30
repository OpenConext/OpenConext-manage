import React from "react";
import I18n from "i18n-js";
import PropTypes from "prop-types";
import InlineEditable from "./InlineEditable";

import "./MetaData.css";

export default class MetaData extends React.PureComponent {

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
            <div className="metadata-metadata">
                <p>MetaData</p>
            </div>
        );
    }
}

MetaData.propTypes = {
    onChange: PropTypes.func.isRequired,
    whiteListing: PropTypes.array.isRequired,
    entries: PropTypes.array.isRequired,
    configuration: PropTypes.object.isRequired
};

