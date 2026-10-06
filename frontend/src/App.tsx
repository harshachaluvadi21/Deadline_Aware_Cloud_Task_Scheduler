import React, { useEffect, useState } from 'react';
import { BrowserRouter, Routes, Route, useLocation, Link } from 'react-router-dom';
import { Navbar } from './components/Navbar';
import { LandingPage } from './pages/LandingPage';
import { SimulatorPage } from './pages/SimulatorPage';
import { AboutPage } from './pages/AboutPage';
import { PrivacyPage } from './pages/PrivacyPage';
import { TermsPage } from './pages/TermsPage';
import { simulationApi } from './services/api';

const ScrollToTop: React.FC = () => {
  const { pathname } = useLocation();
  useEffect(() => { window.scrollTo(0, 0); }, [pathname]);
  return null;
};

const AppContent: React.FC = () => {
  const [backendConnected, setBackendConnected] = useState<boolean | null>(null);

  useEffect(() => {
    let isMounted = true;
    const check = async () => {
      const isUp = await simulationApi.healthCheck();
      if (isMounted) setBackendConnected(isUp);
    };
    check();
    const interval = setInterval(check, 15000);
    return () => {
      isMounted = false;
      clearInterval(interval);
    };
  }, []);

  return (
    <div className="page-wrapper">
      <Navbar backendConnected={backendConnected} />
      <ScrollToTop />
      <Routes>
        <Route path="/" element={<LandingPage />} />
        <Route path="/simulator" element={<SimulatorPage />} />
        <Route path="/about" element={<AboutPage />} />
        <Route path="/privacy" element={<PrivacyPage />} />
        <Route path="/privacy-policy" element={<PrivacyPage />} />
        <Route path="/terms" element={<TermsPage />} />
        <Route path="*" element={<LandingPage />} />
      </Routes>
      <footer className="footer">
        <div className="container">
          <div className="footer-grid">
            <div className="footer-col-main">
              <div className="footer-brand">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="var(--cyan)" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                  <path d="M18 10h-1.26A8 8 0 1 0 9 20h9a5 5 0 0 0 0-10z" />
                </svg>
                <span className="footer-title">CloudSched</span>
              </div>
              <p className="footer-desc">
                An academic research simulation platform for studying deadline-aware vs. priority-based
                task scheduling in cloud datacenters using CloudSim Plus.
              </p>
            </div>
            <div className="footer-col">
              <div className="footer-heading">Product</div>
              <ul className="footer-nav">
                <li><Link to="/simulator">Simulator</Link></li>
                <li><Link to="/about">Documentation</Link></li>
                <li><a href="https://github.com/harshachaluvadi21/Deadline_Aware_Cloud_Task_Scheduler" target="_blank" rel="noopener noreferrer">Source Code</a></li>
              </ul>
            </div>
            <div className="footer-col">
              <div className="footer-heading">Legal &amp; Info</div>
              <ul className="footer-nav">
                <li><Link to="/privacy">Privacy Policy</Link></li>
                <li><Link to="/terms">Terms of Service</Link></li>
                <li><Link to="/about">System Architecture</Link></li>
              </ul>
            </div>
          </div>
          <div className="footer-bottom">
            <span>&copy; {new Date().getFullYear()} CloudSched. Built with CloudSim Plus and React.</span>
            <span>Academic Mini Project</span>
          </div>
        </div>
      </footer>
    </div>
  );
};

export const App: React.FC = () => (
  <BrowserRouter>
    <AppContent />
  </BrowserRouter>
);

export default App;
