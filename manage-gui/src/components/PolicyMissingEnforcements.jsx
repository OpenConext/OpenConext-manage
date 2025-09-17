import React, {useEffect, useState} from "react";
import I18n from "i18n-js";
import "./PolicyMissingEnforcements.scss";
import {missingEnforcementPolicies} from "../api";
import {isEmpty} from "../utils/Utils";
import {getNameForLanguage, getOrganisationForLanguage} from "../utils/Language";

export default function PolicyMissingEnforcements({}) {
    const [loading, setLoading] = useState(true);
    const [policies, setPolicies] = useState([]);

    useEffect(() => {
        missingEnforcementPolicies().then(res => {
            setPolicies(res);
            setLoading(false);
        });
    }, []);

    const organisationName = provider => {
        const org = getOrganisationForLanguage(provider.data.metaDataFields);
        return isEmpty(org) ? "" : ` (${org})`;
    }

    const headers = ["name", "type", "description", "providers", "identityProviders"]
    return (
        <section className="missing-enforcement-policies">
            {(!loading && isEmpty(policies)) && <h2>All of the SP's, RP's and IdP's used in policies have
                set <em>coin:policy_enforcement_decision_required</em> to true</h2>}
            {(!loading && !isEmpty(policies)) &&
                <>
                    <h2>The policies below are for SP's, RP's or IdP's that have not
                        set <em>coin:policy_enforcement_decision_required</em> to true</h2>
                    <section className="policy-response">
                        <table>
                            <thead>
                            <tr>
                            {headers.map((header, index) => <th className={header} key={index}>
                                {I18n.t(`policies.${header}`)}
                            </th>)}
                            </tr>
                            </thead>
                            <tbody>
                            {policies.map((policy, index) => {
                                const policyEnforcementDecisionAbsent = policy.data.policyEnforcementDecisionAbsent || [];
                                const policyIdPEnforcementDecisionAbsent = policy.data.policyIdPEnforcementDecisionAbsent || [];
                                return <tr key={index}>
                                    <td><a href={`/metadata/policy/${policy.id}`} target="_blank">
                                        {policy.data.name}
                                    </a></td>
                                    <td>{I18n.t(`policies.${policy.data.type}`)}</td>
                                    <td>{policy.data.description}</td>
                                    <td>
                                        <div className="providers">
                                            {policyEnforcementDecisionAbsent.map((provider, index) =>
                                                <a key={index} href={`/metadata/${provider.type}/${provider.id}`}
                                                   target="_blank">
                                                    {`${getNameForLanguage(provider.data.metaDataFields)}${organisationName(provider)}`}
                                                </a>
                                            )}
                                        </div>
                                    </td>
                                    <td>
                                        <div className="providers">
                                            {policyIdPEnforcementDecisionAbsent.map((provider, index) =>
                                                <a key={index} href={`/metadata/${provider.type}/${provider.id}`}
                                                   target="_blank">
                                                    {`${getNameForLanguage(provider.data.metaDataFields)}${organisationName(provider)}`}
                                                </a>
                                            )}
                                        </div>
                                    </td>
                                </tr>;
                            })}
                            </tbody>
                        </table>
                    </section>
                </>}
        </section>
    );
}
