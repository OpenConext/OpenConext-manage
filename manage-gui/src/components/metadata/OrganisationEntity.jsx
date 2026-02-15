import React from "react";
import "./OrganisationEntity.scss";
import {getAllEntities, update} from "../../api";
import PropTypes from "prop-types";
import I18n from "i18n-js";
import {Link} from "react-router-dom";
import {isEmpty, stop} from "../../utils/Utils";
import NotesTooltip from "../NotesTooltip";
import {Select} from "../index";
import {setFlash} from "../../utils/Flash";
import {getNameForLanguage} from "../../utils/Language";

export default class OrganisationEntity extends React.PureComponent {

    constructor(props) {
        super(props);

        this.state = {
            rawEntities: [],
            allEntities: [],
            availableEntities: [],
            currentEntities: [],
            entityToRemove: null,
            showConfirm: false,
        };
    }

    componentDidMount() {
        Promise.all([getAllEntities()]).then(result => {
            let rawEntities = result[0];
            this.setState({
                    rawEntities: rawEntities,
                    allEntities: rawEntities.map(entity => this.enrichEntity(entity))
            },
            () => {
                this.updateEntities();
            });
        });
    };

    updateEntities = () => {
        this.setState({
            availableEntities: this.state.allEntities.filter(e => isEmpty(e.organisationid)),
            currentEntities: this.state.allEntities.filter(e => e.organisationid === this.props.organisation.id)
        })
    }

    enrichEntity = (entity) => {
        return {
            status: I18n.t(`metadata.${entity.data.state}`),
            entityid: entity.data.entityid,
            organisationid: entity.data.organisationid,
            name:
                entity.data.metaDataFields["name:en"] ||
                entity.data.metaDataFields["name:nl"] ||
                "",
            id: entity.id,
            notes: entity.data.notes,
            type: entity.type
        };
    };

    addEntity = (entity) => {
        this.updateEntity(entity.value, true);
    }

    removeEntity = (entity) => {
        const message = I18n.t("organisation_entity.confirmRemoval", { entity: entity.name || entity.id });

        if (window.confirm(message)) {
            this.updateEntity(entity.id, false);
        }
    }

    updateEntity = (entityId, link) => {
        var metadata = this.state.rawEntities.find(e => e.id === entityId)

        if (link) {
            metadata.data.organisationid = this.props.organisation.id;
        } else {
            delete metadata.data.organisationid;
        }

        update(metadata).then(json => {
            if (json.exception || json.error) {
                setFlash(json.validations || json.message, "error");
                window.scrollTo(0, 0);
            } else {
                const name = json.data.name || getNameForLanguage(json.data.metaDataFields) || "this service";
                setFlash(
                    I18n.t("metadata.flash.updated", {
                        name: name,
                        revision: json.revision.number
                    })
                );
                const path = encodeURIComponent(`/metadata/organisation/${this.props.organisation.id}/organisation_entity`);
                this.props.navigate(`/refresh-route/${path}`, {replace: true});
            }
        });
    }

    renderTable = (entries) => {
        const th = name => (
            <th
                key={name}
                className={name}
            >
                {I18n.t(`organisation_entity.headers.${name}`)}
            </th>
        );
        const names = ["name", "type", "entityid", "status", "notes"];
        return (
            <section className="connected-entities">
                <table>
                    <thead>
                    <tr>
                        <th />
                        {names.map(th)}
                    </tr>
                    </thead>
                    <tbody>
                    {entries.map(entity => this.renderEntity(entity))}
                    </tbody>
                </table>
            </section>
        );
    };

    renderEntity = (entity) => {
        return (
            <tr key={entity.entityid}>
                <td>
                    <a
                        onClick={e => {
                            stop(e);
                            this.removeEntity(entity);
                        }}
                    >
                        <i className="fas fa-trash"/>
                    </a>
                </td>
                <td>
                    <Link to={`/metadata/${entity.type}/${entity.id}`} target="_blank">
                        {entity.name}
                    </Link>
                </td>
                <td>{typeLabels[entity.type] || ""}</td>
                <td>{entity.entityid}</td>
                <td>{entity.status}</td>
                <td className="info">
                    {isEmpty(entity.notes) ? (
                        <span>
            </span>
                    ) : (
                        <NotesTooltip identifier={entity.entityid} notes={entity.notes}/>
                    )}
                </td>
            </tr>
        );
    };

    render() {
        const {guest, organisation} = this.props;
        const {availableEntities, currentEntities} = this.state;

        const availableEntityOptions = (availableEntities || []).map(o => ({
            label: o.name + " ("  + typeLabels[o.type] + ")",
            value: o.id
        }))

        return (
            <div className="organisation-entity">
                <div className="description">
                    <h2>{I18n.t("organisation_entity.title")}</h2>
                    {!guest && (
                        <p>
                            {I18n.t("organisation_entity.description", {
                                organisation: organisation.data.name
                            })}
                        </p>
                    )}
                </div>
                <Select
                    options={availableEntityOptions}
                    onChange={this.addEntity}
                    placeholder={I18n.t("organisation_entity.searchPlaceholder")}
                />
                {currentEntities.length > 0 &&
                    this.renderTable(currentEntities)}
                {currentEntities.length === 0 &&
                    <span>
                        {I18n.t("organisation_entity.no_entities")}
                    </span>
                }

            </div>
        );
    }
}

const typeLabels = {
    saml20_sp: "Service Provider",
    oidc10_rp: "Relying Party"
};

OrganisationEntity.propTypes = {
    organisation: PropTypes.object.isRequired,
    navigate: PropTypes.func.isRequired,
    guest: PropTypes.bool
}
