import React from "react";
import PropTypes from "prop-types";
import Modal from "react-modal";
import I18n from "i18n-js";
import {stop} from "../utils/Utils";

import "./ConfirmationDialog.scss";

export default function ConfirmationDialog({
                                               isOpen = false, cancel, confirm, question = "",
                                               leavePage = false, isError = false
                                           }) {
    return (
        <Modal
            isOpen={isOpen}
            onRequestClose={cancel}
            ariaHideApp={false}
            contentLabel={I18n.t("confirmation_dialog.title")}
            className="confirmation-dialog-content"
            overlayClassName="confirmation-dialog-overlay"
            closeTimeoutMS={250}>
            <section className="dialog-header">
                {I18n.t("confirmation_dialog.title")}
            </section>
            {leavePage ?
                <section className={`dialog-content ${isError ? " error" : ""}`}>
                    <h2>{I18n.t("confirmation_dialog.leavePage")}</h2>
                    <p>{I18n.t("confirmation_dialog.leavePageSub")}</p>
                </section> :
                <section className="dialog-content">
                    <h2>{question}</h2>
                </section>}
            <section className="dialog-buttons">
                <a className="button" onClick={e => {
                    stop(e);
                    cancel();
                }}>
                    {leavePage ? I18n.t("confirmation_dialog.leave") : I18n.t("confirmation_dialog.cancel")}
                </a>
                <a className="button blue" onClick={e => {
                    stop(e);
                    confirm();
                }}>
                    {leavePage ? I18n.t("confirmation_dialog.stay") : I18n.t("confirmation_dialog.confirm")}
                </a>
            </section>
        </Modal>
    );

}

ConfirmationDialog.propTypes = {
    isOpen: PropTypes.bool,
    cancel: PropTypes.func.isRequired,
    confirm: PropTypes.func.isRequired,
    question: PropTypes.string,
    leavePage: PropTypes.bool,
    isError: PropTypes.bool
};


