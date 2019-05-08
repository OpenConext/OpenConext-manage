import {options} from "../../../utils/MetaDataConfiguration";

test("Do not show already selected metadata", () => {
    //WIP
    const res = () => options({},[]);
    expect(res).toThrow(TypeError);
});
