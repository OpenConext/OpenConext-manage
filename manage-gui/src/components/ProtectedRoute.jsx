import React from "react";
import {Navigate, Route} from "react-router-dom";
import PropTypes from "prop-types";

export default function ProtectedRoute({path, guest, render}) {
  if (guest) {
    return <Navigate to={"/"}/>;
  }
  return <Route path={path} render={render}/>;
}

ProtectedRoute.propTypes = {
  path: PropTypes.string.isRequired,
  guest: PropTypes.bool.isRequired,
  render: PropTypes.func.isRequired
};
