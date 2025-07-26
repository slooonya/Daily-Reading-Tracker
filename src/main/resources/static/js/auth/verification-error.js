document.addEventListener('DOMContentLoaded', function() {
    document.querySelectorAll("#sign-in-btn, #sign-up-btn").forEach(btn => {
        if (btn) btn.style.display = 'none';
    });
});