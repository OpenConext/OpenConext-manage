import React from "react";
import I18n from "i18n-js";
import PropTypes from "prop-types";
import CodeMirror from "react-codemirror";
import "codemirror/mode/javascript/javascript";
import "codemirror/lib/codemirror.css";
import "./Manipulation.css";

export default class Manipulation extends React.PureComponent {

    constructor(props) {
        super(props);
        this.state = {};
    }

    componentDidMount() {
        window.scrollTo(0, 0);
    }

    onChange = name => value => {
        this.props.onChange(name, value);
    };

    render() {
        const {content, guest} = this.props;
        const optionsForInfo= {lineNumbers: false, mode: "javascript", readOnly: true};
        const optionsForContent  = {lineNumbers: true, mode: "javascript", readOnly: guest};

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
            <div className="metadata-manipulation">
                <div className="manipulation-info">
                    <h2>
                        <a href="https://github.com/OpenConext/OpenConext-engineblock/wiki/Attribute-Manipulations" target="_blank" rel="noopener noreferrer">
                            {I18n.t("manipulation.description")}
                    </a>
                    </h2>
                </div>
                <CodeMirror className="comments" value={info} options={optionsForInfo} />
                <div className="spacer"></div>
                <CodeMirror value={content} onChange={this.onChange("data.manipulation")} options={optionsForContent} />
            </div>
        );
    }
}

Manipulation.propTypes = {
    content: PropTypes.string,
    onChange: PropTypes.func.isRequired,
    guest: PropTypes.bool.isRequired
};

