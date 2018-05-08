import React from "react";
import I18n from "i18n-js";
import PropTypes from "prop-types";
import {Link} from "react-router-dom";
import ConfirmationDialog from "../../components/ConfirmationDialog";
import CheckBox from "./../CheckBox";
import SelectEntities from "./../SelectEntities";
import {stop} from "../../utils/Utils";

import "./WhiteList.css";

export default class WhiteList extends React.Component {

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
        const {allowedEntities = [], entityId, whiteListing} = this.props;
        this.enrichAllowedEntries(allowedEntities, entityId, whiteListing);
    }

    componentWillReceiveProps(nextProps) {
        if (nextProps.allowedAll) {
            this.setState({enrichedAllowedEntries: []})
        }
    }

    enrichAllowedEntries = (allowedEntities = [], entityId, whiteListing) => {
        const enrichedAllowedEntries = allowedEntities
            .map(entity => this.enrichAllowedEntry(entity, entityId, whiteListing))
            .filter(enriched => enriched !== null);
        this.setAllowedEntryState(enrichedAllowedEntries);
    };

    enrichAllowedEntry = (allowedEntry, entityId, whiteListing) => {
        const moreInfo = whiteListing.find(entry => entry.data.entityid === allowedEntry.name);
        if (moreInfo === undefined) {
            //this can happen as SP's are deleted
            return null;
        }
        return {
            "blocked": !moreInfo.data.allowedall && !(moreInfo.data.allowedEntities || [])
                .some(allowed => allowed.name === entityId),
            "status": I18n.t(`metadata.${moreInfo.data.state}`),
            "entityid": allowedEntry.name,
            "name": moreInfo.data.metaDataFields["name:en"] || moreInfo.data.metaDataFields["name:nl"] || "",
            "id": moreInfo["_id"]
        };
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

    setAllowedEntryState = newAllowedEntries =>
        this.setState({enrichedAllowedEntries: newAllowedEntries.sort(this.sortByAttribute(this.state.sorted, this.state.reverse))});

    addAllowedEntry = allowedEntryEntityId => {
        const {allowedAll, allowedEntities = [], entityId, whiteListing} = this.props;
        const newState = [...allowedEntities].concat({name: allowedEntryEntityId});
        if (allowedAll) {
            this.props.onChange(["data.allowedEntities", "data.allowedall"], [newState, false]);
        } else {
            this.props.onChange("data.allowedEntities", newState);
        }

        const newAllowedEntries = [...this.state.enrichedAllowedEntries]
            .concat(this.enrichAllowedEntry({name: allowedEntryEntityId}, entityId, whiteListing));
        this.setAllowedEntryState(newAllowedEntries);
    };

    removeAllowedEntry = allowedEntry => {
        const {allowedEntities = []} = this.props;
        const newState = [...allowedEntities].filter(entity => entity.name !== allowedEntry.entityid);
        this.props.onChange("data.allowedEntities", newState);

        const newAllowedEntries = [...this.state.enrichedAllowedEntries]
            .filter(entity => entity.name !== allowedEntry.name);
        this.setAllowedEntryState(newAllowedEntries);
    };

    onChange = name => value => {
        if (value.target) {
            this.props.onChange(name, value.target.checked);
        } else {
            this.props.onChange(name, value);
        }
    };

    allowAllChanged = e => {
        const {allowedEntities = [], name, type} = this.props;
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

    renderAllowedEntity = (entity, type, guest) => {
        return <tr key={entity.entityid}>
            <td className="remove">
                {!guest && <span><a onClick={e => {
                    stop(e);
                    this.removeAllowedEntry(entity)
                }}><i className="fa fa-trash-o"></i></a></span>    }
            </td>
            <td className="blocked">
                {entity.blocked ? <i className="fa fa-window-close"></i> : <span></span>}
            </td>
            <td>
                {entity.status}
            </td>
            <td>
                <Link to={`/metadata/${type}/${entity.id}`} target="_blank">{entity.name}</Link>
            </td>
            <td>
                {entity.entityid}
            </td>
        </tr>
    };

    renderAllowedEntitiesTable = (enrichedAllowedEntries, type, guest) => {
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
                {enrichedAllowedEntries.map(entity => this.renderAllowedEntity(entity, type === "saml20_sp" ? "saml20_idp" : "saml20_sp", guest))}
                </tbody>
            </table>

        </section>
    };

    render() {
        const {allowedAll, allowedEntities = [], whiteListing, name, type, guest} = this.props;
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
                          onChange={this.allowAllChanged} readOnly={guest}/>
                <div className="whitelist-info">
                    <h2>{I18n.t("whitelisting.title", {type: providerType})}</h2>
                    {!guest && <p>{I18n.t("whitelisting.description", {type: providerType, name: name})}</p>}
                </div>
                {!guest && <SelectEntities whiteListing={whiteListing} allowedEntities={allowedEntities}
                                           onChange={this.addAllowedEntry} placeholder={placeholder}/>}
                {enrichedAllowedEntries.length > 0 && this.renderAllowedEntitiesTable(enrichedAllowedEntries, type, guest)}

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
    onChange: PropTypes.func.isRequired,
    guest: PropTypes.bool.isRequired
};

