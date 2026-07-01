import React, { useState, useEffect } from 'react';
import api from './api';
import { Settings, Save, AlertCircle, Check } from 'lucide-react';

export default function InvoiceSettings({ currentUser }) {
  const [schoolName, setSchoolName] = useState('');
  const [address, setAddress] = useState('');
  const [phone, setPhone] = useState('');
  const [email, setEmail] = useState('');
  const [footerNote, setFooterNote] = useState('');
  const [thermalWidth, setThermalWidth] = useState(576);
  const [marginSize, setMarginSize] = useState(20);
  const [headerFontSize, setHeaderFontSize] = useState(28);
  const [bodyFontSize, setBodyFontSize] = useState(14);

  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);

  useEffect(() => {
    fetchSettings();
  }, []);

  const fetchSettings = async () => {
    setLoading(true);
    try {
      const res = await api.get('/api/invoice-settings');
      const data = res.data;
      setSchoolName(data.schoolName || '');
      setAddress(data.address || '');
      setPhone(data.phone || '');
      setEmail(data.email || '');
      setFooterNote(data.footerNote || '');
      setThermalWidth(data.thermalWidth || 576);
      setMarginSize(data.marginSize || 20);
      setHeaderFontSize(data.headerFontSize || 28);
      setBodyFontSize(data.bodyFontSize || 14);
    } catch (err) {
      setError('Failed to load invoice configurations.');
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);
    setError('');
    setSuccess(false);

    try {
      const res = await api.patch('/api/invoice-settings', {
        schoolName,
        address: address || null,
        phone: phone || null,
        email: email || null,
        footerNote,
        thermalWidth: parseInt(thermalWidth) || 576,
        marginSize: parseInt(marginSize) || 20,
        headerFontSize: parseInt(headerFontSize) || 28,
        bodyFontSize: parseInt(bodyFontSize) || 14,
      });
      setSuccess(true);
      setTimeout(() => setSuccess(false), 3000);
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to save configurations.');
    } finally {
      setSaving(false);
    }
  };

  if (currentUser.role !== 'MASTER') {
    return (
      <div className="glass-card" style={{ padding: '32px', textAlign: 'center' }}>
        <AlertCircle size={48} style={{ color: 'var(--error)', marginBottom: '16px' }} />
        <h3 style={{ fontSize: '1.2rem', fontWeight: 600 }}>Access Denied</h3>
        <p style={{ color: 'var(--text-secondary)', marginTop: '8px' }}>
          Only the Master administrator can access or modify school receipt configurations.
        </p>
      </div>
    );
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <h1 className="page-title">Invoice & Receipt Configurator</h1>
          <p className="page-subtitle">Configure invoice profile variables, headers, and thermal/PDF layouts</p>
        </div>
      </div>

      {loading ? (
        <p style={{ color: 'var(--text-secondary)' }}>Loading settings...</p>
      ) : (
        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '32px', maxWidth: '800px' }}>
          {/* Section 1: School Profile */}
          <div className="glass-card">
            <h3 style={{ fontFamily: 'Outfit, sans-serif', fontSize: '1.25rem', fontWeight: 600, marginBottom: '20px', display: 'flex', alignItems: 'center', gap: '8px' }}>
              <Settings size={20} style={{ color: 'var(--accent-secondary)' }} />
              School Details (Receipt Header)
            </h3>
            
            <div className="input-group">
              <label className="input-label">School Name (Prints in UPPERCASE)</label>
              <input
                type="text"
                className="input-field"
                value={schoolName}
                onChange={(e) => setSchoolName(e.target.value)}
                required
              />
            </div>

            <div className="input-group">
              <label className="input-label">Address</label>
              <input
                type="text"
                className="input-field"
                value={address}
                onChange={(e) => setAddress(e.target.value)}
                placeholder="e.g. 12 Main St, New Delhi, India"
              />
            </div>

            <div className="grid-2">
              <div className="input-group">
                <label className="input-label">Phone Contact</label>
                <input
                  type="text"
                  className="input-field"
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  placeholder="e.g. +91 99999 99999"
                />
              </div>
              <div className="input-group">
                <label className="input-label">Email Address</label>
                <input
                  type="email"
                  className="input-field"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="e.g. contact@school.com"
                />
              </div>
            </div>
          </div>

          {/* Section 2: Layout Options */}
          <div className="glass-card">
            <h3 style={{ fontFamily: 'Outfit, sans-serif', fontSize: '1.25rem', fontWeight: 600, marginBottom: '20px', display: 'flex', alignItems: 'center', gap: '8px' }}>
              <Settings size={20} style={{ color: 'var(--accent-secondary)' }} />
              Receipt Format & Layout
            </h3>

            <div className="input-group">
              <label className="input-label">Invoice Footer Note</label>
              <input
                type="text"
                className="input-field"
                value={footerNote}
                onChange={(e) => setFooterNote(e.target.value)}
                placeholder="Thank you for your payment!"
              />
            </div>

            <div className="grid-2">
              <div className="input-group">
                <label className="input-label">Thermal Print Width (Dots)</label>
                <input
                  type="number"
                  className="input-field"
                  value={thermalWidth}
                  onChange={(e) => setThermalWidth(e.target.value)}
                  title="Width in dots. Common widths: 80mm = 576, 58mm = 384"
                  required
                />
              </div>
              <div className="input-group">
                <label className="input-label">PDF Margin Size (Pixels)</label>
                <input
                  type="number"
                  className="input-field"
                  value={marginSize}
                  onChange={(e) => setMarginSize(e.target.value)}
                  required
                />
              </div>
            </div>

            <div className="grid-2">
              <div className="input-group">
                <label className="input-label">Header Font Size (pt)</label>
                <input
                  type="number"
                  className="input-field"
                  value={headerFontSize}
                  onChange={(e) => setHeaderFontSize(e.target.value)}
                  required
                />
              </div>
              <div className="input-group">
                <label className="input-label">Body Font Size (pt)</label>
                <input
                  type="number"
                  className="input-field"
                  value={bodyFontSize}
                  onChange={(e) => setBodyFontSize(e.target.value)}
                  required
                />
              </div>
            </div>
          </div>

          {error && (
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: 'var(--error)' }}>
              <AlertCircle size={18} />
              <span>{error}</span>
            </div>
          )}

          {success && (
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: 'var(--accent-primary)', fontWeight: 600 }}>
              <Check size={18} />
              <span>Settings saved successfully! Mobile clients will pull these changes on next sync.</span>
            </div>
          )}

          <button
            type="submit"
            className="btn btn-primary"
            disabled={saving}
            style={{ padding: '14px 28px', alignSelf: 'flex-end', fontSize: '1rem', gap: '8px' }}
          >
            <Save size={18} />
            {saving ? 'Saving changes...' : 'Save Invoice Config'}
          </button>
        </form>
      )}
    </div>
  );
}
