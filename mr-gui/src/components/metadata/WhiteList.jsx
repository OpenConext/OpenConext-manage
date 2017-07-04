import React from "react";
import I18n from "i18n-js";
import PropTypes from "prop-types";
import ConfirmationDialog from "../../components/ConfirmationDialog";
import CheckBox from "./../CheckBox";
import SelectEntities from "./../SelectEntities";
import {stop} from "../../utils/Utils";

import "./WhiteList.css";

export default class WhiteList extends React.PureComponent {

    constructor(props) {
        super(props);
        this.state = {
            confirmationDialogOpen: false,
            confirmationDialogAction: this.confirmationDialogAction
        };
    }

    componentDidMount() {
        window.scrollTo(0, 0);
    }

    confirmationDialogAction = e => {
        stop(e);
        this.setState({confirmationDialogOpen: false});
        this.props.onChange("data.allowedall", false);
    };

    cancel = e => {
        stop(e);
        this.setState({confirmationDialogOpen: false});
    };

    addAllowedEntry = allowedEntry => {
        const {allowedEntities} = this.props;
        const newState = [...allowedEntities].concat({name: allowedEntry});
        this.onChange("data.allowedEntities")(newState);
    };

    removeAllowedEntry = allowedEntry => {
        const {allowedEntities} = this.props;
        const newState = [...allowedEntities].filter(entity => entity.name !== allowedEntry.name);
        this.onChange("data.allowedEntities")(newState);
    };

    onChange = name => value => {
        if (value.target) {
            this.props.onChange(name, value.target.checked);
        } else {
            this.props.onChange(name, value);
        }
    };

    allowAllChanged = e => {
        if (e.target.checked) {
            this.props.onChange("data.allowedall", true);
        } else if (this.props.allowedEntities.length > 0) {
            this.setState({confirmationDialogOpen: true});
        } else {
            this.props.onChange("data.allowedall", false);
        }
    };

    renderAllowedEntity = entity =>
        <CheckBox key={entity.name} info={entity.name} name={entity.name} value={true} onChange={() => this.removeAllowedEntry(entity)}/>

    render() {
        const {allowedAll, allowedEntities, whiteListing, name, type} = this.props;
        const typeS = type === "saml20_sp" ? "Service Providers" : "Identity Providers";
        const {confirmationDialogOpen} = this.state;
        return (
            <div className="metadata-whitelist">
                <ConfirmationDialog isOpen={confirmationDialogOpen}
                                    cancel={this.cancel}
                                    confirm={this.confirmationDialogAction}
                                    leavePage={false}
                                    question={I18n.t("whitelisting.confirmation", {name: name, type: typeS})}/>
                <CheckBox info={I18n.t("metadata.allowAll", {name: name || "this service"})} name="allow-all" value={allowedAll} onChange={this.allowAllChanged}/>
                <SelectEntities whiteListing={whiteListing} allowedEntities={allowedEntities}
                                onChange={this.addAllowedEntry}/>
                {allowedEntities.map(this.renderAllowedEntity)}
            </div>
        );
    }
}

WhiteList.propTypes = {
    whiteListing: PropTypes.array.isRequired,
    name: PropTypes.string.isRequired,
    type: PropTypes.string.isRequired,
    allowedEntities: PropTypes.array.isRequired,
    allowedAll: PropTypes.bool.isRequired,
    onChange: PropTypes.func.isRequired
};

