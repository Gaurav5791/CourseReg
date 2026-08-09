Auth.requireRole('REGISTRAR');
const username = Auth.getUsername();
document.getElementById('whoami').textContent = username;
document.getElementById('userAvatar').textContent = username.charAt(0).toUpperCase();

let registrarCoursesById = {};
let courseToRemove = null;

function setError(id, message) {
  const el = document.getElementById(id);
  if (!message) { 
    el.classList.add('hidden'); 
    el.textContent = '';
    return; 
  }
  el.textContent = message;
  el.classList.remove('hidden');
}

function switchTab(tab) {
  // Update nav (matches on data-tab, same approach student.js uses —
  // previously this matched on the nav item's visible text, which
  // happened to work but would break the moment a label changed)
  document.querySelectorAll('.nav-item').forEach(el => {
    el.classList.toggle('active', el.dataset.tab === tab);
  });
  
  // Update panels
  document.querySelectorAll('.tab-panel').forEach(el => {
    el.classList.remove('active');
  });
  document.getElementById('panel-' + tab).classList.add('active');
  
  // Update header
  const titles = {
    enrollments: ['Pending Enrollments', 'Review and approve student enrollment requests'],
    drops: ['Pending Drops', 'Review and approve student drop requests'],
    courses: ['All Courses', 'View and manage the course catalog']
  };
  document.getElementById('pageTitle').textContent = titles[tab][0];
  document.getElementById('pageSubtitle').textContent = titles[tab][1];
  
  // Load data
  if (tab === 'enrollments') loadPendingEnrollments();
  if (tab === 'drops') loadPendingDrops();
  if (tab === 'courses') loadCourses();
}

// ============================================================
// PENDING ENROLLMENTS
// ============================================================

async function loadPendingEnrollments() {
  setError('enrollError', null);
  try {
    const rows = await api('/registrar/enrollments/pending');
    const body = document.getElementById('enrollBody');

    if (rows.length === 0) {
      body.innerHTML = '<tr><td colspan="5" class="empty-cell">✅ No pending enrollment requests</td></tr>';
      return;
    }

    body.innerHTML = rows.map(r => `
      <tr>
        <td><strong>${escapeHtml(r.studentName)}</strong></td>
        <td><span class="course-code">${escapeHtml(r.courseCode)}</span></td>
        <td>${escapeHtml(r.courseTitle)}</td>
        <td class="timestamp">${r.requestedAt}</td>
        <td>
          <button class="btn btn-success btn-small" onclick="decide('enrollments', ${r.id}, 'approve', this)"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="vertical-align:-2px; margin-right:5px;"><path d="M20 6 9 17l-5-5"/></svg>Approve</button>
          <button class="btn btn-danger btn-small" onclick="decide('enrollments', ${r.id}, 'reject', this)"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="vertical-align:-2px; margin-right:5px;"><path d="M18 6 6 18M6 6l12 12"/></svg>Reject</button>
        </td>
      </tr>
    `).join('');
  } catch (err) {
    setError('enrollError', err.message);
  }
}

// ============================================================
// PENDING DROPS
// ============================================================

async function loadPendingDrops() {
  setError('dropError', null);
  try {
    const rows = await api('/registrar/drops/pending');
    const body = document.getElementById('dropBody');

    if (rows.length === 0) {
      body.innerHTML = '<tr><td colspan="5" class="empty-cell">✅ No pending drop requests</td></tr>';
      return;
    }

    body.innerHTML = rows.map(r => `
      <tr>
        <td><strong>${escapeHtml(r.studentName)}</strong></td>
        <td><span class="course-code">${escapeHtml(r.courseCode)}</span></td>
        <td>${escapeHtml(r.courseTitle)}</td>
        <td class="timestamp">${r.requestedAt}</td>
        <td>
          <button class="btn btn-success btn-small" onclick="decide('drops', ${r.id}, 'approve', this)"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="vertical-align:-2px; margin-right:5px;"><path d="M20 6 9 17l-5-5"/></svg>Approve Drop</button>
          <button class="btn btn-danger btn-small" onclick="decide('drops', ${r.id}, 'reject', this)"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="vertical-align:-2px; margin-right:5px;"><path d="M18 6 6 18M6 6l12 12"/></svg>Keep Enrolled</button>
        </td>
      </tr>
    `).join('');
  } catch (err) {
    setError('dropError', err.message);
  }
}

