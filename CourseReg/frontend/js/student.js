Auth.requireRole('STUDENT');
const username = Auth.getUsername();
document.getElementById('whoami').textContent = username;
document.getElementById('userAvatar').textContent = username.charAt(0).toUpperCase();

// Tab switching
function switchTab(tab) {
  // Update nav items
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
    catalog: ['Course Catalog', 'Browse courses and request enrollment'],
    mine: ['My Enrollments', 'Track your enrollment requests'],
    schedule: ['My Schedule', 'Your approved courses schedule'],
    paths: ['Career Paths', 'See which courses lead where you want to go'],
    badges: ['Badges', 'Achievements you\'ve earned along the way']
  };
  document.getElementById('pageTitle').textContent = titles[tab][0];
  document.getElementById('pageSubtitle').textContent = titles[tab][1];
  
  // Load data
  if (tab === 'catalog') loadCatalog();
  if (tab === 'mine') loadMine();
  if (tab === 'schedule') loadSchedule();
  if (tab === 'paths') { showPathsListView(); loadPaths(); }
  if (tab === 'badges') loadBadges();
}

function setError(id, message) {
  const el = document.getElementById(id);
  if (!message) { 
    el.classList.add('hidden'); 
    return; 
  }
  el.textContent = message;
  el.classList.remove('hidden');
}

function dayLabel(day) {
  return day.charAt(0) + day.slice(1).toLowerCase();
}

function fmtSchedule(course) {
  return dayLabel(course.dayOfWeek) + ' · ' + course.startTime.slice(0, 5) + '–' + course.endTime.slice(0, 5);
}

// ---------- Search ----------
let searchDebounce = null;
function debouncedSearch() {
  clearTimeout(searchDebounce);
  searchDebounce = setTimeout(loadCatalog, 300);
}

// ---------- Catalog ----------
async function loadCatalog() {
  setError('catalogError', null);
  const keyword = document.getElementById('searchKeyword').value.trim();
  const semester = document.getElementById('searchSemester').value.trim();
  const params = new URLSearchParams();
  if (keyword) params.set('keyword', keyword);
  if (semester) params.set('semester', semester);

  try {
    const courses = await api('/courses?' + params.toString());
    const body = document.getElementById('catalogBody');

    if (courses.length === 0) {
      body.innerHTML = `<tr><td colspan="7" class="empty-cell">📭 No courses match your search</td></tr>`;
      return;
    }

    body.innerHTML = courses.map(c => {
      const seatsAvailable = c.capacity - c.seatsTaken;
      const isFull = seatsAvailable <= 0;
      return `
      <tr>
        <td><span class="course-code">${escapeHtml(c.code)}</span></td>
        <td><strong>${escapeHtml(c.title)}</strong></td>
        <td>${escapeHtml(c.instructorName)}</td>
        <td>${fmtSchedule(c)}</td>
        <td>${c.credits}</td>
        <td>
          <span class="seats ${isFull ? 'seats-full' : ''}">
            ${seatsAvailable} / ${c.capacity}
          </span>
        </td>
        <td>
          <button class="btn ${isFull ? 'btn-outline' : 'btn-primary'} btn-small" 
                  ${isFull ? 'disabled' : ''}
                  onclick="requestEnroll(${c.id}, this)">
            ${isFull ? 'Full' : 'Request Seat'}
          </button>
        </td>
      </tr>
    `}).join('');
  } catch (err) {
    setError('catalogError', err.message);
  }
}

async function requestEnroll(courseId, btn) {
  btn.disabled = true;
  btn.textContent = '⏳ Requesting...';
  try {
    await api('/student/enrollments', { method: 'POST', body: { courseId } });
    btn.textContent = '✅ Requested!';
    setTimeout(() => { btn.textContent = 'Request Seat'; btn.disabled = false; }, 2000);
    await loadCatalog();
  } catch (err) {
    setError('catalogError', err.message);
    btn.textContent = 'Request Seat';
    btn.disabled = false;
  }
}

