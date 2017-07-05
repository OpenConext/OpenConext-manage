import React from "react";
import I18n from "i18n-js";
import PropTypes from "prop-types";
import {NavLink} from "react-router-dom";
import ConfirmationDialog from "../../components/ConfirmationDialog";
import CheckBox from "./../CheckBox";
import SelectEntities from "./../SelectEntities";
import {stop} from "../../utils/Utils";

import "./WhiteList.css";

export default class WhiteList extends React.PureComponent {

    constructor(props) {
        super(props);
        this.state = {
            confirmationDialogOpen: false,
            confirmationDialogQuestion: "",
            confirmationValue: false,
            sorted: "blocked",
            reverse: true,
            enrichedAllowedEntries: []
        };
    }

    componentDidMount() {
        window.scrollTo(0, 0);
        const {allowedEntities, entityId, whiteListing}= this.props;
        this.enrichAllowedEntries(allowedEntities, entityId, whiteListing);
    }

    componentWillReceiveProps(nextProps) {
        if (nextProps.allowedEntities.length !== this.state.enrichedAllowedEntries.length) {
            const {allowedEntities, entityId, whiteListing}= nextProps;
            this.enrichAllowedEntries(allowedEntities, entityId, whiteListing);
        }
    }

    enrichAllowedEntries = (allowedEntities, entityId, whiteListing) => {
        const enrichedAllowedEntries = allowedEntities.map(entity => {
            const moreInfo = whiteListing.find(entry => entry.data.entityid === entity.name);
            return {
                "blocked": !moreInfo.data.allowedall && moreInfo.data.allowedEntities.find(allowed => allowed.name === entityId),
                "status": I18n.t(`metadata.${moreInfo.data.state}`),
                "entityid": entity.name,
                "name": moreInfo.data.metaDataFields["name:en"] || moreInfo.data.metaDataFields["name:nl"] || "",
                "id": moreInfo[".id"]
            };
        });
        this.setState({enrichedAllowedEntries: enrichedAllowedEntries.sort(this.sortByAttribute(this.state.sorted, this.state.reverse))});
    };

    confirmationDialogAction = e => {
        stop(e);
        this.setState({confirmationDialogOpen: false});
        const allowedAll = this.state.confirmationValue;

        if (allowedAll) {
            this.props.onChange(["data.allowedall", "data.allowedEntities"], [allowedAll, []]);
        } else {
            this.props.onChange("data.allowedall", allowedAll);
        }
    };

    cancel = e => {
        stop(e);
        this.setState({confirmationDialogOpen: false});
    };

    addAllowedEntry = allowedEntry => {
        const {allowedAll, allowedEntities} = this.props;
        const newState = [...allowedEntities].concat({name: allowedEntry});

        if (allowedAll) {
            this.props.onChange(["data.allowedEntities", "data.allowedall"], [newState, false]);
        } else {
            this.props.onChange("data.allowedEntities", newState);
        }
    };

    removeAllowedEntry = allowedEntry => {
        const {allowedEntities} = this.props;
        const newState = [...allowedEntities].filter(entity => entity.name !== allowedEntry.entityid);
        this.props.onChange("data.allowedEntities", newState);
    };

    onChange = name => value => {
        if (value.target) {
            this.props.onChange(name, value.target.checked);
        } else {
            this.props.onChange(name, value);
        }
    };

    allowAllChanged = e => {
        const {allowedEntities, name, type} = this.props;
        const allowedEntitiesLength = allowedEntities.length;
        const typeS = type === "saml20_sp" ? "Identity Providers" : "Service Providers";
        if (e.target.checked) {
            if (allowedEntitiesLength > 0) {
                this.setState({
                    confirmationDialogOpen: true,
                    confirmationDialogQuestion: I18n.t("whitelisting.confirmationAllowAll", {name: name, type: typeS}),
                    confirmationValue: true
                });
            } else {
                this.props.onChange(["data.allowedEntities", "data.allowedall"], [[], true]);
            }
        } else if (allowedEntitiesLength > 0) {
            this.setState({
                confirmationDialogOpen: true,
                confirmationDialogQuestion: I18n.t("whitelisting.confirmationAllowNone", {name: name, type: typeS}),
                confirmationValue: false
            });
        } else {
            this.props.onChange("data.allowedall", false);
        }
    };

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

    renderAllowedEntity = (entity, type) => {
        return <tr key={entity.entityid}>
            <td>
                <CheckBox name={entity.entityid} value={true}
                          onChange={() => this.removeAllowedEntry(entity)}/>
            </td>
            <td className="blocked">
                {entity.blocked ? <i className="fa fa-window-close"></i> : <span></span>}
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

    renderAllowedEntitiesTable = (enrichedAllowedEntries, type) => {
        const {sorted, reverse} = this.state;
        const icon = name => {
            return name === sorted ? (reverse ? <i className="fa fa-arrow-up reverse"></i> :
                <i className="fa fa-arrow-down current"></i>)
                : <i className="fa fa-arrow-down"></i>;
        };
        const th = name =>
            <th key={name} className={name}
                onClick={this.sortTable(enrichedAllowedEntries, name)}>{I18n.t(`whitelisting.allowedEntries.${name}`)}{icon(name)}</th>
        const names = ["blocked", "status", "name", "entityid"];
        return <section className="allowed-entities">
            <table>
                <thead>
                <tr>
                    <th className="select"></th>
                    {names.map(th)}
                </tr>
                </thead>
                <tbody>
                {enrichedAllowedEntries.map(entity => this.renderAllowedEntity(entity, type === "saml20_sp" ? "saml20_idp" : "saml20_sp"))}
                </tbody>
            </table>

        </section>
    };

    render() {
        const {allowedAll, allowedEntities, whiteListing, name, type, entityId} = this.props;
        const providerType = type === "saml20_sp" ? "Identity Providers" : "Service Providers";

        const allowAllCheckBoxInfo = I18n.t("whitelisting.allowAllProviders", {
            type: providerType,
            name: name || "this service"
        });
        const placeholder = I18n.t("whitelisting.placeholder", {type: providerType});

        const {confirmationDialogOpen, confirmationDialogQuestion, enrichedAllowedEntries} = this.state;

        return (
            <div className="metadata-whitelist">
                <ConfirmationDialog isOpen={confirmationDialogOpen}
                                    cancel={this.cancel}
                                    confirm={this.confirmationDialogAction}
                                    leavePage={false}
                                    question={confirmationDialogQuestion}/>
                <CheckBox info={allowAllCheckBoxInfo} name="allow-all" value={allowedAll}
                          onChange={this.allowAllChanged}/>
                <div className="whitelist-info">
                    <h2>{I18n.t("whitelisting.title", {type: providerType})}</h2>
                    <p>{I18n.t("whitelisting.description", {type: providerType, name: name})}</p>
                </div>
                <SelectEntities whiteListing={whiteListing} allowedEntities={allowedEntities}
                                onChange={this.addAllowedEntry} placeholder={placeholder}/>
                {enrichedAllowedEntries.length > 0 && this.renderAllowedEntitiesTable(enrichedAllowedEntries, type)}

            </div>
        );
    }
}

WhiteList.propTypes = {
    whiteListing: PropTypes.array.isRequired,
    name: PropTypes.string.isRequired,
    type: PropTypes.string.isRequired,
    entityId: PropTypes.string.isRequired,
    allowedEntities: PropTypes.array.isRequired,
    allowedAll: PropTypes.bool.isRequired,
    onChange: PropTypes.func.isRequired
};

