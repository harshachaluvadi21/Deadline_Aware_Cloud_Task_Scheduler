import React, { useState } from 'react';
import { Link, useLocation } from 'react-router-dom';

interface NavbarProps {
  backendConnected: boolean | null;
}

export const Navbar: React.FC<NavbarProps> = ({ backendConnected }) => {
  const [mobileOpen, setMobileOpen] = useState(false);
  const location = useLocation();
  const isActive = (path: string) => location.pathname === path ? 'active' : '';

  const links = [
    { to: '/', label: 'Home' },
    { to: '/simulator', label: 'Simulator' },
    { to: '/about', label: 'How It Works' },
  ];

  return (
    <nav className="navbar">
      <div className="container">
        <div className="navbar-inner">
          <Link to="/" className="navbar-logo">
            <div className="navbar-logo-icon">⚡</div>
            <span>CloudSched</span>
          </Link>

          <ul className="navbar-links">
            {links.map(l => (
              <li key={l.to}>
                <Link to={l.to} className={isActive(l.to)}>{l.label}</Link>
              </li>
            ))}
          </ul>

          <div className="navbar-right">
            {backendConnected === true && (
              <span className="status-pill online">
                <span className="status-dot" />API Live
              </span>
            )}
            {backendConnected === false && (
              <span className="status-pill offline">
                <span className="status-dot" />Offline
              </span>
            )}
            {backendConnected === null && (
              <span className="status-pill connecting">
                <span className="status-dot pulse" />Connecting…
              </span>
            )}
            <Link to="/simulator" className="btn btn-primary btn-sm">
              Launch Simulator
            </Link>
            <button
              className="navbar-hamburger"
              onClick={() => setMobileOpen(!mobileOpen)}
              aria-label="Toggle menu"
            >
              <span /><span /><span />
            </button>
          </div>
        </div>
      </div>

      <div className={`navbar-mobile-menu ${mobileOpen ? 'open' : ''}`}>
        {links.map(l => (
          <Link key={l.to} to={l.to} className={isActive(l.to)} onClick={() => setMobileOpen(false)}>
            {l.label}
          </Link>
        ))}
        <Link to="/simulator" className="btn btn-primary btn-sm" style={{ marginTop: 8 }} onClick={() => setMobileOpen(false)}>
          Launch Simulator
        </Link>
      </div>
    </nav>
  );
};
