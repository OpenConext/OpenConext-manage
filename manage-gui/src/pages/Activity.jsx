import React from "react";
import I18n from "i18n-js";
import PropTypes from "prop-types";
import {recentActivity} from "../api";
import "./Activity.css";
import SelectMulti from "../components/form/SelectMulti";
import {copyToClip, isEmpty} from "../utils/Utils";
import {Link} from "react-router-dom";
import Select from "../components/Select";
import NotesTooltip from "../components/NotesTooltip";
import CheckBox from "../components/CheckBox";

const limitOptions = ["25", "50", "75", "100"].map(s => ({value: s, label:s}));

export default class Activity extends React.Component {

  constructor(props) {
    super(props);
    this.state = {
      activity: [],
      filteredActivity: [],
      types: ["saml20_idp", "saml20_sp", "oidc10_rp", "oauth20_rs"],
      reverse: false,
      allTypes: ["saml20_idp", "saml20_sp", "oidc10_rp", "oauth20_rs", "single_tenant_template"],
      limit: "25",
      query: "",
      loaded: false,
      sorted: "created",
      copiedToClipboardClassName: ""
    };
  }

  componentDidMount() {
    this.refreshActivities();
  }

  refreshActivities = () => {
    const {types, limit} = this.state;
    recentActivity(types, parseInt(limit, 10)).then(res => {
      const activities = res.map(a => ({
        id: a.id,
        type: a.type,
        entityId: a.data.entityid,
        state: a.data.state,
        terminated: a.revision.terminated,
        revisionNote: a.data.revisionnote,
        name: a.data.metaDataFields["name:en"],
        organization: a.data.metaDataFields["OrganizationName:en"] || a.data.metaDataFields["OrganizationName:nl"] || "",
        created: new Date(a.revision.created),
        updatedBy: a.revision.updatedBy,
      }));
      const sorted = activities.sort(this.sortByAttribute("created", false));
      this.setState({activity: sorted, filteredActivity: sorted, loaded: true});
    });
  };

  sortByAttribute = (name, reverse = false) => (a, b) => {
    const aSafe = a[name] || "";
    const bSafe = b[name] || "";
    if (name === "created") {
      return reverse ? aSafe - bSafe : bSafe - aSafe;
    }
    return aSafe.toString().localeCompare(bSafe.toString()) * (reverse ? -1 : 1);
  };

  sortTable = (filteredActivity, name, reversed) => () => {
    const reverse = reversed || (this.state.sorted === name ? !this.state.reverse : false);
    const sorted = [...filteredActivity].sort(this.sortByAttribute(name, reverse));
    this.setState({filteredActivity: sorted, sorted: name, reverse: reverse});
  };

  search = e => {
    const query = e.target.value ? e.target.value.toLowerCase() : "";
    const {sorted, reverse, activity} = this.state;
    const names = ["name", "organization", "entityId", "updatedBy"];
    const result = isEmpty(query) ? activity : activity.filter(a => names.some(name =>
      a[name].toLowerCase().indexOf(query) > -1));
    this.setState({query: query, filteredActivity: result.sort(this.sortByAttribute(sorted, reverse))});
  };

  changeTypes = types => {
    this.setState({types});
  }

  changeLimit = limit => {
    this.setState({limit: limit.value});
  }

  renderTable = (filteredActivity) => {
    const {sorted, reverse} = this.state;
    const icon = name => {
      return name === sorted ? (reverse ? <i className="fa fa-arrow-up reverse"></i> :
        <i className="fa fa-arrow-down current"></i>)
        : <i className="fa fa-arrow-down"></i>;
    };
    const th = name =>
      <th key={name} className={name}
          onClick={this.sortTable(filteredActivity, name)}>{I18n.t(`activity.${name}`)}{icon(name)}</th>
    const names = [
      "name", "organization", "entityId", "type", "created", "terminated", "revisionNote", "updatedBy"
    ];
    return <section className="activity-table">
      <table>
        <thead>
        <tr>
          {names.map(th)}
        </tr>
        </thead>
        <tbody>
        {filteredActivity.map(a => this.renderActivity(a))}
        </tbody>
      </table>

    </section>
  }

  renderActivity = (a) => {
    //"name", "entityId", "type", "created", "state", "revisionNote", "updatedBy"
    return <tr key={a.id}>
      <td>
        {isEmpty(a.terminated) && <Link to={`/metadata/${a.type}/${a.id}/revisions`} target="_blank">
          {a.name}
        </Link>}
        {!isEmpty(a.terminated) && <span>{a.name}</span>}
      </td>
      <td>
        {a.organization}
      </td>
      <td>
        {a.entityId}
      </td>
      <td>
        {a.type}
      </td>
      <td>
        {a.created.toGMTString()}
      </td>
      <td className="terminated"><CheckBox name="terminated" value={!isEmpty(a.terminated)} readOnly={true}/> </td>
      <td className="info">
        {isEmpty(a.revisionNote) ? <span></span> :
          <NotesTooltip identifier={a.entityId} notes={a.revisionNote}/>}
      </td>
      <td>
        {a.updatedBy}
      </td>
    </tr>
  };

  copyToClipboard = () => {
    copyToClip("activity-printable");
    this.setState({copiedToClipboardClassName: "copied"});
    setTimeout(() => this.setState({copiedToClipboardClassName: ""}), 5000);
  };

  renderActivityTablePrintable = filteredActivity =>
    <section id="activity-printable"
             className="activity-printable">{filteredActivity.map(a => `${a.name + '\t'}${a.entityId + '\t'}${a.updatedBy}`)
      .join("\n")}</section>;


  renderHeader = (filteredActivity, query, limit, types, allTypes, copiedToClipboardClassName) => {
    return <div className="header">
      <section className="explanation">
        <p>{I18n.t("activity.info")}</p>
      </section>
      <section className="options">
        <SelectMulti className="entity-types" enumValues={allTypes} onChange={this.changeTypes} value={types}/>
        <span className="span-option">{I18n.t("activity.limit")}</span>
        <Select className="limit-value" options={limitOptions} onChange={this.changeLimit} value={limit}/>
        <span className="button green" onClick={this.refreshActivities}>
          {I18n.t("activity.refresh")}<i className="fa fa-refresh"/>
        </span>
      </section>
      <section className="search">
        <div className="search-input-container">
          <input className="search-input"
                 placeholder={I18n.t("activity.searchPlaceHolder")}
                 type="text"
                 onChange={this.search}
                 value={query}/>
          <i className="fa fa-search"/>
        </div>
        <span className={`button green ${copiedToClipboardClassName}`} onClick={this.copyToClipboard}>
                            {I18n.t("clipboard.copy")}<i className="fa fa-clone"/>
                        </span>
      </section>
    </div>
  };

  render() {
    const {
      filteredActivity, types, allTypes, limit, query, loaded, copiedToClipboardClassName, reverse
    } = this.state;
    if (!loaded) {
      return null;
    }
    const hasResults = !isEmpty(filteredActivity);
    return (
      <div className="activity">
        {this.renderHeader(filteredActivity, query, limit, types, allTypes, copiedToClipboardClassName)}
        {hasResults && this.renderTable(filteredActivity, reverse)}
        {!hasResults && <p className="no-results">{I18n.t("activity.noResults")}</p>}
        {this.renderActivityTablePrintable(filteredActivity)}
      </div>
    );
  }

}

Activity.propTypes = {
  history: PropTypes.object.isRequired
};
