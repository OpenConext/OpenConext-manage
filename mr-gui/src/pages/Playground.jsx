import React from "react";
import I18n from "i18n-js";
import PropTypes from "prop-types";
import {ping} from "../api";
const tabsSp = ["connection", "whitelist", "metadata", "arp", "manipulation"];
const tabsIdP = ["connection", "whitelist", "consent", "metadata", "manipulation"];

export default class New extends React.PureComponent {

    constructor(props) {
        super(props);
        this.state = {
            metaData: {},
            notFound: false,
            loaded: false,
            selectedTab: "connection"
        };
    }

    componentDidMount() {
        ping();//.then(configuration =>
           // this.setState({configuration: configuration}))
    }

    render() {
        const {loaded, notFound, metaData, selectedTab} = this.state;
        const type = metaData.type;
        const tabs = type == "saml20_sp" ? tabsSp : tabsIdP;
        return (
            <div className="playground-metadata">
                <section className="playground">
                    {I18n.t("playground.migrate")}
                </section>

            </div>
        );
    }
}

New.propTypes = {
    history: PropTypes.object.isRequired,
    configuration: PropTypes.array.isRequired,
};

