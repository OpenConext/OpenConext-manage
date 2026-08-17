import React from "react";
import PropTypes from "prop-types";

export default function NotesTooltip({notes}) {
    return (
        <span className={"notes-tooltip"}>
            <i className="fas fa-info-circle tooltip-trigger" data-tooltip-content={notes}></i>
        </span>
    );

}

NotesTooltip.propTypes = {
    identifier: PropTypes.string.isRequired,
    notes: PropTypes.string.isRequired,
};


