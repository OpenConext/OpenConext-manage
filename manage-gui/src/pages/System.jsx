import React from "react";
import I18n from "i18n-js";

import CopyToClipboard from "react-copy-to-clipboard";
import PropTypes from "prop-types";
import {
    deleteOrphanedReferences,
    orphans,
    ping,
    push,
    pushPreview,
    restoreDeletedRevision,
    search,
    validate
} from "../api";
import {isEmpty, stop} from "../utils/Utils";
import JsonView from "react-pretty-json";
import ConfirmationDialog from "../components/ConfirmationDialog";
import "./System.css";
import "react-pretty-json/assets/json-view.css";
import {setFlash} from "../utils/Flash";
import SelectMetaDataType from "../components/metadata/SelectMetaDataType";
import NotesTooltip from "../components/NotesTooltip";
import CheckBox from "../components/CheckBox";

export default class System extends React.PureComponent {

    constructor(props) {
        super(props);
        const tabs = props.currentUser.featureToggles.map(feature => feature.toLowerCase());
        this.state = {
            tabs: tabs,
            selectedTab: tabs[0],
            validationResults: undefined,
            orphansResults: undefined,
            findMyDataInput: "",
            findMyDataEntityType: "saml20_sp",
            findMyDataResults: undefined,
            pushPreviewResults: undefined,
            pushResults: undefined,
            showNonRestorable: true,
            loading: false,
            copiedToClipboardClassName: "",
            confirmationDialogOpen: false,
            confirmationQuestion: "",
            confirmationDialogAction: () => this,
            cancelDialogAction: () => this.setState({confirmationDialogOpen: false})
        };
    }

    componentDidMount() {
        ping();
    }

    runValidations = (e) => {
        stop(e);
        if (this.state.loading) {
            return;
        }
        this.setState({loading: true});
        validate().then(json => this.setState({validationResults: json, loading: false}));
    };

    runOrphans = (e) => {
        stop(e);
        if (this.state.loading) {
            return;
        }
        this.setState({loading: true});
        orphans().then(json => this.setState({orphansResults: json, loading: false}));
    };


    switchTab = tab => e => {
        stop(e);
        this.setState({selectedTab: tab});
        if (tab !== "push_preview") {
            this.setState({pushPreviewResults: undefined});
        }
        if (tab !== "push") {
            this.setState({pushResults: undefined, pushPreviewResults: undefined});
        }
    };

    renderTab = (tab, selectedTab) =>
        <span key={tab} className={tab === selectedTab ? "active" : ""} onClick={this.switchTab(tab)}>
            {I18n.t(`playground.${tab}`)}
        </span>;

    runPush = e => {
        stop(e);
        if (this.state.loading) {
            return;
        }
        this.setState({loading: true});
        push().then(json => {
            this.setState({loading: false, pushResults: json.deltas});
            const ok = json.status === "OK";
            const msg = ok ? "playground.pushedOk" : "playground.pushedNotOk";
            setFlash(I18n.t(msg, {name: this.props.currentUser.push.name}), ok ? "info" : "error");
        });
    };

    renderDeltaValue = value => {
        if (value === null) {
            return "null";
        }
        return value.toString();
    };

    runPushPreview = e => {
        stop(e);
        if (this.state.loading) {
            return;
        }
        this.setState({loading: true});
        pushPreview().then(json => this.setState({pushPreviewResults: json, loading: false}));
    };

    findMyData = e => {
        stop(e);

        const {findMyDataInput, findMyDataEntityType} = this.state;
        if (findMyDataInput.length > 3) {
            const metaDataSearch = {"entityid": `.*${findMyDataInput}.*`};
            search(metaDataSearch, `${findMyDataEntityType}_revision`)
                .then(json => this.setState({findMyDataResults: json}));
        }
    };

