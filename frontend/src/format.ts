const inr = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2 });

export const money = (n: number | null | undefined) => (n == null ? '—' : inr.format(n));

export const monthLabel = (ym: string) => {
  const [y, m] = ym.split('-').map(Number);
  return new Date(y, m - 1, 1).toLocaleDateString('en-IN', { month: 'short', year: 'numeric' });
};

export const dateLabel = (iso: string) =>
  new Date(iso + 'T00:00:00').toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });

/** Local date as YYYY-MM-DD (toISOString would shift to UTC and can return yesterday in IST). */
export const isoDate = (d: Date) => {
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
};

export const todayIso = () => isoDate(new Date());

export const anomalyExplanation = (e: { amount: number; categoryAverage: number | null; anomalyRatio: number | null; categoryName: string }) =>
  e.categoryAverage == null
    ? 'Flagged as anomaly'
    : `${e.anomalyRatio}× the average ${e.categoryName} expense (${money(e.categoryAverage)}). Threshold is 3×.`;
