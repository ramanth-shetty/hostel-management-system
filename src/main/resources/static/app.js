// ---------------------------------------------------------------------------
// Hostel dashboard front end. Talks to the Spring Boot API:
//   /api/students  /api/payments  /api/rooms  /api/settings
// Money is NEVER calculated here: feesPaid / pendingFees always come from the server.
// ---------------------------------------------------------------------------
const API = {
  students: "/api/students",
  payments: "/api/payments",
  rooms: "/api/rooms",
  fee: "/api/settings/standard-monthly-fee",
};
const ROOM_PATTERN = /^[A-Z]-[1-9][0-9]{2}$/;
const ROOM_ERROR = "Room number must match the format A-101";

const state = {
  students: [],
  pendingOnly: false,
  query: "",
  checkoutId: null,
  paymentStudentId: null,
};

const $ = (id) => document.getElementById(id);

document.addEventListener("DOMContentLoaded", () => {
  $("student-form").addEventListener("submit", handleFormSubmit);
  $("btn-cancel").addEventListener("click", resetForm);
  $("btn-refresh").addEventListener("click", () => {
    fetchStudents();
    loadFee();
  });
  $("btn-pending").addEventListener("click", togglePendingFilter);
  $("student-search").addEventListener("input", (event) => {
    state.query = event.target.value.trim().toLowerCase();
    renderAll();
  });
  $("student-table-body").addEventListener("click", handleTableAction);

  // Real-time room number check (A-101 style)
  $("roomNumber").addEventListener("input", (event) => {
    event.target.value = event.target.value.toUpperCase();
    if (ROOM_PATTERN.test(event.target.value)) clearFieldError("roomNumber");
  });
  $("roomNumber").addEventListener("blur", (event) => {
    const value = event.target.value.trim();
    if (value && !ROOM_PATTERN.test(value)) {
      renderFieldErrors({ roomNumber: ROOM_ERROR });
    }
  });

  $("btn-confirm-action").addEventListener("click", confirmCheckout);
  $("payment-form").addEventListener("submit", handlePaymentSubmit);
  $("btn-rooms").addEventListener("click", openRooms);
  $("btn-fee").addEventListener("click", openFeeModal);
  $("fee-form").addEventListener("submit", handleFeeSubmit);
  $("btn-logout").addEventListener("click", logout);
  $("btn-mobile-register").addEventListener("click", () => {
    $("student-form").scrollIntoView({ behavior: "smooth" });
    $("name").focus();
  });

  document.querySelectorAll("[data-close-modal]").forEach((button) =>
    button.addEventListener("click", () =>
      closeModal(button.closest(".modal-overlay").id),
    ),
  );
  document.querySelectorAll(".modal-overlay").forEach((overlay) =>
    overlay.addEventListener("click", (event) => {
      if (event.target === overlay) closeModal(overlay.id);
    }),
  );
  document.addEventListener("keydown", (event) => {
    if (event.key !== "Escape") return;
    document
      .querySelectorAll(".modal-overlay:not(.hidden)")
      .forEach((overlay) => closeModal(overlay.id));
  });

  fetchStudents();
  loadFee();
});

// ------------------------------------------------------------------ HTTP

async function apiRequest(url, options = {}) {
  const method = (options.method || "GET").toUpperCase();
  const headers = { Accept: "application/json", ...(options.headers || {}) };
  if (method !== "GET" && method !== "HEAD") {
    const csrfToken = getCookie("XSRF-TOKEN");
    if (csrfToken) headers["X-XSRF-TOKEN"] = decodeURIComponent(csrfToken);
  }

  const response = await fetch(url, { ...options, headers });

  // Session expired: the server answers 401 (or redirects to the HTML login page).
  if (
    response.status === 401 ||
    (response.redirected && response.url.includes("/login"))
  ) {
    window.location.href = "/login";
    throw new Error("Session expired. Redirecting to login...");
  }

  const contentType = response.headers.get("content-type") || "";
  const body = contentType.includes("application/json")
    ? await response.json()
    : null;

  if (!response.ok) {
    const fallback =
      response.status === 403
        ? "Security token missing or expired. Refresh the page and try again."
        : `Request failed (${response.status})`;
    const error = new Error(body?.message || fallback);
    error.status = response.status;
    error.fieldErrors = body?.fieldErrors || {};
    throw error;
  }
  return body;
}

// -------------------------------------------------------------- residents

