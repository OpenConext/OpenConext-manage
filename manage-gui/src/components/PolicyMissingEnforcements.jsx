import React, {useEffect, useState} from "react";

import "./PolicyPlayground.scss";
import {missingEnforcementPolicies} from "../api";
import {isEmpty} from "../utils/Utils";

export default function PolicyMissingEnforcements({}) {
    const [loading, setLoading] = useState(true);
    const [policies, setPolicies] = useState([]);

    useEffect(() => {
        missingEnforcementPolicies().then(res => {
            setPolicies(res);
            setLoading(false);
        });
    }, []);


    return (
        <section className="missing-enforcement-policies">
            {(!loading && isEmpty(policies)) && <p>All of the SP's and RP's used in policies have
                set <em>coin:policy_enforcement_decision_required</em> to true</p>}
            {(!loading && !isEmpty(policies)) &&
                <>
                    <p>All of the SP's and RP's below are used in policies, but have not
                        set <em>coin:policy_enforcement_decision_required</em> to true</p>
                    <section className="policy-response">
                        {JSON.stringify(policies)}
                    </section>
                </>}
        </section>
    );
}