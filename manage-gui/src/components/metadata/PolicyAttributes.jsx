import React from "react";

import "./PolicyAttributes.scss";
import I18n from "i18n-js";
import {Select} from "../index";
import {groupBy, isEmpty, stop} from "../../utils/Utils";
import CheckBox from "../CheckBox";

export default function PolicyAttributes({
                                             attributes = [],
                                             allowedAttributes = [],
                                             setAttributes,
                                             onError,
                                             embedded,
                                             includeNegate,
                                             isRequired
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

    const deleteAttribute = name => {
        const newAttributes = attributes.filter(attr => attr.name !== name);
        setAttributes(newAttributes, () => hasAttributes())
    }

    const deleteValue = (name, index) => {
        const newAttributes = attributes.filter(attr => attr.name !== name || (attr.name === name && attr.index !== index));
        setAttributes(newAttributes, () => hasAttributes())
    }

    const addAttribute = option => {
        const newAttributes = [...attributes];
        newAttributes.push({name: option.value, value: "", negated: false});
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

    const addValue = (e, name) => {
        stop(e);
        const newAttributes = [...attributes];
        newAttributes.push({name: name, value: "", negated: false});
        setAttributes(newAttributes);
    }

    const resolveAttributeLabel = name => {
        return allowedAttributes.find(attr => attr.value === name)?.label;
    }

    const groupedAttributes = groupBy(attributes.map((attr, index) => {
        attr.index = index;
        return attr;
    }), "name");


    return (
        <div className={`attributes ${embedded ? "max" : ""}`}>
            {Object.keys(groupedAttributes).map((name, i) =>
                <div key={i} className="attribute-container">
                    <div className="attribute">
                        <input className="max"
                               type="text"
                               disabled={true}
                               value={`${resolveAttributeLabel(name)} - ${name}`}/>
                        <span onClick={() => deleteAttribute(name)}>
                                    <i className="fa fa-trash-o"/>
                                </span>
                    </div>
                    <p>{I18n.t("policies.values")}</p>
                    {groupedAttributes[name].map((attr, i) =>
                        <div key={i} className="value">
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
                        </div>)}
                    <a href="#" onClick={e => addValue(e, name)}>{I18n.t("policies.addValue")}</a>
                </div>
            )}
            <Select
                className="policy-select max"
                onChange={addAttribute}
                value={null}
                options={allowedAttributes.filter(attr => !attributes.some(dataAttr => attr.value === dataAttr.name))}
                placeholder={I18n.t("policies.addAttribute")}
                isSearchable={false}
            />
            {!hasAttributes(false) &&
                <div className="error"><span>{I18n.t("metadata.required", {name: "Attribute"})}</span></div>}
        </div>
    );

}