// ---------- My Enrollments ----------
async function loadMine() {
  setError('mineError', null);
  try {
    const rows = await api('/student/enrollments');
    const body = document.getElementById('mineBody');

    if (rows.length === 0) {
      body.innerHTML = `<tr><td colspan="6" class="empty-cell">📭 You haven't requested any courses yet</td></tr>`;
      return;
    }

    body.innerHTML = rows.map(r => {
      const statusMap = {
        'PENDING': 'badge-pending',
        'APPROVED': 'badge-approved',
        'REJECTED': 'badge-rejected',
        'DROP_PENDING': 'badge-drop_pending',
        'DROPPED': 'badge-dropped',
        'DROP_REJECTED': 'badge-drop_rejected'
      };
      const labelMap = {
        'PENDING': 'Pending',
        'APPROVED': 'Approved',
        'REJECTED': 'Rejected',
        'DROP_PENDING': 'Drop Pending',
        'DROPPED': 'Dropped',
        'DROP_REJECTED': 'Drop Rejected'
      };
      return `
      <tr>
        <td><span class="course-code">${escapeHtml(r.courseCode)}</span></td>
        <td>${escapeHtml(r.courseTitle)}</td>
        <td class="timestamp">${r.requestedAt}</td>
        <td><span class="badge ${statusMap[r.status] || 'badge-pending'}">${labelMap[r.status] || r.status}</span></td>
        <td>${r.remarks ? escapeHtml(r.remarks) : '—'}</td>
        <td>
          ${r.status === 'APPROVED' 
            ? `<button class="btn btn-primary btn-small" onclick="openContentViewer(${r.courseId}, '${escapeHtml(r.courseCode)}')"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="vertical-align:-2px; margin-right:5px;"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2Z"/></svg>View Content</button>
               <button class="btn btn-outline btn-small" onclick="openQuizList(${r.courseId}, '${escapeHtml(r.courseCode)}')"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="vertical-align:-2px; margin-right:5px;"><rect x="9" y="2" width="6" height="4" rx="1"/><path d="M9 4H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6a2 2 0 0 0-2-2h-4"/><path d="m9 14 2 2 4-4"/></svg>Quizzes</button>
               <button class="btn btn-outline btn-small" onclick="openCertificateModal(${r.courseId}, '${escapeHtml(r.courseCode)}', '${escapeHtml(r.courseTitle)}')"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="vertical-align:-2px; margin-right:5px;"><circle cx="12" cy="8" r="6"/><path d="M15.5 13.5 17 22l-5-3-5 3 1.5-8.5"/></svg>Certificate</button>
               <button class="btn btn-outline btn-small" onclick="openQuestListModal(${r.courseId}, '${escapeHtml(r.courseCode)}')"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="vertical-align:-2px; margin-right:5px;"><path d="M4 22V4a1 1 0 0 1 1-1c1.5 1.5 4.5 1.5 6 0s4.5-1.5 6 0a1 1 0 0 1 1 1v10a1 1 0 0 1-1 1c-1.5-1.5-4.5-1.5-6 0s-4.5 1.5-6 0"/></svg>Quests</button>
               <button class="btn btn-danger btn-small" onclick="requestDrop(${r.id}, this)">Drop</button>`
            : r.status === 'PENDING'
            ? `<span class="status-muted">⏳ Awaiting decision</span>`
            : `<span class="status-muted">—</span>`
          }
        </td>
      </tr>
    `}).join('');
  } catch (err) {
    setError('mineError', err.message);
  }
}

async function requestDrop(enrollmentId, btn) {
  btn.disabled = true;
  btn.textContent = '⏳';
  try {
    await api('/student/enrollments/' + enrollmentId + '/drop', { method: 'POST' });
    await loadMine();
  } catch (err) {
    setError('mineError', err.message);
    btn.disabled = false;
    btn.textContent = 'Drop';
  }
}

