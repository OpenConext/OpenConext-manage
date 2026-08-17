import React from "react";
import {Tooltip as ReactTooltip} from "react-tooltip";
import "./Tooltip.scss";

export default function Tooltip() {
    return (
        <ReactTooltip
            id="app-tooltip"
            anchorSelect=".tooltip-trigger"
            className="tool-tip"
            variant="info"
            place="top"
            render={({content, activeAnchor}) => {
                const html = activeAnchor?.getAttribute("data-tooltip-html");
                return html
                    ? <span dangerouslySetInnerHTML={{__html: html}}/>
                    : <span>{content}</span>;
            }}
        />
    );
}
