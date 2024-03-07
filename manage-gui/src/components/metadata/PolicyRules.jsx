import React from "react";

import I18n from "i18n-js";
import ReactTooltip from "react-tooltip";
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
                        <i className="fa fa-info-circle"
                           data-for={`${andName}-tooltip`}
                           data-tip/>
                        <ReactTooltip id={`${andName}-tooltip`}
                                      type="info"
                                      place="right"
                                      class="tool-tip"
                                      effect="solid">
                            <span>{I18n.t("policies.andTooltip")}</span>
                        </ReactTooltip>
                    </label>
                    <CheckBox
                        name={andName}
                        onChange={e => setRule(!e.target.checked)}
                        value={value}
                    />
                </div>
                <div className="checkbox-container adjustment">
                    <label htmlFor={orName}>
                        <span>{I18n.t("policies.or")}</span>
                        <i className="fa fa-info-circle"
                           data-for={`${orName}-tooltip`}
                           data-tip/>
                        <ReactTooltip id={`${orName}-tooltip`}
                                      type="info"
                                      place="right"
                                      class="tool-tip"
                                      effect="solid">
                            <span>{I18n.t("policies.orTooltip")}</span>
                        </ReactTooltip>
                    </label>
                    <CheckBox
                        name={orName}
                        onChange={e => setRule(e.target.checked)}
                        value={!value}
                    />
                </div>
            </div>
        </div>

    );

}