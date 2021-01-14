import React from "react";
import I18n from "i18n-js";
import PropTypes from "prop-types";
import {allScopes, deleteScope, saveScope, scopeSupportedLanguagers} from "../api";
import {isEmpty, validScope} from "../utils/Utils";
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
      cancelDialogAction: () => this.setState({confirmationDialogOpen: false}),
      inUse: false,
      inUseAction: "",
      inUseEntities: [],
      nameInUse: false
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
        })
      });
  }

  refreshScopes = () => {
    allScopes().then(res =>
      this.setState({
        scopes: res,
        inUseEntities: [],
        inUse: false,
        inUseAction: "",
        selectedScope: {},
        nameInUse: false
      }));
  }

  submit = () => {
    const {selectedScope} = this.state;
    if (selectedScope.name) {
      saveScope(selectedScope)
        .then(() => {
          this.refreshScopes();
          const args = {name: selectedScope.name}
          setFlash(selectedScope.id ? I18n.t("scopes.flash.updated", args) : I18n.t("scopes.flash.saved", args))
        })
        .catch(err => this.handleError(err, "change the name "));
    }
  }

  handleError = (err, action) => {
    if (err.response && err.response.status === 409) {
      err.response.json().then(json => {
        this.setState({inUse: true, inUseEntities: JSON.parse(json.message), inUseAction: action})
      });
    } else if (err.response && err.response.status === 400) {
      this.setState({nameInUse: true})
    } else {
      throw err;
    }
  }

  confirmDelete = scope => () => {
    if (scope.id) {
      this.setState({
        confirmationDialogOpen: true,
        confirmationQuestion: I18n.t("scopes.deleteConfirmation", {name: scope.name}),
        confirmationDialogAction: () => {
          this.setState({confirmationDialogOpen: false});
          deleteScope(scope.id)
            .then(() => {
              this.refreshScopes();
              setFlash(I18n.t("scopes.flash.deleted", {name: scope.name}));
            })
            .catch(err => this.handleError(err, "delete"));
        }
      });
    }
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

  renderEmptyDetails = () => {
    return (<section className="scope-details">
      Select a scope in the list on the left to edit the name and description. Or add a new scope.
    </section>)

  }

  renderDetails = () => {
    const {supportedLanguages, selectedScope, inUseEntities, inUse, inUseAction, nameInUse} = this.state;
    const invalidName = !validScope(selectedScope.name);
    if (isEmpty(selectedScope)) {
      return this.renderEmptyDetails();
    }
    return (<section className="scope-details">
      {inUse && <section className="errors">
        <p>{I18n.t("scopes.inUse", {action: inUseAction})}</p>
        <ul>
          {inUseEntities.map(entity => <li key={entity.id}>
            <a href={`/metadata/${entity.type}/${entity.id}`} target="_blank">{entity.entityid}</a>
          </li>)}
        </ul>
      </section>}
      {nameInUse && <section className="errors">
        <p>{I18n.t("scopes.nameInUse")}</p>
      </section>}

      <label className="required">
        {I18n.t("scopes.name")}
      </label>
      <String placeholder={I18n.t("scopes.namePlaceholder")} value={selectedScope.name || ""}
              onChange={this.updateName}/>
      {invalidName &&
      <p className="errors">{I18n.t("scopes.invalidName")}</p>}
      <p>{I18n.t("scopes.details")}</p>
      {supportedLanguages.map(lang => <div key={lang} className="form-element">
        <label htmlFor={lang}>
          {I18n.t("scopes.lang", {lang})}
        </label>
        <String placeholder={I18n.t("scopes.descriptionPlaceholder", {lang: I18n.t(`scopes.${lang}`)})}
                value={selectedScope.descriptions[lang] || ""} onChange={this.updateDescription(lang)}/>

      </div>)}
      <section className="detail-actions">
        <a className="button" onClick={this.refreshScopes}>
          {I18n.t("scopes.cancel")}
        </a>
        <a className={`button ${(inUse || !selectedScope.id) ? 'grey' : 'red'}`}
           onClick={this.confirmDelete(selectedScope)}>
          {I18n.t("scopes.delete")}
        </a>
        <a className={`button ${(!selectedScope.name || invalidName) ? 'grey' : 'blue'}`} onClick={this.submit}>
          {selectedScope.id ? I18n.t("scopes.update") : I18n.t("scopes.save")}
        </a>
      </section>

    </section>);
  }

  newScope = () => {
    const scope = {
      name: "",
      descriptions: {}
    }
    this.setState({
      inUseEntities: [],
      inUse: false,
      inUseAction: "",
      selectedScope: scope,
      nameInUse: false
    });
  }

  renderResults = (scopes, selectedScope, query, reversed) =>
    <div>
      <section className="explanation">
        {
          scopes.length === 0 ? <p>{I18n.t("scopes.noScopes")}</p> : <p>{I18n.t("scopes.info")}</p>
        }
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
                  onClick={() => this.setState({
                    selectedScope: cloneDeep(scope),
                    inUseEntities: [],
                    inUse: false,
                    inUseAction: ""
                  })}>
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

  render() {
    const {
      scopes, loaded, query, reversed, selectedScope,
      confirmationDialogOpen, confirmationQuestion, confirmationDialogAction, cancelDialogAction
    } = this.state;
    if (!loaded) {
      return null;
    }
    const filteredScopes = query ? scopes
      .filter(scope => scope.name.toLowerCase().indexOf(query.toLowerCase()) > -1) : scopes;

    return (
      <div className="scopes">
        <ConfirmationDialog isOpen={confirmationDialogOpen}
                            cancel={cancelDialogAction}
                            confirm={confirmationDialogAction}
                            question={confirmationQuestion}/>
        {this.renderResults(filteredScopes, selectedScope, query, reversed)}
      </div>
    );
  }

}

Scopes.propTypes = {
  history: PropTypes.object.isRequired
};
