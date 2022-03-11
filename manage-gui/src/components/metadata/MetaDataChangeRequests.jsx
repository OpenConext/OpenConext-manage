import React from "react";
import I18n from "i18n-js";
import ReactJson from "react-json-view";
import {DiffPatcher, formatters} from 'jsondiffpatch';

import PropTypes from "prop-types";
import cloneDeep from "lodash.clonedeep";
import CheckBox from "../../components/CheckBox";
import ConfirmationDialog from "../../components/ConfirmationDialog";
import {collapseDotKeys, createDiffObject, isEmpty, sortDict, stop} from "../../utils/Utils";

import "jsondiffpatch/dist/formatters-styles/html.css";
import "./MetaDataChangeRequests.scss";
import {acceptChangeRequest, rejectChangeRequest} from "../../api";
import {emitter, setFlash} from "../../utils/Flash";
import withRouterHooks from "../../utils/RouterBackwardCompatability";
import NotesTooltip from "../NotesTooltip";


class MetaDataChangeRequests extends React.Component {

    constructor(props) {
        super(props);
        const {requests} = this.props;
        this.state = {
            showChangeRequests: requests.reduce((acc, request) => {
                acc[request.id] = false;
                return acc;
            }, {}),
            showAllDetails: false,
            confirmationDialogOpen: false,
            confirmationDialogAction: () => this,
            cancelDialogAction: () => this.setState({confirmationDialogOpen: false}),
            confirmationQuestion: ""
        };
        this.differ = new DiffPatcher({
            // https://github.com/benjamine/jsondiffpatch/blob/HEAD/docs/arrays.md
            objectHash: (obj, index) => obj.name || obj.level || obj.type || obj.source || obj.value || '$$index:' + index
        });
    }

    componentDidMount() {
        window.scrollTo(0, 0);
        this.toggleAllShowDetail({target: {checked: true}});
    }

    componentDidUpdate(prevProps) {
        if (prevProps.requests.length !== this.props.requests.length) {
            this.toggleAllShowDetail({target: {checked: true}});
        }
    }

    toggleAllShowDetail = e => {
        const showAll = e.target.checked;
        const {requests} = this.props;
        const checkedState = {...this.state.showChangeRequests};
        const newShowChangeRequests = isEmpty(checkedState) ?
            requests.reduce((acc, request) => {
                acc[request.id] = false;
                return acc;
            }, {}) : checkedState;
        Object.keys(newShowChangeRequests).forEach(id => newShowChangeRequests[id] = showAll);
        this.setState({showChangeRequests: newShowChangeRequests, showAllDetails: showAll});
    };

    toggleShowDetail = request => {
        const {requests} = this.props;
        const checkedState = {...this.state.showChangeRequests};
        const newShowChangeRequests = isEmpty(checkedState) ?
            requests.reduce((acc, request) => {
                acc[request.id] = false;
                return acc;
            }, {}) : checkedState;
        newShowChangeRequests[request.id] = !newShowChangeRequests[request.id];
        this.setState({showChangeRequests: newShowChangeRequests});
    };

    doAction = (id, entityType, metaData, accept) => () => {
        this.setState({confirmationDialogOpen: false});
        let changeRequest = {id: id, type: entityType, metaDataId: metaData.id};
        const promise = accept ? acceptChangeRequest : rejectChangeRequest;
        promise(changeRequest).then(json => {
            if (json.exception || json.error) {
                setFlash(json.validations, "error");
                window.scrollTo(0, 0);
            } else {
                const name = json.data.metaDataFields["name:en"] || json.data.metaDataFields["name:nl"] || "this service";
                setFlash(I18n.t(`changeRequests.flash.${accept ? "accepted" : "rejected"}`, {
                    name: name
                }));
                emitter.emit("changeRequests");
                const path = encodeURIComponent(`/metadata/${entityType}/${metaData.id}/requests`);
                this.props.navigate(`/refresh-route/${path}`, {replace: true});
            }
        });
    };

    action = (id, entityType, metaData, accept) => e => {
        stop(e);
        this.setState({
            confirmationDialogOpen: true,
            confirmationQuestion: I18n.t(`changeRequests.${accept ? "accept" : "reject"}Confirmation`),
            confirmationDialogAction: this.doAction(id, entityType, metaData, accept)
        });
    };

