document.addEventListener('DOMContentLoaded', () => {
    if (!document.querySelector('meta[name="_csrf"]')) {
        window.location.href = '/auth';
        return;
    }

    const logsContainer = document.getElementById('logs');
    const logForm = document.getElementById('logForm');
    const cancelBtn = document.getElementById('cancelBtn');;
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

        filterToggle.addEventListener('click', () => {
            filterControls.classList.toggle('expanded');
            filterToggle.classList.toggle('active');
        });

        document.querySelector("#detailModal .modal-close").addEventListener("click", () => {
            document.getElementById("detailModal").style.display = "none";
        });

        document.addEventListener('click', (e) => {
            if (e.target.classList.contains('modal')) {
                e.target.style.display = 'none';
            }
        });

        document.getElementById("cancelBtn").addEventListener("click", () => {
            document.getElementById("formModal").style.display = "none";
            resetForm(); 
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

    function fetchLogs() {
        logsContainer.innerHTML = '<div class="loading">Loading logs...</div>';

        makeAuthenticatedRequest('/getviologs')  
            .then(handleResponse)
            .then(data => {
                logs = data;
                renderLogs();
            })
            .catch(error => {
                console.error('Error fetching logs:', error);
                logsContainer.innerHTML = `<div class="empty-state">Error loading logs: ${error}</div>`;
            });
    }

    function renderLogs() {
        if (logs.length === 0) {
            logsContainer.innerHTML = '<div class="empty-state">No violation logs found.</div>';
            return;
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
    
            const response = await makeAuthenticatedRequest(`/getviologs/filter?${params}`);
            
            if (!response.ok) {
                throw new Error('Failed to fetch filtered logs');
            }
            
            logs = await response.json();
            renderLogs();
            
        } catch (error) {
            console.error('Error applying filters:', error);
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
            
            const logDate = new Date(log.date || log.createdAt);
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

    function createLogElement(log) {
        const logElement = document.createElement('div');
        logElement.className = 'log-entry';

        const progress = log.totalPages ? Math.round((log.currentPage / log.totalPages) * 100) : 0;

        logElement.innerHTML = `
            <div class="log-card">
                <div class="log-header">
                    <h3 class="log-title">${log.title}</h3>
                    <p class="log-author">by ${log.author}</p>
                    <span class="violation-badge">
                    <i class="fas fa-exclamation-triangle"></i> Violation
                    </span>
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
                    <span>${log.username || 'Unknown User'}</span>
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
                    <button class="restore-btn" data-id="${log.id}">
                    <i class="fas fa-undo"></i> Restore
                    </button>
                </div>
                </div>
            `;

        logElement.querySelector('.view-btn').addEventListener('click', () => viewDetails(log.id));
        logElement.querySelector('.restore-btn').addEventListener('click', () => deleteLog(log.id));
        logElement.querySelector('.edit-btn').addEventListener('click', () => updateLog(log.id));

        return logElement;
    }

    function viewDetails(logId) {
        makeAuthenticatedRequest(`/getviologs/${logId}`)
            .then(handleResponse)
            .then(log => {
                document.getElementById('detailId').textContent = log.id;
                document.getElementById('detailTitleText').textContent = log.title;
                document.getElementById('detailAuthor').textContent = log.author;
                document.getElementById('detailDate').textContent = new Date(log.createdAt).toLocaleDateString();
                document.getElementById('detailTime').textContent = log.timeSpent;
                document.getElementById('detailNotes').textContent = log.notes || 'None';
                document.getElementById('detailUser').textContent = log.username || 'Unknown';
                
                document.getElementById('detailModal').style.display = 'flex';
            })
            .catch(error => {
                alert('Failed to get details: ' + error.message);
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

    function createLog(logData) {
        makeAuthenticatedRequest('/getviologs', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(logData)
        })
            .then(handleResponse)
            .then(() => {
                resetForm();
                fetchLogs();
            })
            .catch(error => {
                console.error('Error creating log:', error);
                alert(`Error: ${error.message}`);
            });
    }

    function updateLog(logId, logData) {
        if (!logData) {
            makeAuthenticatedRequest(`/getviologs/${logId}`)
                .then(handleResponse)
                .then(log => {
                    document.getElementById('logId').value = log.id;
                    document.getElementById('title').value = log.title;
                    document.getElementById('author').value = log.author;
                    document.getElementById('date').value = log.date ? log.date.split('T')[0] : '';
                    document.getElementById('timeSpent').value = log.timeSpent;
                    document.getElementById('currentPage').value = log.currentPage;
                    document.getElementById('totalPages').value = log.totalPages;
                    document.getElementById('notes').value = log.notes || '';
                    
                    isEditing = true;
                    currentLogId = logId;
                    
                    document.getElementById('formModal').style.display = 'flex';
                })
                .catch(error => {
                    console.error('Error fetching log:', error);
                    alert('Failed to load log: ' + error.message);
                });
        } else {
            makeAuthenticatedRequest(`/getviologs/${logId}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(logData)
            })
            .then(handleResponse)
            .then(() => {
                resetForm();
                fetchLogs();
                showToast('Violation log updated successfully', 'success');
                document.getElementById('formModal').style.display = 'none';
            })
            .catch(error => {
                console.error('Error updating log:', error);
                alert(`Error: ${error.message}`);
            });
        }
    }
    
    function handleFormSubmit(e) {
        e.preventDefault();
        const logData = {
            title: document.getElementById('title').value,
            author: document.getElementById('author').value,
            date: document.getElementById('date').value,
            timeSpent: parseInt(document.getElementById('timeSpent').value),
            currentPage: parseInt(document.getElementById('currentPage').value),
            totalPages: parseInt(document.getElementById('totalPages').value),
            notes: document.getElementById('notes').value
        };
    
        if (isEditing) {
            updateLog(currentLogId, logData);
        } else {
            createLog(logData);
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
          
          modal.style.display = 'block';
          
          const cleanUp = () => {
            confirmBtn.removeEventListener('click', confirmHandler);
            cancelBtn.removeEventListener('click', cancelHandler);
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
          
          confirmBtn.addEventListener('click', confirmHandler);
          cancelBtn.addEventListener('click', cancelHandler);
          
          modal.querySelector('.modal-close').addEventListener('click', cancelHandler);
        });
      }

      
      async function deleteLog(logId) {
        const confirmed = await showConfirmation({
          title: 'Restore Reading Log',
          message: 'Are you sure you want to restore this reading log?',
          confirmText: 'Restore Log',
          danger: false
        });
        
        if (!confirmed) return;
      
        try {
          await makeAuthenticatedRequest(`/getviologs/${logId}`, {
            method: 'DELETE'
          });
          await fetchLogs();
          showToast('Reading log restored successfully', 'success');
        } catch (error) {
          console.error('Error restoring log:', error);
          showToast(`Error: ${error.message}`, 'error');
        }
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

    function resetForm() {
        logForm.reset();
        isEditing = false;
        currentLogId = null;
    }

    function handleResponse(response) {
        if (!response.ok) {
            return response.json().then(error => {
                throw new Error(error.error || 'Unknown error occurred');
            });
        }
        return response.json();
    }

    const formModal = document.getElementById("formModal");

    function handleFormSubmit(e) {
        e.preventDefault();

        const dateInput = document.getElementById('date').value;
        const selectedDate = new Date(dateInput);
        const today = new Date();

        selectedDate.setHours(0, 0, 0, 0);
        today.setHours(0, 0, 0, 0); 
        
        if (selectedDate > today) {
            console.log("345254");
            showError('date-error', 'Please select a date that is not in the future');
            return;
        }

        const currentPage = parseInt(document.getElementById('currentPage').value);
        const totalPages = parseInt(document.getElementById('totalPages').value);

        if (currentPage > totalPages) {
            showError('currentPage', 'Current page cannot be greater than total pages');
            return;
        }

        const logData = {
            title: document.getElementById('title').value.trim(),
            author: document.getElementById('author').value.trim(),
            date: document.getElementById('date').value,
            timeSpent: parseInt(document.getElementById('timeSpent').value),
            currentPage: parseInt(document.getElementById('currentPage').value),
            totalPages: parseInt(document.getElementById('totalPages').value),
            notes: document.getElementById('notes').value
        };

        if (isEditing) {
            updateLog(currentLogId, logData);
        } else {
            createLog(logData);
        }

        formModal.style.display = "none"; 
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

    init();
});