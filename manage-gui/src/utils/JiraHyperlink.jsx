import React from "react";

export const isCxtTicket = (text) => text.match(/^CXT-\d+$/);

export const splitOnCxtTickets = (text) => text.split(/(CXT-\d+)/g);

export const hyperlinkRevisionNote = (revisionNote, currentUser) => {
    if (!revisionNote) {
        return revisionNote;
    }
    const jiraBaseUrl = currentUser && currentUser.product && currentUser.product.jiraBaseUrl;
    if (!jiraBaseUrl) {
        return revisionNote;
    }
    const parts = splitOnCxtTickets(revisionNote);
    return (
        <span>
            {parts.map((part, index) => (
                isCxtTicket(part)
                    ? <a key={index} href={`${jiraBaseUrl}${part}`} target="_blank" rel="noopener noreferrer">{part}</a>
                    : part
            ))}
        </span>
    );
}