// ============================================================
// DECIDE (Approve/Reject)
// ============================================================

async function decide(kind, id, action, btn) {
  btn.disabled = true;
  btn.textContent = '⏳';
  const errId = kind === 'enrollments' ? 'enrollError' : 'dropError';
  
  try {
    await api('/registrar/' + kind + '/' + id + '/' + action, { method: 'POST', body: {} });
    if (kind === 'enrollments') await loadPendingEnrollments();
    else await loadPendingDrops();
  } catch (err) {
    setError(errId, err.message);
    btn.disabled = false;
    btn.innerHTML = action === 'approve' ? '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="vertical-align:-2px; margin-right:5px;"><path d="M20 6 9 17l-5-5"/></svg>Approve' : '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="vertical-align:-2px; margin-right:5px;"><path d="M18 6 6 18M6 6l12 12"/></svg>Reject';
  }
}

// ============================================================
// ALL COURSES
// ============================================================

async function loadCourses() {
  setError('courseError', null);
  try {
    const courses = await api('/registrar/courses');
    registrarCoursesById = Object.fromEntries(courses.map(c => [c.id, c]));
    const body = document.getElementById('registrarCourseBody');

    if (courses.length === 0) {
      body.innerHTML = '<tr><td colspan="6" class="empty-cell">📭 No courses in the catalog</td></tr>';
      return;
    }

    body.innerHTML = courses.map(c => `
      <tr>
        <td><span class="course-code">${escapeHtml(c.code)}</span></td>
        <td><strong>${escapeHtml(c.title)}</strong></td>
        <td>${escapeHtml(c.instructorName)}</td>
        <td>${c.seatsTaken || 0} / ${c.capacity}</td>
        <td><span class="badge badge-${c.status.toLowerCase()}">${c.status}</span></td>
        <td>
          ${c.status === 'ACTIVE'
            ? `<button class="btn btn-danger btn-small" onclick="openConfirmModal(${c.id})"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 6h18"/><path d="M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6"/></svg>Remove</button>`
            : `<span class="status-muted">—</span>`
          }
        </td>
      </tr>
    `).join('');
  } catch (err) {
    setError('courseError', err.message);
  }
}

// ============================================================
// CONFIRM REMOVE MODAL
// ============================================================

function openConfirmModal(id) {
  const course = registrarCoursesById[id];
  if (!course) return;
  courseToRemove = course;
  document.getElementById('confirmText').textContent = 
    `Are you sure you want to remove "${course.code} - ${course.title}"? This will also drop all enrolled students. This action cannot be undone.`;
  document.getElementById('confirmModal').classList.remove('hidden');
}

function closeConfirmModal() {
  document.getElementById('confirmModal').classList.add('hidden');
  courseToRemove = null;
}

document.getElementById('confirmRemoveBtn').addEventListener('click', async () => {
  if (!courseToRemove) return;
  const btn = document.getElementById('confirmRemoveBtn');
  btn.disabled = true;
  btn.textContent = '⏳ Removing...';
  
  try {
    await api('/registrar/courses/' + courseToRemove.id, { method: 'DELETE' });
    closeConfirmModal();
    await loadCourses();
  } catch (err) {
    setError('courseError', err.message);
    closeConfirmModal();
  } finally {
    btn.disabled = false;
    btn.textContent = 'Remove Course';
  }
});

// Initial load - default to enrollments
loadPendingEnrollments();
