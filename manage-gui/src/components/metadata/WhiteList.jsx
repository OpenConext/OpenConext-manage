import React from "react";
import I18n from "i18n-js";
import PropTypes from "prop-types";
import {Link} from "react-router-dom";
import ConfirmationDialog from "../ConfirmationDialog";
import CheckBox from "./../CheckBox";
import SelectEntities from "./../SelectEntities";
import {copyToClip, isEmpty, stop} from "../../utils/Utils";

import "./WhiteList.scss";
import NotesTooltip from "../NotesTooltip";
import {getNameForLanguage, getOrganisationForLanguage} from "../../utils/Language";

export default class WhiteList extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            confirmationDialogOpen: false,
            confirmationDialogQuestion: "",
            confirmationValue: false,
            sorted: "blocked",
            reverse: true,
            enrichedAllowedEntries: [],
            enrichedAllowedEntriesFiltered: [],
            copiedToClipboardClassName: "",
            query: ""
        };
    }

    componentDidMount() {
        this.initialiseAllowedEntities(this.props.whiteListing);
    }

    componentWillReceiveProps(nextProps) {
        if (nextProps.allowedAll) {
            this.setState({
                enrichedAllowedEntries: [],
                enrichedAllowedEntriesFiltered: []
            });
        } else if (
            nextProps.whiteListing &&
            this.props.whiteListing &&
            nextProps.whiteListing.length !== this.props.whiteListing.length
        ) {
            this.initialiseAllowedEntities(nextProps.whiteListing);
        }
    }

    whiteListEntityType = type => {
        return (type === "saml20_sp" || type === "oidc10_rp") ? "saml20_idp" : "saml20_sp";
    };

    initialiseAllowedEntities(whiteListing) {
        window.scrollTo(0, 0);
        const {allowedEntities, entityId} = this.props;

        const enrichedAllowedEntries = allowedEntities
            .map(entity => this.enrichAllowedEntry(entity, entityId, whiteListing))
            .filter(enriched => enriched !== null);
        this.setAllowedEntryState(enrichedAllowedEntries);
    }

    enrichAllowedEntry = (allowedEntry, entityId, whiteListing) => {
        const moreInfo = whiteListing.find(
            entry => entry.data.entityid === allowedEntry.name
        );

        if (moreInfo === undefined) {
            //this can happen as SP's are deleted
            return null;
        }
        return {
            status: I18n.t(`metadata.${moreInfo.data.state}`),
            entityid: allowedEntry.name,
            name: getNameForLanguage(moreInfo.data.metaDataFields) || "",
            organization: getOrganisationForLanguage(moreInfo.data.metaDataFields) || "",
            id: moreInfo["_id"],
            blocked:
                !moreInfo.data.allowedall &&
                !moreInfo.data.allowedEntities.some(
                    allowed => allowed.name === this.props.entityId
                ),
            notes: moreInfo.data.notes,
            type: moreInfo.type
        };
    };

    copyToClipboard = () => {
        copyToClip("allowed-entities-printable");
        this.setState({copiedToClipboardClassName: "copied"});
        setTimeout(() => this.setState({copiedToClipboardClassName: ""}), 5000);
    };

    confirmationDialogAction = e => {
        stop(e);
        this.setState({confirmationDialogOpen: false});
        const allowedAll = this.state.confirmationValue;

        if (allowedAll) {
            this.props.onChange(
                ["data.allowedall", "data.allowedEntities"],
                [allowedAll, []]
            );
        } else {
            this.props.onChange("data.allowedall", allowedAll);
        }
    };

    cancel = e => {
        stop(e);
        this.setState({confirmationDialogOpen: false});
    };

    setAllowedEntryState = newAllowedEntries => {
        const enrichedAllowedEntries = newAllowedEntries.sort(
            this.sortByAttribute(this.state.sorted, this.state.reverse)
        );
        this.setState({
            enrichedAllowedEntries: enrichedAllowedEntries,
            enrichedAllowedEntriesFiltered: this.doSearch(
                this.state.query,
                enrichedAllowedEntries
            )
        });
    };

    doSearch = (query, enrichedAllowedEntries) => {
        if (isEmpty(query)) {
            return enrichedAllowedEntries;
        }
        const attributes = ["entityid", "organization", "name"];
        const lowerQuery = query.toLowerCase();
        return enrichedAllowedEntries.filter(entry =>
            attributes.some(
                attr => (entry[attr] || "").toLowerCase().indexOf(lowerQuery) > -1
            )
        );
    };

    search = e => {
        const query = e.target.value;
        const {enrichedAllowedEntries} = this.state;
        this.setState({
            query: query,
            enrichedAllowedEntriesFiltered: this.doSearch(
                query,
                enrichedAllowedEntries
            )
        });
    };

    addAllowedEntry = allowedEntryEntityId => {
        const {
            allowedAll,
            allowedEntities,
            entityId,
            whiteListing,
            onChangeWhiteListedEntity
        } = this.props;
        const newState = [...allowedEntities];
        newState.unshift({name: allowedEntryEntityId});
        if (allowedAll) {
            this.props.onChange(
                ["data.allowedEntities", "data.allowedall"],
                [newState, false]
            );
        } else {
            this.props.onChange("data.allowedEntities", newState);
        }

        const newAllowedEntries = [...this.state.enrichedAllowedEntries];
        const newEntry = this.enrichAllowedEntry(
            {name: allowedEntryEntityId},
            entityId,
            whiteListing
        );
        newAllowedEntries.unshift(newEntry);
        this.setAllowedEntryState(newAllowedEntries);
        onChangeWhiteListedEntity(true, newEntry);
    };

    removeAllowedEntry = allowedEntry => {
        const {allowedEntities, onChangeWhiteListedEntity} = this.props;
        const newState = [...allowedEntities].filter(
            entity => entity.name !== allowedEntry.entityid
        );
        this.props.onChange("data.allowedEntities", newState);

        const newAllowedEntries = [...this.state.enrichedAllowedEntries].filter(
            entity => entity.entityid !== allowedEntry.entityid
        );
        this.setAllowedEntryState(newAllowedEntries);
        onChangeWhiteListedEntity(false, allowedEntry);
    };

    onChange = name => value => {
        if (value.target) {
            this.props.onChange(name, value.target.checked);
        } else {
            this.props.onChange(name, value);
        }
    };

    allowAllChanged = e => {
        const {allowedEntities, name, type, onChange} = this.props;
        const allowedEntitiesLength = allowedEntities.length;
        const typeS = this.whiteListEntityType(type) === "saml20_sp" ? "Service Providers" : "Identity Providers";
        if (e.target.checked) {
            if (allowedEntitiesLength > 0) {
                this.setState({
                    confirmationDialogOpen: true,
                    confirmationDialogQuestion: I18n.t(
                        "whitelisting.confirmationAllowAll",
                        {name: name, type: typeS}
                    ),
                    confirmationValue: true
                });
            } else {
                onChange(["data.allowedEntities", "data.allowedall"], [[], true]);
            }
        } else if (allowedEntitiesLength > 0) {
            this.setState({
                confirmationDialogOpen: true,
                confirmationDialogQuestion: I18n.t(
                    "whitelisting.confirmationAllowNone",
                    {name: name, type: typeS}
                ),
                confirmationValue: false
            });
        } else {
            onChange("data.allowedall", false);
        }
    };

    sortByAttribute = (name, reverse = false) => (a, b) => {
        const aSafe = a[name] || "";
        const bSafe = b[name] || "";
        return (
            aSafe.toString().localeCompare(bSafe.toString()) * (reverse ? -1 : 1)
        );
    };

    sortTable = (enrichedAllowedEntries, name) => () => {
        const reverse = this.state.sorted === name ? !this.state.reverse : false;
        const sorted = [...enrichedAllowedEntries].sort(
            this.sortByAttribute(name, reverse)
        );
        this.setState({
            enrichedAllowedEntriesFiltered: sorted,
            sorted: name,
            reverse: reverse
        });
    };

    renderAllowedEntity = (entity, guest, addedWhiteListedEntities) => {
        const className = addedWhiteListedEntities.some(
            e => e.entityid === entity.entityid
        )
            ? "added"
            : "";
        return (
            <tr key={entity.entityid} className={className}>
                <td className="remove">
                    {!guest && (
                        <span>
              <a
                  onClick={e => {
                      stop(e);
                      this.removeAllowedEntry(entity);
                  }}
              >
                <i className="fa fa-trash-o"/>
              </a>
            </span>
                    )}
                </td>
                <td className="blocked">
                    {entity.blocked ? <i className="fa fa-window-close"/> : <span/>}
                </td>
                <td>{entity.status}</td>
                <td>
                    <Link to={`/metadata/${entity.type}/${entity.id}`} target="_blank">
                        {entity.name}
                    </Link>
                </td>
                <td>{entity.organization}</td>
                <td>{entity.entityid}</td>
                <td className="info">
                    {isEmpty(entity.notes) ? (
                        <span/>
                    ) : (
                        <NotesTooltip identifier={entity.entityid} notes={entity.notes}/>
                    )}
                </td>
            </tr>
        );
    };

    renderRemovedEntities = (removedWhiteListedEntities) => {
        return (
            <section className="removed-entities">
                <p>{I18n.t("whitelisting.removedWhiteListedEntities")}</p>
                <table>
                    <tbody>
                    {removedWhiteListedEntities.map(entity => (
                        <tr key={entity.entityid}>
                            <td>
                                <Link
                                    to={`/metadata/${entity.type}/${entity.id}`}
                                    target="_blank"
                                >
                                    {entity.name}
                                </Link>
                            </td>
                            <td>{entity.status}</td>
                            <td>{entity.entityid}</td>
                            <td className="info">
                                {isEmpty(entity.notes) ? (
                                    <span/>
                                ) : (
                                    <NotesTooltip
                                        identifier={entity.entityid}
                                        notes={entity.notes}
                                    />
                                )}
                            </td>
                        </tr>
                    ))}
                    </tbody>
                </table>
            </section>
        );
    };

    renderAllowedEntitiesTable = (
        enrichedAllowedEntries,
        type,
        guest,
        addedWhiteListedEntities
    ) => {
        const {sorted, reverse} = this.state;
        const icon = name => {
            return name === sorted ? (
                reverse ? (
                    <i className="fa fa-arrow-up reverse"/>
                ) : (
                    <i className="fa fa-arrow-down current"/>
                )
            ) : (
                <i className="fa fa-arrow-down"/>
            );
        };
        const th = name => (
            <th
                key={name}
                className={name}
                onClick={this.sortTable(enrichedAllowedEntries, name)}
            >
                {I18n.t(`whitelisting.allowedEntries.${name}`)}
                {icon(name)}
            </th>
        );
        const names = ["blocked", "status", "name", "organization", "entityid", "notes"];
        return (
            <section className="allowed-entities">
                <table>
                    <thead>
                    <tr>
                        <th className="select"/>
                        {names.map(th)}
                    </tr>
                    </thead>
                    <tbody>
                    {enrichedAllowedEntries.map(entity =>
                        this.renderAllowedEntity(
                            entity,
                            guest,
                            addedWhiteListedEntities
                        )
                    )}
                    </tbody>
                </table>
            </section>
        );
    };

    renderAllowedEntitiesTablePrintable = enrichedAllowedEntries => (
        <section
            id="allowed-entities-printable"
            className="allowed-entities-printable"
        >
            {enrichedAllowedEntries
                .map(
                    entity => `${entity.name ? entity.name + "	" : ""}${entity.entityid}`
                )
                .join("\n")}
        </section>
    );

    render() {
        const {
            allowedAll,
            allowedEntities,
            whiteListing,
            name,
            type,
            guest,
            addedWhiteListedEntities,
            removedWhiteListedEntities
        } = this.props;
        const providerType = this.whiteListEntityType(type) === "saml20_sp" ? "Service Providers" : "Identity Providers";
        const {
            confirmationDialogOpen,
            confirmationDialogQuestion,
            enrichedAllowedEntriesFiltered,
            copiedToClipboardClassName,
            query
        } = this.state;
        const allowAllCheckBoxInfo = I18n.t("whitelisting.allowAllProviders", {
            type: providerType,
            name: name || "this service"
        });
        const placeholder = I18n.t("whitelisting.placeholder", {
            type: providerType
        });
        return (
            <div className="metadata-whitelist">
                <ConfirmationDialog
                    isOpen={confirmationDialogOpen}
                    cancel={this.cancel}
                    confirm={this.confirmationDialogAction}
                    leavePage={false}
                    question={confirmationDialogQuestion}
                />
                <section className="options">
                    <CheckBox
                        info={allowAllCheckBoxInfo}
                        name="allow-all"
                        value={allowedAll}
                        onChange={this.allowAllChanged}
                        readOnly={guest}
                    />
                    <span
                        className={`button green ${copiedToClipboardClassName}`}
                        onClick={this.copyToClipboard}
                    >
            {I18n.t("clipboard.copy")}
                        <i className="fa fa-clone"/>
          </span>
                </section>
                <div className="whitelist-info">
                    <h2>{I18n.t("whitelisting.title", {type: providerType})}</h2>
                    {!guest && (
                        <p>
                            {I18n.t("whitelisting.description", {
                                type: providerType,
                                name: name
                            })}
                        </p>
                    )}
                </div>
                {removedWhiteListedEntities.length > 0 &&
                this.renderRemovedEntities(removedWhiteListedEntities)}

                {!guest && (
                    <SelectEntities
                        whiteListing={whiteListing}
                        allowedEntities={allowedEntities}
                        onChange={this.addAllowedEntry}
                        placeholder={placeholder}
                    />
                )}
                <div className="search-input-container">
                    <input
                        className="search-input"
                        placeholder={I18n.t(`whitelisting.searchPlaceHolder_${type}`)}
                        type="text"
                        onChange={this.search}
                        value={query}
                    />
                    <i className="fa fa-search"/>
                </div>
                {enrichedAllowedEntriesFiltered.length > 0 &&
                this.renderAllowedEntitiesTable(
                    enrichedAllowedEntriesFiltered,
                    type,
                    guest,
                    addedWhiteListedEntities
                )}
                {this.renderAllowedEntitiesTablePrintable(
                    enrichedAllowedEntriesFiltered
                )}
            </div>
        );
    }
}

WhiteList.defaultProps = {
    allowedEntities: []
};

WhiteList.propTypes = {
    whiteListing: PropTypes.array.isRequired,
    name: PropTypes.string.isRequired,
    type: PropTypes.string.isRequired,
    entityId: PropTypes.string.isRequired,
    allowedEntities: PropTypes.array,
    allowedAll: PropTypes.bool.isRequired,
    onChange: PropTypes.func.isRequired,
    guest: PropTypes.bool.isRequired,
    addedWhiteListedEntities: PropTypes.array.isRequired,
    removedWhiteListedEntities: PropTypes.array.isRequired,
    onChangeWhiteListedEntity: PropTypes.func.isRequired
};
