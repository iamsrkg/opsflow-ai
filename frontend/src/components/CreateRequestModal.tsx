import React, { useState } from 'react';
import { ApiService } from '../services/api';
import { X, Plus, Sparkles } from 'lucide-react';

interface CreateRequestModalProps {
  onClose: () => void;
  onRefresh: () => void;
}

export const CreateRequestModal: React.FC<CreateRequestModalProps> = ({ onClose, onRefresh }) => {
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [category, setCategory] = useState('PAYMENT');
  const [priority, setPriority] = useState('HIGH');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!title || !description) return;

    setLoading(true);
    try {
      await ApiService.createRequest(title, description, category, priority);
      onRefresh();
      onClose();
    } catch {
      // the reason is shown in the banner at the top of the page
    } finally {
      setLoading(false);
    }
  };

  const setSample = (sampleTitle: string, sampleDesc: string, sampleCat: string) => {
    setTitle(sampleTitle);
    setDescription(sampleDesc);
    setCategory(sampleCat);
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-card" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h2 style={{ fontSize: '1.1rem', fontWeight: 700 }}>Submit Operational Request</h2>
          <button className="btn-secondary" style={{ padding: '0.4rem' }} onClick={onClose}>
            <X size={18} />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="modal-body">
          <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap', marginBottom: '0.5rem' }}>
            <span style={{ fontSize: '0.78rem', color: 'var(--text-muted)', display: 'flex', alignItems: 'center', gap: '0.25rem' }}>
              <Sparkles size={12} color="#fbbf24" /> Quick Presets:
            </span>
            <button
              type="button"
              className="btn-secondary"
              style={{ fontSize: '0.72rem', padding: '0.2rem 0.5rem' }}
              onClick={() => setSample('Charged ₹500 but order payment pending.', 'Customer money deducted at 10:31 AM, order status remains PENDING.', 'PAYMENT')}
            >
              Payment Sync Issue
            </button>
            <button
              type="button"
              className="btn-secondary"
              style={{ fontSize: '0.72rem', padding: '0.2rem 0.5rem' }}
              onClick={() => setSample('Flight booking timeout after credit debit.', 'Booking SQ-321 failed with timeout, wallet debited.', 'BOOKING')}
            >
              Booking Timeout
            </button>
          </div>

          <div>
            <label style={{ fontSize: '0.82rem', fontWeight: 600, color: 'var(--text-muted)', display: 'block', marginBottom: '0.35rem' }}>Title</label>
            <input
              type="text"
              required
              className="search-box"
              style={{ width: '100%' }}
              placeholder="e.g. Charged but order status pending..."
              value={title}
              onChange={(e) => setTitle(e.target.value)}
            />
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
            <div>
              <label style={{ fontSize: '0.82rem', fontWeight: 600, color: 'var(--text-muted)', display: 'block', marginBottom: '0.35rem' }}>Category</label>
              <select
                className="search-box"
                style={{ width: '100%' }}
                value={category}
                onChange={(e) => setCategory(e.target.value)}
              >
                <option value="PAYMENT">PAYMENT</option>
                <option value="BOOKING">BOOKING</option>
                <option value="DELIVERY">DELIVERY</option>
                <option value="ORDER">ORDER</option>
                <option value="TECHNICAL">TECHNICAL</option>
              </select>
            </div>

            <div>
              <label style={{ fontSize: '0.82rem', fontWeight: 600, color: 'var(--text-muted)', display: 'block', marginBottom: '0.35rem' }}>Priority</label>
              <select
                className="search-box"
                style={{ width: '100%' }}
                value={priority}
                onChange={(e) => setPriority(e.target.value)}
              >
                <option value="LOW">LOW</option>
                <option value="MEDIUM">MEDIUM</option>
                <option value="HIGH">HIGH</option>
                <option value="CRITICAL">CRITICAL</option>
              </select>
            </div>
          </div>

          <div>
            <label style={{ fontSize: '0.82rem', fontWeight: 600, color: 'var(--text-muted)', display: 'block', marginBottom: '0.35rem' }}>Description & Telemetry</label>
            <textarea
              required
              rows={4}
              className="search-box"
              style={{ width: '100%', resize: 'none' }}
              placeholder="Provide exact error, transaction IDs, or customer logs..."
              value={description}
              onChange={(e) => setDescription(e.target.value)}
            />
          </div>

          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '0.5rem' }}>
            <button type="button" className="btn-secondary" onClick={onClose}>
              Cancel
            </button>
            <button type="submit" className="btn-primary" disabled={loading}>
              <Plus size={16} />
              <span>{loading ? 'Submitting...' : 'Create Request'}</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
