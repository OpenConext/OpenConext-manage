import React from "react";
import I18n from "i18n-js";
import PropTypes from "prop-types";
import {migrate, ping, push, pushPreview, validate} from "../api";
import {stop} from "../utils/Utils";
import JsonView from "react-pretty-json";
import ConfirmationDialog from "../components/ConfirmationDialog";
import "./System.css";
import "react-pretty-json/assets/json-view.css";
import {setFlash} from "../utils/Flash";

export default class System extends React.PureComponent {

    constructor(props) {
        super(props);
        const tabs = props.currentUser.featureToggles.map(feature => feature.toLowerCase());
        this.state = {
            tabs: tabs,
            selectedTab: tabs[0],
            migrationResults: undefined,
            validationResults: undefined,
            pushResults: undefined,
            loading: false,
            confirmationDialogOpen: false,
            confirmationQuestion: "",
            confirmationDialogAction: () => this,
            cancelDialogAction: () => this.setState({confirmationDialogOpen: false})
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

    runPush = e => {
        stop(e);
        if (this.state.loading) {
            return;
        }
        this.setState({loading: true});
        push().then(json => {
            this.setState({loading: false});
            const ok = json.status === "OK";
            const msg = ok ? "playground.pushedOk" : "playground.pushedNotOk";
            setFlash(I18n.t(msg, {name: this.props.currentUser.push.name}), ok ? "info" : "error");
        });
    };

    runPushPreview = e => {
        stop(e);
        if (this.state.loading) {
            return;
        }
        this.setState({loading: true});
        pushPreview().then(json => this.setState({pushResults: json, loading: false}));
    };

    renderPush = () => {
        const {loading} = this.state;
        const {currentUser} = this.props;
        const action = () => {
            this.setState({confirmationDialogOpen: false});
            this.runPush();
        };

        return (
            <section className="push">
                <p>{I18n.t("playground.pushInfo", {url: currentUser.push.url, name: currentUser.push.name})}</p>
                <a className={`button ${loading ? "grey disabled" : "green"}`}
                   onClick={() => this.setState({
                       confirmationDialogOpen: true,
                       confirmationQuestion: I18n.t("playground.pushConfirmation", {url: currentUser.push.url, name: currentUser.push.name}),
                       confirmationDialogAction: action
                   })}>{I18n.t("playground.runPush")}
                    <i className="fa fa-refresh"></i>
                </a>
            </section>
        );
    };

    renderPushPreview = () => {
        const {pushResults, loading} = this.state;
        const {currentUser} = this.props;
        return (
            <section className="push">
                <p>{I18n.t("playground.pushPreviewInfo", {name: currentUser.push.name})}</p>
                <a className={`button ${loading ? "grey disabled" : "green"}`}
                   onClick={this.runPushPreview}>{I18n.t("playground.runPushPreview")}
                    <i className="fa fa-refresh"></i></a>
                {pushResults &&
                <section className="results pushResults">
                    {JSON.stringify(pushResults)}
                </section>}
            </section>
        );
    };

    renderMigrate = () => {
        const {migrationResults, loading} = this.state;
        const action = () => {
            this.setState({confirmationDialogOpen: false});
            this.runMigration();
        };
        return (
            <section className="migrate">
                <p>The migration will query the janus database - or a copy based on the server configuration - and
                    migrate all data to MongoDB collections.</p>
                <a className={`button ${loading ? "grey disabled" : "green"}`}
                   onClick={() => this.setState({
                       confirmationDialogOpen: true,
                       confirmationQuestion: I18n.t("playground.migrationConfirmation"),
                       confirmationDialogAction: action
                   })}>{I18n.t("playground.runMigration")}
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
            case "push_preview":
                return this.renderPushPreview();
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

