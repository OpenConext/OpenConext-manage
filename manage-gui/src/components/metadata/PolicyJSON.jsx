import React, {useEffect, useState} from "react";

import "./PolicyJSON.scss";
import {parsePolicyJSON} from "../../api";
import "highlight.js/styles/default.css";
import JSONPretty from "react-json-pretty";
import "react-json-pretty/themes/monikai.css";
import I18n from "i18n-js";

export default function PolicyJSON({data}) {

    const [loading, setLoading] = useState(true);
    const [copiedToClipboardClassName, setCopiedToClipboardClassName] = useState(true);
    const [json, setJSON] = useState("");

    useEffect(() => {
        parsePolicyJSON(data).then(res => {
            setJSON(res);
            setLoading(false);
        })
    }, [data]);

    if (loading) {
        return;
    }

    const copyToClipboard = () => {
        navigator.clipboard.writeText(JSON.stringify(json, null, 4));
        setCopiedToClipboardClassName("copied")
        setTimeout(() => setCopiedToClipboardClassName(""), 5000);
    };

    return (
        <section className="metadata-policy-json">
            <section className="policy-json">
                <div className="copy-container">
                    <span className={`button green ${copiedToClipboardClassName}`} onClick={copyToClipboard}>
                                {I18n.t("clipboard.copy")}<i className="fa fa-clone"/>
                    </span>
                </div>
                <JSONPretty id="json-pretty" json={json}></JSONPretty>
            </section>
        </section>
    );
}
