document.addEventListener('DOMContentLoaded', function() {
    const avatarInput = document.getElementById('avatar');
    const avatarPreview = document.getElementById('avatarPreview');
    const signInBtn = document.querySelector("#sign-in-btn");
    const signUpBtn = document.querySelector("#sign-up-btn");
    const container = document.querySelector(".container");
    const passwordField = document.getElementById('password');
    const confirmPasswordField = document.getElementById('confirmPassword');

    const formInputs = document.querySelectorAll('.sign-up-form .input-field input');
    const form = document.querySelector('.sign-up-form');

    if (avatarInput && avatarPreview) {
        avatarInput.addEventListener('change', function(e) {
            if (this.files && this.files[0]) {
                const file = this.files[0];
                const validTypes = ['image/jpeg', 'image/png'];
                const maxSize = 5 * 1024 * 1024;
                
                document.querySelector('.error-message[th\\:errors="*{avatarFile}"]')?.remove();

                const existingError = avatarInput.parentNode.querySelector('.error-message:not([th\\:errors])');
                if (existingError) {
                    existingError.remove();
                    avatarInput.parentNode.classList.remove('error');
                }
                
                if (!validTypes.includes(file.type.toLowerCase()) || file.name.toLowerCase().endsWith('.jpg')) {
                    displayAvatarError('Invalid file');
                    this.value = '';
                    avatarPreview.style.display = 'none';
                    return;
                }

                if (file.size > maxSize) {
                    displayAvatarError('Oversized file');
                    this.value = '';
                    avatarPreview.style.display = 'none';
                    return;
                }
                
                avatarPreview.src = URL.createObjectURL(file);
                avatarPreview.style.display = 'block';
                avatarInput.parentNode.classList.remove('error');
            } else {
                const existingError = avatarInput.parentNode.querySelector('.error-message:not([th\\:errors])');
                if (existingError) {
                    existingError.remove();
                    avatarInput.parentNode.classList.remove('error');
                }
                avatarPreview.style.display = 'none';
            }
        });
    }

    signUpBtn.addEventListener("click", () => {
        container.classList.add("sign-up-mode");
    });

    signInBtn.addEventListener("click", () => {
        container.classList.remove("sign-up-mode");
    });

    const errorElements = document.querySelectorAll('.error-message, .alert.alert-danger');

    function displayAvatarError(message) {
        const errorContainer = document.createElement('small');
        errorContainer.className = 'error-message';
        errorContainer.textContent = message;
        avatarInput.parentNode.appendChild(errorContainer);
        avatarInput.parentNode.classList.add('error');
    }

    function validateUsername(username) {
        const regex = /^[a-zA-Z0-9_]{4,20}$/;
        if (!username) return 'Username is required';
        if (!regex.test(username)) return '4-20 characters (letters, numbers, _ only)';
        return '';
    }

    function validateEmail(email) {
        const regex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!email) return 'Email is required';
        if (!regex.test(email)) return 'Please enter a valid email address';
        return '';
    }

    function validatePassword(password) {
        const regex = /^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,64}$/;
        if (!password) return 'Password is required';
        if (!regex.test(password)) return 'Must contain 1 number, 1 lower- & 1 uppercase letter, 8-64 chars';
        return '';
    }

    function validateConfirmPassword(password, confirmPassword) {
        if (!confirmPassword) return 'Please confirm your password';
        if (password !== confirmPassword) return 'Passwords do not match';
        return '';
    }

    function displayFieldError(field, message) {
        const parent = field.closest('.input-field');
        if (!parent) return;
        
        let errorElement = parent.querySelector('.inline-error');
        
        if (!errorElement) {
            errorElement = document.createElement('small');
            errorElement.className = 'inline-error';
            parent.appendChild(errorElement);
        }
        
        errorElement.textContent = message;
        parent.classList.add('error');
    }

    function clearFieldError(field) {
        const parent = field.closest('.input-field');
        if (!parent) return;
        
        const errorElement = parent.querySelector('.inline-error');
        if (errorElement) {
            errorElement.remove();
        }
        parent.classList.remove('error');
    }

    function validateField(field) {
        const value = field.value.trim();
        let error = '';
        
        if (field.id === 'username') {
            error = validateUsername(value);
        } else if (field.id === 'email') {
            error = validateEmail(value);
        } else if (field.id === 'password') {
            error = validatePassword(value);
            if (confirmPasswordField && confirmPasswordField.value.trim()) {
                validateField(confirmPasswordField);
            }
        } else if (field.id === 'confirmPassword') {
            error = validateConfirmPassword(passwordField.value.trim(), value);
        }
        
        if (error) {
            displayFieldError(field, error);
            return false;
        } else {
            clearFieldError(field);
            return true;
        }
    }

    formInputs.forEach(input => {
        input.addEventListener('blur', function() {
            validateField(this);
        });
        
        let timeout;
        input.addEventListener('input', function() {
            clearTimeout(timeout);
            timeout = setTimeout(() => {
                validateField(this);
            }, 500);
        });
    });

    if (form) {
        form.addEventListener('submit', function(e) {
            let isValid = true;
            
            formInputs.forEach(input => {
                if (!validateField(input)) {
                    isValid = false;
                }
            });
            
            if (passwordField.value !== confirmPasswordField.value) {
                displayFieldError(confirmPasswordField, 'Passwords do not match');
                isValid = false;
            }
            
            if (!isValid) {
                e.preventDefault();
                const firstInvalid = form.querySelector('.error input');
                if (firstInvalid) {
                    firstInvalid.focus();
                }
            }
        });
    }
});