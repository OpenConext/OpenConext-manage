import React from "react";
import I18n from "i18n-js";
import PropTypes from "prop-types";
import {detail} from "../api";

const tabsSp = ["connection", "whitelist", "metadata", "arp", "manipulation"];
const tabsIdP = ["connection", "whitelist", "consent", "metadata", "manipulation"];

export default class New extends React.PureComponent {

    constructor(props) {
        super(props);
        this.state = {
            metaData: {},
            notFound: false,
            loaded: false,
            selectedTab: "connection"
        };
    }

    componentDidMount() {
        window.scrollTo(0, 0);
    }

    renderTab = tab =>
        <span key={tab} className={this.state.selectedTab == tab ? "active" : ""}>{I18n.t(`metadata.tabs.${tab}`)}</span>;

    render() {
        const {loaded, notFound, metaData, selectedTab} = this.state;
        const type = metaData.type;
        const tabs = type == "saml20_sp" ? tabsSp : tabsIdP;
        return (
            <div className="detail-metadata">
                <section className="tabs">
                    {tabs.map(tab => this.renderTab(tab))}
                </section>

            </div>
        );
    }
}

New.propTypes = {
    history: PropTypes.object.isRequired,
    configuration: PropTypes.object.isRequired,
};

