import React from "react";
import I18n from "i18n-js";
import PropTypes from "prop-types";
import {importFeed, ping, validation} from "../api";
import "./EduGain.css";
import {isEmpty, stop} from "../utils/Utils";
import {setFlash} from "../utils/Flash";

export default class EduGain extends React.PureComponent {

    constructor(props) {
        super(props);
        this.state = {
            url: "http://mds.edugain.org/",
            invalidUrl: false,
            results: {}
        };
    }

    componentDidMount() {
        ping().then(() => this);
    }

    doImportFeed = e => {
        stop(e);
        const {url} = this.state;
        validation("url", url).then(result => {
            this.setState({
                invalidUrl: !result
            });
            if (result) {
                importFeed(url).then(result => {
                    if (result.indexOf("errors") > -1) {
                        setFlash(JSON.stringify(result.get(0)), "error");
                    } else {
                        this.setState({results: result});
                    }
                });
            }
        });
    };

    renderResults = results => {
        return (
            <section className="results">
                {JSON.stringify(results)}
            </section>
        );
    };

    render() {
        const {results} = this.state;
        return (
            <div className="edugain">
                <p className="info">Import all Service Providers from the specified feed</p>
                <section className="form">
                    {this.state.invalidUrl && <p className="invalid">{I18n.t("import.invalid", {type: "URL"})}</p>}
                    <input type="text" value={this.state.url}
                           onChange={e => this.setState({url: e.target.value})}/>
                    <a onClick={this.doImportFeed} className="button green large">
                        {I18n.t("import.fetch")}<i className="fa fa-cloud-download"></i></a>
                </section>
                {!isEmpty(results) && this.renderResults(results)}
            </div>
        );
    }
}

EduGain.propTypes = {
    history: PropTypes.object.isRequired,
    currentUser: PropTypes.object.isRequired,
    configuration: PropTypes.array.isRequired
};