async function fetchStudents() {
  setTableLoading();
  try {
    // Always load everyone; the "pending only" toggle filters on screen so the stat cards stay correct.
    state.students = (await apiRequest(API.students)) || [];
    renderAll();
  } catch (error) {
    renderTableError(error.message);
    showToast(error.message, "error");
  }
}

function renderAll() {
  renderTable(getVisibleStudents());
  updateStats(state.students);
}

function getVisibleStudents() {
  return state.students.filter((student) => {
    if (state.pendingOnly && Number(student.pendingFees || 0) <= 0)
      return false;
    if (!state.query) return true;
    return [student.name, student.roomNumber, student.status].some((value) =>
      String(value || "")
        .toLowerCase()
        .includes(state.query),
    );
  });
}

function renderTable(students) {
  const tbody = $("student-table-body");
  if (!students.length) {
    tbody.innerHTML = `<tr><td colspan="7" class="py-10 text-center text-slate-400"><i class="fa-solid fa-inbox text-2xl mb-2 block"></i>No residents match this view.</td></tr>`;
    return;
  }

  tbody.innerHTML = students
    .map((student) => {
      const pending = Number(student.pendingFees || 0);
      const active = student.status === "ACTIVE";
      const disabledEdit = active ? "" : "disabled";
      const dimmed = active ? "" : "opacity-40 cursor-not-allowed";
      return `
        <tr class="hover:bg-slate-50/80 transition">
          <td class="py-3 px-4 font-mono text-xs text-slate-500">#${escapeHtml(student.id)}</td>
          <td class="py-3 px-4 font-medium text-slate-900">${escapeHtml(student.name)}</td>
          <td class="py-3 px-4"><span class="px-2 py-1 bg-slate-100 rounded text-xs font-semibold text-slate-700">${escapeHtml(student.roomNumber)}</span></td>
          <td class="py-3 px-4 text-slate-600">${escapeHtml(student.admissionDate)}</td>
          <td class="py-3 px-4 text-right font-medium text-emerald-600">${formatCurrency(student.feesPaid)}</td>
          <td class="py-3 px-4 text-right font-medium ${pending > 0 ? "text-rose-600" : "text-slate-400"}">${formatCurrency(pending)}</td>
          <td class="py-3 px-4 text-center whitespace-nowrap">
            <span class="mr-2 px-2 py-1 rounded-full text-[10px] font-bold ${active ? "bg-emerald-100 text-emerald-700" : "bg-slate-100 text-slate-500"}">${escapeHtml(student.status)}</span>
            <button type="button" data-action="pay" data-id="${escapeHtml(student.id)}" class="p-1.5 text-slate-500 hover:text-emerald-600 hover:bg-emerald-50 rounded transition" title="Record payment / history"><i class="fa-solid fa-indian-rupee-sign"></i></button>
            <button type="button" data-action="edit" data-id="${escapeHtml(student.id)}" class="p-1.5 text-slate-500 hover:text-brand-600 hover:bg-slate-100 rounded transition ${dimmed}" title="Edit resident" ${disabledEdit}><i class="fa-solid fa-pen-to-square"></i></button>
            <button type="button" data-action="checkout" data-id="${escapeHtml(student.id)}" class="p-1.5 text-slate-500 hover:text-rose-600 hover:bg-rose-50 rounded transition ${dimmed}" title="Check out resident" ${disabledEdit}><i class="fa-solid fa-right-from-bracket"></i></button>
          </td>
        </tr>`;
    })
    .join("");
}

function updateStats(students) {
  const active = students.filter((student) => student.status === "ACTIVE");
  const collected = students.reduce(
    (sum, student) => sum + Number(student.feesPaid || 0),
    0,
  );
  const pending = students.reduce(
    (sum, student) => sum + Number(student.pendingFees || 0),
    0,
  );
  $("stat-total").textContent = active.length;
  $("stat-collected").textContent = formatCurrency(collected);
  $("stat-pending").textContent = formatCurrency(pending);
}

