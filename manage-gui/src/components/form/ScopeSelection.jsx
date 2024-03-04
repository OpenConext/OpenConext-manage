import React from "react";
import {Select} from "./../../components";
import {fetchEnumValues, scopeInUse} from "../../api";
import I18n from "i18n-js";
import "./ScopeSelection.scss";
import {getNameForLanguage} from "../../utils/Language";

export default class ScopeSelection extends React.PureComponent {

    state = {
        fetchValues: [],
        alreadyUsedScopes: []
    };

    componentDidMount() {
        fetchEnumValues("scopes").then(res => {
            this.setState({fetchValues: res});
            const {value} = this.props;
            this.checkForScopesInUse(value);
        });

    }

    valuesToOptions(values) {
        return values.map(value => ({value: value, label: value}));
    }

    internalOnChange = options => {
        const {onChange} = this.props;
        const changes = options.map(option => option.value);
        onChange(changes);
        this.checkForScopesInUse(changes);
    }

    checkForScopesInUse(scopes) {
        scopes = scopes.filter(scope => scope !== "openid");
        scopeInUse(scopes).then(res => {
            const {isNewEntity, originalEntityId} = this.props;
            if (!isNewEntity) {
                res = res.filter(entity => entity.data.entityid !== originalEntityId);
            }
            if (res.length > 0) {
                //There are other Resource Servers who are using on of the states in the array "scopes"
                const alreadyUsedScopes = scopes.filter(scope => res.some(entity => entity.data.metaDataFields.scopes.includes(scope)));
                let warningMsg = alreadyUsedScopes.map(scope => I18n.t("scopes.duplicateScope", {
                    name: scope,
                    entities: res.filter(entity => entity.data.metaDataFields.scopes.includes(scope))
                        .map(entity => `<a href="/metadata/oauth20_rs/${entity.id}/metadata" target="_blank">${getNameForLanguage(entity.data.metaDataFields)}</a>`).join(" and ")
                }));
                this.setState({alreadyUsedScopes: warningMsg});
            } else {
                this.setState({alreadyUsedScopes: []});
            }
        });
    }

    render() {
        const {onChange, value, ...rest} = this.props;
        const {fetchValues, alreadyUsedScopes} = this.state;

        const selectedOptions = this.valuesToOptions(value);
        const options = this.valuesToOptions(fetchValues);

        return (
            <div className="scope-selection">
                <Select
                    {...rest}
                    isMulti={true}
                    onChange={this.internalOnChange}
                    optionRenderer={option => option.label}
                    options={options}
                    value={selectedOptions}
                /> <span/>
                {alreadyUsedScopes.map((msg, i) => <span key={i} className="error"
                                                         dangerouslySetInnerHTML={{__html: msg}}/>)}
            </div>
        );
    }
}
