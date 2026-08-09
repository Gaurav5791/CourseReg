Auth.requireRole('ADMIN');
const username = Auth.getUsername();
document.getElementById('whoami').textContent = username;
document.getElementById('userAvatar').textContent = username.charAt(0).toUpperCase();

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

function dayLabel(day) {
  return day.charAt(0) + day.slice(1).toLowerCase();
}

let coursesById = {};

async function loadCourses() {
  setError('listError', null);
  try {
    const courses = await api('/admin/courses');
    coursesById = Object.fromEntries(courses.map(c => [c.id, c]));
    const body = document.getElementById('courseBody');

    // Update stats
    document.getElementById('totalCourses').textContent = courses.length;
    document.getElementById('activeCourses').textContent = courses.filter(c => c.status === 'ACTIVE').length;
    const uniqueInstructors = new Set(courses.map(c => c.instructorName));
    document.getElementById('instructorCount').textContent = uniqueInstructors.size;
    
    const totalEnrollments = courses.reduce((sum, c) => sum + (c.seatsTaken || 0), 0);
    document.getElementById('enrollmentCount').textContent = totalEnrollments;

    if (courses.length === 0) {
      body.innerHTML = '<tr><td colspan="7" class="empty-cell">📭 No courses yet. Create your first course!</td></tr>';
      return;
    }

    body.innerHTML = courses.map(c => `
      <tr>
        <td><span class="course-code">${escapeHtml(c.code)}</span></td>
        <td><strong>${escapeHtml(c.title)}</strong></td>
        <td>${escapeHtml(c.instructorName)}</td>
        <td>${dayLabel(c.dayOfWeek)} · ${c.startTime.slice(0,5)}–${c.endTime.slice(0,5)}</td>
        <td>${c.seatsTaken || 0} / ${c.capacity}</td>
        <td><span class="badge badge-${c.status.toLowerCase()}">${c.status}</span></td>
        <td>
          ${c.status === 'ACTIVE'
            ? `<button class="btn btn-primary btn-small" onclick="openEditModal(${c.id})"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="vertical-align:-2px; margin-right:5px;"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.12 2.12 0 0 1 3 3L12 15l-4 1 1-4Z"/></svg>Edit</button>
               <button class="btn btn-outline btn-small" onclick="openContentModal(${c.id})"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="vertical-align:-2px; margin-right:5px;"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2Z"/></svg>Content</button>
               <button class="btn btn-outline btn-small" onclick="openQuizModal(${c.id})"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="vertical-align:-2px; margin-right:5px;"><rect x="9" y="2" width="6" height="4" rx="1"/><path d="M9 4H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6a2 2 0 0 0-2-2h-4"/><path d="m9 14 2 2 4-4"/></svg>Quizzes</button>
               <button class="btn btn-outline btn-small" onclick="openQuestModal(${c.id})"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="vertical-align:-2px; margin-right:5px;"><path d="M4 22V4a1 1 0 0 1 1-1c1.5 1.5 4.5 1.5 6 0s4.5-1.5 6 0a1 1 0 0 1 1 1v10a1 1 0 0 1-1 1c-1.5-1.5-4.5-1.5-6 0s-4.5 1.5-6 0"/></svg>Quests</button>
               <button class="btn btn-outline btn-small" onclick="openAnalyticsModal(${c.id})"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="vertical-align:-2px; margin-right:5px;"><path d="M3 3v18h18"/><path d="M18 17V9M13 17V5M8 17v-3"/></svg>Analytics</button>`
            : `<span class="status-muted">—</span>`
          }
        </td>
      </tr>
    `).join('');
  } catch (err) {
    setError('listError', err.message);
  }
}

// ============================================================
// MODAL FUNCTIONS
// ============================================================

function openCreateModal() {
  document.getElementById('modalTitle').textContent = 'Create Course';
  document.getElementById('courseForm').reset();
  document.getElementById('courseId').value = '';
  document.getElementById('fCode').disabled = false;
  setError('modalError', null);
  document.getElementById('courseModal').classList.remove('hidden');
}

