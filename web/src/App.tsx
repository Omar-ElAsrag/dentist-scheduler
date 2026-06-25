import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { I18nextProvider } from 'react-i18next'
import { AuthProvider } from './context/AuthContext'
import i18n from './lib/i18n'
import ProtectedRoute from './components/ProtectedRoute'
import Layout from './components/Layout'
import LoginPage from './pages/LoginPage'
import PatientsPage from './features/patients/PatientsPage'
import PatientDetailPage from './features/patients/PatientDetailPage'
import SchedulePage from './features/schedule/SchedulePage'
import ProceduresPage from './features/procedures/ProceduresPage'
import AnalyticsPage from './features/analytics/AnalyticsPage'
import AssociatesPage from './features/associates/AssociatesPage'
import SettingsPage from './features/settings/SettingsPage'

const queryClient = new QueryClient()

function Dashboard() {
  return <div className="text-gray-600 text-lg">Dashboard — coming soon</div>
}

export default function App() {
  return (
    <I18nextProvider i18n={i18n}>
      <QueryClientProvider client={queryClient}>
        <AuthProvider>
          <BrowserRouter>
            <Routes>
              <Route path="/login" element={<LoginPage />} />
              <Route
                path="/"
                element={
                  <ProtectedRoute>
                    <Layout />
                  </ProtectedRoute>
                }
              >
                <Route index element={<Dashboard />} />
                <Route path="patients">
                  <Route index element={<PatientsPage />} />
                  <Route path=":id" element={<PatientDetailPage />} />
                </Route>
                <Route path="schedule" element={<SchedulePage />} />
                <Route path="procedures" element={<ProceduresPage />} />
                <Route path="analytics" element={<AnalyticsPage />} />
                <Route path="associates" element={<AssociatesPage />} />
                <Route path="settings" element={<SettingsPage />} />
              </Route>
              <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
          </BrowserRouter>
        </AuthProvider>
      </QueryClientProvider>
    </I18nextProvider>
  )
}