// ---------- Schedule ----------
async function loadSchedule() {
  setError('scheduleError', null);
  try {
    const courses = await api('/student/schedule');
    const body = document.getElementById('scheduleBody');

    if (courses.length === 0) {
      body.innerHTML = `<tr><td colspan="5" class="empty-cell">📭 No approved courses on your schedule yet</td></tr>`;
      return;
    }

    const sorted = [...courses].sort((a, b) => {
      const days = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];
      if (days.indexOf(a.dayOfWeek) !== days.indexOf(b.dayOfWeek)) {
        return days.indexOf(a.dayOfWeek) - days.indexOf(b.dayOfWeek);
      }
      return a.startTime.localeCompare(b.startTime);
    });

    body.innerHTML = sorted.map(c => `
      <tr>
        <td><strong>${dayLabel(c.dayOfWeek)}</strong></td>
        <td class="mono">${c.startTime.slice(0, 5)}–${c.endTime.slice(0, 5)}</td>
        <td><span class="course-code">${escapeHtml(c.code)}</span></td>
        <td>${escapeHtml(c.title)}</td>
        <td>${escapeHtml(c.instructorName)}</td>
      </tr>
    `).join('');
  } catch (err) {
    setError('scheduleError', err.message);
  }
}

// ---------- Course Content Viewer ----------
async function openContentViewer(courseId, courseCode) {
  document.getElementById('contentViewerTitle').textContent = 'Lessons — ' + courseCode;
  const body = document.getElementById('contentViewerBody');
  body.innerHTML = '<div class="spinner" style="margin: 20px auto;"></div>';
  document.getElementById('contentViewerModal').classList.remove('hidden');

  try {
    const items = await api('/student/courses/' + courseId + '/contents');
    if (items.length === 0) {
      body.innerHTML = '<p class="status-muted">No lessons have been added to this course yet.</p>';
      return;
    }
    body.innerHTML = items.map(i => `
      <div class="glass" style="padding:14px 16px; border-radius:10px; margin-bottom:10px;">
        <div style="display:flex; justify-content:space-between; align-items:flex-start; gap:10px;">
          <h3 style="margin-bottom:8px;">${i.completed ? '✅ ' : ''}${escapeHtml(i.title)}</h3>
          ${!i.completed
            ? `<button class="btn btn-outline btn-small" onclick="markContentComplete(${i.id}, ${courseId}, '${escapeHtml(courseCode)}')">Mark Complete</button>`
            : ''
          }
        </div>
        ${i.contentType === 'LINK'
          ? `<a href="${escapeHtml(i.externalUrl)}" target="_blank" rel="noopener">${escapeHtml(i.externalUrl)}</a>`
          : `<p style="color: var(--text-secondary); white-space: pre-wrap;">${escapeHtml(i.body)}</p>`
        }
      </div>
    `).join('');
  } catch (err) {
    body.innerHTML = '<p class="error-banner">' + escapeHtml(err.message) + '</p>';
  }
}

async function markContentComplete(contentId, courseId, courseCode) {
  try {
    await api('/student/contents/' + contentId + '/complete', { method: 'POST' });
    await openContentViewer(courseId, courseCode); // reload to show the checkmark
  } catch (err) {
    alert(err.message);
  }
}

function closeContentViewer() {
  document.getElementById('contentViewerModal').classList.add('hidden');
}

// ---------- Quizzes ----------
let currentQuizStudentCourseId = null;
let currentTakingQuizId = null;
let currentTakingQuestions = [];

async function openQuizList(courseId, courseCode) {
  currentQuizStudentCourseId = courseId;
  document.getElementById('quizStudentTitle').textContent = 'Quizzes — ' + courseCode;
  showQuizStudentListView();
  document.getElementById('quizStudentModal').classList.remove('hidden');
  await loadQuizStudentList(courseId);
}

function closeQuizModal() {
  document.getElementById('quizStudentModal').classList.add('hidden');
}

function showQuizStudentListView() {
  document.getElementById('quizStudentListView').classList.remove('hidden');
  document.getElementById('quizTakeView').classList.add('hidden');
  document.getElementById('quizResultView').classList.add('hidden');
}

function backToQuizStudentList() {
  showQuizStudentListView();
  loadQuizStudentList(currentQuizStudentCourseId);
}

