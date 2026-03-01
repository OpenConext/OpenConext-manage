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

const ticketRegex = (prefixes) => {
    // Match Jira keys for allowed prefixes only, e.g. CXT-12345 or SD-987.
    const prefixesPattern = prefixes.map(escapeRegex).join("|");
    const jiraKeyPattern = `(?:${prefixesPattern})-\\d+`;
    return new RegExp(`^${jiraKeyPattern}$`);
};

const resolvePrefixes = (jiraTicketPrefixes, ticketKey) => {
    const configuredPrefixes = parsePrefixList(jiraTicketPrefixes);
    const ticketPrefix = prefixFromTicketKey(ticketKey);
    return ticketPrefix ? [...new Set([ticketPrefix, ...configuredPrefixes])] : configuredPrefixes;
};

export const isJiraTicket = (text, prefixes = []) => prefixes.length > 0 && text.match(ticketRegex(prefixes));

const splitTicketRegex = (prefixes) => {
    const prefixesPattern = prefixes.map(escapeRegex).join("|");
    const jiraKeyPattern = `(?:${prefixesPattern})-\\d+`;
    return new RegExp(`(${jiraKeyPattern})`, "g");
};

export const splitOnJiraTickets = (text, prefixes = []) => prefixes.length > 0 ? text.split(splitTicketRegex(prefixes)) : [text];

export const jiraHyperlink = (note, currentUser, ticketKey) => {
    if (!note) {
        return note;
    }
    const jiraBaseUrl = currentUser?.product?.jiraBaseUrl;
    if (!jiraBaseUrl) {
        return note;
    }
    const jiraTicketPrefixes = currentUser?.product?.jiraTicketPrefixes;
    const prefixes = resolvePrefixes(jiraTicketPrefixes, ticketKey);
    const parts = splitOnJiraTickets(note, prefixes);

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
