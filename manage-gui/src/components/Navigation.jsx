import React from "react";
import I18n from "i18n-js";
import PropTypes from "prop-types";

import Spinner from "spin.js";
import spinner from "../lib/Spin";

import {NavLink} from "react-router-dom";

import "./Navigation.css";
import {stop} from "../utils/Utils";
import {pushConfirmationFlash, pushFlash, setFlash} from "../utils/Flash";
import {push} from "../api";
import ConfirmationDialog from "./ConfirmationDialog";

export default class Navigation extends React.PureComponent {

  constructor() {
    super();
    this.state = {
      loading: false,
      confirmationDialogOpen: false,
      confirmationQuestion: "",
      confirmationDialogAction: () => this,
      cancelDialogAction: () => this.setState({confirmationDialogOpen: false}),
    };
  }

  componentWillMount() {
    spinner.onStart = () => this.setState({loading: true});
    spinner.onStop = () => this.setState({loading: false});
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
    push().then(json => {
      this.setState({loading: false, pushResults: json.deltas});
      const ok = json.status === "OK"  || json.status === 200;
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
    return <a className={`push button ${loading ? "grey disabled" : "white"}`}
              onClick={() => !this.state.loading && this.setState({
                confirmationDialogOpen: true,
                confirmationQuestion: pushConfirmationFlash(currentUser),
                confirmationDialogAction: action
              })}>{I18n.t("playground.runPush")}
      <i className="fa fa-refresh"></i>
    </a>
  };

  renderItem(href, value) {
    return (
      <NavLink activeClassName="active" className="menu-item" to={href}>{I18n.t("navigation." + value)}</NavLink>
    );
  }

  renderSpinner() {
    return this.state.loading ? <div className="spinner" ref={spinner => this.spinnerNode = spinner}/> : null;
  }

  render() {
    const {confirmationDialogOpen, cancelDialogAction, confirmationDialogAction, confirmationQuestion} = this.state;
    const {currentUser} = this.props;
    return (
      <div className="navigation-container">
        <ConfirmationDialog isOpen={confirmationDialogOpen}
                            cancel={cancelDialogAction}
                            confirm={confirmationDialogAction}
                            question={confirmationQuestion}/>
        <div className="navigation">
          {this.renderItem("/search", "search")}
          {!currentUser.guest && this.renderItem("/import", "import")}
          {!currentUser.guest && this.renderItem("/system", "system")}
          {(!currentUser.guest && currentUser.featureToggles.some(feature => feature.toLowerCase() === "edugain")) && this.renderItem("/edugain", "edugain")}
          {this.renderItem("/api", "api")}
          {!currentUser.guest && this.renderItem("/support", "support")}
          {!currentUser.guest && this.renderItem("/scopes", "scopes")}
          {!currentUser.guest && this.renderItem("/activity", "activity")}
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
