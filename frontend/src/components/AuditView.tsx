import React, { useState, useEffect } from 'react';
import { AuditEvent } from '../services/types';
import { ApiService } from '../services/api';
import { FileText, Search, Activity } from 'lucide-react';

export const AuditView: React.FC = () => {
  const [events, setEvents] = useState<AuditEvent[]>([]);
  const [filterQuery, setFilterQuery] = useState('');

  useEffect(() => {
    ApiService.getAuditTrail().then(setEvents);
  }, []);

  const filtered = events.filter(
    (e) =>
      e.who.toLowerCase().includes(filterQuery.toLowerCase()) ||
      e.what.toLowerCase().includes(filterQuery.toLowerCase()) ||
      (e.correlationId && e.correlationId.toLowerCase().includes(filterQuery.toLowerCase()))
  );

  return (
    <div className="content-body">
      <div className="table-container">
        <div className="table-header-toolbar">
          <div style={{ fontWeight: 700, fontSize: '1rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <FileText size={18} color="#34d399" />
            <span>Operational System Audit Trail</span>
          </div>

          <div className="search-box">
            <Search size={16} color="#9ca3af" />
            <input
              type="text"
              placeholder="Filter by user, action or correlation ID..."
              value={filterQuery}
              onChange={(e) => setFilterQuery(e.target.value)}
            />
          </div>
        </div>

        <table className="table-element">
          <thead>
            <tr>
              <th>Timestamp</th>
              <th>Correlation ID</th>
              <th>Who</th>
              <th>Action / Event</th>
              <th>Reason & Summary</th>
              <th>Result</th>
            </tr>
          </thead>
          <tbody>
            {filtered.map((e) => (
              <tr key={e.id}>
                <td style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>
                  {new Date(e.createdAt).toLocaleTimeString()}
                </td>
                <td style={{ fontFamily: 'var(--font-mono)', fontSize: '0.78rem', color: '#c084fc' }}>
                  {e.correlationId || 'CORR-884920A1'}
                </td>
                <td style={{ fontWeight: 600 }}>{e.who}</td>
                <td style={{ fontWeight: 600, color: '#818cf8' }}>{e.what}</td>
                <td style={{ maxWidth: '320px', fontSize: '0.82rem' }}>{e.why || e.action}</td>
                <td>
                  <span className={`badge ${e.result === 'SUCCESS' ? 'badge-status' : 'badge-critical'}`}>
                    {e.result}
                  </span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};
