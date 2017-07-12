import React from "react";
import I18n from "i18n-js";
import PropTypes from "prop-types";
import {migrate, ping, validate} from "../api";
import {stop} from "../utils/Utils";
import JsonView from 'react-pretty-json';

import "./Playground.css";
import "react-pretty-json/assets/json-view.css";

export default class New extends React.PureComponent {

    constructor(props) {
        super(props);
        this.state = {
            tabs: ["migrate", "validate"],
            selectedTab: "migrate",
            migrationResults: undefined,
            validationResults: undefined,
            loading: false
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

    renderMigrate = () => {
        const {migrationResults, loading} = this.state;
        return (
            <section className="migrate">
                <p>The migration will query the janus database - or a copy based on the server configuration - and
                migrate all data to MongoDB collections.</p>
                <a className={`button ${loading ? "grey disabled" : "green"}`} onClick={this.runMigration}>{I18n.t("playground.runMigration")}
                    <i className="fa fa-retweet" aria-hidden="true"></i></a>
                {migrationResults &&
                <section className="results">
                    <JsonView json={migrationResults} />
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
                <a className={`button ${loading ? "grey disabled" : "green"}`}onClick={this.runValidations}>{I18n.t("playground.runValidation")}
                    <i className="fa fa-check" aria-hidden="true"></i></a>
                {validationResults &&
                <section className="results">
                    <JsonView json={validationResults} />
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
            default :
                throw new Error("Unknown tab");
        }
    };

    render() {
        const {tabs, selectedTab} = this.state;
        return (
            <div className="playground">
                <section className="tabs">
                    {tabs.map(tab => this.renderTab(tab, selectedTab))}

                </section>
                {this.renderCurrentTab(selectedTab)}
            </div>
        );
    }
}

New.propTypes = {
    history: PropTypes.object.isRequired,
    configuration: PropTypes.array.isRequired,
};

