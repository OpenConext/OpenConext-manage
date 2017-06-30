import React from "react";
import I18n from "i18n-js";
import PropTypes from "prop-types";
import InlineEditable from "./InlineEditable";

import "./Connection.css";

export default class Connection extends React.PureComponent {

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
        const {type, revision, data} = this.props.metaData;
        return (
            <div className="metadata-connection">
                <InlineEditable name="metadata.entityId" mayEdit={true} value={data.entityid || ""}
                                onChange={this.onChange("entityId")}/>
            </div>
        );
    }
}

Connection.propTypes = {
    metaData: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired
};

