import React from "react";
import I18n from "i18n-js";
import {isEmpty, stop} from "../utils/Utils";
import ConfirmationDialog from "../components/ConfirmationDialog";
import "./Policies.scss";
import {
    getMigratedPdPPolicies,
    getPdPPolicies,
    getPlaygroundPolicies,
    importPdPPolicies,
    policyAttributes,
    policySAMLAttributes
} from "../api";
import ReactDiffViewer from 'react-diff-viewer-continued';

export default class Policies extends React.PureComponent {

    constructor(props) {
        super(props);
        const tabs = ["import", "push", "playground"];
        this.state = {
            tabs: tabs,
            selectedTab: tabs[0],
            importResults: {},
            playGroundData: {},
            pdpPolicies: [],
            pdpMigratedPolicies: [],
            loading: true,
            copiedToClipboardClassName: "",
            confirmationDialogOpen: false,
            confirmationQuestion: "",
            confirmationDialogAction: () => this,
            cancelDialogAction: () => this.setState({confirmationDialogOpen: false})
        };
    }

    componentDidMount() {
        this.setState({loading: true})
        Promise.all([getPlaygroundPolicies(), policyAttributes(), policySAMLAttributes()])
            .then(res => this.setState({
                policies: res[0],
                attributes: res[1],
                samlAttributes: res[2],
                loading: false
            }));
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
        if (tab !== "playground") {
            this.setState({playGroundData: {}});
        }
        if (tab !== "push") {
            this.setState({
                pdpPolicies: [],
                pdpMigratedPolicies: []
            });
        }
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
                <p>For now PdP does not overwrite the current policies, but stores them in a policy migrations table</p>
                <a className={`button ${loading ? "grey disabled" : "green"}`}
                   onClick={this.runImport}>
                    {I18n.t("policies.runImport")}
                </a>
                {!isEmpty(importResults) &&
                    <section className="results">
                        <h2>Imported policies</h2>
                        <ul className="policies">
                            {importResults.imported.map(metaData => <li>
                                <span>{metaData.data.name}</span>
                                <span>{metaData.data.description}</span>
                            </li>)}
                        </ul>
                        <h2>Not imported policies</h2>
                        <ul className="policies">
                            {importResults.errors.map(data => <li>
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

        return (
            <section className="import">
                <p>After importing the current PdP policies into Manage and subsequently pushing those Manage policies
                    to PdP,
                    we now can compare the original PdP policies with the pushed ones.</p>
                <a className={`button ${loading ? "grey disabled" : "green"}`}
                   onClick={this.componentDidMount}>
                    {I18n.t("policies.reload")}
                </a>
                {!isEmpty(pdpPolicies) &&
                    <section className="results">
                        <h2>Not imported policies</h2>
                        <ul className="policies">
                            {missingPolicies.map(policy => <li>
                                <span>{policy.name}</span>
                                <span>{policy.description}</span>
                            </li>)}
                        </ul>
                        <h2>Policies compared</h2>
                        <ul className="policies">
                            {pdpMigratedPolicies.map(policy => <li>
                                <span>{policy.name}</span>
                                <span>{policy.description}</span>
                                <ReactDiffViewer oldValue={pdpPolicies.find(p => p.name === policy.name).xml}
                                                 newValue={policy.xml}
                                                 splitView={true}/>
                            </li>)}
                        </ul>

                    </section>}
            </section>
        );
    };

    renderPlayground = () => {
        return (
            <section className="playground">
                <span>TODO</span>
            </section>
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
