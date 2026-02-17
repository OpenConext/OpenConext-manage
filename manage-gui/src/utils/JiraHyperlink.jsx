import React from "react";

export const hyperlinkRevisionNote = (revisionNote, currentUser) => {
    if (!revisionNote) {
        return revisionNote;
    }
    const jiraBaseUrl = currentUser && currentUser.product && currentUser.product.jiraBaseUrl;
    if (!jiraBaseUrl) {
        return revisionNote;
    }
    const parts = revisionNote.split(/(CXT-\d+)/g);
    return (
        <span>
            {parts.map((part, index) => {
                if (part.match(/^CXT-\d+$/)) {
                    return <a key={index} href={`${jiraBaseUrl}${part}`} target="_blank" rel="noopener noreferrer">{part}</a>
                }
                return part;
            })}
        </span>
    );
}
