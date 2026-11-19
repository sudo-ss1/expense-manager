import { useCallback, useEffect, useState } from 'react';
import { api } from './api';
import type { Category } from './types';
import Dashboard from './components/Dashboard';
import ExpenseList from './components/ExpenseList';
import ExpenseForm from './components/ExpenseForm';
import CsvUpload from './components/CsvUpload';
import RulesPanel from './components/RulesPanel';

type Tab = 'dashboard' | 'expenses' | 'add' | 'rules';

const TABS: { id: Tab; label: string }[] = [
  { id: 'dashboard', label: 'Dashboard' },
  { id: 'expenses', label: 'Expenses' },
  { id: 'add', label: 'Add / Import' },
  { id: 'rules', label: 'Category rules' },
];

export default function App() {
  const [tab, setTab] = useState<Tab>('dashboard');
  const [categories, setCategories] = useState<Category[]>([]);
  // Bumped after every write so views refetch - simple and explicit instead of a cache library.
  const [version, setVersion] = useState(0);
  const changed = useCallback(() => setVersion((v) => v + 1), []);

  useEffect(() => {
    api.categories().then(setCategories).catch(() => setCategories([]));
  }, [version]);

  return (
    <div className="app">
      <header className="topbar">
        <div className="brand">
          <span className="logo">₹</span>
          <div>
            <h1>Mini Expense Manager</h1>
            <p>Rule-based categorisation · anomaly detection</p>
          </div>
        </div>
        <nav>
          {TABS.map((t) => (
            <button key={t.id} className={tab === t.id ? 'tab active' : 'tab'} onClick={() => setTab(t.id)}>
              {t.label}
            </button>
          ))}
        </nav>
      </header>

      <main>
        {tab === 'dashboard' && <Dashboard version={version} />}
        {tab === 'expenses' && <ExpenseList categories={categories} version={version} onChanged={changed} />}
        {tab === 'add' && (
          <div className="grid two">
            <ExpenseForm categories={categories} onCreated={changed} />
            <CsvUpload onImported={changed} />
          </div>
        )}
        {tab === 'rules' && <RulesPanel categories={categories} version={version} onChanged={changed} />}
      </main>
    </div>
  );
}
