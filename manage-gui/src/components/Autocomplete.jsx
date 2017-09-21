import React from "react";
import I18n from "i18n-js";
import PropTypes from "prop-types";
import scrollIntoView from "scroll-into-view";
import {isEmpty} from "../utils/Utils";

import CheckBox from "./CheckBox";
import "./Autocomplete.css";

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

    render() {
        const {suggestions, query, selected, itemSelected} = this.props;
        const showSuggestions = (suggestions && suggestions.length > 0);
        return (
            <section className="metadata-autocomplete">
                {!showSuggestions &&
                <div className="no-results">{I18n.t("metadata_autocomplete.no_results")}</div>
                }
                {showSuggestions && <table className="result">
                    <thead>
                    <tr>
                        <th className="name">{I18n.t("metadata_autocomplete.name")}</th>
                        <th className="state">{I18n.t("metadata_autocomplete.state")}</th>
                        <th className="entity_id">{I18n.t("metadata_autocomplete.entity_id")}</th>
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
                                    <td className="state">
                                        <CheckBox name="state" value={item.data.state === "prodaccepted"} onChange={() => this} readOnly={true}/>
                                    </td>
                                    <td>{this.item(item.data.entityid, query)}</td>
                                </tr>
                            )
                        )}
                    </tbody>
                </table>}
            </section>
        );
    }

}

Autocomplete.propTypes = {
    suggestions: PropTypes.array.isRequired,
    query: PropTypes.string.isRequired,
    selected: PropTypes.number.isRequired,
    itemSelected: PropTypes.func.isRequired
};


