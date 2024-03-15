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
    changeRequests,
    detail,
    getAllowedLoas,
    idpPolicies,
    policyAttributes,
    provisioningById,
    relyingPartiesByResourceServer,
    remove,
    revisions,
    save,
    search,
    spPolicies,
    template,
    update,
    whiteListing
} from "../api";
import {isEmpty, stop} from "../utils/Utils";
import {setFlash} from "../utils/Flash";

import "./Detail.scss";
import ResourceServers from "../components/metadata/ResourceServers";
import Stepup from "../components/metadata/Stepup";
import ReactTooltip from "react-tooltip";
import {getConnectedEntities} from "../utils/TabNumbers";
import withRouterHooks from "../utils/RouterBackwardCompatability";
import MetaDataChangeRequests from "../components/metadata/MetaDataChangeRequests";
import RelyingParties from "../components/metadata/RelyingParties";
import ProvisioningApplications from "../components/metadata/ProvisioningApplications";
import AutoRefresh from "../components/metadata/AutoRefresh";
import PolicyForm from "../components/metadata/PolicyForm";
import {getNameForLanguage, getOrganisationForLanguage} from "../utils/Language";
import Policies from "../components/metadata/Policies";

let tabsSp = [
    "connection",
    "connected_idps",
    "metadata",
    "arp",
    "whitelist",
    "policies",
    "manipulation",
    "requests",
    "revisions",
    "import",
    "export"
];

let tabsIdP = [
    "connection",
    "whitelist",
    "consent_disabling",
    "stepup_entities",
    "metadata",
    "policies",
    "manipulation",
    "requests",
    "revisions",
    "import",
    "export"
];

const tabsRp = [
    "connection",
    "connected_idps",
    "metadata",
    "resource_servers",
    "arp",
    "whitelist",
    "policies",
    "manipulation",
    "requests",
    "revisions",
    "import",
    "export"
];

const tabsRs = [
    "connection",
    "metadata",
    "connected_rps",
    "requests",
    "revisions",
    "import",
    "export"
];

const tabsPr = [
    "connection",
    "metadata",
    "connected_applications",
    "revisions"
];

const tabsPolicy = [
    "policy_form",
    "revisions"
];

const tabsSingleTenant = [
    "connection",
    "metadata",
    "arp",
    "revisions",
    "import",
    "export"
];


const aliasTabChanges = {
    "mfa_entities": "stepup_entities"
}

class Detail extends React.PureComponent {

    constructor(props) {
        super(props);
        const type = isEmpty(props.newMetaData)
            ? this.props.params.type
            : props.newMetaData.connection.type.value.replace(/-/g, "_");
        let {tab = "connection"} = this.props.params;
        if (tab === "connection" && type === "policy") {
            tab = "policy_form";
        }
        const id = isEmpty(props.newMetaData) ? this.props.params.id : "new";
        this.state = {
            metaData: {},
            whiteListing: [],
            resourceServers: [],
            relyingParties: [],
            applications: [],
            policies: [],
            revisions: [],
            requests: [],
            provisioningGroups: [],
            notFound: false,
            loaded: false,
            selectedTab: tab,
            revisionNote: "",
            revisionNoteClone: "",
            confirmationDialogOpen: false,
            confirmationDialogAction: () => this,
            cancelDialogAction: () => this,
            leavePage: false,
            errors: {},
            changes: {},
            originalEntityId: undefined,
            type: type,
            id: id,
            isNew: id === "new",
            revisionNoteError: false,
            addedWhiteListedEntities: [],
            removedWhiteListedEntities: [],
            whiteListingLoaded: false,
            changeRequestsLoaded: false,
            initial: true
        };
    }

