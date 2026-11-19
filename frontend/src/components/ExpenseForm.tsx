import { useEffect, useState, type FormEvent } from 'react';
import { api, ApiError } from '../api';
import type { CategorizationPreview, Category, Expense } from '../types';
import { anomalyExplanation, money, todayIso } from '../format';

interface Props {
  categories: Category[];
  onCreated: () => void;
}

const empty = () => ({ date: todayIso(), amount: '', vendorName: '', description: '', categoryId: '', remember: false });

export default function ExpenseForm({ categories, onCreated }: Props) {
  const [form, setForm] = useState(empty);
  const [preview, setPreview] = useState<CategorizationPreview | null>(null);
  const [errors, setErrors] = useState<string[]>([]);
  const [saved, setSaved] = useState<Expense | null>(null);
  const [submitting, setSubmitting] = useState(false);

  // Live, debounced preview of what the rule engine will pick for this vendor.
  useEffect(() => {
    const vendor = form.vendorName.trim();
    if (!vendor) {
      setPreview(null);
      return;
    }
    const ctrl = new AbortController();
    const t = setTimeout(() => {
      api.previewCategory(vendor, ctrl.signal).then(setPreview).catch(() => undefined);
    }, 250);
    return () => {
      clearTimeout(t);
      ctrl.abort();
    };
  }, [form.vendorName]);

  const set = (field: keyof ReturnType<typeof empty>, value: string | boolean) =>
    setForm((f) => ({ ...f, [field]: value }));

  const validate = (): string[] => {
    const errs: string[] = [];
    const amount = Number(form.amount);
    if (!form.date) errs.push('Date is required');
    else if (form.date > todayIso()) errs.push('Date cannot be in the future');
    if (!form.amount || Number.isNaN(amount) || amount <= 0) errs.push('Amount must be a number greater than 0');
    else if (!/^\d+(\.\d{1,2})?$/.test(form.amount.trim())) errs.push('Amount can have at most 2 decimal places');
    if (!form.vendorName.trim()) errs.push('Vendor name is required');
    return errs;
  };

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setSaved(null);
    const errs = validate();
    setErrors(errs);
    if (errs.length) return;
    setSubmitting(true);
    try {
      const created = await api.createExpense({
        date: form.date,
        amount: Number(form.amount),
        vendorName: form.vendorName.trim(),
        description: form.description.trim() || undefined,
        categoryId: form.categoryId ? Number(form.categoryId) : null,
        rememberVendor: Boolean(form.categoryId) && form.remember,
      });
      setSaved(created);
      setForm({ ...empty(), date: form.date });
      onCreated();
    } catch (err) {
      const e2 = err as ApiError;
      setErrors(e2.details?.length ? e2.details : [e2.message]);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <section className="card">
      <h3>Add expense</h3>
      <form onSubmit={submit} className="form" noValidate>
        <div className="row">
          <label>
            Date
            <input type="date" value={form.date} max={todayIso()} onChange={(e) => set('date', e.target.value)} />
          </label>
          <label>
            Amount (₹)
            <input inputMode="decimal" placeholder="0.00" value={form.amount} onChange={(e) => set('amount', e.target.value)} />
          </label>
        </div>
        <label>
          Vendor name
          <input placeholder="e.g. Swiggy, Uber, Amazon" value={form.vendorName} onChange={(e) => set('vendorName', e.target.value)} />
        </label>
        {preview && !form.categoryId && (
          <div className="hint">
            Auto category:{' '}
            <span className="chip" style={{ borderColor: preview.categoryColor, color: preview.categoryColor }}>
              {preview.categoryName}
            </span>{' '}
            {preview.source === 'RULE' ? (
              <span className="muted">matched rule “{preview.matchedPattern}”</span>
            ) : (
              <span className="muted">no rule matched - default category</span>
            )}
          </div>
        )}
        <label>
          Description
          <input placeholder="Optional" value={form.description} onChange={(e) => set('description', e.target.value)} />
        </label>
        <label>
          Category
          <select value={form.categoryId} onChange={(e) => set('categoryId', e.target.value)}>
            <option value="">Auto (rule-based)</option>
            {categories.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </select>
        </label>
        {form.categoryId && form.vendorName.trim() && (
          <label className="checkbox">
            <input type="checkbox" checked={form.remember} onChange={(e) => set('remember', e.target.checked)} />
            Always categorise “{form.vendorName.trim()}” this way (creates a vendor rule)
          </label>
        )}

        {errors.length > 0 && (
          <div className="alert error">
            {errors.map((m) => (
              <div key={m}>{m}</div>
            ))}
          </div>
        )}
        {saved && (
          <div className={saved.anomaly ? 'alert error' : 'alert success'}>
            Saved {money(saved.amount)} at {saved.vendorName} → <strong>{saved.categoryName}</strong>
            {saved.anomaly && (
              <div>
                <strong>⚠ Flagged as anomaly:</strong> {anomalyExplanation(saved)}
              </div>
            )}
          </div>
        )}
        <button className="primary" disabled={submitting}>
          {submitting ? 'Saving…' : 'Add expense'}
        </button>
      </form>
    </section>
  );
}
