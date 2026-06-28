import React, { useState, useEffect } from 'react';
import api from './api';
import { DollarSign, ArrowUpRight, ArrowDownRight, Award, TrendingUp } from 'lucide-react';

const monthNames = [
  "January", "February", "March", "April", "May", "June",
  "July", "August", "September", "October", "November", "December"
];

export default function Reports() {
  const currentYear = new Date().getFullYear();
  const currentMonth = new Date().getMonth() + 1;

  const [month, setMonth] = useState(currentMonth);
  const [year, setYear] = useState(currentYear);
  const [report, setReport] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const years = Array.from({ length: 5 }, (_, i) => currentYear - i);

  useEffect(() => {
    fetchReport();
  }, [month, year]);

  const fetchReport = async () => {
    setLoading(true);
    setError('');
    try {
      const res = await api.get(`/api/reports/monthly`, {
        params: { month, year }
      });
      setReport(res.data);
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to fetch financial report.');
      setReport(null);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <div className="page-header">
        <div>
          <h1 className="page-title">Financial Reports</h1>
          <p className="page-subtitle">Analyze cash flows, salaries, expenses, and book/uniform profit margins</p>
        </div>
        <div style={{ display: 'flex', gap: '12px' }}>
          <select className="input-field select-field" style={{ width: '150px' }} value={month} onChange={(e) => setMonth(parseInt(e.target.value))}>
            {monthNames.map((name, idx) => (
              <option key={idx} value={idx + 1}>{name}</option>
            ))}
          </select>
          <select className="input-field select-field" style={{ width: '110px' }} value={year} onChange={(e) => setYear(parseInt(e.target.value))}>
            {years.map((y) => (
              <option key={y} value={y}>{y}</option>
            ))}
          </select>
        </div>
      </div>

      {loading && <p style={{ color: 'var(--text-secondary)' }}>Compiling monthly financials...</p>}
      {error && (
        <div style={{ background: 'var(--error-bg)', color: '#fca5a5', padding: '16px', borderRadius: '12px', marginBottom: '24px', border: '1px solid rgba(239, 68, 68, 0.3)' }}>
          {error}
        </div>
      )}

      {!loading && report && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '28px' }}>
          
          {/* Main Financials Grid */}
          <div className="grid-3">
            <div className="glass-card" style={{ display: 'flex', alignItems: 'center', gap: '20px' }}>
              <div style={{ background: 'rgba(16, 185, 129, 0.15)', padding: '16px', borderRadius: '12px', color: 'var(--success)' }}>
                <DollarSign size={28} />
              </div>
              <div>
                <p style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', fontWeight: 600, textTransform: 'uppercase' }}>Cash Collected</p>
                <h2 style={{ fontFamily: 'Outfit, sans-serif', fontSize: '1.75rem', fontWeight: 800, marginTop: '4px' }}>
                  ₹{(report.cashCollected || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                </h2>
              </div>
            </div>

            <div className="glass-card" style={{ display: 'flex', alignItems: 'center', gap: '20px' }}>
              <div style={{ background: 'rgba(239, 68, 68, 0.15)', padding: '16px', borderRadius: '12px', color: 'var(--error)' }}>
                <ArrowDownRight size={28} />
              </div>
              <div>
                <p style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', fontWeight: 600, textTransform: 'uppercase' }}>Expenses & Salaries</p>
                <h2 style={{ fontFamily: 'Outfit, sans-serif', fontSize: '1.75rem', fontWeight: 800, marginTop: '4px' }}>
                  ₹{((report.totalExpenses || 0) + (report.totalSalaries || 0)).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                </h2>
              </div>
            </div>

            <div className="glass-card" style={{ display: 'flex', alignItems: 'center', gap: '20px', background: report.netForMonth >= 0 ? 'rgba(16, 185, 129, 0.05)' : 'rgba(239, 68, 68, 0.05)' }}>
              <div style={{ background: report.netForMonth >= 0 ? 'rgba(16, 185, 129, 0.15)' : 'rgba(239, 68, 68, 0.15)', padding: '16px', borderRadius: '12px', color: report.netForMonth >= 0 ? 'var(--success)' : 'var(--error)' }}>
                <TrendingUp size={28} />
              </div>
              <div>
                <p style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', fontWeight: 600, textTransform: 'uppercase' }}>Net Cash Flow</p>
                <h2 style={{ fontFamily: 'Outfit, sans-serif', fontSize: '1.75rem', fontWeight: 800, marginTop: '4px', color: report.netForMonth >= 0 ? 'var(--success)' : 'var(--error)' }}>
                  ₹{(report.netForMonth || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                </h2>
              </div>
            </div>
          </div>

          {/* Book & Uniform Margin Card */}
          {(report.itemSalesRevenue > 0 || report.itemCostOfSales > 0) && (
            <div className="glass-card" style={{ borderLeft: '4px solid var(--accent-primary)', background: 'linear-gradient(90deg, rgba(99, 102, 241, 0.05) 0%, transparent 100%)' }}>
              <h3 style={{ fontFamily: 'Outfit, sans-serif', fontSize: '1.2rem', marginBottom: '20px', display: 'flex', alignItems: 'center', gap: '10px' }}>
                <Award size={20} style={{ color: 'var(--accent-primary)' }} />
                Books & Uniforms Sales Summary
              </h3>
              
              <div className="grid-3" style={{ background: 'rgba(255, 255, 255, 0.02)', padding: '20px', borderRadius: '12px', border: '1px solid var(--border-color)' }}>
                <div>
                  <p style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', fontWeight: 600, textTransform: 'uppercase' }}>Sales Revenue</p>
                  <h4 style={{ fontSize: '1.4rem', fontWeight: 700, color: 'var(--success)', marginTop: '4px' }}>
                    ₹{(report.itemSalesRevenue || 0).toFixed(2)}
                  </h4>
                </div>
                <div>
                  <p style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', fontWeight: 600, textTransform: 'uppercase' }}>Cost of Goods Sold (COGS)</p>
                  <h4 style={{ fontSize: '1.4rem', fontWeight: 700, color: 'var(--error)', marginTop: '4px' }}>
                    ₹{(report.itemCostOfSales || 0).toFixed(2)}
                  </h4>
                </div>
                <div>
                  <p style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', fontWeight: 600, textTransform: 'uppercase' }}>Gross Profit Margin</p>
                  <h4 style={{ fontSize: '1.4rem', fontWeight: 700, color: 'var(--text-primary)', marginTop: '4px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                    ₹{(report.itemProfit || 0).toFixed(2)}
                    {report.itemCostOfSales > 0 && (
                      <span style={{ fontSize: '0.9rem', color: 'var(--success)' }}>
                        ({((report.itemProfit / report.itemCostOfSales) * 100).toFixed(0)}%)
                      </span>
                    )}
                  </h4>
                </div>
              </div>
            </div>
          )}

          {/* Breakdown Lists */}
          <div className="grid-2">
            
            {/* Collected by Mode */}
            <div className="glass-card">
              <h3 style={{ fontFamily: 'Outfit, sans-serif', fontSize: '1.1rem', marginBottom: '16px', paddingBottom: '8px', borderBottom: '1px solid var(--border-color)' }}>
                Collections by Payment Mode
              </h3>
              {report.collectedByMode && Object.keys(report.collectedByMode).length > 0 ? (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                  {Object.entries(report.collectedByMode).map(([mode, amt]) => (
                    <div key={mode} style={{ display: 'flex', justifyContent: 'space-between', padding: '4px 0' }}>
                      <span style={{ fontWeight: 600, color: 'var(--text-secondary)', textTransform: 'uppercase', fontSize: '0.9rem' }}>{mode}</span>
                      <span style={{ fontWeight: 700 }}>₹{amt.toLocaleString('en-IN', { minimumFractionDigits: 2 })}</span>
                    </div>
                  ))}
                </div>
              ) : (
                <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem', fontStyle: 'italic' }}>No payments collected this month.</p>
              )}
            </div>

            {/* Expenses by Category */}
            <div className="glass-card">
              <h3 style={{ fontFamily: 'Outfit, sans-serif', fontSize: '1.1rem', marginBottom: '16px', paddingBottom: '8px', borderBottom: '1px solid var(--border-color)' }}>
                Expenses by Category
              </h3>
              {report.expensesByCategory && Object.keys(report.expensesByCategory).length > 0 ? (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                  {Object.entries(report.expensesByCategory).map(([cat, amt]) => (
                    <div key={cat} style={{ display: 'flex', justifyContent: 'space-between', padding: '4px 0' }}>
                      <span style={{ fontWeight: 600, color: 'var(--text-secondary)', fontSize: '0.9rem' }}>{cat}</span>
                      <span style={{ fontWeight: 700 }}>₹{amt.toLocaleString('en-IN', { minimumFractionDigits: 2 })}</span>
                    </div>
                  ))}
                </div>
              ) : (
                <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem', fontStyle: 'italic' }}>No operational expenses logged this month.</p>
              )}
            </div>

          </div>

        </div>
      )}
    </div>
  );
}
