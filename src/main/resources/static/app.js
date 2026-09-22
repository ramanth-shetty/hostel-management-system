const API_BASE = '/api/students';

document.addEventListener('DOMContentLoaded', () => {
    fetchStudents();
    document.getElementById('student-form').addEventListener('submit', handleFormSubmit);
});

async function fetchStudents() {
    try {
        const response = await fetch(API_BASE);
        const students = await response.json();
        renderTable(students);
        updateStats(students);
    } catch (error) {
        console.error('Error loading students:', error);
    }
}

async function filterPendingOnly() {
    try {
        const response = await fetch(`${API_BASE}/reports/pending-fees?minAmount=0.01`);
        const students = await response.json();
        renderTable(students);
    } catch (error) {
        console.error('Error filtering pending fees:', error);
    }
}

function renderTable(students) {
    const tbody = document.getElementById('student-table-body');
    tbody.innerHTML = '';

    if (students.length === 0) {
        tbody.innerHTML = `<tr><td colspan="7" class="text-center py-8 text-slate-400">No records found.</td></tr>`;
        return;
    }

    students.forEach((student, index) => {
        const tr = document.createElement('tr');
        tr.className = "hover:bg-slate-50/80 transition";
        
        const hasPending = parseFloat(student.pendingFees) > 0;
        
        // Display consecutive index (#1, #2, #3...) instead of raw database primary key (#student.id)
        tr.innerHTML = `
            <td class="py-3 px-4 font-mono text-xs text-slate-500">#${index + 1}</td>
            <td class="py-3 px-4 font-medium text-slate-900">${escapeHtml(student.name)}</td>
            <td class="py-3 px-4"><span class="px-2 py-1 bg-slate-100 rounded text-xs font-semibold text-slate-700">${escapeHtml(student.roomNumber)}</span></td>
            <td class="py-3 px-4 text-slate-600">${student.admissionDate}</td>
            <td class="py-3 px-4 text-right font-medium text-emerald-600">₹${parseFloat(student.feesPaid).toFixed(2)}</td>
            <td class="py-3 px-4 text-right font-medium ${hasPending ? 'text-rose-600' : 'text-slate-400'}">₹${parseFloat(student.pendingFees).toFixed(2)}</td>
            <td class="py-3 px-4 text-center">
                <div class="flex justify-center items-center space-x-2">
                    <button onclick="editStudent(${student.id})" class="p-1.5 text-slate-500 hover:text-brand-600 hover:bg-slate-100 rounded transition" title="Edit">
                        <i class="fa-solid fa-pen-to-square"></i>
                    </button>
                    <button onclick="deleteStudent(${student.id})" class="p-1.5 text-slate-500 hover:text-rose-600 hover:bg-rose-50 rounded transition" title="Delete">
                        <i class="fa-solid fa-trash"></i>
                    </button>
                </div>
            </td>
        `;
        tbody.appendChild(tr);
    });
}

function updateStats(students) {
    let total = students.length;
    let collected = 0;
    let pending = 0;

    students.forEach(s => {
        collected += parseFloat(s.feesPaid || 0);
        pending += parseFloat(s.pendingFees || 0);
    });

    document.getElementById('stat-total').innerText = total;
    document.getElementById('stat-collected').innerText = `₹${collected.toFixed(2)}`;
    document.getElementById('stat-pending').innerText = `₹${pending.toFixed(2)}`;
}

async function handleFormSubmit(e) {
    e.preventDefault();

    const id = document.getElementById('student-id').value;
    const payload = {
        name: document.getElementById('name').value,
        roomNumber: document.getElementById('roomNumber').value,
        admissionDate: document.getElementById('admissionDate').value,
        feesPaid: parseFloat(document.getElementById('feesPaid').value),
        pendingFees: parseFloat(document.getElementById('pendingFees').value)
    };

    const isEdit = Boolean(id);
    const url = isEdit ? `${API_BASE}/${id}` : API_BASE;
    const method = isEdit ? 'PUT' : 'POST';

    try {
        const response = await fetch(url, {
            method: method,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        if (response.ok) {
            resetForm();
            fetchStudents();
        } else {
            const errData = await response.json();
            alert(`Error: ${errData.message || 'Failed to save record'}`);
        }
    } catch (error) {
        console.error('Submission error:', error);
    }
}

async function editStudent(id) {
    try {
        const response = await fetch(`${API_BASE}/${id}`);
        const student = await response.json();

        document.getElementById('student-id').value = student.id;
        document.getElementById('name').value = student.name;
        document.getElementById('roomNumber').value = student.roomNumber;
        document.getElementById('admissionDate').value = student.admissionDate;
        document.getElementById('feesPaid').value = student.feesPaid;
        document.getElementById('pendingFees').value = student.pendingFees;

        document.getElementById('form-title').innerText = 'Edit Resident Details';
        document.getElementById('edit-badge').classList.remove('hidden');
        document.getElementById('btn-cancel').classList.remove('hidden');
        document.getElementById('btn-submit').innerHTML = `<i class="fa-solid fa-floppy-disk"></i> Update Resident`;
    } catch (error) {
        console.error('Error fetching details:', error);
    }
}

async function deleteStudent(id) {
    if (!confirm('Are you sure you want to remove this resident record?')) return;

    try {
        const response = await fetch(`${API_BASE}/${id}`, { method: 'DELETE' });
        if (response.ok) {
            fetchStudents();
        }
    } catch (error) {
        console.error('Delete error:', error);
    }
}

function resetForm() {
    document.getElementById('student-form').reset();
    document.getElementById('student-id').value = '';
    document.getElementById('form-title').innerText = 'Add New Resident';
    document.getElementById('edit-badge').classList.add('hidden');
    document.getElementById('btn-cancel').classList.add('hidden');
    document.getElementById('btn-submit').innerHTML = `<i class="fa-solid fa-plus"></i> Register Resident`;
}

function escapeHtml(str) {
    return str.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;").replace(/'/g, "&#039;");
}