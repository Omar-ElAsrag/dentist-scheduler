import { NavLink } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../hooks/useAuth'

const navItems = [
  { labelKey: 'nav.patients', path: '/patients', roles: ['admin', 'dentist', 'receptionist'] as const },
  { labelKey: 'nav.schedule', path: '/schedule', roles: ['admin', 'dentist', 'receptionist'] as const },
  { labelKey: 'nav.procedures', path: '/procedures', roles: ['admin', 'dentist'] as const },
  { labelKey: 'nav.analytics', path: '/analytics', roles: ['admin'] as const },
  { labelKey: 'nav.associates', path: '/associates', roles: ['admin'] as const },
  { labelKey: 'nav.settings', path: '/settings', roles: ['admin'] as const },
]

interface SidebarProps {
  open: boolean
  onClose: () => void
}

export default function Sidebar({ open, onClose }: SidebarProps) {
  const { t } = useTranslation()
  const { role } = useAuth()

  const visibleItems = navItems.filter((item) => role && item.roles.includes(role))

  return (
    <>
      {open && (
        <div
          className="fixed inset-0 z-30 bg-black/50 md:hidden"
          onClick={onClose}
        />
      )}

      <aside
        className={`fixed top-0 start-0 z-40 h-full w-64 transform border-e bg-white p-4 transition-transform duration-200 md:static md:translate-x-0 ${
          open ? 'translate-x-0' : '-translate-x-full'
        }`}
      >
        <nav className="flex flex-col gap-1">
          {visibleItems.map((item) => (
            <NavLink
              key={item.path}
              to={item.path}
              end={item.path === '/patients'}
              onClick={onClose}
              className={({ isActive }) =>
                `rounded px-3 py-2 text-sm transition-colors ${
                  isActive
                    ? 'bg-blue-100 text-blue-700 font-medium'
                    : 'text-gray-700 hover:bg-gray-100'
                }`
              }
            >
              {t(item.labelKey)}
            </NavLink>
          ))}
        </nav>
      </aside>
    </>
  )
}