function openEditModal(id) {
  const course = coursesById[id];
  if (!course) return;
  
  document.getElementById('modalTitle').textContent = 'Edit ' + course.code;
  document.getElementById('courseId').value = course.id;
  document.getElementById('fCode').value = course.code;
  document.getElementById('fCode').disabled = true;
  document.getElementById('fTitle').value = course.title;
  document.getElementById('fDescription').value = course.description || '';
  document.getElementById('fCredits').value = course.credits;
  document.getElementById('fInstructor').value = course.instructorName;
  document.getElementById('fDay').value = course.dayOfWeek;
  document.getElementById('fSemester').value = course.semester;
  document.getElementById('fStart').value = course.startTime.slice(0, 5);
  document.getElementById('fEnd').value = course.endTime.slice(0, 5);
  document.getElementById('fCapacity').value = course.capacity;
  setError('modalError', null);
  document.getElementById('courseModal').classList.remove('hidden');
}

function closeCourseModal() {
  document.getElementById('courseModal').classList.add('hidden');
  setError('modalError', null);
}

// ============================================================
// SUBMIT FORM
// ============================================================

async function submitCourseForm(event) {
  event.preventDefault();
  setError('modalError', null);

  const id = document.getElementById('courseId').value;
  const payload = {
    code: document.getElementById('fCode').value.trim(),
    title: document.getElementById('fTitle').value.trim(),
    description: document.getElementById('fDescription').value.trim(),
    credits: Number(document.getElementById('fCredits').value),
    instructorName: document.getElementById('fInstructor').value.trim(),
    dayOfWeek: document.getElementById('fDay').value,
    semester: document.getElementById('fSemester').value.trim(),
    startTime: document.getElementById('fStart').value,
    endTime: document.getElementById('fEnd').value,
    capacity: Number(document.getElementById('fCapacity').value)
  };

  // Validation
  if (!payload.code) { setError('modalError', 'Course code is required'); return; }
  if (!payload.title) { setError('modalError', 'Title is required'); return; }
  if (!payload.instructorName) { setError('modalError', 'Instructor name is required'); return; }
  if (!payload.semester) { setError('modalError', 'Semester is required'); return; }
  if (!payload.startTime || !payload.endTime) { setError('modalError', 'Start and end times are required'); return; }
  if (payload.startTime >= payload.endTime) { setError('modalError', 'End time must be after start time'); return; }

  const submitBtn = document.getElementById('modalSubmitBtn');
  submitBtn.disabled = true;
  submitBtn.textContent = '⏳ Saving...';

  try {
    if (id) {
      await api('/admin/courses/' + id, { method: 'PUT', body: payload });
    } else {
      await api('/admin/courses', { method: 'POST', body: payload });
    }
    closeCourseModal();
    await loadCourses();
  } catch (err) {
    setError('modalError', err.message);
    submitBtn.disabled = false;
    submitBtn.textContent = 'Save Course';
  } finally {
    submitBtn.disabled = false;
    submitBtn.textContent = 'Save Course';
  }
}

// ============================================================
// COURSE CONTENT (LESSONS) MANAGEMENT
// ============================================================

let currentContentCourseId = null;
let contentItemsById = {};

async function openContentModal(courseId) {
  currentContentCourseId = courseId;
  const course = coursesById[courseId];
  document.getElementById('contentModalTitle').textContent =
    'Manage Content — ' + (course ? course.code : '');
  document.getElementById('cCourseId').value = courseId;
  resetContentForm();
  setError('contentError', null);
  document.getElementById('contentModal').classList.remove('hidden');
  await loadContentList(courseId);
}

function closeContentModal() {
  document.getElementById('contentModal').classList.add('hidden');
  currentContentCourseId = null;
}

