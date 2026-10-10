import React, { useState, useEffect } from 'react';
import { Sidebar } from './components/Sidebar';
import { TopBar } from './components/TopBar';
import { DashboardView } from './components/DashboardView';
import { RequestListView } from './components/RequestListView';
import { AIInvestigationModal } from './components/AIInvestigationModal';
import { ApprovalsView } from './components/ApprovalsView';
import { RunbooksView } from './components/RunbooksView';
import { AuditView } from './components/AuditView';
import { CreateRequestModal } from './components/CreateRequestModal';

import { DashboardMetrics, RequestResponseDTO, ApprovalRequest } from './services/types';
import { ApiService } from './services/api';

export const App: React.FC = () => {
  const [currentTab, setCurrentTab] = useState('dashboard');
  const [metrics, setMetrics] = useState<DashboardMetrics | null>(null);
  const [requests, setRequests] = useState<RequestResponseDTO[]>([]);
  const [approvals, setApprovals] = useState<ApprovalRequest[]>([]);
  const [selectedRequest, setSelectedRequest] = useState<RequestResponseDTO | null>(null);
  const [isNewRequestOpen, setIsNewRequestOpen] = useState(false);

  const loadData = async () => {
    try {
      const [m, reqs, apps] = await Promise.all([
        ApiService.getDashboardMetrics(),
        ApiService.getRequests(),
        ApiService.getPendingApprovals(),
      ]);
      setMetrics(m);
      setRequests(reqs);
      setApprovals(apps);
    } catch (e) {
      console.error('Failed loading telemetry:', e);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  return (
    <div className="app-container">
      <Sidebar
        currentTab={currentTab}
        setCurrentTab={setCurrentTab}
        pendingApprovalsCount={approvals.length}
      />

      <div className="main-content">
        <TopBar
          currentTab={currentTab}
          onNewRequest={() => setIsNewRequestOpen(true)}
          onRefresh={loadData}
        />

        {currentTab === 'dashboard' && (
          <DashboardView
            metrics={metrics}
            recentRequests={requests}
            onSelectRequest={setSelectedRequest}
            onNavigateTab={setCurrentTab}
          />
        )}

        {currentTab === 'requests' && (
          <RequestListView
            requests={requests}
            onSelectRequest={setSelectedRequest}
          />
        )}

        {currentTab === 'approvals' && (
          <ApprovalsView
            approvals={approvals}
            onRefresh={loadData}
          />
        )}

        {currentTab === 'knowledge' && <RunbooksView />}

        {currentTab === 'audit' && <AuditView />}
      </div>

      {selectedRequest && (
        <AIInvestigationModal
          request={selectedRequest}
          onClose={() => setSelectedRequest(null)}
          onRefresh={loadData}
        />
      )}

      {isNewRequestOpen && (
        <CreateRequestModal
          onClose={() => setIsNewRequestOpen(false)}
          onRefresh={loadData}
        />
      )}
    </div>
  );
};

export default App;
