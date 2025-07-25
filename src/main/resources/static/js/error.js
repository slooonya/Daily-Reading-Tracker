document.addEventListener('DOMContentLoaded', function() {
    if (window.history.length <= 1) {
        document.querySelector('.error-btn.secondary').disabled = true;
        document.querySelector('.error-btn.secondary').style.opacity = '0.6';
        document.querySelector('.error-btn.secondary').cursor = 'not-allowed';
    }
});