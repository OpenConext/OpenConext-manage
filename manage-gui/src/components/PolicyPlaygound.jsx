import React, {useEffect, useState} from "react";

import "./PolicyPlayground.scss";
import I18n from "i18n-js";
import CodeMirror from '@uiw/react-codemirror';
import {json} from '@codemirror/lang-json';
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
import {determineStatus, isEmpty, stop} from "../utils/Utils";
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
    const [showResponse, setShowResponse] = useState(true);
    const [took, setTook] = useState(0);
    const [copiedToClipboardClassName, setCopiedToClipboardClassName] = useState(true);

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

    const copyToClipboard = codeValue => {
        const val = JSON.stringify(codeValue, null, 3);
        navigator.clipboard.writeText(val).then(() => {
            setCopiedToClipboardClassName("copied")
            setTimeout(() => setCopiedToClipboardClassName(""), 5000);
        });
    };

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

    const renderServiceProvider = () => {
        return (
            <div className="input-field">
                <label htmlFor="serviceProvider">
                    <span>{I18n.t("policyPlayGround.serviceProvider")}</span>
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

    const renderIdentityProvider = () => {
        return (
            <div className="input-field">
                <label htmlFor="institutionProvider">
                    <span>{I18n.t("policyPlayGround.institutionProvider")}</span>
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

    const doSetAttributes = attrs => {
        setAttributes(attrs);
    }

    const renderAttributes = () => {
        return (
            <>
                <p>{I18n.t("policies.attribute")}</p>
                <PolicyAttributes
                    attributes={attributes}
                    embedded={true}
                    allowedAttributes={samlAttributes}
                    setAttributes={doSetAttributes}
                    includeNegate={false}
                    isRequired={false}
                    isPlayground={true}
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
                            "Value": selectedServiceProvider.value
                        },
                        {
                            "AttributeId": "IDPentityID",
                            "Value": selectedIdentityProvider.value
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
        const now = new Date().getTime();
        playGroundPolicyDecision(pdpRequestInstance)
            .then(res => {
                setPdpResponse(res);
                setPdpRequest(pdpRequestInstance);
                setShowResponse(true);
                setLoading(false);
                setTook(new Date().getTime() - now);
            })
            .catch(() => {
                setTook(new Date().getTime() - now);
                setLoading(false);
                setPdpResponse(null);
                setPdpRequest(pdpRequestInstance);
                setShowResponse(false);
                setFlash("Exception from PdP decision endpoint. Check the logs", "error")
            });
    }

    const clear = () => {
        setPolicy(null);
        setPdpResponse(null);
        setPdpRequest(null);
        setShowResponse(true);
        setSelectedIdentityProvider(null);
        setSelectedServiceProvider(null);
        setAttributes([]);
        setInitial(true);
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

    const toggleResponseRequestView = e => {
        stop(e);
        setShowResponse(!showResponse);
    }

    const renderPolicyResponse = () => {
        const codeValue = showResponse ? pdpResponse : pdpRequest;
        return (
            <>
                <div className="pdp-response-options">
                    <a href="/"
                       onClick={toggleResponseRequestView}
                       className={!showResponse ? "active" : ""}>PdP Request</a>
                    <span>|</span>
                    <a href="/"
                       onClick={toggleResponseRequestView}
                       className={showResponse ? "active" : ""}>{`PdP Response (took ${took} ms)`}</a>
                    <div className="copy-container">
                        <span className={`button green ${copiedToClipboardClassName}`}
                              onClick={() => copyToClipboard(codeValue)}>
                                    {I18n.t("clipboard.copy")}<i className="fa fa-clone"/>
                        </span>
                    </div>
                </div>
                <div className="pdp-response">
                    <CodeMirror value={JSON.stringify(codeValue, null, 3)}
                                basicSetup={{
                                    lineNumbers: false,
                                    foldGutter: false,
                                    highlightActiveLineGutter: false,
                                    highlightActiveLine: false,
                                }}
                                extensions={[json({})]}
                                readOnly={true}/>
                </div>
            </>
        )
            ;

    }

    return (
        <section className="playground-policy-form">
            <section className="policy-form">
                {renderPolicies()}
                {renderServiceProvider()}
                {renderIdentityProvider()}
                {renderAttributes()}
                {renderActions()}
            </section>
            <section className="policy-response">
                {pdpResponse && renderPolicyResponse()}
            </section>
        </section>
    );
}
