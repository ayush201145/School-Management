import React, { useState, useEffect } from 'react';
import api from './api';
import { Calendar, UserCheck, AlertCircle, Save } from 'lucide-react';

export default function Attendance({ currentUser, selectedYearId }) {
  const [classes, setClasses] = useState([]);
  const [selectedClassId, setSelectedClassId] = useState('');
  const [sections, setSections] = useState([]);
  const [selectedSectionId, setSelectedSectionId] = useState('');
  const [date, setDate] = useState(new Date().toISOString().split('T')[0]);

  const [students, setStudents] = useState([]);
  const [statusMap, setStatusMap] = useState({});
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);

  useEffect(() => {
    fetchClasses();
  }, [selectedYearId]);

  useEffect(() => {
    if (selectedClassId) {
      fetchSections(selectedClassId);
    } else {
      setSections([]);
      setSelectedSectionId('');
    }
  }, [selectedClassId]);

  useEffect(() => {
    if (selectedSectionId && date) {
      fetchStudentsAndAttendance();
    } else {
      setStudents([]);
      setStatusMap({});
    }
  }, [selectedSectionId, date]);

  const fetchClasses = async () => {
    try {
      const res = await api.get('/api/classes', { params: { academicYearId: selectedYearId } });
      setClasses(res.data);
      if (res.data.length > 0) {
        setSelectedClassId(res.data[0].id);
      }
    } catch (err) {
      setError('Failed to fetch classes.');
    }
  };

  const fetchSections = async (classId) => {
    try {
      const res = await api.get('/api/sections', { params: { classId } });
      setSections(res.data);
      if (res.data.length > 0) {
        setSelectedSectionId(res.data[0].id);
      } else {
        setSelectedSectionId('');
      }
    } catch (err) {
      setError('Failed to fetch sections.');
    }
  };

  const fetchStudentsAndAttendance = async () => {
    setLoading(true);
    setError('');
    setSuccess(false);
    try {
      // 1. Fetch students in the section
      const studentRes = await api.get('/api/students', { params: { sectionId: selectedSectionId, isActive: true } });
      
      // 2. Fetch existing attendance records
      const attendanceRes = await api.get('/api/attendance', { params: { sectionId: selectedSectionId, date } });
      
      const attendanceList = attendanceRes.data;
      const initialMap = {};
      
      studentRes.data.forEach(student => {
        const record = attendanceList.find(r => r.studentId === student.id);
        initialMap[student.id] = record ? record.status : 'PRESENT';
      });

      setStudents(studentRes.data);
      setStatusMap(initialMap);
    } catch (err) {
      setError('Failed to load students or attendance records.');
    } finally {
      setLoading(false);
    }
  };

  const handleStatusChange = (studentId, status) => {
    setStatusMap(prev => ({
      ...prev,
      [studentId]: status
    }));
    setSuccess(false);
  };

  const handleSave = async () => {
    setSaving(true);
    setError('');
    setSuccess(false);
    try {
      const records = Object.keys(statusMap).map(studentId => ({
        studentId,
        status: statusMap[studentId]
      }));

      await api.post('/api/attendance', {
        sectionId: selectedSectionId,
        date,
        records
      });

      setSuccess(true);
      setTimeout(() => setSuccess(false), 3000);
    } catch (err) {
      setError('Failed to save attendance.');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div>
      <div className="page-header">
        <div>
          <h1 className="page-title">Student Attendance</h1>
          <p className="page-subtitle">Track, edit, and record daily attendance records class-wise</p>
        </div>
      </div>

      {/* Filters Card */}
      <div className="glass-card" style={{ marginBottom: '32px' }}>
        <div className="grid-3" style={{ alignItems: 'flex-end' }}>
          <div className="input-group" style={{ marginBottom: 0 }}>
            <label className="input-label">Select Class</label>
            <select
              className="input-field select-field"
              value={selectedClassId}
              onChange={(e) => setSelectedClassId(e.target.value)}
            >
              <option value="">-- Choose Class --</option>
              {classes.map((c) => (
                <option key={c.id} value={c.id}>{c.name}</option>
              ))}
            </select>
          </div>

          <div className="input-group" style={{ marginBottom: 0 }}>
            <label className="input-label">Select Section</label>
            <select
              className="input-field select-field"
              value={selectedSectionId}
              onChange={(e) => setSelectedSectionId(e.target.value)}
              disabled={!selectedClassId}
            >
              <option value="">-- Choose Section --</option>
              {sections.map((s) => (
                <option key={s.id} value={s.id}>{s.name}</option>
              ))}
            </select>
          </div>

          <div className="input-group" style={{ marginBottom: 0 }}>
            <label className="input-label">Attendance Date</label>
            <input
              type="date"
              className="input-field"
              value={date}
              onChange={(e) => setDate(e.target.value)}
            />
          </div>
        </div>
      </div>

      {error && (
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: 'var(--error)', marginBottom: '20px' }}>
          <AlertCircle size={18} />
          <span>{error}</span>
        </div>
      )}

      {success && (
        <div style={{ color: 'var(--accent-primary)', fontWeight: 600, marginBottom: '20px' }}>
          Attendance saved successfully!
        </div>
      )}

      {loading ? (
        <p style={{ color: 'var(--text-secondary)' }}>Loading student records...</p>
      ) : students.length > 0 ? (
        <div className="glass-card" style={{ padding: '24px 0' }}>
          <div style={{ padding: '0 24px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
            <h3 style={{ fontFamily: 'Outfit, sans-serif', fontSize: '1.2rem', fontWeight: 600 }}>
              Roll Call List ({students.length} students)
            </h3>
            <button
              className="btn btn-primary"
              disabled={saving}
              onClick={handleSave}
              style={{ padding: '10px 20px', fontSize: '0.9rem' }}
            >
              <Save size={16} />
              {saving ? 'Saving...' : 'Save Attendance'}
            </button>
          </div>

          <div className="table-container" style={{ margin: 0, border: 'none', background: 'transparent' }}>
            <table className="custom-table">
              <thead>
                <tr>
                  <th style={{ paddingLeft: '24px' }}>Adm No.</th>
                  <th>Roll No.</th>
                  <th>Student Name</th>
                  <th style={{ textAlign: 'right', paddingRight: '24px' }}>Status</th>
                </tr>
              </thead>
              <tbody>
                {students.map((student) => (
                  <tr key={student.id}>
                    <td style={{ paddingLeft: '24px', color: 'var(--text-secondary)', fontSize: '0.9rem' }}>
                      {student.admissionNo}
                    </td>
                    <td>{student.rollNo || '-'}</td>
                    <td style={{ fontWeight: 600, color: 'var(--text-primary)' }}>
                      {student.firstName} {student.lastName}
                    </td>
                    <td style={{ textAlign: 'right', paddingRight: '24px' }}>
                      <div style={{ display: 'inline-flex', gap: '4px', background: 'rgba(255,255,255,0.02)', padding: '4px', borderRadius: '8px', border: '1px solid var(--border-color)' }}>
                        {[
                          { value: 'PRESENT', label: 'P', color: '#10b981' },
                          { value: 'ABSENT', label: 'A', color: '#ef4444' },
                          { value: 'LATE', label: 'L', color: '#f59e0b' },
                          { value: 'LEAVE', label: 'E', color: '#3b82f6' }
                        ].map((btn) => {
                          const isSel = statusMap[student.id] === btn.value;
                          return (
                            <button
                              key={btn.value}
                              onClick={() => handleStatusChange(student.id, btn.value)}
                              style={{
                                width: '32px',
                                height: '32px',
                                borderRadius: '6px',
                                border: 'none',
                                cursor: 'pointer',
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                                fontSize: '0.85rem',
                                fontWeight: 700,
                                transition: 'all 0.15s ease',
                                background: isSel ? btn.color : 'transparent',
                                color: isSel ? '#ffffff' : 'var(--text-secondary)',
                                boxShadow: isSel ? `0 2px 8px ${btn.color}40` : 'none',
                              }}
                              title={btn.value}
                            >
                              {btn.label}
                            </button>
                          );
                        })}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      ) : selectedSectionId ? (
        <div className="glass-card" style={{ textAlign: 'center', padding: '48px 24px', color: 'var(--text-secondary)' }}>
          <UserCheck size={48} style={{ opacity: 0.3, marginBottom: '16px', color: 'var(--text-secondary)' }} />
          <p>No active students enrolled in this section.</p>
        </div>
      ) : (
        <div className="glass-card" style={{ textAlign: 'center', padding: '48px 24px', color: 'var(--text-secondary)' }}>
          <Calendar size={48} style={{ opacity: 0.3, marginBottom: '16px', color: 'var(--text-secondary)' }} />
          <p>Please select a Class and Section above to display student checklist.</p>
        </div>
      )}
    </div>
  );
}
