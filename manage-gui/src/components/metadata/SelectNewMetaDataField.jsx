import React from "react";
import PropTypes from "prop-types";
import {Select} from "./../../components";
import {options} from "../../utils/MetaDataConfiguration";

import "./SelectNewMetaDataField.scss";

export default function SelectNewMetaDataField({onChange, configuration, metaDataFields, placeholder = "Select..."}) {

    return (
        <Select className="select-new-metadata"
                onChange={option => option && onChange(option.value)}
                options={options(configuration, metaDataFields)}
                value={null}
                name="select-new-metadata"
                searchable={true}
                placeholder={placeholder}
                onFocus={() => setTimeout(() => window.scrollTo(0, document.body.scrollHeight), 150)}/>
    );

}

SelectNewMetaDataField.propTypes = {
    onChange: PropTypes.func.isRequired,
    configuration: PropTypes.object.isRequired,
    metaDataFields: PropTypes.object.isRequired,
    placeholder: PropTypes.string
};
