import React, { createContext, useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api/client';
import { fetchCurrentUser } from './tokenUtils';

export const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [sessionExpired, setSessionExpired] = useState(false);
  const navigate = useNavigate();

  const logout = useCallback(async (expired = false) => {
    try {
      await api.post('/auth/logout');
    } catch {
      // Cookie may already be gone — that's fine
    }
    setUser(null);
    if (expired) {
      setSessionExpired(true);
    }
    navigate('/');
  }, [navigate]);

  const clearSessionExpired = useCallback(() => {
    setSessionExpired(false);
  }, []);

  // Hydrate from HTTP-only cookie on mount
  useEffect(() => {
    fetchCurrentUser()
      .then((data) => {
        if (data) {
          setUser({ email: data.email, userId: data.userId, role: data.role, emailVerified: data.emailVerified });
        }
      })
      .finally(() => setIsLoading(false));
  }, []);

  // Listen for session expiry signal from the API client
  useEffect(() => {
    const handler = () => logout(true);
    window.addEventListener('auth:session-expired', handler);
    return () => window.removeEventListener('auth:session-expired', handler);
  }, [logout]);

  async function login(email, password, recaptchaToken = '') {
    const data = await api.post('/auth/login', { email, password, recaptchaToken });
    if (!data || !data.userId || !data.email || !data.role) {
      throw new Error('Authentication failed: invalid response from server.');
    }
    setSessionExpired(false);
    const userInfo = { email: data.email, userId: data.userId, role: data.role, emailVerified: data.emailVerified };
    setUser(userInfo);
    return userInfo;
  }

  async function signup(email, password) {
    const data = await api.post('/auth/register', { email, password });
    if (!data || !data.userId || !data.email || !data.role) {
      throw new Error('Registration failed: invalid response from server.');
    }
    setSessionExpired(false);
    const userInfo = { email: data.email, userId: data.userId, role: data.role, emailVerified: data.emailVerified };
    setUser(userInfo);
    return userInfo;
  }

  const refreshUser = useCallback(async () => {
    const data = await fetchCurrentUser();
    if (data) {
      setUser({ email: data.email, userId: data.userId, role: data.role, emailVerified: data.emailVerified });
    }
  }, []);

  const value = {
    user,
    isLoading,
    sessionExpired,
    clearSessionExpired,
    login,
    signup,
    logout,
    refreshUser,
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
}
