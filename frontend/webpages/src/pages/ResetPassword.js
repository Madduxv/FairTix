import React, { useState, useMemo } from 'react';
import { useSearchParams, Link, useNavigate } from 'react-router-dom';
import api from '../api/client';
import '../styles/Login.css';

const PASSWORD_RULES = [
  { key: 'length', label: 'At least 8 characters', test: (p) => p.length >= 8 },
  { key: 'upper', label: 'Uppercase letter', test: (p) => /[A-Z]/.test(p) },
  { key: 'lower', label: 'Lowercase letter', test: (p) => /[a-z]/.test(p) },
  { key: 'digit', label: 'A digit', test: (p) => /\d/.test(p) },
  { key: 'special', label: 'Special character', test: (p) => /[^A-Za-z0-9]/.test(p) },
];

function ResetPassword() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token') || '';
  const navigate = useNavigate();

  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [tokenInvalid, setTokenInvalid] = useState(false);

  const passwordChecks = useMemo(
    () => PASSWORD_RULES.map((rule) => ({ ...rule, passed: rule.test(password) })),
    [password]
  );
  const allPasswordRequirementsMet = passwordChecks.every((c) => c.passed);

  if (!token) {
    return (
      <div className="login-page">
        <h2>Invalid Reset Link</h2>
        <p>This password reset link is missing a token.</p>
        <p className="form-link">
          <Link to="/forgot-password">Request a new reset link</Link>
        </p>
      </div>
    );
  }

  if (tokenInvalid) {
    return (
      <div className="login-page">
        <h2>Reset Link Expired</h2>
        <p>This password reset link is invalid or has expired.</p>
        <p className="form-link">
          <Link to="/forgot-password">Request a new reset link</Link>
        </p>
      </div>
    );
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');

    if (!allPasswordRequirementsMet) {
      setError('Password does not meet all requirements.');
      return;
    }

    if (password !== confirmPassword) {
      setError('Passwords do not match.');
      return;
    }

    setLoading(true);
    try {
      await api.post('/auth/reset-password', { token, newPassword: password });
      navigate('/login', { state: { message: 'Password reset successfully. Please log in.' } });
    } catch (err) {
      if (err.status === 400) {
        const msg = err.body?.message || '';
        if (msg.toLowerCase().includes('expired') || msg.toLowerCase().includes('invalid') || msg.toLowerCase().includes('used')) {
          setTokenInvalid(true);
        } else {
          setError(msg || 'Invalid request. Please check your password and try again.');
        }
      } else if (err.status >= 500) {
        setError('Server error. Please try again later.');
      } else if (err.status) {
        setError('Something went wrong. Please try again.');
      } else {
        setError('Network error. Please check your connection.');
      }
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="login-page">
      <h2>Reset Password</h2>
      <form onSubmit={handleSubmit} className="login-form">
        {error && <div className="error-message">{error}</div>}
        <div className="form-group">
          <label htmlFor="password">New Password</label>
          <input
            id="password"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            minLength={8}
            autoFocus
          />
          {password.length > 0 && (
            <ul className="password-strength">
              {passwordChecks.map((check) => (
                <li key={check.key} className={check.passed ? 'met' : 'unmet'}>
                  {check.passed ? '\u2713' : '\u2717'} {check.label}
                </li>
              ))}
            </ul>
          )}
        </div>
        <div className="form-group">
          <label htmlFor="confirmPassword">Confirm New Password</label>
          <input
            id="confirmPassword"
            type="password"
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            required
          />
        </div>
        <button type="submit" disabled={loading || !allPasswordRequirementsMet}>
          {loading ? 'Resetting...' : 'Reset Password'}
        </button>
      </form>
      <p className="form-link">
        <Link to="/forgot-password">Request a new reset link</Link>
      </p>
    </div>
  );
}

export default ResetPassword;