async function loadContentList(courseId) {
  setError('contentError', null);
  try {
    const items = await api('/admin/courses/' + courseId + '/contents');
    contentItemsById = Object.fromEntries(items.map(i => [i.id, i]));
    const list = document.getElementById('contentList');

    if (items.length === 0) {
      list.innerHTML = '<li class="status-muted" style="padding:8px 0;">No lessons yet — add the first one below.</li>';
      return;
    }

    list.innerHTML = items.map(i => `
      <li class="glass" style="display:flex; align-items:center; justify-content:space-between; padding:10px 14px; border-radius:8px;">
        <span>
          <strong>${escapeHtml(i.title)}</strong>
          <span class="badge ${i.contentType === 'LINK' ? 'badge-pending' : 'badge-approved'}" style="margin-left:8px;">
            ${i.contentType === 'LINK' ? 'Link' : 'Text'}
          </span>
        </span>
        <span>
          <button type="button" class="btn btn-outline btn-small" onclick="editContentItem(${i.id})"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="vertical-align:-2px; margin-right:5px;"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.12 2.12 0 0 1 3 3L12 15l-4 1 1-4Z"/></svg></button>
          <button type="button" class="btn btn-danger btn-small" onclick="deleteContentItem(${i.id})"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 6h18"/><path d="M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6"/></svg></button>
        </span>
      </li>
    `).join('');
  } catch (err) {
    setError('contentError', err.message);
  }
}

function toggleContentTypeFields() {
  const isLink = document.getElementById('cType').value === 'LINK';
  document.getElementById('cBodyWrap').classList.toggle('hidden', isLink);
  document.getElementById('cUrlWrap').classList.toggle('hidden', !isLink);
}

function editContentItem(id) {
  const item = contentItemsById[id];
  if (!item) return;
  document.getElementById('cContentId').value = item.id;
  document.getElementById('cTitle').value = item.title;
  document.getElementById('cType').value = item.contentType;
  document.getElementById('cOrder').value = item.orderIndex;
  document.getElementById('cBody').value = item.body || '';
  document.getElementById('cUrl').value = item.externalUrl || '';
  toggleContentTypeFields();
  document.getElementById('contentSubmitBtn').textContent = 'Save Changes';
}

function resetContentForm() {
  document.getElementById('contentForm').reset();
  document.getElementById('cContentId').value = '';
  document.getElementById('contentSubmitBtn').textContent = 'Add Lesson';
  toggleContentTypeFields();
  setError('contentError', null);
}

async function submitContentForm(event) {
  event.preventDefault();
  setError('contentError', null);

  const id = document.getElementById('cContentId').value;
  const courseId = document.getElementById('cCourseId').value;
  const payload = {
    title: document.getElementById('cTitle').value.trim(),
    contentType: document.getElementById('cType').value,
    body: document.getElementById('cBody').value,
    externalUrl: document.getElementById('cUrl').value.trim(),
    orderIndex: Number(document.getElementById('cOrder').value) || 0
  };

  if (!payload.title) { setError('contentError', 'Lesson title is required'); return; }

  try {
    if (id) {
      await api('/admin/contents/' + id, { method: 'PUT', body: payload });
    } else {
      await api('/admin/courses/' + courseId + '/contents', { method: 'POST', body: payload });
    }
    resetContentForm();
    await loadContentList(courseId);
  } catch (err) {
    setError('contentError', err.message);
  }
}

async function deleteContentItem(id) {
  if (!confirm('Remove this lesson? This cannot be undone.')) return;
  try {
    await api('/admin/contents/' + id, { method: 'DELETE' });
    await loadContentList(currentContentCourseId);
  } catch (err) {
    setError('contentError', err.message);
  }
}

// ============================================================
// QUIZ MANAGEMENT
// ============================================================

let currentQuizCourseId = null;
let currentQuizId = null;
let quizzesById = {};

