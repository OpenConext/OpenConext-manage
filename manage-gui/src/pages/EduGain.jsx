import React from "react";
import I18n from "i18n-js";
import PropTypes from "prop-types";
import {countFeed, deleteFeed, importFeed, validation} from "../api";
import "./EduGain.css";
import {isEmpty, stop} from "../utils/Utils";
import {setFlash} from "../utils/Flash";
import JSONPretty from "react-json-pretty";
import "react-json-pretty/JSONPretty.monikai.styl";

export default class EduGain extends React.PureComponent {

    constructor(props) {
        super(props);
        this.state = {
            url: "http://mds.edugain.org/",//"http://localhost:8000/edugain.xml"
            invalidUrl: false,
            results: {},
            tabs: ["import_feed", "delete_import"],
            selectedTab: "import_feed",
            loading: false,
            deleting: false,
            count: "?"
        };
    }

    componentDidMount() {
        countFeed().then(json => this.setState({count: json.count}));
    }

    deleteImport = e => {
        stop(e);
        const {deleting} = this.state;
        if (deleting) {
            return;
        }
        this.setState({deleting: true});
        deleteFeed().then(json => {
            setFlash(I18n.t("edugain.deletedFlash", {number: json.deleted}));
            this.setState({deleting: false, count: "0"})
        });

    };

    doImportFeed = e => {
        stop(e);
        const {url, loading} = this.state;
        if (loading) {
            return;
        }
        validation("url", url).then(result => {
            this.setState({
                invalidUrl: !result
            });
            if (result) {
                this.setState({loading: true});
                importFeed(url).then(result => {
                    if (result["errors"]) {
                        setFlash(JSON.stringify(result), "error");
                        this.setState({results: {}, loading: false});
                    } else {
                        this.setState({results: result, loading: false});
                    }
                });
            }
        });
    };

    renderResults = results => {
        return (
            <section className="results">
                <JSONPretty json={results}></JSONPretty>
            </section>
        );
    };

    renderImportFeed = (results, loading) =>
        <section><p className="info">Import all Service Providers from the specified feed.
            <span className="warning"> Note that there is no review before the Service Providers are imported.</span>
        </p>
            <section className="form">
                {this.state.invalidUrl && <p className="invalid">{I18n.t("import.invalid", {type: "URL"})}</p>}
                <input type="text" value={this.state.url}
                       onChange={e => this.setState({url: e.target.value})}/>
                <a onClick={this.doImportFeed} className={`button large ${loading ? "disabled grey" : "green"}`}>
                    {I18n.t("import.fetch")}<i className="fa fa-cloud-download"></i></a>
            </section>
            {!isEmpty(results) && this.renderResults(results)}
        </section>;

    renderTab = (tab, selectedTab) =>
        <span key={tab} className={tab === selectedTab ? "active" : ""} onClick={this.switchTab(tab)}>
            {I18n.t(`edugain.${tab}`)}
        </span>;

    renderDeleteImport = (deleting, count) => <section className="delete">
        <p className="info">The <span className="warning">delete</span> functionality will delete
            <span className="code"> {count} </span>
            service providers with the metadata field <span className="code">coin:imported_from_edugain </span>
            set to <span className="code"> 1</span>. This flag is set during the import of the metadata feed.
        </p>
        <a onClick={this.deleteImport} className={`button large ${deleting ? "disabled grey" : "red"}`}>
            {I18n.t("edugain.delete")}<i className="fa fa-trash"></i></a>
    </section>;

    renderSelectedTab = (selectedTab, results, loading, deleting, count) => {
        switch (selectedTab) {
            case "import_feed":
                return this.renderImportFeed(results, loading);
            case "delete_import":
                return this.renderDeleteImport(deleting, count);
            default:
                throw new Error("unknown tab: " + selectedTab);
        }
    };

    switchTab = tab => e => {
        stop(e);
        this.setState({selectedTab: tab});
        countFeed().then(json => this.setState({count: json.count}));
    };


    render() {
        const {results, tabs, selectedTab, loading, deleting, count} = this.state;
        return (
            <div className="edugain">
                <section className="sub-tabs">
                    {tabs.map(tab => this.renderTab(tab, selectedTab))}
                </section>
                <section className="content">
                    {this.renderSelectedTab(selectedTab, results, loading, deleting, count)}
                </section>
            </div>
        );
    }
}

EduGain.propTypes = {
    history: PropTypes.object.isRequired,
    currentUser: PropTypes.object.isRequired,
    configuration: PropTypes.array.isRequired
};

