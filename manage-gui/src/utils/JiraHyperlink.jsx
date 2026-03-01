import React from "react";

const FALLBACK_JIRA_TICKET_PREFIXES = ["CXT", "SD"];

const escapeRegex = (value) => value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");

const parsePrefixes = (jiraTicketPrefixes) => {
    if (Array.isArray(jiraTicketPrefixes)) {
        return jiraTicketPrefixes.filter(Boolean);
    }
    if (typeof jiraTicketPrefixes === "string") {
        return jiraTicketPrefixes.split(",").map(prefix => prefix.trim()).filter(Boolean);
    }
    return [];
};

const configuredPrefixes = (currentUser) => {
    const jiraTicketPrefixes = currentUser && currentUser.product && currentUser.product.jiraTicketPrefixes;
    const prefixes = parsePrefixes(jiraTicketPrefixes);
    return prefixes.length > 0 ? prefixes : FALLBACK_JIRA_TICKET_PREFIXES;
};

const prefixesFromTicketKey = (ticketKey) => {
    if (typeof ticketKey !== "string") {
        return [];
    }
    const normalized = ticketKey.trim();
    if (!normalized) {
        return [];
    }
    const prefix = normalized.includes("-") ? normalized.split("-")[0] : normalized;
    return prefix ? [prefix] : [];
};

const ticketRegex = (prefixes, global = false) => {
    const sanitizedPrefixes = prefixes.map(escapeRegex);
    const pattern = `(?:${sanitizedPrefixes.join("|")})-\\d+`;
    return new RegExp(global ? `(${pattern})` : `^${pattern}$`, global ? "g" : undefined);
};

export const isJiraTicket = (text, prefixes = FALLBACK_JIRA_TICKET_PREFIXES) => text.match(ticketRegex(prefixes));

export const splitOnJiraTickets = (text, prefixes = FALLBACK_JIRA_TICKET_PREFIXES) => text.split(ticketRegex(prefixes, true));

export const hyperlinkRevisionNote = (revisionNote, currentUser, ticketKey) => {
    if (!revisionNote) {
        return revisionNote;
    }
    const jiraBaseUrl = currentUser && currentUser.product && currentUser.product.jiraBaseUrl;
    if (!jiraBaseUrl) {
        return revisionNote;
    }
    const ticketKeyPrefixes = prefixesFromTicketKey(ticketKey);
    const prefixes = [...new Set([...ticketKeyPrefixes, ...configuredPrefixes(currentUser)])];
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
}
