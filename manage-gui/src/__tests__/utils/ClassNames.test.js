import { describe, it, expect } from 'vitest';
import { getClassNameValue, conditionalClassName } from '../../utils/ClassNames';

describe('ClassNames', () => {

    describe('getClassNameValue', () => {
        it('joins multiple class names with a space', () => {
            expect(getClassNameValue('a', 'b', 'c')).toBe('a b c');
        });

        it('filters out falsy values', () => {
            expect(getClassNameValue('a', undefined, 'b', null, '', false, 'c')).toBe('a b c');
        });

        it('returns empty string when all values are falsy', () => {
            expect(getClassNameValue(undefined, null, false, '')).toBe('');
        });

        it('returns single class name', () => {
            expect(getClassNameValue('only')).toBe('only');
        });

        it('returns empty string when called with no arguments', () => {
            expect(getClassNameValue()).toBe('');
        });
    });

    describe('conditionalClassName', () => {
        it('returns class name when predicate is true', () => {
            expect(conditionalClassName('active', true)).toBe('active');
        });

        it('returns undefined when predicate is false', () => {
            expect(conditionalClassName('active', false)).toBeUndefined();
        });
    });

    describe('getClassNameValue with conditionalClassName', () => {
        it('works together to build conditional class strings', () => {
            const result = getClassNameValue(
                'base',
                conditionalClassName('active', true),
                conditionalClassName('disabled', false)
            );
            expect(result).toBe('base active');
        });
    });

});
