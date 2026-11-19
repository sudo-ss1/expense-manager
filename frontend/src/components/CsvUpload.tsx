import { useRef, useState, type DragEvent } from 'react';
import { api, ApiError } from '../api';
import type { CsvImportResult } from '../types';

/**
 * Builds a template with ~6 months of realistic expenses ending today, so its dates never go stale or into the
 * future. It also demonstrates the accepted formats: DD/MM/YYYY dates, quoted "1,299.00" amounts, an explicit
 * category column (Local Kirana has no rule), and one deliberate anomaly (the ₹42,999 laptop).
 */
function buildTemplate(today = new Date()): string {
  const rows = ['date,amount,vendor,description,category'];
  const pad = (n: number) => String(n).padStart(2, '0');
  for (let back = 5; back >= 0; back--) {
    const y = new Date(today.getFullYear(), today.getMonth() - back, 1).getFullYear();
    const m = new Date(today.getFullYear(), today.getMonth() - back, 1).getMonth();
    const v = 5 - back; // small month-to-month variation in amounts
    const entries: [number, string, string, string, string][] = [
      [1, '22000.00', 'NoBroker Rent', 'House rent', ''],
      [3, '999.00', 'Airtel Postpaid', 'Mobile bill', ''],
      [5, `${420 + v * 15}.00`, 'Swiggy', 'Dinner', ''],
      [8, `${860 + v * 25}.00`, 'BigBasket', 'Weekly groceries', ''],
      [12, `${180 + v * 10}.00`, 'Uber', 'Office commute', ''],
      [15, `"${(1 + v * 0.1).toFixed(3).replace('.', ',')}.00"`, 'Amazon', '"Books, household"', ''],
      [18, '649.00', 'Netflix', 'Monthly plan', ''],
      [22, '60.00', 'Local Kirana', 'Milk', 'Groceries'],
      [26, `${1700 + v * 30}.00`, 'BESCOM Electricity', 'Electricity bill', ''],
    ];
    if (back === 1) entries.push([20, '42999.00', 'Croma', 'New laptop (anomaly)', '']);
    for (const [day, amount, vendor, description, category] of entries) {
      const date = new Date(y, m, day);
      if (date > today) continue;
      // Amazon rows use the DD/MM/YYYY format to show it's accepted; everything else is ISO.
      const dateText = vendor === 'Amazon' ? `${pad(day)}/${pad(m + 1)}/${y}` : `${y}-${pad(m + 1)}-${pad(day)}`;
      rows.push([dateText, amount, vendor, description, category].join(','));
    }
  }
  return rows.join('\n') + '\n';
}

export default function CsvUpload({ onImported }: { onImported: () => void }) {
  const [file, setFile] = useState<File | null>(null);
  const [result, setResult] = useState<CsvImportResult | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [dragging, setDragging] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);

  const choose = (f: File | null | undefined) => {
    setResult(null);
    setError(null);
    if (f && !f.name.toLowerCase().endsWith('.csv')) {
      setError('Please choose a .csv file');
      setFile(null);
      return;
    }
    setFile(f ?? null);
  };

  const onDrop = (e: DragEvent) => {
    e.preventDefault();
    setDragging(false);
    choose(e.dataTransfer.files?.[0]);
  };

  const upload = async () => {
    if (!file) return;
    setBusy(true);
    setError(null);
    try {
      const r = await api.uploadCsv(file);
      setResult(r);
      if (r.imported > 0) onImported();
    } catch (err) {
      setError((err as ApiError).message);
    } finally {
      setBusy(false);
    }
  };

  const downloadTemplate = () => {
    const url = URL.createObjectURL(new Blob([buildTemplate()], { type: 'text/csv' }));
    const a = document.createElement('a');
    a.href = url;
    a.download = 'expenses_template.csv';
    a.click();
    URL.revokeObjectURL(url);
  };

  return (
    <section className="card">
      <h3>Import from CSV</h3>
      <p className="muted small">
        Columns: <code>date, amount, vendor</code> (required), <code>description, category</code> (optional). Dates as
        YYYY-MM-DD or DD/MM/YYYY. Invalid rows are reported and skipped; rows already imported are not duplicated.{' '}
        <button type="button" className="link" onClick={downloadTemplate}>
          Download template
        </button>
      </p>

      <div
        className={`dropzone ${dragging ? 'dragging' : ''}`}
        onDragOver={(e) => {
          e.preventDefault();
          setDragging(true);
        }}
        onDragLeave={() => setDragging(false)}
        onDrop={onDrop}
        onClick={() => inputRef.current?.click()}
      >
        <input ref={inputRef} type="file" accept=".csv,text/csv" hidden onChange={(e) => choose(e.target.files?.[0])} />
        {file ? (
          <span>
            <strong>{file.name}</strong> <span className="muted">({(file.size / 1024).toFixed(1)} KB)</span>
          </span>
        ) : (
          <span className="muted">Drop a CSV here or click to choose</span>
        )}
      </div>

      <button className="primary" disabled={!file || busy} onClick={upload}>
        {busy ? 'Importing…' : 'Upload & import'}
      </button>

      {error && <div className="alert error">{error}</div>}

      {result && (
        <div className="import-result">
          <div className="stats small-stats">
            <div><span className="stat-value">{result.totalRows}</span><span className="muted small">rows</span></div>
            <div className="ok"><span className="stat-value">{result.imported}</span><span className="muted small">imported</span></div>
            <div><span className="stat-value">{result.duplicatesSkipped}</span><span className="muted small">duplicates skipped</span></div>
            <div className={result.failed ? 'bad' : ''}><span className="stat-value">{result.failed}</span><span className="muted small">failed</span></div>
            <div className={result.anomaliesAmongImported ? 'bad' : ''}><span className="stat-value">{result.anomaliesAmongImported}</span><span className="muted small">anomalies</span></div>
          </div>
          {result.errors.length > 0 && (
            <table className="table compact">
              <thead>
                <tr>
                  <th>Line</th>
                  <th>Problem</th>
                </tr>
              </thead>
              <tbody>
                {result.errors.map((e) => (
                  <tr key={e.lineNumber}>
                    <td>{e.lineNumber}</td>
                    <td>{e.message}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}
    </section>
  );
}
