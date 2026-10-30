import React from "react";
import PropTypes from "prop-types";
import I18n from "../locale/I18n";
import {hasDuplicates} from "../utils/Utils";

import "./RemoveDuplicates.scss";

export default function RemoveDuplicates({entries, onRemove, guest}) {
    if (guest || !hasDuplicates(entries)) {
        return null;
    }
    return (
        <div className="remove-duplicates">
            <span className="button blue" onClick={onRemove}>
                {I18n.t("duplicates.remove")}
                <i className="fas fa-trash"/>
            </span>
        </div>
    );
}

RemoveDuplicates.propTypes = {
    entries: PropTypes.array.isRequired,
    onRemove: PropTypes.func.isRequired,
    guest: PropTypes.bool
};
