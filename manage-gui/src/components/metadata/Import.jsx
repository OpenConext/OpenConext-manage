import React from "react";
import I18n from "i18n-js";
import PropTypes from "prop-types";
import CodeMirror from "react-codemirror";
import "codemirror/mode/javascript/javascript";
import "codemirror/mode/xml/xml";
import {isEmpty, stop} from "../../utils/Utils";
import {
    importMetaDataJSON,
    importMetaDataJsonUrl,
    importMetaDataXML,
    importMetaDataXmlUrl,
    validation
} from "../../api";
import { Select, CheckBox } from "./../../components"


import "codemirror/lib/codemirror.css";
import "./Import.css";


export default class Import extends React.Component {

    constructor(props) {
        super(props);
        const {newEntity, metaData} = this.props;

        const sharedTabs = ["import_json_url", "import_json", "results"]
        const tabs = metaData.type === 'oidc10_rp' ? sharedTabs : ["import_xml_url", "import_xml", ...sharedTabs]

        this.state = {
            url: newEntity ? "" : metaData.data.metadataurl,
            jsonUrl: "",
            entityId: newEntity ? "" : metaData.data.entityid,
            invalidUrl: false,
            invalidJsonUrl: false,
            xml: "",
            invalidXml: false,
            json: "",
            entityType: props.entityType,
            invalidJson: false,
            results: undefined,
            resultsMap: undefined,
            errorsUrl: undefined,
            errorsJsonUrl: false,
            errorsJson: undefined,
            errorsXml: undefined,
            tabs: tabs,
            selectedTab: "import_xml_url",
            applyChangesFor: {}
        };
    }

    componentDidMount() {
        window.scrollTo(0, 0);
        if (this.importUrlField) {
            this.importUrlField.focus();
        }
    }

    sortArpArrayValues = (a, b) => a.source === b.source ? a.value.localeCompare(b.value) : a.source.localeCompare(b.source);

    arpChanged = (current, imported) => {
        if (!current.enabled && !imported.enabled) {
            return false;
        }
        if (current.enabled !== imported.enabled) {
            return true;
        }
        const currentAttributes = Object.keys(current.attributes);
        const importedAttributes = Object.keys(imported.attributes);
        const removed = currentAttributes.some(name => imported.attributes[name] === undefined);
        if (removed) {
            return true;
        }
        const added = importedAttributes.some(name => current.attributes[name] === undefined);
        if (added) {
            return true;
        }
        const detailsChanged = currentAttributes.some(name => {
            const imp = imported.attributes[name];
            const curr = current.attributes[name];
            if (curr.length !== imp.length) {
                return true;
            }
            imp.sort(this.sortArpArrayValues);
            curr.sort(this.sortArpArrayValues);
            return JSON.stringify(imp) !== JSON.stringify(curr);
        });
        return detailsChanged;
    };

    allowedEntitiesOrDisableConsentChanged = (current, imported) => {
        const currentNames = current.map(entity => entity.name);
        const importedNames = imported.map(entity => entity.name);
        const added = currentNames.some(name => importedNames.indexOf(name) === -1);
        if (added) {
            return true;
        }
        return importedNames.some(name => currentNames.indexOf(name) === -1);
    };

    /**
     * This is not generic on purpose. It is possible, but it makes the code very complex and
     * we need to make assumptions about the data structure anyway for the different tabs.
     */
    resultsToMap = results => {
        const currentMetaData = this.props.metaData.data;
        results.connection = {};
        const keys = Object.keys(results);
        if (!this.props.newEntity) {
            const notifyByRemoval = ["certData", "certData2", "certData3"];
            notifyByRemoval.forEach(name => {
                if (currentMetaData.metaDataFields[name] && (!results.metaDataFields || !results.metaDataFields[name])) {
                    if (!results.metaDataFields) {
                        results.metaDataFields = {};
                    }
                    results.metaDataFields[name] = {
                        value: undefined,
                        selected: true,
                        current: currentMetaData.metaDataFields[name]
                    };
                }
            });
        }
        keys.forEach(key => {
            const value = results[key];
            if (key === "allowedEntities" || key === "disableConsent") {
                const changed = this.allowedEntitiesOrDisableConsentChanged(currentMetaData[key], value);
                if (!changed) {
                    delete results[key];
                }
            } else if (key === "arp") {
                const changed = this.arpChanged(currentMetaData[key], value);
                if (!changed) {
                    delete results[key];
                }
            } else if (key === "metaDataFields") {
                const metaDataFields = Object.keys(value);
                metaDataFields.forEach(field => {
                    const current = currentMetaData[key][field];
                    if (current === value[field]) {
                        delete value[field];
                    } else if (!value[field].current) {
                        value[field] = {
                            value: value[field],
                            selected: true,
                            current: current
                        };
                    }
                });
                if (Object.keys(value).length === 0) {
                    delete results[key];
                }
            } else if (key !== "connection") {
                const current = currentMetaData[key];
                if (current === results[key]) {
                    delete results[key];
                } else {
                    results.connection[key] = {value: results[key], selected: true, current: current};
                    delete results[key];
                }
            }
        });
        if (Object.keys(results.connection).length === 0) {
            delete results.connection;
        }
    };

