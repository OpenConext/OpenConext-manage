import React from "react";
import I18n from "i18n-js";
import PropTypes from "prop-types";

import ARP from "../components/metadata/ARP";
import Connection from "../components/metadata/Connection";
import ConsentDisabling from "../components/metadata/ConsentDisabling";
import Manipulation from "../components/metadata/Manipulation";
import MetaData from "../components/metadata/MetaData";
import WhiteList from "../components/metadata/WhiteList";
import Revisions from "../components/metadata/Revisions";
import Export from "../components/metadata/Export";
import Import from "../components/metadata/Import";

import ConfirmationDialog from "../components/ConfirmationDialog";

import {detail, remove, revisions, save, template, update, whiteListing} from "../api";
import {isEmpty, stop} from "../utils/Utils";
import {setFlash} from "../utils/Flash";

import "./Detail.css";

const tabsSp = ["connection", "whitelist", "metadata", "arp", "manipulation", "revisions", "import", "export"];
const tabsIdP = ["connection", "whitelist", "consent_disabling", "metadata", "manipulation", "revisions", "import", "export"];

export default class Detail extends React.PureComponent {

    constructor(props) {
        super(props);
        const type = isEmpty(props.newMetaData) ?
            this.props.match.params.type : props.newMetaData.connection.type.value.replace("-", "_");
        const id = isEmpty(props.newMetaData) ? this.props.match.params.id : "new";
        this.state = {
            metaData: {},
            whiteListing: [],
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
            id: id
        };
    }

