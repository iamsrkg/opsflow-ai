import React from 'react';
import { ApprovalRequest } from '../services/types';
import { ApiService } from '../services/api';
import { ShieldAlert, Check, X, Bot, AlertCircle } from 'lucide-react';

interface ApprovalsViewProps {
  approvals: ApprovalRequest[];
  onRefresh: () => void;
}

export const ApprovalsView: React.FC<ApprovalsViewProps> = ({ approvals, onRefresh }) => {
  const handleApprove = async (id: number) => {
    await ApiService.approveAction(id);
    onRefresh();
  };

  return (
    <div className="content-body">
      <div className="table-container">
        <div className="table-header-toolbar">
          <div style={{ fontWeight: 700, fontSize: '1rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <ShieldAlert size={18} color="#fbbf24" />
            <span>Human-in-the-Loop Pending Approvals</span>
          </div>
          <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
            Backend enforces deterministic authorization. AI cannot execute direct actions.
          </span>
        </div>

        {approvals.length === 0 ? (
          <div style={{ padding: '3rem', textAlign: 'center', color: 'var(--text-muted)' }}>
            <Check size={32} color="#34d399" style={{ marginBottom: '0.5rem' }} />
            <div>No pending approvals in queue. All actions resolved!</div>
          </div>
        ) : (
          <table className="table-element">
            <thead>
              <tr>
                <th>Req ID</th>
                <th>Action Type</th>
                <th>Proposed By</th>
                <th>AI Reasoning & Context</th>
                <th>Status</th>
                <th>Authorize Action</th>
              </tr>
            </thead>
            <tbody>
              {approvals.map((app) => (
                <tr key={app.id}>
                  <td style={{ fontFamily: 'var(--font-mono)', fontWeight: 600, color: '#a5b4fc' }}>
                    REQ-{app.requestId}
                  </td>
                  <td style={{ fontWeight: 700, color: '#fbbf24' }}>
                    {app.actionType}
                  </td>
                  <td>
                    <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.35rem', color: '#c084fc' }}>
                      <Bot size={14} />
                      {app.proposedBy}
                    </span>
                  </td>
                  <td style={{ maxWidth: '340px', fontSize: '0.83rem' }}>
                    {app.reasoning}
                  </td>
                  <td>
                    <span className="badge badge-medium">
                      {app.status}
                    </span>
                  </td>
                  <td>
                    <div style={{ display: 'flex', gap: '0.5rem' }}>
                      <button className="btn-primary" style={{ padding: '0.35rem 0.75rem', fontSize: '0.78rem', background: 'var(--accent-emerald)' }} onClick={() => handleApprove(app.id)}>
                        <Check size={14} />
                        <span>Approve & Execute</span>
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
};
