// Auto-verify if the page was opened with ?code=XXXX (the link printed on the certificate itself).
(function checkUrlForCode() {
  const params = new URLSearchParams(window.location.search);
  const code = params.get('code');
  if (code) {
    document.getElementById('codeInput').value = code;
    verifyCode(code);
  }
})();

function handleVerify(event) {
  event.preventDefault();
  const code = document.getElementById('codeInput').value.trim();
  if (!code) return;
  verifyCode(code);
}

async function verifyCode(code) {
  setError('verifyError', null);
  const resultEl = document.getElementById('verifyResult');
  resultEl.innerHTML = '<div class="spinner" style="margin:20px auto;"></div>';

  try {
    const cert = await api('/certificates/verify/' + encodeURIComponent(code));
    resultEl.innerHTML = `
      <div class="glass" style="padding:14px 18px; border-radius:12px; margin-bottom:16px; text-align:center; border: 1px solid var(--success);">
        <strong style="color: var(--success);">✓ This certificate is genuine</strong>
      </div>
      ${renderVerifiedCertificate(cert)}
    `;
  } catch (err) {
    resultEl.innerHTML = '';
    setError('verifyError', err.message || 'No certificate found with that code.');
  }
}

function setError(id, message) {
  const el = document.getElementById(id);
  if (!message) { el.classList.add('hidden'); return; }
  el.textContent = message;
  el.classList.remove('hidden');
}

function renderVerifiedCertificate(cert) {
  const dateOnly = formatCertDate(cert.issuedAt);
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

          <p class="certificate-code">Verification code: ${escapeHtml(cert.certificateCode)}</p>
        </div>
      </div>
    </div>
    <div style="text-align:center; margin-top:16px;">
      <button class="btn btn-outline btn-small" onclick="window.print()">Print / Save as PDF</button>
    </div>
  `;
}

function formatCertDate(issuedAt) {
  const parsed = new Date(issuedAt.replace(' ', 'T'));
  if (isNaN(parsed)) return issuedAt;
  return parsed.toLocaleDateString('en-US', { year: 'numeric', month: 'long', day: 'numeric' });
}
