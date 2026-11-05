type LogContext = Record<string, unknown>;

function describeError(error: unknown, depth = 0): Record<string, unknown> {
  if (error instanceof Error) {
    return {
      name: error.name,
      message: error.message,
      stack: error.stack,
      code: 'code' in error ? error.code : undefined,
      cause: depth < 3 && error.cause !== undefined
        ? describeError(error.cause, depth + 1)
        : undefined,
    };
  }
  return { message: typeof error === 'string' ? error : 'Unknown error' };
}

// Request bodies, headers and full URLs are deliberately excluded by callers.
export function logError(event: string, error: unknown, context: LogContext = {}) {
  console.error('[frontend]', {
    timestamp: new Date().toISOString(),
    event,
    ...context,
    error: describeError(error),
  });
}
