import React from "react";
import I18n from "i18n-js";
import PropTypes from "prop-types";

import Spinner from "spin.js";
import spinner from "../lib/Spin";

import {NavLink} from "react-router-dom";

import "./Navigation.scss";
import {stop} from "../utils/Utils";
import {emitter, pushConfirmationFlash, pushFlash, setFlash} from "../utils/Flash";
import {hasOpenChangeRequests, push} from "../api";
import ConfirmationDialog from "./ConfirmationDialog";
import CheckBox from "./CheckBox";

export default class Navigation extends React.PureComponent {

    constructor() {
        super();
        this.state = {
            loading: false,
            openChangeRequestsCount: 0,
            confirmationDialogOpen: false,
            confirmationQuestion: "",
            confirmationDialogAction: () => this,
            cancelDialogAction: () => this.setState({
                confirmationDialogOpen: false, pushOptions: {
                    includeEB: true,
                    includeOIDC: true,
                    includePdP: true
                }
            }),
            pushOptions: {
                includeEB: true,
                includeOIDC: true,
                includePdP: true,
            }
        };
    }

    componentDidMount() {
        spinner.onStart = () => this.setState({loading: true});
        spinner.onStop = () => this.setState({loading: false});
        this.changeRequests();
        emitter.addListener("changeRequests", this.changeRequests);
    }

    componentWillUnmount() {
        emitter.removeListener("changeRequests", this.changeRequests);
    }

    changeRequests = () => {
        hasOpenChangeRequests().then(r => this.setState({openChangeRequestsCount: r}));
    }

    componentDidUpdate() {
        if (this.state.loading) {
            if (!this.spinner) {
                this.spinner = new Spinner({
                    lines: 20, // The number of lines to draw
                    length: 15, // The length of each line
                    width: 3, // The line thickness
                    radius: 8, // The radius of the inner circle
                    color: "#4DB3CF", // #rgb or #rrggbb or array of colors
                    top: "40px",
                    position: "fixed"
                }).spin(this.spinnerNode);
            }
        } else {
            this.spinner = null;
        }
    }

    runPush = e => {
        stop(e);
        if (this.state.loading) {
            return;
        }
        this.setState({loading: true});

        const {pushOptions} = this.state;
        push(pushOptions.includeEB, pushOptions.includeOIDC, pushOptions.includePdP).then(json => {
            this.setState({
                loading: false, pushResults: json.deltas, pushOptions: {
                    includeEB: true,
                    includeOIDC: true,
                    includePdP: true
                }
            });
            const ok = json.status === "OK" || json.status === 200;
            setFlash(pushFlash(ok, this.props.currentUser), ok ? "info" : "error");
        });
    };

    renderPushButton = () => {
        const {currentUser} = this.props;
        if (currentUser.featureToggles.indexOf("PUSH") < 0) {
            return null;
        }
        const {loading} = this.state;
        const action = () => {
            this.setState({confirmationDialogOpen: false});
            this.runPush();
        };
        return (
            <a className={`push button ${loading ? "grey disabled" : "white"}`}
                  onClick={() => !this.state.loading && this.setState({
                      confirmationDialogOpen: true,
                      confirmationQuestion: pushConfirmationFlash(currentUser),
                      confirmationDialogAction: action
                  })}>{I18n.t("playground.runPush")}
            <i className="fa fa-refresh"></i>
        </a>
        );
    };

    renderItem(href, value, details = null) {
        return (
            <NavLink className={({isActive}) => {
                return isActive ? "menu-item active" : "menu-item";
            }

            } to={href}>
                {I18n.t("navigation." + value)}
                {details && <span className="details">{details}</span>}
            </NavLink>
        );
    }

    renderSpinner() {
        return this.state.loading ? <div className="spinner" ref={spinner => this.spinnerNode = spinner}/> : null;
    }

    render() {
        const {
            confirmationDialogOpen, cancelDialogAction, confirmationDialogAction, confirmationQuestion,
            openChangeRequestsCount, pushOptions
        } = this.state;
        const {currentUser} = this.props;
        return (
            <div className="navigation-container">
                <ConfirmationDialog isOpen={confirmationDialogOpen}
                                    cancel={cancelDialogAction}
                                    confirm={confirmationDialogAction}
                                    disableConfirm={!pushOptions.includeEB && !pushOptions.includeOIDC && !pushOptions.includePdP}
                                    question={confirmationQuestion}>
                    <div className="push-options">
                        <CheckBox name={I18n.t("playground.includeEB")}
                                  info={I18n.t("playground.includeEB")}
                                  value={pushOptions.includeEB}
                                  onChange={e => this.setState({
                                      pushOptions: {
                                          ...pushOptions,
                                          includeEB: e.target.checked
                                      }
                                  })}
                        />
                        <CheckBox name={I18n.t("playground.includeOIDC")}
                                  info={I18n.t("playground.includeOIDC")}
                                  value={pushOptions.includeOIDC}
                                  onChange={e => this.setState({
                                      pushOptions: {
                                          ...pushOptions,
                                          includeOIDC: e.target.checked
                                      }
                                  })}
                        />
                        <CheckBox name={I18n.t("playground.includePdP")}
                                  info={I18n.t("playground.includePdP")}
                                  value={pushOptions.includePdP}
                                  onChange={e => this.setState({
                                      pushOptions: {
                                          ...pushOptions,
                                          includePdP: e.target.checked
                                      }
                                  })}
                        />
                    </div>
                </ConfirmationDialog>
                <div className="navigation">
                    {this.renderItem("/search", "search")}
                    {this.renderItem("/system", "system")}
                    {(currentUser.featureToggles.some(feature => feature.toLowerCase() === "edugain")) && this.renderItem("/edugain", "edugain")}
                    {this.renderItem("/api", "api")}
                    {this.renderItem("/staging", "staging", openChangeRequestsCount === 0 ? null : openChangeRequestsCount)}
                    {this.renderItem("/scopes", "scopes")}
                    {this.renderItem("/activity", "activity")}
                    {this.renderItem("/policies", "policies")}
                    {this.renderSpinner()}
                    {this.renderPushButton()}
                </div>
            </div>
        );
    }
}

Navigation.propTypes = {
    currentUser: PropTypes.object.isRequired
};
