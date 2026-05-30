import React, { useState, useEffect } from 'react';
import { useAuth } from '../hooks/useAuth';
import { getProfile, updateProfile } from '../services/api';

const SPORTS = ['CRICKET', 'FOOTBALL', 'BASKETBALL', 'BADMINTON'];
const LEVELS = ['BEGINNER', 'INTERMEDIATE', 'ADVANCED'];
const sportEmoji = { CRICKET: '🏏', FOOTBALL: '⚽', BASKETBALL: '🏀', BADMINTON: '🏸' };

export default function ProfilePage() {
  const { user, login } = useAuth();
  const [profile, setProfile] = useState(null);
  const [editing, setEditing] = useState(false);
  const [form, setForm] = useState({});
  const [saving, setSaving] = useState(false);
  const [success, setSuccess] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    getProfile().then((res) => {
      setProfile(res.data.data);
      setForm({
        fullName: res.data.data.fullName,
        sport: res.data.data.sport,
        position: res.data.data.position,
        experienceLevel: res.data.data.experienceLevel,
        age: res.data.data.age || '',
      });
    }).catch(() => setError('Could not load profile.'));
  }, []);

  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }));

  const handleSave = async (e) => {
    e.preventDefault();
    setSaving(true);
    setError('');
    try {
      await updateProfile({ ...form, age: form.age ? parseInt(form.age) : undefined });
      const res = await getProfile();
      setProfile(res.data.data);
      setEditing(false);
      setSuccess('Profile updated successfully!');
      setTimeout(() => setSuccess(''), 3000);
    } catch (err) {
      setError(err.response?.data?.message || 'Update failed.');
    } finally {
      setSaving(false);
    }
  };

  if (!profile) return (
    <div className="page container" style={{ textAlign: 'center', paddingTop: '4rem' }}>
      <div className="spinner" style={{ margin: '0 auto', width: 32, height: 32 }} />
    </div>
  );

  return (
    <div className="page">
      <div className="container" style={{ maxWidth: 580, margin: '0 auto' }}>
        <h1 style={{ fontSize: '1.5rem', fontWeight: 800, marginBottom: '1.5rem' }}>My Profile</h1>

        {success && <div className="alert alert-success">{success}</div>}
        {error && <div className="alert alert-error">{error}</div>}

        {/* Stats bar */}
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '1rem', marginBottom: '1.5rem' }}>
          {[
            { label: 'Sport', value: `${sportEmoji[profile.sport]} ${profile.sport}` },
            { label: 'Sessions', value: profile.totalSessions },
            { label: 'Level', value: profile.experienceLevel },
          ].map(({ label, value }) => (
            <div key={label} className="card" style={{ textAlign: 'center' }}>
              <div style={{ fontSize: '1.2rem', fontWeight: 800, color: '#6c63ff' }}>{value}</div>
              <div style={{ fontSize: '.75rem', color: '#8b90a8', marginTop: '.25rem' }}>{label}</div>
            </div>
          ))}
        </div>

        <div className="card">
          {!editing ? (
            <>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem' }}>
                <h3 style={{ fontWeight: 700 }}>Player Details</h3>
                <button className="btn btn-outline" onClick={() => setEditing(true)} style={{ padding: '.35rem .9rem', fontSize: '.85rem' }}>
                  Edit
                </button>
              </div>
              {[
                ['Full Name', profile.fullName],
                ['Email', profile.email],
                ['Sport', `${sportEmoji[profile.sport]} ${profile.sport}`],
                ['Position', profile.position],
                ['Experience', profile.experienceLevel],
                ['Age', profile.age || '—'],
                ['Member Since', new Date(profile.memberSince).toLocaleDateString('en', { dateStyle: 'medium' })],
              ].map(([label, value]) => (
                <div key={label} style={{ display: 'flex', justifyContent: 'space-between', padding: '.6rem 0', borderBottom: '1px solid #2e3250' }}>
                  <span style={{ color: '#8b90a8', fontSize: '.9rem' }}>{label}</span>
                  <span style={{ fontWeight: 500, fontSize: '.9rem' }}>{value}</span>
                </div>
              ))}
            </>
          ) : (
            <form onSubmit={handleSave}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem' }}>
                <h3 style={{ fontWeight: 700 }}>Edit Profile</h3>
                <button type="button" className="btn btn-outline" onClick={() => setEditing(false)} style={{ padding: '.35rem .9rem', fontSize: '.85rem' }}>
                  Cancel
                </button>
              </div>

              <div className="form-group">
                <label>Full Name</label>
                <input value={form.fullName} onChange={set('fullName')} required />
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
                <div className="form-group">
                  <label>Sport</label>
                  <select value={form.sport} onChange={set('sport')}>
                    {SPORTS.map((s) => <option key={s} value={s}>{s}</option>)}
                  </select>
                </div>
                <div className="form-group">
                  <label>Experience Level</label>
                  <select value={form.experienceLevel} onChange={set('experienceLevel')}>
                    {LEVELS.map((l) => <option key={l} value={l}>{l}</option>)}
                  </select>
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
                <div className="form-group">
                  <label>Position</label>
                  <input value={form.position} onChange={set('position')} required />
                </div>
                <div className="form-group">
                  <label>Age</label>
                  <input type="number" value={form.age} onChange={set('age')} min={5} max={100} />
                </div>
              </div>

              <button type="submit" className="btn btn-primary btn-full" disabled={saving}>
                {saving ? <><span className="spinner" /> Saving…</> : 'Save Changes'}
              </button>
            </form>
          )}
        </div>
      </div>
    </div>
  );
}
