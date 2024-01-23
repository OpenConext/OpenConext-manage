import React from "react";
import I18n from "i18n-js";
import ReactJson from "react-json-view";
import {DiffPatcher, formatters} from 'jsondiffpatch';
import merge from "lodash.merge";
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
            confirmationQuestion: "",
            currentRequest: {},
            revisionNotes: "",
            accept: false
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
        const {revisionNotes} = this.state;
        this.setState({confirmationDialogOpen: false});
        let changeRequest = {id: id, type: entityType, metaDataId: metaData.id, revisionNotes: revisionNotes};
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

    action = (request, entityType, metaData, accept) => e => {
        stop(e);
        this.setState({
            currentRequest: request,
            accept: accept,
            revisionNotes: request.auditData.notes || "",
            confirmationDialogOpen: true,
            confirmationQuestion: I18n.t(`changeRequests.${accept ? "accept" : "reject"}Confirmation`),
            confirmationDialogAction: this.doAction(request.id, entityType, metaData, accept)
        });
    };

    applyPathUpdates = (pathUpdates, data, pathUpdateType) => {
        const newData = cloneDeep(data);

        Object.keys(pathUpdates).forEach(key => {
            const singleValue = pathUpdates[key];
            if (typeof singleValue === "string") {
                newData[key] = singleValue;
            } else if (typeof singleValue === "object" && (!newData[key] || !Array.isArray(newData[key])) &&
                ["allowedEntities", "disableConsent", "stepupEntities", "mfaEntities", "allowedResourceServers",
                    "mfaEntities"].indexOf(key) === -1) {
                if (pathUpdateType === "ADDITION") {
                    if (!newData[key]) {
                        newData[key] = {};
                    }
                    newData[key] = merge(newData[key], singleValue);
                } else if (newData[key]) {
                    Object.keys(singleValue).forEach(k => delete newData[k]);
                }
            } else {
                const value = Array.isArray(singleValue) ? singleValue : [singleValue];
                if (pathUpdateType === "ADDITION") {
                    if (!newData[key]) {
                        newData[key] = []
                    }
                    //first remove everything, could be an attribute value change like loa-level
                    const newValue = newData[key];
                    if (Array.isArray(newValue)) {
                        const newValues = newValue.filter(entry => !value.some(ent => entry.name === ent.name));
                        //this alters the order and confuses the diff
                        newData[key] = newValues.concat(value);
                        newData[key].sort((a, b) => a.name.localeCompare(b.name));
                    }
                    const currentValue = data[key];
                    if (!currentValue) {
                        data[key] = []
                    }
                    if (Array.isArray(currentValue)) {
                        data[key].sort((a, b) => a.name.localeCompare(b.name));
                    }
                } else if (newData[key]) {
                    const newKey = newData[key];
                    if (Array.isArray(newKey)) {
                        newData[key] = newKey.filter(entry => !value.some(ent => entry.name === ent.name));
                    }

                }
            }
        });
        return newData;
    }

    renderDiff = (request, metaData) => {
        const pathUpdates = request.pathUpdates
        const data = cloneDeep(metaData.data);
        const newData = collapseDotKeys(cloneDeep(pathUpdates));
        sortDict(data);
        sortDict(newData);
        let diffs;
        if (request.incrementalChange) {
            //we can not simply show the pathUpdate (e.g. newData) as this is not against the actual data
            const appliedData = this.applyPathUpdates(newData, data, request.pathUpdateType);
            diffs = this.differ.diff(data, appliedData);
        } else {
            const originalDict = createDiffObject(data, newData);
            diffs = this.differ.diff(originalDict, newData);
        }
        const html = formatters.html.format(diffs);
        return diffs ? <p dangerouslySetInnerHTML={{__html: html}}/> : <p>{I18n.t("changeRequests.identical")}</p>
    };

    requestToJson = request => {
        const type = request.incrementalChange ? request.pathUpdateType : "REPLACEMENT";
        return {type: type, pathUpdates: request.pathUpdates};
    }

    renderChangeRequestTable = (request, entityType, metaData, i) => {
        const showDetail = this.state.showChangeRequests[request.id];
        const headers = ["created", "apiClient", "incremental", "changes", "nope"];
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
                    <td className={"notes"} colSpan={headers.length}>Summary: <span
                        className={"notes"}>{request.note ? request.note : "-"}</span></td>
                </tr>
                <tr>
                    <td>{new Date(request.created).toGMTString()}</td>
                    <td>{request.auditData.userName}</td>
                    <td className={"incremental"}>
                        <div className={"wrapper"}><CheckBox name={"incremental"} readOnly={true}
                                                            value={request.incrementalChange || false}/>
                        </div>
                    </td>
                    <td><ReactJson src={this.requestToJson(request)} name="changeRequest" collapsed={true}/></td>
                    <td className="nope">
                        <div className="accept">
                            <a className="button blue" href={`/accept/${request.id}`}
                               onClick={this.action(request, entityType, metaData, true)}>
                                {I18n.t("changeRequests.accept")}</a>
                            <a className="button red" href={`/reject/${request.id}`}
                               onClick={this.action(request, entityType, metaData, false)}>
                                {I18n.t("changeRequests.reject")}</a>
                        </div>

                    </td>
                </tr>
                <tr>
                    <td colSpan={5}><CheckBox name={`${i}`}
                                  value={showDetail || false}
                                  info={I18n.t("changeRequests.toggleDetails")}
                                  onChange={() => this.toggleShowDetail(request)}/>
                    </td>
                </tr>
                {showDetail &&
                <tr>
                    <td className="diff"
                        colSpan={headers.length}>{this.renderDiff(request, metaData)}</td>
                </tr>}
                </tbody>
            </table>
        );
    };

    renderNotes = revisionNotes => {
        return (
            <div className={"accept-notes"}>
                <p>{I18n.t("changeRequests.revisionNotes")}</p>
                <textarea name={"acceptance-notes"}
                          value={revisionNotes}
                          placeholder={I18n.t("changeRequests.revisionNotesPlaceholder")}
                          onChange={e => this.setState({revisionNotes: e.target.value})}/>

            </div>
        );
    }

    render() {
        const {requests, entityType, metaData, changeRequestsLoaded} = this.props;
        const {
            showAllDetails, cancelDialogAction, confirmationDialogAction, confirmationDialogOpen,
            confirmationQuestion, revisionNotes, accept
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
                                    children={accept && this.renderNotes(revisionNotes)}
                                    confirm={confirmationDialogAction}
                                    disableConfirm={accept && isEmpty(revisionNotes)}
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

