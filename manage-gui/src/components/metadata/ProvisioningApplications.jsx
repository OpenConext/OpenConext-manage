import React from "react";
import I18n from "i18n-js";
import PropTypes from "prop-types";
import {Link} from "react-router-dom";
import SelectEntities from "./../SelectEntities";
import {copyToClip, isEmpty, stop} from "../../utils/Utils";

import "./ProvisioningApplications.scss";
import NotesTooltip from "../NotesTooltip";

export default class ProvisioningApplications extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            sorted: "blocked",
            reverse: true,
            enrichedApplications: [],
            enrichedApplicationsFiltered: [],
            copiedToClipboardClassName: "",
            query: ""
        };
    }

    componentDidMount() {
        this.initialiseAllowedApplications(this.props.applications);
    }

    componentWillReceiveProps(nextProps) {
        if (nextProps.applications && this.props.applications && nextProps.applications.length !== this.props.applications.length) {
            this.initialiseAllowedApplications(nextProps.applications);
        }
    }

    initialiseAllowedApplications(applications) {
        window.scrollTo(0, 0);
        const {allowedApplications} = this.props;
        const enrichedApplications = allowedApplications
            .map(entity => this.enrichApplication(entity, applications))
            .filter(enriched => enriched !== null);
        this.setApplicationState(enrichedApplications);
    }

    enrichApplication = (application, applications) => {
        const moreInfo = applications.find(
            entry => entry._id === application._id || entry._id === application.id
        );
        if (moreInfo === undefined) {
            //this can happen as RP's are deleted
            return null;
        }
        return {
            status: I18n.t(`metadata.${moreInfo.data.state}`),
            entityid: moreInfo.data.entityid,
            name:
                moreInfo.data.metaDataFields["name:en"] ||
                moreInfo.data.metaDataFields["name:nl"] ||
                "",
            organization:
                moreInfo.data.metaDataFields["OrganizationName:en"] ||
                moreInfo.data.metaDataFields["OrganizationName:nl"] ||
                "",
            id: moreInfo["_id"],
            _id: moreInfo["_id"],
            type: moreInfo.type,
            notes: moreInfo.data.notes
        };
    };

    copyToClipboard = () => {
        copyToClip("allowed-applications-printable");
        this.setState({copiedToClipboardClassName: "copied"});
        setTimeout(() => this.setState({copiedToClipboardClassName: ""}), 5000);
    };


    setApplicationState = newAllowedApplications => {
        const enrichedApplications = newAllowedApplications.sort(
            this.sortByAttribute(this.state.sorted, this.state.reverse)
        );
        this.setState({
            enrichedApplications: enrichedApplications,
            enrichedApplicationsFiltered: this.doSearch(
                this.state.query,
                enrichedApplications
            )
        });
    };

    doSearch = (query, enrichedApplications) => {
        if (isEmpty(query)) {
            return enrichedApplications;
        }
        const attributes = ["entityid", "organization", "name"];
        const lowerQuery = query.toLowerCase();
        return enrichedApplications.filter(entry =>
            attributes.some(
                attr => (entry[attr] || "").toLowerCase().indexOf(lowerQuery) > -1
            )
        );
    };

    search = e => {
        const query = e.target.value;
        const {enrichedApplications} = this.state;
        this.setState({
            query: query,
            enrichedApplicationsFiltered: this.doSearch(
                query,
                enrichedApplications
            )
        });
    };

    addApplication = applicationEntityId => {
        const {
            allowedApplications,
            entityId,
            applications,
            onChange
        } = this.props;
        const application = applications.find(app => app.data.entityid === applicationEntityId);
        const newState = [...allowedApplications];
        newState.unshift({id: application._id, type: application.type});
        onChange("data.applications", newState);

        const newAllowedEntries = [...this.state.enrichedApplications];
        const newEntry = this.enrichApplication(
            {_id: application._id},
            applications
        );
        newAllowedEntries.unshift(newEntry);
        this.setApplicationState(newAllowedEntries);
    };

    removeApplication = application => {
        const {allowedApplications, onChange} = this.props;
        const newState = [...allowedApplications].filter(
            entity => entity.id !== application.id
        );
        onChange("data.applications", newState);

        const newAllowedEntries = [...this.state.enrichedApplications].filter(
            entity => entity.id !== application.id
        );
        this.setApplicationState(newAllowedEntries);
    };

    sortByAttribute = (name, reverse = false) => (a, b) => {
        const aSafe = a[name] || "";
        const bSafe = b[name] || "";
        return (
            aSafe.toString().localeCompare(bSafe.toString()) * (reverse ? -1 : 1)
        );
    };

    sortTable = (enrichedApplications, name) => () => {
        const reverse = this.state.sorted === name ? !this.state.reverse : false;
        const sorted = [...enrichedApplications].sort(
            this.sortByAttribute(name, reverse)
        );
        this.setState({
            enrichedApplicationsFiltered: sorted,
            sorted: name,
            reverse: reverse
        });
    };

    renderAllowedEntity = entity => {
        return (
            <tr key={entity.entityid}>
                <td className="remove">

                        <span>
              <a
                  onClick={e => {
                      stop(e);
                      this.removeApplication(entity);
                  }}
              >
                <i className="fa fa-trash-o"/>
              </a>
            </span>
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


    renderAllowedEntitiesTable = enrichedApplications => {
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
                onClick={this.sortTable(enrichedApplications, name)}
            >
                {I18n.t(`applications.allowedEntries.${name}`)}
                {icon(name)}
            </th>
        );
        const names = ["status", "name", "organization", "entityid", "notes"];
        const entityType = "oauth20_rs";
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
                    {enrichedApplications.map(entity =>
                        this.renderAllowedEntity(
                            entity,
                            entityType
                        )
                    )}
                    </tbody>
                </table>
            </section>
        );
    };

    renderAllowedEntitiesTablePrintable = enrichedApplications => (
        <section
            id="allowed-applications-printable"
            className="allowed-applications-printable"
        >
            {enrichedApplications
                .map(
                    entity => `${entity.name ? entity.name + "	" : ""}${entity.entityid}`
                )
                .join("\n")}
        </section>
    );

    render() {
        const {allowedApplications, applications, name} = this.props;
        const providerType = "Applications";
        const {
            enrichedApplicationsFiltered,
            copiedToClipboardClassName,
            query
        } = this.state;
        const availableApplications = applications.filter(app => !allowedApplications.some(allowed => allowed.id === app._id)  )
        const placeholder = I18n.t("applications.placeholder", {
            type: providerType
        });
        return (
            <div className="metadata-applications">
                <div className="options">
          <span
              className={`button green ${copiedToClipboardClassName}`}
              onClick={this.copyToClipboard}>
            {I18n.t("clipboard.copy")}
              <i className="fa fa-clone"/>
          </span>
                </div>
                <div className="applications-info">
                    <h2>{I18n.t("applications.title", {type: providerType})}</h2>
                    <p>
                        {I18n.t("applications.description", {
                            type: providerType,
                            name: name
                        })}
                    </p>
                </div>
                <SelectEntities
                    whiteListing={availableApplications}
                    allowedEntities={allowedApplications}
                    onChange={this.addApplication}
                    placeholder={placeholder}
                />
                <div className="search-input-container">
                    <input
                        className="search-input"
                        placeholder={I18n.t("applications.searchPlaceHolder")}
                        type="text"
                        onChange={this.search}
                        value={query}
                    />
                    <i className="fa fa-search"/>
                </div>
                {enrichedApplicationsFiltered.length > 0 &&
                this.renderAllowedEntitiesTable(enrichedApplicationsFiltered)}
                {this.renderAllowedEntitiesTablePrintable(
                    enrichedApplicationsFiltered
                )}
            </div>
        );
    }
}

ProvisioningApplications.defaultProps = {
    applications: [],
    allowedApplications: [],
};

ProvisioningApplications.propTypes = {
    allowedApplications: PropTypes.array,
    entityId: PropTypes.string.isRequired,
    name: PropTypes.string.isRequired,
    onChange: PropTypes.func.isRequired,
    applications: PropTypes.array,
    guest: PropTypes.bool.isRequired
};
