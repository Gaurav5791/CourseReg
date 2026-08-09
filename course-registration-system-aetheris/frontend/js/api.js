// Shared API helper. Change API_BASE if your backend runs somewhere else.
const API_BASE = 'http://localhost:8080/api';

const Auth = {
  getToken() { return localStorage.getItem('crs_token'); },
  getRole() { return localStorage.getItem('crs_role'); },
  getUsername() { return localStorage.getItem('crs_username'); },
  getFullName() { return localStorage.getItem('crs_fullname'); },

  save({ token, role, username, fullName }) {
    localStorage.setItem('crs_token', token);
    localStorage.setItem('crs_role', role);
    localStorage.setItem('crs_username', username);
    localStorage.setItem('crs_fullname', fullName);
  },

  clear() {
    localStorage.removeItem('crs_token');
    localStorage.removeItem('crs_role');
    localStorage.removeItem('crs_username');
    localStorage.removeItem('crs_fullname');
  },

  logout() {
    this.clear();
    window.location.href = 'index.html';
  },

  /** Redirects to the login page unless the user is signed in with the given role. */
  requireRole(role) {
    if (this.getToken() === null || this.getRole() !== role) {
      window.location.href = 'index.html';
    }
  }
};

/**
 * Calls the API and returns the parsed JSON body.
 * Throws an Error with a human-readable message on failure (network
 * error, non-2xx status, or a 401 which also clears the stored session).
 */
async function api(path, { method = 'GET', body } = {}) {
  const headers = { 'Content-Type': 'application/json' };
  const token = Auth.getToken();
  if (token) headers['Authorization'] = 'Bearer ' + token;

  let response;
  try {
    response = await fetch(API_BASE + path, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined
    });
  } catch (networkErr) {
    throw new Error('Could not reach the server. Is the backend running on ' + API_BASE + '?');
  }

  if (response.status === 204) return null;

  let data = null;
  try { data = await response.json(); } catch (_) { /* empty body */ }

  if (!response.ok) {
    if (response.status === 401) {
      Auth.clear();
    }
    const message = (data && data.error) ? data.error : ('Request failed (' + response.status + ')');
    throw new Error(message);
  }

  return data;
}

// Minimal HTML-escaping for free-text values (titles, names, remarks) that
// get interpolated into innerHTML template strings. Without this, a course
// title or instructor name containing '<' or '&' would corrupt the table
// markup — and in principle inject a script tag.
function escapeHtml(value) {
  const div = document.createElement('div');
  div.textContent = value == null ? '' : String(value);
  return div.innerHTML;
}

// ---------------------------------------------------------------------
// Shared icon set — small inline SVGs (stroke-based, inherits the
// button's text color via currentColor) used on every action button.
// Inline rather than a CDN icon font so nothing can fail to load, and the
// whole app still works completely offline.
// ---------------------------------------------------------------------
const ICONS = {
  edit:      '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.12 2.12 0 0 1 3 3L12 15l-4 1 1-4Z"/></svg>',
  trash:     '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 6h18"/><path d="M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6"/></svg>',
  plus:      '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 5v14M5 12h14"/></svg>',
  book:      '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2Z"/></svg>',
  quiz:      '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="9" y="2" width="6" height="4" rx="1"/><path d="M9 4H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6a2 2 0 0 0-2-2h-4"/><path d="m9 14 2 2 4-4"/></svg>',
  chart:     '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 3v18h18"/><path d="M18 17V9M13 17V5M8 17v-3"/></svg>',
  award:     '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="8" r="6"/><path d="M15.5 13.5 17 22l-5-3-5 3 1.5-8.5"/></svg>',
  compass:   '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="m16.24 7.76-2.12 6.36-6.36 2.12 2.12-6.36z"/></svg>',
  logout:    '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><path d="M16 17 21 12 16 7"/><path d="M21 12H9"/></svg>',
  x:         '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M18 6 6 18M6 6l12 12"/></svg>',
  check:     '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 6 9 17l-5-5"/></svg>',
  list:      '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M8 6h13M8 12h13M8 18h13M3 6h.01M3 12h.01M3 18h.01"/></svg>',
  calendar:  '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="4" width="18" height="18" rx="2"/><path d="M16 2v4M8 2v4M3 10h18"/></svg>',
  back:      '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m12 19-7-7 7-7M19 12H5"/></svg>',
  flag:      '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 22V4a1 1 0 0 1 1-1c1.5 1.5 4.5 1.5 6 0s4.5-1.5 6 0a1 1 0 0 1 1 1v10a1 1 0 0 1-1 1c-1.5-1.5-4.5-1.5-6 0s-4.5 1.5-6 0"/></svg>'
};

/** Icon + label together, spaced consistently — use inside a <button>. */
function iconLabel(name, label) {
  return `<span style="display:inline-flex; align-items:center; gap:6px; vertical-align:middle;">${ICONS[name] || ''}${label ? '<span>' + label + '</span>' : ''}</span>`;
}
