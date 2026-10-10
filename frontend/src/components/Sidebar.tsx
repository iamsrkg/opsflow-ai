import React from 'react';
import { LayoutDashboard, Ticket, ShieldCheck, BookOpen, FileText, Cpu, LogOut } from 'lucide-react';
import { SessionUser } from '../services/types';

interface SidebarProps {
  currentTab: string;
  setCurrentTab: (tab: string) => void;
  pendingApprovalsCount: number;
  user: SessionUser | null;
  onLogout?: () => void;
}

export const Sidebar: React.FC<SidebarProps> = ({ currentTab, setCurrentTab, pendingApprovalsCount, user, onLogout }) => {
  const navItems = [
    { id: 'dashboard', label: 'Dashboard', icon: LayoutDashboard },
    { id: 'requests', label: 'Requests', icon: Ticket },
    { id: 'approvals', label: 'Approvals', icon: ShieldCheck, badge: pendingApprovalsCount },
    { id: 'knowledge', label: 'Runbooks', icon: BookOpen },
    { id: 'audit', label: 'Audit trail', icon: FileText },
  ];

  return (
    <div className="sidebar">
      <div className="brand-header">
        <div className="brand-icon">
          <Cpu size={22} />
        </div>
        <div>
          <div className="brand-title">OpsFlow AI</div>
          <div style={{ fontSize: '0.7rem', color: '#9ca3af', fontWeight: 500 }}>Ops & Workflow Platform</div>
        </div>
      </div>

      <div className="nav-section">
        {navItems.map((item) => {
          const Icon = item.icon;
          const isActive = currentTab === item.id;
          return (
            <div
              key={item.id}
              className={`nav-item ${isActive ? 'active' : ''}`}
              onClick={() => setCurrentTab(item.id)}
            >
              <Icon size={18} />
              <span>{item.label}</span>
              {item.badge && item.badge > 0 ? (
                <span className="nav-badge">{item.badge}</span>
              ) : null}
            </div>
          );
        })}
      </div>

      <div className="user-profile">
        <div className="user-avatar">{(user?.username ?? 'sample').slice(0, 2).toUpperCase()}</div>
        <div className="user-info">
          <span className="user-name">{user?.fullName ?? 'Sample user'}</span>
          <span className="user-role">{user?.role ?? 'NOT SIGNED IN'}</span>
        </div>
        {onLogout && (
          <button className="logout-btn" onClick={onLogout} title="Sign out" aria-label="Sign out">
            <LogOut size={16} />
          </button>
        )}
      </div>
    </div>
  );
};
