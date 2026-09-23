/* ============================================================
   api.js — Shared API utilities and JWT management
   ============================================================ */

const API_BASE = '/api';

// ── Token helpers ──────────────────────────────────────────────
function getToken()          { return localStorage.getItem('gtw_token'); }
function getRole()           { return localStorage.getItem('gtw_role'); }
function getUsername()       { return localStorage.getItem('gtw_username'); }
function getUserId()         { return localStorage.getItem('gtw_userId'); }

function setSession(token, role, username, userId) {
  localStorage.setItem('gtw_token',    token);
  localStorage.setItem('gtw_role',     role);
  localStorage.setItem('gtw_username', username);
  localStorage.setItem('gtw_userId',   userId || '');
}

function clearSession() {
  ['gtw_token','gtw_role','gtw_username','gtw_userId'].forEach(k => localStorage.removeItem(k));
}

function requireAuth(expectedRole) {
  const token = getToken();
  const role  = getRole();
  if (!token) { window.location.href = '/index.html'; return false; }
  if (expectedRole && role !== expectedRole) {
    clearSession();
    window.location.href = '/index.html';
    return false;
  }
  return true;
}

function logout() {
  clearSession();
  window.location.href = '/index.html';
}

// ── Fetch wrapper ──────────────────────────────────────────────
async function authFetch(url, options = {}) {
  const token = getToken();
  const headers = {
    'Content-Type': 'application/json',
    ...(token ? { 'Authorization': `Bearer ${token}` } : {}),
    ...(options.headers || {}),
  };

  const response = await fetch(API_BASE + url, { ...options, headers });

  if (response.status === 401) {
    clearSession();
    window.location.href = '/index.html';
    throw new Error('Session expired. Please log in again.');
  }

  return response;
}

// ── Toast ──────────────────────────────────────────────────────
function showToast(message, duration = 2000) {
  let container = document.getElementById('toast-container');
  if (!container) {
    container = document.createElement('div');
    container.id = 'toast-container';
    document.body.appendChild(container);
  }
  const toast = document.createElement('div');
  toast.className = 'toast';
  toast.textContent = message;
  container.appendChild(toast);
  setTimeout(() => toast.remove(), duration);
}
