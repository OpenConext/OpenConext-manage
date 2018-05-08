import React from "react";
import I18n from "i18n-js";
import PropTypes from "prop-types";
import {Link} from "react-router-dom";

import "./WhiteList.css";

export default class ConnectedIdps extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            sorted: "blocked",
            reverse: true
        };
    }

    componentDidMount() {
        window.scrollTo(0, 0);
    }

    sortByAttribute = (name, reverse = false) => (a, b) => {
        const aSafe = a[name] || "";
        const bSafe = b[name] || "";
        return aSafe.toString().localeCompare(bSafe.toString()) * (reverse ? -1 : 1);
    };

    sortTable = (enrichedAllowedEntries, name) => () => {
        const reverse = this.state.sorted === name ? !this.state.reverse : false;
        const sorted = [...enrichedAllowedEntries].sort(this.sortByAttribute(name, reverse));
        this.setState({enrichedAllowedEntries: sorted, sorted: name, reverse: reverse});
    };

    renderIdP = (entity, type) => {
        return <tr key={entity.data.entityid}>
            <td>
                <Link to={`/metadata/${type}/${entity.id}`} target="_blank">
                    {entity.data.metaDataFields["name:en"] || entity.data.metaDataFields["name:nl"] || entity.data.entityid}
                </Link>
            </td>
            <td>
                {entity.data.state}
            </td>
            <td>
                {entity.data.entityid}
            </td>
        </tr>
    };

    renderConnectedIdpTable = (entries) => {
        const {sorted, reverse} = this.state;
        const icon = name => {
            return name === sorted ? (reverse ? <i className="fa fa-arrow-up reverse"></i> :
                <i className="fa fa-arrow-down current"></i>)
                : <i className="fa fa-arrow-down"></i>;
        };
        const th = name =>
            <th key={name} className={name}
                onClick={this.sortTable(entries, name)}>{I18n.t(`whitelisting.allowedEntries.${name}`)}{icon(name)}</th>
        const names = ["name", "status", "entityid"];
        return <section className="allowed-entities">
            <table>
                <thead>
                <tr>
                    {names.map(th)}
                </tr>
                </thead>
                <tbody>
                {entries.map(entity => this.renderIdP(entity, "saml20_idp"))}
                </tbody>
            </table>

        </section>
    };

    render() {
        const {allowedAll, allowedEntities = [], whiteListing, name, entityId} = this.props;
        const providerType = "Identity Providers";
        const connectedEntities = whiteListing.filter(idp => idp.data.allowedall ||
            idp.data.allowedEntities.some(entity =>  entity.name === entityId))
            .filter(idp => allowedAll || allowedEntities.some(entity => entity.name === idp.data.entityid));


        return (
            <div className="metadata-whitelist">
                <div className="whitelist-info">
                    <h2>{I18n.t("whitelisting.title", {type: providerType})}</h2>
                    <p>{I18n.t("whitelisting.description", {type: providerType, name: name})}</p>
                </div>
                {connectedEntities.length > 0 && this.renderConnectedIdpTable(connectedEntities)}

            </div>
        );
    }
}

ConnectedIdps.propTypes = {
    whiteListing: PropTypes.array.isRequired,
    allowedEntities: PropTypes.array.isRequired,
    allowedAll: PropTypes.bool.isRequired,
    entityId: PropTypes.string.isRequired,
    name: PropTypes.string.isRequired
};

