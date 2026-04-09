import React, { useState, useEffect } from 'react';
import { useAuth } from '../auth/useAuth';
import api from '../api/client';
import '../styles/Dashboard.css';

function Dashboard() {
  const { user, logout } = useAuth();
  const [showDeleteDialog, setShowDeleteDialog] = useState(false);
  const [confirmEmail, setConfirmEmail] = useState('');
  const [deleting, setDeleting] = useState(false);
  const [error, setError] = useState('');

  // Notification preferences
  const [prefs, setPrefs] = useState(null);
  const [prefsLoading, setPrefsLoading] = useState(true);
  const [prefsSaving, setPrefsSaving] = useState(false);
  const [prefsMessage, setPrefsMessage] = useState('');

  // Data export
  const [exporting, setExporting] = useState(false);

  useEffect(() => {
    api.get('/api/users/me/notifications')
      .then((data) => setPrefs(data))
      .catch(() => setPrefs({ emailOrder: true, emailTicket: true, emailHold: false, emailMarketing: false }))
      .finally(() => setPrefsLoading(false));
  }, []);

  async function handleSavePrefs() {
    setPrefsSaving(true);
    setPrefsMessage('');
    try {
      const updated = await api.put('/api/users/me/notifications', prefs);
      setPrefs(updated);
      setPrefsMessage('Preferences saved.');
    } catch (err) {
      setPrefsMessage(err.message || 'Failed to save preferences.');
    } finally {
      setPrefsSaving(false);
    }
  }

  function togglePref(key) {
    setPrefs((prev) => ({ ...prev, [key]: !prev[key] }));
    setPrefsMessage('');
  }

  async function handleExportData() {
    setExporting(true);
    try {
      const data = await api.get('/api/users/me/data-export');
      const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = 'fairtix-data-export.json';
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
    } catch (err) {
      alert(err.message || 'Failed to export data.');
    } finally {
      setExporting(false);
    }
  }

  async function handleDeleteAccount() {
    if (confirmEmail !== user.email) return;
    setDeleting(true);
    setError('');
    try {
      await api.delete('/api/users/me');
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

      {/* Notification Preferences */}
      <div className="dashboard-section">
        <h3>Notification Preferences</h3>
        {prefsLoading ? (
          <p className="dashboard-muted">Loading preferences...</p>
        ) : prefs && (
          <>
            <div className="prefs-list">
              <label className="pref-toggle">
                <input type="checkbox" checked={prefs.emailOrder} onChange={() => togglePref('emailOrder')} />
                <span>Order confirmation emails</span>
              </label>
              <label className="pref-toggle">
                <input type="checkbox" checked={prefs.emailTicket} onChange={() => togglePref('emailTicket')} />
                <span>Ticket issuance emails</span>
              </label>
              <label className="pref-toggle">
                <input type="checkbox" checked={prefs.emailHold} onChange={() => togglePref('emailHold')} />
                <span>Hold creation &amp; expiry emails</span>
              </label>
              <label className="pref-toggle">
                <input type="checkbox" checked={prefs.emailMarketing} onChange={() => togglePref('emailMarketing')} />
                <span>Marketing &amp; promotional emails</span>
              </label>
            </div>
            <button className="dashboard-save-btn" onClick={handleSavePrefs} disabled={prefsSaving}>
              {prefsSaving ? 'Saving...' : 'Save Preferences'}
            </button>
            {prefsMessage && <p className="prefs-message">{prefsMessage}</p>}
          </>
        )}
      </div>

      {/* Data Export */}
      <div className="dashboard-section">
        <h3>Your Data</h3>
        <p className="dashboard-muted">
          Download a copy of all your personal data including your profile,
          orders, tickets, and notification preferences.
        </p>
        <button className="dashboard-export-btn" onClick={handleExportData} disabled={exporting}>
          {exporting ? 'Exporting...' : 'Export My Data'}
        </button>
      </div>

      {/* Danger Zone */}
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
