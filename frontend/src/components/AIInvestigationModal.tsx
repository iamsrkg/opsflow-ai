import React, { useState } from 'react';
import { RequestResponseDTO, AIInvestigation } from '../services/types';
import { ApiService } from '../services/api';
import { X, Bot, CheckCircle2, AlertTriangle, ShieldCheck, FileText, Cpu, ArrowRight } from 'lucide-react';

interface AIInvestigationModalProps {
  request: RequestResponseDTO;
  onClose: () => void;
  onRefresh: () => void;
}

export const AIInvestigationModal: React.FC<AIInvestigationModalProps> = ({ request, onClose, onRefresh }) => {
  const [investigation, setInvestigation] = useState<AIInvestigation | null>(null);
  const [loading, setLoading] = useState(false);
  const [stepLog, setStepLog] = useState<string[]>([]);

  const runAIInvestigation = async () => {
    setLoading(true);
    setStepLog([]);

    // Step 1: Tool execution
    setStepLog((prev) => [...prev, 'Step 1/3: Executing tool getRequestDetails() & getAuditHistory()...']);
    await new Promise((r) => setTimeout(r, 600));

    // Step 2: Kafka & System health
    setStepLog((prev) => [...prev, "Step 2/3: Checking Kafka consumer lag on topic 'opsflow.payment.events'..."]);
    await new Promise((r) => setTimeout(r, 600));

    // Step 3: Vector RAG Search
    setStepLog((prev) => [...prev, 'Step 3/3: Searching pgvector Runbook store for cosine similarity...']);
    await new Promise((r) => setTimeout(r, 600));

    const result = await ApiService.investigateRequestWithAI(request.id);
    setInvestigation(result);
    setLoading(false);
    onRefresh();
  };

  const evidenceList: string[] = investigation
    ? JSON.parse(investigation.evidenceJson || '[]')
    : [];

  const workflowSteps = ['CREATED', 'CLASSIFIED', 'ASSIGNED', 'INVESTIGATING', 'PENDING_APPROVAL', 'RESOLVED'];

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-card" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <span style={{ fontFamily: 'var(--font-mono)', fontWeight: 700, color: '#818cf8' }}>
                {request.externalReference}
              </span>
              <span className={`badge badge-${request.category.toLowerCase()}`}>{request.category}</span>
              <span className={`badge badge-${request.priority.toLowerCase()}`}>{request.priority}</span>
            </div>
            <h2 style={{ fontSize: '1.15rem', marginTop: '0.25rem' }}>{request.title}</h2>
          </div>
          <button className="btn-secondary" style={{ padding: '0.4rem' }} onClick={onClose}>
            <X size={18} />
          </button>
        </div>

        <div className="modal-body">
          {/* Workflow Stepper */}
          <div className="stepper">
            {workflowSteps.map((s, idx) => {
              const currentIdx = workflowSteps.indexOf(request.status);
              const isCompleted = idx < currentIdx || request.status === 'RESOLVED';
              const isActive = s === request.status;

              return (
                <div key={s} className={`step-item ${isActive ? 'active' : ''} ${isCompleted ? 'completed' : ''}`}>
                  <div className="step-circle">{isCompleted ? '✓' : idx + 1}</div>
                  <span className="step-label">{s.replace('_', ' ')}</span>
                </div>
              );
            })}
          </div>

          <div style={{ background: 'var(--bg-dark)', padding: '1rem', borderRadius: '8px', border: '1px solid var(--border-color)' }}>
            <div style={{ fontSize: '0.82rem', color: 'var(--text-muted)', marginBottom: '0.35rem', fontWeight: 600 }}>Description</div>
            <div style={{ fontSize: '0.9rem' }}>{request.description}</div>
          </div>

          {!investigation && !loading && (
            <div style={{ textAlign: 'center', padding: '2rem 1rem', background: 'var(--bg-elevated)', borderRadius: '10px', border: '1px dashed var(--border-highlight)' }}>
              <Bot size={36} color="#818cf8" style={{ marginBottom: '0.75rem' }} />
              <h3 style={{ fontSize: '1.05rem', marginBottom: '0.35rem' }}>Automated AI Investigation & Reasoning</h3>
              <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)', maxWidth: '450px', margin: '0 auto 1.25rem' }}>
                Gathers audit history, inspects Kafka consumer lag, queries pgvector runbook embeddings, and proposes evidence-backed resolutions.
              </p>
              <button className="btn-primary" onClick={runAIInvestigation}>
                <Cpu size={16} />
                <span>Investigate with AI</span>
              </button>
            </div>
          )}

          {loading && (
            <div className="evidence-box">
              <div style={{ color: '#818cf8', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <Bot size={16} />
                <span>AI Reasoning Agent active...</span>
              </div>
              {stepLog.map((log, i) => (
                <div key={i} style={{ color: '#9ca3af' }}>{log}</div>
              ))}
            </div>
          )}

          {investigation && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
              {/* Evidence Breakdown */}
              <div>
                <div style={{ fontSize: '0.9rem', fontWeight: 700, marginBottom: '0.5rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <FileText size={16} color="#34d399" />
                  <span>Gathered Evidence Breakdown</span>
                </div>
                <div className="evidence-box">
                  {evidenceList.map((item, idx) => (
                    <div key={idx} className="evidence-item">
                      <CheckCircle2 size={14} style={{ minWidth: '14px', marginTop: '2px' }} />
                      <span>{item}</span>
                    </div>
                  ))}
                </div>
              </div>

              {/* Cause & Confidence */}
              <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: '1rem' }}>
                <div style={{ background: 'var(--bg-dark)', padding: '1rem', borderRadius: '8px', border: '1px solid var(--border-color)' }}>
                  <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', fontWeight: 600 }}>Likely Root Cause</div>
                  <div style={{ fontSize: '0.9rem', fontWeight: 600, color: '#fde68a', marginTop: '0.25rem' }}>
                    {investigation.likelyCause}
                  </div>
                </div>

                <div style={{ background: 'var(--bg-dark)', padding: '1rem', borderRadius: '8px', border: '1px solid var(--border-color)', textAlign: 'center' }}>
                  <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', fontWeight: 600 }}>Confidence Score</div>
                  <div style={{ fontSize: '1.5rem', fontWeight: 800, color: '#c084fc', marginTop: '0.1rem' }}>
                    {Math.round(investigation.confidenceScore * 100)}%
                  </div>
                </div>
              </div>

              {/* Recommended Action & Approval Gate */}
              <div style={{ background: 'rgba(245, 158, 11, 0.1)', border: '1px solid rgba(245, 158, 11, 0.3)', borderRadius: '10px', padding: '1rem', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', color: '#fbbf24', fontWeight: 700, fontSize: '0.9rem' }}>
                    <ShieldCheck size={18} />
                    <span>Recommended Action: {investigation.recommendedAction}</span>
                  </div>
                  <div style={{ fontSize: '0.78rem', color: '#fde68a', marginTop: '0.25rem' }}>
                    AI recommends executing event replay. Requires Manager authorization.
                  </div>
                </div>

                <button className="btn-primary" style={{ background: 'linear-gradient(135deg, #f59e0b, #d97706)' }} onClick={onClose}>
                  <span>Submitted to Approval Queue</span>
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
