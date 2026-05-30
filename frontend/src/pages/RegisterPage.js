import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

const SPORTS = ['CRICKET', 'FOOTBALL', 'BASKETBALL', 'BADMINTON'];
const LEVELS = ['BEGINNER', 'INTERMEDIATE', 'ADVANCED'];
const POSITIONS = {
  CRICKET: ['Batsman', 'Bowler', 'All-Rounder', 'Wicket-Keeper'],
  FOOTBALL: ['Goalkeeper', 'Defender', 'Midfielder', 'Striker'],
  BASKETBALL: ['Point Guard', 'Shooting Guard', 'Small Forward', 'Power Forward', 'Center'],
  BADMINTON: ['Singles Player', 'Doubles Player', 'Mixed Doubles'],
};

export default function RegisterPage() {
  const { register } = useAuth();
  const navigate = useNavigate();

  const [form, setForm] = useState({
    fullName: '', email: '', password: '',
    sport: '', position: '', experienceLevel: '', age: '',
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }));

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await register({ ...form, age: form.age ? parseInt(form.age) : undefined });
      navigate('/upload');
    } catch (err) {
      const msg = err.response?.data?.message || 'Registration failed. Please try again.';
      const errs = err.response?.data?.errors;
      setError(errs ? Object.values(errs).join(' · ') : msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="page">
      <div className="container" style={{ maxWidth: 480, margin: '0 auto' }}>
        <div style={{ textAlign: 'center', marginBottom: '2rem' }}>
          <div style={{ fontSize: '2.5rem', marginBottom: '.5rem' }}>👻</div>
          <h1 style={{ fontSize: '1.6rem', fontWeight: 800 }}>Create your account</h1>
          <p style={{ color: '#8b90a8', marginTop: '.4rem' }}>Start receiving AI coaching feedback</p>
        </div>

        <div className="card">
          {error && <div className="alert alert-error">{error}</div>}

          <form onSubmit={handleSubmit}>
            <div className="form-group">
              <label>Full Name</label>
              <input placeholder="Your name" value={form.fullName} onChange={set('fullName')} required />
            </div>
            <div className="form-group">
              <label>Email</label>
              <input type="email" placeholder="you@example.com" value={form.email} onChange={set('email')} required />
            </div>
            <div className="form-group">
              <label>Password</label>
              <input type="password" placeholder="Min 8 characters" value={form.password} onChange={set('password')} required minLength={8} />
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
              <div className="form-group">
                <label>Sport</label>
                <select value={form.sport} onChange={set('sport')} required>
                  <option value="">Select sport</option>
                  {SPORTS.map((s) => <option key={s} value={s}>{s}</option>)}
                </select>
              </div>
              <div className="form-group">
                <label>Experience Level</label>
                <select value={form.experienceLevel} onChange={set('experienceLevel')} required>
                  <option value="">Select level</option>
                  {LEVELS.map((l) => <option key={l} value={l}>{l}</option>)}
                </select>
              </div>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
              <div className="form-group">
                <label>Position / Role</label>
                {form.sport ? (
                  <select value={form.position} onChange={set('position')} required>
                    <option value="">Select position</option>
                    {(POSITIONS[form.sport] || []).map((p) => <option key={p} value={p}>{p}</option>)}
                  </select>
                ) : (
                  <input placeholder="Select sport first" disabled />
                )}
              </div>
              <div className="form-group">
                <label>Age (optional)</label>
                <input type="number" placeholder="e.g. 22" value={form.age} onChange={set('age')} min={5} max={100} />
              </div>
            </div>

            <button type="submit" className="btn btn-primary btn-full" disabled={loading}>
              {loading ? <><span className="spinner" /> Creating account...</> : 'Create Account'}
            </button>
          </form>

          <p style={{ textAlign: 'center', marginTop: '1rem', color: '#8b90a8', fontSize: '.9rem' }}>
            Already have an account? <Link to="/login">Sign in</Link>
          </p>
        </div>
      </div>
    </div>
  );
}
