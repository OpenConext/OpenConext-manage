export const getClassNameValue = (...args) => args.filter(Boolean).join(" ");

export const conditionalClassName = (className, predicate) => predicate ? className : undefined;

