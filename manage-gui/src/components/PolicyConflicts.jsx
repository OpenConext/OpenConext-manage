import React, {useEffect, useState} from "react";
import "./PolicyConflicts.scss";
import {policyConflicts} from "../api";
import {isEmpty} from "../utils/Utils";

export default function PolicyConflicts() {
    const [loading, setLoading] = useState(true);
    const [conflicts, setConflicts] = useState([]);

    useEffect(() => {
        policyConflicts().then(res => {
            setConflicts(res);
            setLoading(false);
        });
    }, []);

    const headers = ["entityID", "policies"]
    return (
        <section className="policy-conflicts">
            {(!loading && isEmpty(conflicts)) && <h2>There are no conflicting policies</h2>}
            {(!loading && !isEmpty(conflicts)) &&
                <>
                    <h2>The policies below are potential conflicts as they apply for duplicates SP&apos;s / RP&apos;s or IdP&apos;s</h2>
                    <section className="conflicts">
                        <table>
                            <thead>
                            <tr>
                                {headers.map((header, index) =>
                                    <th className={header} key={index}>
                                        {header}
                                    </th>)}
                            </tr>
                            </thead>
                            <tbody>
                            {Object.entries(conflicts).map((entry, index) =>
                                <tr key={index}>
                                    <td>{entry[0]}</td>
                                    <td>
                                        <div className="policies">
                                            {entry[1].map((policy, innerIndex) =>
                                                <a key={innerIndex} href={`/metadata/policy/${policy.id}`} target="_blank" rel="noopener noreferrer">
                                                    {policy.data.name}
                                                </a>)
                                            }
                                        </div>
                                    </td>
                                </tr>
                            )}
                            </tbody>
                        </table>
                    </section>
                </>}
        </section>
    );
}
