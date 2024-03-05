import React from "react";
import I18n from "i18n-js";
import {stop} from "../utils/Utils";
import ConfirmationDialog from "../components/ConfirmationDialog";
import "./Policies.scss";
import JSONPretty from "react-json-pretty";
import ReactDiffViewer from 'react-diff-viewer-continued';

export default class Policies extends React.PureComponent {

    constructor(props) {
        super(props);
        const tabs = ["import", "playground"];
        this.state = {
            tabs: tabs,
            selectedTab: tabs[0],
            importResults: [],
            loading: false,
            copiedToClipboardClassName: "",
            confirmationDialogOpen: false,
            confirmationQuestion: "",
            confirmationDialogAction: () => this,
            cancelDialogAction: () => this.setState({confirmationDialogOpen: false})
        };
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
    };

    renderTab = (tab, selectedTab) =>
        <span key={tab}
              className={tab === selectedTab ? "active" : ""}
              onClick={this.switchTab(tab)}>
            {I18n.t(`policies.${tab}`)}
        </span>;

    runImport = () => {

    }

    renderImport = () => {
        const {importResults, loading} = this.state;
        return (
            <section className="import">
                <p>Import the current PdP policies into Manage. Once imported they can be pushed.</p>
                <p>For now PdP does not overwrite the current policies, but  stores then in a policy migrations table</p>
                <a className={`button ${loading ? "grey disabled" : "green"}`}
                   onClick={this.runImport}>
                    {I18n.t("policies.runImport")}
                </a>
                {importResults &&
                    <section className="results">

                        <ReactDiffViewer oldValue={oldCode} newValue={newCode} splitView={true} />
                    </section>}
            </section>
        );
    };


    renderCurrentTab = selectedTab => {
        switch (selectedTab) {
            case "import" :
                return this.renderValidate();
            case "playground" :
                return this.renderOrphans();
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
