import React from "react";
import I18n from "i18n-js";
import PropTypes from "prop-types";
import {Link} from "react-router-dom";
import {ping, search, validation} from "../api";
import {copyToClip, isEmpty, stop} from "../utils/Utils";
import SelectMetaDataType from "../components/metadata/SelectMetaDataType";
import "./API.css";
import "react-pretty-json/assets/json-view.css";
import SelectNewMetaDataField from "../components/metadata/SelectNewMetaDataField";
import debounce from "lodash.debounce";
import Select from "react-select";
import "react-select/dist/react-select.css";

export default class API extends React.PureComponent {

    constructor(props) {
        super(props);
        this.state = {
            selectedType: "saml20_sp",
            searchAttributes: {},
            errorAttributes: {},
            searchResults: undefined,
            newMetaDataFieldKey: null,
            status: "all",
            copiedToClipboardClassName: ""
        };
    }

    componentDidMount() {
        ping();
    }

    componentDidUpdate = () => {
        const newMetaDataFieldKey = this.state.newMetaDataFieldKey;
        if (!isEmpty(newMetaDataFieldKey) && !isEmpty(this.newMetaDataField)) {
            this.newMetaDataField.focus();
            this.newMetaDataField = null;
            this.setState({newMetaDataFieldKey: null})
        }
    };


    addSearchKey = key => {
        const newSearchAttributes = {...this.state.searchAttributes};
        newSearchAttributes[key] = "";
        this.setState({searchAttributes: newSearchAttributes, newMetaDataFieldKey: key});
    };

    changeSearchValue = key => e => {
        const previousValue = this.state.searchAttributes[key];
        const newSearchAttributes = {...this.state.searchAttributes};
        const value = e.target.value;
        newSearchAttributes[key] = value;
        this.setState({searchAttributes: newSearchAttributes});

        if ((previousValue && previousValue.indexOf("*") > -1) || (value && value.indexOf("*") > -1)) {
            this.delayedPatternValidation(key, value);
        }
    };

    delayedPatternValidation = debounce((key, value) =>
        validation("pattern", value).then(result => {
            const newErrorAttributes = {...this.state.errorAttributes};
            newErrorAttributes[key] = !result;
            this.setState({errorAttributes: newErrorAttributes})
        }), 250);


    deleteSearchField = key => e => {
        const newSearchAttributes = {...this.state.searchAttributes};
        delete newSearchAttributes[key];
        this.setState({searchAttributes: newSearchAttributes});

    };

    changeStatus = option => this.setState({status: option ? option.value : null});

    isValidInput = (errorAttributes) => {
        const keys = Object.keys(errorAttributes);
        const invalid = keys.length > 0 && keys.some(key => errorAttributes[key]);
        return !invalid;
    };

    doSearch = e => {
        stop(e);
        const {selectedType, searchAttributes, errorAttributes} = this.state;
        if (this.isValidInput(errorAttributes)) {
            const metaDataSearch = {};
            const keys = Object.keys(searchAttributes);
            keys.forEach(key => {
                metaDataSearch[`metaDataFields.${key}`] = searchAttributes[key];
            });
            if (!isEmpty(searchAttributes)) {
                metaDataSearch.REQUESTED_ATTRIBUTES = Object.keys(metaDataSearch);
            }
            search(metaDataSearch, selectedType)
                .then(json => this.setState({searchResults: json}));
        }

    };

    reset = e => {
        stop(e);
        this.setState({searchAttributes: {}, errorAttributes: {}, searchResults: undefined});
    };

    copyToClipboard = e => {
        stop(e);
        if (!isEmpty(this.state.searchResults)) {
            copyToClip("search-results-printable");
            this.setState({copiedToClipboardClassName: "copied"});
            setTimeout(() => this.setState({copiedToClipboardClassName: ""}), 5000);
        }
    };

    newMetaDataFieldRendered = (ref, autoFocus) => {
        if (autoFocus) {
            this.newMetaDataField = ref;
        }
    };

    renderSearchTable = (searchAttributes, errorAttributes) => {
        return (
            <table className="metadata-search-table">
                <tbody>
                {Object.keys(searchAttributes).map(key => {
                        const error = errorAttributes[key];
                        return (
                            <tr key={key}>
                                <td className="key">{key}</td>
                                <td className="value">
                                    <input
                                        ref={ref => this.newMetaDataFieldRendered(ref, this.state.newMetaDataFieldKey === key)}
                                        type="text" value={searchAttributes[key]}
                                        onChange={this.changeSearchValue(key)}/>
                                    {error && <span className="error">{I18n.t("playground.error")}</span>}
                                </td>
                                <td className="trash">
                                    <span onClick={this.deleteSearchField(key)}><i className="fa fa-trash-o"></i></span>
                                </td>
                            </tr>
                        );
                    }
                )}
                </tbody>
            </table>
        );
    };

