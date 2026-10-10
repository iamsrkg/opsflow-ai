import React from 'react';
import { Plus, RefreshCw, Zap } from 'lucide-react';

interface TopBarProps {
  currentTab: string;
  onNewRequest: () => void;
  onRefresh: () => void;
}

export const TopBar: React.FC<TopBarProps> = ({ currentTab, onNewRequest, onRefresh }) => {
  const getTabTitles = () => {
    switch (currentTab) {
      case 'dashboard':
        return { title: 'Executive Operations Overview', subtitle: 'Real-time telemetry, Kafka consumer status & workflow metrics' };
      case 'requests':
        return { title: 'Operational Request Center', subtitle: 'Manage, classify, investigate & orchestrate business workflows' };
      case 'approvals':
        return { title: 'Human-in-the-Loop Approvals', subtitle: 'Review evidence-backed AI recommendations for sensitive actions' };
      case 'knowledge':
        return { title: 'RAG Knowledge & Runbooks', subtitle: 'Semantic vector store index of operational procedures and policies' };
      case 'audit':
        return { title: 'Compliance & System Audit Trail', subtitle: 'Immutable record of domain events, correlation IDs & user actions' };
      default:
        return { title: 'OpsFlow AI Platform', subtitle: 'Business Operations Orchestration' };
    }
  };

  const { title, subtitle } = getTabTitles();

  return (
    <div className="top-bar">
      <div className="page-title-group">
        <h1>{title}</h1>
        <div className="page-subtitle">{subtitle}</div>
      </div>

      <div className="top-actions">
        <button className="btn-secondary" onClick={onRefresh} title="Refresh System Telemetry">
          <RefreshCw size={15} />
          <span>Refresh</span>
        </button>
        <button className="btn-primary" onClick={onNewRequest}>
          <Plus size={16} />
          <span>New Request</span>
        </button>
      </div>
    </div>
  );
};
