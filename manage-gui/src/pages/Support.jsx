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
import ConfirmationDialog from "../components/ConfirmationDialog";

export default class Support extends React.PureComponent {

    constructor(props) {
        super(props);
        this.state = {
            excludeFromPushServiceProviders: [],
            filteredPushServiceProviders: [],
            query: "",
            copiedToClipboardClassName: "",
            loaded: false,
            status: "all",
            confirmationDialogOpen: false,
            confirmationQuestion: "",
            confirmationDialogAction: () => this,
            cancelDialogAction: () => this.setState({confirmationDialogOpen: false})
        };
    }

    componentDidMount() {
        search({"metaDataFields.coin:exclude_from_push": "1"}, "saml20_sp")
            .then(json => {
                const result = json.map(entity => ({
                    state: entity.data.state,
                    entityid: entity.data.entityid,
                    name: entity.data.metaDataFields["name:en"] || entity.data.metaDataFields["name:nl"] || entity.data.entityid,
                    notes: entity.data.notes,
                    id: entity["_id"]
                }));
                this.setState({
                    excludeFromPushServiceProviders: result,
                    filteredPushServiceProviders: result,
                    query: "",
                    loaded: true
                })
            });
    }

    copyToClipboard = e => {
        stop(e);
        if (!isEmpty(this.state.excludeFromPushServiceProviders)) {
            copyToClip("results-printable");
            this.setState({copiedToClipboardClassName: "copied"});
            setTimeout(() => this.setState({copiedToClipboardClassName: ""}), 5000);
        }
    };

    confirmIncludeInPush = (entityId, name) => {
        this.setState({
            confirmationDialogOpen: true,
            confirmationQuestion: I18n.t("support.includeConfirmation", {name: name}),
            confirmationDialogAction: () => {
                this.setState({confirmationDialogOpen: false});
                includeInPush(entityId).then(() => this.componentDidMount())
            }
        })
    };

    renderSearchResultsTablePrintable = excludeFromPushServiceProviders =>
        <section id={"results-printable"}>
            {excludeFromPushServiceProviders
                .map(entity => `${entity.state},${entity.entityid},${entity.name}`)
                .join("\n")}</section>;

    changeStatus = option => this.setState({status: option ? option.value : null});

    renderSearchResultsTable = (excludeFromPushServiceProviders, status) => {
        const searchHeaders = ["status", "name", "entityid", "notes", "excluded"];
        excludeFromPushServiceProviders = status === "all" ? excludeFromPushServiceProviders : excludeFromPushServiceProviders.filter(entity => entity.state === status);
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
                        key={`${entity.entityid}_${index}`}>
                        <td className="state">{I18n.t(`metadata.${entity.state}`)}</td>
                        <td className="name">
                            <Link to={`/metadata/saml20_sp/${entity.id}/metadata`}
                                  target="_blank">{entity.name}</Link>
                        </td>
                        <td className="entityId">{entity.entityid}</td>
                        <td className="notes">
                            {isEmpty(entity.notes) ? <span></span> :
                                <NotesTooltip identifier={entity.entityid} notes={entity.notes}/>}
                        </td>
                        <td><CheckBox name={entity.id} value={true}
                                      onChange={() => this.confirmIncludeInPush(entity.id, entity.name)}/>
                        </td>
                    </tr>)}
                    </tbody>
                </table>
            </section>);
    };

    search = e => {
        const query = e.target.value ? e.target.value.toLowerCase() : "";
        const {excludeFromPushServiceProviders} = this.state;
        const names = ["name", "status", "entityid"];
        const result = isEmpty(query) ? excludeFromPushServiceProviders : excludeFromPushServiceProviders.filter(sp => names.some(name =>
            sp[name] && sp[name].toLowerCase().indexOf(query) > -1));
        this.setState({query: query, filteredPushServiceProviders: result});
    };


    renderResults = (excludeFromPushServiceProviders, copiedToClipboardClassName, status, query) => <div>
        <section className="explanation">
            <p>Below are all Service Providers with <span className="code">coin:exclude_from_push</span> set to <span
                className="code">1</span>.</p>
            <p/>
            <p>You can either include them in the push by unchecking the checkbox or view them in a separate tab, review
                and subsequently save the metadata.</p>
        </section>
        <section className="options">
            <div className="search-input-container">
                <input className="search-input"
                       placeholder={I18n.t("support.searchPlaceHolder")}
                       type="text"
                       onChange={this.search}
                       value={query}/>
                <i className="fa fa-search"></i>
            </div>
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
        const {
            excludeFromPushServiceProviders, filteredPushServiceProviders, loaded, copiedToClipboardClassName, status, query,
            confirmationDialogOpen, confirmationQuestion, confirmationDialogAction, cancelDialogAction
        } = this.state;
        const showResults = excludeFromPushServiceProviders.length > 0 && loaded;
        const showNoResults = excludeFromPushServiceProviders.length === 0 && loaded;
        return (
            <div className="support">
                <ConfirmationDialog isOpen={confirmationDialogOpen}
                                    cancel={cancelDialogAction}
                                    confirm={confirmationDialogAction}
                                    question={confirmationQuestion}/>
                {showResults && this.renderResults(filteredPushServiceProviders, copiedToClipboardClassName, status, query)}
                {showNoResults && this.renderNoResults()}
            </div>
        );
    }
}

Support.propTypes = {
    history: PropTypes.object.isRequired
};

