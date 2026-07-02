import React, { useState, useEffect } from 'react';
import api from './api';
import { GraduationCap, CheckSquare, CreditCard, Package, TrendingUp, AlertCircle, ArrowUpRight } from 'lucide-react';

export default function Overview({ currentUser, selectedYearId }) {
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (selectedYearId) {
      fetchStats();
    }
  }, [selectedYearId]);

  const fetchStats = async () => {
    setLoading(true);
    setError('');
    try {
      const res = await api.get(`/api/dashboard/stats`, {
        params: { academicYearId: selectedYearId }
      });
      setStats(res.data);
    } catch (err) {
      console.error(err);
      setError('Failed to fetch dashboard stats');
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return <p style={{ color: 'var(--text-secondary)' }}>Loading overview statistics...</p>;
  }

  if (error) {
    return (
      <div style={{ background: 'var(--error-bg)', color: '#fca5a5', padding: '16px', borderRadius: '12px', display: 'flex', alignItems: 'center', gap: '12px' }}>
        <AlertCircle size={20} />
        <span>{error}</span>
      </div>
    );
  }

  if (!stats) return null;

  const collectionPercent = stats.totalBilled > 0 
    ? Math.round((stats.totalCollected / stats.totalBilled) * 100) 
    : 0;

  // Donut chart logic for inventory
  const totalStock = Object.values(stats.stockByCategoryType).reduce((a, b) => a + b, 0);
  const categoriesList = [
    { label: 'Books', val: stats.stockByCategoryType.BOOK, color: '#6366f1' },
    { label: 'Summer Uniforms', val: stats.stockByCategoryType.UNIFORM_SUMMER, color: '#10b981' },
    { label: 'Winter Uniforms', val: stats.stockByCategoryType.UNIFORM_WINTER, color: '#f59e0b' },
    { label: 'Others', val: stats.stockByCategoryType.OTHER, color: '#ec4899' },
  ];

  // Circular ring calculations for fee collections
  const radius = 40;
  const circumference = 2 * Math.PI * radius;
  const strokeDashoffset = circumference - (collectionPercent / 100) * circumference;

  return (
    <div style={{ animation: 'fadeIn 0.3s ease-out' }}>
      <div className="page-header" style={{ marginBottom: '24px' }}>
        <div>
          <h1 className="page-title">Welcome Back, {currentUser.username}!</h1>
          <p className="page-subtitle">Here is a snapshot of your school's current term diagnostics</p>
        </div>
      </div>

      {/* Main KPI Card Grid */}
      <div className="grid-3" style={{ marginBottom: '32px' }}>
        <div className="glass-card" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '24px' }}>
          <div>
            <span style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', fontWeight: 600, textTransform: 'uppercase' }}>Active Students</span>
            <h2 style={{ fontSize: '2.25rem', fontWeight: 800, margin: '8px 0 4px 0', fontFamily: 'Outfit, sans-serif' }}>{stats.totalStudents}</h2>
            <span style={{ fontSize: '0.75rem', color: '#10b981', display: 'flex', alignItems: 'center', gap: '4px' }}>
              <ArrowUpRight size={14} /> Registered in Active Term
            </span>
          </div>
          <div style={{ background: 'rgba(99,102,241,0.15)', padding: '16px', borderRadius: '12px', color: '#818cf8' }}>
            <GraduationCap size={32} />
          </div>
        </div>

        <div className="glass-card" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '24px' }}>
          <div>
            <span style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', fontWeight: 600, textTransform: 'uppercase' }}>Attendance Today</span>
            <h2 style={{ fontSize: '2.25rem', fontWeight: 800, margin: '8px 0 4px 0', fontFamily: 'Outfit, sans-serif' }}>{stats.presentToday}</h2>
            <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
              Out of {stats.totalStudents} active students
            </span>
          </div>
          <div style={{ background: 'rgba(16,185,129,0.15)', padding: '16px', borderRadius: '12px', color: '#10b981' }}>
            <CheckSquare size={32} />
          </div>
        </div>

        <div className="glass-card" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '24px' }}>
          <div>
            <span style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', fontWeight: 600, textTransform: 'uppercase' }}>Fee Collection Rate</span>
            <h2 style={{ fontSize: '2.25rem', fontWeight: 800, margin: '8px 0 4px 0', fontFamily: 'Outfit, sans-serif' }}>{collectionPercent}%</h2>
            <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
              Collected: ₹{stats.totalCollected.toLocaleString()}
            </span>
          </div>
          <div style={{ position: 'relative', width: '90px', height: '90px', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <svg width="90" height="90">
              <circle cx="45" cy="45" r={radius} stroke="rgba(255,255,255,0.05)" strokeWidth="8" fill="transparent" />
              <circle cx="45" cy="45" r={radius} stroke="#10b981" strokeWidth="8" fill="transparent"
                strokeDasharray={circumference}
                strokeDashoffset={strokeDashoffset}
                strokeLinecap="round"
                transform="rotate(-90 45 45)"
              />
            </svg>
            <span style={{ position: 'absolute', fontSize: '0.85rem', fontWeight: 700 }}>{collectionPercent}%</span>
          </div>
        </div>
      </div>

      {/* Charts Section */}
      <div className="grid-2" style={{ gap: '32px', marginBottom: '32px' }}>
        {/* Fee Billing vs collection Bar Chart */}
        <div className="glass-card" style={{ padding: '24px' }}>
          <h3 style={{ fontSize: '1.1rem', fontWeight: 700, fontFamily: 'Outfit, sans-serif', marginBottom: '8px' }}>Collections & Dues Analysis</h3>
          <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginBottom: '24px' }}>Comparison of school ledger outstanding balances against payments received</p>

          <div style={{ display: 'flex', gap: '32px', alignItems: 'center' }}>
            {/* SVG Bar representation */}
            <svg width="220" height="180" style={{ overflow: 'visible' }}>
              {/* Grid lines */}
              <line x1="0" y1="30" x2="220" y2="30" stroke="rgba(255,255,255,0.05)" />
              <line x1="0" y1="80" x2="220" y2="80" stroke="rgba(255,255,255,0.05)" />
              <line x1="0" y1="130" x2="220" y2="130" stroke="rgba(255,255,255,0.05)" />
              <line x1="0" y1="150" x2="220" y2="150" stroke="var(--border-color)" strokeWidth="2" />

              {/* Bar 1: Total Billed */}
              <rect x="30" y={150 - 110} width="45" height="110" fill="rgba(99,102,241,0.2)" stroke="#818cf8" strokeWidth="2" rx="4" />
              <text x="52" y={150 - 120} fill="#818cf8" textAnchor="middle" style={{ fontSize: '0.75rem', fontWeight: 700 }}>Billed</text>

              {/* Bar 2: Total Collected */}
              <rect x="95" y={150 - (110 * (stats.totalCollected / (stats.totalBilled || 1)))} width="45" height={110 * (stats.totalCollected / (stats.totalBilled || 1))} fill="rgba(16,185,129,0.2)" stroke="#10b981" strokeWidth="2" rx="4" />
              <text x="117" y={150 - (110 * (stats.totalCollected / (stats.totalBilled || 1))) - 10} fill="#10b981" textAnchor="middle" style={{ fontSize: '0.75rem', fontWeight: 700 }}>Paid</text>

              {/* Bar 3: Total Outstanding */}
              <rect x="160" y={150 - (110 * (stats.totalOutstanding / (stats.totalBilled || 1)))} width="45" height={110 * (stats.totalOutstanding / (stats.totalBilled || 1))} fill="rgba(239,68,68,0.2)" stroke="#ef4444" strokeWidth="2" rx="4" />
              <text x="182" y={150 - (110 * (stats.totalOutstanding / (stats.totalBilled || 1))) - 10} fill="#ef4444" textAnchor="middle" style={{ fontSize: '0.75rem', fontWeight: 700 }}>Dues</text>
            </svg>

            {/* Legend/Info details */}
            <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', flex: 1 }}>
              <div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '0.85rem' }}>
                  <div style={{ width: '12px', height: '12px', borderRadius: '3px', background: '#818cf8' }}></div>
                  <span style={{ color: 'var(--text-secondary)' }}>Total Billed Dues</span>
                </div>
                <strong style={{ fontSize: '1rem', marginLeft: '20px' }}>₹{stats.totalBilled.toLocaleString()}</strong>
              </div>

              <div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '0.85rem' }}>
                  <div style={{ width: '12px', height: '12px', borderRadius: '3px', background: '#10b981' }}></div>
                  <span style={{ color: 'var(--text-secondary)' }}>Collected Payments</span>
                </div>
                <strong style={{ fontSize: '1rem', marginLeft: '20px', color: '#10b981' }}>₹{stats.totalCollected.toLocaleString()}</strong>
              </div>

              <div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '0.85rem' }}>
                  <div style={{ width: '12px', height: '12px', borderRadius: '3px', background: '#ef4444' }}></div>
                  <span style={{ color: 'var(--text-secondary)' }}>Outstanding Balance</span>
                </div>
                <strong style={{ fontSize: '1rem', marginLeft: '20px', color: '#ef4444' }}>₹{stats.totalOutstanding.toLocaleString()}</strong>
              </div>
            </div>
          </div>
        </div>

        {/* Inventory Stock Split Donut Chart */}
        <div className="glass-card" style={{ padding: '24px' }}>
          <h3 style={{ fontSize: '1.1rem', fontWeight: 700, fontFamily: 'Outfit, sans-serif', marginBottom: '8px' }}>Inventory Stock Distribution</h3>
          <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginBottom: '24px' }}>Visual count partition of uniforms, notebooks, and miscellaneous item stocks</p>

          <div style={{ display: 'flex', gap: '32px', alignItems: 'center' }}>
            {/* SVG circle segments */}
            <div style={{ position: 'relative', width: '120px', height: '120px' }}>
              <svg width="120" height="120" style={{ transform: 'rotate(-90deg)' }}>
                {totalStock === 0 ? (
                  <circle cx="60" cy="60" r="45" stroke="rgba(255,255,255,0.05)" strokeWidth="16" fill="transparent" />
                ) : (
                  (() => {
                    let accumulatedPercent = 0;
                    return categoriesList.map((cat, idx) => {
                      const share = cat.val / totalStock;
                      if (share === 0) return null;
                      const segmentLength = share * (2 * Math.PI * 45);
                      const dashoffset = (2 * Math.PI * 45) - segmentLength;
                      const strokeDasharray = `${segmentLength} ${(2 * Math.PI * 45) - segmentLength}`;
                      const rotation = accumulatedPercent * 360;
                      accumulatedPercent += share;
                      return (
                        <circle
                          key={idx}
                          cx="60"
                          cy="60"
                          r="45"
                          stroke={cat.color}
                          strokeWidth="16"
                          fill="transparent"
                          strokeDasharray={strokeDasharray}
                          transform={`rotate(${rotation} 60 60)`}
                        />
                      );
                    });
                  })()
                )}
              </svg>
              <div style={{ position: 'absolute', top: 0, left: 0, width: '120px', height: '120px', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center' }}>
                <span style={{ fontSize: '1.1rem', fontWeight: 800 }}>{totalStock}</span>
                <span style={{ fontSize: '0.65rem', color: 'var(--text-muted)', textTransform: 'uppercase' }}>Units</span>
              </div>
            </div>

            {/* Legend partition */}
            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', flex: 1 }}>
              {categoriesList.map((cat, idx) => {
                const percent = totalStock > 0 ? Math.round((cat.val / totalStock) * 100) : 0;
                return (
                  <div key={idx} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: '0.85rem' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                      <div style={{ width: '10px', height: '10px', borderRadius: '50%', background: cat.color }}></div>
                      <span style={{ color: 'var(--text-secondary)' }}>{cat.label}</span>
                    </div>
                    <strong style={{ color: 'var(--text-primary)' }}>{cat.val} ({percent}%)</strong>
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
