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

const resolvePrefixes = (jiraTicketPrefixes, ticketKey) => {
    const configuredPrefixes = parsePrefixList(jiraTicketPrefixes);
    const ticketPrefix = prefixFromTicketKey(ticketKey);
    return ticketPrefix ? [...new Set([ticketPrefix, ...configuredPrefixes])] : configuredPrefixes;
};

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

    const prefixesPattern = prefixes.map(escapeRegex).join("|");
    const jiraKeyPattern = `(?:${prefixesPattern})-\\d+`;
    const isJiraTicket = prefixes.length > 0 ? new RegExp(`^${jiraKeyPattern}$`) : null;
    const noteParts = prefixes.length > 0 ? note.split(new RegExp(`(${jiraKeyPattern})`, "g")) : [note];

    return (
        <span>
            {noteParts.map((part, index) => (
                isJiraTicket?.test(part)
                    ? <a key={index} href={`${jiraBaseUrl}${part}`} target="_blank" rel="noopener noreferrer">{part}</a>
                    : part
            ))}
        </span>
    );
};
