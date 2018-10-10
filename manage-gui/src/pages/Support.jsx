import React from "react";
import I18n from "i18n-js";
import PropTypes from "prop-types";
import {Link} from "react-router-dom";
import {ping, search, validation} from "../api";
import {copyToClip, isEmpty, stop} from "../utils/Utils";
import SelectMetaDataType from "../components/metadata/SelectMetaDataType";
import "./Support.css";
import SelectNewMetaDataField from "../components/metadata/SelectNewMetaDataField";
import debounce from "lodash.debounce";
import Select from "react-select";
import "react-select/dist/react-select.css";
import NotesTooltip from "../components/NotesTooltip";
import SelectNewEntityAttribute from "../components/metadata/SelectNewEntityAttribute";

export default class Support extends React.PureComponent {

    constructor(props) {
        super(props);
        this.state = {
            excludeFromPushServiceProviders: [],
            copiedToClipboardClassName: ""
        };
    }

    componentDidMount() {
        ping();
    }

    copyToClipboard = e => {
        stop(e);
        if (!isEmpty(this.state.excludeFromPushServiceProviders)) {
            copyToClip("sp-printable");
            this.setState({copiedToClipboardClassName: "copied"});
            setTimeout(() => this.setState({copiedToClipboardClassName: ""}), 5000);
        }
    };

    renderSearchResultsTablePrintable = (searchResults) =>
        <section id={"search-results-printable"}>
            {searchResults
                .map(entity => `${entity.data.state},${entity.data.entityid},${entity.data.metaDataFields["name:en"] || entity.data.metaDataFields["name:nl"]}`)
                .join("\n")}</section>


    render() {
        return (
            <div className="support">
                TODO
            </div>
        );
    }
}

Support.propTypes = {
    history: PropTypes.object.isRequired
};