async function openQuizModal(courseId) {
  currentQuizCourseId = courseId;
  const course = coursesById[courseId];
  document.getElementById('quizModalTitle').textContent = 'Quizzes — ' + (course ? course.code : '');
  showQuizListView();
  setError('quizError', null);
  document.getElementById('quizModal').classList.remove('hidden');
  await loadQuizList(courseId);
}

function closeQuizModal() {
  document.getElementById('quizModal').classList.add('hidden');
  currentQuizCourseId = null;
  currentQuizId = null;
}

function showQuizListView() {
  document.getElementById('quizListView').classList.remove('hidden');
  document.getElementById('quizDetailView').classList.add('hidden');
  document.getElementById('newQuizForm').reset();
}

async function loadQuizList(courseId) {
  setError('quizError', null);
  try {
    const quizzes = await api('/admin/courses/' + courseId + '/quizzes');
    quizzesById = Object.fromEntries(quizzes.map(q => [q.id, q]));
    const list = document.getElementById('quizList');

    if (quizzes.length === 0) {
      list.innerHTML = '<li class="status-muted" style="padding:8px 0;">No quizzes yet — create one below.</li>';
      return;
    }

    list.innerHTML = quizzes.map(q => `
      <li class="glass" style="display:flex; align-items:center; justify-content:space-between; padding:10px 14px; border-radius:8px;">
        <span>
          <strong>${escapeHtml(q.title)}</strong>
          <span class="status-muted"> — ${q.questionCount} question${q.questionCount === 1 ? '' : 's'}</span>
        </span>
        <span>
          <button type="button" class="btn btn-outline btn-small" onclick="openQuizDetail(${q.id})">Manage</button>
          <button type="button" class="btn btn-danger btn-small" onclick="deleteQuizItem(${q.id})"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 6h18"/><path d="M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6"/></svg></button>
        </span>
      </li>
    `).join('');
  } catch (err) {
    setError('quizError', err.message);
  }
}

async function submitNewQuiz(event) {
  event.preventDefault();
  setError('quizError', null);
  const title = document.getElementById('qTitle').value.trim();
  const description = document.getElementById('qDescription').value.trim();
  if (!title) { setError('quizError', 'Quiz title is required'); return; }

  try {
    await api('/admin/courses/' + currentQuizCourseId + '/quizzes', { method: 'POST', body: { title, description } });
    document.getElementById('newQuizForm').reset();
    await loadQuizList(currentQuizCourseId);
  } catch (err) {
    setError('quizError', err.message);
  }
}

async function deleteQuizItem(id) {
  if (!confirm('Delete this quiz and all its questions? This cannot be undone.')) return;
  try {
    await api('/admin/quizzes/' + id, { method: 'DELETE' });
    await loadQuizList(currentQuizCourseId);
  } catch (err) {
    setError('quizError', err.message);
  }
}

// ---------- Quiz detail view (questions) ----------

async function openQuizDetail(quizId) {
  currentQuizId = quizId;
  document.getElementById('quizListView').classList.add('hidden');
  document.getElementById('quizDetailView').classList.remove('hidden');
  document.getElementById('newQuestionForm').reset();
  renderOptionRows();
  await loadQuestionList(quizId);
}

function backToQuizList() {
  showQuizListView();
}

function renderOptionRows() {
  const wrap = document.getElementById('optionRows');
  wrap.innerHTML = [0, 1, 2, 3].map(i => `
    <div style="display:flex; align-items:center; gap:10px;">
      <input type="radio" name="correctOption" value="${i}" ${i === 0 ? 'checked' : ''} style="width:auto;" />
      <input type="text" id="opt${i}" placeholder="Option ${i + 1}${i < 2 ? ' (required)' : ' (optional)'}" style="flex:1;" />
    </div>
  `).join('');
}

