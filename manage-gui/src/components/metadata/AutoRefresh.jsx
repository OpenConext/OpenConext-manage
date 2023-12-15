import React from "react";

import "./AutoRefresh.scss";
import PropTypes from "prop-types";
import CheckBox from "../CheckBox";
import I18n from "i18n-js";
import {copyToClip, isEmpty} from "../../utils/Utils";

export default class AutoRefresh extends React.PureComponent {

    constructor(props) {
        super(props);
        const {metadataAutoRefresh, defaultAutoRefresh} = props;
        this.state = {
            addInput: false,
            keyForNewInput: undefined,
            value: "",
            newArpAttributeAddedKey: undefined,
            copiedToClipboardClassName: ""
        };
        // Since auto refresh is completely optional and can be null, initialize a default for the auto refresh configuration
        if (!metadataAutoRefresh) {
            this.props.onChange("data.autoRefresh", { ...defaultAutoRefresh });
        }
    }

    onChange = (name, value) => {
        const cleansedName = `data.autoRefresh.fields.${name}`;
        this.props.onChange(cleansedName, value, true);
    };

    autoRefreshEnabled = e => {
        const enableAutoRefresh = e.target.checked;
        if (!enableAutoRefresh) {
            this.props.onChange(["data.autoRefresh.enabled", "data.autoRefresh.fields"], [enableAutoRefresh, {}]);
        } else {
            this.props.onChange("data.autoRefresh.enabled", enableAutoRefresh);
        }
    };

    autoRefreshAllowAll = e => {
        const autoRefreshAllowAll = e.target.checked;
        this.props.onChange(["data.autoRefresh.allowAll", "data.autoRefresh.fields"], [autoRefreshAllowAll, {}]);
    };

    copyToClipboard = () => {
        copyToClip("auto-refresh-fields-printable");
        this.setState({copiedToClipboardClassName: "copied"});
        setTimeout(() => this.setState({copiedToClipboardClassName: ""}), 5000);
    };

    sortFieldKeys = (configField, attributes) => (aKey, bKey) => {
        const aEnabled = !isEmpty(attributes[aKey]);
        const bEnabled = !isEmpty(attributes[bKey]);

        if (aEnabled && !bEnabled) {
            return -1;
        }
        if (!aEnabled && bEnabled) {
            return 1;
        }
        return aKey.localeCompare(bKey);
    };

    renderRefreshFieldsTable = (autoRefresh, autoRefreshConfiguration, guest, metaDataUrl) => {
        const autoRefreshFields = autoRefreshConfiguration.properties.fields.properties;
        const fieldKeys = Object.keys(autoRefreshFields);
        fieldKeys.sort(this.sortFieldKeys(autoRefreshFields, autoRefresh.fields));

        const headers = ["name", "enabled"];

        return <table className="auto-refresh-fields">
            <thead>
            <tr>
                {headers.map(td => <th className={td} key={td}>{I18n.t(`auto_refresh.headers.${td}`)}</th>)}
            </tr>
            </thead>
            <tbody>
            {fieldKeys.map((fieldKey) =>
                <tr key={fieldKey}>
                    <td className="field">{fieldKey}</td>
                    <td className="value">
                        <CheckBox name={fieldKey}
                                  value={autoRefresh.fields[fieldKey] ?? false}
                                  readOnly={guest || autoRefresh.allowAll || !autoRefresh.enabled || !metaDataUrl}
                                  onChange={e => this.onChange(fieldKey, e.target.checked)}/>
                    </td>
                </tr>
            )}
            </tbody>
        </table>
    };

    renderNoMetaDataUrlWarning = () => {
        return <section id="auto-refresh-no-metadata-url" className="metadata-url-warning">
            <h2>{I18n.t(`auto_refresh.no_metadata_url.header`)}</h2>
            <p>{I18n.t(`auto_refresh.no_metadata_url.body`)}</p>
        </section>
    };

    renderArpAttributesTablePrintable = (fields) =>
        <section id="auto-refresh-fields-printable"
                 className="auto-refresh-fields-printable">
            {
                Object.keys(fields).join("\n")
            }
        </section>;

    render() {
        const {metadataAutoRefresh, defaultAutoRefresh, autoRefreshConfiguration, guest, metaDataUrl} = this.props;
        const {copiedToClipboardClassName} = this.state;

        const autoRefresh = isEmpty(metadataAutoRefresh) ? defaultAutoRefresh : metadataAutoRefresh;
        return (
            <div className="metadata-auto-refresh">
                {!metaDataUrl && this.renderNoMetaDataUrlWarning()}
                <section className="options">
                    <CheckBox name="auto-refresh-enabled" value={autoRefresh.enabled}
                              onChange={this.autoRefreshEnabled} readOnly={guest || !metaDataUrl}
                              info={I18n.t("auto_refresh.enabled")}/>
                    <CheckBox name="auto-refresh-allow-all" value={autoRefresh.allowAll}
                              onChange={this.autoRefreshAllowAll} readOnly={guest || !metaDataUrl}
                              info={I18n.t("auto_refresh.allow_all")}/>
                    <span className={`button green ${copiedToClipboardClassName}`} onClick={this.copyToClipboard}>
                        {I18n.t("clipboard.copy")}<i className="fa fa-clone"></i>
                    </span>
                </section>
                <section className="fields">
                    <h2>{I18n.t("auto_refresh.fields")}</h2>
                    {this.renderRefreshFieldsTable(autoRefresh, autoRefreshConfiguration, guest, metaDataUrl)}
                    {this.renderArpAttributesTablePrintable(autoRefresh.fields)}
                </section>
            </div>
        );
    }
}

AutoRefresh.propTypes = {
    autoRefreshConfiguration: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
    guest: PropTypes.bool.isRequired
};

AutoRefresh.defaultProps = {
    defaultAutoRefresh: {enabled: false, allowAll: false, fields: {}}
};