    renderFindMyData = () => {
        const {findMyDataInput, findMyDataEntityType, findMyDataResults, showNonRestorable} = this.state;
        const showResults = findMyDataResults && findMyDataResults.length > 0;
        const noResults = findMyDataResults && findMyDataResults.length === 0;
        const classNameSearch = findMyDataInput.length > 3 ? "green" : "disabled grey";
        return (
            <section className="find-my-data">
                <p>{I18n.t("playground.findMyDataInfo")}</p>
                <section className="find-my-data-controls">
                    <section className="search-input-container">
                        <input className="search-input" type="text" value={findMyDataInput}
                               onChange={e => this.setState({findMyDataInput: e.target.value})}
                               onKeyPress={e => e.key === "Enter" ? this.findMyData(e) : false}/>
                        <i className="fa fa-search"></i>
                    </section>
                    <a className={`${classNameSearch} search button`} onClick={this.findMyData}>
                        {I18n.t("playground.search")}<i className="fa fa-search"></i></a>
                    <SelectMetaDataType onChange={val => this.setState({findMyDataEntityType: val})}
                                        state={findMyDataEntityType}
                                        configuration={this.props.configuration}
                                        defaultToFirst={true}/>
                    <CheckBox name="filter-live" value={showNonRestorable}
                              info={I18n.t("playground.displayNonRestorable")} readOnly={!showResults}
                              onChange={() => this.setState({showNonRestorable: !this.state.showNonRestorable})}/>
                </section>
                {showResults && this.renderMyDataResults(findMyDataResults, showNonRestorable)}
                {noResults && <p>{I18n.t("playground.findMyDataNoResults")}</p>}
            </section>
        );
    };

    terminationDate = revision => revision.terminated ? new Date(revision.terminated).toLocaleDateString() : "";

    doRestoreDeleted = (id, revisionType, parentType, number) => () => {
        this.setState({confirmationDialogOpen: false});
        restoreDeletedRevision(id, revisionType, parentType).then(json => {
            if (json.exception) {
                setFlash(json.validations, "error");
            } else {
                const name = json.data.metaDataFields["name:en"] || json.data.metaDataFields["name:nl"] || "this service";
                setFlash(I18n.t("metadata.flash.restored", {
                    name: name,
                    revision: number,
                    newRevision: json.revision.number
                }));
                this.props.history.replace(`/dummy`);
                setTimeout(() => this.props.history.replace(`/metadata/${json.type}/${json.id}`), 50);
            }
        });
    };

    restoreDeleted = (entity, number, revisionType, parentType, isTerminated) => e => {
        stop(e);
        if (isTerminated) {
            this.setState({
                confirmationDialogOpen: true,
                confirmationQuestion: I18n.t("playground.restoreConfirmation", {
                    name: entity.data.metaDataFields["name:en"] || entity.data.entityid,
                    number: number
                }),
                confirmationDialogAction: this.doRestoreDeleted(entity._id, revisionType, parentType, number)
            });
        }
    };


    renderMyDataResults = (findMyDataResults, showNonRestorable) => {
        const headers = ["status", "name", "entityid", "terminated", "revisionNumber", "updatedBy", "revisionNote", "notes", "nope"];
        const {findMyDataEntityType} = this.state;
        const filteredDataResults = showNonRestorable ? findMyDataResults :
            findMyDataResults.filter(entity => entity.revision.terminated);
        return <section>
            <table className="find-my-data-results">
                <thead>
                <tr>
                    {headers.map(h => <th key={h}
                                          className={`find-my-data-${h}`}>{I18n.t(`playground.headers.${h}`)}</th>)}

                </tr>
                </thead>
                <tbody>
                {filteredDataResults.map((entity, index) => {
                    const metaDataFields = entity.data.metaDataFields || {};
                    const revision = entity.revision || {};
                    const isTerminated = entity.revision.terminated;
                    const restoreClassName = `button ${isTerminated ? "blue" : "grey"}`;
                    return (<tr key={`${entity.data.entityid}_${index}`}>
                        <td className="state">{I18n.t(`metadata.${entity.data.state}`)}</td>
                        <td className="name">{metaDataFields["name:en"] || metaDataFields["name:nl"]}</td>
                        <td className="entityId">{entity.data.entityid}</td>
                        <td className="terminated">{this.terminationDate(revision)}</td>
                        <td className="revisionNumber">{revision.number}</td>
                        <td className="updatedBy">{revision.updatedBy}</td>
                        <td className="revisionNote">{entity.data.revisionnote}</td>
                        <td className="notes">
                            {isEmpty(entity.data.notes) ? <span></span> :
                                <NotesTooltip identifier={entity.data.entityid} notes={entity.data.notes}/>}
                        </td>
                        <td><a className={restoreClassName} href={`/restore/${entity._id}`}
                               onClick={this.restoreDeleted(entity, revision.number,
                                   `${findMyDataEntityType}_revision`,
                                   findMyDataEntityType, isTerminated)}
                               disabled={!isTerminated}>
                            {I18n.t("revisions.restore")}</a>
                        </td>
                    </tr>);
                })}
                </tbody>

            </table>
        </section>;
    };

