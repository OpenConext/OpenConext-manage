import React from "react";
import I18n from "i18n-js";
import PropTypes from "prop-types";
import {Link} from "react-router-dom";
import {ping, search} from "../api";
import {isEmpty, stop} from "../utils/Utils";
import SelectMetaDataType from "../components/metadata/SelectMetaDataType";
import "./API.css";
import "react-pretty-json/assets/json-view.css";
import SelectNewMetaDataField from "../components/metadata/SelectNewMetaDataField";

export default class API extends React.PureComponent {

    constructor(props) {
        super(props);
        this.state = {
            selectedType: "saml20_sp",
            searchAttributes: {},
            searchResults: undefined,
            newMetaDataFieldKey: null
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
        const newSearchAttributes = {...this.state.searchAttributes};
        newSearchAttributes[key] = e.target.value;
        this.setState({searchAttributes: newSearchAttributes});
    };

    deleteSearchField = key => e => {
        const newSearchAttributes = {...this.state.searchAttributes};
        delete newSearchAttributes[key];
        this.setState({searchAttributes: newSearchAttributes});

    };
    doSearch = e => {
        stop(e);
        const {selectedType, searchAttributes} = this.state;
        const keys = Object.keys(searchAttributes);
        const enabled = keys.length > 0;
        if (enabled) {
            const metaDataSearch = {};
            keys.forEach(key => {
                metaDataSearch[`metaDataFields.${key}`] = searchAttributes[key];
            });
            metaDataSearch.REQUESTED_ATTRIBUTES = Object.keys(metaDataSearch);
            search(metaDataSearch, selectedType).then(json => this.setState({searchResults: json}));
        }

    };

    reset = e => {
        stop(e);
        this.setState({searchAttributes: {}, searchResults: undefined});
    };

    newMetaDataFieldRendered = (ref, autoFocus) => {
        if (autoFocus) {
            this.newMetaDataField = ref;
        }
    };

    renderSearchTable = (searchAttributes) =>
        <table className="metadata-search-table">
            <tbody>
            {Object.keys(searchAttributes).map(key =>
                <tr key={key}>
                    <td className="key">{key}</td>
                    <td className="value">
                        <input
                            ref={ref => this.newMetaDataFieldRendered(ref, this.state.newMetaDataFieldKey === key)}
                            type="text" value={searchAttributes[key]}
                            onChange={this.changeSearchValue(key)}/>
                    </td>
                    <td className="trash">
                        <span onClick={this.deleteSearchField(key)}><i className="fa fa-trash-o"></i></span>
                    </td>
                </tr>
            )}
            </tbody>
        </table>;

    renderSearchResultsTable = (searchResults, selectedType, searchAttributes) => {
        const searchHeaders = ["status", "name", "entityid"].concat(Object.keys(searchAttributes));
        return (
            <table className="search-results">
                <thead>
                <tr>
                    {searchHeaders.map((header, index) =>
                        <th key={header}
                            className={index < 3 ? header: "extra"}>{index < 3 ? I18n.t(`playground.headers.${header}`) : header}</th>)}

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
                    {Object.keys(searchAttributes).map(attr =>
                        <td key={attr}>{entity.data.metaDataFields[attr]}</td>)}

                </tr>)}
                </tbody>

            </table>);
    };


    renderSearch = () => {
        const {configuration} = this.props;
        const {selectedType, searchAttributes, searchResults} = this.state;
        const conf = configuration.find(conf => conf.title === selectedType);
        const enabled = Object.keys(searchAttributes).length > 0;
        const hasNoResults = searchResults && searchResults.length === 0;
        return (
            <section className="extended-search">
                <p>Select a Metadata type and metadata fields. The query will AND the different inputs.
                    Wildcards like <span className="code">.*surf.*</span> are translated to a regular expression search.
                    Specify booleans with <span className="code">0</span> or <span className="code">1</span> and
                    leave the value empty for a <span className="code">does not exists</span> query.</p>
                <SelectMetaDataType onChange={value => this.setState({selectedType: value})}
                                    configuration={configuration}
                                    state={selectedType}/>
                {enabled && this.renderSearchTable(searchAttributes)}
                <SelectNewMetaDataField configuration={conf} onChange={this.addSearchKey}
                                        metaDataFields={searchAttributes} placeholder={"Search and add metadata keys"}/>

                <a className="reset button grey" onClick={this.reset}>Reset<i className="fa fa-times"></i></a>
                <a className={`button ${enabled ? "green" : "disabled grey"}`} onClick={this.doSearch}>Search<i
                    className="fa fa-search-plus"></i></a>

                {hasNoResults && <h2>{I18n.t("playground.no_results")}</h2>}
                {(searchResults && !hasNoResults) && this.renderSearchResultsTable(searchResults, selectedType, searchAttributes)}
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

