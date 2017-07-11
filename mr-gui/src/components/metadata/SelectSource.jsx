import React from "react";
import PropTypes from "prop-types";
import Select from "react-select";
import "react-select/dist/react-select.css";
import "./SelectSource.css";

export default class SelectSource extends React.PureComponent {

    renderOption = option => {
        return (
            <span className="select-option">
                <span className="select-label">
                    {option.label}
                </span>
            </span>
        );
    };

    render() {
        const {onChange, source, sources, disabled, autofocus = false} = this.props;
        const options = sources.map(s => {
            return {value: s, label: s};
        });
        return <Select className="select-state"
                       onChange={option => onChange(option.value)}
                       //optionRenderer={this.renderOption}
                       options={options}
                       value={source}
                       autofocus={autofocus}
                       searchable={false}
                       //valueRenderer={this.renderOption}
                       disabled={disabled || false}/>;
    }
}

SelectSource.propTypes = {
    onChange: PropTypes.func.isRequired,
    source: PropTypes.string.isRequired,
    sources: PropTypes.array.isRequired,
    disabled: PropTypes.bool,
    autofocus: PropTypes.bool
};


