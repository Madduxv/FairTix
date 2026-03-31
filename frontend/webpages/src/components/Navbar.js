import React from 'react';
import { NavLink, useLocation } from 'react-router-dom';
import { useAuth } from '../auth/useAuth';
import '../styles/Navbar.css';

function Navbar() {
  const { user, logout } = useAuth();
  const location = useLocation();

  if (location.pathname.startsWith('/admin')) return null;

  return (
    <nav className="navbar">
      <div className="navbar-brand">
        <NavLink to="/">FairTix</NavLink>
      </div>
      <div className="navbar-links">
        <NavLink to="/events">Events</NavLink>

        {!user && (
          <>
            <NavLink to="/login">Log In</NavLink>
            <NavLink to="/signup">Sign Up</NavLink>
          </>
        )}

        {user && (
          <>
            <NavLink to="/my-holds">My Holds</NavLink>
            <NavLink to="/my-tickets">My Tickets</NavLink>
            <NavLink to="/dashboard">Dashboard</NavLink>
            {user.role === 'ADMIN' && (
              <NavLink to="/admin">Admin Panel</NavLink>
            )}
            <button className="navbar-logout" onClick={logout}>
              Log Out
            </button>
          </>
        )}
      </div>
    </nav>
  );
}

export default Navbar;
