import React from "react";
import I18n from "i18n-js";
import PropTypes from "prop-types";

import ARP from "../components/metadata/ARP";
import Connection from "../components/metadata/Connection";
import ConsentDisabling from "../components/metadata/ConsentDisabling";
import Manipulation from "../components/metadata/Manipulation";
import MetaData from "../components/metadata/MetaData";
import WhiteList from "../components/metadata/WhiteList";
import Revisions from "../components/metadata/Revisions";

import {detail, revisions, whiteListing} from "../api";
import {stop} from "../utils/Utils";

import "./Detail.css";

const tabsSp = ["connection", "whitelist", "metadata", "arp", "manipulation", "revisions"];
const tabsIdP = ["connection", "whitelist", "consent_disabling", "metadata", "manipulation", "revisions"];

export default class Detail extends React.PureComponent {

    constructor(props) {
        super(props);
        this.state = {
            metaData: {},
            whiteListing: [],
            revisions: [],
            notFound: false,
            loaded: false,
            selectedTab: "connection",
            revisionNote: ""
        };
    }

    componentDidMount() {
        window.scrollTo(0, 0);
        const {type, id} = this.props.match.params;
        detail(type, id).then(metaData => {
            this.setState({metaData: metaData, loaded: true});
            whiteListing(metaData.type === "saml20_sp" ? "saml20_idp" : "saml20_sp").then(whiteListing => {
                this.setState({whiteListing: whiteListing});
                revisions(type, id).then(revisions => this.setState({revisions: revisions}))
            })
        }).catch(err => {
            debugger;
            if (err.response.status === 404) {
                this.setState({notFound: true, loaded: true});
            } else {
                throw err;
            }

        });
    }

    switchTab = tab => e => {
        stop(e);
        this.setState({selectedTab: tab});
    };

    onChange = (name, value) => {
        const currentState = this.state.metaData;
        const metaData = {
            ...currentState,
            data: {...currentState.data},
            arp: {...currentState.arp}//,
           // metaDataFields: {...currentState.metaDataFields}
        };
        if (Array.isArray(name) && Array.isArray(value)) {
            for (let i = 0; i < name.length; i++) {
                this.changeValueReference(metaData, name[i], value[i]);
            }
        } else {
            this.changeValueReference(metaData, name, value);
        }
        this.setState({metaData: metaData});

    };

    changeValueReference = (metaData, name, value) => {
        const parts = name.split(".");
        const last = parts.pop();

        let ref = metaData;
        parts.forEach(part => ref = ref[part]);
        ref[last] = value;
    };

    renderActions = (revisionNote) => {
        return <section className="actions">
            <section className="notes">
                <label htmlFor="revisionnote">{I18n.t("metadata.revisionnote")}</label>
                <input name="revisionnote" type="text" value={revisionNote}
                       onChange={e => this.setState({revisionNote: e.target.value})}/>
            </section>
            <section className="buttons">
                <a className="button grey">{I18n.t("metadata.cancel")}</a>
                <a className="button blue">{I18n.t("metadata.submit")}</a>
            </section>
        </section>
    };

    renderTab = tab =>
        <span key={tab} className={this.state.selectedTab === tab ? "active" : ""}
              onClick={this.switchTab(tab)}>{I18n.t(`metadata.tabs.${tab}`)}</span>;

    renderCurrentTab = (tab, metaData, whiteListing, revisions) => {
        const configuration = this.props.configuration.find(conf => conf.title === this.props.match.params.type);
        const name = metaData.data.metaDataFields["name:en"] || metaData.data.metaDataFields["name:nl"] || "this service";
        switch (tab) {
            case "connection" :
                return <Connection metaData={metaData} onChange={this.onChange}/>;
            case "whitelist" :
                return <WhiteList whiteListing={whiteListing} name={name}
                                  allowedEntities={metaData.data.allowedEntities}
                                  allowedAll={metaData.data.allowedall} type={metaData.type} onChange={this.onChange}
                                  entityId={metaData.data.entityid}/>;
            case "metadata":
                return <MetaData metaDataFields={metaData.data.metaDataFields} configuration={configuration}
                                 onChange={this.onChange} name={name}/>;
            case "arp":
                return <ARP arp={metaData.data.arp} arpConfiguration={configuration.properties.arp}
                            onChange={this.onChange}/>;
            case "manipulation":
                return <Manipulation content={metaData.data.manipulation} onChange={this.onChange}/>;
            case "consent_disabling":
                return <ConsentDisabling disableConsent={metaData.data.disableConsent} name={name}
                                         whiteListing={whiteListing} onChange={this.onChange}/>;
            case "revisions":
                return <Revisions revisions={revisions} metaData={metaData}/>;
            default:
                throw new Error(`Unknown tab ${tab}`);
        }
    };

    render() {
        const {loaded, notFound, metaData, whiteListing, revisions, selectedTab, revisionNote} = this.state;
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
                {renderContent && this.renderCurrentTab(selectedTab, metaData, whiteListing, revisions)}
                {renderContent && this.renderActions(revisionNote)}
            </div>
        );
    }
}

Detail.propTypes = {
    history: PropTypes.object.isRequired,
    currentUser: PropTypes.object.isRequired,
    configuration: PropTypes.array.isRequired,
};

