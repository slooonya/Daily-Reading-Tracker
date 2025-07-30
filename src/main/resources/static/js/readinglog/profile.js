const userId = document.getElementById('userId').value;

function showError(message) {
    const errorElement = document.getElementById('errorMessage');
    if (errorElement) {
        errorElement.textContent = message;
    }
}

function uploadAvatar(file) {
    const formData = new FormData();
    formData.append("avatar", file);
            
    document.getElementById('loading').style.display = 'block';
            
    fetch(`/user-profile/${userId}/avatar`, {
        method: 'PUT',
        headers: {
            'X-XSRF-TOKEN': document.querySelector('meta[name="_csrf"]').content
        },
        body: formData
    })
    .then(response => {
        if (!response.ok) {
            return response.text().then(text => { throw new Error(text) });
        }
        return response.json();
    })
    .then(data => {
        const imgHost = document.getElementById('imgHost').value;
        document.getElementById('avatar').src = imgHost + data.filename;
        showToast('Avatar updated successfully!', 'success');
    })
    .catch(error => {
        console.error('Upload failed:', error);
        showToast(error.message || 'Avatar upload failed', 'error');
    })
    .finally(() => {
        document.getElementById('loading').style.display = 'none';
    });
}

document.getElementById('editAvatarFile').addEventListener('change', function (e) {
    const file = e.target.files[0];
    if (file) {
        if (!file.type.match('image/jpeg') && !file.type.match('image/png')) {
            showToast('Only JPEG or PNG images are allowed', 'error');
            return;
        }
        uploadAvatar(file);
    }
});

document.getElementById('editForm').addEventListener('submit', async function (event) {
    event.preventDefault();
    showError('');

    const username = document.getElementById('editUsername').value.trim();
    const signature = document.getElementById('editSignature').value.trim();

    if (username.length < 4 || username.length > 20) {
        showToast('Username must be 4-20 characters', 'error');
        return;
    }

    try {
        const response = await fetch('/user-profile/update', {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'X-XSRF-TOKEN': document.querySelector('meta[name="_csrf"]').content
            },
            body: JSON.stringify({
                username: username,
                signature: signature
            })
        });
        
        if (!response.ok) {
            const error = await response.text();
            if (response.status === 400 && error.includes("Username already taken")) {
                document.getElementById('editUsername').classList.add('is-invalid');
                document.getElementById('usernameError').textContent = "Username already taken";
                document.getElementById('usernameError').style.display = 'block';
            }
            throw new Error(error);
        }
        
        const data = await response.json();
        document.getElementById('username').textContent = data.username;
        document.getElementById('editUsername').value = data.username;
        document.getElementById('editUsername').classList.remove('is-invalid');
        document.getElementById('usernameError').style.display = 'none';
        document.getElementById('signature').textContent = data.signature || 'No signature';
        document.getElementById('editSignature').value = data.signature || '';
        
        showToast('Profile updated successfully!', 'success');
    } catch (error) {
        console.error('Update failed:', error);
        if (!error.message.includes("Username already taken")) {
            showToast(error.message || 'Failed to update profile', 'error');
        }
    }
});

document.getElementById('newPassword').addEventListener('blur', function() {
    const errorElement = document.getElementById('newPasswordError');
    if (this.value.length === 0) {
        errorElement.style.display = 'none';
        return;
    } 
    
    const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,64}$/;
    if (!passwordRegex.test(this.value)) {
        errorElement.style.display = 'block';
    } else {
        errorElement.style.display = 'none';
    }
});

document.getElementById('newPassword').addEventListener('input', function() {
    const errorElement = document.getElementById('newPasswordError');
    if (this.value.length > 0) {
        const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,64}$/;
        if (!passwordRegex.test(this.value)) {
            errorElement.style.display = 'block';
            this.classList.add('invalid');
        } else {
            errorElement.style.display = 'none';
            this.classList.remove('invalid');
        }
    } else {
        errorElement.style.display = 'none';
        this.classList.remove('invalid');
    }
});

document.getElementById('changePasswordForm').addEventListener('submit', async function(event) {
    event.preventDefault();
    showError('');

    const currentPassword = document.getElementById('currentPassword').value;
    const newPassword = document.getElementById('newPassword').value;
    const confirmPassword = document.getElementById('confirmPassword').value;

    if (newPassword === '') {
        showToast('Please enter a new password', 'error');
        return;
    }

    if (newPassword !== confirmPassword) {
        showToast('New passwords do not match', 'error');
        return;
    }

    const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,64}$/;
    if (!passwordRegex.test(newPassword)) {
        showToast('Password must be 8-64 chars with 1 uppercase, 1 lowercase, and 1 number', 'error');
        return;
    }


    try {
        const response = await fetch('/user-profile/change-password', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-XSRF-TOKEN': document.querySelector('meta[name="_csrf"]').content
            },
            body: JSON.stringify({
                currentPassword: currentPassword,
                newPassword: newPassword,
                confirmPassword: confirmPassword
            })
        });
        
        if (!response.ok) {
            const error = await response.text();
            throw new Error(error);
        }
        
        showToast('Password changed successfully! Check your email for confirmation.', 'success');
        this.reset();
    } catch (error) {
        console.error('Password change failed:', error);
        showToast(error.message || 'Failed to change password', 'error');
    }
});

function showToast(message, type = 'info', duration = 3000) {
    const container = document.getElementById('toastContainer');
    if (!container) {
        console.error('Toast container not found');
        return;
    }
    
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

function initializeFormFields() {
    const username = document.getElementById('username').textContent;
    const signature = document.getElementById('signature').textContent;
    
    document.getElementById('editUsername').value = username;
    document.getElementById('editSignature').value = signature === 'No signature' ? '' : signature;
}

document.querySelectorAll('input').forEach(input => {
    input.addEventListener('blur', function() {
        this.classList.add('touched');
    });
});

document.addEventListener('DOMContentLoaded', initializeFormFields);