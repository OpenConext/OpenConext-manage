import React from "react";
import I18n from "i18n-js";
import PropTypes from "prop-types";
import CodeMirror from "react-codemirror";
import "codemirror/mode/javascript/javascript";
import "codemirror/mode/xml/xml";

import {stop} from "../../utils/Utils";

import {importMetaDataJSON, importMetaDataUrl, importMetaDataXML, validation} from "../../api";

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
            selectedTab: "import_url"
        };
    }

    componentDidMount() {
        window.scrollTo(0, 0);
    }

    /**
     * This is not generic on purpose. It is possible, but it makes the code very complex and
     * we already make assumptions about the data structure
     */
    resultsToMap = results => {
        const keys = Object.keys(results);
        keys.forEach(key => {
            const value = results[key];
            if (key === "allowedEntities" || key === "disableConsent") {
                value.forEach(obj => obj.selected = true);
            } else if (key === "arp") {
                const arpAttributes = Object.keys(value.attributes);
                arpAttributes.forEach(attr => {
                    value.attributes[attr].forEach(arpValue => arpValue.selected = true);
                });
                value.enabled = {value: value.enabled, selected: true};
            } else if (key === "metaDataFields") {
                const metaDataFields = Object.keys(value);
                metaDataFields.forEach(field => {
                    value[field] = {value: value[field], selected: true};
                })
            } else {
                results[key] = {value: results[key], selected: true}
            }
        });
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
                debugger;
                this.setState({
                    results: json,
                    errorsUrl: undefined,
                    errorsJson: undefined,
                    errorsXml: undefined,
                    selectedTab: "results"
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

    renderResults = () => {
        const {results, errorsXml, errorsUrl, errorsJson} = this.state;
        if (errorsUrl || errorsJson || errorsXml) {
            return this.renderErrors(errorsXml || errorsJson || errorsUrl);
        }
        if (!results) {
            return <h2 className="no_results">{I18n.t("import.no_results")}</h2>
        }
        return <section className="import-results">

        </section>;
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

