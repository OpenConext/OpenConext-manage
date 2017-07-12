import React from "react";
import I18n from "i18n-js";
import PropTypes from "prop-types";
import {NavLink} from "react-router-dom";
import CheckBox from "./../CheckBox";
import SelectEntities from "./../SelectEntities";

import "./ConsentDisabling.css";

export default class ConsentDisabling extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            sorted: "name",
            reverse: false,
            enrichedDisableConsent: []
        };
    }

    componentDidMount() {
        window.scrollTo(0, 0);
        const {disableConsent, entityid, whiteListing} = this.props;
        this.enrichDisableConsent(disableConsent, entityid, whiteListing);
    }

    enrichDisableConsent = (disableConsent, entityid, whiteListing) => {
        const enrichedDisableConsent = disableConsent
            .map(entity => this.enrichSingleDisableConsent(entity, entityid, whiteListing))
            .filter(enriched => enriched !== null);
        this.setDisableConsentState(enrichedDisableConsent);
    };

    enrichSingleDisableConsent = (disableConsent, whiteListing) => {
        const moreInfo = whiteListing.find(entry => entry.data.entityid === disableConsent.name);
        if (moreInfo === undefined) {
            //this can happen as SP's are deleted
            return null;
        }
        return {
            "status": I18n.t(`metadata.${moreInfo.data.state}`),
            "entityid": disableConsent.name,
            "name": moreInfo.data.metaDataFields["name:en"] || moreInfo.data.metaDataFields["name:nl"] || "",
            "id": moreInfo["_id"]
        };
    };

    setDisableConsentState = newDisableConsent =>
        this.setState({enrichedDisableConsent: newDisableConsent.sort(this.sortByAttribute(this.state.sorted, this.state.reverse))});

    addDisableConsent = entityid => {
        const {disableConsent, whiteListing} = this.props;
        const newState = [...disableConsent].concat({name: entityid});
        this.props.onChange("data.disableConsent", newState);

        const newDisableConsent = [...this.state.enrichedDisableConsent]
            .concat(this.enrichSingleDisableConsent({name: entityid}, whiteListing));
        this.setDisableConsentState(newDisableConsent);
    };

    removeDisableConsent = entry => {
        const {disableConsent} = this.props;
        const newState = [...disableConsent].filter(entity => entity.name !== entry.entityid);
        this.props.onChange("data.disableConsent", newState);

        const newDisableConsent = [...this.state.enrichedDisableConsent]
            .filter(entity => entity.name !== entry.name);
        this.setDisableConsentState(newDisableConsent);

    };

    onChange = name => value => {
        if (value.target) {
            this.props.onChange(name, value.target.checked);
        } else {
            this.props.onChange(name, value);
        }
    };

    sortByAttribute = (name, reverse = false) => (a, b) => {
        const aSafe = a[name] || "";
        const bSafe = b[name] || "";
        return aSafe.toString().localeCompare(bSafe.toString()) * (reverse ? -1 : 1);
    };

    sortTable = (enrichedDisableConsent, name) => () => {
        const reverse = this.state.sorted === name ? !this.state.reverse : false;
        const sorted = [...enrichedDisableConsent].sort(this.sortByAttribute(name, reverse));
        this.setState({enrichedDisableConsent: sorted, sorted: name, reverse: reverse});
    };

    renderDisableConsent = (entity, type, guest) => {
        return <tr key={entity.entityid}>
            <td>
                <CheckBox name={entity.entityid} value={true}
                          onChange={() => this.removeDisableConsent(entity)} readOnly={guest}/>
            </td>
            <td>
                {entity.status}
            </td>
            <td>
                {entity.name}
            </td>
            <td>
                <NavLink to={`/metadata/${type}/${entity.id}`}>{entity.entityid}</NavLink>
            </td>
        </tr>
    };

    renderDisableConsentTable = (enrichedDisableConsent, type, guest) => {
        const {sorted, reverse} = this.state;
        const icon = name => {
            return name === sorted ? (reverse ? <i className="fa fa-arrow-up reverse"></i> :
                <i className="fa fa-arrow-down current"></i>)
                : <i className="fa fa-arrow-down"></i>;
        };
        const th = name =>
            <th key={name} className={name}
                onClick={this.sortTable(enrichedDisableConsent, name)}>{I18n.t(`consentDisabling.entries.${name}`)}{icon(name)}</th>
        const names = ["status", "name", "entityid"];
        return <section className="consent-disabling">
            <table>
                <thead>
                <tr>
                    <th className="select"></th>
                    {names.map(th)}
                </tr>
                </thead>
                <tbody>
                {enrichedDisableConsent.map(entity => this.renderDisableConsent(entity, type === "saml20_sp" ? "saml20_idp" : "saml20_sp", guest))}
                </tbody>
            </table>

        </section>
    };

    render() {
        const {disableConsent, whiteListing, name, guest} = this.props;
        const placeholder = I18n.t("consentDisabling.placeholder");
        const {enrichedDisableConsent} = this.state;

        return (
            <div className="metadata-consent-disabling">
                <div className="consent-disabling-info">
                    <h2>{I18n.t("consentDisabling.title")}</h2>
                    {!guest && <p>{I18n.t("consentDisabling.description", {name: name})}</p>}
                </div>
                {!guest && <SelectEntities whiteListing={whiteListing} allowedEntities={disableConsent}
                                           onChange={this.addDisableConsent} placeholder={placeholder}/>}
                {enrichedDisableConsent.length > 0 && this.renderDisableConsentTable(enrichedDisableConsent, "saml20_sp", guest)}

            </div>
        );
    }
}

ConsentDisabling.propTypes = {
    whiteListing: PropTypes.array.isRequired,
    disableConsent: PropTypes.array.isRequired,
    name: PropTypes.string.isRequired,
    onChange: PropTypes.func.isRequired,
    guest: PropTypes.bool.isRequired
};

