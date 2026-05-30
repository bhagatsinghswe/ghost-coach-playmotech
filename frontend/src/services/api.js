import axios from 'axios';

const api = axios.create({
  baseURL: process.env.REACT_APP_API_URL || 'http://localhost:8080',
  timeout: 60000,
});

// Attach JWT to every request
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// On 401, clear session and redirect to login
api.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.location.href = '/login';
    }
    return Promise.reject(err);
  }
);

// ---- Auth ----
export const register = (data) => api.post('/api/auth/register', data);
export const login = (data) => api.post('/api/auth/login', data);

// ---- Sessions ----
export const uploadStance = (file) => {
  const form = new FormData();
  form.append('file', file);
  return api.post('/api/sessions/upload', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
};
export const getSessions = (page = 0, size = 10) =>
  api.get(`/api/sessions?page=${page}&size=${size}`);
export const getSession = (id) => api.get(`/api/sessions/${id}`);
export const getProgress = () => api.get('/api/sessions/progress');

// ---- Profile ----
export const getProfile = () => api.get('/api/profile');
export const updateProfile = (data) => api.patch('/api/profile', data);

// ---- Chat ----
export const sendChat = (sessionId, content) =>
  api.post(`/api/sessions/${sessionId}/chat`, { content });
export const getChatHistory = (sessionId) =>
  api.get(`/api/sessions/${sessionId}/chat`);

export default api;
