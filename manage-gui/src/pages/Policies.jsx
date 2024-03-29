import React from "react";
import I18n from "i18n-js";
import {isEmpty, stop} from "../utils/Utils";
import ConfirmationDialog from "../components/ConfirmationDialog";
import "./Policies.scss";
import {getMigratedPdPPolicies, getPdPPolicies, importPdPPolicies} from "../api";
import ReactDiffViewer, {DiffMethod} from 'react-diff-viewer-continued';
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
            pdpPolicies: [],
            pdpMigratedPolicies: [],
            loading: false,
            copiedToClipboardClassName: "",
            confirmationDialogOpen: false,
            confirmationQuestion: "",
            confirmationDialogAction: () => this,
            cancelDialogAction: () => this.setState({confirmationDialogOpen: false})
        };
    }

    initialState = e => {
        stop(e);
        this.setState({loading: true});
        Promise.all([getMigratedPdPPolicies(), getPdPPolicies()])
            .then(res => this.setState({
                pdpMigratedPolicies: res[0],
                pdpPolicies: res[1],
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
            Promise.all([getMigratedPdPPolicies(), getPdPPolicies()])
                .then(res => this.setState({
                    pdpMigratedPolicies: res[0],
                    pdpPolicies: res[1],
                    loading: false
                }));
        }
        this.props.navigate(`/policies/${tab}`);
    };

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
        const {importResults, loading} = this.state;
        return (
            <section className="import">
                <p>Import the current PdP policies into Manage. Once imported they can be pushed.</p>
                <p>For now PdP does not overwrite the current policies in the push-endpoint, but stores them in a policy
                    migrations table</p>
                <a className={`button ${loading ? "grey disabled" : "green"}`}
                   onClick={this.runImport}>
                    {I18n.t("policies.runImport")}
                </a>
                {!isEmpty(importResults) &&
                    <section className="results">
                        <h2>Imported policies</h2>
                        <ul className="policies">
                            {importResults.imported.map((metaData,index ) => <li key={index}>
                                <span>{metaData.data.name}</span>
                                <span>{metaData.data.description}</span>
                            </li>)}
                        </ul>
                        <h2>Not imported policies</h2>
                        <ul className="policies">
                            {importResults.errors.map((data, index) => <li key={index}>
                                <span>{data.name}</span>
                                <span>{data.error}</span>
                            </li>)}
                        </ul>
                    </section>}
            </section>
        );
    };

    renderPush = () => {
        const {pdpMigratedPolicies, pdpPolicies, loading} = this.state;
        const missingPolicies = pdpPolicies
            .filter(p => !pdpMigratedPolicies.some(mp => mp.name === p.name));
        const forgotToPush = pdpMigratedPolicies.some(policy => !pdpPolicies.find(p => p.name === policy.name))
        return (
            <section className="import">
                <p>After importing the current PdP policies into Manage and subsequently pushing those Manage policies
                    to PdP,
                    we now can compare the original PdP policies with the pushed ones.</p>
                <a className={`button ${loading ? "grey disabled" : "green"}`}
                   onClick={e => this.initialState(e)}>
                    {I18n.t("policies.reload")}
                </a>
                {(!isEmpty(pdpPolicies)) &&
                    <section className="results">
                        {!isEmpty(missingPolicies) && <div>
                        <h2>Not imported policies</h2>
                        <ul className="policies">
                            {missingPolicies.map((policy, index) => <li key={index}>
                                <span>{policy.name}</span>
                                <span>{policy.description}</span>
                            </li>)}
                        </ul>
                        </div>}
                        <h2>Policies compared</h2>
                        {!forgotToPush && <ul className="policies">
                            {pdpMigratedPolicies
                                .map((policy, index) => <li key={index}>
                                <span>{policy.name}</span>
                                <span>{policy.description}</span>
                                <ReactDiffViewer oldValue={pdpPolicies.find(p => p.name === policy.name).xml}
                                                 newValue={policy.xml}
                                                 compareMethod={DiffMethod.TRIMMED_LINES}
                                                 splitView={true}/>
                            </li>)}
                        </ul>}
                        {forgotToPush &&
                            <p>You did not push the latest policies to PdP. Can't compare before you do.</p>}
                    </section>}
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
            case "playground" :
                return this.renderPlayground();
            case "push" :
                return this.renderPush();
            case "missing_enforcements" :
                return this.renderMissingEnforcements();
            default :
                throw new Error(`Unknown tab: ${selectedTab}`);
        }
    };

    render() {
        const {
            tabs,
            selectedTab,
            confirmationDialogOpen,
            confirmationQuestion,
            confirmationDialogAction,
            cancelDialogAction
        } = this.state;
        return (
            <div className="mod-policies">
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
export default withRouterHooks(Policies);
