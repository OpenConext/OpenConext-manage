import React from "react";
import PropTypes from "prop-types";
import I18n from "i18n-js";

import InlineEditable from "./InlineEditable";
import SelectState from "./SelectState";
import FormatInput from "./../FormatInput";

import "./Connection.css";

export default class Connection extends React.PureComponent {

    constructor(props) {
        super(props);
        this.state = {};
    }

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
        const {type, revision, data} = this.props.metaData;
        const logo = data.metaDataFields["logo:0:url"];
        const name = data.metaDataFields["name:en"] || data.metaDataFields["name:nl"] || "";
        const fullName = I18n.t(`metadata.${type}_single`) + " - " + name;
        const {guest} = this.props;
        return (
            <div className="metadata-connection">

                <table className="data">
                    <tbody>
                    {logo &&
                    <tr className="first">
                        <td className="logo"><img src={logo} alt=""/></td>
                        <td className="logo-name">{fullName}</td>
                    </tr>}
                    <tr>
                        <td className="key">{I18n.t("metadata.entityId")}</td>
                        <td className="value">
                            <InlineEditable name="EntityId" mayEdit={!guest}
                                            value={data.entityid || ""}
                                            onChange={this.onChange("data.entityId")}
                                            required={true}
                                            onError={this.onError("entityId")}/>
                        </td>
                    </tr>
                    <tr>
                        <td className="key">{I18n.t("metadata.metaDataUrl")}</td>
                        <td className="value">
                            <FormatInput name="metaDataUrl"
                                         input={data.metadataurl || ""} format="uri"
                                         onChange={this.onChange("data.metadataurl")}
                                         onError={this.onError("metaDataUrl")}
                                         isError={this.props.errors["metaDataUrl"] || false}
                                         readOnly={guest}/>
                        </td>
                    </tr>
                    <tr>
                        <td className="key">{I18n.t("metadata.state")}</td>
                        <td className="value">
                            <SelectState onChange={this.onChange("data.state")} state={data.state} disabled={guest}/>
                        </td>
                    </tr>
                    <tr>
                        <td className="key">{I18n.t("metadata.notes")}</td>
                        <td className="value">
                            <textarea rows={3}
                                      value={data.notes || ""}
                                      onChange={this.onChange("data.notes")}
                                      disabled={guest}/>
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
    guest: PropTypes.bool.isRequired
};

