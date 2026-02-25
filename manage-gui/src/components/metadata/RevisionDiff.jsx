import React, {useEffect, useState} from "react";
import PropTypes from "prop-types";
import {detail, revisions} from "../../api";
import {Diff} from "./Diff";

export const RevisionDiff = ({id, type}) => {
    const [latestRevision, setLatestRevision] = useState(null);
    const [previousRevision, setPreviousRevision] = useState(null);
    const [loaded, setLoaded] = useState(false);

    useEffect(() => {
        Promise.all([revisions(type, id), detail(type, id)]).then(([revisionResults, currentMetaData]) => {
            const all = [...revisionResults, currentMetaData];
            all.sort((a, b) => new Date(b.revision.created) - new Date(a.revision.created));
            setLatestRevision(all[0] || null);
            setPreviousRevision(all[1] || null);
            setLoaded(true);
        });
    }, [id, type]);

    if (!loaded) {
        return null;
    }

    return (
        <Diff
            revision={latestRevision}
            previousRevision={previousRevision}
        />
    );
};

RevisionDiff.propTypes = {
    id: PropTypes.string.isRequired,
    type: PropTypes.string.isRequired
};
