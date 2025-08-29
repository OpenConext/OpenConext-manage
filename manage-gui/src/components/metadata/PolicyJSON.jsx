import React, {useEffect, useState} from "react";

import "./PolicyJSON.scss";
import {parsePolicyJSON} from "../../api";
import "highlight.js/styles/default.css";
import JSONPretty from "react-json-pretty";
import "react-json-pretty/themes/monikai.css";

export default function PolicyJSON({data}) {

    const [loading, setLoading] = useState(true);
    const [json, setJSON] = useState("");

    useEffect(() => {
        parsePolicyJSON(data).then(res => {
            setJSON(res);
            setLoading(false);
        })
    }, []);

    if (loading) {
        return;
    }

    return (
        <section className="metadata-policy-json">
            <section className="policy-json">
                <JSONPretty id="json-pretty" json={json}></JSONPretty>
            </section>
        </section>
    );
}
