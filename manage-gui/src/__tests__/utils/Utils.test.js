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

test("createDiff nested arrau", () => {
    const data = {
        "entityid": "https://idp.test2.surfconext.nl",
        "allowedEntities": [
            {
                "name": "https://1234aaab.nl"
            },
            {
                "name": "https://serviceregistry.test2.surfconext.nl/simplesaml/module.php/saml/sp/metadata.php/default-sp"
            },
            {
                "name": "https://serviceregistry.test2.surfconext.nl/simplesaml/module.php/saml/sp/metadata.php/default-sp-2"
            },
            {
                "name": "http://mock-sp"
            },
            {
                "name": "https://oidc.test.client"
            }
        ],
        "metaDataFields": {
            "name:en": "OpenConext ServiceRegistry Mock",
        }
    }
    const nestedChangeRequest = {
        "allowedEntities": [
            {
                "name": "https://1234aaab.nl"
            },
            {
                "name": "https://serviceregistry.test2.surfconext.nl/simplesaml/module.php/saml/sp/metadata.php/default-sp"
            },
            {
                "name": "https://serviceregistry.test2.surfconext.nl/simplesaml/module.php/saml/sp/metadata.php/default-sp-2"
            },
            {
                "name": "https://profile.test2.surfconext.nl/authentication/metadata"
            },
            {
                "name": "http://mock-sp"
            },
            {
                "name": "https://oidc.test.client"
            }
        ]
    }
    const res = createDiffObject(
        data,
        nestedChangeRequest
    );
    expect(res).toStrictEqual({allowedEntities: data.allowedEntities});
});
