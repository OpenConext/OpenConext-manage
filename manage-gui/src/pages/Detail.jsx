import React from "react";
import I18n from "i18n-js";
import PropTypes from "prop-types";

import {
  ARP,
  ConnectedIdps,
  Connection,
  ConsentDisabling,
  Export,
  Import,
  Manipulation,
  MetaData,
  Revisions,
  WhiteList
} from "../components/metadata";

import ConfirmationDialog from "../components/ConfirmationDialog";

import {
  allResourceServers,
  detail,
  remove,
  revisions,
  save,
  template,
  update,
  whiteListing
} from "../api";
import { isEmpty, stop } from "../utils/Utils";
import { setFlash } from "../utils/Flash";

import "./Detail.css";
import ResourceServers from "../components/metadata/ResourceServers";

const tabsSp = [
  "connection",
  "connected_idps",
  "metadata",
  "arp",
  "whitelist",
  "manipulation",
  "revisions",
  "import",
  "export"
];

const tabsIdP = [
  "connection",
  "whitelist",
  "consent_disabling",
  "metadata",
  "manipulation",
  "revisions",
  "import",
  "export"
];

const tabsOIDC = [
  "connection",
  "connected_idps",
  "metadata",
  "resource_servers",
  "arp",
  "whitelist",
  "revisions",
  "import",
  "export"
];

const tabsSingleTenant = [
  "connection",
  "metadata",
  "arp",
  "revisions",
  "import",
  "export"
];

export default class Detail extends React.PureComponent {
  constructor(props) {
    super(props);
    const type = isEmpty(props.newMetaData)
      ? this.props.match.params.type
      : props.newMetaData.connection.type.value.replace("-", "_");
    const id = isEmpty(props.newMetaData) ? this.props.match.params.id : "new";
    this.state = {
      metaData: {},
      whiteListing: [],
      resourceServers: [],
      revisions: [],
      notFound: false,
      loaded: false,
      selectedTab: "connection",
      revisionNote: "",
      confirmationDialogOpen: false,
      confirmationDialogAction: () => this,
      cancelDialogAction: () => this,
      leavePage: false,
      errors: {},
      changes: {},
      isNew: true,
      originalEntityId: undefined,
      type: type,
      id: id,
      revisionNoteError: false,
      addedWhiteListedEntities: [],
      removedWhiteListedEntities: []
    };
  }

  componentDidMount() {
    window.scrollTo(0, 0);
    const { newMetaData } = this.props;
    let { type, id } = this.state;
    const isNew = id === "new";
    const promise = isNew ? template(type) : detail(type, id);
    promise
      .then(metaData => {
        const isSp = type === "saml20_sp" || type === "oidc10_rp";
        const isOidcRP = type === "oidc10_rp";
        const whiteListingType = isSp ? "saml20_idp" : "saml20_sp";
        const errorKeys = isSp ? tabsSp : tabsIdP;
        if (this.props.clone) {
          //Clean all
          const clonedClearFields = [
            "entityid",
            "revision",
            "created",
            "eid",
            "id",
            "ip",
            "notes",
            "revisionid",
            "revisionnote",
            "user"
          ];
          metaData.id = undefined;
          metaData.revision = undefined;
          clonedClearFields.forEach(attr => delete metaData.data[attr]);
          id = undefined;
        }
        const selectedTab =
          this.props.match &&
          this.props.match.params &&
          this.props.match.params.tab
            ? this.props.match.params.tab
            : "connection";
        this.setState({
          metaData: metaData,
          isNew: isNew || !isEmpty(this.props.clone),
          originalEntityId: metaData.data.entityid || "",
          loaded: isEmpty(newMetaData),
          errors: errorKeys.reduce((acc, tab) => {
            acc[tab] = {};
            return acc;
          }, {}),
          changes: errorKeys.reduce((acc, tab) => {
            acc[tab] = false;
            return acc;
          }, {}),
          selectedTab: selectedTab
        });
        if (!isEmpty(newMetaData)) {
          this.applyImportChanges(newMetaData, {
            connection: true,
            metaDataFields: true,
            allowedEntities: true,
            disableConsent: true,
            arp: true
          });
        } else {
          this.validate(metaData, this.props.configuration, type);
        }
        whiteListing(whiteListingType, metaData.data.state).then(whiteListing => {
          this.setState({whiteListing: whiteListing});
          if (isOidcRP) {
            allResourceServers(metaData.data.state).then(json =>
              this.setState({ resourceServers: json })
            );
          }
          revisions(type, id).then(revisions => {
            revisions.push(metaData);
            revisions.sort((r1, r2) =>
              r1.revision.number < r2.revision.number
                ? 1
                : r1.revision.number > r2.revision.number
                ? -1
                : 0
            );
            this.setState({ revisions: revisions });
          });
        });
      })
      .catch(err => {
        if (err.response && err.response.status === 404) {
          this.setState({ notFound: true, loaded: true });
        } else {
          throw err;
        }
      });
  }