async function handleFormSubmit(event) {
  event.preventDefault();
  clearFieldErrors();

  const id = $("student-id").value;
  const roomNumber = $("roomNumber").value.trim().toUpperCase();
  if (!ROOM_PATTERN.test(roomNumber)) {
    renderFieldErrors({ roomNumber: ROOM_ERROR });
    return;
  }

  const payload = {
    name: $("name").value.trim(),
    roomNumber,
    admissionDate: $("admissionDate").value,
  };
  if (!id) {
    const initial = $("initialPayment").value;
    payload.initialPayment = initial === "" ? 0 : Number(initial);
    payload.paymentMode = $("paymentMode").value;
  }

  const button = $("btn-submit");
  setButtonLoading(button, true, id ? "Updating..." : "Registering...");
  let success = false;
  try {
    await apiRequest(id ? `${API.students}/${id}` : API.students, {
      method: id ? "PUT" : "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload),
    });
    success = true;
  } catch (error) {
    renderFieldErrors(error.fieldErrors);
    showToast(error.message, "error");
  } finally {
    setButtonLoading(button, false);
  }

  if (success) {
    resetForm();
    showToast(id ? "Resident updated." : "Resident registered.", "success");
    await fetchStudents();
  }
}

async function editStudent(id) {
  try {
    const student = await apiRequest(`${API.students}/${id}`);
    $("student-id").value = student.id;
    $("name").value = student.name || "";
    $("roomNumber").value = student.roomNumber || "";
    $("admissionDate").value = student.admissionDate || "";
    $("initial-payment-block").classList.add("hidden");
    $("form-title").textContent = "Edit Resident Details";
    $("edit-badge").classList.remove("hidden");
    $("btn-cancel").classList.remove("hidden");
    $("btn-submit").innerHTML =
      '<i class="fa-solid fa-floppy-disk"></i> Update Resident';
    $("student-form").scrollIntoView({ behavior: "smooth" });
    $("name").focus();
  } catch (error) {
    showToast(error.message, "error");
  }
}

function resetForm() {
  $("student-form").reset();
  $("student-id").value = "";
  $("initial-payment-block").classList.remove("hidden");
  $("form-title").textContent = "Add New Resident";
  $("edit-badge").classList.add("hidden");
  $("btn-cancel").classList.add("hidden");
  $("btn-submit").innerHTML =
    '<i class="fa-solid fa-plus"></i> Register Resident';
  clearFieldErrors();
}

function handleTableAction(event) {
  const button = event.target.closest("button[data-action]");
  if (!button || button.disabled) return;
  const id = button.dataset.id;
  if (button.dataset.action === "edit") editStudent(id);
  if (button.dataset.action === "checkout") requestCheckout(id);
  if (button.dataset.action === "pay") openPayment(id);
}

function togglePendingFilter() {
  state.pendingOnly = !state.pendingOnly;
  const button = $("btn-pending");
  button.classList.toggle("ring-2", state.pendingOnly);
  button.classList.toggle("ring-rose-300", state.pendingOnly);
  button.setAttribute("aria-pressed", String(state.pendingOnly));
  renderAll();
}

// --------------------------------------------------------------- checkout

function requestCheckout(id) {
  state.checkoutId = id;
  openModal("action-modal");
}

async function confirmCheckout() {
  if (!state.checkoutId) return;
  const button = $("btn-confirm-action");
  setButtonLoading(button, true, "Checking out...");
  let success = false;
  try {
    await apiRequest(`${API.students}/${state.checkoutId}`, {
      method: "DELETE",
    });
    success = true;
  } catch (error) {
    showToast(error.message, "error");
  } finally {
    setButtonLoading(button, false);
  }
  if (success) {
    closeModal("action-modal");
    showToast("Resident checked out. History preserved.", "success");
    await fetchStudents();
  }
}

// --------------------------------------------------------------- payments

function openPayment(id) {
  const student = state.students.find((s) => String(s.id) === String(id));
  if (!student) return;
  state.paymentStudentId = student.id;
  $("pay-student-name").textContent = student.name;
  $("pay-pending").textContent = formatCurrency(student.pendingFees);
  $("pay-amount").value = "";
  $("pay-mode").value = "CASH";
  setInlineError("pay-error", "");
  openModal("payment-modal");
  loadPaymentHistory(student.id);
  $("pay-amount").focus();
}

