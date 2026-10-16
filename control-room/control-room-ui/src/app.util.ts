/**
 * Common utilities required throughout the app
 */

/**
 * A regex pattern to match ISO 8601 date strings.
 */
const datePattern = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d+)?(?:Z|[+-]\d{2}:?\d{2})$/;

/**
 * Convert date fields in an object. Can be used directly as a parser.
 * This uses a regex to check the value. Since all dates are provided as ISO8601
 * instants, we can detect.
 */
export function convertDates<T>(obj: any): T {
    if (obj === null || typeof obj !== 'object') return obj;

    for (const key in obj) {
        const value = obj[key];

        // Check for ISO date string pattern
        if (typeof value === 'string' && datePattern.test(value)) {
            obj[key] = new Date(value);
        } else if (typeof value === 'object') {
            convertDates(value);
        }
    }
    return obj;
}

const alphaNumericCharacters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';

/**
 * Generate a random alphanumeric string of length. Do not use this for cryptographic actviites.
 * @param length The length of the string to generate
 * @returns A random string of length.
 */
export function generateRandomString(length: number): string {
  const array = new Uint8Array(length);
  window.crypto.getRandomValues(array);
  return Array.from(array)
    .map((x) => alphaNumericCharacters[x % alphaNumericCharacters.length])
    .join('');
}

/**
 * A type safe filter for observables.
 */
export function observableFilterIsValue<T>(value: T | null | undefined): value is T {
  return value !== null && value !== undefined;
}

/**
 * A consistent way to generate hydrations parameters
 */
export function createHydrationQueryParameters(hydrationIncludes: string[] | undefined): Record<string, string> {
    if (!hydrationIncludes || hydrationIncludes.length == 0) {
        return {}
    } else {
        return {hydrate: hydrationIncludes.join("+")}
    }
}