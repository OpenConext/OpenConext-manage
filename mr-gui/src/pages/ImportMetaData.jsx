import React from "react";
import I18n from "i18n-js";
import PropTypes from "prop-types";
import {ping} from "../api";
import {stop} from "../utils/Utils";
import Import from "../components/metadata/Import";
import "./ImportMetaData.css";

export default class ImportMetaData extends React.PureComponent {

    constructor(props) {
        super(props);
        const configuration = props.configuration;
        const types = configuration.map(metaData => metaData.title).reverse();
        this.state = {
            entityId: "",
            types: types,
            selectedType: types[0]
        };
    }

    componentDidMount() {
        ping().then(() => this);//.entityIdInput.focus());
    }

    switchType = type => e => {
        stop(e);
        this.setState({selectedType: type});
    };

    renderType = (type, selectedType) =>
        <span key={type} className={type === selectedType ? "active" : ""} onClick={this.switchType(type)}>
            {I18n.t(`metadata.${type}`)}
        </span>;

    applyImportChanges = (results, applyImportChanges) => {
        debugger;
    };

    render() {
        const {types, selectedType} = this.state;
        const {configuration} = this.props;
        const metaData = {};
        return (
            <div className="import-metadata">
                <section className="types">
                    {types.map(type => this.renderType(type, selectedType))}
                </section>
                <Import metaData={metaData}
                        configuration={configuration}
                        guest={false}
                        applyImportChanges={this.applyImportChanges}/>
            </div>
        );
    }
}

ImportMetaData.propTypes = {
    history: PropTypes.object.isRequired,
    configuration: PropTypes.array.isRequired
};