async function loadQuestionList(quizId) {
  setError('quizError', null);
  try {
    const detail = await api('/admin/quizzes/' + quizId);
    const list = document.getElementById('questionList');

    if (detail.questions.length === 0) {
      list.innerHTML = '<li class="status-muted" style="padding:8px 0;">No questions yet — add the first one below.</li>';
      return;
    }

    list.innerHTML = detail.questions.map(q => `
      <li class="glass" style="padding:10px 14px; border-radius:8px;">
        <div style="display:flex; justify-content:space-between; align-items:flex-start; gap:10px;">
          <strong>${escapeHtml(q.questionText)}</strong>
          <button type="button" class="btn btn-danger btn-small" onclick="deleteQuestionItem(${q.id})"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 6h18"/><path d="M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6"/></svg></button>
        </div>
        <ul style="margin:8px 0 0; padding-left:18px; font-size:0.85rem; color:var(--text-secondary);">
          ${q.options.map(o => `<li>${o.correct ? '✅ ' : ''}${escapeHtml(o.optionText)}</li>`).join('')}
        </ul>
      </li>
    `).join('');
  } catch (err) {
    setError('quizError', err.message);
  }
}

async function submitNewQuestion(event) {
  event.preventDefault();
  setError('quizError', null);

  const questionText = document.getElementById('qnText').value.trim();
  const correctIndex = Number(document.querySelector('input[name="correctOption"]:checked').value);

  const options = [];
  for (let i = 0; i < 4; i++) {
    const text = document.getElementById('opt' + i).value.trim();
    if (text) options.push({ optionText: text, correct: i === correctIndex });
  }

  if (!questionText) { setError('quizError', 'Question text is required'); return; }
  if (options.length < 2) { setError('quizError', 'Fill in at least 2 options'); return; }
  if (!options.some(o => o.correct)) {
    setError('quizError', 'The option marked correct was left blank — fill it in or pick a different one');
    return;
  }

  try {
    await api('/admin/quizzes/' + currentQuizId + '/questions', {
      method: 'POST',
      body: { questionText, orderIndex: 0, options }
    });
    document.getElementById('newQuestionForm').reset();
    renderOptionRows();
    await loadQuestionList(currentQuizId);
  } catch (err) {
    setError('quizError', err.message);
  }
}

async function deleteQuestionItem(id) {
  if (!confirm('Delete this question?')) return;
  try {
    await api('/admin/questions/' + id, { method: 'DELETE' });
    await loadQuestionList(currentQuizId);
  } catch (err) {
    setError('quizError', err.message);
  }
}

// ============================================================
// ANALYTICS
// ============================================================

async function openAnalyticsModal(courseId) {
  const course = coursesById[courseId];
  document.getElementById('analyticsModalTitle').textContent = 'Analytics — ' + (course ? course.code : '');
  document.getElementById('analyticsModal').classList.remove('hidden');
  const body = document.getElementById('analyticsBody');
  body.innerHTML = '<div class="spinner" style="margin:20px auto;"></div>';
  setError('analyticsError', null);

  try {
    const rows = await api('/admin/courses/' + courseId + '/analytics');
    if (rows.length === 0) {
      body.innerHTML = '<p class="status-muted">No approved students in this course yet.</p>';
      return;
    }
    body.innerHTML = `
      <table class="modern-table">
        <thead>
          <tr><th>Student</th><th>Lessons</th><th>Quizzes Passed</th><th>Avg Score</th><th>Certificate</th></tr>
        </thead>
        <tbody>
          ${rows.map(r => `
            <tr>
              <td>${escapeHtml(r.studentName)}</td>
              <td>${r.progress.lessonsCompleted} / ${r.progress.totalLessons}</td>
              <td>${r.progress.quizzesPassed} / ${r.progress.totalQuizzes}</td>
              <td>${r.progress.avgQuizPercent != null ? Math.round(r.progress.avgQuizPercent) + '%' : '—'}</td>
              <td>${r.progress.eligibleForCertificate
                  ? '<span class="badge badge-approved">Eligible</span>'
                  : '<span class="status-muted">Not yet</span>'}</td>
            </tr>
          `).join('')}
        </tbody>
      </table>
    `;
  } catch (err) {
    setError('analyticsError', err.message);
    body.innerHTML = '';
  }
}

