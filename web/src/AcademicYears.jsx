import React, { useState, useEffect } from 'react';
import api from './api';
import { Calendar, Plus, ToggleLeft, ToggleRight, Check, RefreshCw } from 'lucide-react';

export default function AcademicYears() {
  const [years, setYears] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  // Form states
  const [label, setLabel] = useState('');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [isCurrent, setIsCurrent] = useState(false);
  
  const [isOpenForm, setIsOpenForm] = useState(false);

  // Rollover states
  const [fromYearId, setFromYearId] = useState('');
  const [toYearId, setToYearId] = useState('');
  const [leftoverBooks, setLeftoverBooks] = useState([]);
  const [leftoverLoading, setLeftoverLoading] = useState(false);
  const [rolloverLoading, setRolloverLoading] = useState(false);

  useEffect(() => {
    fetchYears();
  }, []);

  useEffect(() => {
    if (fromYearId) {
      fetchLeftoverBooks();
    } else {
      setLeftoverBooks([]);
    }
  }, [fromYearId]);

  const fetchYears = async () => {
    setLoading(true);
    try {
      const res = await api.get('/api/academic-years');
      setYears(res.data);
    } catch (err) {
      setError('Failed to fetch academic years.');
    } finally {
      setLoading(false);
    }
  };

  const fetchLeftoverBooks = async () => {
    setLeftoverLoading(true);
    try {
      // 1. Fetch all classes of the source academic year
      const classesRes = await api.get(`/api/classes`, { params: { academicYearId: fromYearId } });
      const oldClassIds = classesRes.data.map(c => c.id);

      // 2. Fetch all inventory categories
      const categoriesRes = await api.get('/api/item-categories');
      const bookCategory = categoriesRes.data.find(c => c.type === 'BOOK');
      
      if (bookCategory && bookCategory.variants) {
        // Filter variants belonging to old classes and having stock > 0
        const leftovers = bookCategory.variants.filter(v => 
          v.classId && oldClassIds.includes(v.classId) && v.stockQuantity > 0
        );
        setLeftoverBooks(leftovers);
      } else {
        setLeftoverBooks([]);
      }
    } catch (err) {
      console.error('Failed to fetch leftover book stock', err);
    } finally {
      setLeftoverLoading(false);
    }
  };

  const handleCreateYear = async (e) => {
    e.preventDefault();
    if (!label || !startDate || !endDate) return;

    try {
      await api.post('/api/academic-years', {
        label,
        startDate: new Date(startDate).toISOString(),
        endDate: new Date(endDate).toISOString(),
        isCurrent: !!isCurrent
      });
      setIsOpenForm(false);
      resetForm();
      fetchYears();
    } catch (err) {
      alert(err.response?.data?.error || 'Failed to create academic year.');
    }
  };

  const handleRollover = async () => {
    if (!fromYearId || !toYearId) return;
    if (fromYearId === toYearId) {
      alert('Source year and target year must be different.');
      return;
    }

    const fromYear = years.find(y => y.id === fromYearId);
    const toYear = years.find(y => y.id === toYearId);

    if (!window.confirm(`Are you sure you want to perform inventory metadata rollover from ${fromYear.label} to ${toYear.label}? This will copy variant configurations and update active year labels.`)) {
      return;
    }

    setRolloverLoading(true);
    try {
      const response = await api.post('/api/academic/rollover', {
        fromYearId,
        toYearId
      });
      alert(`Success: Rollover completed! Processed book variants for ${response.data.processed.length} classes.`);
      setFromYearId('');
      setToYearId('');
      fetchYears();
    } catch (err) {
      alert(err.response?.data?.error || 'Failed to complete rollover.');
    } finally {
      setRolloverLoading(false);
    }
  };

  const resetForm = () => {
    setLabel('');
    setStartDate('');
    setEndDate('');
    setIsCurrent(false);
  };

  return (
    <div>
      <div className="page-header">
        <div>
          <h1 className="page-title">Academic Years</h1>
          <p className="page-subtitle">Set up school terms, start/end boundaries, and configure the active term</p>
        </div>
        {!isOpenForm && (
          <button className="btn btn-primary" onClick={() => setIsOpenForm(true)}>
            <Plus size={18} />
            Setup New Year
          </button>
        )}
      </div>

      {isOpenForm && (
        <div className="glass-card" style={{ marginBottom: '32px', maxWidth: '600px', animation: 'fadeIn 0.3s ease-out' }}>
          <h3 style={{ fontFamily: 'Outfit, sans-serif', fontSize: '1.2rem', marginBottom: '20px' }}>Setup New Academic Year</h3>
          <form onSubmit={handleCreateYear}>
            <div className="input-group">
              <label className="input-label">Label / Name</label>
              <input
                type="text"
                className="input-field"
                placeholder="e.g. 2027-28"
                value={label}
                onChange={(e) => setLabel(e.target.value)}
                required
              />
            </div>
            
            <div className="grid-2">
              <div className="input-group">
                <label className="input-label">Start Date</label>
                <input
                  type="date"
                  className="input-field"
                  value={startDate}
                  onChange={(e) => setStartDate(e.target.value)}
                  required
                />
              </div>
              <div className="input-group">
                <label className="input-label">End Date</label>
                <input
                  type="date"
                  className="input-field"
                  value={endDate}
                  onChange={(e) => setEndDate(e.target.value)}
                  required
                />
              </div>
            </div>

            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', margin: '16px 0' }}>
              <input
                type="checkbox"
                id="isCurrentYear"
                checked={isCurrent}
                onChange={(e) => setIsCurrent(e.target.checked)}
                style={{ width: '18px', height: '18px', cursor: 'pointer' }}
              />
              <label htmlFor="isCurrentYear" style={{ cursor: 'pointer', fontSize: '0.95rem' }}>Mark as Current Academic Year</label>
            </div>

            <div style={{ display: 'flex', gap: '12px', justifyContent: 'flex-end', marginTop: '20px' }}>
              <button type="button" className="btn btn-secondary" onClick={() => setIsOpenForm(false)}>Cancel</button>
              <button type="submit" className="btn btn-primary">Save Academic Year</button>
            </div>
          </form>
        </div>
      )}

      {loading && <p style={{ color: 'var(--text-secondary)' }}>Loading terms...</p>}
      {error && <p style={{ color: 'var(--error)' }}>{error}</p>}

      {!loading && !error && (
        <>
          <div className="table-container">
            <table className="custom-table">
              <thead>
                <tr>
                  <th>Year Label</th>
                  <th>Start Date</th>
                  <th>End Date</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {years.map((year) => (
                  <tr key={year.id}>
                    <td style={{ fontWeight: 600, fontSize: '1.05rem' }}>{year.label}</td>
                    <td>{new Date(year.startDate).toLocaleDateString()}</td>
                    <td>{new Date(year.endDate).toLocaleDateString()}</td>
                    <td>
                      {year.isCurrent ? (
                        <span className="badge badge-success" style={{ display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
                          <Check size={12} /> Active / Current
                        </span>
                      ) : (
                        <span className="badge badge-secondary" style={{ background: 'rgba(255,255,255,0.05)', border: '1px solid var(--border-color)', color: 'var(--text-secondary)' }}>
                          Previous Term
                        </span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Rollover Section */}
          <div style={{ marginTop: '48px', borderTop: '1px solid var(--border-color)', paddingTop: '32px' }}>
            <h2 style={{ fontFamily: 'Outfit, sans-serif', fontSize: '1.5rem', fontWeight: 700, marginBottom: '8px', display: 'flex', alignItems: 'center', gap: '10px' }}>
              <RefreshCw size={24} style={{ color: 'var(--accent-secondary)' }} />
              Academic Year Rollover
            </h2>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', marginBottom: '24px' }}>
              Set up the next year's book metadata by copying pricing and setting up active year labels, keeping previous year stock selectable.
            </p>

            <div className="glass-card" style={{ maxWidth: '750px' }}>
              <div className="grid-2" style={{ marginBottom: '24px' }}>
                <div className="input-group">
                  <label className="input-label">Source Year (Previous)</label>
                  <select className="input-field select-field" value={fromYearId} onChange={(e) => setFromYearId(e.target.value)}>
                    <option value="">-- Select Source Year --</option>
                    {years.map(y => (
                      <option key={y.id} value={y.id}>{y.label}</option>
                    ))}
                  </select>
                </div>
                <div className="input-group">
                  <label className="input-label">Target Year (Next)</label>
                  <select className="input-field select-field" value={toYearId} onChange={(e) => setToYearId(e.target.value)}>
                    <option value="">-- Select Target Year --</option>
                    {years.map(y => (
                      <option key={y.id} value={y.id}>{y.label}</option>
                    ))}
                  </select>
                </div>
              </div>

              {/* Display leftover books */}
              {fromYearId && (
                <div style={{ marginBottom: '24px', background: 'rgba(255,255,255,0.02)', padding: '16px', borderRadius: '8px', border: '1px solid var(--border-color)' }}>
                  <h4 style={{ fontSize: '0.95rem', fontWeight: 700, color: 'var(--text-primary)', marginBottom: '12px' }}>
                    Leftover Book Stock from Source Year:
                  </h4>
                  {leftoverLoading ? (
                    <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem' }}>Analyzing stock levels...</p>
                  ) : leftoverBooks.length > 0 ? (
                    <ul style={{ listStyleType: 'none', display: 'flex', flexDirection: 'column', gap: '8px' }}>
                      {leftoverBooks.map((b, idx) => (
                        <li key={idx} style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.9rem', color: 'var(--text-secondary)' }}>
                          <span>{b.label}</span>
                          <span style={{ fontWeight: 700, color: 'var(--warning)' }}>{b.stockQuantity} units remaining</span>
                        </li>
                      ))}
                    </ul>
                  ) : (
                    <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem', fontStyle: 'italic' }}>No leftover book stock found in this source year.</p>
                  )}
                </div>
              )}

              <button 
                type="button"
                className="btn btn-primary" 
                style={{ width: '100%', gap: '8px', background: 'linear-gradient(135deg, #a855f7 0%, #6366f1 100%)', boxShadow: '0 4px 14px 0 rgba(168, 85, 247, 0.4)' }}
                disabled={!fromYearId || !toYearId || fromYearId === toYearId || rolloverLoading}
                onClick={handleRollover}
              >
                {rolloverLoading ? 'Processing Rollover...' : 'Run Automated Rollover'}
              </button>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
