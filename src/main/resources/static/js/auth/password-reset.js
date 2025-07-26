document.addEventListener('DOMContentLoaded', function() {
    const passwordField = document.getElementById('password');
    const confirmPasswordField = document.getElementById('confirmPasswordField');
    const form = document.querySelector('form');

    if (passwordField) {
        passwordField.addEventListener('input', function() {
            const value = this.value;
            document.getElementById('req-length').classList.toggle('valid', value.length >= 8 && value.length <= 64);
            document.getElementById('req-uppercase').classList.toggle('valid', /[A-Z]/.test(value));
            document.getElementById('req-lowercase').classList.toggle('valid', /[a-z]/.test(value));
            document.getElementById('req-number').classList.toggle('valid', /0-9/.test(value));
        });
    }

    if (passwordField && confirmPasswordField) {
        confirmPasswordField.addEventListener('input', validatePasswordMatch);
        passwordField.addEventListener('input', function() {
            if (confirmPasswordField.value)
                validatePasswordMatch();
        });
    }

    if (form) {
        form.addEventListener('submit', function() {
            if (!validatePasswordMatch()) {
                e.preventDefault();
                confirmPasswordField.focus();
            }
        });
    }

    function validatePasswordMatch() {
        const password = passwordField.value;
        const confirmPassword = confirmPasswordField.value;

        if (confirmPassword && password !== confirmPassword) {
            showPasswordMatchError('Passwords do not match');
            return false;
        } else {
            clearPasswordMatchError();
            return true;
        }
    }

    function showPasswordMatchError(message) {
        clearPasswordMatchError();

        const errorElement = document.createElement('small');
        errorElement.className = 'inline-error password-match-error';
        errorElement.textContent = message;

        confirmPasswordField.parentNode.appendChild(errorElement);
        confirmPasswordField.parentNode.classList.add('error');
    }

    function clearPasswordMatchError() {
        const existingError = confirmPasswordField.parentNode.querySelector('.password-match-error');

        if (existingError) {
            existingError.remove();
            confirmPasswordField.parentNode.classList.remove('remove');
        }
    }
});