import React from "react";
import I18n from "i18n-js";
import {stop} from "../utils/Utils";
import "./Policies.scss";
import PolicyPlayground from "../components/PolicyPlaygound";
import withRouterHooks from "../utils/RouterBackwardCompatability";
import PolicyMissingEnforcements from "../components/PolicyMissingEnforcements";

class Policies extends React.PureComponent {

    constructor(props) {
        super(props);
        const tabs = ["playground", "missing_enforcements"];
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

    componentDidMount() {
        const {selectedTab} = this.state;
        if (selectedTab === "push") {
            this.initialState();
        }
    }


    initialState = e => {
        stop(e);
        this.setState({loading: false});
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

    renderMissingEnforcements = () => {
        return (
            <PolicyMissingEnforcements/>
        );
    };

    renderCurrentTab = selectedTab => {
        switch (selectedTab) {
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
