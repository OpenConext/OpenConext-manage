import { describe, it, expect } from 'vitest';
import {isEmpty} from "../../utils/Utils";

describe('MetaDataConfiguration', () => {
    it("0 is not empty", () => {
        const res = isEmpty(0);
        expect(res).toBe(false);
    });
});
