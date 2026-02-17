import { describe, test, expect } from 'vitest';
import { hyperlinkRevisionNote } from '../../utils/JiraHyperlink';

describe('hyperlinkRevisionNote', () => {
    const currentUser = {
        product: {
            jiraBaseUrl: 'https://jira.example.com/browse/'
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
});
