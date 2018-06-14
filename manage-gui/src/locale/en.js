// Interpolation works as follows:
//
// Make a key with the translation and enclose the variable with {{}}
// ie "Hello {{name}}" Do not add any spaces around the variable name.
// Provide the values as: I18n.t("key", {name: "John Doe"})
import I18n from "i18n-js";

I18n.translations.en = {
    code: "EN",
    name: "English",
    select_locale: "Select English",
    EntityId: "",

    header: {
        title: "Manage",
        links: {
            help_html: "<a href=\"https://github.com/OpenConext/OpenConext-manage/wiki\" target=\"_blank\">Help</a>",
            logout: "Logout",
            exit: "Exit"
        },
        role: "Role"
    },

    navigation: {
        search: "Search",
        import: "Import",
        api: "API",
        system: "System"
    },

    metadata: {
        saml20_sp: "Service Providers",
        saml20_idp: "Identity Providers",
        saml20_sp_single: "Service Provider",
        saml20_idp_single: "Identity Provider",
        saml20_sp_revision_single: "Service Provider",
        saml20_idp_revision_single: "Identity Provider",
        single_tenant_template: "Single Tenant Templates",
        single_tenant_template_single: "Single Tenant Template",
        searchPlaceHolder: "Search for metadata",
        new: "New",
        tabs: {
            connection: "Connection",
            whitelist: "Whitelisting",
            metadata: "Metadata",
            metaDataFields: "Metadata fields",
            arp: "ARP",
            manipulation: "Manipulation",
            consent_disabling: "Consent Management",
            connected_idps: "Connected IdP's",
            revisions: "Revisions",
            import: "Import",
            export: "Export"
        },
        notFound: "No Metadata found",
        entityId: "Entity ID",
        entityIdAlreadyExists: "Entity ID {{entityid}} is already taken.",
        metaDataUrl: "Metadata URL",
        state: "State",
        prodaccepted: "prodaccepted",
        testaccepted: "testaccepted",
        all: "All",
        revision: "Revision",
        revisionInfo: "Revision {{number}} last updated by {{updatedBy}} on {{created}}",
        notes: "Notes",
        edit: "Edit",
        none: "",
        submit: "Submit",
        cancel: "Cancel",
        remove: "Delete",
        clone: "Clone",
        revisionnote: "Revision notes",
        revisionnoteRequired: "Revision notes are required.",
        flash: {
            updated: "{{name}} was successfully updated to revision {{revision}}",
            cloned: "{{name}} was successfully cloned. Submit to save the changes.",
            deleted: "{{name}} was successfully deleted",
            restored: "{{name}} revision {{revision}} was successfully restored to new revision {{newRevision}}",
        },
        required: "{{name}} is required",
        extraneous: "{{name}} is an unknown / extraneous key. Remove it as this can not be saved.",
        deleteConfirmation: "Are you sure you want to delete {{name}}?",
        errors: "There are validation errors:"

    },

    playground: {
        migration: "Migrate",
        validation: "Validate",
        orphans: "Referential integrity",
        extended_search: "Extended Search",
        push: "Push",
        push_preview: "Push Preview",
        pushPreviewInfo: "Collect all relevant metadata and perform a simulation of the {{name}} push",
        pushInfo: "Collect all relevant metadata and push the metadata to {{name}}. The metadata will be pushed to {{url}}.",
        pushedOk: "Metadata successfully pushed to {{name}}",
        pushedNotOk: "Error during the push of the Metadata to {{name}}",
        runPushPreview: "Preview MetaData",
        runPush: "Push MetaData",
        runMigration: "RUN MIGRATION",
        runValidation: "VALIDATE MIGRATION",
        runOrphans: "CHECK REFERENTIAL INTEGRITY",
        deleteOrphans: "DELETE REFERENCES",
        orphanConfirmation: "Are you sure you want to delete the above references?",
        migrationConfirmation: "Are you sure you want to run the migration? All current data will be deleted.",
        orphansDeleted: "References to deleted / renamed MetaData removed",
        pushConfirmation: "Are you sure you want to push all metadata to {{name}} with endpoint {{url}}?",
        headers: {
            status: "Status",
            entityid:"Entity ID",
            name: "Name",
            notes: "Notes"
        },
        no_results: "No results",
        error: "Invalid input: dangling meta character '*'",
        pushResults: {
            deltas: "Differences between the pre-push and the post-push EB sso_provider_roles_eb5 data.",
            noDeltas: "The post-push EB sso_provider_roles_eb5 data is identical to the pre-push data.",
            postPushValue: "Post-push value",
            prePushValue: "Pre-push value",
            attribute: "Attribute",
            entityId: "Entity-ID"
        }

    },
    whitelisting: {
        confirmationAllowAll:"Are you sure you want to allow all {{type}} access to '{{name}}'? You will have to add {{type}} one-by-one to selectively deny them again.",
        confirmationAllowNone:"Are you sure you want to deny all {{type}} access to '{{name}}'? You will have to add {{type}} one-by-one or allow them all again.",
        placeholder: "Search, select and add {{type}} to the whitelist",
        allowAllProviders: "Allow all {{type}} access to {{name}}",
        title: "{{type}} Whitelist",
        description: "Add only those {{type}} which are allowed to access {{name}}.",
        searchPlaceHolder_saml20_idp: "Search for whitelisted Service Providers...",
        searchPlaceHolder_saml20_sp: "Search for whitelisted Identity Providers...",
        allowedEntries: {
            blocked: "Blocked",
            status: "Status",
            entityid:"Entity ID",
            name: "Name",
            notes: "Notes"
        }
    },
    connectedIdps: {
        title: "{{type}} connected to {{name}}",
        description: "All {{type}} that are configured to allow access to {{name}}. If {{name}} is configured to not allow all {{type}} then only those {{type}} that are whitelisted are shown.",
        searchPlaceHolder: "Search for connected Identity Providers...",
        noConnections: "No {{type}} are connected. Most likely {{name}} is configured to whitelist none."

    },
    consentDisabling: {
        title: "Consent disabling",
        description: "Search and add Service Providers that will skip consent or need a custom consent message for '{{name}}'.",
        placeholder: "Search, select and add Service Providers to the consent-disabled-list",
        entries: {
            status: "Status",
            entityid:"Entity ID",
            name: "Name",
            no_consent: "No consent is required",
            minimal_consent: "Minimal consent is required",
            default_consent: "Default consent with custom message",
            consent_value: "Type of consent required",
            explanationNl: "Explanation EN",
            explanationEn: "Explanation NL"
        }
    },

    manipulation : {
        manipulationInfo: "Documentation on attribute manipulations",
        notesInfo: "Documentation on attribute manipulations notes",
        manipulation: "PHP Code",
        notes: "Notes"
    },

    metaDataFields: {
        title: "Metadata of {{name}}",
        key: "Key",
        value: "Value",
        error: "Invalid {{format}}",
        placeholder: "Search and add metadata fields"
    },

    selectEntities: {

    },

    revisions: {
        info: "All revisions",
        noRevisions: "No revisions",
        number: "Number",
        created: "Created",
        updatedBy: "Updater",
        status: "Status",
        notes: "Notes",
        toggleAllDetails: "Show diffs for all revisions",
        toggleDetails: "Show diff with previous revision",
        identical: "This revision is identical to the previous revision",
        nope: "",
        restore: "Restore",
        restoreConfirmation: "Are you sure you want to restore revision {{number}} to the latest revision?"
    },

    export: {
        title: "Metadata export",
        description: "The metadata export is available in JSON - flat or structured - and SAML format.",
        showXml: "Show the SAML / XML exported MetaData",
        showJson: "Show the JSON exported MetaData",
        showJsonFlat: "Flatten the MetaData fields. This is the internal repository format",
        showMetaDataOnly: "Show only the connection and metadata."
    },

    import: {
        title: "Metadata import",
        import_xml_url: "Import XML URL",
        import_xml: "Import XML",
        import_json_url: "Import JSON URL",
        import_json: "Import JSON",
        results: "Results",
        url: "Enter a valid SAML metadata endpoint",
        jsonUrl: "Enter a valid JSON metadata endpoint",
        entityId: "Optionally enter the entityID if the feed contains more entities",
        fetch: "Import",
        xml: "Paste XML metadata",
        json: "Paste JSON metadata",
        invalid: "Invalid {{type}}",
        no_results: "No results yet. Import metadata first...",
        validationErrors: "The import validation against the {{type}} schema failed. Correct the errors and import again.",
        nothingChanged: "The imported metadata is exactly the same as the current metadata. Nothing to import...",
        resultsInfo: "Delta of current metadata and the imported metadata",
        new_resultsInfo: "Overview of the imported metadata",
        resultsSubInfo: "Select the metadata parts to add / replace",
        new_resultsSubInfo: "Select the metadata parts to add",
        headers: {
            include: "Include",
            name: "Name",
            current: "Current value",
            newValue: "New value",
        },
        currentEntries: "Current {{name}} entries",
        newEntries: "New {{name}} entries",
        connection: "Apply replacement / adding of new / changed Connection attributes?",
        metaDataFields: "Apply replacement / adding of new / changed Metadata fields",
        allowedEntities: "Apply replacement of the Whitelist?",
        disableConsent: "Apply replacement of the Disabled Consent?",
        arp: "Apply replacement of the ARP?",
        new_connection: "Add Connection attributes?",
        new_metaDataFields: "Add Metadata fields",
        new_allowedEntities: "Add Whitelist?",
        new_disableConsent: "Add Disabled Consent?",
        new_arp: "Add ARP?",
        arpEnabled: "ARP enabled?",
        applyImportChanges: "Import changes",
        applyImportChangesInfo: "Note that after importing the changes you still need to submit a new revision to persist the changes",
        applyImportChangesFlash: "{{changes}} were updated. Note that you still need to submit a new revision to persist the changes",
        new_applyImportChanges: "Import new metadata",
        new_applyImportChangesInfo: "Note that after importing the new metadata you still need to submit the new metadata to persist the changes",
        new_applyImportChangesFlash: "{{changes}} were imported. Note that you still need to submit the new metadata to persist the changes"
    },

    clipboard: {
        copied: "Copied!",
        copy: "Copy to clipboard"
    },

    error_dialog: {
        title: "Unexpected error",
        body: "This is embarrassing; an unexpected error has occurred. It has been logged and reported. Please try again. Still doesn't work? Please click 'Help'.",
        ok: "Close"
    },

    confirmation_dialog: {
        title: "Please confirm",
        confirm: "Confirm",
        cancel: "Cancel",
        leavePage: "Do you really want to leave this page?",
        leavePageSub: "Changes that you made will not be saved.",
        stay: "Stay",
        leave: "Leave"
    },

    metadata_autocomplete: {
        entity_id: "Entity ID",
        name: "Name",
        state: "Production",
        notes: "Notes",
        link: "Link",
        no_results: "No results",
        results_limited: "More entries then shown matched, please narrow your search term....."
    },

    arp: {
        description: "Documentation about ARP",
        arp_enabled: "No ARP - all attributes are released to the SP",
        attributes: "ARP Attributes",
        name: "Name",
        source: "Source",
        enabled: "Enabled",
        matching_rule: "Matching rule",
        action: "",
        wildcard: "Wildcard",
        exact: "Exact",
        prefix: "Prefix",
        new_attribute_value: "Enter the new ARP value for {{key}}",
        new_attribute_motivation: "The motivation for the release of {{key}}",
        new_attribute_motivation_placeholder: "Motivation..."
    },
    not_found: {
        title: "404",
        description_html: "The requested page could not be found"
    },
    server_error: {
        title: "500 Unexpected error",
        description_html: "This is embarrassing; an unexpected error has occurred. It has been logged and reported. Please try again. Still doesn't work? Please click 'Help'.",
    }
};

export default I18n.translations.en;
