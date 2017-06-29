import React from "react";
import I18n from "i18n-js";
import PropTypes from "prop-types";
import debounce from "lodash.debounce";

import Autocomplete from "../components/Autocomplete";
import {autocomplete} from "../api";
import {isEmpty, stop} from "../utils/Utils";

import "./Search.css";

export default class Search extends React.PureComponent {

    constructor(props) {
        super(props);
        const configuration = props.configuration;
        const tabs = configuration.map(metaData => metaData.title);
        this.state = {
            selected: -1,
            suggestions: [],
            query: "",
            tabs: tabs,
            selectedTab: tabs[0],
            loadingAutoComplete: false
        };
    }

    onSearchKeyDown = e => {
        const {suggestions, selected} = this.state;
        if (e.keyCode === 40 && selected < (suggestions.length - 1)) {//keyDown
            stop(e);
            this.setState({selected: (selected + 1)});
        }
        if (e.keyCode === 38 && selected >= 0) {//keyUp
            stop(e);
            this.setState({selected: (selected - 1)});
        }
        if (e.keyCode === 13 && selected >= 0) {//enter
            stop(e);
            this.setState({selected: -1}, () => this.itemSelected(suggestions[selected]));
        }
        if (e.keyCode === 27) {//escape
            stop(e);
            this.setState({selected: -1, query: "", suggestions: []});
        }

    };

    search = selectedTab => e => {
        const query = e.target.value;
        this.setState({query: query, selected: -1});
        if (!isEmpty(query) && query.trim().length > 2) {
            this.setState({loadingAutoComplete: true});
            this.delayedAutocomplete();
        }
    };

    delayedAutocomplete = debounce(() =>
        autocomplete(this.state.selectedTab, this.state.query).then(results => this.setState({
            suggestions: results,
            loadingAutoComplete: false
        })), 200);

    itemSelected = metaData => this.props.history.push(`/metadata/${metaData.type}/${metaData.id}`);

    onBlurSearch = suggestions => () => {
        if (!isEmpty(suggestions)) {
            setTimeout(() => this.setState({suggestions: [], loadingAutoComplete: true}), 250);
        } else {
            this.setState({suggestions: [], loadingAutoComplete: true});
        }
    };

    renderTab = (tab, selectedTab) =>
        <span key={tab} className={tab === selectedTab ? "active" : ""}>
            {I18n.t(`metadata.${tab}`)}
        </span>;

    render() {
        const {selected, suggestions, query, loadingAutoComplete, selectedTab, tabs} = this.state;
        const showAutoCompletes = query.length > 1 && !loadingAutoComplete;
        return (
            <div className="search-metadata">
                <section className="tabs">
                    {tabs.map(tab => this.renderTab(tab, selectedTab))}
                </section>
                <section className="search"
                         tabIndex="1" onBlur={this.onBlurSearch(suggestions)}>
                    <input className="search-input"
                           placeholder={I18n.t("metadata.searchPlaceHolder")}
                           type="text"
                           onChange={this.search(selectedTab)}
                           value={query}
                           onKeyDown={this.onSearchKeyDown}/>
                    <i className="fa fa-search"></i>
                    {showAutoCompletes && <Autocomplete suggestions={suggestions}
                                                        query={query}
                                                        selected={selected}
                                                        itemSelected={this.itemSelected}
                    />}
                </section>
            </div>
        );
    }
}

Search.propTypes = {
    history: PropTypes.object.isRequired,
    configuration: PropTypes.array.isRequired
};

