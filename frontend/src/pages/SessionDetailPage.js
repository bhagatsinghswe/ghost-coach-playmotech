import React, { useState, useEffect, useRef } from 'react';
import { useParams, Link } from 'react-router-dom';
import { getSession, sendChat, getChatHistory } from '../services/api';

const confidenceColor = { HIGH: '#22c55e', MEDIUM: '#f59e0b', LOW: '#ef4444' };

export default function SessionDetailPage() {
  const { id } = useParams();
  const [session, setSession] = useState(null);
  const [chatMessages, setChatMessages] = useState([]);
  const [chatInput, setChatInput] = useState('');
  const [chatLoading, setChatLoading] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const chatEndRef = useRef();

  useEffect(() => {
    (async () => {
      try {
        const [sRes, cRes] = await Promise.all([getSession(id), getChatHistory(id).catch(() => ({ data: { data: [] } }))]);
        setSession(sRes.data.data);
        setChatMessages(cRes.data.data);
      } catch { setError('Could not load session.'); }
      finally { setLoading(false); }
    })();
  }, [id]);

  useEffect(() => { chatEndRef.current?.scrollIntoView({ behavior: 'smooth' }); }, [chatMessages]);

  const handleChat = async (e) => {
    e.preventDefault();
    if (!chatInput.trim() || chatLoading) return;
    const text = chatInput.trim();
    setChatInput('');
    setChatMessages((m) => [...m, { role: 'USER', content: text, id: Date.now() }]);
    setChatLoading(true);
    try {
      const res = await sendChat(id, text);
      setChatMessages((m) => [...m, res.data.data]);
    } catch { setChatMessages((m) => [...m, { role: 'ASSISTANT', content: 'Sorry, I could not respond right now.', id: Date.now() + 1 }]); }
    finally { setChatLoading(false); }
  };

  if (loading) return <div className="page container" style={{ textAlign: 'center', paddingTop: '4rem' }}><div className="spinner" style={{ margin: '0 auto', width: 36, height: 36 }} /></div>;
  if (error || !session) return <div className="page container"><div className="alert alert-error">{error || 'Session not found'}</div><Link to="/history">← Back to History</Link></div>;

  const { feedback } = session;

  return (
    <div className="page">
      <div className="container">
        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', marginBottom: '1.5rem' }}>
          <Link to="/history" style={{ color: '#8b90a8', textDecoration: 'none' }}>← History</Link>
          <h1 style={{ fontSize: '1.3rem', fontWeight: 800 }}>Session #{session.id}</h1>
          <span className="badge badge-info">{new Date(session.createdAt).toLocaleDateString()}</span>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1.6fr', gap: '1.5rem' }}>
          {/* Left — image + score */}
          <div>
            <div className="card" style={{ marginBottom: '1rem' }}>
              <img src={session.imageUrl} alt="Stance" style={{ width: '100%', borderRadius: 8, objectFit: 'cover', maxHeight: 320 }} />
            </div>

            {feedback && (
              <div className="card" style={{ textAlign: 'center' }}>
                <div style={{ fontSize: '.8rem', color: '#8b90a8', marginBottom: '.25rem' }}>OVERALL SCORE</div>
                <div className="score" style={{ fontSize: '3rem' }}>{feedback.overallScore}<span style={{ fontSize: '1.2rem', color: '#8b90a8' }}>/10</span></div>
                <div style={{ marginTop: '.5rem' }}>
                  <span style={{ fontSize: '.8rem', color: confidenceColor[feedback.confidenceLevel], fontWeight: 600 }}>
                    ● {feedback.confidenceLevel} confidence
                  </span>
                </div>
              </div>
            )}
          </div>

          {/* Right — feedback */}
          <div>
            {feedback ? (
              <>
                <div className="card" style={{ marginBottom: '1rem' }}>
                  <h3 style={{ marginBottom: '.75rem', color: '#22c55e' }}>✅ Strengths</h3>
                  {feedback.strengths.map((s, i) => (
                    <div key={i} style={{ display: 'flex', gap: '.5rem', marginBottom: '.5rem' }}>
                      <span style={{ color: '#22c55e', marginTop: '.1rem' }}>•</span>
                      <span style={{ fontSize: '.9rem' }}>{s}</span>
                    </div>
                  ))}
                </div>

                <div className="card" style={{ marginBottom: '1rem' }}>
                  <h3 style={{ marginBottom: '.75rem', color: '#f59e0b' }}>⚠️ Areas to Improve</h3>
                  {feedback.areasToImprove.map((a, i) => (
                    <div key={i} style={{ display: 'flex', gap: '.5rem', marginBottom: '.5rem' }}>
                      <span style={{ color: '#f59e0b', marginTop: '.1rem' }}>•</span>
                      <span style={{ fontSize: '.9rem' }}>{a}</span>
                    </div>
                  ))}
                </div>

                <div className="card" style={{ marginBottom: '1rem', borderLeft: '3px solid #6c63ff' }}>
                  <h3 style={{ marginBottom: '.5rem', color: '#6c63ff' }}>🎯 Priority Fix</h3>
                  <p style={{ fontSize: '.95rem' }}>{feedback.priorityFix}</p>
                </div>

                <div className="card" style={{ background: 'rgba(34,197,94,.05)', borderColor: 'rgba(34,197,94,.2)' }}>
                  <h3 style={{ marginBottom: '.5rem', color: '#22c55e' }}>🏋️ Drill Suggestion</h3>
                  <p style={{ fontSize: '.95rem' }}>{feedback.drillSuggestion}</p>
                </div>
              </>
            ) : (
              <div className="card" style={{ textAlign: 'center', padding: '2rem' }}>
                <div className="spinner" style={{ margin: '0 auto 1rem', width: 32, height: 32 }} />
                <p>AI analysis in progress…</p>
              </div>
            )}
          </div>
        </div>

        {/* Chat section */}
        {feedback && (
          <div className="card" style={{ marginTop: '1.5rem' }}>
            <h3 style={{ marginBottom: '1rem' }}>💬 Ask Ghost Coach</h3>
            <div style={{ maxHeight: 320, overflowY: 'auto', marginBottom: '1rem', display: 'flex', flexDirection: 'column', gap: '.75rem' }}>
              {chatMessages.length === 0 && (
                <p style={{ color: '#8b90a8', fontSize: '.875rem', textAlign: 'center', padding: '1rem 0' }}>
                  Ask a follow-up question about your technique…
                </p>
              )}
              {chatMessages.map((m, i) => (
                <div key={m.id || i} style={{ display: 'flex', justifyContent: m.role === 'USER' ? 'flex-end' : 'flex-start' }}>
                  <div style={{
                    maxWidth: '75%', padding: '.65rem .9rem', borderRadius: 10,
                    background: m.role === 'USER' ? '#6c63ff' : '#22263a',
                    fontSize: '.9rem', lineHeight: 1.5,
                  }}>
                    {m.content}
                  </div>
                </div>
              ))}
              {chatLoading && (
                <div style={{ display: 'flex', justifyContent: 'flex-start' }}>
                  <div style={{ background: '#22263a', borderRadius: 10, padding: '.65rem .9rem' }}>
                    <span className="spinner" style={{ display: 'inline-block', width: 14, height: 14, borderWidth: 2 }} />
                  </div>
                </div>
              )}
              <div ref={chatEndRef} />
            </div>
            <form onSubmit={handleChat} style={{ display: 'flex', gap: '.75rem' }}>
              <input
                value={chatInput} onChange={(e) => setChatInput(e.target.value)}
                placeholder="How do I fix my elbow position?"
                style={{ flex: 1 }} disabled={chatLoading}
              />
              <button type="submit" className="btn btn-primary" disabled={!chatInput.trim() || chatLoading}>Send</button>
            </form>
          </div>
        )}
      </div>
    </div>
  );
}