  validate = (metaData, configurations, type) => {
    const configuration = configurations.find(conf => conf.title === type);
    const requiredMetaData = configuration.properties.metaDataFields.required;
    const metaDataFields = metaData.data.metaDataFields;
    const metaDataErrors = {};
    Object.keys(metaDataFields).forEach(key => {
      if (isEmpty(metaDataFields[key]) && requiredMetaData.indexOf(key) > -1) {
        metaDataErrors[key] = true;
      }
    });
    requiredMetaData.forEach(req => {
      if (!metaDataFields[req]) {
        metaDataErrors[req] = true;
        this.onChange("metadata", `data.metaDataFields.${req}`, "");
      }
    });
    const connectionErrors = {};
    const required = configuration.required;
    Object.keys(metaData.data).forEach(key => {
      if (isEmpty(metaData.data[key]) && required.indexOf(key) > -1) {
        connectionErrors[key] = true;
      }
    });
    required.forEach(req => {
      if (metaData.data[req] === undefined) {
        connectionErrors[req] = true;
      }
    });
    const newErrors = {
      ...this.state.errors,
      connection: connectionErrors,
      metadata: metaDataErrors
    };
    this.setState({ errors: newErrors });
  };

  switchTab = tab => e => {
    stop(e);
    this.setState({ selectedTab: tab });
    const { type, id } = this.state;
    if (!this.props.fromImport) {
      this.props.history.push(`/metadata/${type}/${id}/${tab}`);
    }
  };

  onError = name => (key, isError) => {
    const errors = { ...this.state.errors };
    errors[name][key] = isError;
    this.setState({ errors: errors });
  };

  nameOfMetaData = metaData =>
    metaData.data.metaDataFields["name:en"] ||
    metaData.data.metaDataFields["name:nl"] ||
    metaData.data["entityid"];

  onChange = component => (
    name,
    value,
    replaceAtSignWithDotsInName = false
  ) => {
    const currentState = this.state.metaData;
    const metaData = {
      ...currentState,
      data: { ...currentState.data },
      arp: { ...currentState.arp },
      metaDataFields: { ...currentState.metaDataFields }
    };
    if (Array.isArray(name) && Array.isArray(value)) {
      for (let i = 0; i < name.length; i++) {
        this.changeValueReference(
          metaData,
          name[i],
          value[i],
          replaceAtSignWithDotsInName
        );
      }
    } else {
      this.changeValueReference(
        metaData,
        name,
        value,
        replaceAtSignWithDotsInName
      );
    }
    const changes = { ...this.state.changes };
    changes[component] = true;
    if (
      component === "whitelist" &&
      (name === "data.allowedall" ||
        (Array.isArray(name) && name.includes("data.allowedall")))
    ) {
      this.setState({
        addedWhiteListedEntities: [],
        removedWhiteListedEntities: []
      });
    }
    this.setState({ metaData: metaData, changes: changes });
  };

  onChangeWhiteListedEntity = (added, entity) => {
    const removedWhiteListedEntities = [
      ...this.state.removedWhiteListedEntities
    ];
    const addedWhiteListedEntities = [...this.state.addedWhiteListedEntities];
    if (added) {
      const newRemovedWhiteListedEntities = removedWhiteListedEntities.filter(
        e => e.entityid !== entity.entityid
      );
      if (
        newRemovedWhiteListedEntities.length ===
        removedWhiteListedEntities.length
      ) {
        addedWhiteListedEntities.push(entity);
      }
      this.setState({
        addedWhiteListedEntities: addedWhiteListedEntities,
        removedWhiteListedEntities: newRemovedWhiteListedEntities
      });
    } else {
      const newAddedWhiteListedEntities = addedWhiteListedEntities.filter(
        e => e.entityid !== entity.entityid
      );
      if (
        newAddedWhiteListedEntities.length === addedWhiteListedEntities.length
      ) {
        removedWhiteListedEntities.push(entity);
      }
      this.setState({
        addedWhiteListedEntities: newAddedWhiteListedEntities,
        removedWhiteListedEntities: removedWhiteListedEntities
      });
    }
  };

