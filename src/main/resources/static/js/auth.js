/* ============================================================
   auth.js — Login / Register page logic
   ============================================================ */

document.addEventListener('DOMContentLoaded', () => {
  // Redirect already-logged-in users
  if (getToken()) {
    redirectByRole(getRole());
    return;
  }

  setupTabs();
  setupLoginForm();
  setupRegisterForm();
});

function redirectByRole(role) {
  if (role === 'ADMIN')  window.location.href = '/admin.html';
  else                   window.location.href = '/game.html';
}

// ── Tab switching ───────────────────────────────────────────────
function setupTabs() {
  const tabs  = document.querySelectorAll('.auth-tab');
  const forms = document.querySelectorAll('.auth-form');

  tabs.forEach(tab => {
    tab.addEventListener('click', () => {
      tabs.forEach(t  => t.classList.remove('active'));
      forms.forEach(f => f.classList.remove('active'));
      tab.classList.add('active');
      document.getElementById(`${tab.dataset.target}-form`).classList.add('active');
    });
  });
}

// ── Login ───────────────────────────────────────────────────────
function setupLoginForm() {
  const form   = document.getElementById('login-form');
  const btn    = document.getElementById('login-btn');
  const errEl  = document.getElementById('login-error');

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    errEl.textContent = '';
    btn.disabled = true;
    btn.innerHTML = '<span class="spinner"></span>';

    const username = form.username.value.trim();
    const password = form.password.value;

    try {
      const res  = await fetch('/api/auth/login', {
        method:  'POST',
        headers: { 'Content-Type': 'application/json' },
        body:    JSON.stringify({ username, password }),
      });
      const data = await res.json();

      if (!res.ok || !data.success) {
        throw new Error(data.message || 'Invalid credentials.');
      }

      setSession(data.data.token, data.data.role, data.data.username, data.data.userId);
      redirectByRole(data.data.role);
    } catch (err) {
      errEl.textContent = err.message;
      btn.disabled = false;
      btn.textContent = 'Sign In';
    }
  });
}

// ── Register ────────────────────────────────────────────────────
function setupRegisterForm() {
  const form    = document.getElementById('register-form');
  const btn     = document.getElementById('register-btn');
  const errEl   = document.getElementById('register-error');
  const unameEl = document.getElementById('reg-username');
  const passEl  = document.getElementById('reg-password');

  // Live validation
  unameEl.addEventListener('input', () => {
    const v = unameEl.value;
    const ok = /^(?=.*[a-z])(?=.*[A-Z])[A-Za-z]{5,}$/.test(v);
    document.getElementById('uname-hint').style.color = v.length === 0 ? '' : (ok ? '#538d4e' : '#e74c3c');
  });

  passEl.addEventListener('input', () => {
    const v = passEl.value;
    const ok = /^(?=.*[a-zA-Z])(?=.*\d)(?=.*[$%*&]).{5,}$/.test(v);
    document.getElementById('pass-hint').style.color = v.length === 0 ? '' : (ok ? '#538d4e' : '#e74c3c');
  });

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    errEl.textContent = '';
    btn.disabled = true;
    btn.innerHTML = '<span class="spinner"></span>';

    const username = unameEl.value.trim();
    const password = passEl.value;

    try {
      const res  = await fetch('/api/auth/register', {
        method:  'POST',
        headers: { 'Content-Type': 'application/json' },
        body:    JSON.stringify({ username, password, role: 'PLAYER' }),
      });
      const data = await res.json();

      if (!res.ok || !data.success) {
        const msg = data.data
          ? Object.values(data.data).join(' ')
          : (data.message || 'Registration failed.');
        throw new Error(msg);
      }

      setSession(data.data.token, data.data.role, data.data.username, data.data.userId);
      redirectByRole(data.data.role);
    } catch (err) {
      errEl.textContent = err.message;
      btn.disabled = false;
      btn.textContent = 'Create Account';
    }
  });
}
