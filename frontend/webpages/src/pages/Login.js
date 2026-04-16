import React, { useState, useEffect, useRef } from 'react';
import { useNavigate, useLocation, Link, Navigate } from 'react-router-dom';
import { useAuth } from '../auth/useAuth';
import Recaptcha from '../components/Recaptcha';
import '../styles/Login.css';

function Login() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [loginAttempts, setLoginAttempts] = useState(0);
  const [captchaToken, setCaptchaToken] = useState('');
  const [lockoutSeconds, setLockoutSeconds] = useState(0);
  const lockoutTimer = useRef(null);
  const recaptchaRef = useRef(null);
  const { login, user } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const from = location.state?.from?.pathname || '/dashboard';

  useEffect(() => {
    return () => {
      if (lockoutTimer.current) clearInterval(lockoutTimer.current);
    };
  }, []);

  if (user) {
    return <Navigate to={from} replace />;
  }

  function startLockoutTimer(seconds) {
    setLockoutSeconds(seconds);
    if (lockoutTimer.current) clearInterval(lockoutTimer.current);
    lockoutTimer.current = setInterval(() => {
      setLockoutSeconds((prev) => {
        if (prev <= 1) {
          clearInterval(lockoutTimer.current);
          lockoutTimer.current = null;
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
  }

  function formatTime(totalSeconds) {
    const m = Math.floor(totalSeconds / 60);
    const s = totalSeconds % 60;
    return m > 0 ? `${m}m ${s}s` : `${s}s`;
  }

  async function handleSubmit(e) {
    e.preventDefault();
    if (lockoutSeconds > 0) return;

    const shouldShowCaptcha = loginAttempts >= 2;
    if (shouldShowCaptcha && !captchaToken) {
      setError('Please complete the reCAPTCHA checkbox before logging in.');
      return;
    }

    setError('');
    setLoading(true);

    try {
      await login(email, password, captchaToken);
      navigate(from, { replace: true });
    } catch (err) {
      setLoginAttempts((prev) => prev + 1);
      setCaptchaToken('');
      if (recaptchaRef.current && typeof recaptchaRef.current.reset === 'function') {
        recaptchaRef.current.reset();
      }

      if (err.status === 429) {
        const retryAfter = err.body?.remainingSeconds || 60;
        startLockoutTimer(retryAfter);
        setError('Account temporarily locked due to too many failed attempts.');
      } else if (err.code === 'CAPTCHA_REQUIRED') {
        setLoginAttempts((prev) => (prev < 2 ? 2 : prev));
        setError('Please complete the reCAPTCHA checkbox before logging in.');
      } else if (err.status === 401 || err.status === 400) {
        setError('Invalid email or password.');
      } else if (err.status >= 500) {
        setError('Server error. Please try again later.');
      } else if (err.status) {
        setError('Login failed. Please try again.');
      } else {
        setError('Network error. Please check your connection.');
      }
    } finally {
      setLoading(false);
    }
  }

  const isLocked = lockoutSeconds > 0;
  const shouldShowCaptcha = !isLocked && loginAttempts >= 2;

  return (
    <div className="login-page">
      <h2>Log In</h2>
      <form onSubmit={handleSubmit} className="login-form">
        {error && (
          <div className={`error-message${isLocked ? ' lockout-message' : ''}`}>
            {error}
            {isLocked && (
              <div className="lockout-timer">
                Try again in {formatTime(lockoutSeconds)}
              </div>
            )}
          </div>
        )}
        <div className="form-group">
          <label htmlFor="email">Email</label>
          <input
            id="email"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
            disabled={isLocked}
          />
        </div>
        <div className="form-group">
          <label htmlFor="password">Password</label>
          <input
            id="password"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            disabled={isLocked}
          />
        </div>
        {shouldShowCaptcha && (
          <Recaptcha onChange={setCaptchaToken} ref={recaptchaRef} />
        )}
        <button type="submit" disabled={loading || isLocked}>
          {loading ? 'Logging in...' : isLocked ? 'Account Locked' : 'Log In'}
        </button>
      </form>
      <p className="form-link">
        Don't have an account? <Link to="/signup">Sign up</Link>
      </p>
    </div>
  );
}

export default Login;