  changeValueReference = (
    metaData,
    name,
    value,
    replaceAtSignWithDotsInName
  ) => {
    if (name.endsWith("redirect.sign")) {
      name = name.replace(/redirect\.sign/, "redirect@sign");
      replaceAtSignWithDotsInName = true;
    }
    const parts = name.split(".");
    let last = parts.pop();

    let ref = metaData;
    parts.forEach(part => (ref = ref[part]));
    last = replaceAtSignWithDotsInName ? last.replace(/@/g, ".") : last;
    if (value === null) {
      delete ref[last];
    } else {
      ref[last] = value;
    }
  };

  applyImportChanges = (results, applyChangesFor) => {
    const newChanges = { ...this.state.changes };
    const newData = { ...this.state.metaData.data };
    ["allowedEntities", "disableConsent", "arp"].forEach(name => {
      if (applyChangesFor[name] && results[name]) {
        newData[name] = results[name];
        if (name === "allowedEntities") {
          newChanges.whitelist = true;
        }
        if (name === "disableConsent") {
          newChanges.consent_disabling = true;
        }
        if (name === "arp") {
          newChanges.arp = true;
        }
      }
    });
    if (applyChangesFor["metaDataFields"] && results["metaDataFields"]) {
      Object.keys(results.metaDataFields).forEach(key => {
        if (results.metaDataFields[key].selected) {
          if (results.metaDataFields[key].value) {
            newData.metaDataFields[key] = results.metaDataFields[key].value;
          } else {
            delete this.state.metaData.data.metaDataFields[key];
          }
          newChanges.metadata = true;
        }
      });
    }
    if (applyChangesFor["connection"] && results["connection"]) {
      Object.keys(results.connection).forEach(key => {
        if (results.connection[key].selected) {
          newData[key] = results.connection[key].value;
          newChanges.connection = true;
        }
      });
    }
    const changes = Object.keys(applyChangesFor).filter(
      key => applyChangesFor[key]
    );
    const prefix = isEmpty(this.props.newMetaData) ? "" : "new_";

    const newMetaData = { ...this.state.metaData, data: newData };
    this.setState(
      {
        selectedTab: "connection",
        changes: newChanges,
        metaData: newMetaData,
        loaded: true
      },
      this.validate(newMetaData, this.props.configuration, this.state.type)
    );

    if (changes.length > 0) {
      setFlash(
        I18n.t(`import.${prefix}applyImportChangesFlash`, {
          changes: changes.join(", ")
        }),
        "warning"
      );
    }
  };

  submit = e => {
    stop(e);
    const { errors, revisionNote } = this.state;
    const hasErrors = this.hasGlobalErrors(errors);
    if (isEmpty(revisionNote)) {
      this.setState({ revisionNoteError: true }, () =>
        this.revisionNote.focus()
      );
      return false;
    }
    if (hasErrors) {
      return false;
    }
    this.setState({ revisionNoteError: false });
    const promise = this.state.isNew ? save : update;
    const metaData = this.state.metaData;
    metaData.data.revisionnote = revisionNote;
    promise(metaData).then(json => {
      if (json.exception) {
        setFlash(json.validations, "error");
      } else {
        const name =
          json.data.metaDataFields["name:en"] ||
          json.data.metaDataFields["name:nl"] ||
          "this service";
        setFlash(
          I18n.t("metadata.flash.updated", {
            name: name,
            revision: json.revision.number
          })
        );
        this.props.history.replace(`/dummy`);
        setTimeout(
          () =>
            this.props.history.replace(
              `/metadata/${json.type}/${json.id}/${this.state.selectedTab}`
            ),
          50
        );
      }
    });
  };

  renderActions = revisionNote => {
    if (this.props.currentUser.guest) {
      return null;
    }
    const { errors, revisionNoteError } = this.state;
    const hasErrors = this.hasGlobalErrors(errors);
    const revisionNoteRequired = revisionNoteError && isEmpty(revisionNote);
    return (
      <section className="actions">
        <section className="notes-container">
          <section className="notes">
            <label htmlFor="revisionnote">
              {I18n.t("metadata.revisionnote")}
            </label>
            <input
              name="revisionnote"
              type="text"
              value={revisionNote}
              ref={ref => (this.revisionNote = ref)}
              onKeyPress={e => (e.key === "Enter" ? this.submit(e) : false)}
              onChange={e => this.setState({ revisionNote: e.target.value })}
            />
          </section>
          {revisionNoteRequired && (
            <em className="error">{I18n.t("metadata.revisionnoteRequired")}</em>
          )}
        </section>
        <section className="buttons">
          <a
            className="button"
            onClick={e => {
              stop(e);
              this.setState({
                cancelDialogAction: () => this.props.history.replace("/search"),
                confirmationDialogAction: () =>
                  this.setState({ confirmationDialogOpen: false }),
                confirmationDialogOpen: true,
                leavePage: true
              });
            }}
          >
            {I18n.t("metadata.cancel")}
          </a>
          <a
            className={`button ${hasErrors ? "grey disabled" : "blue"}`}
            onClick={this.submit}
          >
            {I18n.t("metadata.submit")}
          </a>
        </section>
      </section>
    );
  };

