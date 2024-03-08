import React, {useEffect, useState} from "react";

import "./PolicyPlayground.scss";
import I18n from "i18n-js";
import CodeMirror from '@uiw/react-codemirror';
import {javascript} from '@codemirror/lang-javascript';
import {
    getPlaygroundIdentityProviders,
    getPlaygroundPolicies,
    getPlaygroundRelyingParties,
    getPlaygroundServiceProviders,
    playGroundPolicyDecision,
    policySAMLAttributes
} from "../api";
import Select from "./Select";
import {getNameForLanguage} from "../utils/Language";
import {determineStatus, isEmpty} from "../utils/Utils";
import PolicyAttributes from "./metadata/PolicyAttributes";
import {setFlash} from "../utils/Flash";

const providerOptions = provider => ({
    value: provider.data.entityid,
    label: getNameForLanguage(provider.data.metaDataFields)
})

export default function PolicyPlayground({}) {

    const [loading, setLoading] = useState(false);
    const [initial, setInitial] = useState(true);
    const [policies, setPolicies] = useState([]);
    const [samlAttributes, setSamlAttributes] = useState([]);
    const [attributes, setAttributes] = useState([]);
    const [policy, setPolicy] = useState(null);
    const [policyOptions, setPolicyOptions] = useState([]);
    const [serviceProviders, setServiceProviders] = useState([]);
    const [identityProviders, setIdentityProviders] = useState([]);
    const [selectedServiceProvider, setSelectedServiceProvider] = useState(null);
    const [selectedIdentityProvider, setSelectedIdentityProvider] = useState(null);
    const [pdpResponse, setPdpResponse] = useState(null);
    const [pdpRequest, setPdpRequest] = useState(null);

    useEffect(() => {
        Promise.all([
            getPlaygroundPolicies(),
            policySAMLAttributes(),
            getPlaygroundIdentityProviders(),
            getPlaygroundRelyingParties(),
            getPlaygroundServiceProviders()])
            .then(res => {
                setPolicies(res[0]);
                setPolicyOptions(res[0].map(p => ({value: p["_id"], label: p.data.name})));
                setSamlAttributes(res[1]);
                setIdentityProviders(res[2].map(providerOptions));
                setServiceProviders(res[3].map(providerOptions).concat(res[4].map(providerOptions)))
            })
    }, []);

    const renderError = attribute => {
        return (
            <div className="error"><span>{I18n.t("metadata.required", {name: attribute})}</span></div>
        );
    }

    const policyChanged = policyOption => {
        setPolicy(policyOption);
        const policyConf = policies.find(p => p["_id"] === policyOption.value);
        const policyServiceProviders = policyConf.data.serviceProviderIds
            .map(sp => serviceProviders.find(val => val.value === sp.name))
            .filter(sp => !isEmpty(sp));
        setSelectedServiceProvider(policyServiceProviders[0]);
        const policyIdentityProvider = (policyConf.data.identityProviderIds || [])
            .map(idp => identityProviders.find(val => val.value === idp.name))
            .filter(idp => !isEmpty(idp))
        setSelectedIdentityProvider(policyIdentityProvider[0]);
        if (policyConf.data.type === "reg") {
            const attrs = policyConf.data.attributes;
            setAttributes(attrs);
        } else {
            const loas = policyConf.data.loas;
            if (!isEmpty(loas)) {
                const attrs = loas.map(loa => loa.attributes).flat();
                const ipAddresses = loas
                    .map(loa => loa.cidrNotations)
                    .flat()
                    .map(cidr => cidr.ipAddress)
                    .map(ip => ({name: "urn:mace:surfnet.nl:collab:xacml-attribute:ip-address", value: ip}));
                setAttributes(attrs.concat(ipAddresses))
            }
        }

    }

    const renderPolicies = () => {
        return (
            <div className="input-field">
                <label htmlFor="policy">
                    <span>{I18n.t("policyPlayGround.policy")}</span>
                </label>
                <Select
                    className="policy-select"
                    onChange={policyChanged}
                    placeholder={I18n.t("policyPlayGround.policyPlaceholder")}
                    options={policyOptions.filter(p => (policy || {}).value !== p.value)}
                    value={policy}
                />
            </div>
        );
    }

    const renderServiceProviders = () => {
        return (
            <div className="input-field">
                <label htmlFor="serviceProvider">
                    <span>{I18n.t("policies.serviceProviders")}</span>
                </label>
                <Select
                    className="policy-select"
                    onChange={setSelectedServiceProvider}
                    placeholder={I18n.t("policyPlayGround.serviceProviderPlaceholder")}
                    options={serviceProviders.filter(sp => (selectedServiceProvider || {}).value !== sp.value)}
                    value={selectedServiceProvider}
                />
                {!initial && isEmpty(selectedServiceProvider) && renderError("Service provider")}
            </div>
        );
    }

    const renderIdentityProviders = () => {
        return (
            <div className="input-field">
                <label htmlFor="institutionProvider">
                    <span>{I18n.t("policies.institutionProviders")}</span>
                </label>
                <Select
                    className="policy-select"
                    onChange={setSelectedIdentityProvider}
                    placeholder={I18n.t("policyPlayGround.institutionProviderPlaceholder")}
                    options={identityProviders.filter(idp => (selectedIdentityProvider || {}).value !== idp.value)}
                    value={selectedIdentityProvider}
                />
                {!initial && isEmpty(selectedIdentityProvider) && renderError("Identity provider")}
            </div>
        );
    }

    const renderAttributes = () => {
        return (
            <>
                <p>{I18n.t("policies.attribute")}</p>
                <PolicyAttributes
                    attributes={attributes}
                    embedded={true}
                    allowedAttributes={samlAttributes}
                    setAttributes={setAttributes}
                    includeNegate={false}
                    isRequired={false}
                />
                {!initial && isEmpty(attributes) && renderError("Attribute")}
            </>

        );
    }

    const renderStatus = () => {
        const response = pdpResponse.Response[0];
        const decision = response.Decision;
        const statusCode = response.Status.StatusCode.Value;
        const status = determineStatus(decision);
        return (
            <div className={"response-status " + status}>
                <i className={"fa fa-" + status + " " + status}></i>
                <section>
                    <p className="status">{decision}</p>
                    <p className="details">{"Status code: " + "'" + statusCode + "'"}</p>
                </section>
            </div>
        );
    }


    const invalid = !initial && (isEmpty(selectedServiceProvider) || isEmpty(selectedIdentityProvider) || isEmpty(attributes));

    const decide = () => {
        setInitial(false);
        if (isEmpty(selectedServiceProvider) || isEmpty(selectedIdentityProvider) || isEmpty(attributes)) {
            return;
        }
        const pdpRequestInstance = {
            "Request": {
                "AccessSubject": {
                    "Attribute": attributes.map(attr => ({
                        AttributeId: attr.name,
                        Value: attr.value
                    }))
                },
                "Resource": {
                    "Attribute": [
                        {
                            "AttributeId": "SPentityID",
                            "Value": selectedServiceProvider
                        },
                        {
                            "AttributeId": "IDPentityID",
                            "Value": selectedIdentityProvider
                        },
                        {
                            "AttributeId": "ClientID",
                            "Value": "EngineBlock"
                        }
                    ]
                }
            }
        }
        setLoading(true);
        playGroundPolicyDecision(pdpRequestInstance)
            .then(res => {
                setPdpResponse(res);
                setPdpRequest(pdpRequestInstance);
                setLoading(false);
            })
            .catch(() => {
                setLoading(false);
                setFlash("Exception from PdP decision endpoint. Check the logs", "error")
            });
    }

    const clear = () => {
        setPolicy(null);
        setPdpResponse(null);
        setPdpRequest(null);
        setSelectedIdentityProvider(null);
        setSelectedServiceProvider(null);
        setAttributes([]);
    }

    const renderActions = () => {
        return (
            <div className="actions">
                <a className="button clear"
                   onClick={clear}>
                    {I18n.t("policyPlayGround.clear")}
                </a>
                <a className={`button ${(loading || invalid) ? "grey disabled" : "green"}`}
                   onClick={decide}>
                    {I18n.t("policyPlayGround.runDecision")}
                </a>
            </div>
        );
    }

    const renderPolicyResponse = () => {
        return (
            <>
                {renderStatus()}
                <CodeMirror value={JSON.stringify(pdpResponse, null, 3)}
                            readOnly={true}/>;
            </>
        );

    }

    return (
        <section className="playground-policy-form">
            <section className="policy-form">
                {renderPolicies()}
                {renderServiceProviders()}
                {renderIdentityProviders()}
                {renderAttributes()}
                {renderActions()}
            </section>
            <section className="policy-response">
                {pdpResponse && renderPolicyResponse()}
            </section>
        </section>
    );
}