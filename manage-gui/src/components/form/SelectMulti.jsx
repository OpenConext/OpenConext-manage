import React from "react";
import PropTypes from "prop-types";
import {Select} from "./../../components";
import {fetchEnumValues} from "../../api";

export default class SelectMulti extends React.PureComponent {

    state = {
        fetchValues: [],
    };

    componentDidMount() {
        const {fetchValue} = this.props;
        if (fetchValue) {
            fetchEnumValues(fetchValue).then(res => this.setState({fetchValues: res}));
        }

    }

    valuesToOptions(values) {
        return values.map(value => ({value: value, label: value}));
    }

    optionsToValues(options) {
        return options.map(option => option.value);
    }

    render() {
        const {enumValues, fetchValue, onChange, value, ...rest} = this.props;
        const {fetchValues} = this.state;

        const selectedOptions = this.valuesToOptions(value);
        const options = this.valuesToOptions(fetchValue ? fetchValues : enumValues);

        return (
            <Select
                {...rest}
                isMulti={true}
                onChange={options => onChange(this.optionsToValues(options))}
                optionRenderer={option => option.label}
                options={options}
                value={selectedOptions}
            />
        );
    }
}

SelectMulti.propTypes = {
    enumValues: PropTypes.array.isRequired,
    fetchValue: PropTypes.string
};
