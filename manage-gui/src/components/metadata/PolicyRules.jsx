import React from "react";

import I18n from "../../locale/I18n";
import CheckBox from "../CheckBox";

export default function PolicyRules({
                                        value,
                                        setRule,
                                        embedded
                                    }) {
    const andName = window.crypto.randomUUID();
    const orName = window.crypto.randomUUID();
    return (
        <div>
            <p>{I18n.t("policies.rules")}</p>
            <div className={`checkbox-options ${embedded ? "max" : ""}`}>
                <div className="checkbox-container">
                    <label htmlFor={andName}>
                        <span>{I18n.t("policies.and")}</span>
                        <i className="fas fa-info-circle tooltip-trigger"
                           data-tooltip-content={I18n.t("policies.andTooltip")}
                           data-tooltip-place="right"/>
                    </label>
                    <CheckBox
                        name={andName}
                        onChange={e=> setRule(e.target.checked)}
                        value={value}
                    />
                </div>
                <div className="checkbox-container adjustment">
                    <label htmlFor={orName}>
                        <span>{I18n.t("policies.or")}</span>
                        <i className="fas fa-info-circle tooltip-trigger"
                           data-tooltip-content={I18n.t("policies.orTooltip")}
                           data-tooltip-place="right"/>
                    </label>
                    <CheckBox
                        name={orName}
                        onChange={e => setRule(!e.target.checked)}
                        value={!value}
                    />
                </div>
            </div>
        </div>

    );

}