function closeAnalyticsModal() {
  document.getElementById('analyticsModal').classList.add('hidden');
}

// ============================================================
// CAREER PATHS (ADMIN)
// ============================================================

let currentPathId = null;

async function openPathsAdminModal() {
  showPathsAdminListView();
  setError('pathsAdminError', null);
  document.getElementById('pathsAdminModal').classList.remove('hidden');
  await loadPathsAdminList();
}

function closePathsAdminModal() {
  document.getElementById('pathsAdminModal').classList.add('hidden');
  currentPathId = null;
}

function showPathsAdminListView() {
  document.getElementById('pathsAdminListView').classList.remove('hidden');
  document.getElementById('pathDetailAdminView').classList.add('hidden');
  document.getElementById('newPathForm').reset();
}

function backToPathsAdminList() {
  showPathsAdminListView();
  loadPathsAdminList();
}

async function loadPathsAdminList() {
  setError('pathsAdminError', null);
  try {
    const paths = await api('/career-paths');
    const wrap = document.getElementById('pathsAdminList');
    if (paths.length === 0) {
      wrap.innerHTML = '<p class="status-muted">No career paths yet — create one below.</p>';
      return;
    }
    wrap.innerHTML = paths.map(p => `
      <div class="glass" style="display:flex; justify-content:space-between; align-items:center; padding:10px 14px; border-radius:8px; margin-bottom:8px;">
        <span><strong>${escapeHtml(p.name)}</strong> <span class="status-muted">— ${p.courseCount} course${p.courseCount === 1 ? '' : 's'}</span></span>
        <span>
          <button type="button" class="btn btn-outline btn-small" onclick="openPathDetailAdmin(${p.id})">Manage</button>
          <button type="button" class="btn btn-danger btn-small" onclick="deletePathItem(${p.id})"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 6h18"/><path d="M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6"/></svg></button>
        </span>
      </div>
    `).join('');
  } catch (err) {
    setError('pathsAdminError', err.message);
  }
}

async function submitNewPath(event) {
  event.preventDefault();
  setError('pathsAdminError', null);
  const name = document.getElementById('pName').value.trim();
  const description = document.getElementById('pDescription').value.trim();
  if (!name) { setError('pathsAdminError', 'Path name is required'); return; }

  try {
    await api('/admin/career-paths', { method: 'POST', body: { name, description } });
    document.getElementById('newPathForm').reset();
    await loadPathsAdminList();
  } catch (err) {
    setError('pathsAdminError', err.message);
  }
}

async function deletePathItem(id) {
  if (!confirm('Delete this career path?')) return;
  try {
    await api('/admin/career-paths/' + id, { method: 'DELETE' });
    await loadPathsAdminList();
  } catch (err) {
    setError('pathsAdminError', err.message);
  }
}

async function openPathDetailAdmin(pathId) {
  currentPathId = pathId;
  document.getElementById('pathsAdminListView').classList.add('hidden');
  document.getElementById('pathDetailAdminView').classList.remove('hidden');
  await loadPathDetailAdmin();

  // Populate the "add course" dropdown from the courses already loaded on this page.
  const select = document.getElementById('pAddCourseSelect');
  select.innerHTML = Object.values(coursesById)
    .filter(c => c.status === 'ACTIVE')
    .map(c => `<option value="${c.id}">${escapeHtml(c.code)} — ${escapeHtml(c.title)}</option>`)
    .join('');
}

