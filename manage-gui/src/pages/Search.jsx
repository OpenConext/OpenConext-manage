import React from "react";
import I18n from "i18n-js";
import PropTypes from "prop-types";
import debounce from "lodash.debounce";
import Autocomplete from "../components/Autocomplete";
import {autocomplete, ping} from "../api";
import {isEmpty, stop} from "../utils/Utils";

import "./Search.scss";
import withRouterHooks from "../utils/RouterBackwardCompatability";
import {isSystemUser} from "../utils/User";

class Search extends React.PureComponent {

    constructor(props) {
        super(props);
        const configuration = props.configuration;
        const showOidcRp = props.currentUser.product.showOidcRp;
        const tabs = configuration
            .sort((a, b) => a.order > b.order ? 1 : -1)
            .map(metaData => metaData.title)
            .filter(title => showOidcRp || title !== "oidc10_rp");
        this.state = {
            selected: -1,
            suggestions: [],
            alternatives: [],
            query: "",
            tabs: tabs,
            selectedTab: tabs[0],
            loadingAutoComplete: false,
            loading: false,
            moreToShow: false,
            moreAlternativesToShow: false
        };
    }

    componentDidMount() {
        ping().then(() => this.searchInput.focus());
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

    search = e => {
        const query = e.target.value;
        this.setState({query: query, selected: -1});
        if ((!isEmpty(query) && query.trim().length > 2) || "*" === query.trim()) {
            this.setState({loadingAutoComplete: true});
            this.delayedAutocomplete();
        }
    };

    delayedAutocomplete = debounce(() =>
        autocomplete(this.state.selectedTab, this.state.query)
            .then(results => {
                const suggestions = results.suggestions || [];
                const alternatives = results.alternatives || [];
                this.setState({
                    suggestions: suggestions.length > 15 ? suggestions.slice(0, suggestions.length - 1) : suggestions,
                    loadingAutoComplete: false,
                    alternatives: alternatives.length > 15 ? alternatives.slice(0, alternatives.length - 1) : alternatives,
                    moreToShow: suggestions.length > 15 && this.state.query !== "*",
                    moreAlternativesToShow: alternatives.length > 15
                })
            }), 200);

    itemSelected = metaData => {
        const {navigate} = this.props;
        navigate(`/metadata/${metaData.type}/${metaData["_id"]}`);
    }

    newMetaData = e => {
        stop(e);
        this.props.navigate(`/metadata/${this.state.selectedTab}/new`);
    };

    switchTab = tab => e => {
        stop(e);
        this.setState({selectedTab: tab});
        this.search({target: {value: this.state.query}});
        this.searchInput.focus()
    };

    renderTab = (tab, selectedTab) =>
        <span key={tab} className={tab === selectedTab ? "active" : ""} onClick={this.switchTab(tab)}>
            {I18n.t(`metadata.${tab}`)}
        </span>;

    render() {
        const {
            selected, suggestions, query, loadingAutoComplete, selectedTab, tabs, moreToShow, alternatives,
            moreAlternativesToShow
        } = this.state;
        const showAutoCompletes = (query.trim().length > 2 || "*" === query.trim()) && !loadingAutoComplete;
        return (
            <div className="search-metadata">
                <section className="tabs">
                    {tabs.map(tab => this.renderTab(tab, selectedTab))}
                </section>
                <section className="search"
                         tabIndex="1">
                    <div className="search-input-container">
                        <input className="search-input"
                               ref={ref => this.searchInput = ref}
                               placeholder={I18n.t("metadata.searchPlaceHolder")}
                               type="text"
                               onChange={this.search}
                               value={query}
                               onKeyDown={this.onSearchKeyDown}/>
                        <i className="fa fa-search"></i>
                        {showAutoCompletes && <Autocomplete suggestions={suggestions}
                                                            query={query}
                                                            alternatives={alternatives}
                                                            selected={selected}
                                                            itemSelected={this.itemSelected}
                                                            moreToShow={moreToShow}
                                                            moreAlternativesToShow={moreAlternativesToShow}
                        />}
                    </div>
                    {(this.state.selectedTab !== "saml20_idp" ||  isSystemUser(this.props.currentUser)) &&
                        <a className="new button green" onClick={this.newMetaData}>
                        {I18n.t("metadata.new")}<i className="fa fa-plus"></i>
                    </a>}
                </section>
            </div>
        );
    }
}

export default withRouterHooks(Search);

Search.propTypes = {
    currentUser: PropTypes.object.isRequired,
    configuration: PropTypes.array.isRequired
};

