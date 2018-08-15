import React from "react";
import I18n from "i18n-js";
import Editor from "react-md-editor";
import marked from "marked";
import PropTypes from "prop-types";
import CodeMirror from "react-codemirror";
import "codemirror/mode/javascript/javascript";
import "codemirror/lib/codemirror.css";
import "react-md-editor/dist/react-md-editor.css";
import "./Manipulation.css";
import {isEmpty, stop} from "../../utils/Utils";

export default class Manipulation extends React.PureComponent {

    constructor(props) {
        super(props);
        this.state = {
            tabs: ["manipulation", "notes"],
            selectedTab: "manipulation",
        };
    }

    componentDidMount() {
        window.scrollTo(0, 0);
    }

    onChange = name => value => {
        this.props.onChange(name, isEmpty(value) ? null : value);
    };

    renderSelectedTab = selectedTab => {
        switch (selectedTab) {
            case "manipulation":
                return this.renderManipulation();
            case "notes":
                return this.renderNotes();
            default:
                throw new Error("unknown tab");
        }
    };

    switchTab = tab => e => {
        stop(e);
        this.setState({selectedTab: tab});
    };

    renderTab = (tab, selectedTab) =>
        <span key={tab} className={tab === selectedTab ? "active" : ""} onClick={this.switchTab(tab)}>
            {I18n.t(`manipulation.${tab}`)}
        </span>;

    renderNotes() {
        const {notes, guest} = this.props;
        const options = {readOnly: guest};
        const preview = marked(notes);
        return (
            <div className="manipulation-info">
                <h2>
                    <a href="https://github.com/OpenConext/OpenConext-engineblock/wiki/Attribute-Manipulation-Notes"
                       target="_blank" rel="noopener noreferrer">
                        {I18n.t("manipulation.notesInfo")}
                    </a>
                </h2>
                <section className="notes">
                    <Editor value={notes} onChange={this.onChange("data.manipulationNotes")} options={options}/>
                    <div className="preview" dangerouslySetInnerHTML={{__html: preview}}/>
                </section>
            </div>
        );
    }

    renderManipulation() {
        const {content, guest} = this.props;
        const optionsForInfo = {lineNumbers: false, mode: "javascript", readOnly: true};
        const optionsForContent = {lineNumbers: true, mode: "javascript", readOnly: guest};

        const info = `
/**
 * PHP code for advanced Response Manipulation.
 * The following variables are available:
 *
 * @var string &$subjectId  NameID (empty for IdPs)
 * @var array  &$attributes URN attributes (example: array('urn:mace:terena.org:attribute-def:schacHomeOrganization'=>array('example.edu')))
 * @var array  &$response   XmlToArray formatted Response
 */
        `;

        return (
            <div className="manipulation-info">
                <h2>
                    <a href="https://github.com/OpenConext/OpenConext-engineblock/wiki/Attribute-Manipulations"
                       target="_blank" rel="noopener noreferrer">
                        {I18n.t("manipulation.manipulationInfo")}
                    </a>
                </h2>
                <CodeMirror className="comments" value={info} options={optionsForInfo}/>
                <div className="spacer"></div>
                <CodeMirror value={content} onChange={this.onChange("data.manipulation")} options={optionsForContent}/>
            </div>
        );
    }

    render() {
        const {tabs, selectedTab} = this.state;
        return (
            <div className="metadata-manipulation">
                <section className="sub-tabs">
                    {tabs.map(tab => this.renderTab(tab, selectedTab))}
                </section>
                {this.renderSelectedTab(selectedTab)}
            </div>
        );
    }
}

Manipulation.propTypes = {
    content: PropTypes.string,
    notes: PropTypes.string,
    onChange: PropTypes.func.isRequired,
    guest: PropTypes.bool.isRequired
};

