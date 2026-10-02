import { lazy, Suspense, useEffect, useState } from 'react'
import { Navbar } from './components/Navbar'
import { BrandHeroSection } from './components/BrandHeroSection'
import { HeroSection } from './components/HeroSection'
import { ServiceSection } from './components/ServiceSection'
import { OrderStatusSection } from './components/OrderStatusSection'
import { FaqSection } from './components/FaqSection'
import { ContactSection } from './components/ContactSection'
import { Footer } from './components/Footer'
import { ChatWidget } from './components/ChatWidget'
import { BrowserRouter, Navigate, Route, Routes, useLocation, useNavigate } from 'react-router-dom'
import { LanguageProvider, useLanguage } from './context/LanguageContext'
import type { ReservationSelection } from './types'
import './App.css'
import { AuthProvider, useAuth } from './context/AuthContext'
import { Toaster } from './components/ui/sonner'

// 첫 화면(랜딩)을 뺀 페이지는 방문할 때 불러온다.
const AuthPage = lazy(() => import('./components/AuthPage').then(module => ({ default: module.AuthPage })))
const ReservationsPage = lazy(() => import('./components/ReservationsPage').then(module => ({ default: module.ReservationsPage })))
const AdminPage = lazy(() => import('./components/AdminPage').then(module => ({ default: module.AdminPage })))
const ReservationPage = lazy(() => import('./components/ReservationPage').then(module => ({ default: module.ReservationPage })))
const GuestLookupPage = lazy(() => import('./components/GuestLookupPage').then(module => ({ default: module.GuestLookupPage })))

function LandingContent({
  onOpenReservation,
  onOpenChat,
}: {
  onOpenReservation: (selection?: ReservationSelection) => void
  onOpenChat: () => void
}) {
  return (
    <main id="main">
      <BrandHeroSection />
      <HeroSection />
      <ServiceSection onSelectCategory={onOpenReservation} />
      <OrderStatusSection />
      <FaqSection />
      <ContactSection onOpenReservation={onOpenReservation} onOpenChat={onOpenChat} />
      <Footer />
    </main>
  )
}

function AppShell() {
  const { lang, t } = useLanguage()
  const location = useLocation()
  const navigate = useNavigate()
  const [isChatOpen, setIsChatOpen] = useState(false)
  const [reservationRevision] = useState(0)
  const openReservation = (selection: ReservationSelection = { device: 'computer', symptom: '' }) => {
    const basePath = lang === 'en' ? '/en/reserve' : '/reserve'
    navigate(basePath, { state: selection })
  }
  const openChat = () => setIsChatOpen(true)

  useEffect(() => {
    if (location.hash) {
      const id = location.hash.replace('#', '')
      const el = document.getElementById(id)
      if (el) {
        setTimeout(() => {
          el.scrollIntoView({ behavior: 'smooth' })
        }, 80)
      }
    }
  }, [location.pathname, location.hash])

  return (
    <>
      <a className="skip-link" href="#main">{t.nav.skip}</a>
      <Navbar onOpenReservation={() => openReservation()} onOpenChat={openChat} />
      <Suspense fallback={<div className="page-loading" aria-busy="true" />}>
        <Routes>
          <Route path="/" element={<LandingContent onOpenReservation={openReservation} onOpenChat={openChat} />} />
          <Route path="/en" element={<LandingContent onOpenReservation={openReservation} onOpenChat={openChat} />} />
          <Route path="/reserve" element={<ReservationPage />} />
          <Route path="/reservations/new" element={<Navigate to="/reserve" replace />} />
          <Route path="/en/reserve" element={<ReservationPage />} />
          <Route path="/en/reservations/new" element={<Navigate to="/en/reserve" replace />} />
          <Route path="/reservations" element={<CustomerOnly><ReservationsPage key={reservationRevision} onOpenReservation={() => openReservation()} /></CustomerOnly>} />
          <Route path="/reservations/lookup" element={<GuestLookupPage />} />
          <Route path="/en/reservations/lookup" element={<GuestLookupPage />} />
          <Route path="/order" element={<Navigate to="/reservations" replace />} />
          <Route path="/en/reservations" element={<CustomerOnly><ReservationsPage key={reservationRevision} onOpenReservation={() => openReservation()} /></CustomerOnly>} />
          <Route path="/login" element={<AuthPage />} />
          <Route path="/signup" element={<AuthPage signup />} />
          <Route path="/en/login" element={<AuthPage />} />
          <Route path="/en/signup" element={<AuthPage signup />} />
          <Route path="/admin" element={<AdminOnly><AdminPage /></AdminOnly>} />
          <Route path="/ko" element={<Navigate to="/" replace />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </Suspense>
      <ChatWidget isOpen={isChatOpen} onToggle={() => setIsChatOpen(value => !value)} onBookWithSymptom={openReservation} />
      <Toaster position="top-center" richColors />
    </>
  )
}

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <LanguageProvider>
          <AppShell />
        </LanguageProvider>
      </AuthProvider>
    </BrowserRouter>
  )
}

function AdminOnly({ children }: { children: React.ReactNode }) {
  const { token, role } = useAuth()
  if (!token) return <Navigate to="/login" replace state={{ from: '/admin' }} />
  return role === 'ADMIN' ? children : <Navigate to="/" replace />
}

function CustomerOnly({ children }: { children: React.ReactNode }) {
  const { token } = useAuth()
  return token ? <>{children}</> : <Navigate to="/login" replace state={{ from: '/reservations' }} />
}
