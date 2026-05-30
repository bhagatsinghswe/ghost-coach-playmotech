import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';

const sportEmoji = { CRICKET: '🏏', FOOTBALL: '⚽', BASKETBALL: '🏀', BADMINTON: '🏸' };

export default function Navbar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => { logout(); navigate('/login'); };

  return (
    <nav style={styles.nav}>
      <div className="container" style={styles.inner}>
        <Link to="/" style={styles.logo}>
          👻 <span>Ghost Coach</span>
        </Link>

        {user ? (
          <div style={styles.right}>
            <span style={styles.sportBadge}>
              {sportEmoji[user.sport] || '🎯'} {user.sport}
            </span>
            <Link to="/history" style={styles.link}>History</Link>
            <Link to="/profile" style={styles.link}>Profile</Link>
            <Link to="/upload" style={styles.link}>+ New Session</Link>
            <button onClick={handleLogout} style={styles.logoutBtn}>Logout</button>
          </div>
        ) : (
          <div style={styles.right}>
            <Link to="/login" style={styles.link}>Login</Link>
            <Link to="/register" className="btn btn-primary" style={{ padding: '.4rem 1rem', fontSize: '.85rem' }}>
              Sign Up
            </Link>
          </div>
        )}
      </div>
    </nav>
  );
}

const styles = {
  nav: { background: '#1a1d27', borderBottom: '1px solid #2e3250', height: 64, display: 'flex', alignItems: 'center' },
  inner: { display: 'flex', alignItems: 'center', justifyContent: 'space-between', width: '100%' },
  logo: { fontSize: '1.2rem', fontWeight: 800, color: '#e8eaf0', textDecoration: 'none', display: 'flex', alignItems: 'center', gap: 8 },
  right: { display: 'flex', alignItems: 'center', gap: '1.25rem' },
  link: { color: '#8b90a8', fontSize: '.9rem', textDecoration: 'none' },
  sportBadge: { fontSize: '.8rem', background: 'rgba(108,99,255,.15)', color: '#6c63ff', padding: '.2rem .6rem', borderRadius: 20, fontWeight: 600 },
  logoutBtn: { background: 'none', border: '1px solid #2e3250', color: '#8b90a8', borderRadius: 8, padding: '.3rem .8rem', cursor: 'pointer', fontSize: '.85rem' },
};
