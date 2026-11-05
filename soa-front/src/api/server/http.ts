import { z } from 'zod';
import '~/lib/zod-locale';

export type ServiceError = { status: number; message: string; field?: string };
export type ApiResult<T> =
  | { ok: true; data: T }
  | { ok: false; error: ServiceError };

const apiError = z.object({
  status: z.number(),
  message: z.string(),
  field: z.string().nullish(),
});

export async function request<T>(
  service: 'marines' | 'starships',
  path: string,
  schema: z.ZodType<T>,
  init?: RequestInit,
): Promise<ApiResult<T>> {
  const base =
    service === 'marines'
      ? (process.env.SPACE_MARINES_API_URL ?? 'https://localhost:8080/collection-service')
      : (process.env.STARSHIPS_API_URL ?? 'https://localhost:8080/starship-service');

  try {
    // Keep gateway resource prefixes such as /collection-service before /api/v1.
    const url = new URL(`${base.replace(/\/+$/, '')}/${path.replace(/^\/+/, '')}`);
    const response = await fetch(url, {
      ...init,
      headers: { Accept: 'application/json', ...init?.headers },
      cache: 'no-store',
    });
    const text = await response.text();
    let body: unknown;
    try {
      body = text ? JSON.parse(text) : undefined;
    } catch {
      body = text;
    }

    if (!response.ok) {
      const parsed = apiError.safeParse(body);
      return {
        ok: false,
        error: parsed.success
          ? {
              status: response.status,
              message: parsed.data.message,
              field: parsed.data.field ?? undefined,
            }
          : {
              status: response.status,
              message: `Сервис вернул ошибку HTTP ${response.status}`,
            },
      };
    }

    const parsed = schema.safeParse(body);
    if (!parsed.success)
      return {
        ok: false,
        error: {
          status: 0,
          message: 'Ответ сервиса не соответствует схеме данных',
        },
      };
    return { ok: true, data: parsed.data };
  } catch {
    return {
      ok: false,
      error: {
        status: 0,
        message: 'Не удалось связаться с сервисом',
      },
    };
  }
}

export function jsonBody(value: unknown): RequestInit {
  return {
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(value),
  };
}
