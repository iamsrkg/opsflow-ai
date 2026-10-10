import React, { useState, useEffect, useCallback } from 'react';
import { Sidebar } from './components/Sidebar';
import { TopBar } from './components/TopBar';
import { DashboardView } from './components/DashboardView';
import { RequestListView } from './components/RequestListView';
import { AIInvestigationModal } from './components/AIInvestigationModal';
import { ApprovalsView } from './components/ApprovalsView';
import { RunbooksView } from './components/RunbooksView';
import { AuditView } from './components/AuditView';
import { CreateRequestModal } from './components/CreateRequestModal';
import { LoginView } from './components/LoginView';

import { DashboardMetrics, RequestResponseDTO, ApprovalRequest } from './services/types';
import { ApiService, Session } from './services/api';

// checking: asking whether a backend is there · sample: none, show sample data
// login: a backend is there, nobody signed in · live: signed in, everything is real
type Mode = 'checking' | 'sample' | 'login' | 'live';

export const App: React.FC = () => {
  const [mode, setMode] = useState<Mode>('checking');
  const [currentTab, setCurrentTab] = useState('dashboard');
  const [metrics, setMetrics] = useState<DashboardMetrics | null>(null);
  const [requests, setRequests] = useState<RequestResponseDTO[]>([]);
  const [approvals, setApprovals] = useState<ApprovalRequest[]>([]);
  const [selectedRequest, setSelectedRequest] = useState<RequestResponseDTO | null>(null);
  const [isNewRequestOpen, setIsNewRequestOpen] = useState(false);
  const [error, setError] = useState('');

  const loadData = useCallback(async () => {
    try {
      const [m, reqs, apps] = await Promise.all([
        ApiService.getDashboardMetrics(),
        ApiService.getRequests(),
        ApiService.getPendingApprovals(),
      ]);
      setMetrics(m);
      setRequests(reqs);
      setApprovals(apps);
    } catch {
      // the message is already on screen (see Session.onError); a 401 also sends us back to the login
      if (Session.isLive() && !Session.isSignedIn()) setMode('login');
    }
  }, []);

  useEffect(() => {
    Session.onError(setError);
    Session.probe().then((backendIsThere) => {
      if (!backendIsThere) setMode('sample');
      else setMode(Session.isSignedIn() ? 'live' : 'login');
    });
  }, []);

  useEffect(() => {
    if (mode === 'sample' || mode === 'live') loadData();
  }, [mode, loadData]);

  if (mode === 'checking') {
    return <div className="login-screen"><div style={{ color: '#9ca3af' }}>Looking for the backend…</div></div>;
  }
  if (mode === 'login') {
    return <LoginView onSignedIn={() => { setError(''); setMode('live'); }} />;
  }

  return (
    <div className="app-container">
      <Sidebar
        currentTab={currentTab}
        setCurrentTab={setCurrentTab}
        pendingApprovalsCount={approvals.length}
        user={Session.user()}
        onLogout={mode === 'live' ? () => { Session.logout(); setMode('login'); } : undefined}
      />

      <div className="main-content">
        {mode === 'sample' && (
          <div className="mode-banner" role="status">
            <strong>Sample data.</strong> No backend is connected to this page, so nothing here is saved and the numbers are examples.
            {' '}<a href="https://github.com/iamsrkg/opsflow-ai#run-it" target="_blank" rel="noopener">Run the real thing</a>
          </div>
        )}
        {error && (
          <div className="error-banner" role="alert">
            <span>{error}</span>
            <button onClick={() => setError('')} aria-label="Dismiss">×</button>
          </div>
        )}

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