    renderDiff = (changeRequest, metaData) => {
        const data = cloneDeep(metaData.data);
        const request = collapseDotKeys(cloneDeep(changeRequest));
        sortDict(data);
        sortDict(request);
        const originalDict = createDiffObject(data, request);
        const diffs = this.differ.diff(originalDict, request);
        const html = formatters.html.format(diffs);
        return diffs ? <p dangerouslySetInnerHTML={{__html: html}}/> : <p>{I18n.t("changeRequests.identical")}</p>
    };

    renderChangeRequestTable = (request, entityType, metaData, i) => {
        const showDetail = this.state.showChangeRequests[request.id];
        const headers = ["created", "apiClient", "changes", "note", "nope"];
        return (
            <table className="change-requests-table" key={request.id}>
                <thead>
                <tr>
                    {headers.map((header, index) =>
                        <th key={index} className={header}>{I18n.t(`changeRequests.${header}`)}</th>)}
                </tr>
                </thead>
                <tbody>
                <tr>
                    <td>{new Date(request.created).toGMTString()}</td>
                    <td>{request.auditData.userName}</td>
                    <td><ReactJson src={request.pathUpdates} name="pathUpdates" collapsed={true}/></td>
                    <td className="info">
                        {isEmpty(request.note) ? <span></span> :
                            <NotesTooltip identifier={request.id} notes={request.note}/>}
                    </td>
                    <td className="nope">
                        <div className="accept">
                            <a className="button blue" href={`/accept/${request.id}`}
                               onClick={this.action(request.id, entityType, metaData, true)}>
                                {I18n.t("changeRequests.accept")}</a>
                            <a className="button red" href={`/reject/${request.id}`}
                               onClick={this.action(request.id, entityType, metaData, false)}>
                                {I18n.t("changeRequests.reject")}</a>
                        </div>

                    </td>
                </tr>
                <tr>
                    <td><CheckBox name={i}
                                  value={showDetail || false}
                                  info={I18n.t("changeRequests.toggleDetails")}
                                  onChange={() => this.toggleShowDetail(request)}/>
                    </td>
                </tr>
                {showDetail &&
                <tr>
                    <td className="diff"
                        colSpan={headers.length}>{this.renderDiff(request.pathUpdates, metaData)}</td>
                </tr>}
                </tbody>
            </table>
        );
    };

    render() {
        const {requests, entityType, metaData, changeRequestsLoaded} = this.props;
        const {
            showAllDetails, cancelDialogAction, confirmationDialogAction, confirmationDialogOpen,
            confirmationQuestion
        } = this.state;
        if (!changeRequestsLoaded) {
            return <div className="metadata-change-requests">
                <div className="loading"><span>Loading...</span></div>
            </div>
        }
        return (
            <div className="metadata-change-requests">
                <ConfirmationDialog isOpen={confirmationDialogOpen}
                                    cancel={cancelDialogAction}
                                    confirm={confirmationDialogAction}
                                    question={confirmationQuestion}/>
                {requests.length === 0 && <div className="change-request-info">
                    <h2>{I18n.t("changeRequests.noChangeRequests")}</h2>
                </div>}
                {requests.length > 0 && <div className="change-request-info">
                    <h2>{I18n.t("changeRequests.info", {name: metaData.data.metaDataFields["name:en"]})}</h2>
                    {requests.length > 1 && <CheckBox name="toggleDiffs" value={showAllDetails || false}
                                                      info={I18n.t("changeRequests.toggleAllDetails")}
                                                      onChange={this.toggleAllShowDetail}/>}
                </div>}
                {<div className="change-requests">
                    {requests.map((request, i) => this.renderChangeRequestTable(request, entityType, metaData, i))}
                </div>}
            </div>
        );
    }
}

export default withRouterHooks(MetaDataChangeRequests);

MetaDataChangeRequests.propTypes = {
    requests: PropTypes.array.isRequired,
    metaData: PropTypes.object.isRequired,
    entityType: PropTypes.string.isRequired,
    changeRequestsLoaded: PropTypes.bool
};

