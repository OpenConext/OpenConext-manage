import React from "react";
import I18n from "i18n-js";
import PropTypes from "prop-types";
import {Link} from "react-router-dom";
import {includeInPush, search} from "../api";
import {copyToClip, isEmpty, stop} from "../utils/Utils";
import "./Support.css";
import Select from "react-select";
import "react-select/dist/react-select.css";
import NotesTooltip from "../components/NotesTooltip";
import CheckBox from "../components/CheckBox";

export default class Support extends React.PureComponent {

    constructor(props) {
        super(props);
        this.state = {
            excludeFromPushServiceProviders: [],
            copiedToClipboardClassName: "",
            loaded: false,
            status: "all"
        };
    }

    componentDidMount() {
        search({"metaDataFields.coin:exclude_from_push": "1"}, "saml20_sp")
            .then(json => this.setState({excludeFromPushServiceProviders: json, loaded: true}));
    }

    copyToClipboard = e => {
        stop(e);
        if (!isEmpty(this.state.excludeFromPushServiceProviders)) {
            copyToClip("results-printable");
            this.setState({copiedToClipboardClassName: "copied"});
            setTimeout(() => this.setState({copiedToClipboardClassName: ""}), 5000);
        }
    };

    renderSearchResultsTablePrintable = excludeFromPushServiceProviders =>
        <section id={"results-printable"}>
            {excludeFromPushServiceProviders
                .map(entity => `${entity.data.state},${entity.data.entityid},${entity.data.metaDataFields["name:en"] || entity.data.metaDataFields["name:nl"]}`)
                .join("\n")}</section>;

    changeStatus = option => this.setState({status: option ? option.value : null});

    renderSearchResultsTable = (excludeFromPushServiceProviders, status) => {
        const searchHeaders = ["status", "name", "entityid", "notes", "excluded"];
        excludeFromPushServiceProviders = status === "all" ? excludeFromPushServiceProviders : excludeFromPushServiceProviders.filter(entity => entity.data.state === status);
        return (
            <section>
                <table className="search-results">
                    <thead>
                    <tr>
                        {searchHeaders.map((header, index) =>
                            <th key={`${header}_${index}`}
                                className={index < 4 ? header : "extra"}>{index < 4 ? I18n.t(`playground.headers.${header}`) : header}</th>)}
                    </tr>
                    </thead>
                    <tbody>
                    {excludeFromPushServiceProviders.map((entity, index) => <tr
                        key={`${entity.data.entityid}_${index}`}>
                        <td className="state">{I18n.t(`metadata.${entity.data.state}`)}</td>
                        <td className="name">
                            <Link to={`/metadata/saml20_sp/${entity["_id"]}/metadata`}
                                  target="_blank">{entity.data.metaDataFields["name:en"] || entity.data.metaDataFields["name:nl"]}</Link>
                        </td>
                        <td className="entityId">{entity.data.entityid}</td>
                        <td className="notes">
                            {isEmpty(entity.data.notes) ? <span></span> :
                                <NotesTooltip identifier={entity.data.entityid} notes={entity.data.notes}/>}
                        </td>
                        <td><CheckBox name="excluded" value={true}
                                      onChange={() => includeInPush(entity["_id"]).then(() => this.componentDidMount())}/>
                        </td>
                    </tr>)}
                    </tbody>
                </table>
            </section>);
    };

    renderResults = (excludeFromPushServiceProviders, copiedToClipboardClassName, status) => <div>
        <section className="explanation">
            <p>Below are all Service Providers with <span className="code">coin:exclude_from_push</span> set to <span
                className="code">1</span>.</p>
            <p/>
            <p>You can either include them in the push by unchecking the checkbox or view them in a separate tab and
                review and subsequently the metadata.</p>
        </section>
        <section className="options">
            <a className={`clipboard-copy button green ${copiedToClipboardClassName}`}
               onClick={this.copyToClipboard}>
                {I18n.t("clipboard.copy")}<i className="fa fa-clone"></i>
            </a>
        </section>
        <Select onChange={this.changeStatus}
                options={["all", "prodaccepted", "testaccepted"]
                    .map(s => ({value: s, label: I18n.t(`metadata.${s}`)}))}
                value={status}
                className="status-select"/>
        {this.renderSearchResultsTable(excludeFromPushServiceProviders, status)}
        {this.renderSearchResultsTablePrintable(excludeFromPushServiceProviders)}
    </div>;

    renderNoResults = () => <section className="explanation">
        <p>There are no Service Providers with <span className="code">coin:exclude_from_push</span> set to <span
            className="code">1</span>.</p>
    </section>;

    render() {
        const {excludeFromPushServiceProviders, loaded, copiedToClipboardClassName, status} = this.state;
        const showResults = excludeFromPushServiceProviders.length > 0 && loaded;
        const showNoResults = excludeFromPushServiceProviders.length === 0 && loaded;
        return (
            <div className="support">
                {showResults && this.renderResults(excludeFromPushServiceProviders, copiedToClipboardClassName, status)}
                {showNoResults && this.renderNoResults()}
            </div>
        );
    }
}

Support.propTypes = {
    history: PropTypes.object.isRequired
};

