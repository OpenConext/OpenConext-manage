import React, {useEffect, useState} from "react";

import "./PolicyXML.scss";
import {parsePolicyXML} from "../../api";
import Highlight from "react-highlight";
import "highlight.js/styles/default.css";
import format from "xml-formatter";
import {copyToClip} from "../../utils/Utils";
import I18n from "i18n-js";

export default function PolicyXML({data}) {

    const [loading, setLoading] = useState(true);
    const [copiedToClipboardClassName, setCopiedToClipboardClassName] = useState(true);
    const [xml, setXML] = useState("");

    useEffect(() => {
        parsePolicyXML(data).then(res => {
            setXML(format(res.xml, {indentation: "    "}));
            setLoading(false);
        })
    }, []);

    if (loading) {
        return;
    }

    const copyToClipboard = () => {
        navigator.clipboard.writeText(xml);
        setCopiedToClipboardClassName("copied")
        setTimeout(() => setCopiedToClipboardClassName(""), 5000);
    };


    return (
        <section className="metadata-policy-xml">
            <section className="policy-xml">
                <div className="copy-container">
                    <span className={`button green ${copiedToClipboardClassName}`} onClick={copyToClipboard}>
                                {I18n.t("clipboard.copy")}<i className="fa fa-clone"/>
                    </span>
                </div>
                <Highlight className="XML">{xml}</Highlight>
            </section>
        </section>
    );
}
