import React from "react";
import I18n from "i18n-js";
import PropTypes from "prop-types";
import scrollIntoView from "scroll-into-view";
import {isEmpty} from "../utils/Utils";

import CheckBox from "./CheckBox";
import "./Autocomplete.css";
import NotesTooltip from "./NotesTooltip";

export default class Autocomplete extends React.PureComponent {

    componentDidUpdate(prevProps) {
        if (this.selectedRow && prevProps.selected !== this.props.selectedRow) {
            scrollIntoView(this.selectedRow);
        }
    }

    item = (value, query) => {
        if (isEmpty(value)) {
            return <span></span>;
        }
        const nameToLower = value.toLowerCase();
        const indexOf = nameToLower.indexOf(query.toLowerCase());
        if (indexOf < 0) {
            return <span>{value}</span>;
        }
        const first = value.substring(0, indexOf);
        const middle = value.substring(indexOf, indexOf + query.length);
        const last = value.substring(indexOf + query.length);
        return <span>{first}<span className="matched">{middle}</span>{last}</span>;
    };

    renderAlternatives = (alternatives, selected, itemSelected, query, moreAlternativesToShow) => {
        if (isEmpty(alternatives)) {
            return <div className="results-info">
                <p>{I18n.t("metadata_autocomplete.no_results")}</p>
            </div>;
        }
        const msg = moreAlternativesToShow ? "no_results_alternatives_limited" : "no_results_alternatives";
        return (
            <div>
                <div className="results-info">
                    <p>{I18n.t(`metadata_autocomplete.${msg}`)}</p>

                </div>
                {this.getTable(alternatives, selected, itemSelected, query)}
            </div>);
    };

    render() {
        const {
            suggestions, query, selected, itemSelected, moreToShow, alternatives,
            moreAlternativesToShow
        } = this.props;
        const showSuggestions = (suggestions && suggestions.length > 0);
        return (
            <section className="metadata-autocomplete">
                {!showSuggestions && this.renderAlternatives(alternatives, selected, itemSelected, query, moreAlternativesToShow)}
                {showSuggestions &&
                <div>
                    {moreToShow && <div className="results-info">
                        <p>{I18n.t("metadata_autocomplete.results_limited")}</p>
                    </div>}
                    {this.getTable(suggestions, selected, itemSelected, query)}
                </div>}
            </section>
        );
    }

    getTable(suggestions, selected, itemSelected, query) {
        return <table className="result">
            <thead>
            <tr>
                <th className="name">{I18n.t("metadata_autocomplete.name")}</th>
                <th className="type">{I18n.t("metadata_autocomplete.type")}</th>
                <th className="state">{I18n.t("metadata_autocomplete.state")}</th>
                <th className="entity_id">{I18n.t("metadata_autocomplete.entity_id")}</th>
                <th className="info">{I18n.t("metadata_autocomplete.notes")}</th>
                <th className="link">{I18n.t("metadata_autocomplete.link")}</th>
            </tr>
            </thead>
            <tbody>
            {suggestions
                .map((item, index) => (
                        <tr key={index}
                            className={selected === index ? "active" : ""}
                            onClick={() => itemSelected(item)}
                            ref={ref => {
                                if (selected === index) {
                                    this.selectedRow = ref;
                                }
                            }}>
                            <td>{this.item(item.data.metaDataFields["name:en"] || item.data.metaDataFields["name:nl"], query)}</td>
                            <td>{item.type}</td>
                            <td className="state">
                                <CheckBox name="state" value={item.data.state === "prodaccepted"}
                                          onChange={() => this} readOnly={true}/>
                            </td>
                            <td>{this.item(item.data.entityid, query)}</td>
                            <td className="info">
                                {isEmpty(item.data.notes) ? <span></span> :
                                    <NotesTooltip identifier={item.data.entityid} notes={item.data.notes}/>}
                            </td>
                            <td className="link"><a href={`/metadata/${item.type}/${item["_id"]}`} target="_blank"
                                                    onClick={e => e.stopPropagation()}>
                                <i className="fa fa-external-link"></i>
                            </a></td>
                        </tr>
                    )
                )}
            </tbody>
        </table>;
    }
}

Autocomplete.propTypes = {
    suggestions: PropTypes.array.isRequired,
    alternatives: PropTypes.array,
    query: PropTypes.string.isRequired,
    selected: PropTypes.number.isRequired,
    itemSelected: PropTypes.func.isRequired,
    moreToShow: PropTypes.bool,
    moreAlternativesToShow: PropTypes.bool,
};


