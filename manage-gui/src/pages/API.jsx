import React from "react";
import I18n from "i18n-js";
import PropTypes from "prop-types";
import {Link} from "react-router-dom";
import Papaparse from "papaparse";
import {ping, rawSearch, search, validation} from "../api";
import {copyToClip, isEmpty, stop} from "../utils/Utils";
import "./API.scss";
import debounce from "lodash.debounce";
import {CheckBox, NotesTooltip, Select} from '../components'
import {SelectMetaDataType, SelectNewEntityAttribute, SelectNewMetaDataField} from "../components/metadata"
import {getNameForLanguage} from "../utils/Language";

const papaparseConfig = {
    quotes: true,
    escapeChar: '\\'
}

export default class API extends React.PureComponent {

    constructor(props) {
        super(props);
        this.state = {
            selectedType: "saml20_sp",
            searchAttributes: {},
            globalSearchAttributes: {},
            errorAttributes: {},
            globalErrorAttributes: {},
            logicalOperatorIsAnd: true,
            fullTextSearch: "",
            searchResults: undefined,
            newMetaDataFieldKey: null,
            newGlobalAttributeKey: null,
            status: "all",
            copiedToClipboardClassName: "",
            copiedToClipboardJSONClassName: ""
        };
    }

    componentDidMount() {
        ping();
    }

    componentDidUpdate = () => {
        const {newMetaDataFieldKey, newGlobalAttributeKey} = this.state;
        if (!isEmpty(newMetaDataFieldKey) && !isEmpty(this.newMetaDataField)) {
            this.newMetaDataField.focus();
            this.newMetaDataField = null;
            this.setState({newMetaDataFieldKey: null})
        }
        if (!isEmpty(newGlobalAttributeKey) && !isEmpty(this.newGlobalAttributeField)) {
            this.newGlobalAttributeField.focus();
            this.newGlobalAttributeField = null;
            this.setState({newGlobalAttributeKey: null})
        }
    };


    addSearchKey = key => {
        const newSearchAttributes = {...this.state.searchAttributes};
        newSearchAttributes[key] = "";
        this.setState({
            searchAttributes: newSearchAttributes,
            newMetaDataFieldKey: key,
            newGlobalAttributeKey: undefined
        });
    };

    addGlobalSearchKey = key => {
        const newGlobalSearchAttributes = {...this.state.globalSearchAttributes};
        newGlobalSearchAttributes[key] = "";
        this.setState({
            globalSearchAttributes: newGlobalSearchAttributes,
            newMetaDataFieldKey: undefined,
            newGlobalAttributeKey: key
        });
    };

    changeSearchValue = key => e => {
        const previousValue = this.state.searchAttributes[key];
        const newSearchAttributes = {...this.state.searchAttributes};
        const value = e.target.value;
        newSearchAttributes[key] = value;
        this.setState({searchAttributes: newSearchAttributes});

        if ((previousValue && previousValue.indexOf("*") > -1) || (value && value.indexOf("*") > -1)) {
            this.delayedPatternValidation(key, value, "errorAttributes");
        }
    };

    changeGlobalSearchValue = key => e => {
        const previousValue = this.state.globalSearchAttributes[key];
        const newGlobalSearchAttributes = {...this.state.globalSearchAttributes};
        const value = e.target.value;
        newGlobalSearchAttributes[key] = value;
        this.setState({globalSearchAttributes: newGlobalSearchAttributes});

        if ((previousValue && previousValue.indexOf("*") > -1) || (value && value.indexOf("*") > -1)) {
            this.delayedPatternValidation(key, value, "globalErrorAttributes");
        }
    };

    delayedPatternValidation = debounce((key, value, name) =>
        validation("pattern", value).then(result => {
            const newErrorAttributes = {...this.state[name]};
            newErrorAttributes[key] = !result;
            const newState = {};
            newState[name] = newErrorAttributes;
            this.setState(newState);
        }), 250);


    deleteSearchField = key => e => {
        const newSearchAttributes = {...this.state.searchAttributes};
        delete newSearchAttributes[key];
        this.setState({searchAttributes: newSearchAttributes, searchResults: undefined});
    };

    deleteGlobalSearchField = key => e => {
        const newGlobalSearchAttributes = {...this.state.globalSearchAttributes};
        delete newGlobalSearchAttributes[key];
        this.setState({globalSearchAttributes: newGlobalSearchAttributes, searchResults: undefined});
    };

