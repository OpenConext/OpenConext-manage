import React from "react";
import I18n from "i18n-js";
import PropTypes from "prop-types";
import scrollIntoView from "scroll-into-view";

import {isEmpty} from "../utils/Utils";

export default class Autocomplete extends React.PureComponent {

    componentDidUpdate(prevProps) {
        if (this.selectedRow && prevProps.selected !== this.props.selectedRow) {
            scrollIntoView(this.selectedRow);
        }
    }

    itemName = (item, query) => {
        const name = item.name;
        const nameToLower = name.toLowerCase();
        const indexOf = nameToLower.indexOf(query.toLowerCase());
        const first = name.substring(0, indexOf);
        const middle = name.substring(indexOf, indexOf + query.length);
        const last = name.substring(indexOf + query.length);
        return  <span>{first}<span className="matched">{middle}</span>{last}</span>;
    };

    itemDescription = (item, index) => {
        const description = item.description;
        if (isEmpty(description)) {
            return "";
        }
        if (description.length > 45) {
            const id = `description${index}`;
            return (
                <span data-for={id} data-tip>
                {`${description.substring(0, Math.min(description.substring(30).indexOf(" ") + 30, 40))}...`}
                    <i className="fa fa-info-circle"></i>
            </span>
            );
        }
        return description;
    };

    render() {
        const {suggestions, query, selected, itemSelected} = this.props;
        const showSuggestions = (suggestions && suggestions.length > 0);
        return (
            <section className="metadata-autocomplete">
                {!showSuggestions &&
                <div className="no-results">{I18n.t("auto_complete.no_results")}</div>
                }
                {showSuggestions && <table className="result">
                    <thead>
                    <tr>
                        <th className="name">{I18n.t("metadata_autocomplete.name")}</th>
                        <th className="description">{I18n.t("metadata.description")}</th>
                    </tr>
                    </thead>
                    <tbody>
                    {suggestions
                        .filter(item => item.name.toLowerCase().indexOf(query.toLowerCase()) > -1)
                        .map((item, index) => (
                                <tr key={index}
                                    className={selected === index ? "active" : ""}
                                    onClick={() => itemSelected(item)}
                                    ref={ref => {
                                        if (selected === index) {
                                            this.selectedRow = ref;
                                        }
                                    }}>
                                    <td>{this.itemName(item, query)}</td>
                                    <td>{this.itemDescription(item, index)}</td>
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