  hasGlobalErrors = errors =>
    Object.keys(errors).find(key =>
      Object.keys(errors[key]).find(subKey => errors[key][subKey])
    ) !== undefined;

  renderTab = tab => {
    const tabErrors = this.state.errors[tab] || {};
    const tabChanges = this.state.changes[tab] || false;
    const hasChanges = tabChanges ? "changes" : "";
    const className = this.state.selectedTab === tab ? "active" : "";
    const hasErrors =
      Object.keys(tabErrors).find(key => tabErrors[key] === true) !== undefined
        ? "errors"
        : "";
    return (
      <span
        key={tab}
        className={`${className} ${hasErrors} ${hasChanges}`}
        onClick={this.switchTab(tab)}
      >
        {I18n.t(`metadata.tabs.${tab}`)}
        {hasErrors && <i className="fa fa-warning" />}
        {!hasErrors && tabChanges && <i className="fa fa-asterisk" />}
      </span>
    );
  };

  renderCurrentTab = (
    tab,
    metaData,
    resourceServers,
    whiteListing,
    revisions
  ) => {
    const configuration = this.props.configuration.find(
      conf => conf.title === this.state.type
    );
    const guest = this.props.currentUser.guest;
    const {
      isNew,
      originalEntityId,
      type,
      removedWhiteListedEntities,
      addedWhiteListedEntities
    } = this.state;
    const name =
      metaData.data.metaDataFields["name:en"] ||
      metaData.data.metaDataFields["name:nl"] ||
      "this service";
    switch (tab) {
      case "connection":
        return (
          <Connection
            metaData={metaData}
            onChange={this.onChange("connection")}
            onError={this.onError("connection")}
            errors={this.state.errors["connection"]}
            guest={guest}
            isNew={isNew}
            originalEntityId={originalEntityId}
            configuration={configuration}
          />
        );
      case "whitelist":
        return (
          <WhiteList
            whiteListing={whiteListing}
            name={name}
            allowedEntities={metaData.data.allowedEntities}
            allowedAll={metaData.data.allowedall}
            type={metaData.type}
            onChange={this.onChange("whitelist")}
            entityId={metaData.data.entityid}
            guest={guest}
            removedWhiteListedEntities={removedWhiteListedEntities}
            addedWhiteListedEntities={addedWhiteListedEntities}
            onChangeWhiteListedEntity={this.onChangeWhiteListedEntity}
          />
        );
      case "metadata":
        return (
          <MetaData
            metaDataFields={metaData.data.metaDataFields}
            configuration={configuration}
            onChange={this.onChange("metadata")}
            name={name}
            onError={this.onError("metadata")}
            errors={this.state.errors["metadata"]}
            guest={guest}
          />
        );
      case "arp":
        return (
          <ARP
            arp={metaData.data.arp}
            arpConfiguration={configuration.properties.arp}
            onChange={this.onChange("arp")}
            guest={guest}
          />
        );
      case "resource_servers":
        return (
          <ResourceServers
            allowedResourceServers={metaData.data.allowedResourceServers}
            name={name}
            onChange={this.onChange("resource_servers")}
            entityId={metaData.data.entityid}
            resourceServers={resourceServers}
            guest={guest}
          />
        );
      case "connected_idps":
        return (
          <ConnectedIdps
            whiteListing={whiteListing}
            allowedAll={metaData.data.allowedall}
            allowedEntities={metaData.data.allowedEntities}
            name={name}
            entityId={metaData.data.entityid}
            state={metaData.data.state}
          />
        );
      case "manipulation":
        return (
          <Manipulation
            content={metaData.data.manipulation || ""}
            notes={metaData.data.manipulationNotes || ""}
            onChange={this.onChange("manipulation")}
            guest={guest}
          />
        );
      case "consent_disabling":
        return (
          <ConsentDisabling
            disableConsent={metaData.data.disableConsent}
            allowedEntities={metaData.data.allowedEntities}
            allowedAll={metaData.data.allowedall}
            name={name}
            whiteListing={whiteListing}
            onChange={this.onChange("consent_disabling")}
            guest={guest}
          />
        );
      case "revisions":
        return (
          <Revisions
            revisions={revisions}
            isNew={isNew}
            entityType={type}
            history={this.props.history}
          />
        );
      case "export":
        return <Export metaData={metaData} />;
      case "import":
        return (
          <Import
            metaData={metaData}
            guest={guest}
            newEntity={false}
            entityType={type}
            applyImportChanges={this.applyImportChanges}
          />
        );
      default:
        throw new Error(`Unknown tab ${tab}`);
    }
  };

