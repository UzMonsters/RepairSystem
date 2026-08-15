export interface ToastMessage {
  id: number
  type: 'success' | 'error' | 'info'
  message: string
}

let nextId = 1

export function useToast() {
  const toasts = useState<ToastMessage[]>('app:toasts', () => [])

  function addToast(message: string, type: 'success' | 'error' | 'info' = 'info', duration = 5000) {
    const id = nextId++
    toasts.value.push({ id, type, message })
    setTimeout(() => {
      removeToast(id)
    }, duration)
  }

  function removeToast(id: number) {
    const index = toasts.value.findIndex(t => t.id === id)
    if (index !== -1) toasts.value.splice(index, 1)
  }

  function showSuccess(message: string) {
    addToast(message, 'success')
  }

  function showError(message: string) {
    addToast(message, 'error')
  }

  return { toasts, addToast, removeToast, showSuccess, showError }
}
