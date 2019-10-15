import {isEmpty} from "../../utils/Utils";

test("0 is not empty", () => {
  const res = isEmpty(0);
  expect(res).toBe(false);
});
