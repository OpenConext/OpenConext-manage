import {groupBy, isEmpty} from "./Utils";

export const AutoFormat = {

    addQuotes: function (str) {
        return isEmpty(str) ? str : `'${str}'`;
    },

    attributes: function (passedAttributes, allAttributesMustMatch) {
        let attributes = passedAttributes;
        const otherAttr = attributes.filter(attr => {
            return attr.name !== "urn:collab:group:surfteams.nl";
        });
        if (otherAttr.length === 0) {
            return ".";
        }
        attributes = groupBy(attributes, "name");
        const attributeNames = Object.keys(attributes);
        const length = attributeNames.length;
        const lines = attributeNames.map((attributeName, index) => {
            const values = attributes[attributeName].map(attribute => {
                const negated = attribute.negated ? "NOT " : "";
                return negated + this.addQuotes(attribute.value);
            }).join(" or ");
            const logical = index === (length - 1) ? "" : allAttributesMustMatch ? " and " : " or ";

            const result = "he/she has the value " + values + " for attribute '" + attributeName + "'" + logical;
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
        const teamMembershipAttr = attrs.filter(attr => {
            return attr.name === "urn:collab:group:surfteams.nl";
        });
        const teamMembership = teamMembershipAttr.length > 0 ? " he/she is a member of the team " + teamMembershipAttr
            .map(attr => this.addQuotes(attr.value)).join(" or ") : "";

        const and = teamMembershipAttr.length === 0 || teamMembershipAttr.length === attrs.length ? "" : policy.allAttributesMustMatch ? " and" : " or";
        const only = policy.denyRule ? "not" : "only";

        const attributes = this.attributes(attrs, policy.allAttributesMustMatch);

        const loas = policy.loas || [];
        const loasTxt = loas.map(loa => {
            const attrLoa = this.attributes(loa.attributes || [], loa.allAttributesMustMatch);
            let txt = " is required to authenticate with LoA " + this.addQuotes(loa.level);
            if (attrLoa !== ".") {
                txt = txt + " when " + attrLoa;
            }
            const loaTeamMembershipAttr = loa.attributes.filter(attr => attr.name === "urn:collab:group:surfteams.nl");
            const loaTeamMembership = loaTeamMembershipAttr.length > 0 ? " he/she is a member of the team " + loaTeamMembershipAttr
                .map(attr => {
                    const negated = attr.negated ? "NOT " : "";
                    return negated + this.addQuotes(attr.value);
                }).join(" or ") : "";

            const cidrNotationTxt = this.cidrNotations(loa, loa.cidrNotations, loa.allAttributesMustMatch, loa.attributes.length > 0);
            txt = txt + cidrNotationTxt;
            if (loaTeamMembership !== "") {
                txt = txt + ((cidrNotationTxt !== "" || attrLoa !== ".") ? " and" : "") + " when" + loaTeamMembership;
            }
            return txt;
        }).join(" and he /she ");

        //we can't use JS templates as the backtick breaks the uglification. Will be resolved when we upgrade the build tooling
        let description;
        if (policy.type === "step") {
            description = "A user" + idps + loasTxt + " when accessing " + sp;
        } else {
            description = "A user" + idps + " is " + only + " allowed to access " + sp + " when" + teamMembership + and + " " + attributes;
        }


        return description;
    }
};