    componentDidMount() {
        window.scrollTo(0, 0);
        const {newMetaData, currentUser} = this.props;
        let {isNew, type, id} = this.state;
        const promise = isNew ? template(type) : detail(type, id);
        promise
            .then(metaData => {
                const isSp = type === "saml20_sp" || type === "oidc10_rp";
                const isIdp = type === "saml20_idp";
                const isOidcRP = type === "oidc10_rp";
                const isResourceServer = type === "oauth20_rs";
                const isProvisioning = type === "provisioning";
                const isPolicy = type === "policy";
                const whiteListingType = isSp ? "saml20_idp" : "saml20_sp";
                const errorKeys = isSp ? tabsSp : isProvisioning ? tabsPr : isPolicy ? tabsPolicy : tabsIdP;
                const autoRefreshFeature = "AUTO_REFRESH";
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
                        "user",
                        "name"
                    ];
                    metaData.id = undefined;
                    metaData.revision = undefined;
                    clonedClearFields.forEach(attr => delete metaData.data[attr]);
                    id = undefined;
                }
                this.setState({
                    metaData: metaData,
                    revisionNoteClone: metaData.data.revisionnote,
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
                    }, {})
                });
                if (!isEmpty(newMetaData)) {
                    this.applyImportChanges(newMetaData, {
                        connection: true,
                        metaDataFields: true,
                        allowedEntities: true,
                        allowedResourceServers: true,
                        stepupEntities: true,
                        disableConsent: true,
                        arp: true
                    });
                } else {
                    this.validate(metaData, this.props.configuration, type);
                }
                const state = (!isEmpty(newMetaData) && !isEmpty(newMetaData.connection) && !isEmpty(newMetaData.connection.state)
                    && newMetaData.connection.state.selected) ? newMetaData.connection.state.value : metaData.data.state;
                const promise = isPolicy ? Promise.resolve([]) : whiteListing(whiteListingType, state);
                promise.then(whiteListing => {
                    this.setState({whiteListing: whiteListing, whiteListingLoaded: true});
                    if (isOidcRP) {
                        allResourceServers(state).then(json =>
                            this.setState({resourceServers: json})
                        );
                    }
                    if (isIdp && !isNew) {
                        idpPolicies(metaData.data.entityid).then(policies => this.setState({policies: policies}))
                    }
                    if (isSp && !isNew) {
                        spPolicies(metaData.data.entityid).then(policies => this.setState({policies: policies}))

                    }
                    if (isPolicy) {
                        const options = {"REQUESTED_ATTRIBUTES": ["metaDataFields.coin:policy_enforcement_decision_required"]};
                        Promise.all([
                            search({}, "saml20_idp"),
                            search(options, "saml20_sp"),
                            search(options, "oidc10_rp"),
                            policyAttributes(),
                            getAllowedLoas()
                        ]).then(res => {
                            this.setState({
                                identityProviders: res[0],
                                serviceProviders: res[1].concat(res[2]),
                                policyAttributes: res[3],
                                allowedLoas: res[4]
                            })
                        })

                    }
                    if (isResourceServer && !isNew) {
                        relyingPartiesByResourceServer(metaData.data.entityid).then(res => {
                            this.setState({"relyingParties": res})
                        });
                    }
                    if (isSp && !isNew) {
                        provisioningById(id).then(res => {
                           this.setState({provisioningGroups: res});
                        });
                    }
                    Promise.all([revisions(type, id), changeRequests(type, id)])
                        .then(results => {
                            const revisions = results[0];
                            revisions.push(metaData);
                            revisions.sort((r1, r2) =>
                                r1.revision.number < r2.revision.number
                                    ? 1
                                    : r1.revision.number > r2.revision.number
                                        ? -1
                                        : 0
                            );
                            const requests = results[1];
                            requests.forEach(cr => cr.createdAt = new Date(cr.created));
                            requests.sort((a, b) => b.createdAt - a.createdAt);
                            this.setState({revisions: revisions, requests: requests, changeRequestsLoaded: true});
                        });
                });
                const configuration = this.props.configuration.find(conf => conf.title === type);
                if (currentUser.featureToggles && currentUser.featureToggles.includes(autoRefreshFeature) &&
                    (type === 'saml20_idp' || type === 'saml20_sp') && configuration.properties.autoRefresh) {

                    const autoRefreshTab = "auto_refresh";
                    const metadataTab = "metadata";
                    if (type === 'saml20_sp' && !tabsSp.includes(autoRefreshTab)) {
                        const metadataTabIndex = tabsSp.indexOf(metadataTab);
                        tabsSp.splice(metadataTabIndex + 1, 0, autoRefreshTab);
                    }
                    if (type === 'saml20_idp' && !tabsIdP.includes(autoRefreshTab)) {
                        const metadataTabIndex = tabsIdP.indexOf(metadataTab);
                        tabsIdP.splice(metadataTabIndex + 1, 0, autoRefreshTab);
                    }
                }
            })
            .catch(err => {
                if (err.response && err.response.status === 404) {
                    this.setState({notFound: true, loaded: true});
                } else {
                    throw err;
                }
            });
    }

    refreshWhiteListing = () => {
        const {type, metaData} = this.state;
        const isSp = type === "saml20_sp" || type === "oidc10_rp";
        const isOidcRP = type === "oidc10_rp";
        const whiteListingType = isSp ? "saml20_idp" : "saml20_sp";
        whiteListing(whiteListingType, metaData.data.state).then(whiteListing => {
            this.setState({whiteListing: whiteListing});
            if (isOidcRP) {
                allResourceServers(metaData.data.state).then(json =>
                    this.setState({resourceServers: json})
                );
            }
        });
    };

    validate = (metaData, configurations, type) => {
        const configuration = configurations.find(conf => conf.title === type);
        const requiredMetaData = configuration.properties.metaDataFields.required;
        const metaDataFields = metaData.data.metaDataFields;
        const currentErrors = {...this.state.errors};
        const metaDataErrors = currentErrors.metadata || {};
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
        const connectionErrors = currentErrors.connection || {};
        const policyFormErrors = currentErrors.policy_form || {};
        const required = configuration.required;
        if ("policy" !== type) {
            Object.keys(metaData.data).forEach(key => {
                if (isEmpty(metaData.data[key]) && required.indexOf(key) > -1) {
                    connectionErrors[key] = true;
                }
            });
            required.forEach(req => {
                if (isEmpty(metaData.data[req])) {
                    connectionErrors[req] = true;
                }
            });
        } else {
            required.forEach(req => {
                if (isEmpty(metaData.data[req])) {
                    policyFormErrors[req] = true;
                }
            });
        }
        const newErrors = {
            currentErrors,
            connection: connectionErrors,
            metadata: metaDataErrors,
            policy_form: policyFormErrors
        };
        this.setState({errors: newErrors});
    };

    switchTab = tab => e => {
        stop(e);
        this.setState({selectedTab: tab});
        const {type, id} = this.state;
        if (!this.props.fromImport) {
            this.props.navigate(`/metadata/${type}/${id}/${tab}`);
        }
    };

    onError = name => (key, isError) => {
        const errors = {...this.state.errors};
        errors[name][key] = isError;
        this.setState({errors: errors});
    };

    nameOfMetaData = metaData =>
        metaData.data.name || getNameForLanguage(metaData.data.metaDataFields) || metaData.data["entityid"];

    organisationOfMetaData = metaData =>
        getOrganisationForLanguage(metaData.data.metaDataFields) || "";

    onChange = component => (
        name,
        value,
        replaceAtSignWithDotsInName = false,
        callback = undefined
    ) => {
        const currentState = this.state.metaData;
        const metaData = {
            ...currentState,
            data: {...currentState.data},
            arp: {...currentState.arp},
            metaDataFields: {...currentState.metaDataFields}
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
        const changes = {...this.state.changes};
        changes[aliasTabChanges[component] || component] = true;
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
        this.setState({metaData: metaData, changes: changes}, () => {
            callback && typeof callback === "function" && callback();
            if (component === "connection" && name === "data.state") {
                this.refreshWhiteListing();
            }
        });
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
        parts.forEach(part => {
            if (isEmpty(ref[part])) {
                ref[part] = {};
            }
            ref = ref[part];
        });
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
        [
            "allowedEntities",
            "disableConsent",
            "stepupEntities",
            "mfaEntities",
            "arp",
            "allowedResourceServers"
        ].forEach(name => {
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
                if (name === "stepupEntities") {
                    newChanges.stepup_entities = true;
                }
                if (name === "mfaEntities") {
                    newChanges.mfa_entities = true;
                }
                if (name === "allowedResourceServers") {
                    newChanges.resource_servers = true;
                }
            }
        });
        if (applyChangesFor["metaDataFields"] && results["metaDataFields"]) {
            Object.keys(results.metaDataFields).forEach(key => {
                if (results.metaDataFields[key].selected) {
                    if (!isEmpty(results.metaDataFields[key].value)) {
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

        const newMetaData = {...this.state.metaData, data: newData};
        this.setState(
            {
                selectedTab: "connection",
                changes: newChanges,
                metaData: newMetaData,
                loaded: true
            },
            () => {
                this.validate(newMetaData, this.props.configuration, this.state.type);
                this.refreshWhiteListing();
            }
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

    sanitizeMetaData = metaData => {
        if (metaData.type === "policy") {
            //Remove empty attributes and loas
            if (metaData.data.type === "step") {
                metaData.data.loas.forEach(loa => {
                    loa.attributes = loa.attributes.filter(attribute => !isEmpty(attribute.value))
                });
            } else {
                metaData.data.attributes = metaData.data.attributes.filter(attribute => !isEmpty(attribute.value));
            }
        }
    }

    submit = e => {
        stop(e);
        const {errors, revisionNote} = this.state;
        const hasErrors = this.hasGlobalErrors(errors);
        if (isEmpty(revisionNote)) {
            this.setState({revisionNoteError: true}, () =>
                this.revisionNote.focus()
            );
            return false;
        }
        if (hasErrors) {
            this.setState({initial: false})
            return false;
        }
        this.setState({revisionNoteError: false});
        const promise = this.state.isNew ? save : update;
        const metaData = this.state.metaData;
        metaData.data.revisionnote = revisionNote;
        this.sanitizeMetaData(metaData);
        promise(metaData).then(json => {
            if (json.exception || json.error) {
                setFlash(json.validations, "error");
                window.scrollTo(0, 0);
            } else {
                const name = json.data.name || getNameForLanguage(json.data.metaDataFields) || "this service";
                setFlash(
                    I18n.t("metadata.flash.updated", {
                        name: name,
                        revision: json.revision.number
                    })
                );
                const path = encodeURIComponent(`/metadata/${json.type}/${json.id}/${this.state.selectedTab}`);
                this.props.navigate(`/refresh-route/${path}`, {replace: true});
            }
        });
    };

    renderWarningNonExistentAllowedEntities = () => {
        const {whiteListing = [], metaData} = this.state;
        const allowedEntities = (metaData.data || {}).allowedEntities || [];
        const names = allowedEntities.map(e => e.name);
        const existingNames = whiteListing.map(w => w.data.entityid);
        return names.filter(name => existingNames.indexOf(name) < 0);
    }

    renderActions = revisionNote => {
        const {errors, revisionNoteError} = this.state;
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
                            onChange={e => this.setState({revisionNote: e.target.value})}
                        />
                    </section>
                    {revisionNoteRequired && (
                        <em className="error">{I18n.t("metadata.revisionnoteRequired")}</em>
                    )}
                    {/*{nonExistentAllowedEntities.length > 0 && (*/}
                    {/*  <em className="error">The `allowed entities` contains non-existent entities: {nonExistentAllowedEntities.join(", ")}</em>*/}
                    {/*)}*/}
                </section>
                <section className="buttons">
                    <a
                        className="button"
                        onClick={e => {
                            stop(e);
                            this.setState({
                                cancelDialogAction: () => this.props.navigate(`/search`),
                                confirmationDialogAction: () =>
                                    this.setState({confirmationDialogOpen: false}),
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

    hasGlobalErrors = errors => {
        const hasErrors = Object.keys(errors).find(key =>
            Object.keys(errors[key]).find(subKey => errors[key][subKey])
        ) !== undefined;
        return hasErrors;
    }

    renderTabTitle = (tab, metaData, resourceServers, whiteListing, revisions, requests, relyingParties, policies) => {
        const allowedAll = metaData.data.allowedall;
        const allowedEntities = metaData.data.allowedEntities;
        const applications = metaData.data.applications;
        let args = {};
        switch (tab) {
            case "connection":
            case "metadata":
            case "import":
            case "export":
                break;
            case "connected_idps":
                const connectedEntities = getConnectedEntities(whiteListing, allowedAll, allowedEntities, metaData.data.entityid, metaData.data.state);
                args = {nbr: connectedEntities.length};
                break;
            case "arp":
                args = {info: (metaData.data.arp || {}).enabled ? "" : " (no)"};
                break;
            case "whitelist":
                args = {info: allowedAll ? " (*)" : allowedEntities.length === 0 ? " (-)" : ` (${allowedEntities.length})`};
                break;
            case "manipulation":
                args = {info: isEmpty(metaData.data.manipulation) ? "" : " (1)"}
                break;
            case "revisions":
                args = {nbr: (revisions || []).filter(rev => rev.id).length};
                break;
            case "requests":
                args = {nbr: (requests || []).filter(rev => rev.id).length};
                break;
            case "consent_disabling":
                args = {nbr: (metaData.data.disableConsent || []).length};
                break;
            case "stepup_entities":
                args = {nbr: (metaData.data.stepupEntities || []).length + (metaData.data.mfaEntities || []).length};
                break;
            case "resource_servers":
                args = {nbr: (metaData.data.allowedResourceServers || []).length};
                break;
            case "connected_rps":
                args = {nbr: (relyingParties || []).length};
                break;
            case "policies":
                args = {nbr: (policies || []).length};
                break;
            case "connected_applications":
                args = {nbr: (applications || []).length};
                break;
            default:
                args = {};
        }
        return I18n.t(`metadata.tabs.${tab}`, args);
    }

    renderTab = (tab, metaData, resourceServers, whiteListing, revisions, requests, relyingParties, policies) => {
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
                onClick={this.switchTab(tab)}>
        {this.renderTabTitle(tab, metaData, resourceServers, whiteListing, revisions, requests, relyingParties, policies)}
                {hasErrors && <i className="fa fa-warning"/>}
                {!hasErrors && tabChanges && <i className="fa fa-asterisk"/>}
      </span>
        );
    };

    renderCurrentTab = (
        tab,
        metaData,
        resourceServers,
        whiteListing,
        revisions,
        requests,
        revisionNoteClone,
        changeRequestsLoaded,
        relyingParties,
        policies,
        identityProviders,
        serviceProviders,
        policyAttributes,
        allowedLoas,
        initial
    ) => {
        const configuration = this.props.configuration.find(
            conf => conf.title === this.state.type
        );
        const {currentUser} = this.props
        const {guest} = currentUser;
        const {
            isNew,
            originalEntityId,
            type,
            removedWhiteListedEntities,
            addedWhiteListedEntities,
            provisioningGroups,
            errors
        } = this.state;
        const name = metaData.data.name || getNameForLanguage(metaData.data.metaDataFields) || "this service";
        switch (tab) {
            case "connection":
                return (
                    <Connection
                        metaData={metaData}
                        revisionNote={revisionNoteClone}
                        onChange={this.onChange("connection")}
                        onError={this.onError("connection")}
                        errors={errors["connection"]}
                        guest={guest}
                        isNew={isNew}
                        originalEntityId={originalEntityId}
                        configuration={configuration}
                        provisioningGroups={provisioningGroups}
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
                        isNewEntity={this.state.isNew}
                        originalEntityId={this.state.originalEntityId}
                        onError={this.onError("metadata")}
                        errors={errors["metadata"]}
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
                        currentUser={currentUser}
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
            case "stepup_entities":
                return (
                    <Stepup
                        stepupEntities={metaData.data.stepupEntities || []}
                        mfaEntities={metaData.data.mfaEntities || []}
                        allowedEntities={metaData.data.allowedEntities}
                        allowedAll={metaData.data.allowedall}
                        name={name}
                        whiteListing={whiteListing}
                        guest={guest}
                        onChange={this.onChange("stepup_entities")}
                        onChangeMfa={this.onChange("mfa_entities")}
                        loaLevels={configuration.properties.stepupEntities.items.properties.level.enum}
                        mfaLevels={configuration.properties.mfaEntities.items.properties.level.enum}
                    />
                );
            case "revisions":
                return (
                    <Revisions
                        revisions={revisions}
                        firstRevisionNote={revisionNoteClone}
                        isNew={isNew}
                        entityType={type}
                    />
                );
            case "export":
                return <Export metaData={metaData}/>;
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
            case "requests":
                return (
                    <MetaDataChangeRequests
                        requests={requests}
                        metaData={metaData}
                        changeRequestsLoaded={changeRequestsLoaded}
                        entityType={type}
                    />
                )
            case "connected_rps":
                return (
                    <RelyingParties
                        relyingParties={relyingParties}
                        name={name}
                    />
                );
            case "connected_applications":
                return (
                    <ProvisioningApplications
                        allowedApplications={metaData.data.applications}
                        onChange={this.onChange("applications")}
                        entityId={metaData.data.entityid}
                        applications={whiteListing}
                        name={name}/>
                );
            case "policies":
                return (
                    <Policies
                        policies={policies}
                        metaData={metaData}
                        name={name}
                    />
                );
            case "auto_refresh":
                return (
                    <AutoRefresh
                        metadataAutoRefresh={metaData.data.autoRefresh}
                        autoRefreshConfiguration={configuration.properties.autoRefresh}
                        metaDataUrl={metaData.data.metadataurl}
                        onChange={this.onChange("autoRefresh")}
                        guest={guest}
                    />
                );
            case "policy_form":
                return (
                    <PolicyForm identityProviders={identityProviders}
                                serviceProviders={serviceProviders}
                                policyAttributes={policyAttributes}
                                allowedLoas={allowedLoas}
                                data={metaData.data}
                                isNew={isNew}
                                onChange={this.onChange("policy_form")}
                                onError={this.onError("policy_form")}/>
                );
            default:
                throw new Error(`Unknown tab ${tab}`);
        }
    };

    renderHasChangeRequests = () => {
        return (
            <div className={"change-requests"}>
                <span>
                    {I18n.t("metadata.changeRequestsPre")}
                    <a href="/requests" onClick={e => {
                        stop(e);
                        this.setState({selectedTab: "requests"});
                    }}>
                        {I18n.t("metadata.changeRequestsLink")}
                    </a>
                    {I18n.t("metadata.changeRequestsPost")}
                </span>
            </div>)
    }

    renderErrors = errors => {
        const allErrors = {...errors};
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

    renderTopBanner = (name, organization, metaData, resourceServers, whiteListing, isNew, whiteListingLoaded) => {
        const type = metaData.type;
        const {allowedall, state, allowedEntities, entityid} = metaData.data;
        const typeMetaData = I18n.t(`metadata.${type}_single`);
        const isSp = type === "saml20_sp";
        const isRp = type === "oidc10_rp";
        const isRs = type === "oauth20_rs";
        const isProvisioning = type === "provisioning";
        const isSingleTenantTemplate = type === "single_tenant_template";
        const isPolicy = type === "policy";
        const nonExistentAllowedEntities = this.renderWarningNonExistentAllowedEntities();
        const importedFromEdugain = metaData.data.metaDataFields["coin:imported_from_edugain"];
        const excludedFromPush = metaData.data.metaDataFields["coin:exclude_from_push"];
        const pushEnabled = metaData.data.metaDataFields["coin:push_enabled"];
        const isActive = metaData.data.active;
        const connectedEntities = whiteListing
            .filter(idp => idp.data.allowedall || (idp.data.allowedEntities || []).some(entity => entity.name === entityid))
            .filter(idp => idp.data.state === state)
            .filter(idp => allowedall || (allowedEntities || []).some(entity => entity.name === idp.data.entityid));
        const connectedApplications = metaData.data.applications;
        const isTrue = I18n.t("topBannerDetails.isTrue");
        const isFalse = I18n.t("topBannerDetails.isFalse");
        return (
            <section className="info">
                <table className={`${type} ${importedFromEdugain ? "imported-from-edugain" : ""}`}>
                    <thead>
                    <tr>
                        <th>{I18n.t("topBannerDetails.name")}</th>
                        {!isPolicy && <th>{I18n.t("topBannerDetails.organization")}</th>}
                        <th>{I18n.t("topBannerDetails.type")}</th>
                        {!isPolicy && <th>{I18n.t("topBannerDetails.workflow")}</th>}
                        {isPolicy && <th>{I18n.t("topBannerDetails.policyType")}</th>}
                        {(isSp || isRp) && <th>
                            {I18n.t("topBannerDetails.reviewState")}
                            {excludedFromPush && <span className="info">
                            <i className="fa fa-info-circle" data-for="push-excluded-tooltip" data-tip/>
                            <ReactTooltip id="push-excluded-tooltip" type="info" class="tool-tip" effect="solid">
                                <span dangerouslySetInnerHTML={{__html: I18n.t("topBannerDetails.pushExcludedTooltip")}}/>
                            </ReactTooltip>
              </span>}
                        </th>}
                        {isPolicy && <th>
                            {I18n.t("topBannerDetails.reviewState")}
                            {!isActive && <span className="info">
                                <i className="fa fa-info-circle" data-for="not-active-tooltip" data-tip/>
                                <ReactTooltip id="not-active-tooltip" type="info" class="tool-tip" effect="solid">
                                    <span dangerouslySetInnerHTML={{__html: I18n.t("topBannerDetails.notActiveTooltip")}}/>
                                </ReactTooltip>
                        </span>}
                        </th>}
                        {importedFromEdugain && <th>{I18n.t("topBannerDetails.edugainImported")}</th>}
                        {importedFromEdugain && <th>
                            {I18n.t("topBannerDetails.pushEnabled")}
                            <i className="fa fa-info-circle" data-for="push-enabled-tooltip" data-tip/>
                            <ReactTooltip id="push-enabled-tooltip" type="info" class="tool-tip" effect="solid">
                                <span
                                    dangerouslySetInnerHTML={{__html: I18n.t("topBannerDetails.pushEnabledTooltip")}}/>
                            </ReactTooltip>
                        </th>}
                    </tr>
                    </thead>
                    <tbody>
                    <tr>
                        <td>{name}</td>
                        {!isPolicy && <td>{organization}</td>}
                        <td>{typeMetaData}</td>
                        {!isPolicy && <td className={state === "prodaccepted" ? "green" : "orange"}>{state}</td>}
                        {isPolicy && <td>{I18n.t(`topBannerDetails.${metaData.data.type}`)}</td>}
                        {(isSp || isRp) && <td className={excludedFromPush ? "orange" : "green"}>
                            {excludedFromPush ? I18n.t("topBannerDetails.staging") : I18n.t("topBannerDetails.production")}
                        </td>}
                        {isPolicy && <td className={!isActive ? "orange" : "green"}>
                            {isActive ? I18n.t("topBannerDetails.active") : I18n.t("topBannerDetails.notActive")}
                        </td>}
                        {importedFromEdugain && <td className={"blue"}>{isTrue}</td>}
                        {importedFromEdugain && <td className={"blue"}>{pushEnabled ? isTrue : isFalse}</td>}
                    </tr>
                    </tbody>
                </table>
                {(!isEmpty(nonExistentAllowedEntities) && !isSingleTenantTemplate && !isRs
                        && !isPolicy
                    && !isNew && whiteListingLoaded) &&
                <section className="warning">
                    <i className="fa fa-exclamation-circle"></i>
                    <span>{I18n.t("topBannerDetails.unknownEntitiesConnected", {
                        type: typeMetaData,
                        entities: nonExistentAllowedEntities.join(", ")
                    })}</span>
                </section>}
                {(isEmpty(connectedEntities) && !isSingleTenantTemplate && !isNew && !isRs
                        && whiteListingLoaded && !isProvisioning) && !isPolicy &&
                <section className="warning">
                    <i className="fa fa-exclamation-circle"></i>
                    <span>{I18n.t("topBannerDetails.noEntitiesConnected", {type: typeMetaData})}</span>
                </section>}
                {(isEmpty(connectedApplications) && !isNew && whiteListingLoaded && isProvisioning) &&
                <section className="warning">
                    <i className="fa fa-exclamation-circle"></i>
                    <span>{I18n.t("topBannerDetails.noApplicationsConnected", {type: typeMetaData})}</span>
                </section>}
            </section>
        );
    }

    render() {
        const {
            loaded,
            notFound,
            metaData,
            resourceServers,
            relyingParties,
            policies,
            identityProviders,
            serviceProviders,
            policyAttributes,
            allowedLoas,
            initial,
            whiteListing,
            revisions,
            requests,
            selectedTab,
            revisionNote,
            confirmationDialogOpen,
            confirmationDialogAction,
            cancelDialogAction,
            leavePage,
            isNew,
            errors,
            revisionNoteClone,
            changeRequestsLoaded,
            whiteListingLoaded
        } = this.state;

        const type = metaData.type;

        const tabs = (() => {
            switch (type) {
                case "saml20_sp":
                    return tabsSp;
                case "saml20_idp":
                    return tabsIdP;
                case "oidc10_rp":
                    return tabsRp;
                case "oauth20_rs":
                    return tabsRs;
                case "single_tenant_template":
                    return tabsSingleTenant;
                case "provisioning":
                    return tabsPr;
                case "policy":
                    return tabsPolicy;
                default:
                    return [];
            }
        })();

        const renderNotFound = loaded && notFound;
        const renderContent = loaded && !notFound;

        const name = renderContent ? this.nameOfMetaData(metaData) : "";
        const organization = renderContent ? this.organisationOfMetaData(metaData) : "";

        const hasErrors = this.hasGlobalErrors(errors) && !isEmpty(metaData.id);

        const allowedDelete = !isNew;

        return (
            <div className="detail-metadata">
                <ConfirmationDialog
                    isOpen={confirmationDialogOpen}
                    cancel={cancelDialogAction}
                    confirm={confirmationDialogAction}
                    question={
                        leavePage
                            ? undefined
                            : I18n.t("metadata.deleteConfirmation", {name: name})
                    }
                    leavePage={leavePage}
                />
                {renderContent && (
                    <section className="top-detail">
                        <section className="inner-detail">
                            {this.renderTopBanner(name, organization, metaData, resourceServers, whiteListing,
                                isNew, whiteListingLoaded)}
                            {hasErrors && this.renderErrors(errors)}
                            {allowedDelete && (
                                <a
                                    className="button red delete-metadata"
                                    onClick={e => {
                                        stop(e);
                                        this.setState({
                                            confirmationDialogAction: () => {
                                                remove(this.state.metaData, this.state.revisionNote).then(res => {
                                                    const name = this.nameOfMetaData(this.state.metaData);
                                                    setFlash(
                                                        I18n.t("metadata.flash.deleted", {name: name})
                                                    );
                                                    this.props.navigate(`/search`);
                                                });
                                            },
                                            cancelDialogAction: () =>
                                                this.setState({confirmationDialogOpen: false}),
                                            confirmationDialogOpen: true,
                                            leavePage: false
                                        });
                                    }}
                                >
                                    {I18n.t("metadata.remove")}
                                </a>
                            )}
                            {allowedDelete && (
                                <a
                                    className="button green clone-metadata"
                                    onClick={e => {
                                        stop(e);
                                        setTimeout(() => {
                                            const name = metaData.data.name || getNameForLanguage(metaData.data.metaDataFields) || "this service";
                                            setFlash(I18n.t("metadata.flash.cloned", {name: name}));
                                        }, 50);
                                        const path = encodeURIComponent(`/clone/${type}/${metaData.id}`);
                                        this.props.navigate(`/refresh-route/${path}`);
                                    }}
                                >
                                    {I18n.t("metadata.clone")}
                                </a>
                            )}
                        </section>
                        {(changeRequestsLoaded && !isEmpty(requests)) && this.renderHasChangeRequests()}
                    </section>
                )}

                {renderNotFound && <section>{I18n.t("metadata.notFound")}</section>}
                {!notFound && (
                    <section className="tabs">
                        {tabs.map(tab => this.renderTab(tab, metaData, resourceServers, whiteListing, revisions, requests, relyingParties, policies))}
                    </section>
                )}
                {renderContent &&
                this.renderCurrentTab(
                    selectedTab,
                    metaData,
                    resourceServers,
                    whiteListing,
                    revisions,
                    requests,
                    revisionNoteClone,
                    changeRequestsLoaded,
                    relyingParties,
                    policies,
                    identityProviders,
                    serviceProviders,
                    policyAttributes,
                    allowedLoas,
                    initial
                )}
                {renderContent && this.renderActions(revisionNote)}
            </div>
        );
    }
}

export default withRouterHooks(Detail);

Detail.propTypes = {
    currentUser: PropTypes.object.isRequired,
    configuration: PropTypes.array.isRequired,
    clone: PropTypes.bool,
    fromImport: PropTypes.bool.isRequired,
    newMetaData: PropTypes.object
};
