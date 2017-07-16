import React from "react";
import I18n from "i18n-js";

import {DiffPatcher} from 'jsondiffpatch/src/diffpatcher';
import htmlFormatter from "jsondiffpatch/src/formatters/html";

import PropTypes from "prop-types";

import CheckBox from "../../components/CheckBox";

import "jsondiffpatch/public/formatters-styles/html.css";
import "./Revisions.css";

const ignoreInDiff = ["id", "eid", "revisionid", "user", "created", "ip", "revisionnote", "notes"];

export default class Revisions extends React.Component {

    constructor(props) {
        super(props);
        const {revisions} = this.props;
        this.state = {
            showRevisionDetails: revisions.reduce((acc, revision) => {
                acc[revision.id] = false;
                return acc;
            }, {}),
            showAllDetails: false
        };
        this.differ = new DiffPatcher();
    }

    componentDidMount() {
        window.scrollTo(0, 0);
    }

    previousRevision = revision => this.props.revisions.find(rev => rev.revision.number === (revision.revision.number - 1));

    toggleAllShowDetail = e => {
        const showAll = e.target.checked;
        const newShowRevisionDetails = {...this.state.showRevisionDetails};
        Object.keys(newShowRevisionDetails).forEach(id => newShowRevisionDetails[id] = showAll);
        this.setState({showRevisionDetails: newShowRevisionDetails, showAllDetails: showAll});
    };

    toggleShowDetail = revision => {
        const newShowRevisionDetails = {...this.state.showRevisionDetails};
        newShowRevisionDetails[revision.id] = !newShowRevisionDetails[revision.id];
        this.setState({showRevisionDetails: newShowRevisionDetails});
    };

    renderDiff = (revision, previous) => {
        const rev = {...revision.data};
        ignoreInDiff.forEach(ignore => delete rev[ignore]);
        const prev = {...previous.data};
        ignoreInDiff.forEach(ignore => delete prev[ignore]);

        const diffs = this.differ.diff(prev, rev);
        const html = htmlFormatter.format(diffs);
        return diffs ?  <p dangerouslySetInnerHTML={{__html: html}}/> : <p>{I18n.t("revisions.identical")}</p>
    };

    renderRevisionTable = (revision) => {
        const showDetail = this.state.showRevisionDetails[revision.id];
        const headers = ["number", "created", "updatedBy", "status", "notes"];
        const isFirstRevision = revision.revision.number === 0;
        return (
            <table className="revision-table" key={revision.revision.number}>
                <thead>
                <tr>
                    {headers.map((header, index) => <th key={index}
                                                        className={header}>{I18n.t(`revisions.${header}`)}</th>)}
                </tr>
                </thead>
                <tbody>
                <tr>
                    <td>{revision.revision.number}</td>
                    <td>{new Date(revision.revision.created * 1000).toGMTString()}</td>
                    <td>{revision.revision.updatedBy}</td>
                    <td>{I18n.t(`metadata.${revision.data.state}`)}</td>
                    <td>{revision.data.revisionnote || ""}</td>
                </tr>
                {!isFirstRevision &&
                <tr>
                    <td colSpan={headers.length}><CheckBox name={revision.id} value={showDetail}
                                                           info={I18n.t("revisions.toggleDetails")}
                                                           onChange={() => this.toggleShowDetail(revision)}/></td>
                </tr>}
                {(showDetail && !isFirstRevision) &&
                <tr>
                    <td className="diff"
                        colSpan={headers.length}>{this.renderDiff(revision, this.previousRevision(revision))}</td>
                </tr>}
                </tbody>
            </table>
        );
    };

    render() {
        const {revisions, isNew} = this.props;
        const {showAllDetails} = this.state;

        return (
            <div className="metadata-revisions">
                {isNew && <div className="revisions-info">
                    <h2>{I18n.t("revisions.noRevisions")}</h2>
                </div>}
                {!isNew && <div className="revisions-info">
                    <h2>{I18n.t("revisions.info")}</h2>
                    {revisions.length > 1 && <CheckBox name="toggleDiffs" value={showAllDetails}
                              info={I18n.t("revisions.toggleAllDetails")}
                              onChange={this.toggleAllShowDetail}/>}
                </div>}
                {!isNew && <div className="revisions">

                    {revisions.map(rev => this.renderRevisionTable(rev))}
                </div>}
            </div>
        );
    }
}

Revisions.propTypes = {
    revisions: PropTypes.array.isRequired,
    isNew: PropTypes.bool.isRequired
};

