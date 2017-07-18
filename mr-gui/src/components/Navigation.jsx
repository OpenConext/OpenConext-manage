import React from "react";
import I18n from "i18n-js";
import PropTypes from "prop-types";

import Spinner from "spin.js";
import spinner from "../lib/Spin";

import {NavLink} from "react-router-dom";

import "./Navigation.css";

export default class Navigation extends React.PureComponent {

    constructor() {
        super();
        this.state = {
            loading: false
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
                    lines: 25, // The number of lines to draw
                    length: 12, // The length of each line
                    width: 2, // The line thickness
                    radius: 8, // The radius of the inner circle
                    color: "#4DB3CF", // #rgb or #rrggbb or array of colors
                    top: "25%"
                }).spin(this.spinnerNode);
            }
        } else {
            this.spinner = null;
        }
    }

    renderItem(href, value) {
        return (
            <NavLink activeClassName="active" to={href}>{I18n.t("navigation." + value)}</NavLink>
        );
    }

    renderSpinner() {
        return this.state.loading ? <div className="spinner" ref={spinner => this.spinnerNode = spinner}/> : null;
    }

    render() {
        return (
            <div className="navigation-container">
                <div className="navigation">
                    {this.renderItem("/search", "search")}
                    {!this.props.currentUser.guest && this.renderItem("/system", "system")}
                    {this.renderItem("/api", "api")}
                    {this.renderSpinner()}
                </div>
            </div>
        );
    }
}

Navigation.propTypes = {
    currentUser: PropTypes.object.isRequired
};
