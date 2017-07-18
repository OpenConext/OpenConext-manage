import React from "react";
import I18n from "i18n-js";
import PropTypes from "prop-types";
import {Link} from "react-router-dom";
import {migrate, ping, search, validate, push} from "../api";
import {isEmpty, stop} from "../utils/Utils";
import JsonView from "react-pretty-json";
import ConfirmationDialog from "../components/ConfirmationDialog";
import SelectMetaDataType from "../components/metadata/SelectMetaDataType";
import "./Playground.css";
import "react-pretty-json/assets/json-view.css";
import SelectNewMetaDataField from "../components/metadata/SelectNewMetaDataField";

export default class Playground extends React.PureComponent {

    constructor(props) {
        super(props);
        const tabs = props.currentUser.featureToggles.map(feature => feature.toLowerCase()).concat(["extended_search"]);
        this.state = {
            tabs: tabs,
            selectedTab: tabs[0],
            migrationResults: undefined,
            validationResults: undefined,
            pushResults: undefined,
            loading: false,
            confirmationDialogOpen: false,
            confirmationDialogAction: () => {
                this.setState({confirmationDialogOpen: false});
                this.runMigration();
            },
            cancelDialogAction: () => this.setState({confirmationDialogOpen: false}),
            selectedType: "saml20_sp",
            searchAttributes: {},
            searchResults: undefined,
            newMetaDataFieldKey: null
        };
    }

    componentDidMount() {
        ping();
    }

    componentDidUpdate = () => {
        const newMetaDataFieldKey = this.state.newMetaDataFieldKey;
        if (!isEmpty(newMetaDataFieldKey) && !isEmpty(this.newMetaDataField)) {
            this.newMetaDataField.focus();
            this.newMetaDataField = null;
            this.setState({newMetaDataFieldKey: null})
        }
    };


    runMigration = (e) => {
        stop(e);
        if (this.state.loading) {
            return;
        }
        this.setState({loading: true});
        migrate().then(json => this.setState({migrationResults: json, loading: false}));
    };

    runValidations = (e) => {
        stop(e);
        if (this.state.loading) {
            return;
        }
        this.setState({loading: true});
        validate().then(json => this.setState({validationResults: json, loading: false}));
    };

    switchTab = tab => e => {
        stop(e);
        this.setState({selectedTab: tab});
    };

    renderTab = (tab, selectedTab) =>
        <span key={tab} className={tab === selectedTab ? "active" : ""} onClick={this.switchTab(tab)}>
            {I18n.t(`playground.${tab}`)}
        </span>;

    addSearchKey = key => {
        const newSearchAttributes = {...this.state.searchAttributes};
        newSearchAttributes[key] = "";
        this.setState({searchAttributes: newSearchAttributes, newMetaDataFieldKey: key});
    };

    changeSearchValue = key => e => {
        const newSearchAttributes = {...this.state.searchAttributes};
        newSearchAttributes[key] = e.target.value;
        this.setState({searchAttributes: newSearchAttributes});
    };

    deleteSearchField = key => e => {
        const newSearchAttributes = {...this.state.searchAttributes};
        delete newSearchAttributes[key];
        this.setState({searchAttributes: newSearchAttributes});

    };
    doSearch = e => {
        stop(e);
        const {selectedType, searchAttributes} = this.state;
        const keys = Object.keys(searchAttributes);
        const enabled = keys.length > 0;
        if (enabled) {
            const metaDataSearch = {};
            keys.forEach(key => {
                metaDataSearch[`metaDataFields.${key}`] = searchAttributes[key];
            });
            search(metaDataSearch, selectedType).then(json => this.setState({searchResults: json}));
        }

    };

    reset = e => {
        stop(e);
        this.setState({searchAttributes: {}, searchResults: undefined});
    };

    newMetaDataFieldRendered = (ref, autoFocus) => {
        if (autoFocus) {
            this.newMetaDataField = ref;
        }
    };

