import { useTranslation } from 'react-i18next'
import { useAuth } from '../hooks/useAuth'

interface HeaderProps {
  onMenuToggle: () => void
}

export default function Header({ onMenuToggle }: HeaderProps) {
  const { t, i18n } = useTranslation()
  const { fullName, logout } = useAuth()

  function toggleLanguage() {
    const next = i18n.language === 'en' ? 'ar' : 'en'
    i18n.changeLanguage(next)
    localStorage.setItem('i18nextLng', next)
    document.documentElement.dir = next === 'ar' ? 'rtl' : 'ltr'
    document.documentElement.lang = next
  }

  return (
    <header className="flex items-center justify-between border-b bg-white px-4 py-3">
      <div className="flex items-center gap-3">
        <button
          onClick={onMenuToggle}
          className="rounded p-1 text-gray-600 hover:bg-gray-100 md:hidden"
          aria-label="Toggle menu"
        >
          <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
          </svg>
        </button>
        <span className="text-sm text-gray-500">
          {t('header.greeting')}, {fullName || 'User'}
        </span>
      </div>

      <div className="flex items-center gap-2">
        <button
          onClick={toggleLanguage}
          className="rounded border px-3 py-1 text-sm text-gray-700 hover:bg-gray-50"
        >
          {t('header.languageToggle')}
        </button>

        <button
          onClick={logout}
          className="rounded px-3 py-1 text-sm text-red-600 hover:bg-red-50"
        >
          {t('auth.logout')}
        </button>
      </div>
    </header>
  )
}
