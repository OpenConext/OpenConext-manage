import React, {useEffect, useState} from "react";
import PropTypes from "prop-types";
import I18n from "i18n-js";
import {detail, latestRevision} from "../../api";
import {Diff} from "./Diff";
import "./RevisionDiff.scss";

export const RevisionDiff = ({id, type}) => {
    const [currentRevision, setCurrentRevision] = useState(null);
    const [previousRevision, setPreviousRevision] = useState(null);
    const [loaded, setLoaded] = useState(false);
    const [error, setError] = useState(false);
    const [noRevisions, setNoRevisions] = useState(false);

    useEffect(() => {
        Promise.all([latestRevision(type, id), detail(type, id)])
            .then(([prevRevision, currentMetaData]) => {
                setCurrentRevision(currentMetaData);
                setPreviousRevision(prevRevision);
                setLoaded(true);
            })
            .catch(err => {
                if (err.response?.status === 404) {
                    setNoRevisions(true);
                } else {
                    setError(true);
                }
            });
    }, [id, type]);

    if (noRevisions) {
        return <p className="no-revisions">{I18n.t("revisions.noRevisions")}</p>;
    }

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