async function loadQuizStudentList(courseId) {
  setError('quizStudentError', null);
  try {
    const quizzes = await api('/student/courses/' + courseId + '/quizzes');
    const wrap = document.getElementById('quizStudentList');

    if (quizzes.length === 0) {
      wrap.innerHTML = '<p class="status-muted">No quizzes have been posted for this course yet.</p>';
      return;
    }

    wrap.innerHTML = quizzes.map(q => `
      <div class="glass" style="padding:14px 16px; border-radius:10px; margin-bottom:10px; display:flex; justify-content:space-between; align-items:center; gap:14px;">
        <div>
          <strong>${escapeHtml(q.title)}</strong>
          <div class="status-muted" style="font-size:0.82rem; margin-top:2px;">
            ${q.questionCount} question${q.questionCount === 1 ? '' : 's'}
            ${q.attempted ? ' · Best score: ' + q.bestScore + '/' + q.questionCount : ' · Not attempted yet'}
          </div>
        </div>
        <button class="btn btn-primary btn-small" onclick="startQuiz(${q.id})">${q.attempted ? 'Retake' : 'Take Quiz'}</button>
      </div>
    `).join('');
  } catch (err) {
    setError('quizStudentError', err.message);
  }
}

async function startQuiz(quizId) {
  setError('quizStudentError', null);
  try {
    const detail = await api('/student/quizzes/' + quizId + '/take');
    currentTakingQuizId = quizId;
    currentTakingQuestions = detail.questions;

    document.getElementById('quizStudentListView').classList.add('hidden');
    document.getElementById('quizTakeView').classList.remove('hidden');

    document.getElementById('quizTakeQuestions').innerHTML = detail.questions.map((q, idx) => `
      <div class="glass" style="padding:14px 16px; border-radius:10px; margin-bottom:12px;">
        <p style="margin-bottom:10px;"><strong>${idx + 1}. ${escapeHtml(q.questionText)}</strong></p>
        ${q.options.map(o => `
          <label style="display:flex; align-items:center; gap:8px; padding:5px 0; cursor:pointer;">
            <input type="radio" name="q_${q.id}" value="${o.id}" style="width:auto;" />
            ${escapeHtml(o.optionText)}
          </label>
        `).join('')}
      </div>
    `).join('');
  } catch (err) {
    setError('quizStudentError', err.message);
  }
}

async function submitQuizAttempt() {
  setError('quizStudentError', null);
  const answers = currentTakingQuestions.map(q => {
    const picked = document.querySelector('input[name="q_' + q.id + '"]:checked');
    return { questionId: q.id, selectedOptionId: picked ? Number(picked.value) : null };
  });

  const btn = document.getElementById('submitQuizBtn');
  btn.disabled = true;
  btn.textContent = 'Submitting...';

  try {
    const result = await api('/student/quizzes/' + currentTakingQuizId + '/submit', { method: 'POST', body: { answers } });

    document.getElementById('quizTakeView').classList.add('hidden');
    document.getElementById('quizResultView').classList.remove('hidden');

    document.getElementById('quizResultSummary').innerHTML = `
      <div class="glass" style="padding:16px; border-radius:10px; text-align:center;">
        <h3 style="margin-bottom:4px;">Score: ${result.score} / ${result.totalQuestions}</h3>
        <span class="status-muted">Submitted ${result.submittedAt}</span>
      </div>
    `;

    document.getElementById('quizResultQuestions').innerHTML = result.results.map((r, idx) => {
      const question = currentTakingQuestions.find(q => q.id === r.questionId);
      const optionText = (optId) => {
        const opt = question ? question.options.find(o => o.id === optId) : null;
        return opt ? opt.optionText : '(no answer)';
      };
      return `
      <div class="glass" style="padding:12px 16px; border-radius:10px; margin-bottom:10px;">
        <p><strong>${idx + 1}. ${escapeHtml(r.questionText)}</strong></p>
        <p style="margin-top:6px;">Your answer: ${escapeHtml(optionText(r.selectedOptionId))}</p>
        ${r.wasCorrect
          ? `<p style="color: var(--success);">✅ Correct</p>`
          : `<p class="seats-full">❌ Incorrect — correct answer: ${escapeHtml(optionText(r.correctOptionId))}</p>`
        }
      </div>
    `}).join('');
  } catch (err) {
    setError('quizStudentError', err.message);
  } finally {
    btn.disabled = false;
    btn.textContent = 'Submit Answers';
  }
}

