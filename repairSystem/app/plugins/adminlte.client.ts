import { Dropdown } from 'bootstrap'

// Position every dropdown relative to the viewport instead of the nearest
// positioned ancestor, so menus inside overflow-hidden/clipped containers
// (e.g. table-responsive tables) render fully outside the table.
Dropdown.Default.popperConfig = () => ({ strategy: 'fixed' })

export default defineNuxtPlugin(() => {})
