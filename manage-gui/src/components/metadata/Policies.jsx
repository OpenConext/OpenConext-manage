import React from "react";
import I18n from "i18n-js";
import PropTypes from "prop-types";
import {Link} from "react-router-dom";

import "./Policies.scss";
import {copyToClip, isEmpty, stop} from "../../utils/Utils";
import NotesTooltip from "../NotesTooltip";
import withRouterHooks from "../../utils/RouterBackwardCompatability";

class Policies extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            sorted: "name",
            reverse: false,
            policies: [],
            filteredPolicies: [],
            query: "",
            copiedToClipboardClassName: ""
        };
    }

    componentDidMount() {
        this.initialisePolicies(this.props.policies);
    }

    initialisePolicies(policies) {
        window.scrollTo(0, 0);
        const entities = policies.map(policy => ({
            id: policy._id,
            name: policy.data.name,
            description: policy.data.description,
            type: policy.data.type,
            serviceProviders: policy.data.serviceProviderIds.map(sp => sp.name).join(", "),
            identityProviders: (policy.data.identityProviderIds || []).map(sp => sp.name).join(", "),
            notes: policy.data.notes
        }));
        const sorted = entities.sort(this.sortByAttribute("name", false));
        this.setState({policies: entities, filteredPolicies: sorted});
    }

    componentWillReceiveProps(nextProps) {
        if (nextProps.policies && this.props.policies &&
            nextProps.policies.length !== this.props.policies.length) {
            this.initialisePolicies(nextProps.policies);
        }
    }

    sortByAttribute = (name, reverse = false) => (a, b) => {
        const aSafe = a[name] || "";
        const bSafe = b[name] || "";
        return aSafe.toString().localeCompare(bSafe.toString()) * (reverse ? -1 : 1);
    };

    sortTable = (filteredPolicies, name, reversed) => () => {
        const reverse = reversed || (this.state.sorted === name ? !this.state.reverse : false);
        const sorted = [...filteredPolicies].sort(this.sortByAttribute(name, reverse));
        this.setState({filteredPolicies: sorted, sorted: name, reverse: reverse});
    };

    search = e => {
        const query = e.target.value ? e.target.value.toLowerCase() : "";
        const {sorted, reverse, policies} = this.state;
        const names = ["name", "description", "serviceProviders", "identityProviders"];
        const result = isEmpty(query) ? policies : policies.filter(rp => names.some(name =>
            rp[name].toLowerCase().indexOf(query) > -1));
        this.setState({query: query, filteredPolicies: result.sort(this.sortByAttribute(sorted, reverse))});
    };

    copyToClipboard = () => {
        copyToClip("entities-printable");
        this.setState({copiedToClipboardClassName: "copied"});
        setTimeout(() => this.setState({copiedToClipboardClassName: ""}), 5000);
    };

    renderPolicy = (entity) => {
        return (
            <tr key={entity.id}>
                <td>
                    <Link to={`/metadata/policy/${entity.id}`} target="_blank">
                        {entity.name}
                    </Link>
                </td>
                <td>
                    {I18n.t(`policies.${entity.type}`)}
                </td>
                <td>
                    {entity.description}
                </td>
                <td>
                    {entity.serviceProviders}
                </td>
                <td>
                    {entity.identityProviders}
                </td>
                <td className="info">
                    {isEmpty(entity.notes) ? <span></span> :
                        <NotesTooltip identifier={entity.entityid} notes={entity.notes}/>}
                </td>
            </tr>
        );
    };

    renderPolicyTablePrintable = entries =>
        <section id="entities-printable"
                 className="entities-printable">{entries.map(entity => `${entity.name ? entity.name + '\t' : ''}${entity.entityid}`).join("\n")}</section>;

    renderPolicyTable = (entries) => {
        const {sorted, reverse} = this.state;
        const icon = name => {
            return name === sorted ? (reverse ? <i className="fa fa-arrow-up reverse"></i> :
                    <i className="fa fa-arrow-down current"></i>)
                : <i className="fa fa-arrow-down"></i>;
        };
        const th = name =>
            <th key={name} className={name}
                onClick={this.sortTable(entries, name)}>{I18n.t(`policies.${name}`)}{icon(name)}</th>
        const names = ["name", "type", "description", "serviceProviders", "identityProviders", "notes"];
        return <section className="entities">
            <table>
                <thead>
                <tr>
                    {names.map(th)}
                </tr>
                </thead>
                <tbody>
                {entries.map(entity => this.renderPolicy(entity))}
                </tbody>
            </table>

        </section>
    };

    newMetaData = e => {
        stop(e);
        const {metaData} = this.props;
        const type = metaData.type === "saml20_idp" ? "idp" : "sp";
        const path = encodeURIComponent(`/metadata/policy/new?${type}=${metaData.data.entityid}`);
        this.props.navigate(`/refresh-route/${path}`, {replace: true});
    };


    render() {
        const {
            filteredPolicies,
            query,
            policies,
            copiedToClipboardClassName
        } = this.state;
        const {name} = this.props;
        return (
            <div className="metadata-policies">
                {policies.length > 0 && <section className="search">
                    <div className="search-input-container">
                        <input className="search-input"
                               placeholder={I18n.t("policies.searchPlaceHolder")}
                               type="text"
                               onChange={this.search}
                               value={query}/>
                        <i className="fa fa-search"></i>
                    </div>
                    <span className={`button green ${copiedToClipboardClassName}`} onClick={this.copyToClipboard}>
                            {I18n.t("clipboard.copy")}<i className="fa fa-clone"></i>
                        </span>
                    <a className="new button green" onClick={this.newMetaData}>
                        {I18n.t("metadata.newPolicy")}<i className="fa fa-plus"></i>
                    </a>
                </section>}
                {policies.length === 0 &&
                    <div className="no-policies">
                        <h3>{I18n.t("policies.noPolicies", {name: name})}</h3>
                        <a className="new button green" onClick={this.newMetaData}>
                            {I18n.t("metadata.newPolicy")}<i className="fa fa-plus"></i>
                        </a>
                    </div>}
                {policies.length > 0 && this.renderPolicyTable(filteredPolicies)}
                {this.renderPolicyTablePrintable(filteredPolicies)}

            </div>
        )
            ;
    }
}

export default withRouterHooks(Policies);

Policies.propTypes = {
    policies: PropTypes.array.isRequired,
    name: PropTypes.string.isRequired
};