    changeMetaPropertySelected = (group, name) => e => {
        const newResults = {...this.state.results};
        newResults[group][name].selected = e.target.checked;
        if (e.target.checked) {
            const newApplyChangesFor = {...this.state.applyChangesFor};
            newApplyChangesFor[group] = true;
            this.setState({results: newResults, applyChangesFor: newApplyChangesFor});
        } else {
            this.setState({results: newResults});
        }
    };

    doImport = (promise, errorsName) => {
        const newState = {...this.state};

        promise.then(json => {
            window.scrollTo(0, 0);
            if (json.errors) {
                newState[errorsName] = json.errors;
                newState.entityType = this.props.entityType;
                newState.results = undefined;
                this.setState({...newState});
            } else {
                this.resultsToMap(json);
                this.setState({
                    results: json,
                    errorsUrl: undefined,
                    errorsJson: undefined,
                    errorsXml: undefined,
                    selectedTab: "results",
                    applyChangesFor: {
                        "allowedEntities": !isEmpty(json.allowedEntities),
                        "disableConsent": !isEmpty(json.disableConsent),
                        "arp": !isEmpty(json.arp),
                        "metaDataFields": !isEmpty(json.metaDataFields),
                        "connection": !isEmpty(json.connection)
                    }
                });
            }
        });
    };

    importUrl = e => {
        stop(e);
        const {url, entityId, entityType} = this.state;
        validation("url", url).then(result => {
            this.setState({
                invalidUrl: !result
            });
            if (result) {
                this.doImport(importMetaDataXmlUrl(url, entityType, entityId), "errorsUrl");
            }
        });
    };

    importJsonUrl = e => {
        stop(e);
        const {jsonUrl, entityId, entityType} = this.state;
        validation("url", jsonUrl).then(result => {
            this.setState({
                invalidJsonUrl: !result
            });
            if (result) {
                this.doImport(importMetaDataJsonUrl(jsonUrl, entityType, entityId), "errorsJsonUrl");
            }
        });
    };

    importJson = e => {
        stop(e);
        const {json, entityType} = this.state;
        validation("json", json).then(result => {
            this.setState({
                invalidJson: !result
            });
            if (result) {
                this.doImport(importMetaDataJSON(this.props.metaData.type || entityType, json), "errorsJson");
            }
        });
    };

    importXml = e => {
        stop(e);
        const {xml, entityType} = this.state;
        validation("xml", xml).then(result => {
            this.setState({
                invalidXml: !result
            });
            if (result) {
                this.doImport(importMetaDataXML(xml, entityType), "errorsJson");
            }
        });

    };

    changeApplyChangesFor = (name, cascade = false) => e => {
        const newApplyChangesFor = {...this.state.applyChangesFor};
        newApplyChangesFor[name] = e.target.checked;
        this.setState({applyChangesFor: newApplyChangesFor});
        if (cascade) {
            const newResults = {...this.state.results};
            Object.keys(newResults[name]).forEach(key => newResults[name][key].selected = e.target.checked);
            this.setState({results: newResults});
        }
    };

