import React from "react";
import I18n from "i18n-js";
import {allChangeRequests} from "../api";
import {copyToClip, isEmpty, stop} from "../utils/Utils";
import "./Support.scss";
import {Select} from "../components";
import withRouterHooks from "../utils/RouterBackwardCompatability";

class ChangeRequests extends React.PureComponent {

    constructor(props) {
        super(props);
        this.state = {
            changeRequests: [],
            filteredChangeRequests: [],
            query: "",
            copiedToClipboardClassName: "",
            loaded: false,
            status: "all",
            confirmationDialogOpen: false,
            confirmationQuestion: "",
            confirmationDialogAction: () => this,
            cancelDialogAction: () => this.setState({confirmationDialogOpen: false})
        };
    }

    componentDidMount() {
        allChangeRequests().then(res => {
            res.forEach(cr => cr.createdAt = new Date(cr.created));
            this.setState({
                changeRequests: res.sort((a, b) => b.createdAt - a.createdAt),
                filteredChangeRequests: res,
                loaded: true
            });
        })
    }

    copyToClipboard = e => {
        stop(e);
        if (!isEmpty(this.state.changeRequests)) {
            copyToClip("results-printable");
            this.setState({copiedToClipboardClassName: "copied"});
            setTimeout(() => this.setState({copiedToClipboardClassName: ""}), 5000);
        }
    };

    renderSearchResultsTablePrintable = filteredChangeRequests => {
        return <section id={"results-printable"}>
            {filteredChangeRequests
                .map(cr => `${cr.metaDataSummary.state},${cr.metaDataSummary.entityid},${cr.metaDataSummary.name},${cr.metaDataSummary.organizationName}`)
                .join("\n")}</section>;
    }

    changeStatus = option => this.setState({status: option ? option.value : null});

    renderSearchResultsTable = (filteredChangeRequests, status) => {
        const searchHeaders = ["name", "organization", "entityid", "status", "createdAt", "type", "user"];
        filteredChangeRequests = status === "all" ? filteredChangeRequests : filteredChangeRequests.filter(cr => cr.metaDataSummary.state === status);
        return (
            <section>
                <table className="search-results">
                    <thead>
                    <tr>
                        {searchHeaders.map((header, index) =>
                            <th key={`${header}_${index}`}
                                className={header}>{I18n.t(`playground.headers.${header}`)}</th>)}
                    </tr>
                    </thead>
                    <tbody>
                    {filteredChangeRequests.map((cr, index) => <tr
                        className="clickable"
                        key={`${cr.metaDataSummary.entityid}_${index}`}
                        onClick={() => this.props.navigate(`/metadata/${cr.type}/${cr.metaDataId}/requests`)}>
                        <td className="name">{cr.metaDataSummary.name}</td>
                        <td className="organization">{cr.metaDataSummary.organizationName}</td>
                        <td className="entityId">{cr.metaDataSummary.entityid}</td>
                        <td className="state">{I18n.t(`metadata.${cr.metaDataSummary.state}`)}</td>
                        <td className="created">{cr.createdAt.toGMTString()}</td>
                        <td className="type">{cr.type}</td>
                        <td className="user">{cr.auditData.userName}</td>
                    </tr>)}
                    </tbody>
                </table>
            </section>);

    }

    search = e => {
        const query = e.target.value ? e.target.value.toLowerCase() : "";
        const {changeRequests} = this.state;
        const names = ["organizationName", "name", "entityid"];
        const result = isEmpty(query) ? changeRequests : changeRequests.filter(cr => {
            const summary = cr.metaDataSummary;
            return names.some(name => summary[name] && summary[name].toLowerCase().indexOf(query) > -1)
        });
        this.setState({query: query, filteredChangeRequests: result});
    };


    renderResults = (changeRequests, copiedToClipboardClassName, status, query) => <div>
        <section className="explanation">
            <p>Below are all the external change requests. You can look at the details by clicking on the row.</p>
        </section>
        <section className="options">
            <div className="search-input-container">
                <input className="search-input"
                       placeholder={I18n.t("changeRequests.searchPlaceHolder")}
                       type="text"
                       onChange={this.search}
                       value={query}/>
                <i className="fa fa-search"/>
            </div>
            <a className={`clipboard-copy button green ${copiedToClipboardClassName}`}
               onClick={this.copyToClipboard}>
                {I18n.t("clipboard.copy")}<i className="fa fa-clone"></i>
            </a>
        </section>
        <Select onChange={this.changeStatus}
                options={["all", "prodaccepted", "testaccepted"]
                    .map(s => ({value: s, label: I18n.t(`metadata.${s}`)}))}
                value={status}
                isSearchable={false}
                className="status-select"/>
        {this.renderSearchResultsTable(changeRequests, status)}
        {this.renderSearchResultsTablePrintable(changeRequests)}
    </div>;

    renderNoResults = () => <section className="explanation">
        <p>There are no outstanding change requests.</p>
    </section>;

    render() {
        const {
            changeRequests,
            filteredChangeRequests,
            loaded,
            copiedToClipboardClassName,
            status,
            query
        } = this.state;
        if (!loaded) {
            return null;
        }
        const showResults = changeRequests.length > 0 && loaded;
        const showNoResults = changeRequests.length === 0 && loaded;
        return (
            <div className="support">
                {showResults && this.renderResults(filteredChangeRequests, copiedToClipboardClassName, status, query)}
                {showNoResults && this.renderNoResults()}
            </div>
        );
    }
}

export default withRouterHooks(ChangeRequests)