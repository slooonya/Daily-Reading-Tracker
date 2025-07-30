document.addEventListener('DOMContentLoaded', () => {
    if (!document.querySelector('meta[name="_csrf"]')) {
        window.location.href = '/auth';
        return;
    }

    const logsContainer = document.getElementById('logs');
    const logForm = document.getElementById('logForm');
    const cancelBtn = document.getElementById('cancelBtn');
    const searchInput = document.getElementById('search');
    const searchBtn = document.getElementById('searchBtn');
    const sortSelect = document.getElementById('sort');
    const applyFiltersBtn = document.getElementById('applyFilters');
    const resetFiltersBtn = document.getElementById('resetFilters');
    const startDateInput = document.getElementById('startDate');
    const endDateInput = document.getElementById('endDate');
    const minTimeInput = document.getElementById('minTime');
    const maxTimeInput = document.getElementById('maxTime');
    const filterToggle = document.getElementById('filterToggle');
    const filterControls = document.getElementById('filterControls');

    let logs = [];
    let isEditing = false;
    let currentLogId = null;
    let searchTerm = '';
    let sortOption = 'date-desc';

    function init() {
        fetchLogs();
        setupEventListeners();
    }

    function setupFilterToggle() {
        if (!filterToggle || !filterControls) return;
        
        filterToggle.addEventListener('click', () => {
            filterControls.classList.toggle('expanded');
            filterToggle.classList.toggle('active');
        });
    }
    
    setupFilterToggle();

    if (filterToggle && filterControls) {
        filterToggle.addEventListener('click', () => {
            filterControls.classList.toggle('expanded');
            filterToggle.classList.toggle('active');
        });
    }

    function setupEventListeners() {
        logForm.addEventListener('submit', handleFormSubmit);

        cancelBtn.addEventListener('click', resetForm);

        document.querySelector("#detailModal .modal-close").addEventListener("click", () => {
            document.getElementById("detailModal").style.display = "none";
        });

        document.addEventListener('click', (e) => {
            if (e.target.classList.contains('modal')) {
                e.target.style.display = 'none';
            }
            
            if (e.target.classList.contains('edit-btn') || e.target.closest('.edit-btn')) {
                const logId = e.target.dataset.id || e.target.closest('.edit-btn').dataset.id;
                editLog(logId);
            }
        });

        filterToggle.addEventListener('click', () => {
            filterControls.classList.toggle('expanded');
            filterToggle.classList.toggle('active');
        });

        searchBtn.addEventListener('click', () => {
            searchTerm = searchInput.value.toLowerCase();
            renderLogs();
        });

        searchInput.addEventListener('keyup', (e) => {
            if (e.key === 'Enter') {
                searchTerm = searchInput.value.toLowerCase();
                renderLogs();
            }
        });

        sortSelect.addEventListener('change', () => {
            sortOption = sortSelect.value;
            renderLogs();
        });

        applyFiltersBtn.addEventListener('click', applyFilters);
        resetFiltersBtn.addEventListener('click', resetFilters);
    }

    function makeAuthenticatedRequest(url, options = {}) {
        options.credentials = 'include';

        const csrfToken = document.querySelector('meta[name="_csrf"]').content;
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]').content;

        options.headers = {
            ...options.headers,
            [csrfHeader]: csrfToken
        };

        return fetch(url, options)
            .then(response => {
                if (response.status === 401) {
                    window.location.href = '/auth';
                    return Promise.reject('Unauthorized');
                }
                return response;
            });
    }

    async function fetchLogs() {
        logsContainer.innerHTML = '<div class="loading">Loading logs...</div>';

        try {
            const response = await makeAuthenticatedRequest('/sorted_loglist_allusers');
            if (!response.ok) {
                throw new Error('Failed to fetch logs');
            }
            const data = await response.json();
            
            logs = Array.isArray(data) ? data : [];
            renderLogs();
        } catch (error) {
            console.error('Error fetching logs:', error);
            logs = [];
            logsContainer.innerHTML = `<div class="empty-state">Error loading logs: ${error.message}</div>`;
        }
    }

    function renderLogs() {
        if (!Array.isArray(logs)) {
            console.error('Logs is not an array:', logs);
            logs = [];
        }

        let filteredLogs = logs.filter(log => {
            if (!searchTerm) return true;

            return log.title.toLowerCase().includes(searchTerm) ||
                log.author.toLowerCase().includes(searchTerm) ||
                log.notes?.toLowerCase().includes(searchTerm);
        });

        filteredLogs = sortLogs(filteredLogs, sortOption);

        logsContainer.innerHTML = '';

        if (filteredLogs.length === 0) {
            const noResults = document.createElement('div');
            noResults.className = 'empty-state search-empty';
            noResults.innerHTML = `
                <i class="fas fa-search fa-2x"></i>
                <h3>No matching logs found</h3>
                <p>Try different search terms</p>
            `;
            logsContainer.appendChild(noResults);
            return;
        }

        filteredLogs.forEach(log => {
            const logElement = createLogElement(log);
            logsContainer.appendChild(logElement);
        });
    }

    function editLog(logId) {
        makeAuthenticatedRequest(`/sorted_loglist_allusers/${logId}`)
            .then(response => response.json())
            .then(log => {
                document.getElementById('logId').value = log.id;
                document.getElementById('title').value = log.title;
                document.getElementById('author').value = log.author;
                document.getElementById('date').value = log.date.split('T')[0];
                document.getElementById('timeSpent').value = log.timeSpent;
                document.getElementById('currentPage').value = log.currentPage;
                document.getElementById('totalPages').value = log.totalPages;
                document.getElementById('notes').value = log.notes || '';
                
                isEditing = true;
                currentLogId = logId;
                document.getElementById('formTitle').textContent = 'Edit Reading Log';
                document.getElementById('formModal').style.display = 'flex';
            })
            .catch(error => {
                console.error('Error fetching log:', error);
                showToast('Failed to load log for editing', 'error');
            });
    }

    function handleFormSubmit(e) {
        e.preventDefault();
        clearAllErrors();

        if (!validateForm()) return;

        const logData = {
            title: document.getElementById('title').value.trim(),
            author: document.getElementById('author').value.trim(),
            date: document.getElementById('date').value,
            timeSpent: parseInt(document.getElementById('timeSpent').value) || 0,
            currentPage: parseInt(document.getElementById('currentPage').value) || null,
            totalPages: parseInt(document.getElementById('totalPages').value) || null,
            notes: document.getElementById('notes').value.trim()
        };

        if (isEditing) {
            submitLogUpdate(currentLogId, logData);
        } else {
            createLog(logData);
        }
    }

    async function submitLogUpdate(logId, logData) {
        try {
            const response = await makeAuthenticatedRequest(`/sorted_loglist_allusers/${logId}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(logData)
            });
    
            if (!response.ok) {
                const errorData = await response.json();
                if (errorData.error && errorData.error.startsWith("PAGE_COUNT_MISMATCH:")) {
                    handlePageMismatchError(errorData.error);
                    return;
                }
                throw new Error(errorData.error || 'Failed to update log');
            }
    
            hideModal('formModal');
            await fetchLogs();
            showToast('Reading log updated successfully', 'success');
        } catch (error) {
            console.error('Error updating log:', error);
            showToast(error.message || 'Failed to update reading log', 'error');
        }
    }

    function handlePageMismatchError(errorMessage) {
        const parts = errorMessage.split(':');
        const existingPages = parts[1];
        const newPages = parts[2];
        
        showError('totalPages', 
            `This book already has ${existingPages} pages recorded. ` +
            `You cannot change it to ${newPages} pages.`);
    }

    function validateForm() {
        let isValid = true;

        if (!document.getElementById('title').value.trim()) {
            showError('title', 'Title is required');
            isValid = false;
        }

        if (!document.getElementById('author').value.trim()) {
            showError('author', 'Author is required');
            isValid = false;
        }

        const dateInput = document.getElementById('date').value;
        const selectedDate = new Date(dateInput);
        const today = new Date();
        selectedDate.setHours(0, 0, 0, 0);
        today.setHours(0, 0, 0, 0);
        
        if (selectedDate > today) {
            showError('date', 'Date cannot be in the future');
            isValid = false;
        }

        const currentPage = parseInt(document.getElementById('currentPage').value) || 0;
        const totalPages = parseInt(document.getElementById('totalPages').value) || 0;
        
        if (currentPage > totalPages) {
            showError('currentPage', 'Current page cannot exceed total pages');
            isValid = false;
        }

        return isValid;
    }

    function createLog(logData) {
        makeAuthenticatedRequest('/sorted_loglist_allusers', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(logData)
        })
        .then(response => {
            if (!response.ok) {
                return response.json().then(err => Promise.reject(err));
            }
            return response.json();
        })
        .then(() => {
            resetForm();
            fetchLogs();
            showToast('Reading log created successfully', 'success');
            document.getElementById('formModal').style.display = 'none';
        })
        .catch(error => {
            console.error('Error creating log:', error);
            showToast(error.error || error.message || 'Failed to create reading log', 'error');
        });
    }

    function createLogElement(log) {
        const logElement = document.createElement('div');
        logElement.className = 'log-entry';
    
        const progress = log.totalPages ? Math.round((log.currentPage / log.totalPages) * 100) : 0;
    
        logElement.innerHTML = `
            <div class="log-card">
                <div class="log-header">
                    <h3 class="log-title">${log.title}</h3>
                    <p class="log-author">by ${log.author}</p>
                </div>
                
                ${log.totalPages ? `
                <div class="log-progress-container">
                    <div class="log-progress">
                        <div class="log-progress-bar" style="width: ${progress}%"></div>
                    </div>
                    <span class="progress-text">${progress}% (${log.currentPage}/${log.totalPages})</span>
                </div>
                ` : ''}
                
                <div class="log-meta">
                    <div class="meta-item">
                    <i class="fas fa-user"></i>
                    <span>${log.userName || 'Unknown User'}</span>
                    </div>
                    <div class="meta-item">
                    <i class="fas fa-calendar-alt"></i>
                    <span>${new Date(log.createdAt).toLocaleDateString()}</span>
                    </div>
                </div>
                
                ${log.notes ? `
                <div class="log-notes">
                    <h4>Notes:</h4>
                    <p>${log.notes.substring(0, 100)}${log.notes.length > 100 ? '...' : ''}</p>
                </div>
                ` : ''}
                
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
        `;
    
        logElement.querySelector('.view-btn').addEventListener('click', () => viewDetails(log.id));
        logElement.querySelector('.delete-btn').addEventListener('click', () => deleteLog(log.id));
    
        return logElement;
    }

    function viewDetails(logId) {
        makeAuthenticatedRequest(`/sorted_loglist_allusers/${logId}`)
            .then(response => {
                if (!response.ok) {
                    return response.json().then(err => Promise.reject(err));
                }
                return response.json();
            })
            .then(log => {
                document.getElementById('detailTitle').textContent = log.title;
                document.getElementById('detailUser').textContent = log.userName || log.user?.username || 'Unknown';
                document.getElementById('detailAuthor').textContent = log.author;
                document.getElementById('detailDate').textContent = new Date(log.createdAt).toLocaleDateString();
                document.getElementById('detailTime').textContent = log.timeSpent;
                document.getElementById('detailCurrentPage').textContent = log.currentPage || 'N/A';
                document.getElementById('detailTotalPages').textContent = log.totalPages || 'N/A';
                document.getElementById('detailProgress').textContent = log.totalPages 
                    ? `${Math.round((log.currentPage / log.totalPages) * 100)}%` 
                    : 'N/A';
                document.getElementById('detailNotes').textContent = log.notes || 'None';
                
                document.getElementById('deleteLogBtn').onclick = () => deleteLog(log.id);
                
                document.getElementById('detailModal').style.display = 'flex';
            })
            .catch(error => {
                console.error('Error fetching log details:', error);
                showToast('Failed to load log details: ' + error.message, 'error');
            });
    }

    async function deleteLog(logId) {
        const confirmed = await showConfirmation({
            title: 'Delete Reading Log',
            message: 'Are you sure you want to delete this reading log?',
            confirmText: 'Delete Log',
            danger: true
        });

        if (!confirmed) return;
        
        makeAuthenticatedRequest(`/sorted_loglist_allusers/${logId}`, {
            method: 'DELETE'   
        })
        .then(response => {
            if (!response.ok) {
                return response.json().then(err => Promise.reject(err));
            }
            return response.json();
        })
        .then(() => {
            fetchLogs();
            showToast('Reading log deleted successfully', 'success');
        })
        .catch(error => {
            console.error('Error deleting log:', error);
            showToast(error.message || 'Failed to delete reading log', 'error');
        });
    }

    function sortLogs(logs, option) {
        const [field, direction] = option.split('-');

        return [...logs].sort((a, b) => {
            let comparison = 0;

            switch (field) {
                case 'date':
                    comparison = new Date(a.createdAt) - new Date(b.createdAt);
                    break;
                case 'time':
                    comparison = a.timeSpent - b.timeSpent;
                    break;
                case 'title':
                    comparison = a.title.localeCompare(b.title);
                    break;
                case 'user':
                    comparison = (a.userName || '').localeCompare(b.userName || '');
                    break;
                default:
                    comparison = 0;
            }

            return direction === 'desc' ? -comparison : comparison;
        });
    }

    async function applyFilters() {
        try {
            logsContainer.innerHTML = '<div class="loading">Loading logs...</div>';
            
            const params = new URLSearchParams();
            
            if (searchInput.value) params.append('query', searchInput.value);
            if (startDateInput.value) params.append('startDate', startDateInput.value);
            if (endDateInput.value) params.append('endDate', endDateInput.value);
            if (minTimeInput.value) params.append('minTime', minTimeInput.value);
            if (maxTimeInput.value) params.append('maxTime', maxTimeInput.value);
            
            params.append('sort', sortSelect.value);
            
            const response = await makeAuthenticatedRequest(`/sorted_loglist_allusers/filter?${params}`);
            
            if (!response.ok) {
                const error = await response.json();
                throw new Error(error.error || 'Failed to fetch filtered logs');
            }
            
            logs = await response.json();
            renderLogs();
            
        } catch (error) {
            console.error('Error applying filters:', error);
            showToast(error.message || 'Failed to apply filters', 'error');
            const filteredLogs = filterLogsLocally(logs);
            renderLogs(filteredLogs);
        }
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

    function resetFilters() {
        searchInput.value = '';
        startDateInput.value = '';
        endDateInput.value = '';
        minTimeInput.value = '';
        maxTimeInput.value = '';
        sortSelect.value = 'date-desc';

        fetchLogs();
    }

    function resetForm() {
        logForm.reset();
        isEditing = false;
        currentLogId = null;
        document.getElementById('formModal').style.display = 'none';
        clearAllErrors();
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
        const errorElement = document.getElementById(`${fieldId}-error`);
        if (errorElement) {
            errorElement.textContent = '';
            errorElement.style.display = 'none';
        }
    
        if (!message) return;
    
        const errorDiv = errorElement || document.createElement('div');
        errorDiv.id = `${fieldId}-error`;
        errorDiv.className = 'error-message';
        errorDiv.textContent = message;
        errorDiv.style.display = 'block';
    
        if (!errorElement) {
            const inputField = document.getElementById(fieldId);
            if (inputField) {
                inputField.parentNode.insertBefore(errorDiv, inputField.nextSibling);
                inputField.classList.add('error-input');
                inputField.focus();
            } else {
                document.getElementById('logForm').appendChild(errorDiv);
            }
        }
    }

    function hideModal(modalId) {
        document.getElementById(modalId).style.display = 'none';
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
            
            modal.style.display = 'block';
            
            const cleanUp = () => {
                confirmBtn.onclick = null;
                cancelBtn.onclick = null;
                modal.style.display = 'none';
            };
            
            const confirmHandler = () => {
                cleanUp();
                resolve(true);
            };
            
            const cancelHandler = () => {
                cleanUp();
                resolve(false);
            };
            
            confirmBtn.onclick = confirmHandler;
            cancelBtn.onclick = cancelHandler;
            
            modal.querySelector('.modal-close').onclick = cancelHandler;
        });
    }

    init();
});