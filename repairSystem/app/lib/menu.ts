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
  roles?: Array<'ADMIN' | 'MANAGER'>
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
  { type: 'header', text: 'main' },
  { type: 'item', text: 'dashboard', href: '/', icon: 'bi-speedometer2' },
  { type: 'item', text: 'notifications', href: '/notifications', icon: 'bi-bell' },
  { type: 'header', text: 'requests' },
  { type: 'item', text: 'requests', href: '/requests', icon: 'bi-clipboard-plus' },
  { type: 'header', text: 'customers' },
  { type: 'item', text: 'customers', href: '/customers', icon: 'bi-people' },
  { type: 'header', text: 'management' },
  { type: 'item', text: 'technicians', href: '/technicians', icon: 'bi-person-wrench' },
  { type: 'item', text: 'categories', href: '/categories', icon: 'bi-tags' },
  { type: 'item', text: 'reviews', href: '/reviews', icon: 'bi-star' },
  { type: 'item', text: 'users', href: '/users', icon: 'bi-person-badge', roles: ['ADMIN'] },
  { type: 'header', text: 'system' },
  { type: 'item', text: 'settings', href: '/settings', icon: 'bi-gear', roles: ['ADMIN'] },
  { type: 'item', text: 'signOut', action: 'signout', icon: 'bi-box-arrow-right', iconColor: 'danger' }
]
