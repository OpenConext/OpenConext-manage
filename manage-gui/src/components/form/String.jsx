import React from "react";
import PropTypes from "prop-types";

const String = ({onChange, ...rest}) => (
    <input {...rest} onChange={e => onChange(e.target.value)} type="text"/>
);

String.propTypes = {
    autoFocus: PropTypes.bool,
    disabled: PropTypes.bool,
    onChange: PropTypes.func.isRequired,
    value: PropTypes.string.isRequired
};

export default String;
