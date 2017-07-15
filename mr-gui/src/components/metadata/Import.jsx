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
            validUrl: false,
            xml: "",
            validXml: false,
            json: "",
            validJson: false,
            results: {},
            errors: {},
            tabs: ["import_url", "import_xml", "import_json", "results"],
            selectedTab: "import_url"
        };
    }

    componentDidMount() {
        window.scrollTo(0, 0);
    }

    doImport = promise => {
        promise.then(json => {
            debugger;
            if (json.errors) {
                this.setState({errors: json.errors});
            } else {
                this.setState({results: json, selectedTab: "results"});
            }
        });

    };

    importUrl = e => {
        stop(e);
        const {url} = this.state;
        const type = this.props.metaData;
        validation("uri", url).then(result => {
            this.setState({
                validUrl: !result
            });
            if (result) {
                this.doImport(importMetaDataUrl(type, url));
            }
        });
    };

    importJson = e => {
        stop(e);
        const {json} = this.state;
        const type = this.props.metaData;
        validation("json", json).then(result => {
            this.setState({
                validJson: !result
            });
            if (result) {
                this.doImport(importMetaDataJSON(type, json));
            }
        });
    };

    importXml = e => {
        stop(e);
        const {xml} = this.state;
        const type = this.props.metaData;
        validation("xml", xml).then(result => {
            this.setState({
                validXml: !result
            });
            if (result) {
                this.doImport(importMetaDataXML(type, xml));
            }
        });

    };


    renderResults = () => {
        const {results, errors} = this.state;
        return <p>Results</p>;
    };

    renderImportHeader = (info, action) =>
        <section className="import-header">
            <h2>{info}</h2>
            <a onClick={action} className="button green large">
                {I18n.t("import.fetch")}<i className="fa fa-cloud-download"></i></a>
        </section>;


    renderImportUrl = () =>
        <section className="import-url">
            {this.renderImportHeader(I18n.t("import.url"), this.importUrl)}
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
            {this.renderImportHeader(I18n.t("import.json"), this.importJson)}
            <CodeMirror key="json" name="json" value={this.state.json} onChange={newJson => this.setState({json: newJson})}
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
            {this.renderImportHeader(I18n.t("import.xml"), this.importXml)}
            <CodeMirror key="xml" name="xml" value={this.state.xml} onChange={newXml => this.setState({xml: newXml})}
                        options={xmlOptions}/>
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
    configuration: PropTypes.object.isRequired
};

