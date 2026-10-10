import React from 'react';
import { Plus, RefreshCw } from 'lucide-react';

interface TopBarProps {
  currentTab: string;
  onNewRequest: () => void;
  onRefresh: () => void;
}

export const TopBar: React.FC<TopBarProps> = ({ currentTab, onNewRequest, onRefresh }) => {
  const getTabTitles = () => {
    switch (currentTab) {
      case 'dashboard':
        return { title: 'Operations overview', subtitle: 'Requests, approvals and the event pipeline' };
      case 'requests':
        return { title: 'Requests', subtitle: 'Create, classify and investigate operational requests' };
      case 'approvals':
        return { title: 'Approvals', subtitle: 'Recommended actions waiting for a manager. Nothing runs until it is approved.' };
      case 'knowledge':
        return { title: 'Runbooks', subtitle: 'Search the operational runbooks by similarity' };
      case 'audit':
        return { title: 'Audit trail', subtitle: 'Who did what, with the correlation id of the request that caused it' };
      default:
        return { title: 'OpsFlow AI', subtitle: '' };
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
        <button className="btn-secondary" onClick={onRefresh} title="Reload">
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
