import React from "react";
import Creatable from 'react-select/creatable';
import "./SFO.scss";
import I18n from "../../locale/I18n";
import {isEmpty} from "../../utils/Utils";
import {validation} from "../../api";
import reactSelectStyles from "./../reactSelectStyles.js";
import Select from "../Select";
import {CheckBox} from "../index";

export default function SFO({
                                configuration,
                                data,
                                errors,
                                onChange,
                                onError
                            }) {

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

    const renderName = () => {
        return (
            <div className="input-field">
                <label htmlFor="name">
                    <span>{I18n.t("sfo.name")}</span>
                </label>
                <input id="name"
                       type="text"
                       value={data.name || ""}
                       onChange={e => internalOnChange(e.target.value, "name")}/>
                {isEmpty(data.name) && renderError("Name")}
            </div>
        );
    }

    const renderEntityID = () => {
        return (
            <div className="input-field">
                <label htmlFor="entityID">
                    <span>{I18n.t("sfo.entityId")}</span>
                </label>
                <input id="entityID"
                       type="text"
                       value={data.entityid || ""}
                       onChange={e => internalOnChange(e.target.value, "entityid")}/>
                {isEmpty(data.entityid) && renderError("EntityID")}
            </div>
        );
    }

    const renderPublicKey = () => {
        return (
            <div className="input-field">
                <label htmlFor="public_key">
                    <span>{I18n.t("sfo.publicKey")}</span>
                </label>
                <input id="public_key"
                       type="text"
                       value={data.public_key || ""}
                       onBlur={e => validateFormat("public_key", e.target.value, "certificate")}
                       onChange={e => {
                           internalOnChange(e.target.value, "public_key");
                       }}/>
                {isEmpty(data.public_key) && renderError("Public key")}
                {(!isEmpty(data.public_key) && errors.public_key) &&
                    renderError("Public key", true)}
            </div>
        );
    }

    const renderAcs = () => {
        return (
            <div className="input-field">
                <label htmlFor="acs">
                    <span>{I18n.t("sfo.acs")}</span>
                </label>
                <Creatable
                    styles={reactSelectStyles}
                    inputId={"react-select-acs"}
                    isMulti={true}
                    onChange={options => internalOnChange(options.map(o => o.value), "acs")}
                    onBlur={() => validateFormat("acs", data.acs, "url")}
                    placeholder="Enter acs..."
                    value={(data.acs || []).map(acs => ({value: acs, label: acs}))}
                />
                {(!isEmpty(data.acs) && errors.acs) &&
                    renderError("ACS", true)}
                {isEmpty(data.acs)  &&
                    renderError("ACS", )}
            </div>
        );
    }

    const renderLoa = () => {
        return (
            <div className="input-field">
                <label htmlFor="loa">
                    <span>{I18n.t("sfo.loa")}</span>
                </label>
                <Select
                    className="policy-select"
                    onChange={option => internalOnChange(option.value, "loa")}
                    options={configuration.properties.loa.enum
                        .map(loa => ({label: loa, value: loa}))}
                    value={data.loa || configuration.properties.loa.default}
                    isSearchable={false}
                />
            </div>
        );
    }

    const renderAssertionEncryptionEnabled = () => {
        return (
            <div className="input-field">
                <CheckBox name="assertion_encryption_enabled"
                          value={data.assertion_encryption_enabled}
                          info={I18n.t("sfo.assertionEncryptionEnabled")}
                          onChange={e => internalOnChange(e.target.checked, "assertion_encryption_enabled")}/>
            </div>
        );
    }

    const renderSecondFactorOnly = () => {
        return (
            <div className="input-field">
                <CheckBox name="second_factor_only"
                          value={data.second_factor_only}
                          info={I18n.t("sfo.secondFactorOnly")}
                          onChange={e => internalOnChange(e.target.checked, "second_factor_only")}/>
            </div>
        );
    }

    const renderSecondFactorOnlyNameidPatterns = () => {
        return (
            <div className="input-field">
                <label htmlFor="second_factor_only_nameid_patterns">
                    <span>{I18n.t("sfo.secondFactorOnlyNameidPatterns")}</span>
                </label>
                <Creatable
                    styles={reactSelectStyles}
                    inputId={"react-select-patterns"}
                    isMulti={true}
                    onChange={options => internalOnChange(options.map(o => o.value), "second_factor_only_nameid_patterns")}
                    placeholder="Enter nameID patterns..."
                    value={(data.second_factor_only_nameid_patterns || []).map(pattern => ({value: pattern, label: pattern}))}
                />
            </div>
        );
    }

    const renderBlacklistedEncryptionAlgorithm = () => {
        return (
            <div className="input-field">
                <label htmlFor="blacklisted_encryption_algorithms">
                    <span>{I18n.t("sfo.blacklistedEncryptionAlgorithm")}</span>
                </label>
                <Creatable
                    styles={reactSelectStyles}
                    inputId={"react-select-patterns"}
                    isMulti={true}
                    onChange={options => internalOnChange(options.map(o => o.value), "blacklisted_encryption_algorithms")}
                    placeholder="Enter blacklisted algorithms..."
                    value={(data.blacklisted_encryption_algorithms || []).map(algorithm => ({value: algorithm, label: algorithm}))}
                />
            </div>
        );
    }

    return (
        <section className="metadata-sfo">
            {/*{JSON.stringify(errors)}*/}
            <section className="sfo">
                {renderName()}
                {renderEntityID()}
                {renderPublicKey()}
                {renderAcs()}
                {renderLoa()}
                {renderAssertionEncryptionEnabled()}
                {renderSecondFactorOnly()}
                {renderSecondFactorOnlyNameidPatterns()}
                {renderBlacklistedEncryptionAlgorithm()}
            </section>
        </section>
    );
}
