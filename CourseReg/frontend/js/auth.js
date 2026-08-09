// If already signed in, skip straight to the right dashboard.
(function redirectIfSignedIn() {
  const role = Auth.getRole();
  if (Auth.getToken() && role) {
    window.location.href = dashboardFor(role);
  }
})();

function dashboardFor(role) {
  if (role === 'STUDENT') return 'student.html';
  if (role === 'ADMIN') return 'admin.html';
  if (role === 'REGISTRAR') return 'registrar.html';
  return 'index.html';
}

function switchAuthTab(which) {
  document.getElementById('loginForm').classList.toggle('hidden', which !== 'login');
  document.getElementById('registerForm').classList.toggle('hidden', which !== 'register');
  document.querySelectorAll('.auth-tab').forEach(t => t.classList.toggle('active', t.dataset.tab === which));
  hideError();
}

function showError(message) {
  const el = document.getElementById('authError');
  el.textContent = message;
  el.classList.remove('hidden');
}

function hideError() {
  document.getElementById('authError').classList.add('hidden');
}

async function handleLogin(event) {
  event.preventDefault();
  hideError();
  const username = document.getElementById('loginUsername').value.trim();
  const password = document.getElementById('loginPassword').value;

  try {
    const auth = await api('/auth/login', { method: 'POST', body: { username, password } });
    Auth.save(auth);
    window.location.href = dashboardFor(auth.role);
  } catch (err) {
    showError(err.message);
  }
}

async function handleRegister(event) {
  event.preventDefault();
  hideError();
  const fullName = document.getElementById('regFullName').value.trim();
  const email = document.getElementById('regEmail').value.trim();
  const username = document.getElementById('regUsername').value.trim();
  const password = document.getElementById('regPassword').value;

  try {
    const auth = await api('/auth/register', { method: 'POST', body: { fullName, email, username, password } });
    Auth.save(auth);
    window.location.href = dashboardFor(auth.role);
  } catch (err) {
    showError(err.message);
  }
}
