document.addEventListener('DOMContentLoaded', () => {
    const csrfToken = document.querySelector('meta[name="_csrf"]').content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]').content;

    const logsContainer = document.getElementById('logs');
    const logForm = document.getElementById('logForm');
    const addLogBtn = document.getElementById('addLogBtn');
    const searchInput = document.getElementById('search');
    const searchBtn = document.getElementById('searchBtn');
    const sortSelect = document.getElementById('sort');
    const startDateInput = document.getElementById('startDate');
    const endDateInput = document.getElementById('endDate');
    const minTimeInput = document.getElementById('minTime');
    const maxTimeInput = document.getElementById('maxTime');
    const applyFiltersBtn = document.getElementById('applyFilters');
    const resetFiltersBtn = document.getElementById('resetFilters');
    const filterToggle = document.getElementById('filterToggle');
    const filterControls = document.getElementById('filterControls');
    
    let logs = [];
    let isEditing = false;
    let currentLogId = null;

    function init() {
        setupEventListeners();
        fetchLogs();
    }

    function setupEventListeners() {
        addLogBtn.addEventListener('click', showAddForm);
        logForm.addEventListener('submit', handleFormSubmit);

        searchBtn.addEventListener('click', filterAndRenderLogs);
        searchInput.addEventListener('keyup', (e) => {
            if (e.key === 'Enter') filterAndRenderLogs();
        });

        sortSelect.addEventListener('change', filterAndRenderLogs);
        applyFiltersBtn.addEventListener('click', applyFilters);
        resetFiltersBtn.addEventListener('click', resetFilters);
        filterToggle.addEventListener('click', toggleFilters);

        minTimeInput.addEventListener('input', validateTimeInput);
        maxTimeInput.addEventListener('input', validateTimeInput);

        setupModalCloseHandlers();
    }

    function toggleFilters() {
        filterControls.classList.toggle('expanded');
        filterToggle.classList.toggle('active');
    }

    function validateTimeInput(e) {
        if (e.target.value < 0) e.target.value = 0;
    }

    function setupModalCloseHandlers() {
        document.querySelectorAll('.modal-close, #cancelBtn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const modal = e.target.closest('.modal');
                if (modal) hideModal(modal.id);
            });
        });

        window.addEventListener('click', (e) => {
            if (e.target.classList.contains('modal')) {
                hideModal(e.target.id);
            }
        });

        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape') {
                document.querySelectorAll('.modal').forEach(modal => {
                    if (modal.style.display === 'flex') {
                        hideModal(modal.id);
                    }
                });
            }
        });
    }

    async function fetchLogs() {
        logsContainer.innerHTML = '<div class="loading">Loading logs...</div>';
        try {
            const response = await fetch('/api/reading-logs', {
                credentials: 'include',
                headers: { [csrfHeader]: csrfToken }
            });
            logs = await response.json();
            renderLogs(logs);
        } catch (error) {
            console.error('Error fetching logs:', error);
            showToast('Failed to load logs', 'error');
        }
    }

    function renderLogs(logsToRender) {
        if (!logsToRender || logsToRender.length === 0) {
            showEmptyState();
            return;
        }

        logsContainer.innerHTML = '';
        logsToRender.forEach(log => {
            const logElement = createLogCard(log);
            setupLogCardButtons(logElement, log);
            logsContainer.appendChild(logElement);
        });
    }

    function createLogCard(log) {
        const progress = log.totalPages ? Math.round((log.currentPage / log.totalPages) * 100) : 0;
        
        const logElement = document.createElement('div');
        logElement.className = 'log-card';
        logElement.innerHTML = `
            <div class="log-card-inner">
                <div class="log-card-front">
                    <h3>${log.title}</h3>
                    <p>by ${log.author}</p>
                    ${log.totalPages ? `
                    <div class="log-progress">
                        <div class="log-progress-bar" style="width: ${progress}%"></div>
                    </div>
                    <p>Progress: ${progress}% (${log.currentPage}/${log.totalPages})</p>
                    ` : ''}
                    <div class="log-meta">
                        <span>${new Date(log.date).toLocaleDateString()}</span>
                        <span>${log.timeSpent} min</span>
                    </div>
                </div>
                <div class="log-card-back">
                    <div>
                        <h3>${log.title}</h3>
                        <p>by ${log.author}</p>
                        <p>Date: ${new Date(log.date).toLocaleDateString()}</p>
                        <p>Time: ${log.timeSpent} min</p>
                        ${log.notes ? `<p>${log.notes.substring(0, 50)}${log.notes.length > 50 ? '...' : ''}</p>` : ''}
                    </div>
                    <div class="log-actions">
                        <button class="view-btn" data-id="${log.id}">
                            <i class="fas fa-eye"></i> Details
                        </button>
                        <button class="edit-btn" data-id="${log.id}">
                            <i class="fas fa-edit"></i> Edit
                        </button>
                        <button class="delete-btn" data-id="${log.id}">
                            <i class="fas fa-trash"></i> Delete
                        </button>
                    </div>
                </div>
            </div>
        `;
        return logElement;
    }

    function setupLogCardButtons(logElement, log) {
        logElement.querySelector('.view-btn').addEventListener('click', () => showLogDetails(log.id));
        logElement.querySelector('.edit-btn').addEventListener('click', () => editLog(log.id));
        logElement.querySelector('.delete-btn').addEventListener('click', () => deleteLog(log.id));
    }

    function showEmptyState() {
        logsContainer.innerHTML = `
            <div class="empty-state">
                <p>No reading logs found</p>
                <button class="btn" id="addFirstLog">
                    <i class="fas fa-plus"></i> Add Log
                </button>
            </div>
        `;
        document.getElementById('addFirstLog').addEventListener('click', showAddForm);
    }

    async function showLogDetails(logOrId) {
        let log;
        
        if (typeof logOrId === 'object') {
            log = logOrId;
        } else {
            try {
                const response = await fetch(`/api/reading-logs/${logOrId}`, {
                    credentials: 'include',
                    headers: { [csrfHeader]: csrfToken }
                });
                log = await response.json();
            } catch (error) {
                console.error('Error fetching log details:', error);
                showToast('Failed to load log details', 'error');
                return;
            }
        }

        document.getElementById('detailTitle').textContent = log.title;
        document.getElementById('detailAuthor').textContent = log.author;
        document.getElementById('detailDate').textContent = new Date(log.date).toLocaleDateString();
        document.getElementById('detailTime').textContent = log.timeSpent;
        document.getElementById('detailCurrentPage').textContent = log.currentPage || 'N/A';
        document.getElementById('detailTotalPages').textContent = log.totalPages || 'N/A';
        document.getElementById('detailNotes').textContent = log.notes || 'No notes';

        if (log.currentPage && log.totalPages) {
            const progress = Math.round((log.currentPage / log.totalPages) * 100);
            document.getElementById('detailProgress').textContent = `${progress}%`;
        } else {
            document.getElementById('detailProgress').textContent = 'N/A';
        }
        
        const viewHistoryBtn = document.getElementById('viewHistoryBtn');
        viewHistoryBtn.dataset.logId = log.id;
        viewHistoryBtn.onclick = () => toggleReadingHistory(log.id);
        
        showModal('detailModal');
    }

    async function toggleReadingHistory(logId) {
        const historyContainer = document.getElementById('historyContainer');
        const viewHistoryBtn = document.getElementById('viewHistoryBtn');
        
        if (historyContainer.style.display === 'block') {
            historyContainer.style.display = 'none';
            viewHistoryBtn.innerHTML = '<i class="fas fa-history"></i> View History';
            return;
        }
    
        viewHistoryBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Loading...';
        
        try {
            const log = await fetchLogDetails(logId);
            if (!log || !log.title || !log.author) {
                throw new Error('Invalid log data');
            }
    
            const params = new URLSearchParams({
                title: encodeURIComponent(log.title.trim()),
                author: encodeURIComponent(log.author.trim()),
                currentLogId: log.id
            });
    
            const response = await fetch(`/api/reading-logs/history?${params}`, {
                headers: { 
                    [csrfHeader]: csrfToken,
                    'Content-Type': 'application/json'
                },
                credentials: 'include'
            });
            
            if (!response.ok) {
                const errorData = await response.json().catch(() => ({}));
                throw new Error(errorData.message || 'Failed to fetch history');
            }
    
            const history = await response.json();
            
            const historyArray = Array.isArray(history) ? history : [];
            renderHistoryList(historyArray, logId);
            
            historyContainer.style.display = 'block';
            viewHistoryBtn.innerHTML = '<i class="fas fa-history"></i> Hide History';
        } catch (error) {
            console.error('Error fetching history:', error);
            showToast(error.message || 'Failed to load reading history', 'error');
            viewHistoryBtn.innerHTML = '<i class="fas fa-history"></i> View History';
            
            const historyList = document.getElementById('historyList');
            historyList.innerHTML = '<p class="no-history">Could not load reading history</p>';
        }
    }

    function renderHistoryList(history, currentLogId) {
        const historyList = document.getElementById('historyList');
        historyList.innerHTML = '';
        
        if (!history || history.length === 0) {
            historyList.innerHTML = '<p class="no-history">No previous reading sessions found for this book.</p>';
            return;
        }
        
        history.forEach(log => {
            if (!log || !log.id || !log.date) return;
            
            const historyItem = document.createElement('div');
            historyItem.className = 'history-item';
            historyItem.dataset.logId = log.id;
            
            const date = new Date(log.date).toLocaleDateString();
            const isCurrent = log.id.toString() === currentLogId.toString();
            
            historyItem.innerHTML = `
                <div>
                    <span class="history-date">${date}</span>
                    ${isCurrent ? '<span class="badge current">Current</span>' : ''}
                </div>
                <div>
                    ${log.currentPage ? `<span class="history-pages">Page ${log.currentPage}</span>` : ''}
                    ${log.timeSpent ? `<span class="history-time">${log.timeSpent} mins</span>` : ''}
                </div>
            `;
            
            historyItem.style.cursor = 'pointer';
            historyItem.addEventListener('click', () => showLogDetails(log.id));
            
            historyList.appendChild(historyItem);
        });
    }

    async function fetchLogDetails(id) {
        try {
            const response = await fetch(`/api/reading-logs/${id}`, {
                credentials: 'include'
            });
            return await response.json();
        } catch (error) {
            console.error('Error fetching log details:', error);
            showError('Failed to load log details');
        }
    }

    function showAddForm() {
        isEditing = false;
        currentLogId = null;
        document.getElementById('formTitle').textContent = 'Add New Reading Log';
        document.getElementById('logForm').querySelector('button[type="submit"]').textContent = 'Add Log';
        logForm.reset();
        showModal('formModal');
    }

    async function editLog(id) {
        const log = logs.find(l => l.id === id);
        if (!log) return;
        
        isEditing = true;
        currentLogId = id;
        document.getElementById('formTitle').textContent = 'Edit Reading Log';
        document.getElementById('logForm').querySelector('button[type="submit"]').textContent = 'Update Log';
        
        document.getElementById('logId').value = log.id;
        document.getElementById('title').value = log.title;
        document.getElementById('author').value = log.author;
        document.getElementById('date').value = log.date.split('T')[0];
        document.getElementById('timeSpent').value = log.timeSpent;
        document.getElementById('currentPage').value = log.currentPage;
        document.getElementById('totalPages').value = log.totalPages;
        document.getElementById('notes').value = log.notes || '';
        
        showModal('formModal');
    }

    async function handleFormSubmit(e) {
        e.preventDefault();
        clearAllErrors();

        if (!validateForm()) return;

        const logData = {
            title: document.getElementById('title').value.trim(),
            author: document.getElementById('author').value.trim(),
            date: document.getElementById('date').value,
            timeSpent: parseInt(document.getElementById('timeSpent').value),
            currentPage: parseInt(document.getElementById('currentPage').value) || null,
            totalPages: parseInt(document.getElementById('totalPages').value) || null,
            notes: document.getElementById('notes').value.trim()
        };

        if (isEditing) {
            const originalLog = logs.find(l => l.id === currentLogId);
            if (!hasChanges(originalLog, logData)) {
                hideModal('formModal');
                return;
            }
        } else {
            const existingLogs = logs.filter(log => 
                log.title.toLowerCase() === logData.title.toLowerCase() && 
                log.author.toLowerCase() === logData.author.toLowerCase()
            );
    
            if (existingLogs.length > 0) {
                const mismatchedLog = existingLogs.find(log => 
                    log.totalPages && logData.totalPages && 
                    log.totalPages !== logData.totalPages
                );
    
                if (mismatchedLog) {
                    const proceed = await showPageMismatchError(
                        mismatchedLog.totalPages, 
                        logData.totalPages
                    );
                    if (!proceed) return;
                }
            }
        }
        
        await saveLog(logData);
    }

    function validateForm() {
        let isValid = true;

        const dateInput = document.getElementById('date').value;
        const selectedDate = new Date(dateInput);
        const today = new Date();
        selectedDate.setHours(0, 0, 0, 0);
        today.setHours(0, 0, 0, 0);
        
        if (selectedDate > today) {
            showError('date', 'Date cannot be in the future');
            isValid = false;
        }

        const currentPage = parseInt(document.getElementById('currentPage').value);
        const totalPages = parseInt(document.getElementById('totalPages').value);
        
        if (currentPage && totalPages && currentPage > totalPages) {
            showError('currentPage', 'Current page cannot exceed total pages');
            isValid = false;
        }

        if (!document.getElementById('title').value.trim()) {
            showError('title', 'Title is required');
            isValid = false;
        }

        if (!document.getElementById('author').value.trim()) {
            showError('author', 'Author is required');
            isValid = false;
        }

        return isValid;
    }

    async function saveLog(logData) {
        const url = isEditing ? `/api/reading-logs/${currentLogId}` : '/api/reading-logs';
        const method = isEditing ? 'PUT' : 'POST';
        
        try {
            const response = await fetch(url, {
                method,
                headers: {
                    'Content-Type': 'application/json',
                    [csrfHeader]: csrfToken
                },
                credentials: 'include',
                body: JSON.stringify(logData)
            });

            const result = await response.json();

            if (!response.ok) {
                if (result.error && result.error.startsWith("PAGE_COUNT_MISMATCH:")) {
                    const [_, existingPages, newPages] = result.error.split(':');
                    showPageMismatchError(existingPages, newPages);
                    return;
                }
                throw new Error(result.error || 'Failed to save reading log');
            }
            
            hideModal('formModal');
            await fetchLogs();
            showToast(`Reading log ${isEditing ? 'updated' : 'created'} successfully`, 'success');
        } catch (error) {
            console.error('Error saving log:', error);
            
            if (error.error && error.error.includes("This book already has a recorded total of")) {
                const match = error.error.match(/recorded total of (\d+).*change it to (\d+)/);
                if (match) {
                    showPageMismatchError(match[1], match[2]);
                } else {
                    showToast(error.error, 'error');
                }
            }
        }
    }

    function showPageMismatchError(existingPages, newPages) {
        clearAllErrors();
        
        const totalPagesInput = document.getElementById('totalPages');
        totalPagesInput.classList.add('error-input');
        
        const errorElement = document.getElementById('totalPages-error');
        errorElement.textContent = `This book is already recorded with ${existingPages} pages. ` +
                                  `You cannot change it to ${newPages} pages.`;
        errorElement.style.display = 'block';
        
        totalPagesInput.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }

    async function deleteLog(id) {
        const confirmed = await showConfirmation({
            title: 'Delete Reading Log',
            message: 'Are you sure you want to delete this reading log?',
            confirmText: 'Delete',
            danger: true
        });

        if (!confirmed) return;
        
        try {
            const response = await fetch(`/api/reading-logs/${id}`, {
                method: 'DELETE',
                headers: { 
                    [csrfHeader]: csrfToken,
                    'Content-Type': 'application/json'
                },
                credentials: 'include'
            });

            if (!response.ok) {
                const errorData = await response.json().catch(() => ({}));
                throw new Error(errorData.error || 'Failed to delete reading log');
            }
            
            await fetchLogs();
            showToast('Reading log deleted successfully', 'success');
        } catch (error) {
            console.error('Error deleting log:', error);
            showToast('Failed to delete reading log', 'error');
        }
    }

    async function showConfirmation(options) {
        return new Promise((resolve) => {
            const modal = document.getElementById('confirmationModal');
            const title = document.getElementById('confirmationTitle');
            const message = document.getElementById('confirmationMessage');
            const confirmBtn = document.getElementById('confirmActionBtn');
            const cancelBtn = document.getElementById('confirmCancelBtn');
            
            title.textContent = options.title || 'Confirm Action';
            message.textContent = options.message || 'Are you sure you want to perform this action?';
            confirmBtn.textContent = options.confirmText || 'Confirm';
            confirmBtn.className = `btn ${options.danger ? 'danger' : 'primary'}`;
            
            showModal('confirmationModal');
            
            const cleanUp = () => {
                confirmBtn.onclick = null;
                cancelBtn.onclick = null;
                hideModal('confirmationModal');
            };
            
            confirmBtn.onclick = () => {
                cleanUp();
                resolve(true);
            };
            
            cancelBtn.onclick = () => {
                cleanUp();
                resolve(false);
            };
        });
    }

    function showToast(message, type = 'info', duration = 3000) {
        const container = document.getElementById('toastContainer');
        const toast = document.createElement('div');
        toast.className = `toast ${type}`;
        
        let icon;
        switch(type) {
            case 'success': icon = 'fa-check-circle'; break;
            case 'error': icon = 'fa-exclamation-circle'; break;
            default: icon = 'fa-info-circle';
        }
        
        toast.innerHTML = `
            <i class="fas ${icon}"></i>
            <span>${message}</span>
        `;
        
        container.appendChild(toast);
        
        setTimeout(() => {
            toast.classList.add('toast-out');
            setTimeout(() => toast.remove(), 300);
        }, duration);
    }

    function showModal(id) {
        document.getElementById(id).style.display = 'flex';
        document.body.style.overflow = 'hidden';
    }

    function hideModal(id) {
        document.getElementById(id).style.display = 'none';
        document.body.style.overflow = 'auto';
        clearAllErrors();

        if (id === 'detailModal') {
            const historyContainer = document.getElementById('historyContainer');
            const viewHistoryBtn = document.getElementById('viewHistoryBtn');
            
            historyContainer.style.display = 'none';
            viewHistoryBtn.innerHTML = '<i class="fas fa-history"></i> View History';
        }
    }

    function clearAllErrors() {
        document.querySelectorAll('.error-message').forEach(el => {
            el.textContent = '';
            el.style.display = 'none';
        });
        
        document.querySelectorAll('.error-input').forEach(el => {
            el.classList.remove('error-input');
        });
    }

    function showError(fieldId, message) {
        const field = document.getElementById(fieldId);
        if (!field) return;
        
        let errorElement = document.getElementById(`${fieldId}-error`);
        if (!errorElement) {
            errorElement = document.createElement('div');
            errorElement.id = `${fieldId}-error`;
            errorElement.className = 'error-message';
            field.parentNode.insertBefore(errorElement, field.nextSibling);
        }
        
        field.classList.add('error-input');
        errorElement.textContent = message;
        errorElement.style.display = 'block';
    }

    function hasChanges(original, updated) {
        return original.title !== updated.title ||
               original.author !== updated.author ||
               original.date !== updated.date ||
               original.timeSpent !== updated.timeSpent ||
               original.currentPage !== updated.currentPage ||
               original.totalPages !== updated.totalPages ||
               original.notes !== updated.notes;
    }

    async function applyFilters() {
        try {
            const params = new URLSearchParams();
            
            if (searchInput.value) params.append('query', searchInput.value);
            if (startDateInput.value) params.append('startDate', startDateInput.value);
            if (endDateInput.value) params.append('endDate', endDateInput.value);
            if (minTimeInput.value) params.append('minTime', minTimeInput.value);
            if (maxTimeInput.value) params.append('maxTime', maxTimeInput.value);
            params.append('sort', sortSelect.value);

            const response = await fetch(`/api/reading-logs/filter?${params}`, {
                credentials: 'include',
                headers: { [csrfHeader]: csrfToken }
            });

            if (!response.ok) throw new Error('Failed to fetch filtered logs');
            
            const filteredLogs = await response.json();
            renderLogs(filteredLogs);
        } catch (error) {
            console.error('Error applying filters:', error);
            const filteredLogs = filterLogsLocally(logs);
            renderLogs(filteredLogs);
        }
    }

    function resetFilters() {
        searchInput.value = '';
        startDateInput.value = '';
        endDateInput.value = '';
        minTimeInput.value = '';
        maxTimeInput.value = '';
        sortSelect.value = 'date-desc';
        fetchLogs();
    }

    function filterLogsLocally(logsToFilter) {
        const searchTerm = searchInput.value.toLowerCase();
        const startDate = startDateInput.value;
        const endDate = endDateInput.value;
        const minTime = minTimeInput.value ? parseInt(minTimeInput.value) : 0;
        const maxTime = maxTimeInput.value ? parseInt(maxTimeInput.value) : Infinity;

        return logsToFilter.filter(log => {
            const matchesSearch = !searchTerm || 
                log.title.toLowerCase().includes(searchTerm) || 
                log.author.toLowerCase().includes(searchTerm) ||
                (log.notes && log.notes.toLowerCase().includes(searchTerm));
            
            const logDate = new Date(log.date);
            const matchesDate = (!startDate || logDate >= new Date(startDate)) && 
                               (!endDate || logDate <= new Date(endDate));
            
            const matchesTime = log.timeSpent >= minTime && log.timeSpent <= maxTime;
            
            return matchesSearch && matchesDate && matchesTime;
        });
    }

    function filterAndRenderLogs() {
        const searchTerm = searchInput.value.toLowerCase();
        const sortOption = sortSelect.value;
        
        let filteredLogs = logs.filter(log => 
            log.title.toLowerCase().includes(searchTerm) || 
            log.author.toLowerCase().includes(searchTerm) ||
            (log.notes && log.notes.toLowerCase().includes(searchTerm))
        );
        
        filteredLogs = sortLogs(filteredLogs, sortOption);
        renderLogs(filteredLogs);
    }

    function sortLogs(logs, option) {
        const [field, direction] = option.split('-');
        
        return [...logs].sort((a, b) => {
            let comparison = 0;
            
            switch (field) {
                case 'date':
                    comparison = new Date(a.date) - new Date(b.date);
                    break;
                case 'time':
                    comparison = a.timeSpent - b.timeSpent;
                    break;
                case 'title':
                    comparison = a.title.localeCompare(b.title);
                    break;
            }
            
            return direction === 'desc' ? -comparison : comparison;
        });
    }

    init();
});