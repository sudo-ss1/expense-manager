import { useEffect, useMemo, useState, type FormEvent } from 'react';
import { api, ApiError } from '../api';
import type { Category, MatchType, Rule } from '../types';

interface Props {
  categories: Category[];
  version: number;
  onChanged: () => void;
}

export default function RulesPanel({ categories, version, onChanged }: Props) {
  const [rules, setRules] = useState<Rule[]>([]);
  const [pattern, setPattern] = useState('');
  const [matchType, setMatchType] = useState<MatchType>('CONTAINS');
  const [categoryId, setCategoryId] = useState('');
  const [newCat, setNewCat] = useState({ name: '', color: '#0ea5e9' });
  const [message, setMessage] = useState<{ kind: 'success' | 'error'; text: string } | null>(null);
  const [filter, setFilter] = useState('');

  useEffect(() => {
    api.rules().then(setRules).catch((e: ApiError) => setMessage({ kind: 'error', text: e.message }));
  }, [version]);

  const grouped = useMemo(() => {
    const f = filter.trim().toLowerCase();
    const map = new Map<string, Rule[]>();
    rules
      .filter((r) => !f || r.pattern.includes(f) || r.categoryName.toLowerCase().includes(f))
      .forEach((r) => map.set(r.categoryName, [...(map.get(r.categoryName) ?? []), r]));
    return [...map.entries()];
  }, [rules, filter]);

  const run = async (fn: () => Promise<string>) => {
    try {
      setMessage({ kind: 'success', text: await fn() });
      onChanged();
    } catch (e) {
      const err = e as ApiError;
      setMessage({ kind: 'error', text: [err.message, ...(err.details ?? [])].join(' · ') });
    }
  };

  const addRule = (e: FormEvent) => {
    e.preventDefault();
    if (!pattern.trim() || !categoryId) return;
    run(async () => {
      const r = await api.createRule(pattern, matchType, Number(categoryId));
      setPattern('');
      return `Rule saved: “${r.pattern}” → ${r.categoryName}. Use “Re-apply” to update existing expenses.`;
    });
  };

  const addCategory = (e: FormEvent) => {
    e.preventDefault();
    if (!newCat.name.trim()) return;
    run(async () => {
      const c = await api.createCategory(newCat.name.trim(), newCat.color);
      setNewCat({ name: '', color: '#0ea5e9' });
      return `Category “${c.name}” created.`;
    });
  };

  const reapply = () =>
    run(async () => {
      const r = await api.reapplyRules();
      return `Re-applied rules to ${r.evaluated} expenses - ${r.recategorized} changed category. Manually categorised expenses were left alone.`;
    });

  return (
    <div className="stack">
      {message && <div className={`alert ${message.kind}`}>{message.text}</div>}
      <div className="grid two">
        <section className="card">
          <h3>Add vendor rule</h3>
          <p className="muted small">
            <strong>Contains</strong> matches whole words anywhere in the vendor name (“uber” matches “Uber India” but not “Tuber”).
            <strong> Exact</strong> must match the full name. Most specific rule wins: exact first, then the pattern with the most words - so
            “uber eats” → Food beats “uber” → Transport.
          </p>
          <form className="form" onSubmit={addRule}>
            <div className="row">
              <label>
                Vendor pattern
                <input value={pattern} placeholder="e.g. chai point" onChange={(e) => setPattern(e.target.value)} />
              </label>
              <label>
                Match
                <select value={matchType} onChange={(e) => setMatchType(e.target.value as MatchType)}>
                  <option value="CONTAINS">Contains</option>
                  <option value="EXACT">Exact</option>
                </select>
              </label>
            </div>
            <label>
              Category
              <select value={categoryId} onChange={(e) => setCategoryId(e.target.value)}>
                <option value="">Choose…</option>
                {categories.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))}
              </select>
            </label>
            <button className="primary" disabled={!pattern.trim() || !categoryId}>
              Save rule
            </button>
          </form>
        </section>

        <section className="card">
          <h3>Categories</h3>
          <div className="chips">
            {categories.map((c) => (
              <span key={c.id} className="chip" style={{ borderColor: c.color, color: c.color }}>
                {c.name}
                {c.isDefault && ' (default)'}
              </span>
            ))}
          </div>
          <form className="form inline" onSubmit={addCategory}>
            <input placeholder="New category" value={newCat.name} onChange={(e) => setNewCat({ ...newCat, name: e.target.value })} />
            <input type="color" value={newCat.color} onChange={(e) => setNewCat({ ...newCat, color: e.target.value })} />
            <button disabled={!newCat.name.trim()}>Add</button>
          </form>
          <hr />
          <p className="muted small">New rules apply to new expenses automatically. To update past expenses as well:</p>
          <button onClick={reapply}>Re-apply rules to existing expenses</button>
        </section>
      </div>

      <section className="card">
        <div className="toolbar">
          <h3>
            Vendor → category rules <span className="muted small">({rules.length})</span>
          </h3>
          <input placeholder="Filter rules" value={filter} onChange={(e) => setFilter(e.target.value)} />
        </div>
        <div className="rule-groups">
          {grouped.map(([category, list]) => (
            <div key={category} className="rule-group">
              <div className="rule-group-title">
                <span className="dot" style={{ background: list[0].categoryColor }} /> {category}
              </div>
              <div className="chips">
                {list.map((r) => (
                  <span key={r.id} className="rule-chip">
                    {r.pattern}
                    {r.matchType === 'EXACT' && <em className="tiny"> exact</em>}
                    <button
                      className="icon"
                      title="Delete rule"
                      onClick={() =>
                        run(async () => {
                          await api.deleteRule(r.id);
                          return `Rule “${r.pattern}” deleted.`;
                        })
                      }
                    >
                      ✕
                    </button>
                  </span>
                ))}
              </div>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}
