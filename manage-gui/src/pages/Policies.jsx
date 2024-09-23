import React from "react";
import I18n from "i18n-js";
import {isEmpty, stop} from "../utils/Utils";
import "./Policies.scss";
import {getPolicyPushAnalysis, importPdPPolicies} from "../api";
import PolicyPlayground from "../components/PolicyPlaygound";
import withRouterHooks from "../utils/RouterBackwardCompatability";
import PolicyMissingEnforcements from "../components/PolicyMissingEnforcements";

class Policies extends React.PureComponent {

    constructor(props) {
        super(props);
        const tabs = ["import", "push", "playground", "missing_enforcements"];
        const {tab = "import"} = props.params || {};
        this.state = {
            tabs: tabs,
            selectedTab: tab,
            importResults: {},
            showMoreImported: false,
            policyPushAnalysis: {differences:[], missing_policies:[]},
            loading: false,
            copiedToClipboardClassName: "",
        };
    }

    componentDidMount() {
        const {selectedTab} = this.state;
        if (selectedTab === "push") {
            this.initialState();
        }
    }


    initialState = e => {
        stop(e);
        this.setState({loading: true});
        getPolicyPushAnalysis()
            .then(res => this.setState({
                policyPushAnalysis: res,
                loading: false
            }));
    }

    switchTab = tab => e => {
        stop(e);
        this.setState({selectedTab: tab});
        if (tab !== "import") {
            this.setState({importResults: []});
        }
        if (tab !== "push") {
            this.setState({
                pdpPolicies: [],
                pdpMigratedPolicies: []
            });
        }
        if (tab === "push") {
            this.setState({loading: true});
            getPolicyPushAnalysis()
                .then(res => this.setState({
                    policyPushAnalysis: res,
                    loading: false
                }));
        }
        this.props.navigate(`/policies/${tab}`);
    };

    toggleShowMore = e => {
        stop(e);
        this.setState({showMoreImported: !this.state.showMoreImported})
    }

    renderTab = (tab, selectedTab) =>
        <span key={tab}
              className={tab === selectedTab ? "active" : ""}
              onClick={this.switchTab(tab)}>
            {I18n.t(`policies.${tab}`)}
        </span>;

    runImport = () => {
        this.setState({loading: true});
        importPdPPolicies()
            .then(res => this.setState({importResults: res, loading: false}))
    }

    renderImport = () => {
        const {importResults, showMoreImported, loading} = this.state;
        return (
            <section className="import">
                <p>Import the current PdP policies into Manage. Once imported they can be pushed.</p>
                <a className={`button ${loading ? "grey disabled" : "green"}`}
                   onClick={this.runImport}>
                    {I18n.t("policies.runImport")}
                </a>
                {!isEmpty(importResults) &&
                    <section className="results">
                        <h2>Not imported policies</h2>
                        <ul className="policies">
                            {importResults.errors.map((data, index) =>
                                <li key={index}>
                                    <span>{data.name}</span>
                                    <span>{data.error}</span>
                                </li>)}
                        </ul>
                        <h2>Imported policies</h2>
                        <a href={"/#show"}
                           onClick={this.toggleShowMore}>
                            {!showMoreImported ? "Show all" : "Hide"}
                        </a>
                        {showMoreImported && <ul className="policies">
                            {importResults.imported.map((metaData, index) =>
                                <li key={index}>
                                    <span>{metaData.data.name}</span>
                                    <span>{metaData.data.description}</span>
                                </li>)}
                        </ul>}
                    </section>}
            </section>
        );
    };

    renderPush = () => {
        const {policyPushAnalysis, loading} = this.state;
        return (
            <section className="import">
                <p>After importing the current PdP policies into Manage and subsequently pushing those Manage policies
                    to PdP, we now compare the original PdP policies with the pushed ones.</p>
                <a className={`button ${loading ? "grey disabled" : "green"}`}
                   onClick={e => this.initialState(e)}>
                    {I18n.t("policies.reload")}
                </a>
                <section className="results">
                    <h2># Total PDP policies </h2>
                    <p>{policyPushAnalysis.policy_count}</p>
                    <h2># Total active PDP policies </h2>
                    <p>{policyPushAnalysis.active_policy_count}</p>
                    <h2># Pushed policies</h2>
                    <p>{policyPushAnalysis.migrated_policy_count}</p>
                    <h2>Missing policies</h2>
                    {policyPushAnalysis.missing_policies.length === 0 && <p>None missing</p>}
                    <ul className="policies">
                        {policyPushAnalysis.missing_policies.map((policy, index) => <li key={index}>
                            <span>{policy.name}</span>
                            <span>{policy.description}</span>
                        </li>)}
                    </ul>
                    <h2>Diffs between policies</h2>
                    {policyPushAnalysis.differences.length === 0 && <p>No diffs</p>}
                    <ul className="policies">
                        {policyPushAnalysis.differences.map((diff, index) => <li key={index}>
                            <span>{Object.keys(diff)[0]}</span>
                            <span>{Object.values(diff)[0]}</span>
                        </li>)}
                    </ul>
                </section>
            </section>
        );
    };

    renderPlayground = () => {
        return (
            <PolicyPlayground/>
        );
    };

    renderMissingEnforcements = () => {
        return (
            <PolicyMissingEnforcements/>
        );
    };

    renderCurrentTab = selectedTab => {
        switch (selectedTab) {
            case "import" :
                return this.renderImport();
            case "push" :
                return this.renderPush();
            case "playground" :
                return this.renderPlayground();
            case "missing_enforcements" :
                return this.renderMissingEnforcements();
            default :
                throw new Error(`Unknown tab: ${selectedTab}`);
        }
    };

    render() {
        const {
            tabs,
            selectedTab
        } = this.state;
        return (
            <div className="mod-policies">
                <section className="tabs">
                    {tabs.map(tab => this.renderTab(tab, selectedTab))}
                </section>
                {this.renderCurrentTab(selectedTab)}
            </div>
        );
    }
}
export default withRouterHooks(Policies);
