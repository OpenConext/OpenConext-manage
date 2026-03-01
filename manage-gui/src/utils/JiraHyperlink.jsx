import React from "react";

const escapeRegex = value => value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");

const parsePrefixList = value => {
    if (Array.isArray(value)) {
        return value.filter(Boolean);
    }
    if (typeof value === "string") {
        return value.split(",").map(prefix => prefix.trim()).filter(Boolean);
    }
    return [];
};

const prefixFromTicketKey = ticketKey => {
    if (typeof ticketKey !== "string") {
        return null;
    }
    const normalized = ticketKey.trim();
    if (!normalized) {
        return null;
    }
    return normalized.includes("-") ? normalized.split("-")[0] : normalized;
};

const ticketRegex = (prefixes, global = false) => {
    // Match Jira keys for allowed prefixes only, e.g. CXT-12345 or SD-987.
    const pattern = `(?:${prefixes.map(escapeRegex).join("|")})-\\d+`;
    return new RegExp(global ? `(${pattern})` : `^${pattern}$`, global ? "g" : undefined);
};

const resolvePrefixes = (currentUser, ticketKey) => {
    const configuredPrefixes = parsePrefixList(currentUser?.product?.jiraTicketPrefixes);
    const ticketPrefix = prefixFromTicketKey(ticketKey);
    return ticketPrefix ? [...new Set([ticketPrefix, ...configuredPrefixes])] : configuredPrefixes;
};

export const isJiraTicket = (text, prefixes = []) => prefixes.length > 0 && text.match(ticketRegex(prefixes));

export const splitOnJiraTickets = (text, prefixes = []) => prefixes.length > 0 ? text.split(ticketRegex(prefixes, true)) : [text];

export const hyperlinkRevisionNote = (revisionNote, currentUser, ticketKey) => {
    if (!revisionNote) {
        return revisionNote;
    }
    const jiraBaseUrl = currentUser?.product?.jiraBaseUrl;
    if (!jiraBaseUrl) {
        return revisionNote;
    }
    const prefixes = resolvePrefixes(currentUser, ticketKey);
    const parts = splitOnJiraTickets(revisionNote, prefixes);

    return (
        <span>
            {parts.map((part, index) => (
                isJiraTicket(part, prefixes)
                    ? <a key={index} href={`${jiraBaseUrl}${part}`} target="_blank" rel="noopener noreferrer">{part}</a>
                    : part
            ))}
        </span>
    );
};
