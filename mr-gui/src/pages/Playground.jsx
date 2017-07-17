import React from "react";
import I18n from "i18n-js";
import PropTypes from "prop-types";
import {migrate, ping, search, validate} from "../api";
import {stop} from "../utils/Utils";
import JsonView from "react-pretty-json";
import ConfirmationDialog from "../components/ConfirmationDialog";
import SelectMetaDataType from "../components/metadata/SelectMetaDataType";
import "./Playground.css";
import "react-pretty-json/assets/json-view.css";
import SelectNewMetaDataField from "../components/metadata/SelectNewMetaDataField";

export default class Playground extends React.PureComponent {

    constructor(props) {
        super(props);
        this.state = {
            tabs: ["migrate", "validate", "extended_search"],
            selectedTab: "migrate",
            migrationResults: undefined,
            validationResults: undefined,
            loading: false,
            confirmationDialogOpen: false,
            confirmationDialogAction: () => {
                this.setState({confirmationDialogOpen: false});
                this.runMigration();
            },
            cancelDialogAction: () => this.setState({confirmationDialogOpen: false}),
            selectedType: "saml20_sp",
            searchAttributes: {}
        };
    }

    componentDidMount() {
        ping();
    }

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
        this.setState({searchAttributes: newSearchAttributes});
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
            search(metaDataSearch, selectedType);
        }

    };

    renderSearch = () => {
        const {configuration} = this.props;
        const {selectedType, searchAttributes} = this.state;
        const conf = configuration.find(conf => conf.title === selectedType);
        const enabled = Object.keys(searchAttributes).length > 0;
        return (
            <section className="extended-search">
                <p>Select a Metadata type and properties. The query will AND the different inputs.</p>
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
                                <input type="text" value={searchAttributes[key]}
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

                <a className={`button ${enabled ? "green" : "disabled grey"}`} onClick={this.doSearch}>Search<i
                    className="fa fa-search-plus"></i></a>
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
            case "migrate" :
                return this.renderMigrate();
            case "validate" :
                return this.renderValidate();
            case "extended_search":
                return this.renderSearch();
            default :
                throw new Error("Unknown tab");
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
};

