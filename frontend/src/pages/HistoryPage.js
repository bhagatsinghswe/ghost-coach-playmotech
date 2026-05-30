import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { getSessions, getProgress } from '../services/api';
import { LineChart, Line, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid } from 'recharts';

const StatusBadge = ({ status }) => {
  const map = { COMPLETED: ['badge-success', '✓ Done'], PROCESSING: ['badge-warning', '⏳ Processing'], PENDING: ['badge-warning', '⏳ Pending'], FAILED: ['badge-danger', '✗ Failed'] };
  const [cls, label] = map[status] || ['badge-info', status];
  return <span className={`badge ${cls}`}>{label}</span>;
};

export default function HistoryPage() {
  const [sessions, setSessions] = useState([]);
  const [progress, setProgress] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    (async () => {
      setLoading(true);
      try {
        const [sRes, pRes] = await Promise.all([
          getSessions(page, 10),
          getProgress().catch(() => ({ data: { data: [] } })),
        ]);
        setSessions(sRes.data.data.content);
        setTotalPages(sRes.data.data.totalPages);
        const pts = pRes.data.data.map((p) => ({
          date: new Date(p.date).toLocaleDateString('en', { month: 'short', day: 'numeric' }),
          score: p.score,
        }));
        setProgress(pts);
      } catch (e) { console.error(e); }
      finally { setLoading(false); }
    })();
  }, [page]);

  return (
    <div className="page">
      <div className="container">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
          <h1 style={{ fontSize: '1.5rem', fontWeight: 800 }}>Session History</h1>
          <Link to="/upload" className="btn btn-primary">+ New Session</Link>
        </div>

        {/* Progress Chart (bonus) */}
        {progress.length > 1 && (
          <div className="card" style={{ marginBottom: '1.5rem' }}>
            <h3 style={{ marginBottom: '1rem', fontSize: '1rem' }}>📈 Score Progress</h3>
            <ResponsiveContainer width="100%" height={180}>
              <LineChart data={progress} margin={{ top: 5, right: 20, bottom: 5, left: 0 }}>
                <CartesianGrid stroke="#2e3250" strokeDasharray="3 3" />
                <XAxis dataKey="date" tick={{ fill: '#8b90a8', fontSize: 11 }} />
                <YAxis domain={[0, 10]} tick={{ fill: '#8b90a8', fontSize: 11 }} />
                <Tooltip contentStyle={{ background: '#1a1d27', border: '1px solid #2e3250', borderRadius: 8 }} />
                <Line type="monotone" dataKey="score" stroke="#6c63ff" strokeWidth={2} dot={{ fill: '#6c63ff', r: 4 }} />
              </LineChart>
            </ResponsiveContainer>
          </div>
        )}

        {/* Session cards */}
        {loading ? (
          <div style={{ textAlign: 'center', paddingTop: '3rem' }}><div className="spinner" style={{ margin: '0 auto', width: 32, height: 32 }} /></div>
        ) : sessions.length === 0 ? (
          <div className="card" style={{ textAlign: 'center', padding: '3rem' }}>
            <div style={{ fontSize: '2.5rem', marginBottom: '1rem' }}>📸</div>
            <p style={{ fontWeight: 600 }}>No sessions yet</p>
            <p style={{ color: '#8b90a8', marginBottom: '1.5rem' }}>Upload your first stance photo to get AI coaching feedback</p>
            <Link to="/upload" className="btn btn-primary">Upload Your First Photo</Link>
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            {sessions.map((s) => (
              <Link key={s.id} to={`/sessions/${s.id}`} style={{ textDecoration: 'none' }}>
                <div className="card" style={{ display: 'flex', gap: '1rem', alignItems: 'center', cursor: 'pointer', transition: 'border-color .2s' }}
                  onMouseEnter={(e) => e.currentTarget.style.borderColor = '#6c63ff'}
                  onMouseLeave={(e) => e.currentTarget.style.borderColor = '#2e3250'}>
                  <img src={s.thumbnailUrl} alt="Stance" style={{ width: 80, height: 80, objectFit: 'cover', borderRadius: 8, flexShrink: 0 }} />
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '.25rem' }}>
                      <span style={{ fontSize: '.85rem', color: '#8b90a8' }}>
                        {new Date(s.uploadedAt).toLocaleString('en', { dateStyle: 'medium', timeStyle: 'short' })}
                      </span>
                      <StatusBadge status={s.status} />
                    </div>
                    {s.overallScore != null && (
                      <div style={{ fontWeight: 800, fontSize: '1.3rem', color: '#6c63ff', marginBottom: '.2rem' }}>
                        {s.overallScore}<span style={{ fontSize: '.8rem', color: '#8b90a8' }}>/10</span>
                      </div>
                    )}
                    {s.priorityFix && (
                      <p style={{ fontSize: '.875rem', color: '#e8eaf0', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                        🎯 {s.priorityFix}
                      </p>
                    )}
                  </div>
                  <span style={{ color: '#8b90a8', fontSize: '1.2rem' }}>›</span>
                </div>
              </Link>
            ))}
          </div>
        )}

        {/* Pagination */}
        {totalPages > 1 && (
          <div style={{ display: 'flex', justifyContent: 'center', gap: '.75rem', marginTop: '1.5rem' }}>
            <button className="btn btn-outline" onClick={() => setPage(p => p - 1)} disabled={page === 0}>← Prev</button>
            <span style={{ alignSelf: 'center', color: '#8b90a8' }}>Page {page + 1} of {totalPages}</span>
            <button className="btn btn-outline" onClick={() => setPage(p => p + 1)} disabled={page >= totalPages - 1}>Next →</button>
          </div>
        )}
      </div>
    </div>
  );
}
