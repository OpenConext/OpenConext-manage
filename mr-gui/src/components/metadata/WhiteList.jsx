import React from "react";
import I18n from "i18n-js";
import PropTypes from "prop-types";
import InlineEditable from "./InlineEditable";
import CheckBox from "./../CheckBox";

import "./WhiteList.css";

export default class WhiteList extends React.PureComponent {

    constructor(props) {
        super(props);
        this.state = {};
    }

    componentDidMount() {
        window.scrollTo(0, 0);
    }

    onChange = name => value => {
        if (value.target) {
            this.props.onChange(name, value.target.checked);
        } else {
            this.props.onChange(name, value);
        }
    };

    render() {
        const {allowedAll, allowedEntities, whiteListing} = this.props;
        return (
            <div className="metadata-whitelist">
                <CheckBox info={I18n.t("metadata.allowAll")} name="allow-all" value={allowedAll} onChange={this.onChange("data.allowedall")}/>
            </div>
        );
    }
}

WhiteList.propTypes = {
    whiteListing: PropTypes.array.isRequired,
    allowedEntities: PropTypes.array.isRequired,
    allowedAll: PropTypes.bool.isRequired,
    onChange: PropTypes.func.isRequired
};

