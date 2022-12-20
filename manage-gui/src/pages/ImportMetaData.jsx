import React from "react";
import PropTypes from "prop-types";
import {ping} from "../api";
import Import from "../components/metadata/Import";
import Detail from "./Detail";
import "./ImportMetaData.scss";

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
        const {currentUser, configuration} = this.props;
        if (showDetails) {
            return (
                <Detail currentUser={currentUser}
                        fromImport={true}
                        configuration={configuration}
                        newMetaData={results}/>
            );
        }
        const metaData = {
            data: {
                allowedEntities: [],
                disableConsent: [],
                stepupEntities: [],
                allowedResourceServers: [],
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
                        entityType="saml20_sp"
                        applyImportChanges={this.applyImportChanges}/>
            </div>
        );
    }
}

ImportMetaData.propTypes = {
    currentUser: PropTypes.object.isRequired,
    configuration: PropTypes.array.isRequired
};

