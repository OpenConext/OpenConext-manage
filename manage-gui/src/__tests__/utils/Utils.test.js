import {collapseDotKeys, createDiffObject} from "../../utils/Utils";

test("collapseDotKeys", () => {
    const res = collapseDotKeys({
        "a.b.c": "val",
        "d.e.f": ["some"],
        "g": {a: 1},
        "x": "y"
    });
    expect(res.a.b.c).toStrictEqual("val");
    expect(res.d.e.f).toStrictEqual(["some"]);
    expect(res.g).toStrictEqual({a: 1});
    expect(res.x).toStrictEqual("y");
});

test("createDiff", () => {
    const res = createDiffObject(
        {a: "b", c: {d: "val", ign: "x"}, ign: [1, 2, 3]},
        {a: "x", c: {d: "changed"}, extra: [1]}
    );
    expect(res).toStrictEqual({a: "b", c: {d: "val"}});
});