    renderKeyValueTable = (keyValues, headers, name, newEntity) => {
        const applyChangesFor = this.state.applyChangesFor[name];
        const prefix = newEntity ? "new_" : "";
        return (
            <table>
                <thead>
                <tr>
                    <th className="title" colSpan={4}>
                        <CheckBox name={name}
                                  value={applyChangesFor}
                                  onChange={this.changeApplyChangesFor(name, true)}
                                  info={I18n.t(`import.${prefix}${name}`)}/></th>
                </tr>
                <tr>
                    {headers.map(header => <th key={header}
                                               className={header}>{I18n.t(`import.headers.${header}`)}</th>)}
                </tr>
                </thead>
                <tbody>
                {Object.keys(keyValues).map(key => {
                    const prop = keyValues[key];
                    return (
                        <tr key={key}>
                            <td className="isCheckBox">{<CheckBox name={key} value={prop.selected}
                                                                  onChange={this.changeMetaPropertySelected(name, key)}/>}</td>
                            <td>{key}</td>
                            <td>{prop.current ? prop.current.toString() : ""}</td>
                            <td>{prop.value ? prop.value.toString() : ""}</td>
                        </tr>)
                })}
                </tbody>
            </table>
        );
    };

    renderAllowedEntitiesDisableContentTable = (entities, currentEntities, name, entryName, newEntity) => {
        const prefix = newEntity ? "new_" : "";
        return (
            <table>
                <thead>
                <tr>
                    <th className="title" colSpan={2}><CheckBox name={name} value={this.state.applyChangesFor[name]}
                                                                onChange={this.changeApplyChangesFor(name)}
                                                                info={I18n.t(`import.${prefix}${name}`)}/></th>
                </tr>
                <tr>
                    <th className="left">{I18n.t("import.currentEntries", {name: entryName})}</th>
                    <th className="right">{I18n.t("import.newEntries", {name: entryName})}</th>
                </tr>
                </thead>
                <tbody>
                <tr>
                    <td>
                        <ul className="entities">{currentEntities.map(entity => <li
                            key={entity.name}>{entity.name}</li>)}</ul>
                    </td>
                    <td>
                        <ul className="entities">{entities.map(entity => <li key={entity.name}>{entity.name}</li>)}</ul>
                    </td>
                </tr>
                </tbody>
            </table>
        );
    };

    nameOfArpKey = key => key.substring(key.lastIndexOf(":") + 1);

    renderArpAttribute = (key, arpValues, counterPart, currentValue) => {
        const emptyRows = isEmpty(counterPart) ? [] : [...Array(counterPart.length - 1).keys()];
        if (isEmpty(arpValues)) {
            return <tbody key={key}>
            <tr>
                <td className="arpKey" colSpan="2"><i className={`fa fa-trash-o ${currentValue ? "old" : ""}`}></i></td>
            </tr>
            {emptyRows.map((val, index) => <tr key={index} className="spacer">
                <td className="arpKey" colSpan="2"><i className="fa fa-trash-o old"></i></td>
            </tr>)}
            </tbody>
        }
        return (
            <tbody key={key}>
            <tr>
                <td className="arpKey">{this.nameOfArpKey(key)}</td>
                <td className="arpAttribute">
                    <table className="arpValues">
                        <tbody>
                        {arpValues.map(arpValue =>
                            <tr key={`${arpValue.source}-${arpValue.value}`}>
                                <td>
                                    <span className="arpSource">{arpValue.source}</span>
                                    <i className="fa fa-arrow-right"></i>
                                    <span className="arpValue">{arpValue.value}</span>
                                </td>
                            </tr>)}
                        </tbody>
                    </table>
                </td>
            </tr>
            </tbody>
        );
    };

    renderArpTable = (arp, currentArp) => {
        const arpKeys = Object.keys(arp.attributes);
        const currentArpKeys = Object.keys(currentArp.attributes);
        const uniqueKeys = Array.from(new Set([...arpKeys, ...currentArpKeys])).sort();
        return (
            <table>
                <thead>
                <tr>
                    <th className="title" colSpan={2}><CheckBox name="arp_changes"
                                                                value={this.state.applyChangesFor.arp}
                                                                onChange={this.changeApplyChangesFor("arp")}
                                                                info={I18n.t("import.arp")}/></th>
                </tr>
                <tr>
                    <th>{I18n.t("import.currentEntries", {name: "ARP"})}</th>
                    <th>{I18n.t("import.newEntries", {name: "ARP"})}</th>
                </tr>
                </thead>
                <tbody>
                <tr>
                    <td><CheckBox name="current_arp" value={currentArp.enabled} readOnly={true}
                                  info={I18n.t("import.arpEnabled")}/>
                    </td>
                    <td><CheckBox name="new_arp" value={arp.enabled} readOnly={true}
                                  info={I18n.t("import.arpEnabled")}/>
                    </td>
                </tr>
                <tr>
                    <td>
                        <table className="arp">
                            {uniqueKeys.map(key => this.renderArpAttribute(key, currentArp.attributes[key], arp.attributes[key], true))}
                        </table>
                    </td>
                    <td>
                        <table className="arp">
                            {uniqueKeys.map(key => this.renderArpAttribute(key, arp.attributes[key], currentArp.attributes[key], false))}
                        </table>
                    </td>
                </tr>
                </tbody>
            </table>

        );
    };

