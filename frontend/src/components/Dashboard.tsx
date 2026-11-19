import { useEffect, useMemo, useState } from 'react';
import { api, ApiError } from '../api';
import type { Dashboard as DashboardData } from '../types';
import { anomalyExplanation, dateLabel, money, monthLabel } from '../format';

export default function Dashboard({ version }: { version: number }) {
  const [month, setMonth] = useState<string>('');
  const [data, setData] = useState<DashboardData | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [initialised, setInitialised] = useState(false);

  useEffect(() => {
    let cancelled = false;
    api
      .dashboard(month || undefined)
      .then((d) => {
        if (cancelled) return;
        // First load: jump to the most recent month that has data instead of showing all-time.
        if (!initialised) {
          setInitialised(true);
          if (!month && d.availableMonths.length > 0) {
            setMonth(d.availableMonths[0]);
            return;
          }
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
  }, [month, version, initialised]);

  if (error) return <div className="alert error">{error}</div>;
  if (!data) return <div className="muted">Loading dashboard…</div>;

  const maxCategory = Math.max(1, ...data.categoryTotals.map((c) => c.total));
  const maxVendor = Math.max(1, ...data.topVendors.map((v) => v.total));
  const topCategory = data.categoryTotals[0];

  return (
    <div className="stack">
      <div className="toolbar">
        <h2>Summary</h2>
        <select value={month} onChange={(e) => setMonth(e.target.value)} aria-label="Month">
          <option value="">All time</option>
          {data.availableMonths.map((m) => (
            <option key={m} value={m}>
              {monthLabel(m)}
            </option>
          ))}
        </select>
      </div>

      {data.expenseCount === 0 ? (
        <div className="card empty">
          No expenses {month ? `in ${monthLabel(month)}` : 'yet'}. Add one or import <code>sample-data/expenses_sample.csv</code> from the
          “Add / Import” tab.
        </div>
      ) : (
        <>
          <div className="stats">
            <Stat label="Total spend" value={money(data.totalSpend)} />
            <Stat label="Expenses" value={String(data.expenseCount)} />
            <Stat label="Top category" value={topCategory ? topCategory.category : '—'} sub={topCategory ? money(topCategory.total) : ''} />
            <Stat label="Anomalies" value={String(data.anomalyCount)} tone={data.anomalyCount > 0 ? 'danger' : undefined} />
          </div>

          <div className="grid two">
            <section className="card">
              <h3>Spend by category {month && <span className="muted">· {monthLabel(month)}</span>}</h3>
              <ul className="bars">
                {data.categoryTotals.map((c) => (
                  <li key={c.categoryId}>
                    <div className="bar-label">
                      <span className="dot" style={{ background: c.color }} />
                      {c.category}
                      <span className="muted small">
                        {c.count} · {Math.round((c.total / data.totalSpend) * 100)}%
                      </span>
                    </div>
                    <div className="bar-track">
                      <div className="bar-fill" style={{ width: `${(c.total / maxCategory) * 100}%`, background: c.color }} />
                    </div>
                    <div className="bar-value">{money(c.total)}</div>
                  </li>
                ))}
              </ul>
            </section>

            <section className="card">
              <h3>Top 5 vendors</h3>
              <ol className="bars ranked">
                {data.topVendors.map((v, i) => (
                  <li key={v.vendor}>
                    <div className="bar-label">
                      <span className="rank">{i + 1}</span>
                      {v.vendor}
                      <span className="muted small">{v.count} txn</span>
                    </div>
                    <div className="bar-track">
                      <div className="bar-fill accent" style={{ width: `${(v.total / maxVendor) * 100}%` }} />
                    </div>
                    <div className="bar-value">{money(v.total)}</div>
                  </li>
                ))}
              </ol>
            </section>
          </div>

          <section className="card anomaly-card">
            <h3>
              Anomalies <span className="badge danger">{data.anomalyCount}</span>
              <span className="muted small"> · amount more than 3× the average of other expenses in its category</span>
            </h3>
            {data.anomalies.length === 0 ? (
              <p className="muted">{month ? 'No anomalies this month.' : 'No anomalies.'}</p>
            ) : (
              <table className="table">
                <thead>
                  <tr>
                    <th>Date</th>
                    <th>Vendor</th>
                    <th>Category</th>
                    <th className="num">Amount</th>
                    <th>Why flagged</th>
                  </tr>
                </thead>
                <tbody>
                  {data.anomalies.map((a) => (
                    <tr key={a.id} className="anomaly-row">
                      <td>{dateLabel(a.date)}</td>
                      <td>
                        <strong>{a.vendorName}</strong>
                        {a.description && <div className="muted small">{a.description}</div>}
                      </td>
                      <td>
                        <span className="chip" style={{ borderColor: a.categoryColor, color: a.categoryColor }}>
                          {a.categoryName}
                        </span>
                      </td>
                      <td className="num strong">{money(a.amount)}</td>
                      <td className="small">{anomalyExplanation(a)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </section>
        </>
      )}

      <MonthlyTrend data={data} />
    </div>
  );
}

function Stat({ label, value, sub, tone }: { label: string; value: string; sub?: string; tone?: 'danger' }) {
  return (
    <div className={`card stat ${tone ?? ''}`}>
      <div className="muted small">{label}</div>
      <div className="stat-value">{value}</div>
      {sub && <div className="muted small">{sub}</div>}
    </div>
  );
}

/** Monthly totals per category: stacked bars + the exact numbers in a table. */
function MonthlyTrend({ data }: { data: DashboardData }) {
  const { months, categories, cell, monthTotals, colors } = useMemo(() => {
    const months = [...new Set(data.monthlyTrend.map((r) => r.month))].sort();
    const colors = new Map<string, string>();
    const cell = new Map<string, number>();
    const catTotals = new Map<string, number>();
    data.monthlyTrend.forEach((r) => {
      colors.set(r.category, r.color);
      cell.set(`${r.month}|${r.category}`, r.total);
      catTotals.set(r.category, (catTotals.get(r.category) ?? 0) + r.total);
    });
    const categories = [...catTotals.entries()].sort((a, b) => b[1] - a[1]).map(([c]) => c);
    const monthTotals = months.map((m) => categories.reduce((s, c) => s + (cell.get(`${m}|${c}`) ?? 0), 0));
    return { months, categories, cell, monthTotals, colors };
  }, [data.monthlyTrend]);

  if (months.length === 0) return null;
  const max = Math.max(1, ...monthTotals);

  return (
    <section className="card">
      <h3>Monthly totals per category <span className="muted small">· last {months.length} month(s)</span></h3>
      <div className="stacked">
        {months.map((m, i) => (
          <div className="stacked-col" key={m}>
            <div className="stacked-bar" style={{ height: `${(monthTotals[i] / max) * 100}%` }}>
              {categories.map((c) => {
                const v = cell.get(`${m}|${c}`) ?? 0;
                return v > 0 ? (
                  <div
                    key={c}
                    title={`${c}: ${money(v)}`}
                    style={{ flexGrow: v, background: colors.get(c) }}
                  />
                ) : null;
              })}
            </div>
            <div className="small strong">{money(monthTotals[i])}</div>
            <div className="muted small">{monthLabel(m)}</div>
          </div>
        ))}
      </div>
      <div className="table-scroll">
        <table className="table compact">
          <thead>
            <tr>
              <th>Category</th>
              {months.map((m) => (
                <th key={m} className="num">
                  {monthLabel(m)}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {categories.map((c) => (
              <tr key={c}>
                <td>
                  <span className="dot" style={{ background: colors.get(c) }} /> {c}
                </td>
                {months.map((m) => (
                  <td key={m} className="num">
                    {cell.has(`${m}|${c}`) ? money(cell.get(`${m}|${c}`)) : <span className="muted">—</span>}
                  </td>
                ))}
              </tr>
            ))}
            <tr className="total-row">
              <td>Total</td>
              {monthTotals.map((t, i) => (
                <td key={months[i]} className="num">
                  {money(t)}
                </td>
              ))}
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  );
}
