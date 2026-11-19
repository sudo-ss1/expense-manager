export type CategorySource = 'RULE' | 'MANUAL' | 'DEFAULT';
export type ExpenseSource = 'MANUAL' | 'CSV';
export type MatchType = 'EXACT' | 'CONTAINS';

export interface Category {
  id: number;
  name: string;
  color: string;
  isDefault: boolean;
}

export interface Expense {
  id: number;
  date: string;
  amount: number;
  vendorName: string;
  description: string | null;
  categoryId: number;
  categoryName: string;
  categoryColor: string;
  categorySource: CategorySource;
  source: ExpenseSource;
  anomaly: boolean;
  categoryAverage: number | null;
  anomalyRatio: number | null;
}

export interface ExpenseRequest {
  date: string;
  amount: number;
  vendorName: string;
  description?: string;
  categoryId?: number | null;
  rememberVendor?: boolean;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface CategorizationPreview {
  categoryId: number;
  categoryName: string;
  categoryColor: string;
  source: CategorySource;
  matchedPattern: string | null;
}

export interface RowError {
  lineNumber: number;
  message: string;
}

export interface CsvImportResult {
  totalRows: number;
  imported: number;
  duplicatesSkipped: number;
  failed: number;
  anomaliesAmongImported: number;
  errors: RowError[];
}

export interface CategoryTotal {
  categoryId: number;
  category: string;
  color: string;
  total: number;
  count: number;
}

export interface VendorTotal {
  vendor: string;
  total: number;
  count: number;
}

export interface MonthlyCategoryTotal {
  month: string;
  category: string;
  color: string;
  total: number;
}

export interface Dashboard {
  month: string | null;
  availableMonths: string[];
  totalSpend: number;
  expenseCount: number;
  categoryTotals: CategoryTotal[];
  topVendors: VendorTotal[];
  anomalyCount: number;
  anomalies: Expense[];
  monthlyTrend: MonthlyCategoryTotal[];
}

export interface Rule {
  id: number;
  pattern: string;
  matchType: MatchType;
  categoryId: number;
  categoryName: string;
  categoryColor: string;
}

export interface ReapplyResult {
  evaluated: number;
  recategorized: number;
}
