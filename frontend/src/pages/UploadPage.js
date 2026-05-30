import React, { useState, useRef, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { uploadStance, getSession } from '../services/api';
import { useAuth } from '../hooks/useAuth';

const sportEmoji = { CRICKET: '🏏', FOOTBALL: '⚽', BASKETBALL: '🏀', BADMINTON: '🏸' };

export default function UploadPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [file, setFile] = useState(null);
  const [preview, setPreview] = useState(null);
  const [dragging, setDragging] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [analysing, setAnalysing] = useState(false);
  const [error, setError] = useState('');
  const inputRef = useRef();

  const handleFile = useCallback((f) => {
    if (!f) return;
    if (!['image/jpeg', 'image/png', 'image/webp'].includes(f.type)) {
      setError('Only JPEG, PNG, or WebP images are accepted.');
      return;
    }
    if (f.size > 5 * 1024 * 1024) {
      setError('File must be under 5 MB.');
      return;
    }
    setError('');
    setFile(f);
    setPreview(URL.createObjectURL(f));
  }, []);

  const onDrop = (e) => { e.preventDefault(); setDragging(false); handleFile(e.dataTransfer.files[0]); };
  const onDragOver = (e) => { e.preventDefault(); setDragging(true); };
  const onDragLeave = () => setDragging(false);

  const pollUntilDone = async (sessionId) => {
    for (let i = 0; i < 40; i++) {      // max ~2 min
      await new Promise((r) => setTimeout(r, 3000));
      const res = await getSession(sessionId);
      const status = res.data.data.status;
      if (status === 'COMPLETED' || status === 'FAILED') {
        return res.data.data;
      }
    }
    throw new Error('Analysis timed out — please try again.');
  };

  const handleUpload = async () => {
    if (!file) return;
    setError('');
    setUploading(true);
    try {
      const res = await uploadStance(file);
      const sessionId = res.data.data.id;
      setUploading(false);
      setAnalysing(true);
      const session = await pollUntilDone(sessionId);
      if (session.status === 'FAILED') throw new Error(session.errorMessage || 'AI analysis failed');
      navigate(`/sessions/${sessionId}`);
    } catch (err) {
      setError(err.message || 'Upload failed. Please try again.');
      setUploading(false);
      setAnalysing(false);
    }
  };

  return (
    <div className="page">
      <div className="container" style={{ maxWidth: 600, margin: '0 auto' }}>
        <h1 style={{ fontSize: '1.5rem', fontWeight: 800, marginBottom: '.4rem' }}>
          {sportEmoji[user?.sport]} New Session
        </h1>
        <p style={{ color: '#8b90a8', marginBottom: '2rem' }}>
          Upload a photo of your {user?.position} stance for AI coaching feedback.
        </p>

        {error && <div className="alert alert-error">{error}</div>}

        {/* Drop zone */}
        <div
          onClick={() => !uploading && !analysing && inputRef.current.click()}
          onDrop={onDrop} onDragOver={onDragOver} onDragLeave={onDragLeave}
          style={{
            border: `2px dashed ${dragging ? '#6c63ff' : '#2e3250'}`,
            borderRadius: 12, padding: '2.5rem', textAlign: 'center',
            cursor: uploading || analysing ? 'default' : 'pointer',
            background: dragging ? 'rgba(108,99,255,.05)' : 'transparent',
            transition: 'all .2s', marginBottom: '1.5rem',
          }}
        >
          {preview ? (
            <img src={preview} alt="Preview" style={{ maxHeight: 320, maxWidth: '100%', borderRadius: 8, objectFit: 'contain' }} />
          ) : (
            <>
              <div style={{ fontSize: '2.5rem', marginBottom: '.75rem' }}>📸</div>
              <p style={{ fontWeight: 600, marginBottom: '.4rem' }}>Drop your photo here</p>
              <p style={{ color: '#8b90a8', fontSize: '.875rem' }}>or click to browse · JPEG / PNG · max 5 MB</p>
            </>
          )}
          <input ref={inputRef} type="file" accept="image/jpeg,image/png,image/webp" style={{ display: 'none' }} onChange={(e) => handleFile(e.target.files[0])} />
        </div>

        {/* Tips card */}
        <div className="card" style={{ marginBottom: '1.5rem', fontSize: '.875rem' }}>
          <p style={{ fontWeight: 600, marginBottom: '.5rem' }}>📋 Photo tips for best results</p>
          <ul style={{ paddingLeft: '1.2rem', color: '#8b90a8', lineHeight: 2 }}>
            <li>Full body visible in frame</li>
            <li>Good lighting — avoid harsh shadows</li>
            <li>Side or front angle showing your technique</li>
            <li>Steady, non-blurry shot</li>
          </ul>
        </div>

        {/* Status display while analysing */}
        {analysing && (
          <div className="card" style={{ textAlign: 'center', marginBottom: '1.5rem', padding: '2rem' }}>
            <div style={{ marginBottom: '1rem' }}>
              <div className="spinner" style={{ margin: '0 auto', width: 32, height: 32, borderWidth: 3 }} />
            </div>
            <p style={{ fontWeight: 600 }}>Ghost Coach is analysing your technique…</p>
            <p style={{ color: '#8b90a8', fontSize: '.875rem', marginTop: '.4rem' }}>This takes 15–30 seconds</p>
          </div>
        )}

        <button
          className="btn btn-primary btn-full"
          onClick={handleUpload}
          disabled={!file || uploading || analysing}
          style={{ fontSize: '1rem', padding: '.75rem' }}
        >
          {uploading ? <><span className="spinner" /> Uploading…</>
            : analysing ? <><span className="spinner" /> Analysing…</>
            : '🚀 Get AI Coaching Feedback'}
        </button>

        {preview && !uploading && !analysing && (
          <button className="btn btn-outline btn-full" onClick={() => { setFile(null); setPreview(null); }} style={{ marginTop: '.75rem' }}>
            Choose different photo
          </button>
        )}
      </div>
    </div>
  );
}
