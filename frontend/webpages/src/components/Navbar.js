import React, { useState } from 'react';
import { NavLink } from 'react-router-dom';
import { useAuth } from '../auth/useAuth';
import { useTheme } from '../theme/ThemeProvider';
import '../styles/Navbar.css';

function Navbar() {
  const { user, logout } = useAuth();
  const { theme, toggle } = useTheme();
  const [menuOpen, setMenuOpen] = useState(false);

  function closeMenu() { setMenuOpen(false); }

  return (
    <nav className="navbar">
      <div className="navbar-brand">
        <NavLink to="/">FairTix</NavLink>
      </div>
      <button
        className="navbar-hamburger"
        aria-label={menuOpen ? 'Close menu' : 'Open menu'}
        aria-expanded={menuOpen}
        aria-controls="navbar-links"
        onClick={() => setMenuOpen(v => !v)}
      >
        <span className="hamburger-bar" />
        <span className="hamburger-bar" />
        <span className="hamburger-bar" />
      </button>
      <div
        id="navbar-links"
        className={`navbar-links${menuOpen ? ' navbar-links--open' : ''}`}
      >
        <button
          className="navbar-theme-toggle"
          onClick={toggle}
          aria-label={theme === 'dark' ? 'Switch to light mode' : 'Switch to dark mode'}
        >
          {theme === 'dark' ? '☀' : '☾'}
        </button>
        <NavLink to="/events" onClick={closeMenu}>Events</NavLink>
        <NavLink to="/events/near-me" onClick={closeMenu}>Near Me</NavLink>

        {!user && (
          <>
            <NavLink to="/login" onClick={closeMenu}>Log In</NavLink>
            <NavLink to="/signup" onClick={closeMenu}>Sign Up</NavLink>
          </>
        )}

        {user && (
          <>
            <NavLink to="/my-holds" onClick={closeMenu}>My Holds</NavLink>
            <NavLink to="/my-tickets" onClick={closeMenu}>My Tickets</NavLink>
            <NavLink to="/order-history" onClick={closeMenu}>Order History</NavLink>
            <NavLink to="/transfers" onClick={closeMenu}>Transfers</NavLink>
            <NavLink to="/support" onClick={closeMenu}>Support</NavLink>
            <NavLink to="/dashboard" onClick={closeMenu}>Dashboard</NavLink>
            {user.role === 'ADMIN' && (
              <NavLink to="/admin" onClick={closeMenu}>Admin Panel</NavLink>
            )}
            <button className="navbar-logout" onClick={() => { closeMenu(); logout(); }}>
              Log Out
            </button>
          </>
        )}
      </div>
    </nav>
  );
}

export default Navbar;
