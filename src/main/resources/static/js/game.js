/* ============================================================
   game.js — Player game board logic
   ============================================================ */

// ── State ───────────────────────────────────────────────────────
let sessionId      = null;
let currentRow     = 0;
let currentCol     = 0;
let currentGuess   = [];
let gameOver       = false;
let targetWord     = null;   // revealed only on loss

const WORD_LEN  = 5;
const MAX_GUESS = 5;

// Keyboard state
const keyStates = {};

// ── Init ────────────────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', async () => {
  if (!requireAuth('PLAYER')) return;

  document.getElementById('player-name').textContent = getUsername();
  document.getElementById('logout-btn').addEventListener('click', logout);
  document.getElementById('new-game-btn').addEventListener('click', startNewGame);

  buildBoard();
  buildKeyboard();
  setupKeyboardInput();
  await startNewGame();
});

// ── Board ───────────────────────────────────────────────────────
function buildBoard() {
  const board = document.getElementById('board');
  board.innerHTML = '';
  for (let r = 0; r < MAX_GUESS; r++) {
    const row = document.createElement('div');
    row.className = 'guess-row';
    row.id = `row-${r}`;
    for (let c = 0; c < WORD_LEN; c++) {
      const tile = document.createElement('div');
      tile.className = 'tile';
      tile.id = `tile-${r}-${c}`;
      row.appendChild(tile);
    }
    board.appendChild(row);
  }
}

function resetBoard() {
  currentRow   = 0;
  currentCol   = 0;
  currentGuess = [];
  gameOver     = false;
  targetWord   = null;
  Object.keys(keyStates).forEach(k => delete keyStates[k]);
  buildBoard();
  buildKeyboard();
}

// ── Start / Init session ────────────────────────────────────────
async function startNewGame() {
  const btn   = document.getElementById('new-game-btn');
  const limit = document.getElementById('limit-banner');
  btn.disabled = true;
  btn.innerHTML = '<span class="spinner"></span>';

  try {
    const res  = await authFetch('/game/start', { method: 'POST' });
    const data = await res.json();

    if (!res.ok) {
      if (res.status === 400 && data.message && data.message.includes('Daily limit')) {
        limit.classList.add('visible');
        btn.disabled = true;
        btn.textContent = 'Limit Reached';
        return;
      }
      throw new Error(data.message || 'Failed to start game.');
    }

    limit.classList.remove('visible');
    sessionId = data.data.id;
    resetBoard();
    btn.disabled = false;
    btn.textContent = 'New Game';
    showToast('Game started! Guess the 5-letter word.');
  } catch (err) {
    showToast(err.message);
    btn.disabled = false;
    btn.textContent = 'New Game';
  }
}

// ── Input ───────────────────────────────────────────────────────
function setupKeyboardInput() {
  document.addEventListener('keydown', e => {
    if (gameOver) return;
    if (e.ctrlKey || e.metaKey || e.altKey) return;

    if (e.key === 'Enter')     { submitGuess(); return; }
    if (e.key === 'Backspace') { deleteLetter(); return; }
    if (/^[a-zA-Z]$/.test(e.key)) { addLetter(e.key.toUpperCase()); }
  });
}

function addLetter(letter) {
  if (!sessionId || gameOver || currentCol >= WORD_LEN) return;
  currentGuess.push(letter);
  const tile = getTile(currentRow, currentCol);
  tile.textContent = letter;
  tile.dataset.state = 'tbd';
  currentCol++;
}

function deleteLetter() {
  if (!sessionId || gameOver || currentCol <= 0) return;
  currentCol--;
  currentGuess.pop();
  const tile = getTile(currentRow, currentCol);
  tile.textContent = '';
  tile.dataset.state = '';
}

async function submitGuess() {
  if (!sessionId || gameOver) return;
  if (currentGuess.length < WORD_LEN) {
    shakeRow(currentRow);
    showToast('Not enough letters');
    return;
  }

  const guessText = currentGuess.join('');
  const submitBtn = document.querySelector('.key[data-key="ENTER"]');
  if (submitBtn) submitBtn.style.pointerEvents = 'none';

  try {
    const res  = await authFetch(`/game/${sessionId}/guess`, {
      method: 'POST',
      body:   JSON.stringify({ guessText }),
    });
    const data = await res.json();

    if (!res.ok) {
      shakeRow(currentRow);
      showToast(data.message || 'Invalid guess.');
      if (submitBtn) submitBtn.style.pointerEvents = '';
      return;
    }

    const pattern = data.data.resultPattern.split(','); // ['G','O','X','G','G']
    await revealRow(currentRow, guessText, pattern);

    // Update keyboard
    guessText.split('').forEach((letter, i) => {
      const newState = patternToState(pattern[i]);
      const existing = keyStates[letter];
      // Green > Orange > Grey (don't downgrade)
      if (!existing || statePriority(newState) > statePriority(existing)) {
        keyStates[letter] = newState;
        const key = document.querySelector(`.key[data-key="${letter}"]`);
        if (key) key.dataset.state = newState;
      }
    });

    currentRow++;
    currentGuess = [];
    currentCol   = 0;

    // Check win/loss
    const won  = pattern.every(p => p === 'G');
    const lost = !won && currentRow >= MAX_GUESS;

    if (won) {
      gameOver = true;
      bounceRow(currentRow - 1);
      setTimeout(() => {
        launchConfetti();
        showWinModal(currentRow - 1);
      }, 400);
    } else if (lost) {
      gameOver = true;
      // Fetch session to get the word revealed in modal
      const sessionRes  = await authFetch(`/game/${sessionId}`);
      const sessionData = await sessionRes.json();
      // Backend doesn't expose the word directly, show generic message
      setTimeout(() => showLossModal(), 300);
    }
  } catch (err) {
    showToast(err.message);
  } finally {
    if (submitBtn) submitBtn.style.pointerEvents = '';
  }
}

