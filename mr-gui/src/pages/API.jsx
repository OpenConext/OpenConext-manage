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

export default class Playground extends React.PureComponent {

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

    renderSearch = () => {
        const {configuration} = this.props;
        const {selectedType, searchAttributes, searchResults} = this.state;
        const conf = configuration.find(conf => conf.title === selectedType);
        const enabled = Object.keys(searchAttributes).length > 0;
        const searchHeaders = ["status", "name", "entityid"];
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
                {enabled &&
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
                </table>
                }
                <SelectNewMetaDataField configuration={conf} onChange={this.addSearchKey}
                                        metaDataFields={searchAttributes} placeholder={"Search and add metadata keys"}/>

                <a className="reset button grey" onClick={this.reset}>Reset<i className="fa fa-times"></i></a>
                <a className={`button ${enabled ? "green" : "disabled grey"}`} onClick={this.doSearch}>Search<i
                    className="fa fa-search-plus"></i></a>

                {hasNoResults && <h2>{I18n.t("playground.no_results")}</h2>}
                {(searchResults && !hasNoResults) && <table className="search-results">
                    <thead>
                    {searchHeaders.map(header => <th key={header}
                                                     className={header}>{I18n.t(`playground.headers.${header}`)}</th>)}
                    </thead>
                    <tbody>
                    {searchResults.map(entity => <tr key={entity.data.entityid}>
                        <td className="state">{I18n.t(`metadata.${entity.data.state}`)}</td>
                        <td className="name">{entity.data.metaDataFields["name:en"] || entity.data.metaDataFields["name:nl"]}</td>
                        <td className="entityId">
                            <Link to={`/metadata/${selectedType}/${entity["_id"]}`}
                                  target="_blank">{entity.data.entityid}</Link>
                        </td>

                    </tr>)}
                    </tbody>

                </table>}
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

Playground.propTypes = {
    history: PropTypes.object.isRequired,
    configuration: PropTypes.array.isRequired
};

