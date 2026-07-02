import React, { useState, useEffect } from 'react';
import api from './api';
import { 
  Users, UserPlus, Search, Edit2, ArrowUpCircle, XCircle, CheckCircle, 
  Eye, FileText, ShoppingBag, DollarSign, Calendar, RefreshCw 
} from 'lucide-react';

export default function Students({ currentUser, selectedYearId }) {
  // Lists
  const [students, setStudents] = useState([]);
  const [classes, setClasses] = useState([]);
  const [sections, setSections] = useState([]);
  const [inventoryVariants, setInventoryVariants] = useState([]);
  const [academicYears, setAcademicYears] = useState([]);

  // Search & Filters
  const [search, setSearch] = useState('');
  const [filterClassId, setFilterClassId] = useState('');
  const [filterSectionId, setFilterSectionId] = useState('');
  const [filterIsActive, setFilterIsActive] = useState('true');

  // Loading States
  const [loading, setLoading] = useState(false);
  const [actionLoading, setActionLoading] = useState(false);
  const [error, setError] = useState('');

  // Selected Student for details view
  const [selectedStudentId, setSelectedStudentId] = useState(null);
  const [studentDetails, setStudentDetails] = useState(null);
  const [detailTab, setDetailTab] = useState('profile'); // profile, fees, purchases

  // Modals
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showPromoteModal, setShowPromoteModal] = useState(false);
  const [showWithdrawModal, setShowWithdrawModal] = useState(false);
  const [showPaymentModal, setShowPaymentModal] = useState(false);
  const [showPurchaseModal, setShowPurchaseModal] = useState(false);

  // Form: Create Student
  const [admNo, setAdmNo] = useState('');
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [createClassId, setCreateClassId] = useState('');
  const [createSectionId, setCreateSectionId] = useState('');
  const [createSections, setCreateSections] = useState([]);
  const [guardianName, setGuardianName] = useState('');
  const [guardianPhone, setGuardianPhone] = useState('');
  const [whatsappPhone, setWhatsappPhone] = useState('');
  const [address, setAddress] = useState('');
  const [tuitionFee, setTuitionFee] = useState('');
  const [createRollNo, setCreateRollNo] = useState('');

  // Form: Promote Student
  const [promoteYearId, setPromoteYearId] = useState('');
  const [promoteClassId, setPromoteClassId] = useState('');
  const [promoteSectionId, setPromoteSectionId] = useState('');
  const [promoteClasses, setPromoteClasses] = useState([]);
  const [promoteSections, setPromoteSections] = useState([]);
  const [promoteFeeOverride, setPromoteFeeOverride] = useState('');
  const [promoteRollNo, setPromoteRollNo] = useState('');

  // Form: Withdraw Student
  const [withdrawReason, setWithdrawReason] = useState('TRANSFERRED');
  const [withdrawNotes, setWithdrawNotes] = useState('');

  // Form: Record Payment
  const [paymentFeeId, setPaymentFeeId] = useState('');
  const [paymentFeeDesc, setPaymentFeeDesc] = useState('');
  const [paymentAmount, setPaymentAmount] = useState('');
  const [paymentMode, setPaymentMode] = useState('CASH');
  const [paymentRef, setPaymentRef] = useState('');
  const [paymentNotes, setPaymentNotes] = useState('');
  const [paymentMax, setPaymentMax] = useState(0);

  // Form: Sell Item
  const [sellVariantId, setSellVariantId] = useState('');
  const [sellQty, setSellQty] = useState(1);
  const [sellDueDate, setSellDueDate] = useState(new Date().toISOString().split('T')[0]);

  // Effects
  useEffect(() => {
    fetchClasses();
    fetchAcademicYears();
    fetchInventoryVariants();
  }, [selectedYearId]);

  useEffect(() => {
    fetchStudents();
  }, [search, filterSectionId, filterIsActive, selectedYearId]);

  // When class filter changes, load its sections
  useEffect(() => {
    if (filterClassId) {
      fetchSectionsForFilter(filterClassId);
    } else {
      setSections([]);
      setFilterSectionId('');
    }
  }, [filterClassId]);

  // Create student class selection
  useEffect(() => {
    if (createClassId) {
      fetchSectionsForCreate(createClassId);
    } else {
      setCreateSections([]);
      setCreateSectionId('');
    }
  }, [createClassId]);

  // Promotion Year changes classes list
  useEffect(() => {
    if (promoteYearId) {
      fetchClassesForPromote(promoteYearId);
    } else {
      setPromoteClasses([]);
      setPromoteClassId('');
    }
  }, [promoteYearId]);

  // Promotion Class changes sections list
  useEffect(() => {
    if (promoteClassId) {
      fetchSectionsForPromote(promoteClassId);
    } else {
      setPromoteSections([]);
      setPromoteSectionId('');
    }
  }, [promoteClassId]);

  useEffect(() => {
    if (createSectionId) {
      const sectionStudents = students.filter(s => s.sectionId === createSectionId && s.isActive);
      const maxRoll = sectionStudents.reduce((max, s) => s.rollNo && s.rollNo > max ? s.rollNo : max, 0);
      setCreateRollNo(maxRoll + 1);
    } else {
      setCreateRollNo('');
    }
  }, [createSectionId, students]);

  useEffect(() => {
    if (promoteSectionId) {
      api.get('/api/students', { params: { sectionId: promoteSectionId, isActive: true } })
        .then(res => {
          const maxRoll = res.data.reduce((max, s) => s.rollNo && s.rollNo > max ? s.rollNo : max, 0);
          setPromoteRollNo(maxRoll + 1);
        })
        .catch(err => console.error(err));
    } else {
      setPromoteRollNo('');
    }
  }, [promoteSectionId]);

  // Fetch functions
  const fetchStudents = async () => {
    setLoading(true);
    try {
      const params = {
        search: search || undefined,
        sectionId: filterSectionId || undefined,
        isActive: filterIsActive === 'all' ? undefined : (filterIsActive === 'true'),
        academicYearId: selectedYearId
      };
      // Note: filterSectionId overrides class filter because section is a child of class.
      // If only class filter is set but no section filter, we don't pass sectionId to backend.
      // We list students and filter locally or let backend filter.
      const res = await api.get('/api/students', { params });
      
      // If we filtered by class but no section was picked:
      if (filterClassId && !filterSectionId) {
        const classFiltered = res.data.filter(s => s.section?.classId === filterClassId);
        setStudents(classFiltered);
      } else {
        setStudents(res.data);
      }
    } catch (err) {
      setError('Failed to fetch students.');
    } finally {
      setLoading(false);
    }
  };

  const fetchClasses = async () => {
    try {
      const res = await api.get('/api/classes', { params: { academicYearId: selectedYearId } });
      setClasses(res.data);
    } catch (err) {
      console.error(err);
    }
  };

  const fetchAcademicYears = async () => {
    try {
      const res = await api.get('/api/academic-years');
      setAcademicYears(res.data);
    } catch (err) {
      console.error(err);
    }
  };

  const fetchInventoryVariants = async () => {
    try {
      const res = await api.get('/api/inventory');
      // res.data is a list of variants with items categories
      setInventoryVariants(res.data);
    } catch (err) {
      console.error(err);
    }
  };

  const fetchSectionsForFilter = async (classId) => {
    try {
      const res = await api.get('/api/sections', { params: { classId } });
      setSections(res.data);
    } catch (err) {
      console.error(err);
    }
  };

  const fetchSectionsForCreate = async (classId) => {
    try {
      const res = await api.get('/api/sections', { params: { classId } });
      setCreateSections(res.data);
      if (res.data.length > 0) {
        setCreateSectionId(res.data[0].id);
      }
    } catch (err) {
      console.error(err);
    }
  };

  const fetchClassesForPromote = async (yearId) => {
    try {
      const res = await api.get('/api/classes', { params: { academicYearId: yearId } });
      setPromoteClasses(res.data);
      if (res.data.length > 0) {
        setPromoteClassId(res.data[0].id);
      }
    } catch (err) {
      console.error(err);
    }
  };

  const fetchSectionsForPromote = async (classId) => {
    try {
      const res = await api.get('/api/sections', { params: { classId } });
      setPromoteSections(res.data);
      if (res.data.length > 0) {
        setPromoteSectionId(res.data[0].id);
      }
    } catch (err) {
      console.error(err);
    }
  };

  const fetchStudentDetails = async (id) => {
    setSelectedStudentId(id);
    setError('');
    try {
      const res = await api.get(`/api/students/${id}`);
      // Also fetch purchases
      const purchaseRes = await api.get(`/api/students/${id}/purchases`);
      setStudentDetails({
        ...res.data,
        purchases: purchaseRes.data
      });
    } catch (err) {
      setError('Failed to fetch student details.');
    }
  };

  // Actions
  const handleCreateStudent = async (e) => {
    e.preventDefault();
    setActionLoading(true);
    setError('');
    try {
      await api.post('/api/students', {
        admissionNo: admNo || undefined,
        rollNo: createRollNo ? parseInt(createRollNo, 10) : null,
        firstName,
        lastName,
        sectionId: createSectionId,
        guardianName: guardianName || null,
        guardianPhone: guardianPhone || null,
        whatsappPhone: whatsappPhone || null,
        address: address || null,
        tuitionFee: tuitionFee ? parseFloat(tuitionFee) : null,
      });

      setShowCreateModal(false);
      // Reset Form
      setAdmNo('');
      setCreateRollNo('');
      setFirstName('');
      setLastName('');
      setCreateClassId('');
      setCreateSectionId('');
      setGuardianName('');
      setGuardianPhone('');
      setWhatsappPhone('');
      setAddress('');
      setTuitionFee('');

      fetchStudents();
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to create student.');
    } finally {
      setActionLoading(false);
    }
  };

  const handlePromoteStudent = async (e) => {
    e.preventDefault();
    if (!promoteSectionId) return;
    setActionLoading(true);
    setError('');
    try {
      await api.patch(`/api/students/${selectedStudentId}`, {
        sectionId: promoteSectionId,
        rollNo: promoteRollNo ? parseInt(promoteRollNo, 10) : null,
        tuitionFee: promoteFeeOverride ? parseFloat(promoteFeeOverride) : null,
      });

      setShowPromoteModal(false);
      setPromoteYearId('');
      setPromoteClassId('');
      setPromoteSectionId('');
      setPromoteFeeOverride('');
      setPromoteRollNo('');

      // Reload
      fetchStudentDetails(selectedStudentId);
      fetchStudents();
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to promote student.');
    } finally {
      setActionLoading(false);
    }
  };

  const handleMarkFeeDefaulted = async (feeId) => {
    if (!window.confirm("Are you sure you want to mark this fee as defaulted? This will write off the remaining balance and exclude it from active dues reports.")) {
      return;
    }
    try {
      await api.post(`/api/student-fees/${feeId}/default`);
      alert("Fee marked as defaulted.");
      if (selectedStudentId) {
        fetchStudentDetails(selectedStudentId);
      }
      fetchStudents();
    } catch (err) {
      alert(err.response?.data?.error || "Failed to mark fee as defaulted.");
    }
  };

  const handleWithdrawStudent = async (e) => {
    e.preventDefault();
    setActionLoading(true);
    setError('');
    try {
      await api.post(`/api/students/${selectedStudentId}/withdraw`, {
        reason: withdrawReason,
        notes: withdrawNotes || null
      });
      setShowWithdrawModal(false);
      setWithdrawNotes('');
      // Reload
      fetchStudentDetails(selectedStudentId);
      fetchStudents();
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to withdraw student.');
    } finally {
      setActionLoading(false);
    }
  };

  const handleReinstateStudent = async () => {
    if (!window.confirm('Are you sure you want to reinstate this student to active status?')) return;
    setActionLoading(true);
    setError('');
    try {
      await api.post(`/api/students/${selectedStudentId}/reinstate`);
      // Reload
      fetchStudentDetails(selectedStudentId);
      fetchStudents();
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to reinstate student.');
    } finally {
      setActionLoading(false);
    }
  };

  const handleRecordPayment = async (e) => {
    e.preventDefault();
    setActionLoading(true);
    setError('');
    try {
      await api.post(`/api/student-fees/${paymentFeeId}/payments`, {
        amount: parseFloat(paymentAmount),
        mode: paymentMode,
        referenceNo: paymentRef || null,
        notes: paymentNotes || null,
      });

      setShowPaymentModal(false);
      setPaymentAmount('');
      setPaymentRef('');
      setPaymentNotes('');
      
      // Reload
      fetchStudentDetails(selectedStudentId);
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to record payment.');
    } finally {
      setActionLoading(false);
    }
  };

  const handleSellItem = async (e) => {
    e.preventDefault();
    if (!sellVariantId) return;
    setActionLoading(true);
    setError('');
    try {
      const res = await api.post(`/api/students/${selectedStudentId}/purchases`, {
        itemVariantId: sellVariantId,
        quantity: parseInt(sellQty),
        dueDate: sellDueDate ? new Date(sellDueDate).toISOString() : null,
      });

      if (res.data.warning) {
        alert(`Purchase Recorded! Warning: ${res.data.warning}`);
      }

      setShowPurchaseModal(false);
      setSellVariantId('');
      setSellQty(1);

      // Reload
      fetchStudentDetails(selectedStudentId);
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to record item sale.');
    } finally {
      setActionLoading(false);
    }
  };

  return (
    <div style={{ display: 'flex', gap: '32px', alignItems: 'flex-start' }}>
      
      {/* Main Student Directory List */}
      <div style={{ flexGrow: 1, minWidth: 0 }}>
        <div className="page-header">
          <div>
            <h1 className="page-title">Student Directory</h1>
            <p className="page-subtitle">Add students, track accounts, mark promotions, and handle active registers</p>
          </div>
          {['MASTER', 'ADMIN', 'ACCOUNTANT'].includes(currentUser.role) && (
            <button className="btn btn-primary" onClick={() => setShowCreateModal(true)}>
              <UserPlus size={18} /> Add Student
            </button>
          )}
        </div>

        {/* Filters Card */}
        <div className="glass-card" style={{ marginBottom: '24px' }}>
          <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr 1fr 1fr', gap: '16px', alignItems: 'flex-end' }}>
            <div className="input-group" style={{ marginBottom: 0 }}>
              <label className="input-label">Search</label>
              <div style={{ position: 'relative' }}>
                <Search size={18} style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-secondary)' }} />
                <input
                  type="text"
                  className="input-field"
                  placeholder="Name, Adm No, Parent Phone..."
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                  style={{ paddingLeft: '40px' }}
                />
              </div>
            </div>

            <div className="input-group" style={{ marginBottom: 0 }}>
              <label className="input-label">Class</label>
              <select
                className="input-field select-field"
                value={filterClassId}
                onChange={(e) => setFilterClassId(e.target.value)}
              >
                <option value="">All Classes</option>
                {classes.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
              </select>
            </div>

            <div className="input-group" style={{ marginBottom: 0 }}>
              <label className="input-label">Section</label>
              <select
                className="input-field select-field"
                value={filterSectionId}
                onChange={(e) => setFilterSectionId(e.target.value)}
                disabled={!filterClassId}
              >
                <option value="">All Sections</option>
                {sections.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
              </select>
            </div>

            <div className="input-group" style={{ marginBottom: 0 }}>
              <label className="input-label">Register Status</label>
              <select
                className="input-field select-field"
                value={filterIsActive}
                onChange={(e) => setFilterIsActive(e.target.value)}
              >
                <option value="true">Active Enrolled</option>
                <option value="false">Withdrawn</option>
                <option value="all">All Records</option>
              </select>
            </div>
          </div>
        </div>

        {error && !selectedStudentId && (
          <div style={{ color: 'var(--error)', marginBottom: '16px', fontWeight: 600 }}>{error}</div>
        )}

        {loading ? (
          <p style={{ color: 'var(--text-secondary)' }}>Loading directory...</p>
        ) : (
          <div className="glass-card" style={{ padding: 0 }}>
            <div className="table-container" style={{ margin: 0, border: 'none', background: 'transparent' }}>
              <table className="custom-table">
                <thead>
                  <tr>
                    <th style={{ paddingLeft: '24px' }}>Adm No.</th>
                    <th>Roll No.</th>
                    <th>Name</th>
                    <th>Class & Section</th>
                    <th>Guardian Details</th>
                    <th>Status</th>
                    <th style={{ textAlign: 'right', paddingRight: '24px' }}>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {students.map((student) => (
                    <tr 
                      key={student.id} 
                      style={{ 
                        background: selectedStudentId === student.id ? 'rgba(129,140,248,0.08)' : 'transparent',
                        cursor: 'pointer'
                      }}
                      onClick={() => fetchStudentDetails(student.id)}
                    >
                      <td style={{ paddingLeft: '24px', fontWeight: 700, color: 'var(--text-secondary)' }}>
                        {student.admissionNo}
                      </td>
                      <td style={{ fontWeight: 600, color: 'var(--text-secondary)' }}>
                        {student.rollNo || '-'}
                      </td>
                      <td style={{ fontWeight: 600, color: 'var(--text-primary)' }}>
                        {student.firstName} {student.lastName}
                      </td>
                      <td>
                        {student.section ? `${student.section.schoolClass.name} - ${student.section.name}` : 'Not Assigned'}
                      </td>
                      <td>
                        {student.guardianName || '-' }
                        {student.guardianPhone && <span style={{ display: 'block', fontSize: '0.8rem', color: 'var(--text-secondary)' }}>{student.guardianPhone}</span>}
                      </td>
                      <td>
                        <span style={{
                          display: 'inline-flex',
                          alignItems: 'center',
                          gap: '6px',
                          padding: '4px 10px',
                          borderRadius: '12px',
                          fontSize: '0.8rem',
                          fontWeight: 600,
                          background: student.isActive ? 'rgba(16,185,129,0.1)' : 'rgba(239,68,68,0.1)',
                          color: student.isActive ? '#10b981' : '#ef4444',
                        }}>
                          <span style={{ width: '6px', height: '6px', borderRadius: '50%', background: student.isActive ? '#10b981' : '#ef4444' }} />
                          {student.isActive ? 'Active' : 'Withdrawn'}
                        </span>
                      </td>
                      <td style={{ textAlign: 'right', paddingRight: '24px' }} onClick={(e) => e.stopPropagation()}>
                        <button 
                          className="btn btn-secondary" 
                          onClick={() => fetchStudentDetails(student.id)}
                          style={{ padding: '6px 12px', fontSize: '0.8rem', gap: '4px' }}
                        >
                          <Eye size={12} /> View
                        </button>
                      </td>
                    </tr>
                  ))}
                  {students.length === 0 && (
                    <tr>
                      <td colSpan="6" style={{ textAlign: 'center', padding: '36px', color: 'var(--text-secondary)' }}>
                        No student records match search parameters.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        )}
      </div>

      {/* Right Details Panel */}
      {studentDetails && (
        <div className="glass-card" style={{ width: '450px', position: 'sticky', top: '24px', flexShrink: 0, padding: '24px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '20px' }}>
            <div>
              <h2 style={{ fontFamily: 'Outfit, sans-serif', fontSize: '1.4rem', fontWeight: 700, color: 'var(--text-primary)' }}>
                {studentDetails.firstName} {studentDetails.lastName}
              </h2>
              <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
                Admission No: {studentDetails.admissionNo} · {studentDetails.section ? `${studentDetails.section.schoolClass.name} - ${studentDetails.section.name}` : 'No Class'} · Roll No: {studentDetails.rollNo || 'N/A'}
              </span>
            </div>
            <button 
              className="btn btn-secondary" 
              onClick={() => setStudentDetails(null)}
              style={{ minWidth: 'auto', padding: '6px' }}
            >
              &times;
            </button>
          </div>

          {/* Action Row */}
          {['MASTER', 'ADMIN', 'ACCOUNTANT'].includes(currentUser.role) && (
            <div style={{ display: 'flex', gap: '8px', marginBottom: '24px' }}>
              <button 
                className="btn btn-secondary" 
                onClick={() => {
                  setPromoteYearId(selectedYearId);
                  setShowPromoteModal(true);
                }}
                style={{ flex: 1, fontSize: '0.8rem', padding: '8px 12px', gap: '6px' }}
              >
                <ArrowUpCircle size={14} style={{ color: 'var(--accent-primary)' }} />
                Promote / Migrate
              </button>
              
              {studentDetails.isActive ? (
                <button 
                  className="btn btn-secondary" 
                  onClick={() => setShowWithdrawModal(true)}
                  style={{ flex: 1, fontSize: '0.8rem', padding: '8px 12px', gap: '6px', color: 'var(--error)' }}
                >
                  <XCircle size={14} />
                  Withdraw
                </button>
              ) : (
                <button 
                  className="btn btn-secondary" 
                  onClick={handleReinstateStudent}
                  style={{ flex: 1, fontSize: '0.8rem', padding: '8px 12px', gap: '6px', color: '#10b981' }}
                >
                  <CheckCircle size={14} />
                  Reinstate
                </button>
              )}
            </div>
          )}

          {/* Details Tabs */}
          <div style={{ display: 'flex', borderBottom: '1px solid var(--border-color)', marginBottom: '20px' }}>
            {[
              { id: 'profile', label: 'Profile' },
              { id: 'fees', label: 'Fees & Dues' },
              { id: 'purchases', label: 'Shop Purchases' }
            ].map(tab => (
              <button
                key={tab.id}
                onClick={() => setDetailTab(tab.id)}
                style={{
                  flex: 1,
                  padding: '10px',
                  border: 'none',
                  background: 'transparent',
                  color: detailTab === tab.id ? 'var(--accent-primary)' : 'var(--text-secondary)',
                  fontWeight: detailTab === tab.id ? 700 : 500,
                  borderBottom: detailTab === tab.id ? '2px solid var(--accent-primary)' : 'none',
                  cursor: 'pointer'
                }}
              >
                {tab.label}
              </button>
            ))}
          </div>

          {/* Tab 1: Profile Details */}
          {detailTab === 'profile' && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '16px', fontSize: '0.9rem' }}>
              <div>
                <span style={{ color: 'var(--text-muted)', display: 'block', fontSize: '0.8rem' }}>Guardian/Parent Name</span>
                <span style={{ fontWeight: 600 }}>{studentDetails.guardianName || 'N/A'}</span>
              </div>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
                <div>
                  <span style={{ color: 'var(--text-muted)', display: 'block', fontSize: '0.8rem' }}>Guardian Phone</span>
                  <span style={{ fontWeight: 600 }}>{studentDetails.guardianPhone || 'N/A'}</span>
                </div>
                <div>
                  <span style={{ color: 'var(--text-muted)', display: 'block', fontSize: '0.8rem' }}>WhatsApp Contact</span>
                  <span style={{ fontWeight: 600 }}>{studentDetails.whatsappPhone || 'N/A'}</span>
                </div>
              </div>
              <div>
                <span style={{ color: 'var(--text-muted)', display: 'block', fontSize: '0.8rem' }}>Guardian Email</span>
                <span>{studentDetails.guardianEmail || 'N/A'}</span>
              </div>
              <div>
                <span style={{ color: 'var(--text-muted)', display: 'block', fontSize: '0.8rem' }}>Residential Address</span>
                <span>{studentDetails.address || 'N/A'}</span>
              </div>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
                <div>
                  <span style={{ color: 'var(--text-muted)', display: 'block', fontSize: '0.8rem' }}>Base Tuition Fee</span>
                  <span style={{ fontWeight: 600 }}>{studentDetails.tuitionFee ? `₹${parseFloat(studentDetails.tuitionFee).toFixed(2)}` : 'Class Default'}</span>
                </div>
                <div>
                  <span style={{ color: 'var(--text-muted)', display: 'block', fontSize: '0.8rem' }}>Admission Date</span>
                  <span>{new Date(studentDetails.admissionDate).toLocaleDateString()}</span>
                </div>
              </div>
              {!studentDetails.isActive && (
                <div style={{ background: 'rgba(239,68,68,0.05)', border: '1px solid rgba(239,68,68,0.2)', padding: '12px', borderRadius: '8px', marginTop: '8px' }}>
                  <span style={{ color: '#ef4444', fontWeight: 700, display: 'block', fontSize: '0.8rem' }}>Withdrawal Record</span>
                  <p style={{ margin: '4px 0 0 0', fontSize: '0.85rem' }}>
                    Reason: <strong>{studentDetails.withdrawalReason}</strong> <br />
                    Notes: {studentDetails.withdrawalNotes || 'None'}
                  </p>
                </div>
              )}
            </div>
          )}

          {/* Tab 2: Fees Ledger */}
          {detailTab === 'fees' && (
            <div>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
                <span style={{ fontWeight: 700, fontSize: '0.9rem' }}>Billed Ledgers</span>
                <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
                  Total Dues: <strong style={{ color: 'var(--error)' }}>
                    ₹{studentDetails.studentFees?.reduce((sum, f) => {
                      if (f.isDefaulted) return sum;
                      const paid = f.payments.reduce((s, p) => s + parseFloat(p.amount), 0);
                      return sum + (parseFloat(f.amount) - parseFloat(f.discount) - paid);
                    }, 0).toFixed(2)}
                  </strong>
                </span>
              </div>

              <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', maxHeight: '400px', overflowY: 'auto', paddingRight: '4px' }}>
                {studentDetails.studentFees?.map(fee => {
                  const paid = fee.payments?.reduce((sum, p) => sum + parseFloat(p.amount), 0) || 0;
                  const balance = parseFloat(fee.amount) - parseFloat(fee.discount) - paid;
                  return (
                    <div 
                      key={fee.id} 
                      style={{ 
                        border: '1px solid var(--border-color)', 
                        padding: '12px', 
                        borderRadius: '8px', 
                        background: 'rgba(255,255,255,0.01)',
                        display: 'flex',
                        flexDirection: 'column',
                        gap: '8px'
                      }}
                    >
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                        <div>
                          <span style={{ fontWeight: 700, color: 'var(--text-primary)' }}>{fee.description}</span>
                          <span style={{ display: 'block', fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                            Due: {new Date(fee.dueDate).toLocaleDateString()}
                          </span>
                        </div>
                        <span style={{
                          padding: '2px 8px',
                          borderRadius: '8px',
                          fontSize: '0.75rem',
                          fontWeight: 700,
                          background: fee.isDefaulted ? 'rgba(107,114,128,0.1)' : balance <= 0 ? 'rgba(16,185,129,0.1)' : paid > 0 ? 'rgba(245,158,11,0.1)' : 'rgba(239,68,68,0.1)',
                          color: fee.isDefaulted ? '#6b7280' : balance <= 0 ? '#10b981' : paid > 0 ? '#f59e0b' : '#ef4444',
                        }}>
                          {fee.isDefaulted ? 'DEFAULTED' : balance <= 0 ? 'PAID' : paid > 0 ? 'PARTIAL' : 'UNPAID'}
                        </span>
                      </div>
                      
                      <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.8rem', color: 'var(--text-secondary)' }}>
                        <span>Billed: ₹{parseFloat(fee.amount).toFixed(0)}</span>
                        <span>Paid: ₹{paid.toFixed(0)}</span>
                        <span style={{ fontWeight: 700, color: fee.isDefaulted ? '#6b7280' : balance > 0 ? 'var(--error)' : 'var(--text-primary)' }}>
                          Bal: {fee.isDefaulted ? '₹0 (Defaulted)' : `₹${balance.toFixed(0)}`}
                        </span>
                      </div>

                      {balance > 0 && !fee.isDefaulted && ['MASTER', 'ADMIN', 'ACCOUNTANT'].includes(currentUser.role) && (
                        <div style={{ display: 'flex', gap: '8px', justifyContent: 'flex-end', marginTop: '4px' }}>
                          <button
                            className="btn btn-secondary"
                            onClick={() => {
                              setPaymentFeeId(fee.id);
                              setPaymentFeeDesc(fee.description);
                              setPaymentAmount(balance);
                              setPaymentMax(balance);
                              setShowPaymentModal(true);
                            }}
                            style={{ padding: '4px 10px', fontSize: '0.75rem' }}
                          >
                            <DollarSign size={12} /> Pay Fee
                          </button>
                          <button
                            className="btn btn-danger"
                            onClick={() => handleMarkFeeDefaulted(fee.id)}
                            style={{ padding: '4px 10px', fontSize: '0.75rem', background: 'rgba(239,68,68,0.1)', color: '#ef4444', border: '1px solid rgba(239,68,68,0.2)' }}
                          >
                            Mark Default
                          </button>
                        </div>
                      )}
                    </div>
                  );
                })}
                {studentDetails.studentFees?.length === 0 && (
                  <p style={{ color: 'var(--text-muted)', fontStyle: 'italic', textAlign: 'center', fontSize: '0.85rem' }}>No bills generated yet.</p>
                )}
              </div>
            </div>
          )}

          {/* Tab 3: Purchases Ledger */}
          {detailTab === 'purchases' && (
            <div>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
                <span style={{ fontWeight: 700, fontSize: '0.9rem' }}>Purchased Items</span>
                {['MASTER', 'ADMIN', 'ACCOUNTANT'].includes(currentUser.role) && (
                  <button 
                    className="btn btn-secondary"
                    onClick={() => setShowPurchaseModal(true)}
                    style={{ padding: '6px 12px', fontSize: '0.75rem', gap: '4px' }}
                  >
                    <ShoppingBag size={12} /> Sell Item
                  </button>
                )}
              </div>

              <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', maxHeight: '400px', overflowY: 'auto' }}>
                {studentDetails.purchases?.map(p => (
                  <div 
                    key={p.id} 
                    style={{ 
                      border: '1px solid var(--border-color)', 
                      padding: '12px', 
                      borderRadius: '8px', 
                      background: 'rgba(255,255,255,0.01)'
                    }}
                  >
                    <span style={{ fontWeight: 700, color: 'var(--text-primary)' }}>{p.itemVariant?.itemCategory?.name} - {p.itemVariant?.label}</span>
                    <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.8rem', color: 'var(--text-secondary)', marginTop: '6px' }}>
                      <span>Qty: {p.quantity}</span>
                      <span>Total: ₹{(parseFloat(p.itemVariant?.price) * p.quantity).toFixed(2)}</span>
                      <span>{new Date(p.purchasedAt).toLocaleDateString()}</span>
                    </div>
                  </div>
                ))}
                {studentDetails.purchases?.length === 0 && (
                  <p style={{ color: 'var(--text-muted)', fontStyle: 'italic', textAlign: 'center', fontSize: '0.85rem' }}>No inventory purchases recorded.</p>
                )}
              </div>
            </div>
          )}

        </div>
      )}

      {/* CREATE STUDENT MODAL */}
      {showCreateModal && (
        <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, background: 'rgba(0,0,0,0.6)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000 }}>
          <div className="glass-card" style={{ width: '500px', padding: '32px' }}>
            <h2 style={{ fontFamily: 'Outfit, sans-serif', fontSize: '1.5rem', fontWeight: 700, marginBottom: '24px' }}>Add New Student</h2>
            <form onSubmit={handleCreateStudent}>
              <div className="grid-2">
                <div className="input-group">
                  <label className="input-label">First Name</label>
                  <input type="text" className="input-field" value={firstName} onChange={e => setFirstName(e.target.value)} required />
                </div>
                <div className="input-group">
                  <label className="input-label">Last Name</label>
                  <input type="text" className="input-field" value={lastName} onChange={e => setLastName(e.target.value)} required />
                </div>
              </div>

              <div className="grid-2">
                <div className="input-group">
                  <label className="input-label">Admission Number (Blank for auto)</label>
                  <input type="text" className="input-field" placeholder="e.g. ADM202601" value={admNo} onChange={e => setAdmNo(e.target.value)} />
                </div>
                <div className="input-group">
                  <label className="input-label">Roll Number</label>
                  <input type="number" className="input-field" placeholder="e.g. 15" value={createRollNo} onChange={e => setCreateRollNo(e.target.value)} />
                </div>
              </div>

              <div className="grid-2">
                <div className="input-group">
                  <label className="input-label">Select Class</label>
                  <select className="input-field select-field" value={createClassId} onChange={e => setCreateClassId(e.target.value)} required>
                    <option value="">-- Choose Class --</option>
                    {classes.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
                  </select>
                </div>
                <div className="input-group">
                  <label className="input-label">Select Section</label>
                  <select className="input-field select-field" value={createSectionId} onChange={e => setCreateSectionId(e.target.value)} required disabled={!createClassId}>
                    <option value="">-- Choose Section --</option>
                    {createSections.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
                  </select>
                </div>
              </div>

              <div className="grid-2">
                <div className="input-group">
                  <label className="input-label">Guardian Name</label>
                  <input type="text" className="input-field" value={guardianName} onChange={e => setGuardianName(e.target.value)} />
                </div>
                <div className="input-group">
                  <label className="input-label">Guardian Phone</label>
                  <input type="text" className="input-field" value={guardianPhone} onChange={e => setGuardianPhone(e.target.value)} />
                </div>
              </div>

              <div className="grid-2">
                <div className="input-group">
                  <label className="input-label">WhatsApp Contact</label>
                  <input type="text" className="input-field" value={whatsappPhone} onChange={e => setWhatsappPhone(e.target.value)} />
                </div>
                <div className="input-group">
                  <label className="input-label">Custom Tuition Fee Override (Optional)</label>
                  <input type="number" className="input-field" placeholder="e.g. 1800" value={tuitionFee} onChange={e => setTuitionFee(e.target.value)} />
                </div>
              </div>

              <div className="input-group">
                <label className="input-label">Address</label>
                <input type="text" className="input-field" value={address} onChange={e => setAddress(e.target.value)} />
              </div>

              {error && <div style={{ color: 'var(--error)', marginBottom: '16px', fontSize: '0.9rem' }}>{error}</div>}

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px', marginTop: '24px' }}>
                <button type="button" className="btn btn-secondary" onClick={() => setShowCreateModal(false)}>Cancel</button>
                <button type="submit" className="btn btn-primary" disabled={actionLoading}>
                  {actionLoading ? 'Saving...' : 'Create Student'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* PROMOTE / MIGRATE MODAL */}
      {showPromoteModal && (
        <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, background: 'rgba(0,0,0,0.6)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000 }}>
          <div className="glass-card" style={{ width: '450px', padding: '32px' }}>
            <h2 style={{ fontFamily: 'Outfit, sans-serif', fontSize: '1.5rem', fontWeight: 700, marginBottom: '12px', display: 'flex', alignItems: 'center', gap: '8px' }}>
              <ArrowUpCircle size={24} style={{ color: 'var(--accent-primary)' }} />
              Promote / Migrate Student
            </h2>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', marginBottom: '24px' }}>
              Reassign the student to another academic year and class. Pre-existing billed ledger balances remain unaffected.
            </p>

            <form onSubmit={handlePromoteStudent}>
              <div className="input-group">
                <label className="input-label">Target Academic Year</label>
                <select 
                  className="input-field select-field" 
                  value={promoteYearId} 
                  onChange={e => setPromoteYearId(e.target.value)} 
                  required
                >
                  <option value="">-- Choose Target Year --</option>
                  {academicYears.map(y => <option key={y.id} value={y.id}>{y.label}</option>)}
                </select>
              </div>

              <div className="input-group">
                <label className="input-label">Target Class</label>
                <select 
                  className="input-field select-field" 
                  value={promoteClassId} 
                  onChange={e => setPromoteClassId(e.target.value)} 
                  required
                  disabled={!promoteYearId}
                >
                  <option value="">-- Choose Class --</option>
                  {promoteClasses.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
                </select>
              </div>

              <div className="input-group">
                <label className="input-label">Target Section</label>
                <select 
                  className="input-field select-field" 
                  value={promoteSectionId} 
                  onChange={e => setPromoteSectionId(e.target.value)} 
                  required
                  disabled={!promoteClassId}
                >
                  <option value="">-- Choose Section --</option>
                  {promoteSections.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
                </select>
              </div>

              <div className="grid-2">
                <div className="input-group">
                  <label className="input-label">New Roll Number</label>
                  <input 
                    type="number" 
                    className="input-field" 
                    placeholder="e.g. 15" 
                    value={promoteRollNo} 
                    onChange={e => setPromoteRollNo(e.target.value)} 
                  />
                </div>
                <div className="input-group">
                  <label className="input-label">Tuition Fee Override (Optional)</label>
                  <input 
                    type="number" 
                    className="input-field" 
                    placeholder="Class Default" 
                    value={promoteFeeOverride} 
                    onChange={e => setPromoteFeeOverride(e.target.value)} 
                  />
                </div>
              </div>

              {error && <div style={{ color: 'var(--error)', marginBottom: '16px', fontSize: '0.9rem' }}>{error}</div>}

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px', marginTop: '24px' }}>
                <button type="button" className="btn btn-secondary" onClick={() => setShowPromoteModal(false)}>Cancel</button>
                <button type="submit" className="btn btn-primary" disabled={actionLoading || !promoteSectionId}>
                  {actionLoading ? 'Migrating...' : 'Migrate Register'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* WITHDRAW MODAL */}
      {showWithdrawModal && (
        <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, background: 'rgba(0,0,0,0.6)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000 }}>
          <div className="glass-card" style={{ width: '450px', padding: '32px' }}>
            <h2 style={{ fontFamily: 'Outfit, sans-serif', fontSize: '1.5rem', fontWeight: 700, marginBottom: '12px', color: 'var(--error)' }}>
              Mark Student as Withdrawn
            </h2>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', marginBottom: '24px' }}>
              This flags the student register record as inactive and records a leaving reason.
            </p>

            <form onSubmit={handleWithdrawStudent}>
              <div className="input-group">
                <label className="input-label">Withdrawal Reason</label>
                <select 
                  className="input-field select-field" 
                  value={withdrawReason} 
                  onChange={e => setWithdrawReason(e.target.value)} 
                  required
                >
                  <option value="TRANSFERRED">Transferred to another school</option>
                  <option value="GRADUATED">Graduated</option>
                  <option value="EXPELLED">Expelled</option>
                  <option value="OTHER">Other / Dropped Out</option>
                </select>
              </div>

              <div className="input-group">
                <label className="input-label">Notes / Remarks</label>
                <input 
                  type="text" 
                  className="input-field" 
                  placeholder="e.g. Moved to another city" 
                  value={withdrawNotes} 
                  onChange={e => setWithdrawNotes(e.target.value)} 
                />
              </div>

              {error && <div style={{ color: 'var(--error)', marginBottom: '16px', fontSize: '0.9rem' }}>{error}</div>}

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px', marginTop: '24px' }}>
                <button type="button" className="btn btn-secondary" onClick={() => setShowWithdrawModal(false)}>Cancel</button>
                <button type="submit" className="btn btn-primary" style={{ background: 'var(--error)' }} disabled={actionLoading}>
                  {actionLoading ? 'Saving...' : 'Confirm Withdrawal'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* RECORD PAYMENT MODAL */}
      {showPaymentModal && (
        <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, background: 'rgba(0,0,0,0.6)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000 }}>
          <div className="glass-card" style={{ width: '450px', padding: '32px' }}>
            <h2 style={{ fontFamily: 'Outfit, sans-serif', fontSize: '1.4rem', fontWeight: 700, marginBottom: '8px' }}>
              Record Payment
            </h2>
            <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)', display: 'block', marginBottom: '24px' }}>
              Billed Ledger: <strong>{paymentFeeDesc}</strong> (Max Payable: ₹{paymentMax.toFixed(2)})
            </span>

            <form onSubmit={handleRecordPayment}>
              <div className="grid-2">
                <div className="input-group">
                  <label className="input-label">Amount Paid (₹)</label>
                  <input 
                    type="number" 
                    step="0.01"
                    className="input-field" 
                    max={paymentMax}
                    value={paymentAmount} 
                    onChange={e => setPaymentAmount(e.target.value)} 
                    required 
                  />
                </div>
                <div className="input-group">
                  <label className="input-label">Payment Mode</label>
                  <select 
                    className="input-field select-field" 
                    value={paymentMode} 
                    onChange={e => setPaymentMode(e.target.value)} 
                    required
                  >
                    <option value="CASH">Cash</option>
                    <option value="UPI">UPI</option>
                    <option value="CHEQUE">Cheque</option>
                    <option value="BANK_TRANSFER">Bank Transfer</option>
                    <option value="CARD">Card</option>
                    <option value="OTHER">Other</option>
                  </select>
                </div>
              </div>

              <div className="input-group">
                <label className="input-label">Reference Number (Txn ID, Cheque No, etc.)</label>
                <input 
                  type="text" 
                  className="input-field" 
                  value={paymentRef} 
                  onChange={e => setPaymentRef(e.target.value)} 
                />
              </div>

              <div className="input-group">
                <label className="input-label">Remarks</label>
                <input 
                  type="text" 
                  className="input-field" 
                  value={paymentNotes} 
                  onChange={e => setPaymentNotes(e.target.value)} 
                />
              </div>

              {error && <div style={{ color: 'var(--error)', marginBottom: '16px', fontSize: '0.9rem' }}>{error}</div>}

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px', marginTop: '24px' }}>
                <button type="button" className="btn btn-secondary" onClick={() => setShowPaymentModal(false)}>Cancel</button>
                <button type="submit" className="btn btn-primary" disabled={actionLoading}>
                  {actionLoading ? 'Recording...' : 'Record Payment'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* SELL ITEM MODAL */}
      {showPurchaseModal && (
        <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, background: 'rgba(0,0,0,0.6)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000 }}>
          <div className="glass-card" style={{ width: '450px', padding: '32px' }}>
            <h2 style={{ fontFamily: 'Outfit, sans-serif', fontSize: '1.4rem', fontWeight: 700, marginBottom: '24px' }}>
              Sell Inventory Item to Student
            </h2>

            <form onSubmit={handleSellItem}>
              <div className="input-group">
                <label className="input-label">Select Item Variant (Stock Available)</label>
                <select 
                  className="input-field select-field" 
                  value={sellVariantId} 
                  onChange={e => setSellVariantId(e.target.value)} 
                  required
                >
                  <option value="">-- Choose Item --</option>
                  {inventoryVariants.map(v => (
                    <option key={v.id} value={v.id}>
                      {v.itemCategory?.name} - {v.label} (Price: ₹{parseFloat(v.price).toFixed(2)}, Stock: {v.stockQuantity})
                    </option>
                  ))}
                </select>
              </div>

              <div className="grid-2">
                <div className="input-group">
                  <label className="input-label">Quantity</label>
                  <input 
                    type="number" 
                    min="1"
                    className="input-field" 
                    value={sellQty} 
                    onChange={e => setSellQty(e.target.value)} 
                    required 
                  />
                </div>
                <div className="input-group">
                  <label className="input-label">Due Date for Bill</label>
                  <input 
                    type="date" 
                    className="input-field" 
                    value={sellDueDate} 
                    onChange={e => setSellDueDate(e.target.value)} 
                    required 
                  />
                </div>
              </div>

              {error && <div style={{ color: 'var(--error)', marginBottom: '16px', fontSize: '0.9rem' }}>{error}</div>}

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px', marginTop: '24px' }}>
                <button type="button" className="btn btn-secondary" onClick={() => setShowPurchaseModal(false)}>Cancel</button>
                <button type="submit" className="btn btn-primary" disabled={actionLoading || !sellVariantId}>
                  {actionLoading ? 'Recording sale...' : 'Confirm Sale'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

    </div>
  );
}
