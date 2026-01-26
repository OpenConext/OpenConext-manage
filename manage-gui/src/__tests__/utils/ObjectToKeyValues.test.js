import {flattenObjectEntries} from "../../utils/FlattenObjectEntries";

test("objectToKeyValue() - contains all keys", () => {
    const testObject = {
        a: 'aValue',
        b: 'bValue',
        c: {
            c1: 'c1Value',
            c2: 'c2Value',
            c3: {
                c3a: 'c3aValue'
            }
        },
        d: null
    }
    const input = Object.entries(testObject)

    const result = flattenObjectEntries(input);

    expect(result.map(([key]) => key)).toEqual([ 'a', 'b', 'c.c1', 'c.c2', 'c.c3.c3a', 'd'])
})
