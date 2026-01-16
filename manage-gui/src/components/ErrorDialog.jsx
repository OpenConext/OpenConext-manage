import React from "react";
import PropTypes from "prop-types";
import Modal from "react-modal";
import I18n from "i18n-js";
import {stop} from "../utils/Utils";

import "./ConfirmationDialog.scss";

export default function ErrorDialog({
                                        isOpen = false,
                                        close,
                                        title,
                                        body
                                    }) {
    const dialogTitle = title || I18n.t("error_dialog.title");
    const dialogBody = body || I18n.t("error_dialog.body");

    return (
        <Modal
            isOpen={isOpen}
            onRequestClose={close}
            ariaHideApp={false}
            contentLabel={dialogTitle}
            className="confirmation-dialog-content"
            overlayClassName="confirmation-dialog-overlay"
            closeTimeoutMS={250}
        >
            <section className="dialog-header error">
                {dialogTitle}
            </section>
            <section className="dialog-content">
                <h2>{dialogBody}</h2>
            </section>
            <section className="dialog-buttons">
                <a className="button blue error" onClick={e => {
                    stop(e);
                    close?.(e);
                }}>{I18n.t("error_dialog.ok")}</a>
            </section>
        </Modal>
    );

}

ErrorDialog.propTypes = {
    isOpen: PropTypes.bool,
    close: PropTypes.func.isRequired,
    title: PropTypes.string,
    body: PropTypes.string
};