    changeStatus = option => this.setState({status: option ? option.value : null});

    isValidInput = (errorAttributes) => {
        const keys = Object.keys(errorAttributes);
        const invalid = keys.length > 0 && keys.some(key => errorAttributes[key]);
        return !invalid;
    };

    doSearch = e => {
        stop(e);
        const {
            selectedType,
            searchAttributes,
            globalSearchAttributes,
            errorAttributes,
            logicalOperatorIsAnd,
            fullTextSearch
        } = this.state;
        if (!isEmpty(fullTextSearch.trim())) {
            const terms = logicalOperatorIsAnd ? fullTextSearch.split(" ").map(part => `\\"${part.trim()}\\"`).join(" ") : fullTextSearch;
            const query = `{ $text: { $search: "${terms}" } }`;
            rawSearch(query, selectedType).then(json => this.setState({searchResults: json}));
        } else if (this.isValidInput(errorAttributes)) {
            const metaDataSearch = {};
            const keys = Object.keys(searchAttributes);
            keys.forEach(key => metaDataSearch[`metaDataFields.${key}`] = searchAttributes[key]);

            const globalKeys = Object.keys(globalSearchAttributes);
            globalKeys.forEach(key => {
                let val = globalSearchAttributes[key];
                if (val.toLowerCase() === "true") {
                    val = true;
                } else if (val.toLowerCase() === "false") {
                    val = false;
                }
                metaDataSearch[`${key}`] = val;
            });

            if (!isEmpty(searchAttributes) || !isEmpty(globalSearchAttributes)) {
                metaDataSearch.REQUESTED_ATTRIBUTES = Object.keys(metaDataSearch);
            }
            metaDataSearch.LOGICAL_OPERATOR_IS_AND = logicalOperatorIsAnd;
            search(metaDataSearch, selectedType)
                .then(json => this.setState({searchResults: json}));
        }

    };

    reset = e => {
        stop(e);
        this.setState({
            searchAttributes: {},
            globalSearchAttributes: {},
            errorAttributes: {},
            globalErrorAttributes: {},
            fullTextSearch: "",
            searchResults: undefined,
            logicalOperatorIsAnd: true,
            newMetaDataFieldKey: null,
            newGlobalAttributeKey: null,
            status: "all"
        });
    };

    copyToClipboard = e => {
        stop(e);
        if (!isEmpty(this.state.searchResults)) {
            copyToClip("search-results-printable");
            this.setState({copiedToClipboardClassName: "copied"});
            setTimeout(() => this.setState({copiedToClipboardClassName: ""}), 5000);
        }
    };

    copyToClipboardJSON = e => {
        stop(e);
        if (!isEmpty(this.state.searchResults)) {
            copyToClip("search-results-printable-json");
            this.setState({copiedToClipboardJSONClassName: "copied"});
            setTimeout(() => this.setState({copiedToClipboardJSONClassName: ""}), 5000);
        }
    };

    newMetaDataFieldRendered = (ref, autoFocus) => {
        if (autoFocus) {
            this.newMetaDataField = ref;
        }
    };