    componentDidMount() {
        window.scrollTo(0, 0);
        const {newMetaData} = this.props;
        const {type, id} = this.state;
        const isNew = id === "new";
        const promise = isNew ? template(type) : detail(type, id);
        promise.then(metaData => {
            const isSp = (type === "saml20_sp");
            const whiteListingType = isSp ? "saml20_idp" : "saml20_sp";
            const errorKeys = isSp ? tabsSp : tabsIdP;
            this.setState({
                metaData: metaData,
                isNew: isNew,
                originalEntityId: metaData.data.entityid,
                loaded: isEmpty(newMetaData),
                errors: errorKeys.reduce((acc, tab) => {
                    acc[tab] = {};
                    return acc;
                }, {}),
                changes: errorKeys.reduce((acc, tab) => {
                    acc[tab] = false;
                    return acc;
                }, {})
            });
            if (!isEmpty(newMetaData)) {
                this.applyImportChanges(newMetaData, {
                    "connection": true,
                    "metaDataFields": true,
                    "allowedEntities": true,
                    "disableConsent": true,
                    "arp": true
                });
            } else {
                this.validate(metaData, this.props.configuration, type);
            }

            whiteListing(whiteListingType).then(whiteListing => {
                this.setState({whiteListing: whiteListing});
                revisions(type, id).then(revisions => {
                    revisions.push(metaData);
                    revisions.sort((r1, r2) => r1.revision.number < r2.revision.number ? 1 : r1.revision.number > r2.revision.number ? -1 : 0);
                    this.setState({revisions: revisions});
                })
            })
        }).catch(err => {
            if (err.response && err.response.status === 404) {
                this.setState({notFound: true, loaded: true});
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
        const newErrors = {...this.state.errors, "connection": connectionErrors, "metadata": metaDataErrors};
        this.setState({errors: newErrors})
    };

    switchTab = tab => e => {
        stop(e);
        this.setState({selectedTab: tab});
    };

    onError = name => (key, isError) => {
        const errors = {...this.state.errors};
        errors[name][key] = isError;
        this.setState({errors: errors});
    };

    nameOfMetaData = metaData => metaData.data.metaDataFields["name:en"] || metaData.data.metaDataFields["name:nl"] || metaData.data["entityid"];

    onChange = component => (name, value, replaceAtSignWithDotsInName = false) => {
        const currentState = this.state.metaData;
        const metaData = {
            ...currentState,
            data: {...currentState.data},
            arp: {...currentState.arp},
            metaDataFields: {...currentState.metaDataFields}
        };
        if (Array.isArray(name) && Array.isArray(value)) {
            for (let i = 0; i < name.length; i++) {
                this.changeValueReference(metaData, name[i], value[i], replaceAtSignWithDotsInName);
            }
        } else {
            this.changeValueReference(metaData, name, value, replaceAtSignWithDotsInName);
        }
        const changes = {...this.state.changes};
        changes[component] = true;
        this.setState({metaData: metaData, changes: changes});

    };

    changeValueReference = (metaData, name, value, replaceAtSignWithDotsInName) => {
        const parts = name.split(".");
        let last = parts.pop();

        let ref = metaData;
        parts.forEach(part => ref = ref[part]);
        last = replaceAtSignWithDotsInName ? last.replace(/@/g, ".") : last;
        if (value === null) {
            delete ref[last];
        } else {
            ref[last] = value;
        }
    };

    applyImportChanges = (results, applyChangesFor) => {
        const newChanges = {...this.state.changes};

        const newData = {...this.state.metaData.data};
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
                    newData.metaDataFields[key] = results.metaDataFields[key].value;
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
        const changes = Object.keys(applyChangesFor).filter(key => applyChangesFor[key]);
        const prefix = isEmpty(this.props.newMetaData) ? "" : "new_";

        const newMetaData = {...this.state.metaData, data: newData};
        this.setState({
                selectedTab: "connection",
                changes: newChanges,
                metaData: newMetaData,
                loaded: true
            },
            this.validate(newMetaData, this.props.configuration, this.state.type));

        if (changes.length > 0) {
            setFlash(I18n.t(`import.${prefix}applyImportChangesFlash`, {changes: changes.join(", ")}), "warning");

        }
    };

    renderActions = (revisionNote) => {
        if (this.props.currentUser.guest) {
            return null;
        }
        const {errors} = this.state;
        const hasErrors = Object.keys(errors)
            .find(key => Object.keys(errors[key]).find(subKey => errors[key][subKey])) !== undefined;
        return (
            <section className="actions">
                <section className="notes">
                    <label htmlFor="revisionnote">{I18n.t("metadata.revisionnote")}</label>
                    <input name="revisionnote" type="text" value={revisionNote}
                           onChange={e => this.setState({revisionNote: e.target.value})}/>
                </section>
                <section className="buttons">
                    <a className="button" onClick={e => {
                        stop(e);
                        this.setState({
                            cancelDialogAction: () => this.props.history.replace("/search"),
                            confirmationDialogAction: () => this.setState({confirmationDialogOpen: false}),
                            confirmationDialogOpen: true,
                            leavePage: true
                        });
                    }}>{I18n.t("metadata.cancel")}</a>
                    <a className={`button ${hasErrors ? "grey disabled" : "blue"}`} onClick={e => {
                        stop(e);
                        if (hasErrors) {
                            return false;
                        }
                        const promise = this.state.isNew ? save : update;
                        const metaData = this.state.metaData;
                        metaData.data.revisionnote = isEmpty(revisionNote) ? "No revision note" : revisionNote;
                        promise(metaData)
                            .then(json => {
                                if (json.exception) {
                                    debugger;
                                    setFlash(json.validations, "error");
                                } else {
                                    this.props.history.replace("/search");
                                    const name = json.data.metaDataFields["name:en"] || json.data.metaDataFields["name:nl"] || "this service";
                                    setFlash(I18n.t("metadata.flash.updated", {
                                        name: name,
                                        revision: json.revision.number
                                    }));
                                }
                            });
                    }}>{I18n.t("metadata.submit")}</a>
                </section>
            </section>
        );
    };

    renderTab = tab => {
        const tabErrors = this.state.errors[tab] || {};
        const tabChanges = this.state.changes[tab] || false;
        const hasChanges = tabChanges ? "changes" : "";
        const className = this.state.selectedTab === tab ? "active" : "";
        const hasErrors = Object.keys(tabErrors).find(key => tabErrors[key] === true) !== undefined ? "errors" : "";
        return <span key={tab} className={`${className} ${hasErrors} ${hasChanges}`}
                     onClick={this.switchTab(tab)}>{I18n.t(`metadata.tabs.${tab}`)}
            {hasErrors && <i className="fa fa-warning"></i>}
            {(!hasErrors && tabChanges) && <i className="fa fa-asterisk"></i>}
                   </span>;
    };


    renderCurrentTab = (tab, metaData, whiteListing, revisions) => {
        const configuration = this.props.configuration.find(conf => conf.title === this.state.type);
        const guest = this.props.currentUser.guest;
        const {isNew, originalEntityId} = this.state;
        const name = metaData.data.metaDataFields["name:en"] || metaData.data.metaDataFields["name:nl"] || "this service";
        switch (tab) {
            case "connection" :
                return <Connection metaData={metaData} onChange={this.onChange("connection")}
                                   onError={this.onError("connection")}
                                   errors={this.state.errors["connection"]}
                                   guest={guest} isNew={isNew} originalEntityId={originalEntityId}/>;
            case "whitelist" :
                return <WhiteList whiteListing={whiteListing} name={name}
                                  allowedEntities={metaData.data.allowedEntities}
                                  allowedAll={metaData.data.allowedall} type={metaData.type}
                                  onChange={this.onChange("whitelist")}
                                  entityId={metaData.data.entityid} guest={guest}/>;
            case "metadata":
                return <MetaData metaDataFields={metaData.data.metaDataFields} configuration={configuration}
                                 onChange={this.onChange("metadata")} name={name} onError={this.onError("metadata")}
                                 errors={this.state.errors["metadata"]} guest={guest}/>;
            case "arp":
                return <ARP arp={metaData.data.arp} arpConfiguration={configuration.properties.arp}
                            onChange={this.onChange("arp")} guest={guest}/>;
            case "manipulation":
                return <Manipulation content={metaData.data.manipulation}
                                     onChange={this.onChange("manipulation")} guest={guest}/>;
            case "consent_disabling":
                return <ConsentDisabling disableConsent={metaData.data.disableConsent} name={name}
                                         whiteListing={whiteListing} onChange={this.onChange("consent_disabling")}
                                         guest={guest}/>;
            case "revisions":
                return <Revisions revisions={revisions} isNew={isNew}/>;
            case "export":
                return <Export metaData={metaData}/>;
            case "import":
                return <Import metaData={metaData}
                               guest={guest}
                               newEntity={false}
                               applyImportChanges={this.applyImportChanges}/>;
            default:
                throw new Error(`Unknown tab ${tab}`);
        }
    };

    render() {
        const {
            loaded, notFound, metaData, whiteListing, revisions, selectedTab, revisionNote,
            confirmationDialogOpen, confirmationDialogAction, cancelDialogAction, leavePage, isNew
        } = this.state;
        const type = metaData.type;
        const tabs = type === "saml20_sp" ? tabsSp : tabsIdP;

        const renderNotFound = loaded && notFound;
        const renderContent = loaded && !notFound;

        const name = renderContent ? this.nameOfMetaData(metaData) : "";
        const typeMetaData = I18n.t(`metadata.${type}_single`);

        return (
            <div className="detail-metadata">
                <ConfirmationDialog isOpen={confirmationDialogOpen}
                                    cancel={cancelDialogAction}
                                    confirm={confirmationDialogAction}
                                    question={leavePage ? undefined : I18n.t("metadata.deleteConfirmation", {name: name})}
                                    leavePage={leavePage}/>
                {renderContent &&
                <section className="top-detail">
                    <section className="info">{`${typeMetaData} - ${name}`}</section>
                    {!isNew && <a className="button red delete-metadata" onClick={e => {
                        stop(e);
                        this.setState({
                            confirmationDialogAction: () => {
                                remove(this.state.metaData).then(res => {
                                    this.props.history.replace("/search");
                                    const name = this.nameOfMetaData(this.state.metaData);
                                    setFlash(I18n.t("metadata.flash.deleted", {name: name}));
                                });
                            },
                            cancelDialogAction: () => this.setState({confirmationDialogOpen: false}),
                            confirmationDialogOpen: true,
                            leavePage: false
                        });
                    }}>{I18n.t("metadata.remove")}</a>}
                </section>
                }
                {renderNotFound && <section>{I18n.t("metadata.notFound")}</section>}
                {!notFound && <section className="tabs">
                    {tabs.map(tab => this.renderTab(tab))}
                </section>}
                {renderContent && this.renderCurrentTab(selectedTab, metaData, whiteListing, revisions)}
                {renderContent && this.renderActions(revisionNote)}
            </div>
        );
    }
}

Detail.propTypes = {
    history: PropTypes.object.isRequired,
    currentUser: PropTypes.object.isRequired,
    configuration: PropTypes.array.isRequired,
    newMetaData: PropTypes.object
};

