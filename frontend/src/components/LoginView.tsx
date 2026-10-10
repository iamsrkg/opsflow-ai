import React, { useState } from 'react';
import { Cpu, LogIn } from 'lucide-react';
import { Session } from '../services/api';

interface LoginViewProps {
  onSignedIn: () => void;
}

// The accounts the backend seeds for local and demo use. Each role is allowed different things,
// which is the point of trying more than one.
const DEMO_ACCOUNTS = [
  { username: 'manager', note: 'can approve actions' },
  { username: 'agent', note: 'can investigate, cannot approve' },
  { username: 'viewer', note: 'read only' },
];

export const LoginView: React.FC<LoginViewProps> = ({ onSignedIn }) => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const signIn = async (name: string, secret: string) => {
    setBusy(true);
    setError('');
    try {
      await Session.login(name, secret);
      onSignedIn();
    } catch (e: any) {
      setError(e?.status === 401 ? 'Wrong username or password.' : e?.message || 'Could not sign in.');
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="login-screen">
      <form className="login-card" onSubmit={(e) => { e.preventDefault(); signIn(username, password); }}>
        <div className="brand-header" style={{ border: 'none', padding: 0, marginBottom: '1.25rem' }}>
          <div className="brand-icon"><Cpu size={22} /></div>
          <div>
            <div className="brand-title">OpsFlow AI</div>
            <div style={{ fontSize: '0.75rem', color: '#9ca3af' }}>Sign in to the operations dashboard</div>
          </div>
        </div>

        <label className="login-label" htmlFor="login-username">Username</label>
        <input id="login-username" className="login-input" value={username} autoComplete="username"
          onChange={(e) => setUsername(e.target.value)} required />

        <label className="login-label" htmlFor="login-password">Password</label>
        <input id="login-password" className="login-input" type="password" value={password} autoComplete="current-password"
          onChange={(e) => setPassword(e.target.value)} required />

        {error && <div className="login-error" role="alert">{error}</div>}

        <button className="btn-primary" type="submit" disabled={busy} style={{ width: '100%', justifyContent: 'center', marginTop: '1rem' }}>
          <LogIn size={16} />
          <span>{busy ? 'Signing in…' : 'Sign in'}</span>
        </button>

        <div className="login-demo">
          <div className="login-demo-title">Demo accounts</div>
          {DEMO_ACCOUNTS.map((account) => (
            <button key={account.username} type="button" className="btn-secondary login-demo-btn" disabled={busy}
              onClick={() => signIn(account.username, `${account.username}123`)}>
              <strong>{account.username}</strong>
              <span>{account.note}</span>
            </button>
          ))}
        </div>
      </form>
    </div>
  );
};
