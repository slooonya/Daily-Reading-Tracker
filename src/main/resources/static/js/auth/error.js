document.querySelector('.btn.transparent').addEventListener('click', function(e) {
    if (window.history.length > 1) {
        e.preventDefault();
        window.history.back();
    }
});