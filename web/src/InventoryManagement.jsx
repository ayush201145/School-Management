import React, { useState, useEffect } from 'react';
import api from './api';
import { Package, Plus, TrendingUp, DollarSign, Edit } from 'lucide-react';

export default function InventoryManagement() {
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  
  // Selected variant for edit dialog
  const [selectedVariant, setSelectedVariant] = useState(null);
  const [sellingPrice, setSellingPrice] = useState('');
  const [costPrice, setCostPrice] = useState('');
  const [restockQty, setRestockQty] = useState('');
  const [editError, setEditError] = useState('');
  const [filterType, setFilterType] = useState('ALL');

  useEffect(() => {
    fetchInventory();
  }, []);

  const fetchInventory = async () => {
    setLoading(true);
    try {
      const res = await api.get('/api/item-categories');
      setCategories(res.data);
    } catch (err) {
      setError('Failed to fetch inventory categories.');
    } finally {
      setLoading(false);
    }
  };

  const handleUpdateVariant = async (e) => {
    e.preventDefault();
    if (!selectedVariant) return;
    setEditError('');

    const newPrice = parseFloat(sellingPrice);
    const newCostPrice = costPrice.trim() !== '' ? parseFloat(costPrice) : null;

    if (isNaN(newPrice) || newPrice <= 0) {
      setEditError('Invalid selling price');
      return;
    }
    if (newCostPrice !== null && (isNaN(newCostPrice) || newCostPrice < 0)) {
      setEditError('Invalid cost price');
      return;
    }

    try {
      // 1. Update variant price and cost price
      await api.patch(`/api/item-variants/${selectedVariant.id}`, {
        price: newPrice,
        costPrice: newCostPrice,
      });

      // 2. If restock quantity entered, run restock call
      const qty = parseInt(restockQty);
      if (!isNaN(qty) && qty > 0) {
        await api.post(`/api/item-variants/${selectedVariant.id}/restock`, {
          quantity: qty,
          note: 'Restocked from Web Portal UI',
        });
      }

      setSelectedVariant(null);
      resetForm();
      fetchInventory();
    } catch (err) {
      setEditError(err.response?.data?.error || 'Failed to update variant details.');
    }
  };

  const openEditModal = (variant) => {
    setSelectedVariant(variant);
    setSellingPrice(variant.price.toString());
    setCostPrice(variant.costPrice !== null && variant.costPrice !== undefined ? variant.costPrice.toString() : '');
    setRestockQty('');
    setEditError('');
  };

  const resetForm = () => {
    setSellingPrice('');
    setCostPrice('');
    setRestockQty('');
    setEditError('');
  };

  return (
    <div>
      <div className="page-header">
        <div>
          <h1 className="page-title">Inventory & Stock</h1>
          <p className="page-subtitle">Configure book sets, uniform sizes, set purchase margins, and adjust stock quantities</p>
        </div>
      </div>

      {loading && <p style={{ color: 'var(--text-secondary)' }}>Loading inventory metadata...</p>}
      {error && <p style={{ color: 'var(--error)' }}>{error}</p>}

      {!loading && !error && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
          <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap', borderBottom: '1px solid var(--border-color)', paddingBottom: '16px' }}>
            {[
              { label: 'All Inventory', type: 'ALL' },
              { label: 'Books', type: 'BOOK' },
              { label: 'Summer Uniforms', type: 'UNIFORM_SUMMER' },
              { label: 'Winter Uniforms', type: 'UNIFORM_WINTER' },
              { label: 'Others / Miscellaneous', type: 'OTHER' },
            ].map(btn => (
              <button
                key={btn.type}
                className={`btn ${filterType === btn.type ? 'btn-primary' : 'btn-secondary'}`}
                onClick={() => setFilterType(btn.type)}
                style={{ 
                  borderRadius: '20px', 
                  padding: '6px 16px', 
                  fontSize: '0.85rem',
                  fontWeight: 600,
                }}
              >
                {btn.label}
              </button>
            ))}
          </div>

          {categories
            .filter(cat => filterType === 'ALL' || cat.type === filterType)
            .map((category) => (
              <div key={category.id} className="glass-card" style={{ animation: 'fadeIn 0.3s ease-out', marginTop: 0 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '20px', borderBottom: '1px solid var(--border-color)', paddingBottom: '12px' }}>
                <Package size={24} style={{ color: 'var(--accent-primary)' }} />
                <div>
                  <h3 style={{ fontFamily: 'Outfit, sans-serif', fontSize: '1.25rem', fontWeight: 700 }}>{category.name}</h3>
                  <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>Category Type: {category.type}</p>
                </div>
              </div>

              {category.variants && category.variants.length > 0 ? (
                <div className="table-container">
                  <table className="custom-table">
                    <thead>
                      <tr>
                        <th>Variant / Size / Set</th>
                        <th>Selling Price (₹)</th>
                        <th>Cost Price (₹)</th>
                        <th>Markup Margin</th>
                        <th>Stock Available</th>
                        <th>Actions</th>
                      </tr>
                    </thead>
                    <tbody>
                      {category.variants.map((v) => {
                        const sp = parseFloat(v.price) || 0;
                        const cp = parseFloat(v.costPrice) || 0;
                        const margin = cp > 0 ? ((sp - cp) / cp) * 100 : null;
                        const isLow = v.stockQuantity < 10;

                        return (
                          <tr key={v.id}>
                            <td style={{ fontWeight: 600 }}>{v.label}</td>
                            <td>₹{sp.toFixed(2)}</td>
                            <td>{v.costPrice !== null && v.costPrice !== undefined ? `₹${cp.toFixed(2)}` : <span style={{ color: 'var(--text-muted)' }}>Not Set</span>}</td>
                            <td>
                              {margin !== null ? (
                                <span style={{ color: margin >= 0 ? 'var(--success)' : 'var(--error)', fontWeight: 600 }}>
                                  {margin.toFixed(0)}%
                                </span>
                              ) : (
                                <span style={{ color: 'var(--text-muted)' }}>—</span>
                              )}
                            </td>
                            <td>
                              <span style={{ fontWeight: 700, color: isLow ? 'var(--error)' : 'var(--text-primary)' }}>
                                {v.stockQuantity} units
                              </span>
                              {isLow && <span className="badge badge-error" style={{ marginLeft: '8px', padding: '2px 6px', fontSize: '0.65rem' }}>Low Stock</span>}
                            </td>
                            <td>
                              <button className="btn btn-secondary" style={{ padding: '6px 12px' }} onClick={() => openEditModal(v)}>
                                <Edit size={14} style={{ marginRight: '4px' }} /> Edit / Restock
                              </button>
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              ) : (
                <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem', fontStyle: 'italic' }}>No variants added to this category yet.</p>
              )}
            </div>
          ))}
        </div>
      )}

      {/* EDIT & RESTOCK MODAL */}
      {selectedVariant && (
        <div className="modal-overlay">
          <div className="modal-content glass-card" style={{ width: '480px' }}>
            <h3 style={{ fontFamily: 'Outfit, sans-serif', fontSize: '1.25rem', marginBottom: '24px' }}>Edit Variant: {selectedVariant.label}</h3>
            
            {editError && (
              <div style={{ background: 'var(--error-bg)', color: '#fca5a5', padding: '10px 16px', borderRadius: '8px', marginBottom: '16px', fontSize: '0.85rem' }}>
                {editError}
              </div>
            )}

            <form onSubmit={handleUpdateVariant}>
              <div className="input-group">
                <label className="input-label">Selling Price (₹)</label>
                <input
                  type="number"
                  step="0.01"
                  className="input-field"
                  value={sellingPrice}
                  onChange={(e) => setSellingPrice(e.target.value)}
                  placeholder="e.g. 500"
                  required
                />
              </div>

              <div className="input-group">
                <label className="input-label">Cost Price (₹)</label>
                <input
                  type="number"
                  step="0.01"
                  className="input-field"
                  value={costPrice}
                  onChange={(e) => setCostPrice(e.target.value)}
                  placeholder="e.g. 350 (Leave blank if unknown)"
                />
              </div>

              <div className="input-group" style={{ borderTop: '1px solid var(--border-color)', paddingTop: '16px', marginTop: '16px' }}>
                <label className="input-label">Add Stock (Restock Quantity)</label>
                <input
                  type="number"
                  className="input-field"
                  value={restockQty}
                  onChange={(e) => setRestockQty(e.target.value)}
                  placeholder="Enter units to add, e.g. 50 (Optional)"
                />
              </div>

              <div style={{ display: 'flex', gap: '12px', marginTop: '28px', justifyContent: 'flex-end' }}>
                <button type="button" className="btn btn-secondary" onClick={() => setSelectedVariant(null)}>Cancel</button>
                <button type="submit" className="btn btn-primary">Save Changes</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
