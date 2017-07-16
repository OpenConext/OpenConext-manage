import React from "react";
import I18n from "i18n-js";
import PropTypes from "prop-types";
import CodeMirror from "react-codemirror";
import "codemirror/mode/javascript/javascript";
import "codemirror/mode/xml/xml";

import {stop} from "../../utils/Utils";
import {importMetaDataJSON, importMetaDataUrl, importMetaDataXML, validation} from "../../api";

import CheckBox from "../../components/CheckBox";

import "codemirror/lib/codemirror.css";
import "./Import.css";


export default class Import extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            url: "",
            invalidUrl: false,
            xml: "",
            invalidXml: false,
            json: "",
            invalidJson: false,
            results: undefined,
            resultsMap: undefined,
            errorsUrl: undefined,
            errorsJson: undefined,
            errorsXml: undefined,
            tabs: ["import_url", "import_xml", "import_json", "results"],
            selectedTab: "import_url",
            applyChangesFor: {}
        };
    }

    componentDidMount() {
        window.scrollTo(0, 0);
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
                    } else {
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
        if (results.connection.length === 0) {
            delete results.connection;
        }
    };

    changeMetaPropertySelected = (group, name) => e => {
        debugger;
        const newResults = {...this.state.results};
        newResults[group][name].selected = e.target.checked;
        this.setState({results: newResults});
    };

    doImport = (promise, errorsName) => {
        const newState = {...this.state};

        promise.then(json => {
            if (json.errors) {
                newState[errorsName] = json.errors;
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
                        "allowedEntities": true,
                        "disableConsent": true,
                        "arp" : true,
                        "metaDataFields": true,
                        "connection": true
                    }
                });
            }
        });
    };

    importUrl = e => {
        stop(e);
        const {url} = this.state;
        const {type} = this.props.metaData;
        validation("url", url).then(result => {
            this.setState({
                invalidUrl: !result
            });
            if (result) {
                this.doImport(importMetaDataUrl(type, url), "errorsUrl");
            }
        });
    };

    importJson = e => {
        stop(e);
        const {json} = this.state;
        const {type} = this.props.metaData;
        validation("json", json).then(result => {
            this.setState({
                invalidJson: !result
            });
            if (result) {
                this.doImport(importMetaDataJSON(type, json), "errorsJson");
            }
        });
    };

    importXml = e => {
        stop(e);
        const {xml} = this.state;
        const {type} = this.props.metaData;
        validation("xml", xml).then(result => {
            this.setState({
                invalidXml: !result
            });
            if (result) {
                this.doImport(importMetaDataXML(type, xml), "errorsJson");
            }
        });

    };


    renderConnectionTable = (results, headers) =>
        <table>
            <thead>
            <tr>
                <th className="title" colSpan={4}>{I18n.t("metadata.tabs.connection")}</th>
            </tr>
            <tr>
                {headers.map(header => <th key={header}>{I18n.t(`import.headers.${header}`)}</th>)}
            </tr>
            </thead>
            <tbody>
            {Object.keys(results.connection).map(key => {
                const prop = results.connection[key];
                return (
                    <tr key={key}>
                        <td>{<CheckBox name={key} value={prop.selected}
                                       onChange={this.changeMetaPropertySelected("connection", key)}/>}</td>
                        <td>{key}</td>
                        <td>{prop.current}</td>
                        <td>{prop.value}</td>
                    </tr> )
            })}
            </tbody>
        </table>;

    renderResults = () => {
        const {results, errorsXml, errorsUrl, errorsJson} = this.state;
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
        return (
            <section className="import-results">
                <h2>{I18n.t("import.resultsInfo")}</h2>
                <p>{I18n.t("import.resultsSubInfo")}</p>
                {results.connection && this.renderConnectionTable(results, headers)}
            </section>
        );
    };

    renderErrors = errors =>
        <section className="validation-errors">
            <p>{I18n.t("import.validationErrors", {type: this.props.metaData.type})}</p>
            <ul>
                {errors.map((msg, index) =>
                    <li key={index}>{msg}</li>)}
            </ul>
        </section>;

    renderImportHeader = (info, action, errors) =>
        <section>
            <section className="import-header">
                <h2>{info}</h2>
                {!this.props.guest && <a onClick={action} className="button green large">
                    {I18n.t("import.fetch")}<i className="fa fa-cloud-download"></i></a>}
            </section>
            {errors && this.renderErrors(errors)}
        </section>;


    renderImportUrl = () =>
        <section className="import-url">
            {this.renderImportHeader(I18n.t("import.url"), this.importUrl, this.state.errorsUrl)}
            {this.state.invalidUrl && <p className="invalid">{I18n.t("import.invalid", {type: "URL"})}</p>}
            <input type="text" value={this.state.url} onChange={e => this.setState({url: e.target.value})}/>
        </section>;

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
                        options={jsonOptions}/>
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
                        onChange={newXml => this.setState({xml: newXml})} options={xmlOptions}/>
        </section>;
    };


    renderSelectedTab = selectedTab => {
        switch (selectedTab) {
            case "import_url":
                return this.renderImportUrl();
            case "import_xml":
                return this.renderImportXml();
            case "import_json":
                return this.renderImportJson();
            case "results":
                return this.renderResults();
            default:
                throw new Error("unknown tab");
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
                {this.renderSelectedTab(selectedTab)}
            </div>
        );
    }
}

Import.propTypes = {
    metaData: PropTypes.object.isRequired,
    configuration: PropTypes.object.isRequired,
    guest: PropTypes.bool.isRequired
};

