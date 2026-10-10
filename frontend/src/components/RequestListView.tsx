import React, { useState } from 'react';
import { RequestResponseDTO, Category } from '../services/types';
import { Search, Filter, Bot, ArrowRight } from 'lucide-react';

interface RequestListViewProps {
  requests: RequestResponseDTO[];
  onSelectRequest: (req: RequestResponseDTO) => void;
}

export const RequestListView: React.FC<RequestListViewProps> = ({ requests, onSelectRequest }) => {
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCategory, setSelectedCategory] = useState<string>('ALL');

  const filteredRequests = requests.filter((req) => {
    const matchesSearch =
      req.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
      req.externalReference.toLowerCase().includes(searchQuery.toLowerCase()) ||
      req.description.toLowerCase().includes(searchQuery.toLowerCase());
    const matchesCategory = selectedCategory === 'ALL' || req.category === selectedCategory;
    return matchesSearch && matchesCategory;
  });

  const categories: (Category | 'ALL')[] = ['ALL', 'PAYMENT', 'BOOKING', 'DELIVERY', 'ORDER', 'TECHNICAL'];

  return (
    <div className="content-body">
      <div className="table-container">
        <div className="table-header-toolbar">
          <div className="search-box">
            <Search size={16} color="#9ca3af" />
            <input
              type="text"
              placeholder="Search requests, ref ID or description..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
          </div>

          <div style={{ display: 'flex', gap: '0.5rem' }}>
            {categories.map((cat) => (
              <button
                key={cat}
                className={`btn-secondary ${selectedCategory === cat ? 'active' : ''}`}
                style={{
                  fontSize: '0.78rem',
                  padding: '0.35rem 0.75rem',
                  background: selectedCategory === cat ? 'var(--primary-glow)' : undefined,
                  borderColor: selectedCategory === cat ? 'var(--primary)' : undefined,
                }}
                onClick={() => setSelectedCategory(cat)}
              >
                {cat}
              </button>
            ))}
          </div>
        </div>

        <table className="table-element">
          <thead>
            <tr>
              <th>Ref ID</th>
              <th>Request Title</th>
              <th>Category</th>
              <th>Priority</th>
              <th>Status</th>
              <th>Assigned To</th>
              <th>Confidence</th>
              <th>Action</th>
            </tr>
          </thead>
          <tbody>
            {filteredRequests.map((req) => (
              <tr key={req.id}>
                <td style={{ fontFamily: 'var(--font-mono)', fontWeight: 600, color: '#a5b4fc' }}>
                  {req.externalReference}
                </td>
                <td style={{ fontWeight: 500, maxWidth: '280px' }}>
                  <div>{req.title}</div>
                  {req.classificationSummary && (
                    <div style={{ fontSize: '0.75rem', color: '#9ca3af', marginTop: '2px' }}>
                      {req.classificationSummary}
                    </div>
                  )}
                </td>
                <td>
                  <span className={`badge badge-${req.category.toLowerCase()}`}>
                    {req.category}
                  </span>
                </td>
                <td>
                  <span className={`badge badge-${req.priority.toLowerCase()}`}>
                    {req.priority}
                  </span>
                </td>
                <td>
                  <span className="badge badge-status">
                    {req.status.replace('_', ' ')}
                  </span>
                </td>
                <td style={{ color: 'var(--text-muted)' }}>{req.assignedTo || 'Unassigned'}</td>
                <td style={{ fontWeight: 600, color: '#c084fc' }}>
                  {req.confidenceScore ? `${Math.round(req.confidenceScore * 100)}%` : 'N/A'}
                </td>
                <td>
                  <button className="btn-primary" style={{ padding: '0.35rem 0.75rem', fontSize: '0.78rem' }} onClick={() => onSelectRequest(req)}>
                    <Bot size={14} />
                    <span>Investigate</span>
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};