// ── Animations ──────────────────────────────────────────────────
function revealRow(row, word, pattern) {
  return new Promise(resolve => {
    pattern.forEach((code, col) => {
      const tile = getTile(row, col);
      const delay = col * 300;
      setTimeout(() => {
        tile.classList.add('flip');
        // At the mid-point of the flip, apply the color
        setTimeout(() => {
          tile.dataset.state = patternToState(code);
        }, 250);
        if (col === WORD_LEN - 1) {
          setTimeout(resolve, 300);
        }
      }, delay);
    });
  });
}

function bounceRow(row) {
  for (let c = 0; c < WORD_LEN; c++) {
    const tile = getTile(row, c);
    setTimeout(() => tile.classList.add('bounce'), c * 100);
  }
}

function shakeRow(row) {
  const rowEl = document.getElementById(`row-${row}`);
  rowEl.classList.remove('shake');
  void rowEl.offsetWidth;
  rowEl.classList.add('shake');
}

// ── Modals ──────────────────────────────────────────────────────
function showWinModal(attemptsRow) {
  const attempts = attemptsRow + 1;
  const labels   = ['','Genius!','Magnificent!','Impressive!','Splendid!','Great!'];
  document.getElementById('win-title').textContent   = labels[attempts] || 'Congratulations!';
  document.getElementById('win-attempts').textContent = `You got it in ${attempts} ${attempts === 1 ? 'guess' : 'guesses'}!`;
  openModal('win-modal');
}

function showLossModal() {
  openModal('loss-modal');
}

function openModal(id) {
  const backdrop = document.getElementById(id);
  backdrop.classList.add('active');
  backdrop.querySelector('.btn')?.focus();
}

function closeModal(id) {
  document.getElementById(id).classList.remove('active');
}

// ── Keyboard UI ─────────────────────────────────────────────────
const KEYBOARD_ROWS = [
  ['Q','W','E','R','T','Y','U','I','O','P'],
  ['A','S','D','F','G','H','J','K','L'],
  ['ENTER','Z','X','C','V','B','N','M','⌫'],
];

function buildKeyboard() {
  const kb = document.getElementById('keyboard');
  kb.innerHTML = '';

  KEYBOARD_ROWS.forEach(row => {
    const rowEl = document.createElement('div');
    rowEl.className = 'keyboard-row';

    row.forEach(key => {
      const btn = document.createElement('button');
      btn.className = 'key' + (key.length > 1 ? ' wide' : '');
      btn.textContent = key;
      btn.dataset.key = key;
      if (keyStates[key]) btn.dataset.state = keyStates[key];

      btn.addEventListener('click', () => {
        if (key === 'ENTER') submitGuess();
        else if (key === '⌫') deleteLetter();
        else addLetter(key);
      });

      rowEl.appendChild(btn);
    });

    kb.appendChild(rowEl);
  });
}

// ── Confetti ─────────────────────────────────────────────────────
function launchConfetti() {
  const canvas = document.getElementById('confetti-canvas');
  const ctx    = canvas.getContext('2d');
  canvas.width  = window.innerWidth;
  canvas.height = window.innerHeight;

  const pieces = Array.from({ length: 120 }, () => ({
    x:    Math.random() * canvas.width,
    y:    Math.random() * -canvas.height,
    r:    Math.random() * 8 + 4,
    d:    Math.random() * 3 + 2,
    color: ['#538d4e','#b59f3b','#6aaa64','#f0f0f0','#c9b458'][Math.floor(Math.random()*5)],
    tilt: Math.random() * 20 - 10,
    ts:   Math.random() * 0.04 + 0.02,
    t:    Math.random() * Math.PI * 2,
  }));

  let frame;
  function draw() {
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    pieces.forEach(p => {
      p.t    += p.ts;
      p.y    += p.d;
      p.tilt  = Math.sin(p.t) * 15;
      ctx.beginPath();
      ctx.save();
      ctx.translate(p.x + p.r, p.y + p.r);
      ctx.rotate((p.tilt * Math.PI) / 180);
      ctx.fillStyle = p.color;
      ctx.fillRect(-p.r, -p.r, p.r * 2, p.r * 0.6);
      ctx.restore();
    });
    if (pieces.some(p => p.y < canvas.height)) {
      frame = requestAnimationFrame(draw);
    } else {
      ctx.clearRect(0, 0, canvas.width, canvas.height);
    }
  }
  draw();
  setTimeout(() => { cancelAnimationFrame(frame); ctx.clearRect(0, 0, canvas.width, canvas.height); }, 4000);
}

// ── Helpers ─────────────────────────────────────────────────────
function getTile(row, col) { return document.getElementById(`tile-${row}-${col}`); }

function patternToState(code) {
  if (code === 'G') return 'correct';
  if (code === 'O') return 'present';
  return 'absent';
}

function statePriority(state) {
  if (state === 'correct') return 3;
  if (state === 'present') return 2;
  return 1;
}
