import React, { useEffect, useState } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { useAuth } from '../auth/useAuth';
import api from '../api/client';

function VerifyEmail() {
  useEffect(() => { document.title = 'Verify Email | FairTix'; }, []);
  const [searchParams] = useSearchParams();
  const { user, isLoading, refreshUser } = useAuth();
  const [status, setStatus] = useState('loading'); // loading | success | error | already

  useEffect(() => {
    // Wait for AuthContext to finish hydrating before checking user state
    if (isLoading) return;

    const success = searchParams.get('success');
    const error = searchParams.get('error');
    const token = searchParams.get('token');

    if (success === 'true') {
      // Refresh AuthContext so emailVerified is up-to-date without a page reload
      refreshUser().finally(() => setStatus('success'));
      return;
    }
    if (error === 'true') {
      setStatus('error');
      return;
    }
    if (token) {
      // Direct token hit (shouldn't normally happen — backend redirects — but handle it)
      api.get(`/auth/verify?token=${encodeURIComponent(token)}`)
        .then(() => refreshUser().finally(() => setStatus('success')))
        .catch(() => setStatus('error'));
      return;
    }
    if (user?.emailVerified) {
      setStatus('already');
      return;
    }
    setStatus('error');
  }, [searchParams, user, isLoading, refreshUser]);

  if (status === 'loading') {
    return <div className="login-page"><p>Verifying...</p></div>;
  }

  if (status === 'success') {
    return (
      <div className="login-page">
        <h2>Email verified!</h2>
        <p>Your account is now active. You can browse events and purchase tickets.</p>
        <p><Link to="/events">Browse events</Link></p>
      </div>
    );
  }

  if (status === 'already') {
    return (
      <div className="login-page">
        <h2>Already verified</h2>
        <p>Your email address is already verified.</p>
        <p><Link to="/dashboard">Go to dashboard</Link></p>
      </div>
    );
  }

  return (
    <div className="login-page">
      <h2>Verification failed</h2>
      <p>This link is invalid or has expired.</p>
      <p>
        If you need a new link, log in and use the resend option on your dashboard.
      </p>
      <p><Link to="/login">Log in</Link></p>
    </div>
  );
}

export default VerifyEmail;
