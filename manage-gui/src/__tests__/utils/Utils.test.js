import { describe, it, expect } from 'vitest';
import {collapseDotKeys, createDiffObject, groupBy, groupPolicyAttributes, sortDict} from "../../utils/Utils";

describe('Utils', () => {
    it("collapseDotKeys", () => {
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

    it("createDiff", () => {
        const res = createDiffObject(
            {a: "b", c: {d: "val", ign: "x"}, ign: [1, 2, 3]},
            {a: "x", c: {d: "changed"}, extra: [1]}
        );
        expect(res).toStrictEqual({a: "b", c: {d: "val"}});
    });

    it("createDiff nested array", () => {
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

    it("sortDict", () => {
        const data = {a: "a", l1: [{name: "x"}, {name: "a"}], l2: [null, "", undefined]};
        sortDict(data);
        const res = {a: "a", l1: [{name: "a"}, {name: "x"}], l2: [null, "", undefined]};
        expect(data).toStrictEqual(res);
    });

    it("createDiffObjectArpNull", () => {
        const data = {
            "metaDataFields": {
                "name:en": "OIDC SP",
                "accessTokenValidity": 9999,
            },
            "arp": {
                "enabled": true,
                "attributes": {"urn:mace:dir:attribute-def:cn": [{"value": "*", "source": "idp", "motivation": ""}]}
            }
        }
        const nestedChangeRequest = {
            "metaDataFields": {"accessTokenValidity": 86400}, "arp": null
        }
        const res = createDiffObject(
            data,
            nestedChangeRequest
        );
        const expected = {"metaDataFields": {"accessTokenValidity": 9999}, "arp": {}}
        expect(res).toStrictEqual(expected);
    })

    it("createDiffObjectArp", () => {
        const data = {
            "metaDataFields": {
                "name:en": "OIDC SP",
                "accessTokenValidity": 9999,
            },
            "arp": {
                "enabled": true,
                "attributes": {
                    "urn:mace:dir:attribute-def:cn": [{"value": "*", "source": "idp", "motivation": ""}]
                }
            }
        }
        const nestedChangeRequest = {
            "arp": {
                "enabled": true,
                "attributes": {
                    "urn:mace:dir:attribute-def:cn": [{"value": "*", "source": "idp", "motivation": ""}],
                    "urn:mace:dir:attribute-def:extra": [{"value": "*", "source": "idp", "motivation": ""}],
                }
            }

        }
        const res = createDiffObject(
            data,
            nestedChangeRequest
        );
        const expected = {
            "arp": {
                "enabled": true,
                attributes: {
                    "urn:mace:dir:attribute-def:cn": [{"value": "*", "source": "idp", "motivation": ""}]
                }
            }
        }
        expect(res).toStrictEqual(expected);
    })

    it("groupBy", () => {
        let arr = [
            {specie: "sheep", name: "joe"},
            {specie: "sheep", name: "margret"},
            {specie: "wolf", name: "growll"},
        ];
        let res = groupBy(arr, "specie");
        expect(res.sheep.map(s => s.name)).toEqual(["joe", "margret"]);

        arr = [{"name": "urn:mace:terena.org:attribute-def:schacHomeOrganization", "value": "", "negated": false, "index": 0}]
        res = groupBy(arr, "name");
        expect(Object.keys(res)[0]).toEqual("urn:mace:terena.org:attribute-def:schacHomeOrganization");
    })

    it("groupPolicyAttributes", () => {
        let arr = [
            {name: "urn:collab:group:surfteams.nl", negated: false, value: "rockets"},
            {name: "urn:collab:group:surfteams.nl", negated: false, value: "clouds"},
            {name: "urn:collab:mace:eduEntitlement", negated: false, value: "test"}
        ];
        let res = groupPolicyAttributes(arr);
        expect(res['urn:collab:group:surfteams.nl#0'].length).toEqual(2);

        arr = [
            {name: "urn:collab:group:surfteams.nl", negated: false, value: "rockets", groupID: 0},
            {name: "urn:collab:group:surfteams.nl", negated: false, value: "clouds", groupID: 1},
            {name: "urn:collab:mace:eduEntitlement", negated: false, value: "test"}
        ];
        res = groupPolicyAttributes(arr);
        expect(res['urn:collab:group:surfteams.nl#0'].length).toEqual(1);
        expect(res['urn:collab:group:surfteams.nl#1'].length).toEqual(1);
    })
});
