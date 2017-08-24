import React from "react";
import PropTypes from "prop-types";
import {ping} from "../api";
import Import from "../components/metadata/Import";
import Detail from "./Detail";
import "./ImportMetaData.css";

export default class ImportMetaData extends React.PureComponent {

    constructor(props) {
        super(props);
        this.state = {
            showDetails: false,
            results: {}
        };
    }

    componentDidMount() {
        ping().then(() => this);
    }

    applyImportChanges = (results, applyImportChanges) => {
        Object.keys(applyImportChanges).forEach(applyImport => {
            if (!applyImportChanges[applyImport]) {
                delete results[applyImport];
            }
        });
        this.setState({showDetails: true, results: results});
    };

    render() {
        const {showDetails, results} = this.state;
        const {history,currentUser,configuration} = this.props;
        if (showDetails) {
            return (
                <Detail history={history}
                        currentUser={currentUser}
                        configuration={configuration}
                        newMetaData={results}/>
            );
        }
        const metaData = {
            data: {
                allowedEntities: [],
                disableConsent: [],
                arp: {
                    enabled: false,
                    attributes: {}
                },
                metaDataFields: {},
            }
        };
        return (
            <div className="import-metadata">
                <Import metaData={metaData}
                        guest={false}
                        newEntity={true}
                        applyImportChanges={this.applyImportChanges}/>
            </div>
        );
    }
}

ImportMetaData.propTypes = {
    history: PropTypes.object.isRequired,
    currentUser: PropTypes.object.isRequired,
    configuration: PropTypes.array.isRequired
};

