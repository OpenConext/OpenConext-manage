import React from "react";
import I18n from "i18n-js";
import PropTypes from "prop-types";
import debounce from "lodash.debounce";

import Autocomplete from "../components/Autocomplete";
import {autocomplete, ping, push} from "../api";
import {isEmpty, stop} from "../utils/Utils";
import ConfirmationDialog from "../components/ConfirmationDialog";

import "./Search.css";
import {setFlash} from "../utils/Flash";

export default class Search extends React.PureComponent {

    constructor(props) {
        super(props);
        const configuration = props.configuration;
        const tabs = configuration.sort((a, b) => a.order > b.order ? 1 : -1).map(metaData => metaData.title);
        this.state = {
            selected: -1,
            suggestions: [],
            query: "",
            tabs: tabs,
            selectedTab: tabs[0],
            loadingAutoComplete: false,
            confirmationDialogOpen: false,
            confirmationQuestion: "",
            confirmationDialogAction: () => this,
            cancelDialogAction: () => this.setState({confirmationDialogOpen: false}),
            loading: false
        };
    }

    componentDidMount() {
        ping().then(() => this.searchInput.focus());
    }

    runPush = e => {
        stop(e);
        if (this.state.loading) {
            return;
        }
        this.setState({loading: true});
        push().then(json => {
            this.setState({loading: false, pushResults: json.deltas});
            const ok = json.status === "OK";
            const msg = ok ? "playground.pushedOk" : "playground.pushedNotOk";
            setFlash(I18n.t(msg, {name: this.props.currentUser.push.name}), ok ? "info" : "error");
        });
    };

    renderPushButton = () => {
        const {loading} = this.state;
        const {currentUser} = this.props;
        const action = () => {
            this.setState({confirmationDialogOpen: false});
            this.runPush();
        };
        return  <a className={`push button ${loading ? "grey disabled" : "blue"}`}
           onClick={() => this.setState({
               confirmationDialogOpen: true,
               confirmationQuestion: I18n.t("playground.pushConfirmation", {
                   url: currentUser.push.url,
                   name: currentUser.push.name
               }),
               confirmationDialogAction: action
           })}>{I18n.t("playground.runPush")}
            <i className="fa fa-refresh"></i>
        </a>
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

    itemSelected = metaData => this.props.history.push(`/metadata/${this.state.selectedTab}/${metaData["_id"]}`);

    onBlurSearch = suggestions => () => {
        if (!isEmpty(suggestions)) {
            setTimeout(() => this.setState({suggestions: [], loadingAutoComplete: true}), 250);
        } else {
            this.setState({suggestions: [], loadingAutoComplete: true});
        }
    };

    newMetaData = e => {
        stop(e);
        this.props.history.push(`/metadata/${this.state.selectedTab}/new`);
    };

    switchTab = tab => e => {
        stop(e);
        this.setState({selectedTab: tab});
        this.search(tab)({target: {value: this.state.query}});
        this.searchInput.focus()
    };

    renderTab = (tab, selectedTab) =>
        <span key={tab} className={tab === selectedTab ? "active" : ""} onClick={this.switchTab(tab)}>
            {I18n.t(`metadata.${tab}`)}
        </span>;

    render() {
        const {selected, suggestions, query, loadingAutoComplete, selectedTab, tabs,
            confirmationDialogOpen, cancelDialogAction, confirmationDialogAction, confirmationQuestion} = this.state;
        const showAutoCompletes = query.length > 1 && !loadingAutoComplete;
        return (
            <div className="search-metadata">
                {this.renderPushButton()}
                <ConfirmationDialog isOpen={confirmationDialogOpen}
                                    cancel={cancelDialogAction}
                                    confirm={confirmationDialogAction}
                                    question={confirmationQuestion}/>
                <section className="tabs">
                    {tabs.map(tab => this.renderTab(tab, selectedTab))}
                </section>
                <section className="search"
                         tabIndex="1" onBlur={this.onBlurSearch(suggestions)}>
                    <div className="search-input-container">
                        <input className="search-input"
                               ref={ref => this.searchInput = ref}
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
                    </div>
                    {!this.props.currentUser.guest && <a className="new button green" onClick={this.newMetaData}>
                        {I18n.t("metadata.new")}<i className="fa fa-plus"></i>
                    </a>}
                </section>
            </div>
        );
    }
}

Search.propTypes = {
    history: PropTypes.object.isRequired,
    currentUser: PropTypes.object.isRequired,
    configuration: PropTypes.array.isRequired
};

