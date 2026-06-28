import React, { useState, useEffect } from 'react';
import api from './api';
import { UserPlus, Edit, Trash2, ShieldAlert, CheckCircle, XCircle } from 'lucide-react';

export default function UserManagement({ currentUser }) {
  const [users, setUsers] = useState([]);
  const [teachers, setTeachers] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  
  // Modal states
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [selectedUser, setSelectedUser] = useState(null);

  // Form states
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [role, setRole] = useState('TEACHER');
  const [teacherId, setTeacherId] = useState('');
  const [isActive, setIsActive] = useState(true);

  useEffect(() => {
    fetchUsers();
    fetchTeachers();
  }, []);

  const fetchUsers = async () => {
    setLoading(true);
    try {
      const res = await api.get('/api/users');
      setUsers(res.data);
    } catch (err) {
      setError('Failed to fetch users.');
    } finally {
      setLoading(false);
    }
  };

  const fetchTeachers = async () => {
    try {
      const res = await api.get('/api/teachers');
      setTeachers(res.data);
    } catch (err) {
      console.error('Failed to fetch teachers list');
    }
  };

  const handleCreateUser = async (e) => {
    e.preventDefault();
    if (!username || !password || !role) return;

    try {
      await api.post('/api/users', {
        username,
        password,
        role,
        teacherId: role === 'TEACHER' ? teacherId || null : null,
      });
      setIsCreateModalOpen(false);
      resetForm();
      fetchUsers();
    } catch (err) {
      alert(err.response?.data?.error || 'Failed to create user.');
    }
  };

  const handleUpdateUser = async (e) => {
    e.preventDefault();
    if (!selectedUser) return;

    try {
      await api.patch(`/api/users/${selectedUser.id}`, {
        role,
        isActive,
        teacherId: role === 'TEACHER' ? teacherId || null : null,
        ...(password.trim() !== '' ? { password } : {}),
      });
      setIsEditModalOpen(false);
      resetForm();
      fetchUsers();
    } catch (err) {
      alert(err.response?.data?.error || 'Failed to update user.');
    }
  };

  const handleDeleteUser = async (id) => {
    if (id === currentUser.id) {
      alert('You cannot delete your own account.');
      return;
    }
    if (!window.confirm('Are you sure you want to delete this user?')) return;

    try {
      await api.delete(`/api/users/${id}`);
      fetchUsers();
    } catch (err) {
      alert(err.response?.data?.error || 'Failed to delete user.');
    }
  };

  const openEditModal = (user) => {
    setSelectedUser(user);
    setUsername(user.username);
    setRole(user.role);
    setTeacherId(user.teacherId || '');
    setIsActive(user.isActive);
    setPassword('');
    setIsEditModalOpen(true);
  };

  const resetForm = () => {
    setUsername('');
    setPassword('');
    setRole('TEACHER');
    setTeacherId('');
    setIsActive(true);
    setSelectedUser(null);
  };

  return (
    <div>
      <div className="page-header">
        <div>
          <h1 className="page-title">User Accounts</h1>
          <p className="page-subtitle">Manage login credentials and system access permissions</p>
        </div>
        <button className="btn btn-primary" onClick={() => { resetForm(); setIsCreateModalOpen(true); }}>
          <UserPlus size={18} />
          Add New User
        </button>
      </div>

      {loading && <p style={{ color: 'var(--text-secondary)' }}>Loading accounts...</p>}
      {error && <p style={{ color: 'var(--error)' }}>{error}</p>}

      {!loading && !error && (
        <div className="table-container">
          <table className="custom-table">
            <thead>
              <tr>
                <th>Username</th>
                <th>Role</th>
                <th>Status</th>
                <th>Linked Profile</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {users.map((user) => (
                <tr key={user.id}>
                  <td style={{ fontWeight: 600 }}>{user.username}</td>
                  <td>
                    <span className={`badge ${user.role === 'MASTER' ? 'badge-error' : user.role === 'ADMIN' ? 'badge-info' : 'badge-success'}`}>
                      {user.role}
                    </span>
                  </td>
                  <td>
                    {user.isActive ? (
                      <span style={{ display: 'inline-flex', alignItems: 'center', gap: '6px', color: 'var(--success)', fontSize: '0.85rem' }}>
                        <CheckCircle size={14} /> Active
                      </span>
                    ) : (
                      <span style={{ display: 'inline-flex', alignItems: 'center', gap: '6px', color: 'var(--text-muted)', fontSize: '0.85rem' }}>
                        <XCircle size={14} /> Suspended
                      </span>
                    )}
                  </td>
                  <td>
                    {user.teacher ? (
                      <span style={{ color: 'var(--text-secondary)' }}>
                        {user.teacher.firstName} {user.teacher.lastName} ({user.teacher.employeeNo})
                      </span>
                    ) : (
                      <span style={{ color: 'var(--text-muted)' }}>—</span>
                    )}
                  </td>
                  <td>
                    <div style={{ display: 'flex', gap: '8px' }}>
                      <button className="btn btn-secondary" style={{ padding: '6px 12px' }} onClick={() => openEditModal(user)}>
                        <Edit size={14} />
                      </button>
                      {user.id !== currentUser.id && (
                        <button className="btn btn-danger" style={{ padding: '6px 12px' }} onClick={() => handleDeleteUser(user.id)}>
                          <Trash2 size={14} />
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* CREATE USER MODAL */}
      {isCreateModalOpen && (
        <div className="modal-overlay">
          <div className="modal-content glass-card">
            <h3 style={{ fontFamily: 'Outfit, sans-serif', fontSize: '1.25rem', marginBottom: '24px' }}>Create User Account</h3>
            <form onSubmit={handleCreateUser}>
              <div className="input-group">
                <label className="input-label">Username</label>
                <input
                  type="text"
                  className="input-field"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  placeholder="e.g. jdoe"
                  required
                />
              </div>

              <div className="input-group">
                <label className="input-label">Password</label>
                <input
                  type="password"
                  className="input-field"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="••••••••"
                  required
                />
              </div>

              <div className="input-group">
                <label className="input-label">Role</label>
                <select className="input-field select-field" value={role} onChange={(e) => setRole(e.target.value)}>
                  <option value="TEACHER">TEACHER</option>
                  <option value="ACCOUNTANT">ACCOUNTANT</option>
                  <option value="ADMIN">ADMIN</option>
                  <option value="MASTER">MASTER</option>
                </select>
              </div>

              {role === 'TEACHER' && (
                <div className="input-group">
                  <label className="input-label">Link to Teacher Profile</label>
                  <select className="input-field select-field" value={teacherId} onChange={(e) => setTeacherId(e.target.value)}>
                    <option value="">-- No Link --</option>
                    {teachers.map((t) => (
                      <option key={t.id} value={t.id}>
                        {t.firstName} {t.lastName} ({t.employeeNo})
                      </option>
                    ))}
                  </select>
                </div>
              )}

              <div style={{ display: 'flex', gap: '12px', marginTop: '28px', justifyContent: 'flex-end' }}>
                <button type="button" className="btn btn-secondary" onClick={() => setIsCreateModalOpen(false)}>Cancel</button>
                <button type="submit" className="btn btn-primary">Create Account</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* EDIT USER MODAL */}
      {isEditModalOpen && selectedUser && (
        <div className="modal-overlay">
          <div className="modal-content glass-card">
            <h3 style={{ fontFamily: 'Outfit, sans-serif', fontSize: '1.25rem', marginBottom: '24px' }}>Edit User: {username}</h3>
            <form onSubmit={handleUpdateUser}>
              <div className="input-group">
                <label className="input-label">New Password (leave blank to keep current)</label>
                <input
                  type="password"
                  className="input-field"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="••••••••"
                />
              </div>

              <div className="input-group">
                <label className="input-label">Role</label>
                <select className="input-field select-field" value={role} onChange={(e) => setRole(e.target.value)}>
                  <option value="TEACHER">TEACHER</option>
                  <option value="ACCOUNTANT">ACCOUNTANT</option>
                  <option value="ADMIN">ADMIN</option>
                  <option value="MASTER">MASTER</option>
                </select>
              </div>

              {role === 'TEACHER' && (
                <div className="input-group">
                  <label className="input-label">Link to Teacher Profile</label>
                  <select className="input-field select-field" value={teacherId} onChange={(e) => setTeacherId(e.target.value)}>
                    <option value="">-- No Link --</option>
                    {teachers.map((t) => (
                      <option key={t.id} value={t.id}>
                        {t.firstName} {t.lastName} ({t.employeeNo})
                      </option>
                    ))}
                  </select>
                </div>
              )}

              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', margin: '16px 0' }}>
                <input
                  type="checkbox"
                  id="editIsActive"
                  checked={isActive}
                  onChange={(e) => setIsActive(e.target.checked)}
                  style={{ width: '18px', height: '18px', cursor: 'pointer' }}
                />
                <label htmlFor="editIsActive" style={{ cursor: 'pointer', fontSize: '0.95rem' }}>Account is Active</label>
              </div>

              <div style={{ display: 'flex', gap: '12px', marginTop: '28px', justifyContent: 'flex-end' }}>
                <button type="button" className="btn btn-secondary" onClick={() => setIsEditModalOpen(false)}>Cancel</button>
                <button type="submit" className="btn btn-primary">Save Changes</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
