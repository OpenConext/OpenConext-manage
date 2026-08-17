import React from "react";
import I18n from "../locale/I18n";
import PropTypes from "prop-types";
import CopyToClipboard from "react-copy-to-clipboard";

import "./ClipBoardCopy.scss";

export default class ClipBoardCopy extends React.PureComponent {

    constructor(props) {
        super(props);
        this.state = {
            copiedToClipboard: false
        };
    }

    copiedToClipboard = () => {
        this.setState({copiedToClipboard: true});
        setTimeout(() => this.setState({copiedToClipboard: false}), 2500);
    };

    toolTip = () => I18n.t(this.state.copiedToClipboard ? "clipboard.copied" : "clipboard.copy");

    render() {
        const {text} = this.props;
        const copiedToClipboard = this.state.copiedToClipboard;
        const copiedToClipBoardClassName = copiedToClipboard ? "copied" : "";
        return (
            <span className="clipboard">
                <CopyToClipboard text={text} onCopy={this.copiedToClipboard}>
                    <span>
                        <a className="identifier-copy-link tooltip-trigger"
                           data-tooltip-content={this.toolTip()}
                           data-tooltip-place="right">
                            <i className={`fa fa-clipboard ${copiedToClipBoardClassName}`}></i>
                         </a>
                    </span>
    </CopyToClipboard>
    </span>
        )
            ;
    }
}

ClipBoardCopy.propTypes = {
    identifier: PropTypes.string.isRequired,
    text: PropTypes.string.isRequired
};
