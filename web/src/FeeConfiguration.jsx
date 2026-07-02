import React, { useState, useEffect } from 'react';
import api from './api';
import { Plus, Edit2, Trash2, Calendar, FileText, CheckCircle, PlusCircle } from 'lucide-react';

export default function FeeConfiguration({ currentUser, selectedYearId }) {
  // Category States
  const [categories, setCategories] = useState([]);
  const [newCatName, setNewCatName] = useState('');
  const [newCatDesc, setNewCatDesc] = useState('');
  const [editingCatId, setEditingCatId] = useState('');
  const [editCatName, setEditCatName] = useState('');
  const [editCatDesc, setEditCatDesc] = useState('');

  // Structure States
  const [structures, setStructures] = useState([]);
  const [classes, setClasses] = useState([]);
  const [selectedClassId, setSelectedClassId] = useState('');
  const [newStructCatId, setNewStructCatId] = useState('');
  const [newStructAmount, setNewStructAmount] = useState('');
  const [isRecurring, setIsRecurring] = useState(false);
  const [dayOfMonth, setDayOfMonth] = useState(10);
  const [academicYears, setAcademicYears] = useState([]);
  const [newStructDueDate, setNewStructDueDate] = useState('');
  const [newStructDesc, setNewStructDesc] = useState('');

  const [loading, setLoading] = useState(false);
  const [isAssigning, setIsAssigning] = useState('');

  useEffect(() => {
    fetchCategories();
    fetchClasses();
    fetchAcademicYears();
  }, [selectedYearId]);

  useEffect(() => {
    if (selectedClassId) {
      fetchStructures(selectedClassId);
    } else {
      setStructures([]);
    }
  }, [selectedClassId, selectedYearId]);

  const fetchCategories = async () => {
    try {
      const res = await api.get('/api/fee-categories');
      setCategories(res.data);
    } catch (err) {
      console.error('Failed to load categories');
    }
  };

  const fetchClasses = async () => {
    try {
      const res = await api.get('/api/classes', { params: { academicYearId: selectedYearId } });
      setClasses(res.data);
      if (res.data.length > 0) {
        setSelectedClassId(res.data[0].id);
      }
    } catch (err) {
      console.error('Failed to load classes');
    }
  };

  const fetchAcademicYears = async () => {
    try {
      const res = await api.get('/api/academic-years');
      setAcademicYears(res.data);
    } catch (err) {
      console.error('Failed to load academic years');
    }
  };

  const fetchStructures = async (classId) => {
    setLoading(true);
    try {
      const res = await api.get('/api/fee-structures', {
        params: { classId, academicYearId: selectedYearId }
      });
      setStructures(res.data);
    } catch (err) {
      console.error('Failed to load structures');
    } finally {
      setLoading(false);
    }
  };

  // Category Actions
  const handleCreateCategory = async (e) => {
    e.preventDefault();
    if (!newCatName) return;
    try {
      await api.post('/api/fee-categories', {
        name: newCatName,
        description: newCatDesc || null
      });
      setNewCatName('');
      setNewCatDesc('');
      fetchCategories();
    } catch (err) {
      alert(err.response?.data?.error || 'Failed to create category');
    }
  };

  const handleUpdateCategory = async (id) => {
    if (!editCatName) return;
    try {
      await api.patch(`/api/fee-categories/${id}`, {
        name: editCatName,
        description: editCatDesc || null
      });
      setEditingCatId('');
      fetchCategories();
    } catch (err) {
      alert(err.response?.data?.error || 'Failed to update category');
    }
  };

  const handleDeleteCategory = async (id) => {
    if (!window.confirm('Are you sure you want to delete this category? Any linked fee structures might fail to reference it.')) return;
    try {
      await api.delete(`/api/fee-categories/${id}`);
      fetchCategories();
    } catch (err) {
      alert(err.response?.data?.error || 'Failed to delete category');
    }
  };

  // Structure Actions
  const handleCreateStructure = async (e) => {
    e.preventDefault();
    if (!newStructCatId || !selectedClassId || !newStructAmount || (!newStructDueDate && !isRecurring)) return;

    try {
      if (isRecurring) {
        const yearRecord = academicYears.find(y => y.id === selectedYearId);
        if (!yearRecord) {
          alert('Selected academic year not found.');
          return;
        }

        const start = new Date(yearRecord.startDate);
        const end = new Date(yearRecord.endDate);
        
        let current = new Date(start.getFullYear(), start.getMonth(), 1);
        const targetDay = parseInt(dayOfMonth, 10) || 10;
        
        const category = categories.find(c => c.id === newStructCatId);
        const categoryName = category ? category.name : 'Fee';
        
        while (current <= end) {
          let due = new Date(current.getFullYear(), current.getMonth(), targetDay);
          
          if (due < start) due = new Date(start);
          if (due > end) due = new Date(end);
          
          const monthName = due.toLocaleString('en-US', { month: 'long', year: 'numeric' });
          const desc = newStructDesc 
            ? `${newStructDesc} - ${monthName}` 
            : `${categoryName} - ${monthName}`;
          
          await api.post('/api/fee-structures', {
            feeCategoryId: newStructCatId,
            classId: selectedClassId,
            academicYearId: selectedYearId,
            amount: parseFloat(newStructAmount),
            dueDate: due.toISOString(),
            description: desc
          });
          
          current.setMonth(current.getMonth() + 1);
        }
      } else {
        await api.post('/api/fee-structures', {
          feeCategoryId: newStructCatId,
          classId: selectedClassId,
          academicYearId: selectedYearId,
          amount: parseFloat(newStructAmount),
          dueDate: new Date(newStructDueDate).toISOString(),
          description: newStructDesc || null
        });
      }

      setNewStructCatId('');
      setNewStructAmount('');
      setNewStructDueDate('');
      setNewStructDesc('');
      setIsRecurring(false);
      setDayOfMonth(10);
      fetchStructures(selectedClassId);
    } catch (err) {
      alert(err.response?.data?.error || 'Failed to create fee structure');
    }
  };

  const handleAssignStructure = async (id) => {
    setIsAssigning(id);
    try {
      const res = await api.post(`/api/fee-structures/${id}/assign`);
      alert(`Success: Billed ${res.data.created} student invoice records! (Skipped ${res.data.skipped} already billed).`);
    } catch (err) {
      alert(err.response?.data?.error || 'Failed to assign fee structure');
    } finally {
      setIsAssigning('');
    }
  };

  return (
    <div>
      <div className="page-header">
        <div>
          <h1 className="page-title">Fee Configuration & Management</h1>
          <p className="page-subtitle">Configure billing categories, create class fee structures, and bill student balances</p>
        </div>
      </div>

      <div className="grid-2" style={{ alignItems: 'flex-start', gap: '32px' }}>
        {/* Section 1: Categories Manager */}
        <div className="glass-card">
          <h3 style={{ fontFamily: 'Outfit, sans-serif', fontSize: '1.25rem', fontWeight: 600, marginBottom: '20px', display: 'flex', alignItems: 'center', gap: '8px' }}>
            <PlusCircle size={20} style={{ color: 'var(--accent-secondary)' }} />
            Fee Categories
          </h3>

          <form onSubmit={handleCreateCategory} style={{ display: 'flex', gap: '12px', marginBottom: '24px' }}>
            <input
              type="text"
              className="input-field"
              placeholder="e.g. Activity Fee"
              value={newCatName}
              onChange={(e) => setNewCatName(e.target.value)}
              required
            />
            <button type="submit" className="btn btn-primary" style={{ padding: '0 16px', flexShrink: 0 }}>
              <Plus size={16} /> Add Category
            </button>
          </form>

          <ul style={{ listStyleType: 'none', display: 'flex', flexDirection: 'column', gap: '12px' }}>
            {categories.map((cat) => {
              const isEditing = editingCatId === cat.id;
              return (
                <li
                  key={cat.id}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    padding: '12px 16px',
                    borderRadius: '8px',
                    border: '1px solid var(--border-color)',
                    background: 'rgba(255,255,255,0.01)',
                  }}
                >
                  {isEditing ? (
                    <div style={{ display: 'flex', gap: '8px', width: '100%' }}>
                      <input
                        type="text"
                        className="input-field"
                        value={editCatName}
                        onChange={(e) => setEditCatName(e.target.value)}
                        required
                        style={{ padding: '6px 12px', fontSize: '0.9rem' }}
                      />
                      <button onClick={() => handleUpdateCategory(cat.id)} className="btn btn-primary" style={{ padding: '0 12px', fontSize: '0.85rem' }}>Save</button>
                      <button onClick={() => setEditingCatId('')} className="btn btn-secondary" style={{ padding: '0 12px', fontSize: '0.85rem' }}>Cancel</button>
                    </div>
                  ) : (
                    <>
                      <div>
                        <span style={{ fontWeight: 600, color: 'var(--text-primary)' }}>{cat.name}</span>
                        {cat.description && <span style={{ display: 'block', fontSize: '0.8rem', color: 'var(--text-muted)' }}>{cat.description}</span>}
                      </div>
                      <div style={{ display: 'flex', gap: '8px' }}>
                        <button
                          onClick={() => {
                            setEditingCatId(cat.id);
                            setEditCatName(cat.name);
                            setEditCatDesc(cat.description || '');
                          }}
                          className="btn btn-secondary"
                          style={{ padding: '8px', minWidth: 'auto', background: 'transparent', border: 'none' }}
                          title="Rename"
                        >
                          <Edit2 size={14} style={{ color: 'var(--text-secondary)' }} />
                        </button>
                        {/* Standard predefined system categories shouldn't be deleted easily */}
                        {!['Tuition Fee', 'Admission Fee', 'Exam Fee'].includes(cat.name) && (
                          <button
                            onClick={() => handleDeleteCategory(cat.id)}
                            className="btn btn-secondary"
                            style={{ padding: '8px', minWidth: 'auto', background: 'transparent', border: 'none' }}
                            title="Delete"
                          >
                            <Trash2 size={14} style={{ color: 'var(--error)' }} />
                          </button>
                        )}
                      </div>
                    </>
                  )}
                </li>
              );
            })}
          </ul>
        </div>

        {/* Section 2: Structure Configurator */}
        <div className="glass-card" style={{ flexGrow: 1 }}>
          <h3 style={{ fontFamily: 'Outfit, sans-serif', fontSize: '1.25rem', fontWeight: 600, marginBottom: '20px', display: 'flex', alignItems: 'center', gap: '8px' }}>
            <FileText size={20} style={{ color: 'var(--accent-secondary)' }} />
            Fee Structures
          </h3>

          <div className="input-group" style={{ marginBottom: '24px' }}>
            <label className="input-label">Selected Class</label>
            <select
              className="input-field select-field"
              value={selectedClassId}
              onChange={(e) => setSelectedClassId(e.target.value)}
            >
              <option value="">-- Select Class --</option>
              {classes.map((c) => (
                <option key={c.id} value={c.id}>{c.name}</option>
              ))}
            </select>
          </div>

          {selectedClassId && (
            <>
              {/* Add Structure Form */}
              <form onSubmit={handleCreateStructure} style={{ borderBottom: '1px solid var(--border-color)', paddingBottom: '24px', marginBottom: '24px' }}>
                <h4 style={{ fontSize: '0.95rem', fontWeight: 700, marginBottom: '16px' }}>Define New Dues Template:</h4>
                <div className="grid-2" style={{ marginBottom: '12px' }}>
                  <div className="input-group" style={{ marginBottom: 0 }}>
                    <label className="input-label">Category</label>
                    <select
                      className="input-field select-field"
                      value={newStructCatId}
                      onChange={(e) => setNewStructCatId(e.target.value)}
                      required
                    >
                      <option value="">-- Choose --</option>
                      {categories.map((c) => (
                        <option key={c.id} value={c.id}>{c.name}</option>
                      ))}
                    </select>
                  </div>
                  <div className="input-group" style={{ marginBottom: 0 }}>
                    <label className="input-label">Amount (₹)</label>
                    <input
                      type="number"
                      step="0.01"
                      className="input-field"
                      placeholder="e.g. 1500"
                      value={newStructAmount}
                      onChange={(e) => setNewStructAmount(e.target.value)}
                      required
                    />
                  </div>
                </div>

                <div className="grid-2" style={{ marginBottom: '16px' }}>
                  <div className="input-group" style={{ marginBottom: 0 }}>
                    <label className="input-label">Due Date</label>
                    <input
                      type="date"
                      className="input-field"
                      value={newStructDueDate}
                      onChange={(e) => setNewStructDueDate(e.target.value)}
                      required
                    />
                  </div>
                  <div className="input-group" style={{ marginBottom: 0 }}>
                    <label className="input-label">Description / Period</label>
                    <input
                      type="text"
                      className="input-field"
                      placeholder="e.g. May 2026 Tuition"
                      value={newStructDesc}
                      onChange={(e) => setNewStructDesc(e.target.value)}
                    />
                  </div>
                </div>

                <button type="submit" className="btn btn-primary" style={{ width: '100%' }}>
                  <Plus size={16} /> Save Fee Structure
                </button>
              </form>

              {/* Structures List */}
              <h4 style={{ fontSize: '0.95rem', fontWeight: 700, marginBottom: '12px' }}>Active structures for selected class:</h4>
              {loading ? (
                <p style={{ color: 'var(--text-secondary)' }}>Loading structures...</p>
              ) : structures.length > 0 ? (
                <ul style={{ listStyleType: 'none', display: 'flex', flexDirection: 'column', gap: '12px' }}>
                  {structures.map((s) => (
                    <li
                      key={s.id}
                      style={{
                        padding: '16px',
                        borderRadius: '8px',
                        border: '1px solid var(--border-color)',
                        background: 'rgba(255,255,255,0.01)',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                      }}
                    >
                      <div>
                        <span style={{ fontWeight: 700, color: 'var(--text-primary)', fontSize: '1.05rem' }}>
                          ₹{parseFloat(s.amount).toFixed(2)}
                        </span>
                        <span style={{ margin: '0 8px', color: 'var(--text-muted)' }}>·</span>
                        <span style={{ color: 'var(--text-secondary)', fontWeight: 600 }}>{s.feeCategory?.name}</span>
                        <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)', marginTop: '4px' }}>
                          Description: {s.description || 'N/A'} <br />
                          Due: {new Date(s.dueDate).toLocaleDateString()}
                        </p>
                      </div>
                      <button
                        onClick={() => handleAssignStructure(s.id)}
                        disabled={isAssigning === s.id}
                        className="btn btn-primary"
                        style={{ padding: '8px 12px', fontSize: '0.8rem', background: '#3b82f6', boxShadow: 'none' }}
                      >
                        {isAssigning === s.id ? 'Billing...' : 'Bill to Class'}
                      </button>
                    </li>
                  ))}
                </ul>
              ) : (
                <p style={{ color: 'var(--text-muted)', fontStyle: 'italic', fontSize: '0.9rem' }}>No fee structures defined for this class yet.</p>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  );
}