async function loadPathDetailAdmin() {
  setError('pathsAdminError', null);
  try {
    const detail = await api('/admin/career-paths/' + currentPathId);
    document.getElementById('pathDetailAdminList').innerHTML = detail.courses.length === 0
      ? '<p class="status-muted">No courses in this path yet.</p>'
      : detail.courses.map(c => `
          <div class="glass" style="display:flex; justify-content:space-between; align-items:center; padding:8px 14px; border-radius:8px; margin-bottom:6px;">
            <span><span class="course-code">${escapeHtml(c.code)}</span> ${escapeHtml(c.title)}</span>
            <button type="button" class="btn btn-danger btn-small" onclick="removeCourseFromPath(${c.id})">Remove</button>
          </div>
        `).join('');
  } catch (err) {
    setError('pathsAdminError', err.message);
  }
}

async function addCourseToPath() {
  setError('pathsAdminError', null);
  const courseId = Number(document.getElementById('pAddCourseSelect').value);
  if (!courseId) { setError('pathsAdminError', 'Pick a course first'); return; }

  try {
    await api('/admin/career-paths/' + currentPathId + '/courses', {
      method: 'POST',
      body: { courseId, orderIndex: 0 }
    });
    await loadPathDetailAdmin();
  } catch (err) {
    setError('pathsAdminError', err.message);
  }
}

async function removeCourseFromPath(courseId) {
  try {
    await api('/admin/career-paths/' + currentPathId + '/courses/' + courseId, { method: 'DELETE' });
    await loadPathDetailAdmin();
  } catch (err) {
    setError('pathsAdminError', err.message);
  }
}

// ============================================================
// SIDE QUESTS
// ============================================================

let currentQuestCourseId = null;

async function openQuestModal(courseId) {
  currentQuestCourseId = courseId;
  const course = coursesById[courseId];
  document.getElementById('questModalTitle').textContent = 'Side Quests — ' + (course ? course.code : '');
  document.getElementById('newQuestForm').reset();
  setError('questError', null);
  document.getElementById('questModal').classList.remove('hidden');
  await loadQuestList(courseId);
}

function closeQuestModal() {
  document.getElementById('questModal').classList.add('hidden');
  currentQuestCourseId = null;
}

async function loadQuestList(courseId) {
  setError('questError', null);
  try {
    const quests = await api('/admin/courses/' + courseId + '/quests');
    const list = document.getElementById('questList');

    if (quests.length === 0) {
      list.innerHTML = '<li class="status-muted" style="padding:8px 0;">No quests yet — add one below.</li>';
      return;
    }

    list.innerHTML = quests.map(q => `
      <li class="glass" style="display:flex; align-items:center; justify-content:space-between; padding:10px 14px; border-radius:8px;">
        <span>
          <strong>${escapeHtml(q.title)}</strong>
          <span class="badge badge-pending" style="margin-left:8px;">${q.points} pts</span>
          ${q.description ? `<div class="status-muted" style="font-size:0.8rem; margin-top:4px;">${escapeHtml(q.description)}</div>` : ''}
        </span>
        <button type="button" class="btn btn-danger btn-small" onclick="deleteQuestItem(${q.id})">${ICONS.trash}</button>
      </li>
    `).join('');
  } catch (err) {
    setError('questError', err.message);
  }
}

async function submitNewQuest(event) {
  event.preventDefault();
  setError('questError', null);
  const title = document.getElementById('qtTitle').value.trim();
  const description = document.getElementById('qtDescription').value.trim();
  const points = Number(document.getElementById('qtPoints').value) || 10;
  if (!title) { setError('questError', 'Quest title is required'); return; }

  try {
    await api('/admin/courses/' + currentQuestCourseId + '/quests', { method: 'POST', body: { title, description, points } });
    document.getElementById('newQuestForm').reset();
    await loadQuestList(currentQuestCourseId);
  } catch (err) {
    setError('questError', err.message);
  }
}

async function deleteQuestItem(id) {
  if (!confirm('Delete this quest?')) return;
  try {
    await api('/admin/quests/' + id, { method: 'DELETE' });
    await loadQuestList(currentQuestCourseId);
  } catch (err) {
    setError('questError', err.message);
  }
}

// Load courses on page load
loadCourses();
