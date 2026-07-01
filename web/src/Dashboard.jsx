import React, { useState, useEffect } from 'react';
import api from './api';
import UserManagement from './UserManagement';
import AcademicYears from './AcademicYears';
import InventoryManagement from './InventoryManagement';
import Reports from './Reports';
import Attendance from './Attendance';
import InvoiceSettings from './InvoiceSettings';
import FeeConfiguration from './FeeConfiguration';
import { Users, Calendar, Package, BarChart3, LogOut, School, Shield, CheckSquare, Settings, CreditCard } from 'lucide-react';

export default function Dashboard({ user, onLogout }) {
  const [activeTab, setActiveTab] = useState('');
  const [academicYears, setAcademicYears] = useState([]);
  const [selectedYearId, setSelectedYearId] = useState('');
  const [activeYearLabel, setActiveYearLabel] = useState('Loading...');

  // Configure tabs based on user roles
  const menuItems = [
    { id: 'users', label: 'User Accounts', icon: Users, roles: ['MASTER'], component: UserManagement },
    { id: 'years', label: 'Academic Years', icon: Calendar, roles: ['MASTER', 'ADMIN'], component: AcademicYears },
    { id: 'fees', label: 'Fee Configuration', icon: CreditCard, roles: ['MASTER', 'ADMIN'], component: FeeConfiguration },
    { id: 'attendance', label: 'Student Attendance', icon: CheckSquare, roles: ['MASTER', 'ADMIN', 'TEACHER'], component: Attendance },
    { id: 'inventory', label: 'Inventory & Stock', icon: Package, roles: ['MASTER', 'ADMIN', 'ACCOUNTANT'], component: InventoryManagement },
    { id: 'reports', label: 'Financial Reports', icon: BarChart3, roles: ['MASTER', 'ADMIN', 'ACCOUNTANT'], component: Reports },
    { id: 'invoice-settings', label: 'Invoice Settings', icon: Settings, roles: ['MASTER'], component: InvoiceSettings },
  ];

  const visibleMenuItems = menuItems.filter(item => item.roles.includes(user.role));

  useEffect(() => {
    // Set default tab to the first visible one
    if (visibleMenuItems.length > 0) {
      setActiveTab(visibleMenuItems[0].id);
    }
    fetchAcademicYears();
  }, [user.role]);

  const fetchAcademicYears = async () => {
    try {
      const res = await api.get('/api/academic-years');
      setAcademicYears(res.data);
      const active = res.data.find(y => y.isCurrent);
      if (active) {
        setSelectedYearId(active.id);
        setActiveYearLabel(active.label);
      } else if (res.data.length > 0) {
        setSelectedYearId(res.data[0].id);
        setActiveYearLabel(res.data[0].label);
      }
    } catch (err) {
      console.error('Failed to load academic years list');
    }
  };

  const handleSelectYear = (e) => {
    const yearId = e.target.value;
    setSelectedYearId(yearId);
    const yr = academicYears.find(y => y.id === yearId);
    if (yr) {
      setActiveYearLabel(yr.label);
      // Tell API client to use this context header or query param if required
      // Note: in the server-side app, active year context is read per-request or global,
      // switching it locally updates active selection.
    }
  };

  const renderActiveContent = () => {
    const item = visibleMenuItems.find(m => m.id === activeTab);
    if (!item) return <div style={{ color: 'var(--text-secondary)' }}>Access Denied</div>;
    const Component = item.component;
    return <Component currentUser={user} selectedYearId={selectedYearId} />;
  };

  return (
    <div className="app-container">
      {/* Sidebar Navigation */}
      <aside className="sidebar">
        <div className="sidebar-title">
          <School size={28} style={{ color: '#818cf8' }} />
          <span>School Hub</span>
        </div>

        <ul className="sidebar-menu">
          {visibleMenuItems.map((item) => {
            const Icon = item.icon;
            const isActive = activeTab === item.id;
            return (
              <li key={item.id}>
                <a
                  href={`#${item.id}`}
                  className={`sidebar-item ${isActive ? 'active' : ''}`}
                  onClick={(e) => {
                    e.preventDefault();
                    setActiveTab(item.id);
                  }}
                >
                  <Icon size={20} />
                  <span>{item.label}</span>
                </a>
              </li>
            );
          })}
        </ul>

        <div className="sidebar-footer">
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <div style={{
              background: 'rgba(99,102,241,0.1)',
              border: '1px solid rgba(99,102,241,0.2)',
              borderRadius: '50%',
              padding: '8px',
              color: 'var(--accent-primary)',
              display: 'flex',
            }}>
              <Shield size={18} />
            </div>
            <div>
              <p style={{ fontWeight: 700, fontSize: '0.9rem', color: 'var(--text-primary)' }}>{user.username}</p>
              <p style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>{user.role}</p>
            </div>
          </div>
          <button className="btn btn-danger" style={{ width: '100%', gap: '8px' }} onClick={onLogout}>
            <LogOut size={16} />
            Sign Out
          </button>
        </div>
      </aside>

      {/* Main Panel Content */}
      <main className="main-content">
        <div style={{
          display: 'flex',
          justifyContent: 'flex-end',
          alignItems: 'center',
          gap: '16px',
          marginBottom: '20px',
          background: 'rgba(255, 255, 255, 0.02)',
          padding: '12px 24px',
          borderRadius: '12px',
          border: '1px solid var(--border-color)'
        }}>
          <span style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>Selected Term Context:</span>
          <select
            className="input-field select-field"
            style={{ width: '160px', padding: '6px 12px', fontSize: '0.85rem', marginBottom: 0 }}
            value={selectedYearId}
            onChange={handleSelectYear}
          >
            {academicYears.map(y => (
              <option key={y.id} value={y.id}>{y.label} {y.isCurrent ? '(Active)' : ''}</option>
            ))}
          </select>
        </div>

        {renderActiveContent()}
      </main>
    </div>
  );
}
