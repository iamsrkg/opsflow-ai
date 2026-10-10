import React from 'react';
import { LayoutDashboard, Ticket, Bot, ShieldCheck, BookOpen, FileText, Cpu } from 'lucide-react';

interface SidebarProps {
  currentTab: string;
  setCurrentTab: (tab: string) => void;
  pendingApprovalsCount: number;
}

export const Sidebar: React.FC<SidebarProps> = ({ currentTab, setCurrentTab, pendingApprovalsCount }) => {
  const navItems = [
    { id: 'dashboard', label: 'Executive Dashboard', icon: LayoutDashboard },
    { id: 'requests', label: 'Request Management', icon: Ticket },
    { id: 'approvals', label: 'Manager Approvals', icon: ShieldCheck, badge: pendingApprovalsCount },
    { id: 'knowledge', label: 'Runbook Vector Store', icon: BookOpen },
    { id: 'audit', label: 'Audit & Compliance', icon: FileText },
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
        <div className="user-avatar">SU</div>
        <div className="user-info">
          <span className="user-name">Sudheer Manager</span>
          <span className="user-role">MANAGER / ADMIN</span>
        </div>
      </div>
    </div>
  );
};
