const selectStyles = {
  input: () => {},
  control: (provided, { isFocused }) => {
    return {
      ...provided,
      boxShadow: isFocused ? "0 0 3px 3px #65acff" : "",
      borderColor: "#d9d9d8",
      "&:hover": {
        borderColor: "#d9d9d8"
      }
    };
  },
  multiValue: provided => {
    return { ...provided, backgroundColor: "#4DB2CF", color: "white"};
  },
  multiValueLabel: provided => {
    return { ...provided, color: "white", fontSize: "14px" };
  }
};

export default selectStyles;
