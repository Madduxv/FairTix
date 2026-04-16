import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import api from '../api/client';
import '../styles/Login.css';

function ForgotPassword() {
  const [email, setEmail] = useState('');
  const [submitted, setSubmitted] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      await api.post('/auth/forgot-password', { email });
      setSubmitted(true);
    } catch (err) {
      if (err.status === 429) {
        setError('Too many requests. Please wait before trying again.');
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

  if (submitted) {
    return (
      <div className="login-page">
        <h2>Check your email</h2>
        <p>
          If an account exists for <strong>{email}</strong>, we've sent password reset
          instructions. Check your inbox (and spam folder).
        </p>
        <p className="form-link">
          <Link to="/login">Back to login</Link>
        </p>
      </div>
    );
  }

  return (
    <div className="login-page">
      <h2>Forgot Password</h2>
      <p>Enter your email address and we'll send you a link to reset your password.</p>
      <form onSubmit={handleSubmit} className="login-form">
        {error && <div className="error-message">{error}</div>}
        <div className="form-group">
          <label htmlFor="email">Email</label>
          <input
            id="email"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
            autoFocus
          />
        </div>
        <button type="submit" disabled={loading}>
          {loading ? 'Sending...' : 'Send Reset Link'}
        </button>
      </form>
      <p className="form-link">
        Remember your password? <Link to="/login">Log in</Link>
      </p>
    </div>
  );
}

export default ForgotPassword;