// ---------- Certificate ----------
let certCourseId = null, certCourseCode = null, certCourseTitle = null;

async function openCertificateModal(courseId, courseCode, courseTitle) {
  certCourseId = courseId; certCourseCode = courseCode; certCourseTitle = courseTitle;
  document.getElementById('certModalTitle').textContent = 'Certificate — ' + courseCode;
  document.getElementById('certModal').classList.remove('hidden');
  await renderCertificateModal();
}

function closeCertificateModal() {
  document.getElementById('certModal').classList.add('hidden');
}

async function renderCertificateModal() {
  const body = document.getElementById('certModalBody');
  body.innerHTML = '<div class="spinner" style="margin:20px auto;"></div>';

  // A certificate already issued takes priority over showing progress.
  try {
    const cert = await api('/student/courses/' + certCourseId + '/certificate');
    body.innerHTML = renderCertificateCard(cert);
    return;
  } catch (err) {
    // 404 just means "not issued yet" — fall through to showing progress.
  }

  try {
    const progress = await api('/student/courses/' + certCourseId + '/progress');
    body.innerHTML = `
      <div class="glass" style="padding:16px; border-radius:10px; margin-bottom:14px;">
        <p>Lessons: ${progress.lessonsCompleted} / ${progress.totalLessons}</p>
        <p>Quizzes passed: ${progress.quizzesPassed} / ${progress.totalQuizzes}</p>
        ${progress.avgQuizPercent != null ? `<p>Average score: ${Math.round(progress.avgQuizPercent)}%</p>` : ''}
      </div>
      ${progress.eligibleForCertificate
        ? `<button class="btn btn-primary btn-block" onclick="claimCertificate()"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="vertical-align:-2px; margin-right:5px;"><circle cx="12" cy="8" r="6"/><path d="M15.5 13.5 17 22l-5-3-5 3 1.5-8.5"/></svg>Claim Your Certificate</button>`
        : `<p class="status-muted">Finish all lessons and pass every quiz to unlock your certificate.</p>`
      }
    `;
  } catch (err) {
    body.innerHTML = '<p class="error-banner">' + escapeHtml(err.message) + '</p>';
  }
}

function renderCertificateCard(cert) {
  const dateOnly = formatCertDate(cert.issuedAt);
  const verifyUrl = window.location.origin + window.location.pathname.replace('student.html', '') + 'verify.html?code=' + encodeURIComponent(cert.certificateCode);

  return `
    <div class="certificate-printable">
      <div class="certificate-shell">
        <div class="certificate">
          <svg class="certificate-seal" viewBox="0 0 64 64" fill="none" xmlns="http://www.w3.org/2000/svg">
            <circle cx="32" cy="24" r="20" fill="currentColor" opacity="0.12"/>
            <circle cx="32" cy="24" r="20" stroke="currentColor" stroke-width="1.5"/>
            <circle cx="32" cy="24" r="14" stroke="currentColor" stroke-width="1"/>
            <path d="M32 14 L34.5 20.5 L41.5 21 L36 25.5 L38 32.5 L32 28.5 L26 32.5 L28 25.5 L22.5 21 L29.5 20.5 Z" fill="currentColor"/>
            <path d="M20 40 L24 57 L32 51 L40 57 L44 40" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          <p class="certificate-kicker">Certificate of Completion</p>
          <p class="certificate-issuer">Registrar's Ledger</p>

          <p class="certificate-lead">This certifies that</p>
          <h1 class="certificate-name">${escapeHtml(cert.studentName)}</h1>
          <p class="certificate-body">has successfully completed all coursework and requirements for</p>
          <p class="certificate-course">${escapeHtml(cert.courseCode)} — ${escapeHtml(cert.courseTitle)}</p>

          <div class="certificate-divider"></div>

          <div class="certificate-footer">
            <div class="certificate-field">
              <div class="value">${escapeHtml(dateOnly)}</div>
              <div class="label">Date Issued</div>
            </div>
            <div class="certificate-field">
              <div class="value" style="font-family:'EB Garamond',serif; font-style:italic;">Office of the Registrar</div>
              <div class="label">Authorized By</div>
            </div>
          </div>

          <p class="certificate-code">
            Verification code: ${escapeHtml(cert.certificateCode)} &nbsp;·&nbsp; Verify at ${escapeHtml(verifyUrl)}
          </p>
        </div>
      </div>
    </div>
    <div class="certificate-print-btn" style="text-align:center; margin-top:16px;">
      <button class="btn btn-outline btn-small" onclick="window.print()">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="vertical-align:-2px; margin-right:5px;"><path d="M6 9V2h12v7"/><path d="M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2"/><path d="M6 14h12v8H6z"/></svg>
        Print / Save as PDF
      </button>
    </div>
  `;
}

