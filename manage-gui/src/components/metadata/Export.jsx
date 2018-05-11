import React from "react";
import I18n from "i18n-js";
import PropTypes from "prop-types";
import Highlight from "react-highlight";

import {exportMetaData} from "../../api";

import CheckBox from "./../CheckBox";
import ClipBoardCopy from "./../ClipBoardCopy";

import "highlight.js/styles/default.css";
import "./Export.css";


export default class Export extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            showJsonFlat: true,
            showJson: true,
            showJsonMetaDataOnly: false,
            showXml: true,
            json: undefined,
            xml: undefined,
            jsonFlat: undefined,
            jsonMetaDataOnly: undefined,
            jsonMetaDataOnlyFlat: undefined
        };
    }

    componentDidMount() {
        window.scrollTo(0, 0);
        exportMetaData(this.props.metaData)
            .then(json => this.setState({
                xml: json.xml,
                json: json.json,
                jsonFlat: json.jsonFlat,
                jsonMetaDataOnly: json.jsonMetaDataOnly,
                jsonMetaDataOnlyFlat: json.jsonMetaDataOnlyFlat
            }));
    }

    render() {
        const {showJsonFlat, showJson, showXml,showJsonMetaDataOnly ,json, xml, jsonFlat, jsonMetaDataOnly,
            jsonMetaDataOnlyFlat} = this.state;

        return (
            <div className="metadata-export">
                <div className="export-info">
                    <h2>{I18n.t("export.title")}</h2>
                </div>
                {xml && <section className="xml-export">
                    <div className="copy-container">
                        <CheckBox name="xml-export" value={showXml} info={I18n.t("export.showXml")}
                                  onChange={e => this.setState({showXml: e.target.checked})}/>
                        <ClipBoardCopy identifier="xml-export" text={xml}/>
                    </div>
                    {showXml && <Highlight className="XML">
                        {xml}
                    </Highlight>}
                </section>}
                {json && <section className="json-export">
                    <div className="copy-container">
                        <CheckBox name="json-export" value={showJson} info={I18n.t("export.showJson")}
                                  onChange={e => this.setState({showJson: e.target.checked})}/>
                        <ClipBoardCopy identifier="json-export" text={showJsonFlat ? (showJsonMetaDataOnly ? jsonMetaDataOnlyFlat : jsonFlat) :
                            (showJsonMetaDataOnly ? jsonMetaDataOnly: json)}/>
                    </div>
                    {showJson && <CheckBox className="checkbox" name="json-flatten" value={showJsonFlat}
                                           info={I18n.t("export.showJsonFlat")}
                              onChange={e => this.setState({showJsonFlat: e.target.checked})}/>}
                    {showJson && <CheckBox className="checkbox last" name="json-metadata-only" value={showJsonMetaDataOnly}
                                           info={I18n.t("export.showMetaDataOnly")}
                                           onChange={e => this.setState({showJsonMetaDataOnly: e.target.checked})}/>}
                    {showJson && <Highlight className="JSON">
                        {showJsonFlat ? (showJsonMetaDataOnly ? jsonMetaDataOnlyFlat : jsonFlat) :
                            (showJsonMetaDataOnly ? jsonMetaDataOnly: json)}
                    </Highlight>}
                </section>}

            </div>
        );
    }
}

Export.propTypes = {
    metaData: PropTypes.object.isRequired
};

