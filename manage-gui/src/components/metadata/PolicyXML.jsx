import React, {useEffect, useState} from "react";

import "./PolicyXML.scss";
import {parsePolicyXML} from "../../api";
import Highlight from "react-highlight";
import "highlight.js/styles/default.css";
import format from "xml-formatter";

export default function PolicyXML({data}) {

    const [loading, setLoading] = useState(true);
    const [xml, setXML] = useState("");

    useEffect(() => {
        parsePolicyXML(data).then(res => {
            setXML(format(res.xml, { indentation: "    " }));
            setLoading(false);
        })
    }, []);

    if (loading) {
        return ;
    }

    return (
        <section className="metadata-policy-xml">
            <section className="policy-xml">
                <Highlight className="XML">{xml}</Highlight>
            </section>
        </section>
    );
}