    renderSearchResultsTable = (searchResults, selectedType, searchAttributes, status) => {
        const searchHeaders = ["status", "name", "entityid", "notes"].concat(Object.keys(searchAttributes));
        searchResults = status === "all" ? searchResults : searchResults.filter(entity => entity.data.state === status);
        return (
            <section id={"search-results-printable"}>
                <table className="search-results">
                    <thead>
                    <tr>
                        {searchHeaders.map((header, index) =>
                            <th key={header}
                                className={index < 4 ? header : "extra"}>{index < 4 ? I18n.t(`playground.headers.${header}`) : header}</th>)}

                    </tr>
                    </thead>
                    <tbody>
                    {searchResults.map(entity => <tr key={entity.data.entityid}>
                        <td className="state">{I18n.t(`metadata.${entity.data.state}`)}</td>
                        <td className="name">
                            <Link to={`/metadata/${selectedType}/${entity["_id"]}`}
                                  target="_blank">{entity.data.metaDataFields["name:en"] || entity.data.metaDataFields["name:nl"]}</Link>
                        </td>
                        <td className="entityId">{entity.data.entityid}</td>
                        <td className="notes">
                            {isEmpty(entity.data.notes) ? <span></span> : <i className="fa fa-info"></i>}
                        </td>
                        {Object.keys(searchAttributes).map(attr =>
                            <td key={attr}>{entity.data.metaDataFields[attr]}</td>)}

                    </tr>)}
                    </tbody>

                </table>
            </section>);
    };


    renderSearch = () => {
        const {configuration} = this.props;
        const {selectedType, searchAttributes, errorAttributes, searchResults, status, copiedToClipboardClassName} = this.state;
        const conf = configuration.find(conf => conf.title === selectedType);
        const hasSearchAttributes = Object.keys(searchAttributes).length > 0;
        const valid = this.isValidInput(errorAttributes);

        const hasNoResults = searchResults && searchResults.length === 0;
        const showResults = searchResults && !hasNoResults;
        return (
            <section className="extended-search">
                <p>Select a Metadata type and metadata fields. The query will AND the different inputs.
                    Wildcards like <span className="code">.*surf.*</span> are translated to a regular expression search.
                    Specify booleans with <span className="code">0</span> or <span className="code">1</span> and
                    leave the value empty for a <span className="code">does not exists</span> query.</p>
                <SelectMetaDataType onChange={value => this.setState({selectedType: value})}
                                    configuration={configuration}
                                    state={selectedType}/>
                {hasSearchAttributes && this.renderSearchTable(searchAttributes, errorAttributes)}
                <SelectNewMetaDataField configuration={conf} onChange={this.addSearchKey}
                                        metaDataFields={searchAttributes} placeholder={"Search and add metadata keys"}/>
                <section className="options">
                    <a className="reset button" onClick={this.reset}>Reset<i className="fa fa-times"></i></a>
                    <a className={`button ${valid ? "green" : "disabled grey"}`} onClick={this.doSearch}>Search<i
                        className="fa fa-search-plus"></i></a>
                    <a className={`clipboard-copy button ${showResults ? "green" : "disabled grey"} ${copiedToClipboardClassName}`}
                          onClick={this.copyToClipboard}>
                        {I18n.t("clipboard.copy")}<i className="fa fa-clone"></i>
                    </a>
                </section>
                {hasNoResults && <h2>{I18n.t("playground.no_results")}</h2>}
                {showResults && <Select onChange={this.changeStatus}
                                        options={["all", "prodaccepted", "testaccepted"]
                                            .map(s => ({value: s, label: I18n.t(`metadata.${s}`)}))}
                                        value={status}
                                        className="status-select"/>}
                {showResults && this.renderSearchResultsTable(searchResults, selectedType, searchAttributes, status)}

            </section>
        );
    };

    render() {
        return (
            <div className="api">
                {this.renderSearch()}
            </div>
        );
    }
}

API.propTypes = {
    history: PropTypes.object.isRequired,
    configuration: PropTypes.array.isRequired
};

