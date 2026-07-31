export interface MenuItem {
  type: 'item'
  text: string
  href?: string
  icon?: string
  iconColor?: string
  badge?: number | string
  badgeColor?: string
  target?: string
  action?: 'signout'
}

export interface MenuGroup {
  type: 'group'
  text: string
  icon?: string
  badge?: number | string
  badgeColor?: string
  children: MenuNode[]
}

export interface MenuHeader {
  type: 'header'
  text: string
}

export type MenuNode = MenuItem | MenuGroup | MenuHeader

export const menu: MenuNode[] = [
  { type: 'header', text: 'MAIN' },
  { type: 'item', text: 'Dashboard', href: '/', icon: 'bi-speedometer2' },
  { type: 'header', text: 'REQUESTS' },
  { type: 'item', text: 'Requests', href: '/requests', icon: 'bi-clipboard-plus' },
  { type: 'header', text: 'CUSTOMERS' },
  { type: 'item', text: 'Customers', href: '/customers', icon: 'bi-people' },
  { type: 'header', text: 'MANAGEMENT' },
  { type: 'item', text: 'Technicians', href: '/technicians', icon: 'bi-person-wrench' },
  { type: 'item', text: 'Categories', href: '/categories', icon: 'bi-tags' },
  { type: 'item', text: 'Reviews', href: '/reviews', icon: 'bi-star' },
  { type: 'item', text: 'Users', href: '/users', icon: 'bi-person-badge' },
  { type: 'header', text: 'SYSTEM' },
  { type: 'item', text: 'Settings', href: '/settings', icon: 'bi-gear' },
  { type: 'item', text: 'Sign out', action: 'signout', icon: 'bi-box-arrow-right', iconColor: 'danger' }
]
