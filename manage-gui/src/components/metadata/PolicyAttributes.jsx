import React from "react";

import "./PolicyAttributes.scss";
import I18n from "i18n-js";
import {Select} from "../index";
import {groupPolicyAttributes, isEmpty, stop} from "../../utils/Utils";
import CheckBox from "../CheckBox";

export default function PolicyAttributes({
                                             attributes = [],
                                             allowedAttributes = [],
                                             setAttributes,
                                             allAttributesMustMatch,
                                             onError,
                                             embedded,
                                             includeNegate,
                                             isRequired,
                                             isPlayground
                                         }) {

    const hasAttributes = (propagateError = true) => {
        if (!isRequired) {
            return true;
        }
        const attributesEmpty = isEmpty(attributes) || attributes.every(attr => isEmpty(attr.value));
        if (propagateError) {
            onError("attributes", attributesEmpty);
        }
        return !attributesEmpty;
    }

    const deleteAttribute = (name, nameWithGroupPostfix) => {
        const groupID = parseInt(nameWithGroupPostfix.substring(nameWithGroupPostfix.indexOf("#") + 1), 10);
        const newAttributes = attributes.filter(attr => attr.name !== name || (attr.name === name && attr.groupID !== groupID));
        setAttributes(newAttributes, () => hasAttributes())
    }

    const deleteValue = (name, index) => {
        const newAttributes = attributes.filter(attr => attr.name !== name || (attr.name === name && attr.index !== index));
        setAttributes(newAttributes, () => hasAttributes())
    }

    const addAttribute = option => {
        const newAttributes = [...attributes];
        const numberOfDuplicatedAttr = newAttributes.filter(attr => attr.name === option.value).length;
        const groupID = numberOfDuplicatedAttr === 0 ? 0 : (numberOfDuplicatedAttr + 1);
        newAttributes.push({name: option.value, value: "", groupID: groupID, negated: false});
        setAttributes(newAttributes);
    }

    const changeValue = (name, index, e, negated) => {
        const newAttributes = [...attributes];
        const attribute = newAttributes.find(attr => attr.name === name && attr.index === index);
        if (negated) {
            attribute.negated = e.target.checked;
        } else {
            attribute.value = e.target.value;
        }
        newAttributes.splice(index, 1, attribute);
        setAttributes(newAttributes, () => hasAttributes())
    }

    const addValue = (e, nameWithGroupPostfix) => {
        stop(e);
        const newAttributes = [...attributes];
        const hashIndex = nameWithGroupPostfix.indexOf("#");
        const name = nameWithGroupPostfix.substring(0, hashIndex);
        const groupID = parseInt(nameWithGroupPostfix.substring(hashIndex + 1), 10);
        newAttributes.push({name: name, value: "", negated: false, groupID: groupID});
        setAttributes(newAttributes);
    }

    const resolveAttributeLabel = name => {
        return allowedAttributes.find(attr => attr.value === name)?.label;
    }

    const groupedAttributes = groupPolicyAttributes(attributes.map((attr, index) => {
        attr.index = index;
        attr.groupID = attr.groupID || 0;
        return attr;
    }));

    return (
        <div className={`policy-attributes ${embedded ? "max" : ""}`}>
            {Object.keys(groupedAttributes).map((nameWithGroupPostfix, outerIndex) => {
                    const name = nameWithGroupPostfix.substring(0, nameWithGroupPostfix.indexOf("#"))
                    return (
                        <div key={outerIndex} className="attribute-container">
                            {(!isPlayground && outerIndex !== 0) && <span className="logical-separator top">
                                    {I18n.t(allAttributesMustMatch ? "policies.andShort" : "policies.orShort")}
                                </span>}
                            <div className="attribute">
                                <input className="max"
                                       type="text"
                                       disabled={true}
                                       value={`${resolveAttributeLabel(name)} - ${name}`}/>
                                <span onClick={() => deleteAttribute(name, nameWithGroupPostfix)}>
                                    <i className="fa fa-trash-o"/>
                                </span>
                            </div>
                            <p>{I18n.t("policies.values")}</p>
                            {groupedAttributes[nameWithGroupPostfix].map((attr, innerIndex) =>
                                <>
                                    <div key={innerIndex} className="value">
                                        {includeNegate &&
                                            <CheckBox name={window.crypto.randomUUID()}
                                                      value={attr.negated}
                                                      onChange={e => changeValue(name, attr.index, e, true)}
                                                      info={I18n.t("policies.negated")}
                                            />}
                                        <input className="max" type="text" value={attr.value}
                                               onChange={e => changeValue(name, attr.index, e, false)}/>
                                        <span onClick={() => deleteValue(name, attr.index)}>
                                        <i className="fa fa-trash-o"/>
                                    </span>
                                    </div>
                                    {(!isPlayground && innerIndex < groupedAttributes[nameWithGroupPostfix].length - 1) &&
                                        <span className={`logical-separator ${includeNegate ? "loa" : ""}`}>
                                    {I18n.t("policies.orShort")}
                                </span>}

                                </>)}
                            <a href="#" onClick={e => addValue(e, nameWithGroupPostfix)}>{I18n.t("policies.addValue")}</a>
                        </div>

                    );
                }
            )}
            <Select
                className="policy-select max"
                onChange={addAttribute}
                value={null}
                options={allowedAttributes}
                placeholder={I18n.t("policies.addAttribute")}
                isSearchable={false}
            />
            {!hasAttributes(false) &&
                <div className="error"><span>{I18n.t("metadata.required", {name: "Attribute"})}</span></div>}
        </div>
    );

}
