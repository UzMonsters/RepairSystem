async function getModal(id: string) {
  const el = document.getElementById(id)
  if (!el) return null
  const { Modal } = await import('bootstrap')
  return Modal.getOrCreateInstance(el)
}

export async function showModal(id: string) {
  const modal = await getModal(id)
  if (modal) modal.show()
}

export async function hideModal(id: string) {
  const modal = await getModal(id)
  if (!modal) return
  const el = document.getElementById(id)
  if (!el?.classList.contains('show')) {
    modal.hide()
    return
  }
  await new Promise<void>((resolve) => {
    const finish = () => {
      el.removeEventListener('hidden.bs.modal', finish)
      resolve()
    }
    el.addEventListener('hidden.bs.modal', finish, { once: true })
    modal.hide()
    window.setTimeout(finish, 350)
  })
}
