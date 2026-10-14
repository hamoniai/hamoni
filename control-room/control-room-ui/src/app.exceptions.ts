/**
 * An exception that has both a title and detailed message.
 */
export class AppError extends Error {
  constructor(
    message: string, 
    public readonly detail?: string,
    options?: ErrorOptions
  ) {
    super(message, options);
    this.name = 'AppError';
  }
}
