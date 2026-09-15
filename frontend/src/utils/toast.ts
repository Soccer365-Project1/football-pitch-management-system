import Swal, { type SweetAlertIcon } from 'sweetalert2';

// Toast configuration (small notification at the top-end)
const Toast = Swal.mixin({
  toast: true,
  position: 'top-end',
  showConfirmButton: false,
  timer: 3000,
  timerProgressBar: true,
  didOpen: (toast) => {
    toast.addEventListener('mouseenter', Swal.stopTimer);
    toast.addEventListener('mouseleave', Swal.resumeTimer);
  }
});

/**
 * Show a toast notification
 */
export const showToast = (title: string, icon: SweetAlertIcon = 'success') => {
  Toast.fire({
    icon,
    title
  });
};

/**
 * Show a modal alert
 */
export const showAlert = (title: string, text: string, icon: SweetAlertIcon = 'info') => {
  Swal.fire({
    title,
    text,
    icon,
    confirmButtonColor: '#10b981', // green-500
    confirmButtonText: 'Đóng'
  });
};

/**
 * Show a confirmation modal
 * Returns a Promise that resolves to true if confirmed
 */
export const showConfirm = async (
  title: string,
  text: string,
  confirmText: string = 'Đồng ý'
): Promise<boolean> => {
  const result = await Swal.fire({
    title,
    text,
    icon: 'warning',
    showCancelButton: true,
    confirmButtonColor: '#ef4444', // red-500
    cancelButtonColor: '#6b7280', // gray-500
    confirmButtonText: confirmText,
    cancelButtonText: 'Hủy'
  });
  
  return result.isConfirmed;
};
