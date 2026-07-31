export interface MenuItem {
  type: 'item'
  text: string
  href?: string
  icon?: string
  iconColor?: string
  badge?: number | string
  badgeColor?: string
  target?: string
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
  {
    type: 'group',
    text: 'Dashboard',
    icon: 'bi-speedometer',
    children: [
      { type: 'item', text: 'Dashboard', href: '/', icon: 'bi-circle' }
    ]
  },
  { type: 'header', text: 'MAIN' },
  {
    type: 'group',
    text: 'Repairs',
    icon: 'bi-tools',
    badge: 7,
    badgeColor: 'danger',
    children: [
      { type: 'item', text: 'All Repairs', href: '/repairs', icon: 'bi-circle' },
      { type: 'item', text: 'New Repair', href: '/repairs/new', icon: 'bi-circle' },
      { type: 'item', text: 'Statuses', href: '/repairs/statuses', icon: 'bi-circle' }
    ]
  },
  {
    type: 'group',
    text: 'Clients',
    icon: 'bi-people',
    children: [
      { type: 'item', text: 'Client List', href: '/clients', icon: 'bi-circle' },
      { type: 'item', text: 'Add Client', href: '/clients/new', icon: 'bi-circle' }
    ]
  },
  {
    type: 'group',
    text: 'Devices',
    icon: 'bi-device-ssd',
    children: [
      { type: 'item', text: 'Devices', href: '/devices', icon: 'bi-circle' },
      { type: 'item', text: 'Device Types', href: '/devices/types', icon: 'bi-circle' }
    ]
  },
  {
    type: 'group',
    text: 'Inventory',
    icon: 'bi-boxes',
    children: [
      { type: 'item', text: 'Parts', href: '/inventory/parts', icon: 'bi-circle' },
      { type: 'item', text: 'Suppliers', href: '/inventory/suppliers', icon: 'bi-circle' }
    ]
  },
  {
    type: 'group',
    text: 'Reports',
    icon: 'bi-graph-up',
    children: [
      { type: 'item', text: 'Finance', href: '/reports/finance', icon: 'bi-circle' },
      { type: 'item', text: 'Workload', href: '/reports/workload', icon: 'bi-circle' }
    ]
  },
  { type: 'header', text: 'SYSTEM' },
  {
    type: 'group',
    text: 'Settings',
    icon: 'bi-gear',
    children: [
      { type: 'item', text: 'General', href: '/settings', icon: 'bi-circle' },
      { type: 'item', text: 'Users', href: '/settings/users', icon: 'bi-circle' }
    ]
  },
  { type: 'item', text: 'Sign out', href: '#', icon: 'bi-box-arrow-right', iconColor: 'danger' }
]