    renderSearch = () => {
        const {configuration} = this.props;
        const {selectedType, searchAttributes, searchResults} = this.state;
        const conf = configuration.find(conf => conf.title === selectedType);
        const enabled = Object.keys(searchAttributes).length > 0;
        const searchHeaders = ["status", "name", "entityid"];
        const hasNoResults = searchResults && searchResults.length === 0;
        return (
            <section className="extended-search">
                <p>Select a Metadata type and metadata fields. The query will AND the different inputs.
                    Wildcards like <span className="code">.*surf.*</span> are translated to a regular expression search.
                    Specify booleans with <span className="code">0</span> or <span className="code">1</span> and
                    leave the value empty for a <span className="code">does not exists</span> query.</p>
                <SelectMetaDataType onChange={value => this.setState({selectedType: value})}
                                    configuration={configuration}
                                    state={selectedType}/>
                {enabled &&
                <table className="metadata-search-table">
                    <tbody>
                    {Object.keys(searchAttributes).map(key =>
                        <tr key={key}>
                            <td className="key">{key}</td>
                            <td className="value">
                                <input
                                    ref={ref => this.newMetaDataFieldRendered(ref, this.state.newMetaDataFieldKey === key)}
                                    type="text" value={searchAttributes[key]}
                                    onChange={this.changeSearchValue(key)}/>
                            </td>
                            <td className="trash">
                                <span onClick={this.deleteSearchField(key)}><i className="fa fa-trash-o"></i></span>
                            </td>
                        </tr>
                    )}
                    </tbody>
                </table>
                }
                <SelectNewMetaDataField configuration={conf} onChange={this.addSearchKey}
                                        metaDataFields={searchAttributes} placeholder={"Search and add metadata keys"}/>

                <a className="reset button grey" onClick={this.reset}>Reset<i className="fa fa-times"></i></a>
                <a className={`button ${enabled ? "green" : "disabled grey"}`} onClick={this.doSearch}>Search<i
                    className="fa fa-search-plus"></i></a>

                {hasNoResults && <h2>{I18n.t("playground.no_results")}</h2>}
                {(searchResults && !hasNoResults) && <table className="search-results">
                    <thead>
                    {searchHeaders.map(header => <th key={header}
                                                     className={header}>{I18n.t(`playground.headers.${header}`)}</th>)}
                    </thead>
                    <tbody>
                    {searchResults.map(entity => <tr key={entity.data.entityid}>
                        <td className="state">{I18n.t(`metadata.${entity.data.state}`)}</td>
                        <td className="name">{entity.data.metaDataFields["name:en"] || entity.data.metaDataFields["name:nl"]}</td>
                        <td className="entityId">
                            <Link to={`/metadata/${selectedType}/${entity["_id"]}`}
                                  target="_blank">{entity.data.entityid}</Link>
                        </td>

                    </tr>)}
                    </tbody>

                </table>}
            </section>
        );
    };

    runPush = e => {
        stop(e);
        if (this.state.loading) {
            return;
        }
        this.setState({loading: true});
        push().then(json => this.setState({pushResults: json, loading: false}));

    };

    renderPush = () => {
        const {pushResults, loading} = this.state;
        return (
            <section className="push">
                <p>{I18n.t("playground.pushInfo")}</p>
                <a className={`button ${loading ? "grey disabled" : "green"}`}
                   onClick={this.runPush}>{I18n.t("playground.runPush")}
                    <i className="fa fa-refresh" aria-hidden="true"></i></a>
                {pushResults &&
                <section className="results pushResults">
                    {JSON.stringify(pushResults, null, '\t')}
                </section>}
            </section>
        );
    };

    renderMigrate = () => {
        const {migrationResults, loading} = this.state;
        return (
            <section className="migrate">
                <p>The migration will query the janus database - or a copy based on the server configuration - and
                    migrate all data to MongoDB collections.</p>
                <a className={`button ${loading ? "grey disabled" : "green"}`}
                   onClick={() => this.setState({confirmationDialogOpen: true})}>{I18n.t("playground.runMigration")}
                    <i className="fa fa-retweet" aria-hidden="true"></i></a>
                {migrationResults &&
                <section className="results">
                    <JsonView json={migrationResults}/>
                </section>}
            </section>
        );
    };

    renderValidate = () => {
        const {validationResults, loading} = this.state;
        return (
            <section className="validate">
                <p>All latest revisions of the migrated metadata with a production status will be validated against
                    the JSON schema. This validation is performed on every create and update and preferably
                    all migrated metadata is valid.</p>
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

    renderCurrentTab = selectedTab => {
        switch (selectedTab) {
            case "migration" :
                return this.renderMigrate();
            case "validation" :
                return this.renderValidate();
            case "push":
                return this.renderPush();
            case "extended_search":
                return this.renderSearch();
            default :
                throw new Error(`Unknown tab: ${selectedTab}`);
        }
    };

    render() {
        const {tabs, selectedTab, confirmationDialogOpen, confirmationDialogAction, cancelDialogAction} = this.state;
        return (
            <div className="playground">
                <ConfirmationDialog isOpen={confirmationDialogOpen}
                                    cancel={cancelDialogAction}
                                    confirm={confirmationDialogAction}
                                    question={I18n.t("playground.migrationConfirmation")}/>
                <section className="tabs">
                    {tabs.map(tab => this.renderTab(tab, selectedTab))}

                </section>
                {this.renderCurrentTab(selectedTab)}
            </div>
        );
    }
}

Playground.propTypes = {
    history: PropTypes.object.isRequired,
    configuration: PropTypes.array.isRequired,
    currentUser: PropTypes.object.isRequired
};

