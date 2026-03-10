import React, {useEffect, useState} from "react";
import "./Institution.scss";
import I18n from "../../locale/I18n";
import {isEmpty} from "../../utils/Utils";
import {search, uniqueEntityId, uniqueInstitutionIdentifier, validation} from "../../api";
import Select from "../Select";
import {CheckBox} from "../index";
import SelectMulti from "../form/SelectMulti";

export default function Institution({
                                        configuration,
                                        data,
                                        errors,
                                        isNew,
                                        onChange,
                                        onError
                                    }) {

    const [institutions, setInstitutions] = useState([]);
    const [originalIdentifier, setOriginalIdentifier] = useState([]);
    const [duplicateIdentifier, setDuplicateIdentifier] = useState(false);


    useEffect(() => {
        search({}, "institution")
            .then(res => setInstitutions(res));
        if (!isNew) {
            setOriginalIdentifier(data.identifier);
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    const internalOnChange = (value, attribute) => {
        onChange(`data.${attribute}`, value);
        if (configuration.required.includes(attribute)) {
            onError(attribute, isEmpty(value));
        }
    }

    const validateFormat = async (attribute, value, format) => {
        if (isEmpty(value)) {
            return true;
        }
        if (typeof value === "string") {
            const valid = await validation(format, value);
            onError(attribute, !valid);
        }

        if (Array.isArray(value)) {
            const validationValues = await Promise.all(
                value.map(val => validation(format, val))
            );
            const valid = isEmpty(validationValues.filter(val => !val));
            onError(attribute, !valid);
        }
    }

    const renderError = (attribute, formatInvalid = false) => {
        return (
            <div className="error">
                <span>{I18n.t(formatInvalid ? "metaDataFields.error" : "metadata.required", {
                    name: attribute,
                    format: attribute
                })}</span>
            </div>
        );
    }

    const renderDuplicateIdentifier = () => {
        return (
            <div className="error">
                <span>{I18n.t("institution.duplicateIdentifier")}</span>
            </div>
        );
    }

    const onBlurIdentifier = e => {
        const identifier = e.target.value;
        internalOnChange(e.target.value, "entityid");
        if (isEmpty(identifier) || (!isNew && originalIdentifier === identifier)) {
            setDuplicateIdentifier(false);
            return true;
        }
        uniqueInstitutionIdentifier(identifier).then(res => {
            setDuplicateIdentifier(res.length > 0);
        })
    }

    const renderIdentifier = () => {
        return (
            <div className="input-field">
                <label htmlFor="identifier">
                    <span>{I18n.t("institution.identifier")}</span>
                </label>
                <input id="identifier"
                       type="text"
                       value={data.identifier || ""}
                       onChange={e => internalOnChange(e.target.value, "identifier")}
                       onBlur={onBlurIdentifier}/>
                {isEmpty(data.identifier) && renderError("Identifier")}
                {duplicateIdentifier && renderDuplicateIdentifier()}
            </div>
        );
    }

    const renderNumberOfTokensPerIdentity = () => {
        return (
            <div className="input-field">
                <label htmlFor="number_of_tokens_per_identity">
                    <span>{I18n.t("institution.numberOfTokensPerIdentity")}</span>
                </label>
                <input id="number_of_tokens_per_identity"
                       type="number"
                       value={data.number_of_tokens_per_identity}
                       onChange={e => internalOnChange(parseInt(e.target.value, 10), "number_of_tokens_per_identity")}/>
            </div>
        );
    }

    const renderUseRaLocations = () => {
        return (
            <div className="input-field">
                <CheckBox name="use_ra_locations"
                          value={data.use_ra_locations}
                          info={I18n.t("institution.useRaLocations")}
                          onChange={e => internalOnChange(e.target.checked, "use_ra_locations")}/>
            </div>
        );
    }

    const renderShowRaaContactInformation = () => {
        return (
            <div className="input-field">
                <CheckBox name="show_raa_contact_information"
                          value={data.show_raa_contact_information}
                          info={I18n.t("institution.showRaaContactInformation")}
                          onChange={e => internalOnChange(e.target.checked, "show_raa_contact_information")}/>
            </div>
        );
    }

    const renderVerifyEmail = () => {
        return (
            <div className="input-field">
                <CheckBox name="verify_email"
                          value={data.verify_email}
                          info={I18n.t("institution.verifyEmail")}
                          onChange={e => internalOnChange(e.target.checked, "verify_email")}/>
            </div>
        );
    }

    const renderStepupClient = () => {
        return (
            <div className="input-field">
                <label htmlFor="stepupClient">
                    <span>{I18n.t("institution.stepupClient")}</span>
                </label>
                <Select
                    onChange={option => internalOnChange(option.value, "stepup-client")}
                    options={configuration.properties["stepup-client"].enum
                        .map(client => ({label: client, value: client}))}
                    value={data["stepup-client"]}
                    isClearable={false}
                    isSearchable={false}
                />
            </div>
        );
    }


    const renderAllowedSecondFactors = () => {
        return (
            <div className="input-field">
                <label htmlFor="allowed_second_factors">
                    <span>{I18n.t("institution.allowedSecondFactors")}</span>
                </label>
                <SelectMulti
                    enumValues={configuration.properties.allowed_second_factors.items.enum}
                    isClearable={false}
                    onChange={options => internalOnChange(options, "allowed_second_factors")}
                    value={data.allowed_second_factors}
                    isSearchable={false}
                />
                {isEmpty(data.allowed_second_factors) && renderError("Allowed second factors")}
            </div>
        );
    }

    const renderUseRa = () => {
        return (
            <div className="input-field">
                <label htmlFor="use_ra">
                    <span>{I18n.t("institution.useRa")}</span>
                </label>
                <SelectMulti
                    enumValues={institutions.map(instition => instition.data.entityid)}
                    onChange={options => internalOnChange(options, "use_ra")}
                    value={data.use_ra}
                    isSearchable={false}
                />
            </div>
        );
    }

    return (
        <section className="metadata-sfo">
            {/*{JSON.stringify(errors)}*/}
            <section className="sfo">
                {renderIdentifier()}
                {renderUseRaLocations()}
                {renderShowRaaContactInformation()}
                {renderVerifyEmail()}
                {renderAllowedSecondFactors()}
                {renderNumberOfTokensPerIdentity()}
                {renderUseRa()}
                {renderStepupClient()}
            </section>
        </section>
    );
}