function formatCertDate(issuedAt) {
  const parsed = new Date(issuedAt.replace(' ', 'T'));
  if (isNaN(parsed)) return issuedAt;
  return parsed.toLocaleDateString('en-US', { year: 'numeric', month: 'long', day: 'numeric' });
}

async function claimCertificate() {
  try {
    await api('/student/courses/' + certCourseId + '/certificate/claim', { method: 'POST' });
    await renderCertificateModal();
  } catch (err) {
    alert(err.message);
  }
}

// ---------- Career Paths ----------

function showPathsListView() {
  document.getElementById('pathsListView').classList.remove('hidden');
  document.getElementById('pathDetailView').classList.add('hidden');
}

function backToPathsList() {
  showPathsListView();
}

async function loadPaths() {
  setError('pathsError', null);
  const wrap = document.getElementById('pathsList');
  wrap.innerHTML = '<div class="spinner" style="margin:20px auto;"></div>';
  try {
    const paths = await api('/career-paths');
    if (paths.length === 0) {
      wrap.innerHTML = '<p class="status-muted">No career paths have been set up yet.</p>';
      return;
    }
    wrap.innerHTML = paths.map(p => `
      <div class="glass" style="padding:16px; border-radius:10px; margin-bottom:10px; display:flex; justify-content:space-between; align-items:center; gap:14px;">
        <div>
          <strong>${escapeHtml(p.name)}</strong>
          <div class="status-muted" style="font-size:0.85rem; margin-top:4px;">${escapeHtml(p.description || '')}</div>
          <div class="status-muted" style="font-size:0.8rem; margin-top:4px;">${p.courseCount} course${p.courseCount === 1 ? '' : 's'}</div>
        </div>
        <button class="btn btn-primary btn-small" onclick="openPathDetail(${p.id})">View Path</button>
      </div>
    `).join('');
  } catch (err) {
    setError('pathsError', err.message);
  }
}

async function openPathDetail(pathId) {
  document.getElementById('pathsListView').classList.add('hidden');
  document.getElementById('pathDetailView').classList.remove('hidden');
  const body = document.getElementById('pathDetailBody');
  body.innerHTML = '<div class="spinner" style="margin:20px auto;"></div>';

  const statusMeta = {
    COMPLETED: { label: 'Completed', cls: 'badge-approved' },
    ENROLLED:  { label: 'In Progress', cls: 'badge-pending' },
    AVAILABLE: { label: 'Not started', cls: 'badge-dropped' }
  };

  try {
    const detail = await api('/student/career-paths/' + pathId);
    body.innerHTML = `
      <h3 style="margin-bottom:6px;">${escapeHtml(detail.name)}</h3>
      <p class="status-muted" style="margin-bottom:16px;">${escapeHtml(detail.description || '')}</p>
      ${detail.courses.map((c, idx) => {
        const meta = statusMeta[c.status] || statusMeta.AVAILABLE;
        return `
        <div class="glass" style="padding:12px 16px; border-radius:10px; margin-bottom:8px; display:flex; justify-content:space-between; align-items:center;">
          <div>
            <span class="status-muted">${idx + 1}.</span>
            <span class="course-code">${escapeHtml(c.code)}</span>
            <strong style="margin-left:8px;">${escapeHtml(c.title)}</strong>
          </div>
          <span class="badge ${meta.cls}">${meta.label}</span>
        </div>
      `}).join('')}
      <p class="status-muted" style="margin-top:14px; font-size:0.85rem;">
        For any course marked "Not started," search its code in the Catalog tab to request enrollment.
      </p>
    `;
  } catch (err) {
    body.innerHTML = '<p class="error-banner">' + escapeHtml(err.message) + '</p>';
  }
}

