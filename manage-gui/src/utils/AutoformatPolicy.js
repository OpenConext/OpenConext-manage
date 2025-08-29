import {groupPolicyAttributes, isEmpty} from "./Utils";

export const AutoFormat = {

    addQuotes: function (str) {
        return isEmpty(str) ? str : `'${str}'`;
    },

    attributes: function (passedAttributes, allAttributesMustMatch) {
        const attributes = groupPolicyAttributes(passedAttributes);
        const attributeNames = Object.keys(attributes);
        const length = attributeNames.length;
        const lines = attributeNames.map((attributeNameWithPostfix, index) => {
            const attributeValues = attributes[attributeNameWithPostfix];
            const values = attributeValues.map(attribute => {
                const negated = attribute.negated ? "NOT " : "";
                return negated + this.addQuotes(attribute.value);
            }).join(" or ");
            const logical = index === (length - 1) ? "" : allAttributesMustMatch ? " and " : " or ";
            const attributeName = attributeNameWithPostfix.substring(0, attributeNameWithPostfix.indexOf("#"));
            let result;
            let postFix = attributeValues.length > 1 ? "s " : " "
            if (attributeName === "urn:collab:group:surfteams.nl") {
                result = "he/she is a member of the team" + postFix + values + logical;
            } else {
                result = "he/she has the value" + postFix + values + " for attribute '" + attributeName + "'" + logical;

            }
            return result;
        });
        return lines.join("");

    },

    cidrNotations: function (loa, passedCidrNotations, allAttributesMustMatch, hasAttributes) {
        if (passedCidrNotations.length === 0) {
            return "";
        }
        let res = hasAttributes ? (allAttributesMustMatch ? " and" : " or") : "";
        const negate = loa.negateCidrNotation ? "NOT " : "";
        res += " with an IP address " + negate + "in the range(s): ";
        const lines = passedCidrNotations.map(notation => this.addQuotes(notation.ipAddress + "/" + notation.prefix));
        return res + lines.join(" or ");
    },

    description: function (policy, identityProviderNames, serviceProviderNames) {
        const idps = isEmpty(identityProviderNames) ? "" : " from " + identityProviderNames.map(this.addQuotes).join(" or ");
        const sp = this.addQuotes(serviceProviderNames.join(", ")) || "?";
        const attrs = policy.attributes || [];

        const attributes = this.attributes(attrs, policy.allAttributesMustMatch);

        const only = policy.denyRule ? "not" : "only";

        const loas = policy.loas || [];
        const loasTxt = loas.map(loa => {
            const attrLoa = this.attributes(loa.attributes || [], loa.allAttributesMustMatch);
            let txt = " is required to authenticate with LoA " + this.addQuotes(loa.level);
            if (attrLoa !== ".") {
                txt = txt + " when " + attrLoa;
            }

            const cidrNotationTxt = this.cidrNotations(loa, loa.cidrNotations, loa.allAttributesMustMatch, loa.attributes.length > 0);
            txt = txt + cidrNotationTxt;
            return txt;
        }).join(" and he /she ");

        //we can't use JS templates as the backtick breaks the uglification. Will be resolved when we upgrade the build tooling
        let description;
        if (policy.type === "step") {
            description = "A user" + idps + loasTxt + " when accessing " + sp;
        } else {
            description = "A user" + idps + " is " + only + " allowed to access " + sp + " when" + " " + attributes;
        }


        return description;
    }
};
