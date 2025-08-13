import React from "react";
import PropTypes from "prop-types";
import { default as ReactSelect, components } from "react-select";

import reactSelectStyles from "./reactSelectStyles.js";

export default class Select extends React.PureComponent {
    static defaultProps = {
        disabled: false
    };

    valueToOption = value => ({value: value, label: value});

    randomName = () => Math.random().toString(36).substring(2, 15) + Math.random().toString(36).substring(2, 15);

    render() {
        const {name = this.randomName(), value, className = "", ...rest} = this.props;

        const valueAsOption = typeof value === "string" ? this.valueToOption(value) : value;

        const CustomInput = (props) => {
            const { menuIsOpen } = props.selectProps;

            return (
                <components.Input
                    {...props}
                    style={{
                        opacity: menuIsOpen ? 1 : 0,
                        height: menuIsOpen ? 'auto' : 0,
                        padding: 0,
                        margin: 0,
                        border: 'none',
                        boxShadow: 'none',
                        outline: 'none',
                        backgroundColor: 'transparent'
                    }}
                />
            );
        };

        return (
            <ReactSelect
                {...rest}
                className={className}
                isDisabled={rest.disabled}
                styles={this.props.styles || reactSelectStyles}
                inputId={`react-select-${name}`}
                value={valueAsOption}
                onMenuClose={() => { document.activeElement?.blur(); }}
                components={{ Input: CustomInput }}
            />
        );
    }
}

Select.propTypes = {
    autoFocus: PropTypes.bool,
    disabled: PropTypes.bool,
    searchable: PropTypes.bool,
    options: PropTypes.array.isRequired,
    name: PropTypes.string,
    onChange: PropTypes.func.isRequired,
    styles: PropTypes.object,
    value: PropTypes.oneOfType([PropTypes.object, PropTypes.string, PropTypes.array])
};