// ---------- Badges ----------

async function loadBadges() {
  setError('badgesError', null);
  const wrap = document.getElementById('badgesList');
  wrap.innerHTML = '<div class="spinner" style="margin:20px auto;"></div>';
  try {
    const badges = await api('/student/badges');
    if (badges.length === 0) {
      wrap.innerHTML = '<p class="status-muted">No badges yet — complete lessons and quizzes to start earning them.</p>';
      return;
    }
    wrap.innerHTML = `
      <div style="display:grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap:14px;">
        ${badges.map(b => `
          <div class="glass" style="padding:18px; border-radius:10px; text-align:center;">
            <div style="font-size:2rem; margin-bottom:8px;">${b.icon}</div>
            <strong>${escapeHtml(b.title)}</strong>
            <div class="status-muted" style="font-size:0.8rem; margin-top:4px;">${escapeHtml(b.description)}</div>
          </div>
        `).join('')}
      </div>
    `;
  } catch (err) {
    setError('badgesError', err.message);
  }
}

// ---------- Side Quests ----------
let currentQuestListCourseId = null;

async function openQuestListModal(courseId, courseCode) {
  currentQuestListCourseId = courseId;
  document.getElementById('questListTitle').textContent = 'Side Quests — ' + courseCode;
  document.getElementById('questListModal').classList.remove('hidden');
  await loadQuestListForStudent();
}

function closeQuestListModal() {
  document.getElementById('questListModal').classList.add('hidden');
}

async function loadQuestListForStudent() {
  setError('questListError', null);
  const body = document.getElementById('questListBody');
  body.innerHTML = '<div class="spinner" style="margin:20px auto;"></div>';

  try {
    const data = await api('/student/courses/' + currentQuestListCourseId + '/quests');
    if (data.quests.length === 0) {
      body.innerHTML = '<p class="status-muted">No side quests have been posted for this course yet.</p>';
      return;
    }

    body.innerHTML = `
      <div class="glass" style="padding:12px 16px; border-radius:10px; margin-bottom:14px; text-align:center;">
        <strong>${data.totalPointsEarned} points earned</strong>
      </div>
      ${data.quests.map(q => `
        <div class="glass" style="padding:14px 16px; border-radius:10px; margin-bottom:10px; display:flex; justify-content:space-between; align-items:center; gap:14px;">
          <div>
            <strong>${q.completed ? '\u2713 ' : ''}${escapeHtml(q.title)}</strong>
            <span class="badge badge-pending" style="margin-left:8px;">${q.points} pts</span>
            ${q.description ? `<div class="status-muted" style="font-size:0.82rem; margin-top:4px;">${escapeHtml(q.description)}</div>` : ''}
          </div>
          ${!q.completed
            ? `<button class="btn btn-primary btn-small" onclick="completeQuest(${q.id})">Mark Done</button>`
            : ''
          }
        </div>
      `).join('')}
    `;
  } catch (err) {
    setError('questListError', err.message);
    body.innerHTML = '';
  }
}

async function completeQuest(questId) {
  try {
    await api('/student/quests/' + questId + '/complete', { method: 'POST' });
    await loadQuestListForStudent();
  } catch (err) {
    setError('questListError', err.message);
  }
}

// Initial load
loadCatalog();
