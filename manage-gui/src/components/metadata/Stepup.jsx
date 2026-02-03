import React from "react";
import I18n from "i18n-js";
import PropTypes from "prop-types";
import {Link} from "react-router-dom";
import SelectEntities from "./../SelectEntities";
import {Select} from "./../../components";
import {isEmpty, stop} from "../../utils/Utils";
import ReactTooltip from "react-tooltip";

import "./Stepup.scss";
import {getNameForLanguage, getOrganisationForLanguage} from "../../utils/Language";

export default class Stepup extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            sorted: "name",
            reverse: false,
            sortedMfa: "name",
            reverseMfa: false,
            enrichedStepup: [],
            enrichedMfa: [],
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
        const {stepupEntities, mfaEntities} = this.props;

        const enrichedStepup = stepupEntities
            .map(entity => this.enrichSingleStepup(entity, whiteListing))
            .filter(enriched => enriched !== null);

        const enrichedMfa = mfaEntities
            .map(entity => this.enrichSingleStepup(entity, whiteListing))
            .filter(enriched => enriched !== null);

        this.setStepupState(enrichedStepup);
        this.setMfaState(enrichedMfa);
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
            name: getNameForLanguage(moreInfo.data.metaDataFields) || "",
            organization: getOrganisationForLanguage(moreInfo.data.metaDataFields) || "",
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

    setMfaState = newMfa => {
        this.setState({
            enrichedMfa: newMfa.sort(
                this.sortByAttribute(this.state.sortedMfa, this.state.reverseMfa)
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

    addMfa = entityid => {
        const entity = {
            name: entityid,
            level: this.props.mfaLevels[0]
        };
        const {mfaEntities, whiteListing} = this.props;
        const newState = [...mfaEntities].concat(entity);
        this.props.onChangeMfa("data.mfaEntities", newState);

        const newMfa = [...this.state.enrichedMfa].concat(
            this.enrichSingleStepup(entity, whiteListing)
        );

        this.setMfaState(newMfa);
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

    removeMfa = entry => {
        const {mfaEntities} = this.props;
        const newState = [...mfaEntities].filter(
            entity => entity.name !== entry.entityid
        );
        this.props.onChangeMfa("data.mfaEntities", newState);

        const newMfa = [...this.state.enrichedMfa].filter(
            entity => entity.entityid !== entry.entityid
        );
        this.setMfaState(newMfa);
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

    onChangeSelectMfaLevel = (entry, level) => {
        entry.level = level;
        const newState = [...this.props.mfaEntities];
        const pos = newState.map(e => e.name).indexOf(entry.entityid);
        newState[pos] = {
            name: entry.entityid,
            level: level
        };
        this.props.onChangeMfa("data.mfaEntities", newState);
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

    sortTableMfa = (enrichedMfa, name) => () => {
        const reverse = this.state.sortedMfa === name ? !this.state.reverseMfa : false;
        const sorted = [...enrichedMfa].sort(
            this.sortByAttribute(name, reverse)
        );
        this.setState({
            enrichedMfa: sorted,
            sortedMfa: name,
            reverseMfa: reverse
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
                <i className="fas fa-trash-o"/>
              </a>
            </span>
                    )}
                </td>
                <td>{entity.status}</td>
                <td>{entity.name}</td>
                <td>{entity.organization}</td>
                <td>
                    <Select
                        className="select-loa-level"
                        onChange={option =>
                            this.onChangeSelectLoaLevel(entity, option.value)
                        }
                        options={this.props.loaLevels.map(loa => ({
                            label: loa,
                            value: loa
                        }))}
                        value={entity.level}
                        isSearchable={false}
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

    renderMfa = (entity, guest) => {
        return (
            <tr key={entity.entityid}>
                <td className="remove">
                    {!guest && (
                        <span>
              <a
                  onClick={e => {
                      stop(e);
                      this.removeMfa(entity);
                  }}
              >
                <i className="fas fa-trash-o"/>
              </a>
            </span>
                    )}
                </td>
                <td>{entity.status}</td>
                <td>{entity.name}</td>
                <td>{entity.organization}</td>
                <td>
                    <Select
                        className="select-mfa-level"
                        onChange={option =>
                            this.onChangeSelectMfaLevel(entity, option.value)
                        }
                        options={this.props.mfaLevels.map(mfa => ({
                            label: mfa,
                            value: mfa
                        }))}
                        value={entity.level}
                        isSearchable={false}
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
                return <i className="fas fa-arrow-down"/>;
            }

            if (reverse) {
                return <i className="fas fa-arrow-up reverse"/>;
            }

            return <i className="fas fa-arrow-down current"/>;
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
            "organization",
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

    renderMfaTable = (enrichedMfa, guest) => {
        const {sortedMfa, reverseMfa} = this.state;
        const icon = name => {
            if (!(name === sortedMfa)) {
                return <i className="fas fa-arrow-down"/>;
            }

            if (reverseMfa) {
                return <i className="fas fa-arrow-up reverse"/>;
            }

            return <i className="fas fa-arrow-down current"/>;
        };
        const th = name => (
            <th
                key={name}
                className={name}
                onClick={this.sortTableMfa(enrichedMfa, name)}
            >
                {I18n.t(`stepup.entries.${name}`)}
                {icon(name)}
            </th>
        );
        const names = [
            "status",
            "name",
            "organization",
            "mfa_level",
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
                    {enrichedMfa.map(entity =>
                        this.renderMfa(entity, guest)
                    )}
                    </tbody>
                </table>
            </section>
        );
    };

    filterEntityOptions = (allowedAll, allowedEntities, whiteListing, isLoa) => {
        if (isLoa) {
            whiteListing = whiteListing
                .filter(entity => isEmpty(entity.data.metaDataFields["coin:stepup:requireloa"]));
        }

        if (allowedAll) {
            return whiteListing;
        }

        const allowedEntityNames = allowedEntities.map(ent => ent.name);
        return whiteListing.filter(entry =>
            allowedEntityNames.includes(entry.data.entityid)
        );
    }

    renderStepupEntities = (guest, name, allowedAll, allowedEntities, whiteListing, stepupEntities, placeholder, enrichedStepup) => {
        return <div>
            <div className="stepup-info">
                <h2>{I18n.t("stepup.title")}</h2>
                {!guest && (
                    <p>{I18n.t("stepup.description", {name: name})}
                        <i className="fas fa-info-circle"
                           data-for="step-up-tooltip" data-tip></i>
                        <ReactTooltip id="step-up-tooltip" type="info" class="tool-tip" effect="solid">
                            <span dangerouslySetInnerHTML={{__html: I18n.t("stepup.stepupTooltip")}}/>
                        </ReactTooltip>
                    </p>
                )}
            </div>
            {!guest && (
                <SelectEntities
                    whiteListing={this.filterEntityOptions(allowedAll, allowedEntities, whiteListing, true)}
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
    };

    renderMfaEntities = (guest, name, allowedAll, allowedEntities, whiteListing, mfaEntities, placeholder, enrichedMfa) => {
        return <div>
            <div className="mfa-info">
                <h2>{I18n.t("stepup.mfaTitle")}</h2>
                {!guest && (
                    <p>{I18n.t("stepup.mfaDescription", {name: name})}
                        <i className="fas fa-info-circle"
                           data-for="mfa-tooltip" data-tip></i>
                        <ReactTooltip id="mfa-tooltip" type="info" class="tool-tip" effect="solid">
                            <span dangerouslySetInnerHTML={{__html: I18n.t("stepup.mfaTooltip")}}/>
                        </ReactTooltip>
                    </p>
                )}
            </div>
            {!guest && (
                <SelectEntities
                    whiteListing={this.filterEntityOptions(allowedAll, allowedEntities, whiteListing, false)}
                    allowedEntities={mfaEntities}
                    onChange={this.addMfa}
                    placeholder={placeholder}
                />
            )}
            {enrichedMfa.length > 0 &&
            this.renderMfaTable(
                enrichedMfa,
                guest
            )}
        </div>;
    };

    render() {
        const {stepupEntities, name, guest, allowedAll, allowedEntities, whiteListing, mfaEntities} = this.props;
        const placeholder = I18n.t("stepup.placeholder");
        const mfaPlaceholder = I18n.t("stepup.mfaPlaceholder");
        const {enrichedStepup, enrichedMfa} = this.state;
        return (
            <div className="metadata-stepup">
                {this.renderStepupEntities(guest, name, allowedAll, allowedEntities, whiteListing, stepupEntities, placeholder, enrichedStepup)}
                {this.renderMfaEntities(guest, name, allowedAll, allowedEntities, whiteListing, mfaEntities, mfaPlaceholder, enrichedMfa)}
            </div>
        );
    }

}

Stepup.propTypes = {
    stepupEntities: PropTypes.array.isRequired,
    mfaEntities: PropTypes.array.isRequired,
    allowedEntities: PropTypes.array.isRequired,
    allowedAll: PropTypes.bool.isRequired,
    name: PropTypes.string.isRequired,
    whiteListing: PropTypes.array.isRequired,
    onChange: PropTypes.func.isRequired,
    onChangeMfa: PropTypes.func.isRequired,
    guest: PropTypes.bool.isRequired,
    loaLevels: PropTypes.array.isRequired,
    mfaLevels: PropTypes.array.isRequired
};