    renderPush = () => {
        const {loading, pushResults} = this.state;
        const {currentUser} = this.props;
        const action = () => {
            this.setState({confirmationDialogOpen: false});
            this.runPush();
        };
        const showNoDeltas = pushResults !== undefined && pushResults.length === 0;
        const showDeltas = pushResults !== undefined && pushResults.length > 0;
        return (
            <section className="push">
                <p>{I18n.t("playground.pushInfo", {url: currentUser.push.url, name: currentUser.push.name})}</p>
                <a className={`button ${loading ? "grey disabled" : "green"}`}
                   onClick={() => this.setState({
                       confirmationDialogOpen: true,
                       confirmationQuestion: I18n.t("playground.pushConfirmation", {
                           url: currentUser.push.url,
                           name: currentUser.push.name
                       }),
                       confirmationDialogAction: action
                   })}>{I18n.t("playground.runPush")}
                    <i className="fa fa-refresh"></i>
                </a>
                <section className="deltas">
                    {showNoDeltas && <p className="push-result-info">{I18n.t("playground.pushResults.noDeltas")}</p>}
                    {showDeltas &&
                    <p className="push-result-info differences">{I18n.t("playground.pushResults.deltas")}</p>}
                    {showDeltas && <table className="push-results">
                        <thead>
                        <tr>
                            <th className="entityId">{I18n.t("playground.pushResults.entityId")}</th>
                            <th className="attribute">{I18n.t("playground.pushResults.attribute")}</th>
                            <th className="prePushValue">{I18n.t("playground.pushResults.prePushValue")}</th>
                            <th className="postPushValue">{I18n.t("playground.pushResults.postPushValue")}</th>
                        </tr>
                        </thead>
                        <tbody>
                        {pushResults.map((delta, index) =>
                            <tr key={`${index}`}>
                                <td>{delta.entityId}</td>
                                <td>{delta.attribute}</td>
                                <td>{this.renderDeltaValue(delta.prePushValue)}</td>
                                <td>{this.renderDeltaValue(delta.postPushValue)}</td>
                            </tr>
                        )}
                        </tbody>
                    </table>}
                </section>
            </section>
        );
    };

    copiedToClipboard = () => {
        this.setState({copiedToClipboardClassName: "copied"});
        setTimeout(() => this.setState({copiedToClipboardClassName: ""}), 5000);
    };