    renderResults = () => {
        const {results, errorsXml, errorsUrl, errorsJson, applyChangesFor} = this.state;
        const {newEntity} = this.props;
        const prefix = newEntity ? "new_" : "";
        const metaData = this.props.metaData.data;

        if (errorsUrl || errorsJson || errorsXml) {
            return this.renderErrors(errorsXml || errorsJson || errorsUrl);
        }
        if (!results) {
            return <h2 className="no_results">{I18n.t("import.no_results")}</h2>
        }
        const keys = Object.keys(results);
        if (keys.length === 0) {
            return <h2 className="no_results">{I18n.t("import.nothingChanged")}</h2>

        }
        const headers = ["include", "name", "current", "newValue"];
        const enabled = applyChangesFor && Object.keys(applyChangesFor).some(group => applyChangesFor[group])
        return (
            <section className="import-results">
                <div className="import-results-info">
                    <h2>{I18n.t(`import.${prefix}resultsInfo`)}</h2>
                    <p>{I18n.t(`import.${prefix}resultsSubInfo`)}</p>
                </div>
                {results.metaDataFields && this.renderKeyValueTable(results.metaDataFields, headers, "metaDataFields", newEntity)}
                {results.connection && this.renderKeyValueTable(results.connection, headers, "connection", newEntity)}
                {results.allowedEntities && this.renderAllowedEntitiesDisableContentTable(
                    results.allowedEntities, metaData.allowedEntities, "allowedEntities", "whitelist", newEntity)}
                {results.disableConsent && this.renderAllowedEntitiesDisableContentTable(
                    results.disableConsent, metaData.disableConsent, "disableConsent", "disabled consent", newEntity)}
                {results.arp && this.renderArpTable(results.arp, metaData.arp)}
                <div className="result-actions">
                    <span>{I18n.t(`import.${prefix}applyImportChangesInfo`)}</span>
                    <a className={`button ${enabled ? "green" : "grey disabled"}`} onClick={e => {
                        stop(e);
                        if (enabled) {
                            this.props.applyImportChanges(this.state.results, this.state.applyChangesFor);
                        }
                    }}>{I18n.t(`import.${prefix}applyImportChanges`)}<i className="fa fa-cloud-upload"></i>
                    </a>
                </div>
            </section>
        );
    };

    renderErrors = errors => {
        return (
            <section className="validation-errors">
                <p>{I18n.t("import.validationErrors", {type: this.props.metaData.type || this.state.entityType})}</p>
                <ul>
                    {errors.map((msg, index) =>
                        <li key={index}>{msg === "java.io.FileNotFoundException" ? "The URL returned a 404." : msg}</li>)}
                </ul>
            </section>
        );
    };

    changeType = option => this.setState({entityType: option ? option.value : null});

    renderImportHeader = (info, action, errors) =>
        <section>
            <section className="import-header">
                <h2>{info}</h2>
                <Select onChange={this.changeType}
                        name="select-entity-type"
                        options={["saml20_sp", "saml20_idp"].map(s => ({
                            value: s,
                            label: I18n.t(`metadata.${s}_single`)
                        }))}
                        value={this.state.entityType || "saml20_sp"}
                        disabled={this.props.guest || !this.props.newEntity}/>
                {!this.props.guest && <a onClick={action} className="button green large">
                    {I18n.t("import.fetch")}<i className="fa fa-cloud-download"></i></a>}
            </section>
            {errors && this.renderErrors(errors)}
        </section>;

    renderImportFooter = (action) =>
        <section className="import-footer">
            <Select onChange={this.changeType}
                    name="select-entity-type"
                    options={["saml20_idp", "saml20_sp"].map(s => ({value: s, label: I18n.t(`metadata.${s}_single`)}))}
                    value={this.state.entityType || "saml20_sp"}
                    disabled={this.props.guest || !this.props.newEntity}/>
            {!this.props.guest && <a onClick={action} className="button green footer">
                {I18n.t("import.fetch")}<i className="fa fa-cloud-download"></i></a>}
        </section>;

