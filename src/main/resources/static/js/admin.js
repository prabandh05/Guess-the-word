/* ============================================================
   admin.js — Admin reports dashboard logic
   ============================================================ */

document.addEventListener('DOMContentLoaded', () => {
  if (!requireAuth('ADMIN')) return;

  document.getElementById('admin-name').textContent = getUsername();
  document.getElementById('logout-btn').addEventListener('click', logout);

  // Set date picker default to today
  const datePicker = document.getElementById('report-date');
  datePicker.value  = new Date().toISOString().split('T')[0];

  document.getElementById('daily-report-btn').addEventListener('click', loadDailyReport);
  document.getElementById('user-report-btn').addEventListener('click', loadUserReport);

  // Load today's daily report and user list automatically on open
  loadDailyReport();
  loadUsersList();
});

// ── Daily Report ────────────────────────────────────────────────
async function loadDailyReport() {
  const date = document.getElementById('report-date').value;
  if (!date) { showToast('Please select a date.'); return; }

  const btn = document.getElementById('daily-report-btn');
  btn.disabled = true;
  btn.innerHTML = '<span class="spinner"></span>';

  const usersEl  = document.getElementById('stat-users');
  const correctEl= document.getElementById('stat-correct');

  try {
    const res  = await authFetch(`/admin/reports/daily?date=${date}`);
    const data = await res.json();

    if (!res.ok || !data.success) throw new Error(data.message || 'Failed to load report.');

    animateCount(usersEl,   0, data.data.distinctUsersPlayed);
    animateCount(correctEl, 0, data.data.correctGuesses);
  } catch (err) {
    showToast(err.message);
    usersEl.textContent   = '—';
    correctEl.textContent = '—';
  } finally {
    btn.disabled = false;
    btn.textContent = 'Load Report';
  }
}

// ── User Report ─────────────────────────────────────────────────
async function loadUserReport() {
  const userId = document.getElementById('user-id-input').value.trim();
  if (!userId) { showToast('Please enter a User ID.'); return; }

  const btn     = document.getElementById('user-report-btn');
  const results = document.getElementById('user-report-results');
  btn.disabled  = true;
  btn.innerHTML = '<span class="spinner"></span>';
  results.innerHTML = '';

  try {
    const res  = await authFetch(`/admin/reports/user/${userId}`);
    const data = await res.json();

    if (!res.ok || !data.success) throw new Error(data.message || 'User not found.');

    const { username, dailyStats } = data.data;

    if (!dailyStats || dailyStats.length === 0) {
      results.innerHTML = `
        <div class="empty-state">
          <div class="es-icon">🎮</div>
          <p><strong>${username}</strong> hasn't played any games yet.</p>
        </div>`;
      return;
    }

    // Header
    const header = document.createElement('p');
    header.style.cssText = 'font-size:0.85rem;color:var(--text-muted);margin-bottom:0.75rem;';
    header.innerHTML = `Activity for <strong style="color:var(--text)">${username}</strong> (ID: ${userId})`;
    results.appendChild(header);

    // Table
    const table = document.createElement('table');
    table.className = 'report-table';
    table.innerHTML = `
      <thead>
        <tr>
          <th>Date</th>
          <th>Words Tried</th>
          <th>Correct Guesses</th>
          <th>Rate</th>
        </tr>
      </thead>
      <tbody></tbody>`;

    const tbody = table.querySelector('tbody');
    dailyStats.forEach(stat => {
      const rate = stat.wordsTried > 0
        ? Math.round((stat.correctGuesses / stat.wordsTried) * 100)
        : 0;
      const badgeClass = rate >= 50 ? 'badge-green' : 'badge-orange';
      const tr = document.createElement('tr');
      tr.innerHTML = `
        <td>${stat.date}</td>
        <td>${stat.wordsTried}</td>
        <td>${stat.correctGuesses}</td>
        <td><span class="badge ${badgeClass}">${rate}%</span></td>`;
      tbody.appendChild(tr);
    });

    results.appendChild(table);
  } catch (err) {
    results.innerHTML = `<div class="empty-state"><div class="es-icon">⚠️</div><p>${err.message}</p></div>`;
  } finally {
    btn.disabled = false;
    btn.textContent = 'Load Report';
  }
}

// ── Animated counter ─────────────────────────────────────────────
function animateCount(el, from, to) {
  const duration = 600;
  const start    = performance.now();
  function step(now) {
    const progress = Math.min((now - start) / duration, 1);
    const eased    = 1 - Math.pow(1 - progress, 3);
    el.textContent = Math.round(from + (to - from) * eased);
    if (progress < 1) requestAnimationFrame(step);
  }
  requestAnimationFrame(step);
}

// ── Users List ──────────────────────────────────────────────────
async function loadUsersList() {
  const results = document.getElementById('users-list-results');
  
  try {
    const res = await authFetch('/admin/users');
    const data = await res.json();
    
    if (!res.ok || !data.success) throw new Error(data.message || 'Failed to load users.');
    
    const users = data.data;
    if (!users || users.length === 0) {
      results.innerHTML = `<div class="empty-state"><p>No users found.</p></div>`;
      return;
    }
    
    let html = `
      <table class="report-table" style="cursor: pointer;">
        <thead>
          <tr>
            <th>ID</th>
            <th>Username</th>
            <th>Role</th>
          </tr>
        </thead>
        <tbody>
    `;
    
    users.forEach(u => {
      html += `
        <tr onclick="selectUserForReport(${u.id})">
          <td>${u.id}</td>
          <td><strong>${u.username}</strong></td>
          <td><span class="badge ${u.role === 'ADMIN' ? 'badge-orange' : 'badge-green'}">${u.role}</span></td>
        </tr>
      `;
    });
    
    html += `</tbody></table>`;
    results.innerHTML = html;
  } catch (err) {
    results.innerHTML = `<div class="empty-state"><p>${err.message}</p></div>`;
  }
}

window.selectUserForReport = function(userId) {
  document.getElementById('user-id-input').value = userId;
  loadUserReport();
  // Scroll up to the report
  document.getElementById('user-report-btn').scrollIntoView({ behavior: 'smooth', block: 'center' });
};