async function loadPaymentHistory(studentId) {
  const list = $("pay-history");
  list.innerHTML = `<li class="py-2 text-slate-400">Loading...</li>`;
  try {
    const payments = (await apiRequest(`${API.payments}/student/${studentId}`)) || [];
    if (!payments.length) {
      list.innerHTML = `<li class="py-2 text-slate-400">No payments recorded yet.</li>`;
      return;
    }
    list.innerHTML = payments
      .map(
        (payment) => `
        <li class="py-2 flex items-center justify-between">
          <span class="text-slate-600">${escapeHtml(formatDateTime(payment.paymentDate))}
            <span class="ml-2 px-1.5 py-0.5 bg-slate-100 rounded text-[10px] font-semibold">${escapeHtml(payment.paymentMode)}</span></span>
          <span class="font-medium text-emerald-600">${formatCurrency(payment.amount)}</span>
        </li>`,
      )
      .join("");
  } catch (error) {
    list.innerHTML = `<li class="py-2 text-rose-500">${escapeHtml(error.message)}</li>`;
  }
}

async function handlePaymentSubmit(event) {
  event.preventDefault();
  setInlineError("pay-error", "");
  const amount = Number($("pay-amount").value);
  if (!(amount > 0)) {
    setInlineError("pay-error", "Enter an amount greater than 0.");
    return;
  }

  const button = $("btn-pay-submit");
  setButtonLoading(button, true, "Saving...");
  let success = false;
  try {
    await apiRequest(API.payments, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        studentId: state.paymentStudentId,
        amount,
        paymentMode: $("pay-mode").value,
      }),
    });
    success = true;
  } catch (error) {
    const detail = Object.values(error.fieldErrors || {})[0];
    setInlineError("pay-error", detail || error.message);
  } finally {
    setButtonLoading(button, false);
  }

  if (success) {
    showToast("Payment recorded.", "success");
    $("pay-amount").value = "";
    await fetchStudents();
    const student = state.students.find(
      (s) => String(s.id) === String(state.paymentStudentId),
    );
    if (student) $("pay-pending").textContent = formatCurrency(student.pendingFees);
    loadPaymentHistory(state.paymentStudentId);
  }
}

// ------------------------------------------------------------------ rooms

async function openRooms() {
  const body = $("rooms-body");
  body.innerHTML = `<p class="text-slate-400 text-sm">Loading rooms...</p>`;
  openModal("rooms-modal");
  try {
    const rooms = (await apiRequest(API.rooms)) || [];
    if (!rooms.length) {
      body.innerHTML = `<p class="text-slate-400 text-sm">No rooms yet. Rooms are created when you register the first resident in them.</p>`;
      return;
    }
    body.innerHTML = rooms
      .map((room) => {
        const full = room.status === "FULL";
        const residents = room.residents.length
          ? room.residents.map((name) => `<li>${escapeHtml(name)}</li>`).join("")
          : `<li class="text-slate-400">Empty</li>`;
        return `
          <div class="border border-slate-200 rounded-lg p-3">
            <div class="flex items-center justify-between">
              <span class="font-bold text-slate-900">${escapeHtml(room.roomNumber)}</span>
              <span class="px-2 py-0.5 rounded-full text-[10px] font-bold ${full ? "bg-rose-100 text-rose-700" : "bg-emerald-100 text-emerald-700"}">${escapeHtml(room.status)}</span>
            </div>
            <p class="text-xs text-slate-500 mt-1">${room.currentOccupancy} / ${room.capacity} beds used</p>
            <ul class="mt-2 text-sm text-slate-700 list-disc list-inside">${residents}</ul>
          </div>`;
      })
      .join("");
  } catch (error) {
    body.innerHTML = `<p class="text-rose-500 text-sm">${escapeHtml(error.message)}</p>`;
  }
}

// ---------------------------------------------------------- monthly fee

async function loadFee() {
  try {
    const setting = await apiRequest(API.fee);
    state.fee = setting?.value;
    $("fee-chip").textContent = formatCurrency(setting?.value);
  } catch (error) {
    $("fee-chip").textContent = "?";
  }
}

function openFeeModal() {
  $("fee-input").value = state.fee ?? "";
  setInlineError("fee-error", "");
  openModal("fee-modal");
  $("fee-input").focus();
}

async function handleFeeSubmit(event) {
  event.preventDefault();
  setInlineError("fee-error", "");
  const raw = $("fee-input").value;
  const value = Number(raw);
  if (raw === "" || Number.isNaN(value) || value < 0) {
    setInlineError("fee-error", "Enter a fee of 0 or more.");
    return;
  }
  const button = $("btn-fee-submit");
  setButtonLoading(button, true, "Saving...");
  let success = false;
  try {
    await apiRequest(API.fee, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ value }),
    });
    success = true;
  } catch (error) {
    const detail = Object.values(error.fieldErrors || {})[0];
    setInlineError("fee-error", detail || error.message);
  } finally {
    setButtonLoading(button, false);
  }
  if (success) {
    closeModal("fee-modal");
    showToast("Monthly fee updated.", "success");
    await Promise.all([loadFee(), fetchStudents()]);
  }
}