  renderErrors = errors => {
    const allErrors = { ...errors };
    const errorKeys = Object.keys(allErrors).filter(
      err => !isEmpty(allErrors[err])
    );
    return (
      <section className="errors">
        <h2>{I18n.t("metadata.errors")}</h2>
        {errorKeys.map(err => (
          <div key={err}>
            <p>{err}</p>
            <ul>
              {Object.keys(allErrors[err])
                .filter(name => allErrors[err][name])
                .map((name, index) => (
                  <li key={index}>{name}</li>
                ))}
            </ul>
          </div>
        ))}
      </section>
    );
  };

  render() {
    const {
      loaded,
      notFound,
      metaData,
      resourceServers,
      whiteListing,
      revisions,
      selectedTab,
      revisionNote,
      confirmationDialogOpen,
      confirmationDialogAction,
      cancelDialogAction,
      leavePage,
      isNew,
      errors
    } = this.state;

    const type = metaData.type;

    const tabs = (() => {
      switch (type) {
        case "saml20_sp":
          return tabsSp;
        case "saml20_idp":
          return tabsIdP;
        case "oidc10_rp":
          return tabsOIDC;
        case "single_tenant_template":
          return tabsSingleTenant;
        default:
          return [];
      }
    })();

    const renderNotFound = loaded && notFound;
    const renderContent = loaded && !notFound;

    const name = renderContent ? this.nameOfMetaData(metaData) : "";
    const typeMetaData = I18n.t(`metadata.${type}_single`);
    const hasErrors = this.hasGlobalErrors(errors) && !isEmpty(metaData.id);

    return (
      <div className="detail-metadata">
        <ConfirmationDialog
          isOpen={confirmationDialogOpen}
          cancel={cancelDialogAction}
          confirm={confirmationDialogAction}
          question={
            leavePage
              ? undefined
              : I18n.t("metadata.deleteConfirmation", { name: name })
          }
          leavePage={leavePage}
        />
        {renderContent && (
          <section className="top-detail">
            <section className="info">{`${typeMetaData} - ${name}`}</section>
            {hasErrors && this.renderErrors(errors)}
            {!isNew && (
              <a
                className="button red delete-metadata"
                onClick={e => {
                  stop(e);
                  this.setState({
                    confirmationDialogAction: () => {
                      remove(this.state.metaData).then(res => {
                        this.props.history.replace("/search");
                        const name = this.nameOfMetaData(this.state.metaData);
                        setFlash(
                          I18n.t("metadata.flash.deleted", { name: name })
                        );
                      });
                    },
                    cancelDialogAction: () =>
                      this.setState({ confirmationDialogOpen: false }),
                    confirmationDialogOpen: true,
                    leavePage: false
                  });
                }}
              >
                {I18n.t("metadata.remove")}
              </a>
            )}
            {!isNew && (
              <a
                className="button green clone-metadata"
                onClick={e => {
                  stop(e);
                  this.props.history.replace(`/dummy`);
                  setTimeout(() => {
                    const name =
                      metaData.data.metaDataFields["name:en"] ||
                      metaData.data.metaDataFields["name:nl"] ||
                      "this service";
                    setFlash(I18n.t("metadata.flash.cloned", { name: name }));
                    this.props.history.replace(`/clone/${type}/${metaData.id}`);
                  }, 50);
                }}
              >
                {I18n.t("metadata.clone")}
              </a>
            )}
          </section>
        )}
        {renderNotFound && <section>{I18n.t("metadata.notFound")}</section>}
        {!notFound && (
          <section className="tabs">
            {tabs.map(tab => this.renderTab(tab))}
          </section>
        )}
        {renderContent &&
          this.renderCurrentTab(
            selectedTab,
            metaData,
            resourceServers,
            whiteListing,
            revisions
          )}
        {renderContent && this.renderActions(revisionNote)}
      </div>
    );
  }
}

Detail.propTypes = {
  history: PropTypes.object.isRequired,
  currentUser: PropTypes.object.isRequired,
  configuration: PropTypes.array.isRequired,
  clone: PropTypes.bool,
  fromImport: PropTypes.bool.isRequired,
  newMetaData: PropTypes.object
};
