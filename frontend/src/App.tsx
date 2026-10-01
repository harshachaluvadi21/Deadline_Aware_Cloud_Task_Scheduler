import React, { useEffect, useState } from 'react';
import { BrowserRouter, Routes, Route, useLocation } from 'react-router-dom';
import { Navbar } from './components/Navbar';
import { LandingPage } from './pages/LandingPage';
import { SimulatorPage } from './pages/SimulatorPage';
import { AboutPage } from './pages/AboutPage';
import { simulationApi } from './services/api';

const ScrollToTop: React.FC = () => {
  const { pathname } = useLocation();
  useEffect(() => { window.scrollTo(0, 0); }, [pathname]);
  return null;
};

const AppContent: React.FC = () => {
  const [backendConnected, setBackendConnected] = useState<boolean | null>(null);

  useEffect(() => {
    simulationApi.healthCheck().then(setBackendConnected);
  }, []);

  return (
    <div className="page-wrapper">
      <Navbar backendConnected={backendConnected} />
      <ScrollToTop />
      <Routes>
        <Route path="/" element={<LandingPage />} />
        <Route path="/simulator" element={<SimulatorPage />} />
        <Route path="/about" element={<AboutPage />} />
        <Route path="*" element={<LandingPage />} />
      </Routes>
      <footer className="footer">
        <div className="container">
          <span style={{ color: 'var(--text-muted)' }}>
            ⚡ CloudSched · Deadline-Aware Cloud Task Scheduler · Mini Project · CloudSim Plus Simulation Platform
          </span>
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
