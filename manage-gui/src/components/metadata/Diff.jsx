import React from "react";
import I18n from "../../locale/I18n";
import DOMPurify from "dompurify";
import {create} from "jsondiffpatch";

import * as htmlFormatter from "jsondiffpatch/formatters/html";

import cloneDeep from "lodash.clonedeep";
import {sortDict} from "../../utils/Utils";
import "./Diff.scss";

const ignoreInDiff = ["id", "eid", "revisionid", "user", "created", "ip", "revisionnote"];

const differ = create({
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
    const html = DOMPurify.sanitize(htmlFormatter.format(diffs));

    return diffs
        ? <div dangerouslySetInnerHTML={{__html: html}}/>
        : <p className="diff-identical">{I18n.t("revisions.identical")}</p>;
};