    renderPushPreview = () => {
        const {pushPreviewResults, loading, copiedToClipboardClassName} = this.state;
        const {currentUser} = this.props;
        const json = pushPreviewResults ? JSON.stringify(pushPreviewResults) : "";
        const showCopy = (pushPreviewResults && json.length > 0 && json.length < 150 * 1000);
        const showSelectText = !showCopy && json.length > 0;
        return (
            <section className="push">
                <p>{I18n.t("playground.pushPreviewInfo", {name: currentUser.push.name})}</p>
                <a className={`button ${loading ? "grey disabled" : "green"}`}
                   onClick={this.runPushPreview}>{I18n.t("playground.runPushPreview")}
                    <i className="fa fa-refresh"></i></a>
                {showCopy &&
                <CopyToClipboard text={json} onCopy={this.copiedToClipboard}>
                    <span className={`button green ${copiedToClipboardClassName}`}>
                       Copy JSON to clipboard <i className="fa fa-clone"></i>
                    </span>
                </CopyToClipboard>
                }
                {showSelectText &&
                <span className="button green" onClick={() => {
                    const range = document.createRange();
                    const sel = window.getSelection();
                    range.selectNodeContents(this.pushPreviewResults);
                    sel.removeAllRanges();
                    sel.addRange(range);
                }}>
                       Select all JSON <i className="fa fa-clone"></i>
                    </span>
                }
                {pushPreviewResults &&
                <section className="results pushPreviewResults" ref={ref => this.pushPreviewResults = ref}>
                    {json}
                </section>}
            </section>
        );
    };

    renderValidate = () => {
        const {validationResults, loading} = this.state;
        return (
            <section className="validate">
                <p>All latest revisions of the metadata with a production status will be validated against
                    the JSON schema. This validation is performed on every create and update and preferably
                    all metadata is valid.</p>
                <a className={`button ${loading ? "grey disabled" : "green"}`}
                   onClick={this.runValidations}>{I18n.t("playground.runValidation")}
                    <i className="fa fa-check" aria-hidden="true"></i></a>
                {validationResults &&
                <section className="results">
                    <JsonView json={validationResults}/>
                </section>}
            </section>
        );
    };

    renderOrphans = () => {
        const {orphansResults, loading} = this.state;
        const action = e => {
            stop(e);
            this.setState({confirmationDialogOpen: false});
            deleteOrphanedReferences().then(() => {
                setFlash(I18n.t("playground.orphansDeleted"));
                this.runOrphans();
            });
        };

        return (
            <section className="orphans">
                <p>The allowed entries in both IdP and SP whiteListings and the Service Providers in the IdP Consent
                    Management are references to
                    other MetaData instances. The referential integrity is not enforced by the underlying storage. Run
                    the referential integrity check to identify all
                    references to MetaData instances that do not exist.</p>
                <a className={`button ${loading ? "grey disabled" : "green"}`}
                   onClick={this.runOrphans}>{I18n.t("playground.runOrphans")}
                    <i className="fa fa-check" aria-hidden="true"></i></a>
                {orphansResults &&
                <section className="results">
                    <JsonView json={orphansResults}/>
                </section>}
                {(orphansResults && orphansResults.length > 0) &&
                <a className={`button ${loading ? "grey disabled" : "blue"}`}
                   onClick={() => this.setState({
                       confirmationDialogOpen: true,
                       confirmationQuestion: I18n.t("playground.orphanConfirmation"),
                       confirmationDialogAction: action
                   })}>{I18n.t("playground.deleteOrphans")}
                    <i className="fa fa-trash"></i>
                </a>}
            </section>
        );
    };

    renderCurrentTab = selectedTab => {
        switch (selectedTab) {
            case "validation" :
                return this.renderValidate();
            case "orphans" :
                return this.renderOrphans();
            case "push":
                return this.renderPush();
            case "push_preview":
                return this.renderPushPreview();
            case "find_my_data":
                return this.renderFindMyData();
            default :
                throw new Error(`Unknown tab: ${selectedTab}`);
        }
    };

    render() {
        const {tabs, selectedTab, confirmationDialogOpen, confirmationQuestion, confirmationDialogAction, cancelDialogAction} = this.state;
        return (
            <div className="playground">
                <ConfirmationDialog isOpen={confirmationDialogOpen}
                                    cancel={cancelDialogAction}
                                    confirm={confirmationDialogAction}
                                    question={confirmationQuestion}/>
                <section className="tabs">
                    {tabs.map(tab => this.renderTab(tab, selectedTab))}

                </section>
                {this.renderCurrentTab(selectedTab)}
            </div>
        );
    }
}

System.propTypes = {
    history: PropTypes.object.isRequired,
    configuration: PropTypes.array.isRequired,
    currentUser: PropTypes.object.isRequired
};

