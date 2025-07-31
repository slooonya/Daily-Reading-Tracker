document.addEventListener('DOMContentLoaded', () => {
    let progressChart = null;

    initProgressSection();
    setupEventListeners();
    fetchAndDisplayBookProgress();

    function initProgressSection() {
        const today = new Date().toISOString().split('T')[0];
        const oneMonthAgo = new Date();
        oneMonthAgo.setMonth(oneMonthAgo.getMonth() - 1);
        const oneMonthAgoStr = oneMonthAgo.toISOString().split('T')[0];

        document.getElementById('customStartDate').value = oneMonthAgoStr;
        document.getElementById('customEndDate').value = today;

        fetchReadingProgress('total');
        fetchStatistics('total');
    }

    function setupEventListeners() {
        document.getElementById('progressPeriod').addEventListener('change', function() {
            if (this.value === 'custom') {
                document.getElementById('customRangeContainer').style.display = 'flex';
            } else {
                document.getElementById('customRangeContainer').style.display = 'none';
                fetchReadingProgress(this.value);
                fetchStatistics(this.value);
            }
        });

        document.getElementById('applyCustomRange').addEventListener('click', function() {
            const startDate = document.getElementById('customStartDate').value;
            const endDate = document.getElementById('customEndDate').value;

            if (startDate && endDate) {
                const start = new Date(startDate);
                const end = new Date(endDate);

                if (start > end) {
                    alert('Start date cannot be after end date');
                    return;
                }

                fetchReadingProgress('custom', startDate, endDate);
                fetchCustomRangeStatistics(startDate, endDate);
            }
        });
    }

    function fetchReadingProgress(period, startDate, endDate) {
        let url = `/api/reading-statistics/by-period?period=${encodeURIComponent(period)}`;
        if (period === 'custom') {
            url = `/api/reading-statistics/by-date-range?startDate=${startDate}&endDate=${endDate}`;
        }

        makeAuthenticatedRequest(url)
            .then(handleResponse)
            .then(data => {
                renderProgressChart(data);
            })
            .catch(error => {
                console.error('Error fetching progress data:', error);
                showChartError();
            });
    }

    function fetchStatistics(period) {
        const url = `/api/reading-statistics/by-period?period=${encodeURIComponent(period)}`;

        makeAuthenticatedRequest(url)
            .then(handleResponse)
            .then(updateStatsCards)
            .catch(error => {
                console.error('Error fetching statistics:', error);
            });
    }

    function fetchCustomRangeStatistics(startDate, endDate) {
        const url = `/api/reading-logs/reading-statistics?startDate=${encodeURIComponent(startDate)}&endDate=${encodeURIComponent(endDate)}`;

        makeAuthenticatedRequest(url)
            .then(handleResponse)
            .then(updateStatsCards)
            .catch(error => {
                console.error('Error fetching custom range statistics:', error);
            });
    }

    function updateStatsCards(data) {
        document.getElementById('statBookCount').textContent = data.bookCount;
        document.getElementById('statTotalTime').textContent = `${data.totalReadingTime} mins`;
        document.getElementById('statAvgTime').textContent = `${data.avgDailyTime.toFixed(1)} mins`;
    }

    function renderProgressChart(data) {
        const ctx = document.getElementById('progressChart').getContext('2d');

        if (progressChart) {
            progressChart.destroy();
        }

        progressChart = new Chart(ctx, {
            type: 'bar',
            data: {
                labels: data.dates,
                datasets: [{
                    label: 'Reading Time (minutes)',
                    data: data.readingTimes,
                    backgroundColor: 'rgba(114, 47, 55, 0.5)',
                    borderColor: 'rgba(114, 47, 55, 1)',
                    borderWidth: 1
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                scales: {
                    y: {
                        beginAtZero: true,
                        title: {
                            display: true,
                            text: 'Minutes'
                        }
                    },
                    x: {
                        title: {
                            display: true,
                            text: 'Date'
                        }
                    }
                }
            }
        });
    }
    
function fetchAndDisplayBookProgress() {
    fetch('/book-progress')
        .then(response => response.json())
        .then(data => {
            const container = document.getElementById('booksProgressContainer');
            container.innerHTML = '';

            for (const [bookTitle, progress] of Object.entries(data.bookProgress)) {
                const progressItem = document.createElement('div');
                progressItem.className = 'book-progress-item';

                progressItem.innerHTML = `
                    <div class="book-title">${bookTitle}</div>
                    <div class="progress-bar-container">
                        <div class="progress-bar" style="width: ${Math.min(progress, 100)}%">
                            ${Math.round(progress)}%
                        </div>
                    </div>
                `;

                container.appendChild(progressItem);
            }
        })
        .catch(error => {
            console.error('Failed to get book progress:', error);
        });
}
    function showChartError() {
        document.querySelector('.chart-container').innerHTML =
            `<div class="chart-error">Failed to load chart data</div>`;
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
                    window.location.href = '/login';
                    return Promise.reject('Unauthorized');
                }
                return response;
            });
    }

    function handleResponse(response) {
        if (!response.ok) {
            return response.json().then(error => {
                throw new Error(error.error || 'Request failed');
            });
        }
        return response.json();
    }
});