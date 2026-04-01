import React, { useState } from 'react';
import { useAuth } from '../auth/useAuth';
import api from '../api/client';
import { removeToken } from '../auth/tokenUtils';
import '../styles/Dashboard.css';

function Dashboard() {
  const { user, logout } = useAuth();
  const [showDeleteDialog, setShowDeleteDialog] = useState(false);
  const [confirmEmail, setConfirmEmail] = useState('');
  const [deleting, setDeleting] = useState(false);
  const [error, setError] = useState('');

  async function handleDeleteAccount() {
    if (confirmEmail !== user.email) return;
    setDeleting(true);
    setError('');
    try {
      await api.delete('/api/users/me');
      removeToken();
      logout();
    } catch (err) {
      setError(err.message || 'Failed to delete account.');
      setDeleting(false);
    }
  }

  return (
    <div className="dashboard">
      <h2>Dashboard</h2>
      <div className="dashboard-info">
        <p><strong>Email:</strong> {user.email}</p>
        <p><strong>Role:</strong> {user.role}</p>
        <p><strong>User ID:</strong> {user.userId}</p>
      </div>

      <div className="dashboard-danger-zone">
        <h3>Danger Zone</h3>
        <p>Permanently delete your account and all associated data.</p>
        <button
          className="dashboard-delete-btn"
          onClick={() => setShowDeleteDialog(true)}
        >
          Delete My Account
        </button>
      </div>

      {showDeleteDialog && (
        <div className="dashboard-overlay" onClick={() => !deleting && setShowDeleteDialog(false)}>
          <div className="dashboard-dialog" onClick={(e) => e.stopPropagation()}>
            <h3>Delete Account</h3>
            <p>This action is <strong>permanent</strong> and cannot be undone. All your data will be removed.</p>
            <p>Type your email to confirm:</p>
            <p className="dashboard-dialog-email">{user.email}</p>
            <input
              type="email"
              value={confirmEmail}
              onChange={(e) => setConfirmEmail(e.target.value)}
              placeholder="Enter your email"
              disabled={deleting}
              autoFocus
            />
            {error && <div className="dashboard-dialog-error">{error}</div>}
            <div className="dashboard-dialog-actions">
              <button
                className="dashboard-dialog-cancel"
                onClick={() => { setShowDeleteDialog(false); setConfirmEmail(''); setError(''); }}
                disabled={deleting}
              >
                Cancel
              </button>
              <button
                className="dashboard-dialog-confirm"
                onClick={handleDeleteAccount}
                disabled={confirmEmail !== user.email || deleting}
              >
                {deleting ? 'Deleting...' : 'Delete My Account'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default Dashboard;