    renderImportUrl = () => (
        <section className="import-url-container">
            <section className="import-url">
                <section>
                    <section className="import-header">
                        <h2>{I18n.t("import.url")}</h2>
                    </section>
                    {this.state.errorsUrl && this.renderErrors(this.state.errorsUrl)}
                </section>
                {this.state.invalidUrl && <p className="invalid">{I18n.t("import.invalid", {type: "URL"})}</p>}
                <input ref={ref => this.importUrlField = ref} type="text" value={this.state.url}
                       onChange={e => this.setState({url: e.target.value})}/>
            </section>
            <section className="import-entity-id">
                <h3>{I18n.t("import.entityId")}</h3>
                <input type="text" value={this.state.entityId}
                       onChange={e => this.setState({entityId: e.target.value})}/>
            </section>
            {this.renderImportFooter(this.importUrl)}
        </section>);

    renderImportJsonUrl = () => (
        <section className="import-url-container">
            <section className="import-url">
                <section>
                    <section className="import-header">
                        <h2>{I18n.t("import.jsonUrl")}</h2>
                    </section>
                    {this.state.errorsJsonUrl && this.renderErrors(this.state.errorsJsonUrl)}
                </section>
                {this.state.invalidJsonUrl && <p className="invalid">{I18n.t("import.invalid", {type: "URL"})}</p>}
                <input type="text" value={this.state.jsonUrl}
                       onChange={e => this.setState({jsonUrl: e.target.value})}/>
            </section>
            {this.renderImportFooter(this.importJsonUrl)}
        </section>);

    renderImportJson = () => {
        const jsonOptions = {
            mode: {name: "javascript", json: true},
            lineWrapping: true,
            lineNumbers: true,
            scrollbarStyle: null
        };
        return <section className="import-json">
            {this.renderImportHeader(I18n.t("import.json"), this.importJson, this.state.errorsJson)}
            {this.state.invalidJson && <p className="invalid">{I18n.t("import.invalid", {type: "JSON"})}</p>}
            <CodeMirror key="json" name="json" value={this.state.json}
                        onChange={newJson => this.setState({json: newJson})}
                        options={jsonOptions} autoFocus={true}/>
            {this.renderImportFooter(this.importJson)}
        </section>

    };

    renderImportXml = () => {
        const xmlOptions = {
            mode: {name: "xml"},
            lineWrapping: true,
            lineNumbers: true,
            scrollbarStyle: null
        };
        return <section className="import-xml">
            {this.renderImportHeader(I18n.t("import.xml"), this.importXml, this.state.errorsXml)}
            {this.state.invalidXml && <p className="invalid">{I18n.t("import.invalid", {type: "XML"})}</p>}
            <CodeMirror key="xml" name="xml" value={this.state.xml}
                        onChange={newXml => this.setState({xml: newXml})}
                        options={xmlOptions} autoFocus={true}/>
            {this.renderImportFooter(this.importXml)}
        </section>;
    };


    renderSelectedTab = selectedTab => {
        switch (selectedTab) {
            case "import_xml_url":
                return this.renderImportUrl();
            case "import_xml":
                return this.renderImportXml();
            case "import_json_url":
                return this.renderImportJsonUrl();
            case "import_json":
                return this.renderImportJson();
            case "results":
                return this.renderResults();
            default:
                throw new Error("unknown tab: " + selectedTab);
        }
    };

    switchTab = tab => e => {
        stop(e);
        this.setState({selectedTab: tab});
    };

    renderTab = (tab, selectedTab) =>
        <span key={tab} className={tab === selectedTab ? "active" : ""} onClick={this.switchTab(tab)}>
            {I18n.t(`import.${tab}`)}
        </span>;

    render() {
        const {tabs, selectedTab} = this.state;
        return (
            <div className="metadata-import">
                <section className="sub-tabs">
                    {tabs.map(tab => this.renderTab(tab, selectedTab))}
                </section>
                <section className="content">
                    {this.renderSelectedTab(selectedTab)}
                </section>

            </div>
        );
    }
}

Import.propTypes = {
    metaData: PropTypes.object.isRequired,
    guest: PropTypes.bool.isRequired,
    entityType: PropTypes.string.isRequired,
    newEntity: PropTypes.bool.isRequired,
    applyImportChanges: PropTypes.func.isRequired
};
