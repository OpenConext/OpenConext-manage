import React from "react";
import I18n from "../locale/I18n";
import "./NotFound.scss";

export default function ServerError() {
    return (
        <div className="mod-server-error">
            <h1>{I18n.t("server_error.title")}</h1>
            <p dangerouslySetInnerHTML={{__html: I18n.t("server_error.description_html")}}/>
        </div>
    );
}
