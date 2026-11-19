import type {
  CategorizationPreview, Category, CsvImportResult, Dashboard, Expense, ExpenseRequest, MatchType, Page,
  ReapplyResult, Rule,
} from './types';

/** Error carrying the backend's { error, details[] } payload so forms can show field-level messages. */
export class ApiError extends Error {
  constructor(message: string, public readonly status: number, public readonly details: string[] = []) {
    super(message);
  }
}

const BASE = import.meta.env.VITE_API_BASE ?? '/api';

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  let res: Response;
  try {
    res = await fetch(BASE + path, init);
  } catch {
    throw new ApiError('Cannot reach the server. Is the backend running on port 8080?', 0);
  }
  if (!res.ok) {
    let message = `Request failed (${res.status})`;
    let details: string[] = [];
    try {
      const body = await res.json();
      message = body.error ?? message;
      details = body.details ?? [];
    } catch {
      /* non-JSON error body */
    }
    throw new ApiError(message, res.status, details);
  }
  return res.status === 204 ? (undefined as T) : ((await res.json()) as T);
}

const json = (method: string, body: unknown): RequestInit => ({
  method,
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify(body),
});

function query(params: Record<string, string | number | boolean | undefined | null>): string {
  const q = new URLSearchParams();
  Object.entries(params).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== '' && v !== false) q.set(k, String(v));
  });
  const s = q.toString();
  return s ? `?${s}` : '';
}

export const api = {
  dashboard: (month?: string) => request<Dashboard>(`/dashboard${query({ month })}`),

  listExpenses: (f: { month?: string; from?: string; to?: string; categoryId?: number; anomaliesOnly?: boolean; q?: string; page?: number; size?: number }) =>
    request<Page<Expense>>(`/expenses${query(f)}`),
  createExpense: (body: ExpenseRequest) => request<Expense>('/expenses', json('POST', body)),
  deleteExpense: (id: number) => request<void>(`/expenses/${id}`, { method: 'DELETE' }),
  previewCategory: (vendor: string, signal?: AbortSignal) =>
    request<CategorizationPreview>(`/expenses/categorize${query({ vendor })}`, { signal }),
  uploadCsv: (file: File) => {
    const form = new FormData();
    form.append('file', file);
    return request<CsvImportResult>('/expenses/upload', { method: 'POST', body: form });
  },

  categories: () => request<Category[]>('/categories'),
  createCategory: (name: string, color: string) => request<Category>('/categories', json('POST', { name, color })),

  rules: () => request<Rule[]>('/rules'),
  createRule: (pattern: string, matchType: MatchType, categoryId: number) =>
    request<Rule>('/rules', json('POST', { pattern, matchType, categoryId })),
  deleteRule: (id: number) => request<void>(`/rules/${id}`, { method: 'DELETE' }),
  reapplyRules: () => request<ReapplyResult>('/rules/reapply', { method: 'POST' }),
};