// ----------------------------------------------------------------- logout

function logout() {
  // POST /logout needs the CSRF token; a plain form submit works with the redirect.
  const form = document.createElement("form");
  form.method = "POST";
  form.action = "/logout";
  const input = document.createElement("input");
  input.type = "hidden";
  input.name = "_csrf";
  input.value = decodeURIComponent(getCookie("XSRF-TOKEN") || "");
  form.appendChild(input);
  document.body.appendChild(form);
  form.submit();
}

// ------------------------------------------------------------- UI helpers

function openModal(id) {
  $(id).classList.remove("hidden");
}

function closeModal(id) {
  $(id).classList.add("hidden");
  if (id === "action-modal") state.checkoutId = null;
  if (id === "payment-modal") state.paymentStudentId = null;
}

function setTableLoading() {
  $("student-table-body").innerHTML = Array.from(
    { length: 4 },
    () =>
      `<tr class="animate-pulse"><td colspan="7" class="py-4 px-4"><div class="h-4 bg-slate-100 rounded"></div></td></tr>`,
  ).join("");
}

function renderTableError(message) {
  $("student-table-body").innerHTML =
    `<tr><td colspan="7" class="py-10 text-center text-rose-500"><i class="fa-solid fa-circle-exclamation text-2xl mb-2 block"></i>${escapeHtml(message)}</td></tr>`;
}

function setButtonLoading(button, loading, label) {
  if (loading) {
    button.dataset.original = button.innerHTML;
    button.disabled = true;
    button.innerHTML = `<i class="fa-solid fa-circle-notch fa-spin"></i> ${escapeHtml(label)}`;
  } else {
    button.disabled = false;
    if (button.dataset.original) button.innerHTML = button.dataset.original;
  }
}

function setInlineError(id, message) {
  const element = $(id);
  element.textContent = message;
  element.classList.toggle("hidden", !message);
}

function renderFieldErrors(errors) {
  Object.entries(errors || {}).forEach(([field, message]) => {
    const input = $(field);
    const error = document.querySelector(`[data-error-for="${field}"]`);
    if (input) input.classList.add("border-rose-500", "ring-1", "ring-rose-200");
    if (error) {
      error.textContent = message;
      error.classList.remove("hidden");
    }
  });
}

function clearFieldError(field) {
  const input = $(field);
  const error = document.querySelector(`[data-error-for="${field}"]`);
  if (input) input.classList.remove("border-rose-500", "ring-1", "ring-rose-200");
  if (error) {
    error.textContent = "";
    error.classList.add("hidden");
  }
}

function clearFieldErrors() {
  document
    .querySelectorAll("[data-error-for]")
    .forEach((error) => clearFieldError(error.dataset.errorFor));
}

function showToast(message, type = "success") {
  const toast = document.createElement("div");
  toast.className = `pointer-events-auto flex items-start gap-3 rounded-lg border bg-white px-4 py-3 shadow-lg ${type === "error" ? "border-rose-200 text-rose-800" : "border-emerald-200 text-emerald-800"}`;
  toast.innerHTML = `<i class="fa-solid ${type === "error" ? "fa-circle-exclamation" : "fa-circle-check"} mt-0.5"></i><span class="text-sm font-medium">${escapeHtml(message)}</span>`;
  $("toast-container").appendChild(toast);
  setTimeout(() => toast.remove(), 4500);
}

function formatCurrency(value) {
  return `₹${Number(value || 0).toFixed(2)}`;
}

function formatDateTime(value) {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? String(value ?? "") : date.toLocaleString();
}

function getCookie(name) {
  const prefix = `${name}=`;
  const row = document.cookie.split("; ").find((item) => item.startsWith(prefix));
  return row ? row.substring(prefix.length) : undefined;
}

function escapeHtml(value) {
  return String(value ?? "").replace(
    /[&<>"']/g,
    (character) =>
      ({
        "&": "&amp;",
        "<": "&lt;",
        ">": "&gt;",
        '"': "&quot;",
        "'": "&#039;",
      })[character],
  );
}
