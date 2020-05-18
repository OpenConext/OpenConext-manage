import React from "react";
import I18n from "i18n-js";
import PropTypes from "prop-types";
import {allScopes, deleteScope, saveScope, scopeSupportedLanguagers} from "../api";
import {isEmpty} from "../utils/Utils";
import "./Scopes.css";
import ConfirmationDialog from "../components/ConfirmationDialog";
import String from "../components/form/String";
import cloneDeep from "lodash.clonedeep";
import {setFlash} from "../utils/Flash";

export default class Scopes extends React.Component {

  constructor(props) {
    super(props);
    this.state = {
      scopes: [],
      supportedLanguages: [],
      query: "",
      loaded: false,
      reversed: false,
      selectedScope: {},
      confirmationDialogOpen: false,
      confirmationQuestion: "",
      confirmationDialogAction: () => this,
      cancelDialogAction: () => this.setState({confirmationDialogOpen: false})
    };
  }

  componentDidMount() {
    Promise.all([allScopes(), scopeSupportedLanguagers()])
      .then(res => {
        this.setState({
          scopes: res[0],
          supportedLanguages: res[1],
          query: "",
          loaded: true,
          selectedScope: res[0].length > 0 ? cloneDeep(res[0][0]) : {}
        })
      });
  }

  cancel = () => {
    allScopes().then(res => this.setState({scopes: res}));
  }

  submit = () => {
    const {selectedScope} = this.state;
    let isNew = selectedScope.id === -1;
    selectedScope.id = (isNew ? null : selectedScope.id);

    saveScope(selectedScope).then(() => {
      this.cancel();
      const args = {name: selectedScope.name}
      setFlash(isNew ? I18n.t("scopes.flash.saved", args) : I18n.t("scopes.flash.updated", args))
    });
  }

  confirmDelete = (scope) => {
    this.setState({
      confirmationDialogOpen: true,
      confirmationQuestion: I18n.t("scopes.deleteConfirmation", {name: scope.name}),
      confirmationDialogAction: () => {
        this.setState({confirmationDialogOpen: false});
        deleteScope(scope.id).then(() => this.componentDidMount())
      }
    })
  };

  icon = reversed => reversed ? <i className="fa fa-arrow-down current"/> : <i className="fa fa-arrow-down current"/>;

  sortTable = () => {
    const {reversed, scopes} = this.state;
    this.setState({scopes: scopes.reverse(), reversed: !reversed});
  };

  updateDescription = lang => val => {
    const {selectedScope} = this.state;
    selectedScope.descriptions[lang] = val;
    this.setState({selectedScope: selectedScope});
  }

  updateName = val => {
    const {selectedScope} = this.state;
    selectedScope.name = val;
    this.setState({selectedScope: selectedScope});
  }

  renderDetails = () => {
    const {supportedLanguages, selectedScope} = this.state;
    return (<section className="scope-details">
      <label>
        {I18n.t("scopes.name")}
      </label>
      <String value={selectedScope.name || ""} onChange={this.updateName}/>

      <p>{I18n.t("scopes.details")}</p>
      {supportedLanguages.map(lang => <div key={lang} className="form-element">
        <label htmlFor={lang}>
          {I18n.t("scopes.lang", {lang})}
        </label>
        <String value={selectedScope.descriptions[lang] || ""} onChange={this.updateDescription(lang)}/>

      </div>)}
      <section className="detail-actions">
        <a className="button" onClick={this.cancel}>
          {I18n.t("scopes.cancel")}
        </a>
        <a className="button red" onClick={this.confirmDelete}>
          {I18n.t("scopes.delete")}
        </a>
        <a className="button blue" onClick={this.submit}>
          {selectedScope.id === -1 ? I18n.t("scopes.save") : I18n.t("scopes.update")}
        </a>
      </section>

    </section>);
  }

  newScope = () => {
    const scope = {
      id: -1,
      name: "new scope",
      descriptions: {}
    }
    const {scopes} = this.state;
    scopes.push(scope);
    this.setState({selectedScope: scope});
  }

  renderResults = (scopes, selectedScope, query, reversed) =>
    <div>
      <section className="explanation">
        <p>Below are all Scopes that can be configured on OIDC Relying Parties.</p>
      </section>
      <section className="options">
        <div className="search-input-container">
          <input className="search-input"
                 placeholder={I18n.t("scopes.searchPlaceHolder")}
                 type="text"
                 onChange={e => this.setState({query: e.target.value})}
                 value={query}/>
          <i className="fa fa-search"></i>
        </div>
      </section>
      <section className="master-detail">
        <section className="scopes-overview">
          <table className="search-results">
            <thead>
            <tr>
              <th className="name" onClick={this.sortTable}>
                {I18n.t("scopes.name")}{this.icon(reversed)}
              </th>
            </tr>
            </thead>
            <tbody>
            {scopes.map((scope, index) =>
              <tr key={`${scope.name}_${index}`}
                  onClick={() => this.setState({selectedScope: cloneDeep(scope)})}>
                <td className={`${selectedScope.id === scope.id ? 'selected' : ''}`}>{scope.name}</td>
              </tr>)}
            </tbody>
          </table>
          <section className="actions">
            <a className="button blue" onClick={this.newScope}>
              {I18n.t("scopes.new")}
            </a>
          </section>
        </section>
        {this.renderDetails()}
      </section>
    </div>;

  renderNoResults = () => <section className="no-scopes">
    <p>There are no Scopes configured.</p>
  </section>;

  render() {
    const {
      scopes, loaded, query, reversed, selectedScope,
      confirmationDialogOpen, confirmationQuestion, confirmationDialogAction, cancelDialogAction
    } = this.state;
    if (!loaded) {
      return null;
    }
    let filteredScopes = query ? scopes
      .filter(scope => scope.name.toLowerCase().indexOf(query.toLowerCase()) > -1) : scopes;
    filteredScopes = filteredScopes.filter(scope => scope.id !== -1);

    const noScopes = isEmpty(scopes);
    return (
      <div className="scopes">
        <ConfirmationDialog isOpen={confirmationDialogOpen}
                            cancel={cancelDialogAction}
                            confirm={confirmationDialogAction}
                            question={confirmationQuestion}/>
        {!noScopes && this.renderResults(filteredScopes, selectedScope, query, reversed)}
        {noScopes && this.renderNoResults()}
      </div>
    );
  }

}

Scopes.propTypes = {
  history: PropTypes.object.isRequired
};
