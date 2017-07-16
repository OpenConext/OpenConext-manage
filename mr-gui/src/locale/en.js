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
        title: "Metadata Registry",
        links: {
            help_html: "<a href=\"https://wiki.surfnet.nl/pages/viewpage.action?pageId=35422637\" target=\"_blank\">Help</a>",
            logout: "Logout",
            exit: "Exit"
        },
        role: "Role"
    },

    navigation: {
        search: "Search",
        playground: "Playground"
    },

    metadata: {
        saml20_sp: "Service Providers",
        saml20_idp: "Identity Providers",
        saml20_sp_single: "Service Provider",
        saml20_idp_single: "Identity Provider",
        searchPlaceHolder: "Search for metadata",
        new: "New",
        tabs: {
            connection: "Connection",
            whitelist: "Whitelisting",
            metadata: "Metadata",
            arp: "ARP",
            manipulation: "manipulation",
            consent_disabling: "Consent Disabling",
            revisions: "Revisions",
            import: "Import",
            export: "Export"
        },
        notFound: "No Metadata found",
        entityId: "Entity ID",
        entityIdAlreadyExists: "Entity ID {{entityid}} is already taken.",
        metaDataUrl: "Metadata URL",
        state: "State",
        prodaccepted: "Production",
        testaccepted: "Test",
        revision: "Revision",
        revisionInfo: "Revision {{number}} last updated by {{updatedBy}} on {{created}}",
        notes: "Notes",
        edit: "Edit",
        none: "",
        submit: "Submit",
        cancel: "Cancel",
        remove: "Delete",
        revisionnote: "Revision notes",
        flash: {
            updated: "{{name}} was successfully updated to revision {{revision}}",
            deleted: "{{name}} was successfully deleted"
        },
        required: "{{name}} is required",
        deleteConfirmation: "Are you sure you want to delete {{name}}?"

    },

    playground: {
        migrate: "Migrate",
        validate: "Validate",
        runMigration: "RUN MIGRATION",
        runValidation: "VALIDATE MIGRATION",
    },

    whitelisting: {
        confirmationAllowAll:"Are you sure you want to allow all {{type}} access to '{{name}}'? You will have to add {{type}} one-by-one to selectively deny them again.",
        confirmationAllowNone:"Are you sure you want to deny all {{type}} access to '{{name}}'? You will have to add {{type}} one-by-one or allow them all again.",
        placeholder: "Search, select and add {{type}} to the whitelist",
        allowAllProviders: "Allow all {{type}} access to {{name}}",
        title: "{{type}} Whitelist",
        description: "Add only those {{type}} which are allowed to access {{name}}.",
        allowedEntries: {
            blocked: "Blocked",
            status: "Status",
            entityid:"Entity ID",
            name: "Name"
        }
    },

    consentDisabling: {
        title: "Consent disabling",
        description: "Search and add Service Providers that will skip consent for '{{name}}'.",
        placeholder: "Search, select and add Service Providers to the consent-disabled-list",
        entries: {
            status: "Status",
            entityid:"Entity ID",
            name: "Name"
        }
    },

    manipulation : {
        description: "Documentation on attribute manipulations"
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
        identical: "This revision is identical to the previous revision"
    },

    export: {
        title: "Metadata export",
        description: "The metadata export is available in JSON - flat or structured - and SAML format.",
        showXml: "Show the SAML / XML exported MetaData",
        showJson: "Show the JSON exported MetaData",
        showJsonFlat: "Flatten the MetaData fields. This is the repository format, but not backward compatible with legacy ServiceRegistry exports",
        showMetaDataOnly: "Show only the connection and metadata. This JSON format causes less complex imports"
    },

    import: {
        title: "Metadata import",
        import_url: "Import URL",
        import_xml: "Import XML",
        import_json: "Import JSON",
        results: "Results",
        url: "Enter a valid SAML metadata endpoint",
        fetch: "Import",
        xml: "Paste XML metadata",
        json: "Paste JSON metadata",
        invalid: "Invalid {{type}}",
        no_results: "No results yet. Import metadata first...",
        validationErrors: "The import validation against the {{type}} schema failed. Correct the errors and import again."
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
        no_results: "No results"
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
        new_attribute_value: "Enter the new ARP value for {{key}}"
    }
};

export default I18n.translations.en;
