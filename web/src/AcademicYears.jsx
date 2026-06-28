import React, { useState, useEffect } from 'react';
import api from './api';
import { Calendar, Plus, ToggleLeft, ToggleRight, Check } from 'lucide-react';

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

  useEffect(() => {
    fetchYears();
  }, []);

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
      )}
    </div>
  );
}
