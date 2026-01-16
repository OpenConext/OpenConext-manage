import React from "react";
import PropTypes from "prop-types";
import ConfirmationDialog from "../ConfirmationDialog";
import ErrorDialog from "../ErrorDialog";

export const DIALOG_TYPES = {
    CONFIRM: "confirm",
    INFO: "info",
    ERROR: "error"
};

export default function Dialog({ dialogType, ...forwardedProps }) {
    switch (dialogType) {
        case DIALOG_TYPES.CONFIRM:
            return <ConfirmationDialog {...forwardedProps} />;
        case DIALOG_TYPES.INFO:
            return <h1>Todo: Info Dialog</h1>;
        case DIALOG_TYPES.ERROR:
            return <ErrorDialog {...forwardedProps} />;
        default:
            console.error(`Unknown dialog type: ${dialogType}`);
            return null;
    }
}

Dialog.propTypes = {
    dialogType: PropTypes.oneOf(Object.values(DIALOG_TYPES)).isRequired
};
