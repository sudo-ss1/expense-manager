import { isoDate, monthLabel } from './format';

export type PeriodId = 'this-month' | 'last-3' | 'last-6' | 'last-12' | 'all' | 'custom';

/** Inclusive YYYY-MM-DD bounds; a missing bound means "unbounded". */
export interface DateRange {
  from?: string;
  to?: string;
}

/**
 * Presets are whole calendar months ending today: "Last 3 months" on 19 Nov = 1 Sep … 19 Nov
 * (the current month plus the 2 before it), which lines up with the monthly dashboard.
 */
const MONTHS_BEFORE_CURRENT: Partial<Record<PeriodId, number>> = {
  'this-month': 0,
  'last-3': 2,
  'last-6': 5,
  'last-12': 11,
};

export function presetRange(id: PeriodId, today = new Date()): DateRange {
  const back = MONTHS_BEFORE_CURRENT[id];
  if (back === undefined) return {}; // 'all' (and 'custom', which the caller fills in)
  return { from: isoDate(new Date(today.getFullYear(), today.getMonth() - back, 1)), to: isoDate(today) };
}

export function periodOptions(today = new Date()): { id: PeriodId; label: string }[] {
  const ym = isoDate(today).slice(0, 7);
  return [
    { id: 'this-month', label: `This month (${monthLabel(ym)})` },
    { id: 'last-3', label: 'Last 3 months' },
    { id: 'last-6', label: 'Last 6 months' },
    { id: 'last-12', label: 'Last 12 months' },
    { id: 'all', label: 'All time' },
    { id: 'custom', label: 'Custom range…' },
  ];
}
