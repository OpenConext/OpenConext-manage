import React from "react";
import I18n from "i18n-js";
import PropTypes from "prop-types";
import {stop} from "../utils/Utils";
import "./System.scss";
import "react-json-pretty/themes/monikai.css";
import System from "./System";
import Support from "./Support";
import ChangeRequests from "./ChangeRequests";

export default class Staging extends React.PureComponent {

    constructor(props) {
        super(props);
        const tabs = ["changeRequests", "staging"]
        this.state = {
            tabs: tabs,
            selectedTab: tabs[0],
        };
    }

    switchTab = tab => e => {
        stop(e);
        this.setState({selectedTab: tab});
    };

    renderTab = (tab, selectedTab) =>
        <span key={tab} className={tab === selectedTab ? "active" : ""} onClick={this.switchTab(tab)}>
            {I18n.t(`staging.${tab}`)}
        </span>;

    renderCurrentTab = selectedTab => {
        const {history, configuration, currentUser} = this.props;
        switch (selectedTab) {
            case "changeRequests" :
                return <ChangeRequests history={history} configuration={configuration} currentUser={currentUser}/>
            case "staging" :
                return <Support history={history}/>;
            default :
                throw new Error(`Unknown tab: ${selectedTab}`);
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
