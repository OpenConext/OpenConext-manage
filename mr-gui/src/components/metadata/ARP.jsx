import React from "react";
import I18n from "i18n-js";
import PropTypes from "prop-types";
import InlineEditable from "./InlineEditable";

import "./ARP.css";

export default class ARP extends React.PureComponent {

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
                <p>ARP</p>
            </div>
        );
    }
}

ARP.propTypes = {
    arp: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
    arpConfiguration: PropTypes.object.isRequired,
    guest: PropTypes.bool.isRequired
};

