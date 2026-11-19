import { useEffect, useMemo, useState, type FormEvent } from 'react';
import { api, ApiError } from '../api';
import type { Category, Expense, Page } from '../types';
import { anomalyExplanation, dateLabel, money, todayIso } from '../format';
import { periodOptions, presetRange, type DateRange, type PeriodId } from '../periods';

interface Props {
  categories: Category[];
  version: number;
  onChanged: () => void;
}

const SOURCE_LABEL = { RULE: 'rule', MANUAL: 'manual', DEFAULT: 'no rule' } as const;

export default function ExpenseList({ categories, version, onChanged }: Props) {
  const [period, setPeriod] = useState<PeriodId>('this-month');
  // Custom range: the date inputs edit a draft; the list only changes when the draft is applied.
  const [customFrom, setCustomFrom] = useState('');
  const [customTo, setCustomTo] = useState('');
  const [draftFrom, setDraftFrom] = useState('');
  const [draftTo, setDraftTo] = useState('');
  const [categoryId, setCategoryId] = useState('');
  const [anomaliesOnly, setAnomaliesOnly] = useState(false);
  const [search, setSearch] = useState('');
  const [q, setQ] = useState('');
  const [page, setPage] = useState(0);
  const [data, setData] = useState<Page<Expense> | null>(null);
  const [error, setError] = useState<string | null>(null);

  // debounce free-text search
  useEffect(() => {
    const t = setTimeout(() => setQ(search), 300);
    return () => clearTimeout(t);
  }, [search]);

  const range: DateRange = useMemo(
    () => (period === 'custom' ? { from: customFrom || undefined, to: customTo || undefined } : presetRange(period)),
    [period, customFrom, customTo],
  );
  const draftError = draftFrom && draftTo && draftFrom > draftTo ? '“From” must be on or before “To”.' : null;
  const draftDirty = draftFrom !== customFrom || draftTo !== customTo;
  const canApply = Boolean(draftFrom && draftTo && !draftError && draftDirty);

  const choosePeriod = (id: PeriodId) => {
    if (id === 'custom') {
      // Start from what's on screen, so switching to "Custom range" doesn't change the list by itself.
      setCustomFrom(range.from ?? '');
      setCustomTo(range.to ?? '');
      setDraftFrom(range.from ?? presetRange('this-month').from ?? '');
      setDraftTo(range.to ?? todayIso());
    }
    setPeriod(id);
  };

  const applyCustomRange = (e: FormEvent) => {
    e.preventDefault();
    if (!canApply) return;
    setCustomFrom(draftFrom);
    setCustomTo(draftTo);
  };

  useEffect(() => setPage(0), [range.from, range.to, categoryId, anomaliesOnly, q]);

  useEffect(() => {
    let cancelled = false;
    api
      .listExpenses({ from: range.from, to: range.to, categoryId: categoryId ? Number(categoryId) : undefined, anomaliesOnly, q, page, size: 20 })
      .then((d) => {
        if (cancelled) return;
        // Deleting the last row of the last page leaves an empty page with no pager - step back to a real page.
        if (d.content.length === 0 && page > 0) {
          setPage(Math.max(0, d.totalPages - 1));
          return;
        }
        setData(d);
        setError(null);
      })
      .catch((e: ApiError) => {
        if (!cancelled) setError(e.message);
      });
    return () => {
      cancelled = true;
    };
  }, [range.from, range.to, categoryId, anomaliesOnly, q, page, version]);

  const remove = async (e: Expense) => {
    if (!window.confirm(`Delete ${money(e.amount)} at ${e.vendorName}?`)) return;
    try {
      await api.deleteExpense(e.id);
      onChanged();
    } catch (err) {
      setError((err as ApiError).message);
    }
  };

  return (
    <section className="card">
      <div className="toolbar wrap">
        <h3>Expenses</h3>
        <select value={period} onChange={(e) => choosePeriod(e.target.value as PeriodId)} aria-label="Period">
          {periodOptions().map((p) => (
            <option key={p.id} value={p.id}>
              {p.label}
            </option>
          ))}
        </select>
        {period === 'custom' && (
          <form className="range-form" onSubmit={applyCustomRange}>
            <input type="date" value={draftFrom} max={todayIso()} onChange={(e) => setDraftFrom(e.target.value)} aria-label="From date" />
            <span className="muted small">to</span>
            <input type="date" value={draftTo} max={todayIso()} onChange={(e) => setDraftTo(e.target.value)} aria-label="To date" />
            <button type="submit" className="primary" disabled={!canApply}>
              Apply
            </button>
          </form>
        )}
        <select value={categoryId} onChange={(e) => setCategoryId(e.target.value)} aria-label="Category">
          <option value="">All categories</option>
          {categories.map((c) => (
            <option key={c.id} value={c.id}>
              {c.name}
            </option>
          ))}
        </select>
        <input placeholder="Search vendor / description" value={search} onChange={(e) => setSearch(e.target.value)} />
        <label className="checkbox">
          <input type="checkbox" checked={anomaliesOnly} onChange={(e) => setAnomaliesOnly(e.target.checked)} />
          Anomalies only
        </label>
        {(period !== 'this-month' || categoryId || anomaliesOnly || search) && (
          <button
            className="link"
            onClick={() => {
              setPeriod('this-month');
              setCategoryId('');
              setAnomaliesOnly(false);
              setSearch('');
            }}
          >
            Clear filters
          </button>
        )}
      </div>

      <p className="muted small range-hint">
        Showing{' '}
        {range.from && range.to
          ? `${dateLabel(range.from)} – ${dateLabel(range.to)}`
          : range.from
            ? `from ${dateLabel(range.from)}`
            : range.to
              ? `up to ${dateLabel(range.to)}`
              : 'all dates'}
        {period === 'custom' && draftDirty && !draftError && ' · press Apply to update'}
      </p>

      {period === 'custom' && draftError && <div className="alert error">{draftError}</div>}
      {error && <div className="alert error">{error}</div>}

      {data && data.content.length === 0 && (
        <p className="muted">
          No expenses match these filters.
          {period !== 'all' && (
            <>
              {' '}
              <button className="link" onClick={() => setPeriod('all')}>
                Show all time
              </button>
            </>
          )}
        </p>
      )}

      {data && data.content.length > 0 && (
        <>
          <table className="table">
            <thead>
              <tr>
                <th>Date</th>
                <th>Vendor</th>
                <th>Category</th>
                <th className="num">Amount</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {data.content.map((e) => (
                <tr key={e.id} className={e.anomaly ? 'anomaly-row' : ''}>
                  <td className="nowrap">{dateLabel(e.date)}</td>
                  <td>
                    <strong>{e.vendorName}</strong>
                    {e.anomaly && (
                      <span className="badge danger" title={anomalyExplanation(e)}>
                        ⚠ Anomaly · {e.anomalyRatio}×
                      </span>
                    )}
                    {e.description && <div className="muted small">{e.description}</div>}
                  </td>
                  <td>
                    <span className="chip" style={{ borderColor: e.categoryColor, color: e.categoryColor }}>
                      {e.categoryName}
                    </span>
                    <span className="muted tiny"> {SOURCE_LABEL[e.categorySource]}</span>
                  </td>
                  <td className={`num ${e.anomaly ? 'strong danger-text' : ''}`}>{money(e.amount)}</td>
                  <td className="num">
                    <button className="icon" title="Delete" onClick={() => remove(e)}>
                      ✕
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <div className="pager">
            <button disabled={data.number === 0} onClick={() => setPage((p) => p - 1)}>
              ← Prev
            </button>
            <span className="muted small">
              Page {data.number + 1} of {Math.max(1, data.totalPages)} · {data.totalElements} expenses
            </span>
            <button disabled={data.number + 1 >= data.totalPages} onClick={() => setPage((p) => p + 1)}>
              Next →
            </button>
          </div>
        </>
      )}
    </section>
  );
}
