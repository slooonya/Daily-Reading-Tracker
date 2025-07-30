document.addEventListener('DOMContentLoaded', function() {
  document.querySelectorAll('#flashMessages .toast').forEach(toast => {
    const type = toast.classList.contains('success') ? 'success' : 'error';
    showToast(toast.textContent, type);
    });

    document.querySelectorAll('button[formaction*="users_freeze"], button[formaction*="users_unfreeze"]').forEach(button => {
        button.addEventListener('click', async function(e) {
          e.preventDefault();
          const form = this.form;
          const selectedUsers = Array.from(form.querySelectorAll('input[name="selectedUsers"]:checked')).map(el => el.value);
          
          if (selectedUsers.length === 0) {
            showToast('Please select at least one user', 'error');
            return;
          }
          
          const actionType = this.formAction.includes('users_unfreeze') ? 'unfreeze' : 'freeze';
          const confirmed = await showConfirmation({
            title: `Confirm ${actionType}`,
            message: `Are you sure you want to ${actionType} ${selectedUsers.length} user(s)?`,
            confirmText: `Yes, ${actionType}`,
            danger: true
          });
          
          if (confirmed) {
            form.action = this.formAction;
            form.submit();
          }
        });
      });
  
    document.querySelectorAll('button[formaction*="promote"], button[formaction*="demote"]').forEach(button => {
      button.addEventListener('click', async function(e) {
        e.preventDefault();
        const form = this.form;
        const selectedUsers = Array.from(form.querySelectorAll('input[name="selectedUsers"]:checked')).map(el => el.value);
        
        if (selectedUsers.length === 0) {
          showToast('Please select at least one user', 'error');
          return;
        }
        
        const action = this.formAction.includes('promote') ? 'promote to admin' : 'demote to user';
        const confirmed = await showConfirmation({
          title: `Confirm ${action}`,
          message: `Are you sure you want to ${action} ${selectedUsers.length} user(s)?`,
          confirmText: `Yes, ${action}`,
          danger: true
        });
        
        if (confirmed) {
          form.action = this.formAction;
          form.submit();
        }
      });
    });
  });

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