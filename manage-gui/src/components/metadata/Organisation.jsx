import React from "react";

import "./Organisation.scss";
import PropTypes from "prop-types";
import I18n from "i18n-js";
import String from "../form/String";
import {isEmpty} from "../../utils/Utils";
import {validateUniqueField} from "../../api";

export default class Organisation extends React.PureComponent {

    componentDidMount() {
        window.scrollTo(0, 0);
        this.validateName(this.props.organisation.data.name || "");
    }

    onChange = name => value => {
        if (name === this.props.nameField) {
            this.validate(name, value);
        }

        if (value.target) {
            this.props.onChange(name, value.target.value);
        } else {
            this.props.onChange(name, value);
        }
    };

    validate = (name, value) => {
        let valid = true;
        if (name === this.props.nameField) {
            valid = valid && !isEmpty(value);
        }
        // this.hasError("name", !valid);
        this.hasError(name, !valid);
    };

    hasError = (key, value = true) => {
        if (this.props.errors) {
            this.props.onError(key, value || undefined);
        }
    };

    async validateName(value) {
        let valid = false;

        if (!isEmpty(value)) {
            if (value === this.props.originalName) {
                valid = true;
            } else {
                try {
                    await validateUniqueField("organisation", "name", value)
                    valid = true;
                } catch {
                    valid = false;
                }
            }
        }

        this.hasError(this.props.nameField, !valid);
    };

    render() {
        const {
            organisation: {id, revision, data},
            guest,
            nameField,
            errors
        } = this.props;
        const hasErrorName = !isEmpty(data.name) && errors ? errors[nameField] : false;
        return (
            <div className="organisation">
                <table className="data">
                    <tbody>
                    <tr>
                        <td className="key">{I18n.t("metadata.name")}</td>
                        <td className="value">
                            <div className="format-input">
                                <String
                                    name="name"
                                    disabled={guest}
                                    value={data.name || ""}
                                    onChange={this.onChange("data.name")}
                                    onBlur={e => this.validateName(e.target.value)}
                                />
                                {hasErrorName && (
                                    <span>
                                        <i className="fa fa-warning"/>
                                            {I18n.t("organisation.name.notUnique")}
                                    </span>
                                )}
                                {isEmpty(data.name) && (
                                    <p className="error">
                                        {I18n.t("metadata.required", {name: nameField })}
                                    </p>
                                )}
                            </div>
                        </td>
                    </tr>
                    <tr>
                        <td className="key">KVK Number</td>
                        <td className="value">
                            <div className="format-input">
                                <String
                                    name="kvkNumber"
                                    disabled={guest}
                                    value={data.kvkNumber || ""}
                                    onChange={this.onChange("data.kvkNumber")}
                                />
                            </div>
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
                        created: new Date(revision.created).toUTCString()
                    })}
                  </span>
                            </td>
                        </tr>
                    )}
                    <tr>
                        <td className="key">{I18n.t("metadata.revisionnote")}</td>
                        <td className="value">
                            <span>{data.revisionnote}</span>
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

Organisation.defaultProps = {
    nameField: 'data.name'
};

Organisation.propTypes = {
    organisation: PropTypes.object.isRequired,
    originalName: PropTypes.string.isRequired,
    onChange: PropTypes.func.isRequired,
    onError: PropTypes.func.isRequired,
    errors: PropTypes.object.isRequired,
    guest: PropTypes.bool
};
