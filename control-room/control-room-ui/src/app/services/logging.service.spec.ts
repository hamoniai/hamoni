import { HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { describe, expect, it } from 'vitest';
import { createGenericProblemDetails, getProblemDetailsFromHttpError } from './logging.service';

describe('problem details', () => {
  it('builds the backend instance reference from the request id and status code', () => {
    // Given / When
    const problem = createGenericProblemDetails(
      404,
      'Page not found',
      'The requested page was not found.',
      'about:blank',
      'A7K2Q',
    );

    // Then
    expect(problem.instance).toBe('urn:hamoni:error?requestId=A7K2Q&code=404');
  });

  it('keeps the instance supplied by the backend', () => {
    // Given
    const error = new HttpErrorResponse({
      status: 404,
      error: {
        type: 'https://hamoni.ai/problems/not-found',
        title: 'Page not found',
        status: 404,
        detail: 'The requested page was not found.',
        instance: 'urn:hamoni:error?requestId=server&code=404',
      },
    });

    // When
    const problem = getProblemDetailsFromHttpError(error);

    // Then
    expect(problem.instance).toBe('urn:hamoni:error?requestId=server&code=404');
  });

  it('builds an instance from the response request id when the backend omits one', () => {
    // Given
    const error = new HttpErrorResponse({
      status: 500,
      error: { detail: 'Something failed.' },
      headers: new HttpHeaders({ 'Request-Id': 'header-id' }),
    });

    // When
    const problem = getProblemDetailsFromHttpError(error);

    // Then
    expect(problem.instance).toBe('urn:hamoni:error?requestId=header-id&code=500');
  });
});
