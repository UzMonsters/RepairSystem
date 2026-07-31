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
  if (modal) modal.hide()
}
