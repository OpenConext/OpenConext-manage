import React from "react";
import I18n from "i18n-js";
import DOMPurify from "dompurify";
import {DiffPatcher, formatters} from "jsondiffpatch";
import cloneDeep from "lodash.clonedeep";
import {sortDict} from "../../utils/Utils";

const ignoreInDiff = ["id", "eid", "revisionid", "user", "created", "ip", "revisionnote"];

const differ = new DiffPatcher({
    // https://github.com/benjamine/jsondiffpatch/blob/HEAD/docs/arrays.md
    objectHash: (obj, index) => obj.name || obj.level || obj.type || obj.source || obj.value || "$$index:" + index
});

export const Diff = ({revision, previousRevision}) => {
    const rev = cloneDeep(revision.data);
    ignoreInDiff.forEach(ignore => delete rev[ignore]);
    sortDict(rev);

    const prev = cloneDeep(previousRevision?.data ?? {});
    ignoreInDiff.forEach(ignore => delete prev[ignore]);
    sortDict(prev);

    const diffs = differ.diff(prev, rev);
    const html = DOMPurify.sanitize(formatters.html.format(diffs));

    // Todo: apply style to the "identical" message
    return diffs
        ? <p dangerouslySetInnerHTML={{__html: html}}/>
        : <p>{I18n.t("revisions.identical")}</p>;
};
