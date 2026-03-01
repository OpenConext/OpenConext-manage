import { describe, test, expect } from 'vitest';
import { hyperlinkRevisionNote } from '../../utils/JiraHyperlink';

describe('hyperlinkRevisionNote', () => {
    const currentUser = {
        product: {
            jiraBaseUrl: 'https://jira.example.com/browse/',
            jiraTicketPrefixes: 'CXT,SD'
        }
    };

    test('returns the same text if it is null or empty', () => {
        expect(hyperlinkRevisionNote(null, currentUser)).toBeNull();
        expect(hyperlinkRevisionNote('', currentUser)).toBe('');
    });

    test('returns the same text if no jiraBaseUrl is provided', () => {
        const text = 'Check CXT-12345';
        const result = hyperlinkRevisionNote(text, null);
        expect(result).toBe(text);
    });

    test('replaces CXT-XXXXX with hyperlinks', () => {
        const text = 'Fixed in CXT-12345 and CXT-67890';
        const result = hyperlinkRevisionNote(text, currentUser);
        
        // result is a <span> containing mixed text and <a> elements
        expect(result.type).toBe('span');
        const children = result.props.children;
        expect(children).toHaveLength(5); // ["Fixed in ", <a>, " and ", <a>, ""]
        
        const link1 = children[1];
        expect(link1.type).toBe('a');
        expect(link1.props.href).toBe('https://jira.example.com/browse/CXT-12345');
        expect(link1.props.children).toBe('CXT-12345');
        
        const link2 = children[3];
        expect(link2.type).toBe('a');
        expect(link2.props.href).toBe('https://jira.example.com/browse/CXT-67890');
        expect(link2.props.children).toBe('CXT-67890');
    });

    test('replaces SD-XXXXX with hyperlinks', () => {
        const text = 'Fixed in SD-12345 and SD-67890';
        const result = hyperlinkRevisionNote(text, currentUser);

        expect(result.type).toBe('span');
        const children = result.props.children;
        expect(children).toHaveLength(5);

        const link1 = children[1];
        expect(link1.type).toBe('a');
        expect(link1.props.href).toBe('https://jira.example.com/browse/SD-12345');
        expect(link1.props.children).toBe('SD-12345');

        const link2 = children[3];
        expect(link2.type).toBe('a');
        expect(link2.props.href).toBe('https://jira.example.com/browse/SD-67890');
        expect(link2.props.children).toBe('SD-67890');
    });

    test('handles text with no CXT mentions', () => {
        const text = 'Just some regular text';
        const result = hyperlinkRevisionNote(text, currentUser);
        expect(result.type).toBe('span');
        expect(result.props.children).toHaveLength(1);
        expect(result.props.children[0]).toBe(text);
    });
    
    test('handles CXT mentions at the start or end of the text', () => {
        const text = 'CXT-12345 is the issue';
        const result = hyperlinkRevisionNote(text, currentUser);
        const children = result.props.children;
        expect(children[0]).toBe(""); // split result for start match
        expect(children[1].props.children).toBe('CXT-12345');
    });

    test('uses configured jiraTicketPrefixes from currentUser', () => {
        const currentUserWithPrefix = {
            product: {
                jiraBaseUrl: 'https://jira.example.com/browse/',
                jiraTicketPrefixes: 'ABC'
            }
        };

        const text = 'Fixed in ABC-123 and CXT-999';
        const result = hyperlinkRevisionNote(text, currentUserWithPrefix);
        const children = result.props.children;
        expect(children).toHaveLength(3);
        expect(children[1].type).toBe('a');
        expect(children[1].props.children).toBe('ABC-123');
        expect(children[2]).toBe(' and CXT-999');
    });

    test('combines explicit ticketKey prefix with configured prefixes', () => {
        const currentUserWithPrefix = {
            product: {
                jiraBaseUrl: 'https://jira.example.com/browse/',
                jiraTicketPrefixes: 'ABC'
            }
        };

        const text = 'Fixed in SD-123 and ABC-999';
        const result = hyperlinkRevisionNote(text, currentUserWithPrefix, 'SD');
        const children = result.props.children;
        expect(children).toHaveLength(5);
        expect(children[1].type).toBe('a');
        expect(children[1].props.children).toBe('SD-123');
        expect(children[3].type).toBe('a');
        expect(children[3].props.children).toBe('ABC-999');
    });

    test('accepts full ticketKey and extracts its prefix', () => {
        const text = 'Fixed in CXT-123 and SD-9';
        const result = hyperlinkRevisionNote(text, currentUser, 'CXT-123');
        const children = result.props.children;
        expect(children).toHaveLength(5);
        expect(children[1].type).toBe('a');
        expect(children[1].props.children).toBe('CXT-123');
        expect(children[3].type).toBe('a');
        expect(children[3].props.children).toBe('SD-9');
    });

    test('links ticketKey prefix and configured prefixes in one summary', () => {
        const text = 'ticketKey: XYZ-123456 no ticketkey: CXT-654321 SD-654321 BCA-654321';
        const result = hyperlinkRevisionNote(text, currentUser, 'XYZ-123456');
        const children = result.props.children;
        expect(children).toHaveLength(7);
        expect(children[1].props.children).toBe('XYZ-123456');
        expect(children[3].props.children).toBe('CXT-654321');
        expect(children[5].props.children).toBe('SD-654321');
        expect(children[6]).toBe(' BCA-654321');
    });
});
