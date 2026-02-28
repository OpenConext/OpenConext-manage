import React from "react";
import PropTypes from "prop-types";
import I18n from "i18n-js";

import "./PolicyMaintenance.scss";

export default class PolicyMaintenance extends React.PureComponent {

    constructor(props) {
        super(props);
        this.state = {
            organisationid: props.metaData?.data?.organisationid
        }
    }

    componentDidMount() {
        window.scrollTo(0, 0);
    }

    render() {
        const {
            metaData: {revision, data, id},
            revisionNote,
            onRemove,
            onClone
        } = this.props;

        return (
            <div className="metadata-policy-maintenance">
                <table className="data">
                    <tbody>
                    <tr>
                        <td className="key">{I18n.t("metadata.entityId")}</td>
                        <td className="value">{data.entityid}</td>
                    </tr>
                    <tr>
                        <td className="key">{I18n.t("metadata.adminAction")}</td>
                        <td className="value">
                            <button className="button red delete-metadata" onClick={() => onRemove()}>
                                {I18n.t("metadata.remove")}
                            </button>
                            <button className="button green clone-metadata" onClick={() => onClone()}>
                                {I18n.t("metadata.clone")}
                            </button>
                        </td>
                    </tr>

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
                    </tbody>
                </table>
            </div>
        );
    }
}

PolicyMaintenance.propTypes = {
    metaData: PropTypes.object.isRequired,
    configuration: PropTypes.object.isRequired,
    revisionNote: PropTypes.string,
};
