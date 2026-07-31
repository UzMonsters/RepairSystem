export function useLteBehaviors() {
  const isMobile = () => window.innerWidth <= 992

  const toggleSidebar = () => {
    if (isMobile()) {
      document.body.classList.toggle('sidebar-open')
    } else {
      document.body.classList.toggle('sidebar-collapse')
    }
  }

  const onClick = (event: Event) => {
    const target = event.target as HTMLElement | null
    const trigger = target?.closest<HTMLElement>('[data-lte-toggle]')
    if (!trigger) return
    const action = trigger.getAttribute('data-lte-toggle')

    if (action === 'sidebar') {
      event.preventDefault()
      toggleSidebar()
    } else if (action === 'card-collapse') {
      const card = trigger.closest<HTMLElement>('.card')
      if (!card) return
      event.preventDefault()
      card.classList.toggle('collapsed-card')
    } else if (action === 'card-remove') {
      const card = trigger.closest<HTMLElement>('.card')
      if (!card) return
      event.preventDefault()
      card.remove()
    } else if (action === 'chat-pane') {
      const chat = trigger.closest<HTMLElement>('.direct-chat')
      if (!chat) return
      event.preventDefault()
      chat.classList.toggle('direct-chat-contacts-open')
    }
  }

  onMounted(() => {
    if (isMobile()) {
      document.body.classList.add('sidebar-collapse')
    }
    document.addEventListener('click', onClick)
  })

  onBeforeUnmount(() => {
    document.removeEventListener('click', onClick)
  })
}
