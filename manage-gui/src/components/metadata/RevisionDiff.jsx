import React, {useEffect, useState} from "react";
import PropTypes from "prop-types";
import I18n from "i18n-js";
import {detail, latestRevision} from "../../api";
import {Diff} from "./Diff";

export const RevisionDiff = ({id, type}) => {
    const [currentRevision, setCurrentRevision] = useState(null);
    const [previousRevision, setPreviousRevision] = useState(null);
    const [loaded, setLoaded] = useState(false);
    const [error, setError] = useState(false);

    useEffect(() => {
        Promise.all([latestRevision(type, id), detail(type, id)])
            .then(([prevRevision, currentMetaData]) => {
                setCurrentRevision(currentMetaData);
                setPreviousRevision(prevRevision);
                setLoaded(true);
            })
            .catch(() => setError(true));
    }, [id, type]);

    if (error) {
        return <p className="error">{I18n.t("revisions.error")}</p>;
    }

    if (!loaded) {
        return null;
    }

    return (
        <Diff
            revision={currentRevision}
            previousRevision={previousRevision}
        />
    );
};

RevisionDiff.propTypes = {
    id: PropTypes.string.isRequired,
    type: PropTypes.string.isRequired
};