    newGlobalFieldRendered = (ref, autoFocus) => {
        if (autoFocus) {
            this.newGlobalAttributeField = ref;
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
                                        onKeyDown={e => e.key === "Enter" ? this.doSearch(e) : false}
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

    renderGlobalSearchTable = (globalSearchAttributes, globalErrorAttributes) => {
        return (
            <table className="metadata-search-table">
                <tbody>
                {Object.keys(globalSearchAttributes).map(key => {
                        const error = globalErrorAttributes[key];
                        return (
                            <tr key={key}>
                                <td className="key">{key}</td>
                                <td className="value">
                                    <input
                                        ref={ref => this.newGlobalFieldRendered(ref, this.state.newGlobalAttributeKey === key)}
                                        type="text" value={globalSearchAttributes[key]}
                                        onChange={this.changeGlobalSearchValue(key)}/>
                                    {error && <span className="error">{I18n.t("playground.error")}</span>}
                                </td>
                                <td className="trash">
                                    <span onClick={this.deleteGlobalSearchField(key)}><i
                                        className="fa fa-trash-o"></i></span>
                                </td>
                            </tr>
                        );
                    }
                )}
                </tbody>
            </table>
        );
    };

    renderSearchResultsTable = (searchResults, selectedType, searchAttributes, globalSearchAttributes, status, fullTextSearch) => {
        const searchHeaders = ["count", "status", "name", "entityid", "notes"]
            .concat(Object.keys(searchAttributes))
            .concat(Object.keys(globalSearchAttributes));
        searchResults = status === "all" ? searchResults : searchResults.filter(entity => entity.data.state === status);
        return (
            <section>
                <table className="search-results">
                    <thead>
                    <tr>
                        {searchHeaders.map((header, index) =>
                            <th key={`${header}_${index}`}
                                className={index < 4 ? header : "extra"}>{index < 4 ? I18n.t(`playground.headers.${header}`) : header}</th>)}
                    </tr>
                    </thead>
                    <tbody>
                    {searchResults.map((entity, index) =>
                        <tr key={`${entity.data.entityid}_${index}`}>
                            <td className="count">{index + 1}</td>
                            <td className="state">{I18n.t(`metadata.${entity.data.state}`)}</td>
                            <td className="name">
                            <Link
                                to={`/metadata/${selectedType}/${isEmpty(fullTextSearch) ? entity["_id"] : entity.id}`}
                                target="_blank">{getNameForLanguage(entity.data.metaDataFields)}</Link>
                            </td>
                            <td className="entityId">{entity.data.entityid}</td>
                            <td className="notes">
                                {isEmpty(entity.data.notes) ? <span></span> :
                                    <NotesTooltip identifier={entity.data.entityid} notes={entity.data.notes}/>}
                            </td>
                            {Object.keys(searchAttributes).map(attr => {
                                return <td key={attr}>{"" + entity.data.metaDataFields[attr]}</td>
                            })}
                            {Object.keys(globalSearchAttributes).map(attr => {
                                    //split by dot results in too many parts for
                                    // "arp.attributes.urn:mace:terena.org:attribute-def:schacHomeOrganization"

                                    const arpAttribute = attr.startsWith("arp.attributes");
                                    if (arpAttribute) {
                                        attr = "arp.attributes." + attr.substring("arp.attributes.".length).replace(/\./g, "@")
                                    }
                                    let parts = attr.split(".");
                                    if (arpAttribute) {
                                        parts = parts.map(p => p.replace(/@/g, "."));
                                    }
                                    let last = parts.pop();
                                    let ref = entity.data;
                                    parts.forEach(part => ref = ref ? ref[part] : {});
                                    if (ref) {
                                        const val = Array.isArray(ref) ? ref.map(x => x[last]) : ref[last];
                                        return <td key={attr}>{JSON.stringify(val)}</td>
                                    }
                                    return <td key={attr}></td>
                                }
                            )}
                        </tr>)}
                    </tbody>

                </table>
            </section>);
    };


    renderSearchResultsTablePrintable = (searchResults) => {
        // Todo move to utils and add unit tests
        const objectToKeyValue = (inputEntries, keyPrefix) =>
            inputEntries.reduce((acc, curr) => {
                const [currKey, currValue] = curr;

                if (Array.isArray(currValue)) {
                    console.error(`Arrays are currently not supported, skips processing the value of "${currKey}"`);
                    acc.push([currKey, currValue]);
                    return acc;
                }

                if (typeof currValue === "object") {
                    const nestedInputEntries = Object.entries(currValue);
                    acc.push(...objectToKeyValue(nestedInputEntries, currKey));
                    return acc;
                }
                const theKey = keyPrefix ? `${keyPrefix}.${currKey}` : currKey
                acc.push([theKey, currValue]);
                return acc;
            }, [])

        const flattenedSearchResults = searchResults.map((row) => Object.fromEntries(objectToKeyValue(Object.entries(row))));
        const csvResult = Papaparse.unparse({
            fields: Object.keys(flattenedSearchResults[0]),
            data: flattenedSearchResults.map((row) => Object.values(row))
        }, papaparseConfig);

        return (
            <section id={"search-results-printable"}>
                {csvResult}
            </section>
        );
    }

    renderSearchResultsJSONPrintable = (searchResults) => {
        return (
            <section id={"search-results-printable-json"}>
                {JSON.stringify(searchResults, null, 4)}
            </section>
        );
    }

    renderSearch = () => {
        const {configuration} = this.props;
        const {
            selectedType, searchAttributes, errorAttributes, searchResults, status, copiedToClipboardClassName,
            copiedToClipboardJSONClassName, globalSearchAttributes, globalErrorAttributes, logicalOperatorIsAnd,
            fullTextSearch
        } = this.state;
        const conf = configuration.find(conf => conf.title === selectedType);
        const hasSearchAttributes = Object.keys(searchAttributes).length > 0;
        const hasGlobalSearchAttributes = Object.keys(globalSearchAttributes).length > 0;
        const valid = this.isValidInput(errorAttributes);

        const hasNoResults = searchResults && searchResults.length === 0;
        const showResults = searchResults && !hasNoResults;
        return (
            <section className="extended-search">
                <p>Select a Metadata type and metadata fields. Wildcards like <span className="code">.*surf.*</span> are
                    translated to a regular expression search.
                    Specify booleans with <span className="code">true</span> or <span className="code">false</span> and
                    leave the value empty for a <span className="code">does not exists</span> query.</p>
                <SelectMetaDataType onChange={value => this.setState({selectedType: value})}
                                    configuration={configuration}
                                    state={selectedType}/>
                {hasSearchAttributes && this.renderSearchTable(searchAttributes, errorAttributes)}
                <SelectNewMetaDataField configuration={conf}
                                        onChange={this.addSearchKey}
                                        metaDataFields={searchAttributes}
                                        placeholder={"Search and add metadata keys"}/>
                <p>Add non-Metadata fields to search for. Type <span className="code">true</span> or <span
                    className="code">false</span> for boolean fields
                    including the <span className="code">arp.attributes</span>. You can use the <span
                        className="code">.*</span> wildcard for text fields.</p>
                <SelectNewEntityAttribute configuration={conf} onChange={this.addGlobalSearchKey}
                                          attributes={globalSearchAttributes}
                                          placeholder={"Search and add global attributes"}/>
                {hasGlobalSearchAttributes && this.renderGlobalSearchTable(globalSearchAttributes, globalErrorAttributes)}
                <p>Full text search to find all metadata containing any or all - depending on the logical operator -
                    space separated terms.</p>
                <input className="fullTextSearch" type="text" value={fullTextSearch}
                       onChange={e => this.setState({fullTextSearch: e.target.value})}
                       onKeyDown={e => e.key === "Enter" ? this.doSearch(e) : false}/>
                <CheckBox name="logicalOperatorIsAnd" value={logicalOperatorIsAnd}
                          info="Use the logical operater AND (instead of OR) for the different search criteria"
                          onChange={() => this.setState({logicalOperatorIsAnd: !this.state.logicalOperatorIsAnd})}/>
                <section className="options">
                    <a className="reset button"
                       onClick={this.reset}>Reset<i className="fa fa-times"></i>
                    </a>
                    <a className={`button ${valid ? "green" : "disabled grey"}`}
                       onClick={this.doSearch}>Search<i className="fa fa-search-plus"></i>
                    </a>
                    <a className={`clipboard-copy button ${showResults ? "green" : "disabled grey"} ${copiedToClipboardJSONClassName}`}
                       onClick={this.copyToClipboardJSON}>
                        {I18n.t("clipboard.copyAsJSON")}<i className="fa fa-clone"></i>
                    </a>
                    <a className={`clipboard-copy button ${showResults ? "green" : "disabled grey"} ${copiedToClipboardClassName}`}
                       onClick={this.copyToClipboard}>
                        {I18n.t("clipboard.copyAsCSV")}<i className="fa fa-clone"></i>
                    </a>
                </section>
                {hasNoResults && <h2>{I18n.t("playground.no_results")}</h2>}
                {showResults && <Select onChange={this.changeStatus}
                                        options={["all", "prodaccepted", "testaccepted"]
                                            .map(s => ({value: s, label: I18n.t(`metadata.${s}`)}))}
                                        value={status}
                                        isSearchable={false}
                                        className="status-select"/>}
                {showResults && this.renderSearchResultsTable(searchResults, selectedType, searchAttributes, globalSearchAttributes, status, fullTextSearch)}
                {showResults && this.renderSearchResultsTablePrintable(searchResults)}
                {showResults && this.renderSearchResultsJSONPrintable(searchResults)}

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
    configuration: PropTypes.array.isRequired
};
