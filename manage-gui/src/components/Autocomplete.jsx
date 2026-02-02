import React from "react";
import I18n from "i18n-js";
import PropTypes from "prop-types";
import scrollIntoView from "scroll-into-view";
import {isEmpty} from "../utils/Utils";

import CheckBox from "./CheckBox";
import "./Autocomplete.scss";
import NotesTooltip from "./NotesTooltip";
import {getNameForLanguage, getOrganisationForLanguage} from "../utils/Language";

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
            moreAlternativesToShow, type
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
                    {this.getTable(suggestions, selected, itemSelected, query, type)}
                </div>}
            </section>
        );
    }

    getTable(suggestions, selected, itemSelected, query, type) {
        const isOrganisation = type === "organisation";
        const isPolicy = type === "policy";
        return <table className="result">
            <thead>
            <tr>
                {!isOrganisation && <th className="count"></th>}
                <th className="name">{I18n.t("metadata_autocomplete.name")}</th>
                {!isPolicy && !isOrganisation && <th className="organization">{I18n.t("metadata_autocomplete.organization")}</th>}
                {isPolicy && <th className="organization">{I18n.t("metadata_autocomplete.policy")}</th>}
                {!isOrganisation && <th className="type">{I18n.t("metadata_autocomplete.type")}</th>}
                {!isOrganisation && <th className="state">{I18n.t("metadata_autocomplete.state")}</th>}
                {!isOrganisation && <th className="entity_id">{I18n.t("metadata_autocomplete.entity_id")}</th>}
                <th className="info">{I18n.t("metadata_autocomplete.notes")}</th>
                <th className="link">{I18n.t("metadata_autocomplete.link")}</th>
            </tr>
            </thead>
            <tbody>
            {suggestions
                .map((item, index) => {
                        switch (item.type) {
                            case "organisation":
                                return this.renderOrganisation(item, index, selected, itemSelected);
                            default:
                                return this.renderMetadata(item, index, selected, itemSelected, isPolicy, query);
                        }
                    }
                )}
            </tbody>
        </table>;
    }

    renderOrganisation(item, index, selected, itemSelected) {
        return (
            <tr key={index}
                className={selected === index ? "active" : ""}
                onClick={() => itemSelected(item)}
                ref={ref => {
                    if (selected === index) {
                        this.selectedRow = ref;
                    }
                }}>
                <td className="name">{item.data.name || ""}{item.data.kvkNumber ? ` (${item.data.kvkNumber})` : ""}</td>
                <td className="info">
                    {isEmpty(item.data.notes) ? <span></span> :
                        <NotesTooltip identifier={item["_id"]} notes={item.data.notes}/>}
                </td>
                <td className="link"><a href={`/metadata/${item.type}/${item["_id"]}`} target="_blank" rel="noopener noreferrer"
                                        onClick={e => e.stopPropagation()}>
                    <i className="fa fa-external-link"></i>
                </a></td>
            </tr>
        );
    };

    renderMetadata(item, index, selected, itemSelected, isPolicy, query) {
        return (
            <tr key={index}
                className={selected === index ? "active" : ""}
                onClick={() => itemSelected(item)}
                ref={ref => {
                    if (selected === index) {
                        this.selectedRow = ref;
                    }
                }}>
                <td className="count">{index + 1}</td>
                <td>
                    {!isPolicy && this.item(getNameForLanguage(item.data.metaDataFields), query)}
                    {isPolicy && this.item(item.data.name, query)}
                </td>
                <td>
                    {!isPolicy && this.item(getOrganisationForLanguage(item.data.metaDataFields), query)}
                    {isPolicy && I18n.t(`topBannerDetails.${item.data.type}`)}
                </td>
                <td>{item.type}</td>
                <td className="state">
                    <CheckBox name="state" value={item.data.state === "prodaccepted" || isPolicy}
                              onChange={() => this} readOnly={true}/>
                </td>
                <td>{this.item(item.data.entityid, query)}</td>
                <td className="info">
                    {isEmpty(item.data.notes) ? <span></span> :
                        <NotesTooltip identifier={item.data.entityid} notes={item.data.notes}/>}
                </td>
                <td className="link"><a href={`/metadata/${item.type}/${item["_id"]}`} target="_blank" rel="noopener noreferrer"
                                        onClick={e => e.stopPropagation()}>
                    <i className="fa fa-external-link"></i>
                </a></td>
            </tr>
        );
    };
}

Autocomplete.propTypes = {
    suggestions: PropTypes.array.isRequired,
    alternatives: PropTypes.array,
    query: PropTypes.string.isRequired,
    type: PropTypes.string,
    selected: PropTypes.number.isRequired,
    itemSelected: PropTypes.func.isRequired,
    moreToShow: PropTypes.bool,
    moreAlternativesToShow: PropTypes.bool,
};


