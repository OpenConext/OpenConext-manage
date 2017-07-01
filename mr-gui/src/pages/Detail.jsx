import React from "react";
import I18n from "i18n-js";
import PropTypes from "prop-types";

import ARP from "../components/metadata/ARP";
import Connection from "../components/metadata/Connection";
import ConsentDisabling from "../components/metadata/ConsentDisabling";
import Manipulation from "../components/metadata/Manipulation";
import MetaData from "../components/metadata/MetaData";
import WhiteList from "../components/metadata/WhiteList";

import {detail, whiteListing} from "../api";
import {stop} from "../utils/Utils";

import "./Detail.css";

const tabsSp = ["connection", "whitelist", "metadata", "arp", "manipulation"];
const tabsIdP = ["connection", "whitelist", "consent_disabling", "metadata", "manipulation"];

export default class Detail extends React.PureComponent {

    constructor(props) {
        super(props);
        this.state = {
            metaData: {},
            whiteListing: [],
            notFound: false,
            loaded: false,
            selectedTab: "connection"
        };
    }

    componentDidMount() {
        const {type, id} = this.props.match.params;
        detail(type, id).then(metaData => {
            this.setState({metaData: metaData, loaded: true});
            whiteListing(metaData.type === "saml20_sp" ? "saml20_idp" : "saml20_sp").then(whiteListing => this.setState({whiteListing: whiteListing}))
        }).catch(err => {
            if (err.response.status === 404) {
                this.setState({notFound: true, loaded: true});
            } else {
                throw err;
            }

        });
        window.scrollTo(0, 0);
    }

    switchTab = tab => e => {
        stop(e);
        this.setState({selectedTab: tab});
    };

    onChange = (name, value) => {
        //todo
    };

    renderTab = tab =>
        <span key={tab} className={this.state.selectedTab === tab ? "active" : ""}
              onClick={this.switchTab(tab)}>{I18n.t(`metadata.tabs.${tab}`)}</span>;

    renderCurrentTab = (tab, metaData, whiteListing) => {
        const configuration = this.props.configuration.filter(conf => conf.title === this.props.match.params.type)[0];
        switch (tab) {
            case "connection" :
                return <Connection metaData={metaData} onChange={this.onChange}/>;
            case "whitelist" :
                return <WhiteList whiteListing={whiteListing} allowedEntities={metaData.data.allowedEntities} allowedAll={metaData.data.allowedall} onChange={this.onChange}/>;
            case "metadata":
                return <MetaData entries={metaData.data.metaDataFields} configuration={configuration} onChange={this.onChange}/>;
            case "arp":
                return <ARP arp={metaData.data.arp} arpConfiguration={configuration.properties.arp} onChange={this.onChange}/>;
            case "manipulation":
                return <Manipulation content={metaData.data.manipulation} onChange={this.onChange}/>;
            case "consent_disabling":
                return <ConsentDisabling disableConsent={metaData.data.disableConsent} whiteListing={whiteListing} onChange={this.onChange}/>;
            default: throw new Error(`Unknown tab ${tab}`);
        }
    };

    render() {
        const {loaded, notFound, metaData, whiteListing, selectedTab} = this.state;
        const type = metaData.type;
        const tabs = type === "saml20_sp" ? tabsSp : tabsIdP;

        const renderNotFound = loaded && notFound;
        const renderContent = loaded && !notFound;

        return (
            <div className="detail-metadata">
                {renderNotFound && <section>{I18n.t("metadata.notFound")}</section>}
                {!notFound && <section className="tabs">
                    {tabs.map(tab => this.renderTab(tab))}
                </section>}
                {renderContent && this.renderCurrentTab(selectedTab, metaData, whiteListing)}
            </div>
        );
    }
}

Detail.propTypes = {
    history: PropTypes.object.isRequired,
    currentUser: PropTypes.object.isRequired,
    configuration: PropTypes.array.isRequired,
};

