import React from "react";
import PropTypes from "prop-types";
import I18n from "i18n-js";

import EntityId from "./EntityId";
import SelectState from "./SelectState";
import FormatInput from "./../FormatInput";

import "./Connection.scss";
import {isEmpty} from "../../utils/Utils";
import {Link} from "react-router-dom";
import {getNameForLanguage} from "../../utils/Language";


export default class Connection extends React.PureComponent {

    componentDidMount() {
        window.scrollTo(0, 0);
    }

    onError = key => value => this.props.onError(key, value);

    onChange = name => value => {
        if (value.target) {
            this.props.onChange(name, value.target.value);
        } else {
            this.props.onChange(name, value);
        }
    };

    render() {
        const {
            guest,
            originalEntityId,
            metaData: {type, revision, data, id},
            revisionNote,
            provisioningGroups
        } = this.props;
        const isRelyingParty = type === "oidc10_rp";
        const isSP = type === "saml20_sp";
        const isResourceServer = type === "oauth20_rs";
        const isProvisioning = type === "provisioning";
        const entityIdFormat = this.props.configuration.properties.entityid.format;
        const states = this.props.configuration.properties.state.enum;

        const logo = data.metaDataFields["logo:0:url"];
        const name = getNameForLanguage(data.metaDataFields);
        const fullName = I18n.t(`metadata.${type}_single`) + " - " + name;

        return (
            <div className="metadata-connection">
                <table className="data">
                    <tbody>
                    {logo && (
                        <tr className="first">
                            <td className="logo">
                                <img src={logo} alt=""/>
                            </td>
                            <td className="logo-name">{fullName}</td>
                        </tr>
                    )}
                    <tr>
                        <td className="key">{I18n.t("metadata.entityId")}</td>
                        <td className="value">
                            <EntityId
                                name="EntityId"
                                mayEdit={!guest}
                                value={data.entityid || ""}
                                onChange={this.onChange("data.entityid")}
                                onError={this.onError("entityid")}
                                {...{originalEntityId, type, entityIdFormat}}
                                hasError={this.props.errors["entityid"] || false}
                            />
                        </td>
                    </tr>
                    {isRelyingParty && (
                        <tr>
                            <td className="key">{I18n.t("metadata.discoveryUrl")}</td>
                            <td className="value">
                                <FormatInput
                                    name="discoveryUrl"
                                    input={data.discoveryurl || ""}
                                    format="url"
                                    onChange={this.onChange("data.discoveryurl")}
                                    onError={this.onError("discoveryUrl")}
                                    isError={this.props.errors["discoveryUrl"] || false}
                                    readOnly={guest}
                                    isRequired={false}
                                />
                            </td>
                        </tr>
                    )}
                    {(!isRelyingParty && !isResourceServer && !isProvisioning) && (
                        <tr>
                            <td className="key">{I18n.t("metadata.metaDataUrl")}</td>
                            <td className="value">
                                <FormatInput
                                    name="metaDataUrl"
                                    input={data.metadataurl || ""}
                                    format="url"
                                    onChange={this.onChange("data.metadataurl")}
                                    onError={this.onError("metaDataUrl")}
                                    isError={this.props.errors["metaDataUrl"] || false}
                                    readOnly={guest}
                                    isRequired={false}
                                />
                            </td>
                        </tr>
                    )}
                    <tr>
                        <td className="key">{I18n.t("metadata.state")}</td>
                        <td className="value">
                            <SelectState
                                onChange={this.onChange("data.state")}
                                state={data.state}
                                states={states}
                                disabled={guest}
                            />
                        </td>
                    </tr>
                    {((isSP || isRelyingParty) && id) &&
                    <tr>
                        <td className="key">{I18n.t("metadata.provisioning")}</td>
                        <td className="value provisioning">
                            {isEmpty(provisioningGroups) ? I18n.t("metadata.noProvisioning") :
                                provisioningGroups.map(prov =>
                                    <Link to={`/metadata/${prov.type}/${prov._id}`} target="_blank">
                                        <span className="provisioning">
                                            {getNameForLanguage(prov.data.metaDataFields)} - {I18n.t(`metadata.provisioningTypes.${prov.data.metaDataFields.provisioning_type}`)}
                                        </span>
                                    </Link>
                                )}
                        </td>
                    </tr>}
                    {id && revision && (
                        <tr>
                            <td className="key">{I18n.t("metadata.revision")}</td>
                            <td className="value">
                  <span>
                    {I18n.t("metadata.revisionInfo", {
                        number: revision.number,
                        updatedBy: revision.updatedBy,
                        created: new Date(revision.created).toGMTString()
                    })}
                  </span>
                            </td>
                        </tr>
                    )}
                    <tr>
                        <td className="key">{I18n.t("metadata.revisionnote")}</td>
                        <td className="value">
                            <span>{revisionNote}</span>
                        </td>
                    </tr>
                    <tr>
                        <td className="key">{I18n.t("metadata.notes")}</td>
                        <td className="value">
                <textarea
                    rows={3}
                    value={data.notes || ""}
                    onChange={this.onChange("data.notes")}
                    disabled={guest}
                />
                        </td>
                    </tr>
                    </tbody>
                </table>
            </div>
        );
    }
}

Connection.propTypes = {
    metaData: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
    onError: PropTypes.func.isRequired,
    errors: PropTypes.object.isRequired,
    guest: PropTypes.bool.isRequired,
    isNew: PropTypes.bool.isRequired,
    configuration: PropTypes.object.isRequired,
    originalEntityId: PropTypes.string.isRequired,
    revisionNote: PropTypes.string,
    provisioningGroups: PropTypes.array
};
