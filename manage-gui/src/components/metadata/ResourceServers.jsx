import React from "react";
import I18n from "i18n-js";
import PropTypes from "prop-types";
import {Link} from "react-router-dom";
import SelectEntities from "./../SelectEntities";
import {copyToClip, isEmpty, stop} from "../../utils/Utils";

import "./ResourceServers.css";
import NotesTooltip from "../NotesTooltip";

export default class ResourceServers extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      sorted: "blocked",
      reverse: true,
      enrichedResourceServers: [],
      enrichedResourceServersFiltered: [],
      copiedToClipboardClassName: "",
      query: ""
    };
  }

  componentDidMount() {
    this.initialiseAllowedResourceServers(this.props.resourceServers);
  }

  componentWillReceiveProps(nextProps) {
    if (nextProps.resourceServers && this.props.resourceServers && nextProps.resourceServers.length !== this.props.resourceServers.length) {
      this.initialiseAllowedResourceServers(nextProps.resourceServers);
    }
  }

  initialiseAllowedResourceServers(resourceServers) {
    window.scrollTo(0, 0);
    const {allowedResourceServers, entityId} = this.props;

    const enrichedResourceServers = allowedResourceServers
      .map(entity => this.enrichResourceServer(entity, entityId, resourceServers))
      .filter(enriched => enriched !== null);
    this.setResourceServerState(enrichedResourceServers);
  }

  enrichResourceServer = (resourceServer, entityId, resourceServers) => {
    const moreInfo = resourceServers.find(
      entry => entry.data.entityid === resourceServer.name
    );

    if (moreInfo === undefined) {
      //this can happen as RP's are deleted
      return null;
    }

    return {
      status: I18n.t(`metadata.${moreInfo.data.state}`),
      entityid: resourceServer.name,
      name:
        moreInfo.data.metaDataFields["name:en"] ||
        moreInfo.data.metaDataFields["name:nl"] ||
        "",
      id: moreInfo["_id"],
      notes: moreInfo.data.notes
    };
  };

  copyToClipboard = () => {
    copyToClip("allowed-resource-servers-printable");
    this.setState({copiedToClipboardClassName: "copied"});
    setTimeout(() => this.setState({copiedToClipboardClassName: ""}), 5000);
  };


  setResourceServerState = newAllowedResourceServers => {
    const enrichedResourceServers = newAllowedResourceServers.sort(
      this.sortByAttribute(this.state.sorted, this.state.reverse)
    );
    this.setState({
      enrichedResourceServers: enrichedResourceServers,
      enrichedResourceServersFiltered: this.doSearch(
        this.state.query,
        enrichedResourceServers
      )
    });
  };

  doSearch = (query, enrichedResourceServers) => {
    if (isEmpty(query)) {
      return enrichedResourceServers;
    }
    const attributes = ["entityid", "name"];
    const lowerQuery = query.toLowerCase();
    return enrichedResourceServers.filter(entry =>
      attributes.some(
        attr => (entry[attr] || "").toLowerCase().indexOf(lowerQuery) > -1
      )
    );
  };

  search = e => {
    const query = e.target.value;
    const {enrichedResourceServers} = this.state;
    this.setState({
      query: query,
      enrichedResourceServersFiltered: this.doSearch(
        query,
        enrichedResourceServers
      )
    });
  };

  addResourceServer = resourceServerEntityId => {
    const {
      allowedResourceServers,
      entityId,
      resourceServers,
      onChange
    } = this.props;
    const newState = [...allowedResourceServers];
    newState.unshift({name: resourceServerEntityId});
    onChange("data.allowedResourceServers", newState);

    const newAllowedEntries = [...this.state.enrichedResourceServers];
    const newEntry = this.enrichResourceServer(
      {name: resourceServerEntityId},
      entityId,
      resourceServers
    );
    newAllowedEntries.unshift(newEntry);
    this.setResourceServerState(newAllowedEntries);
  };

  removeResourceServer = resourceServer => {
    const {allowedResourceServers, onChange} = this.props;
    const newState = [...allowedResourceServers].filter(
      entity => entity.name !== resourceServer.entityid
    );
    onChange("data.allowedResourceServers", newState);

    const newAllowedEntries = [...this.state.enrichedResourceServers].filter(
      entity => entity.entityid !== resourceServer.entityid
    );
    this.setResourceServerState(newAllowedEntries);
  };

  sortByAttribute = (name, reverse = false) => (a, b) => {
    const aSafe = a[name] || "";
    const bSafe = b[name] || "";
    return (
      aSafe.toString().localeCompare(bSafe.toString()) * (reverse ? -1 : 1)
    );
  };

  sortTable = (enrichedResourceServers, name) => () => {
    const reverse = this.state.sorted === name ? !this.state.reverse : false;
    const sorted = [...enrichedResourceServers].sort(
      this.sortByAttribute(name, reverse)
    );
    this.setState({
      enrichedResourceServersFiltered: sorted,
      sorted: name,
      reverse: reverse
    });
  };

  renderAllowedEntity = (entity, type, guest) => {
    return (
      <tr key={entity.entityid}>
        <td className="remove">
          {!guest && (
            <span>
              <a
                onClick={e => {
                  stop(e);
                  this.removeResourceServer(entity);
                }}
              >
                <i className="fa fa-trash-o"/>
              </a>
            </span>
          )}
        </td>
        <td>{entity.status}</td>
        <td>
          <Link to={`/metadata/${type}/${entity.id}`} target="_blank">
            {entity.name}
          </Link>
        </td>
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


  renderAllowedEntitiesTable = (
    enrichedResourceServers,
    type,
    guest
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
        onClick={this.sortTable(enrichedResourceServers, name)}
      >
        {I18n.t(`resource_servers.allowedEntries.${name}`)}
        {icon(name)}
      </th>
    );
    const names = ["status", "name", "entityid", "notes"];
    const entityType = "oidc10_rp";
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
          {enrichedResourceServers.map(entity =>
            this.renderAllowedEntity(
              entity,
              entityType,
              guest
            )
          )}
          </tbody>
        </table>
      </section>
    );
  };

  renderAllowedEntitiesTablePrintable = enrichedResourceServers => (
    <section
      id="allowed-resource-servers-printable"
      className="allowed-resource-servers-printable"
    >
      {enrichedResourceServers
        .map(
          entity => `${entity.name ? entity.name + "	" : ""}${entity.entityid}`
        )
        .join("\n")}
    </section>
  );

  render() {
    const {
      allowedResourceServers,
      resourceServers,
      name,
      type,
      entityId,
      guest,
    } = this.props;
    const providerType = "Resource Servers";
    const {
      enrichedResourceServersFiltered,
      copiedToClipboardClassName,
      query
    } = this.state;
    const placeholder = I18n.t("resource_servers.placeholder", {
      type: providerType
    });
    return (
      <div className="metadata-resource-servers">
        <div className="options">
          <span
            className={`button green ${copiedToClipboardClassName}`}
            onClick={this.copyToClipboard}>
            {I18n.t("clipboard.copy")}
            <i className="fa fa-clone"/>
          </span>
        </div>
        <div className="resource-servers-info">
          <h2>{I18n.t("resource_servers.title", {type: providerType})}</h2>
          <p>
            {I18n.t("resource_servers.description", {
              type: providerType,
              name: name
            })}
          </p>
        </div>
        {!guest && (
          <SelectEntities
            whiteListing={resourceServers.filter(rs => rs.data.entityid !== entityId)}
            allowedEntities={allowedResourceServers}
            onChange={this.addResourceServer}
            placeholder={placeholder}
          />
        )}
        <div className="search-input-container">
          <input
            className="search-input"
            placeholder={I18n.t("resource_servers.searchPlaceHolder")}
            type="text"
            onChange={this.search}
            value={query}
          />
          <i className="fa fa-search"/>
        </div>
        {enrichedResourceServersFiltered.length > 0 &&
        this.renderAllowedEntitiesTable(
          enrichedResourceServersFiltered,
          type,
          guest
        )}
        {this.renderAllowedEntitiesTablePrintable(
          enrichedResourceServersFiltered
        )}
      </div>
    );
  }
}

ResourceServers.defaultProps = {
  resourceServers: [],
  allowedResourceServers: [],
};

ResourceServers.propTypes = {
  allowedResourceServers: PropTypes.array,
  entityId: PropTypes.string.isRequired,
  name: PropTypes.string.isRequired,
  onChange: PropTypes.func.isRequired,
  resourceServers: PropTypes.array,
  guest: PropTypes.bool.isRequired
};
