import React from 'react';
import { DashboardMetrics, RequestResponseDTO } from '../services/types';
import { Ticket, Activity, CheckCircle2, ShieldAlert, Cpu, Layers } from 'lucide-react';

interface DashboardViewProps {
  metrics: DashboardMetrics | null;
  recentRequests: RequestResponseDTO[];
  onSelectRequest: (req: RequestResponseDTO) => void;
  onNavigateTab: (tab: string) => void;
}

export const DashboardView: React.FC<DashboardViewProps> = ({
  metrics,
  recentRequests,
  onSelectRequest,
  onNavigateTab,
}) => {
  return (
    <div className="content-body">
      {/* Metric Telemetry Cards */}
      <div className="metrics-grid">
        <div className="metric-card" style={{ '--card-accent': '#6366f1' } as any}>
          <div className="metric-header">
            <span>Total Operational Requests</span>
            <Ticket size={18} color="#818cf8" />
          </div>
          <div className="metric-value">{metrics?.totalRequests ?? 4}</div>
          <div className="metric-foot">
            <Activity size={14} />
            <span>Active: {metrics?.activeRequests ?? 4}</span>
          </div>
        </div>

        <div className="metric-card" style={{ '--card-accent': '#10b981' } as any}>
          <div className="metric-header">
            <span>Resolution Rate</span>
            <CheckCircle2 size={18} color="#34d399" />
          </div>
          <div className="metric-value">{metrics?.resolutionRatePercentage ?? 75.0}%</div>
          <div className="metric-foot" style={{ color: '#34d399' }}>
            <span>Target &gt; 90.0%</span>
          </div>
        </div>

        <div className="metric-card" style={{ '--card-accent': '#f59e0b' } as any}>
          <div className="metric-header">
            <span>Pending Approvals</span>
            <ShieldAlert size={18} color="#fbbf24" />
          </div>
          <div className="metric-value" style={{ color: (metrics?.pendingApprovals ?? 0) > 0 ? '#fbbf24' : 'inherit' }}>
            {metrics?.pendingApprovals ?? 0}
          </div>
          <div className="metric-foot" style={{ color: '#fbbf24', cursor: 'pointer' }} onClick={() => onNavigateTab('approvals')}>
            <span>Review required actions &rarr;</span>
          </div>
        </div>

        <div className="metric-card" style={{ '--card-accent': '#06b6d4' } as any}>
          <div className="metric-header">
            <span>Outbox backlog</span>
            <Layers size={18} color="#22d3ee" />
          </div>
          <div className="metric-value">{metrics?.outboxPending ?? 0} <span style={{ fontSize: '0.9rem', color: '#9ca3af' }}>waiting for Kafka</span></div>
          <div className="metric-foot" style={{ color: '#22d3ee' }}>
            <span>{metrics?.eventsPublished ?? 0} published · {metrics?.eventsConsumed ?? 0} consumed</span>
          </div>
        </div>

        <div className="metric-card" style={{ '--card-accent': '#a855f7' } as any}>
          <div className="metric-header">
            <span>Investigation confidence</span>
            <Cpu size={18} color="#c084fc" />
          </div>
          <div className="metric-value">{metrics?.aiAvgConfidence != null ? `${Math.round(metrics.aiAvgConfidence * 100)}%` : '—'}</div>
          <div className="metric-foot" style={{ color: '#c084fc' }}>
            <span>average over stored investigations</span>
          </div>
        </div>
      </div>

      {/* Main Request Stream */}
      <div className="table-container">
        <div className="table-header-toolbar">
          <div style={{ fontWeight: 700, fontSize: '1rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <Ticket size={18} color="#818cf8" />
            <span>Active Operational Requests</span>
          </div>
          <button className="btn-secondary" onClick={() => onNavigateTab('requests')}>
            View All Requests
          </button>
        </div>

        <table className="table-element">
          <thead>
            <tr>
              <th>Ref ID</th>
              <th>Title</th>
              <th>Category</th>
              <th>Priority</th>
              <th>Status</th>
              <th>Created By</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {recentRequests.map((req) => (
              <tr key={req.id}>
                <td style={{ fontFamily: 'var(--font-mono)', fontWeight: 600, color: '#a5b4fc' }}>
                  {req.externalReference}
                </td>
                <td style={{ maxWidth: '300px', fontWeight: 500 }}>
                  {req.title}
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
                <td style={{ color: 'var(--text-muted)' }}>{req.createdBy}</td>
                <td>
                  <button className="btn-secondary" style={{ padding: '0.35rem 0.75rem', fontSize: '0.78rem' }} onClick={() => onSelectRequest(req)}>
                    Investigate
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
