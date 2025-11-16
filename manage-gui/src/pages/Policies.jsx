import React from "react";
import I18n from "i18n-js";
import {stop} from "../utils/Utils";
import "./Policies.scss";
import PolicyPlayground from "../components/PolicyPlaygound";
import withRouterHooks from "../utils/RouterBackwardCompatability";
import PolicyConflicts from "../components/PolicyConflicts";

class Policies extends React.PureComponent {

    constructor(props) {
        super(props);
        const tabs = ["playground", "conflicts"];
        const {tab = "playground"} = props.params || {};
        this.state = {
            tabs: tabs,
            selectedTab: tab,
            importResults: {},
            showMoreImported: false,
            policyPushAnalysis: {differences: [], missing_policies: []},
            loading: false,
            copiedToClipboardClassName: "",
        };
    }

    switchTab = tab => e => {
        stop(e);
        this.setState({selectedTab: tab});
        this.props.navigate(`/policies/${tab}`);
    };

    renderTab = (tab, selectedTab) =>
        <span key={tab}
              className={tab === selectedTab ? "active" : ""}
              onClick={this.switchTab(tab)}>
            {I18n.t(`policies.${tab}`)}
        </span>;

    renderPlayground = () => {
        return (
            <PolicyPlayground/>
        );
    };

    renderConflicts = () => {
        return (
            <PolicyConflicts/>
        );
    };

    renderCurrentTab = selectedTab => {
        switch (selectedTab) {
            case "playground" :
                return this.renderPlayground();
            case "conflicts" :
                return this.renderConflicts();
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
