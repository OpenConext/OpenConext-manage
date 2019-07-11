import React from "react";
import I18n from "i18n-js";
import PropTypes from "prop-types";
import {Link} from "react-router-dom";
import SelectEntities from "./../SelectEntities";
import {Select} from "./../../components";
import {isEmpty, stop} from "../../utils/Utils";

import "./Stepup.css";

export default class Stepup extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      sorted: "name",
      reverse: false,
      enrichedStepup: []
    };
  }

  componentDidMount() {
    this.initialiseStepup(this.props.whiteListing);
  }

  componentWillReceiveProps(nextProps) {
    if ((nextProps.whiteListing || []).length !== (this.props.whiteListing || []).length ||
      (nextProps.allowedEntities || []).length !== (this.props.allowedEntities || []).length) {
      this.initialiseStepup(nextProps.whiteListing);
    }
  }

  initialiseStepup(whiteListing) {
    window.scrollTo(0, 0);
    const {stepupEntities} = this.props;

    const enrichedStepup = stepupEntities
      .map(entity => this.enrichSingleStepup(entity, whiteListing))
      .filter(enriched => enriched !== null);

    this.setStepupState(enrichedStepup);
  }

  enrichSingleStepup = (stepupEntity, whiteListing) => {
    const moreInfo = whiteListing.find(
      entry => entry.data.entityid === stepupEntity.name
    );
    if (moreInfo === undefined) {
      //this can happen as SP's are deleted
      return null;
    }
    return {
      status: I18n.t(`metadata.${moreInfo.data.state}`),
      entityid: stepupEntity.name,
      name:
        moreInfo.data.metaDataFields["name:en"] ||
        moreInfo.data.metaDataFields["name:nl"] ||
        "",
      id: moreInfo["_id"],
      requireloa: moreInfo.data.metaDataFields["coin:stepup:requireloa"] || undefined,
      type: moreInfo.type,
      level: stepupEntity.level
    };
  };

  setStepupState = newStepup => {
    this.setState({
      enrichedStepup: newStepup.sort(
        this.sortByAttribute(this.state.sorted, this.state.reverse)
      )
    });
  };

  addStepup = entityid => {
    const entity = {
      name: entityid,
      level: this.props.loaLevels[0]
    };
    const {stepupEntities, whiteListing} = this.props;
    const newState = [...stepupEntities].concat(entity);
    this.props.onChange("data.stepupEntities", newState);

    const newStepup = [...this.state.enrichedStepup].concat(
      this.enrichSingleStepup(entity, whiteListing)
    );

    this.setStepupState(newStepup);
  };

  removeStepup = entry => {
    const {stepupEntities} = this.props;
    const newState = [...stepupEntities].filter(
      entity => entity.name !== entry.entityid
    );
    this.props.onChange("data.stepupEntities", newState);

    const newStepup = [...this.state.enrichedStepup].filter(
      entity => entity.entityid !== entry.entityid
    );
    this.setStepupState(newStepup);
  };

  onChangeSelectLoaLevel = (entry, level) => {
    entry.level = level;
    const newState = [...this.props.stepupEntities];
    const pos = newState.map(e => e.name).indexOf(entry.entityid);
    newState[pos] = {
      name: entry.entityid,
      level: level
    };
    this.props.onChange("data.stepupEntities", newState);
  };

  sortByAttribute = (name, reverse = false) => (a, b) => {
    const aSafe = a[name] || "";
    const bSafe = b[name] || "";
    return (
      aSafe.toString().localeCompare(bSafe.toString()) * (reverse ? -1 : 1)
    );
  };

  sortTable = (enrichedStepup, name) => () => {
    const reverse = this.state.sorted === name ? !this.state.reverse : false;
    const sorted = [...enrichedStepup].sort(
      this.sortByAttribute(name, reverse)
    );
    this.setState({
      enrichedStepup: sorted,
      sorted: name,
      reverse: reverse
    });
  };

  renderStepup = (entity, guest) => {
    return (
      <tr key={entity.entityid}>
        <td className="remove">
          {!guest && (
            <span>
              <a
                onClick={e => {
                  stop(e);
                  this.removeStepup(entity);
                }}
              >
                <i className="fa fa-trash-o"/>
              </a>
            </span>
          )}
        </td>
        <td>{entity.status}</td>
        <td>{entity.name}</td>
        <td>
          <Select
            name="select-loa-level"
            className="select-loa-level"
            onChange={option =>
              this.onChangeSelectLoaLevel(entity, option.value)
            }
            options={this.props.loaLevels.map(loa => ({
              label: loa,
              value: loa
            }))}
            value={entity.level}
            searchable={false}
          />
        </td>
        <td>
          <Link to={`/metadata/${entity.type}/${entity.id}`} target="_blank">
            {entity.entityid}
          </Link>
        </td>
      </tr>
    );
  };

  renderStepupTable = (enrichedStepup, guest) => {
    const {sorted, reverse} = this.state;
    const icon = name => {
      if (!(name === sorted)) {
        return <i className="fa fa-arrow-down"/>;
      }

      if (reverse) {
        return <i className="fa fa-arrow-up reverse"/>;
      }

      return <i className="fa fa-arrow-down current"/>;
    };
    const th = name => (
      <th
        key={name}
        className={name}
        onClick={this.sortTable(enrichedStepup, name)}
      >
        {I18n.t(`stepup.entries.${name}`)}
        {icon(name)}
      </th>
    );
    const names = [
      "status",
      "name",
      "loa_level",
      "entityid"
    ];
    return (
      <section className="stepup">
        <table>
          <thead>
          <tr>
            <th className="remove"/>
            {names.map(th)}
          </tr>
          </thead>
          <tbody>
          {enrichedStepup.map(entity =>
            this.renderStepup(entity, guest)
          )}
          </tbody>
        </table>
      </section>
    );
  };

  filterEntityOptions(allowedAll, allowedEntities, whiteListing) {
    whiteListing = whiteListing.filter(entity => isEmpty(entity.data.metaDataFields["coin:stepup:requireloa"]));

    if (allowedAll) {
      return whiteListing;
    }

    const allowedEntityNames = allowedEntities.map(ent => ent.name);
    return whiteListing.filter(entry =>
      allowedEntityNames.includes(entry.data.entityid)
    );
  }

  render() {
    const {stepupEntities, name, guest, allowedAll, allowedEntities, whiteListing} = this.props;
    const placeholder = I18n.t("stepup.placeholder");
    const {enrichedStepup} = this.state;
    return (
      <div className="metadata-stepup">
        <div className="stepup-info">
          <h2>{I18n.t("stepup.title")}</h2>
          {!guest && (
            <p>{I18n.t("stepup.description", {name: name})}</p>
          )}
        </div>
        {!guest && (
          <SelectEntities
            whiteListing={this.filterEntityOptions(allowedAll, allowedEntities, whiteListing)}
            allowedEntities={stepupEntities}
            onChange={this.addStepup}
            placeholder={placeholder}
          />
        )}
        {enrichedStepup.length > 0 &&
        this.renderStepupTable(
          enrichedStepup,
          guest
        )}
      </div>
    );
  }
}

Stepup.propTypes = {
  stepupEntities: PropTypes.array.isRequired,
  allowedEntities: PropTypes.array.isRequired,
  allowedAll: PropTypes.bool.isRequired,
  name: PropTypes.string.isRequired,
  whiteListing: PropTypes.array.isRequired,
  onChange: PropTypes.func.isRequired,
  guest: PropTypes.bool.isRequired,
  loaLevels: PropTypes.array.isRequired
};
