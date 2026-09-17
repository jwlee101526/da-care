import { useState } from 'react'
import { Navbar } from './components/Navbar'
import { HeroSection } from './components/HeroSection'
import { ServiceSection } from './components/ServiceSection'
import { OrderStatusSection } from './components/OrderStatusSection'
import { FaqSection } from './components/FaqSection'
import { ContactSection } from './components/ContactSection'
import { Footer } from './components/Footer'
import { ReservationModal } from './components/ReservationModal'
import { ChatWidget } from './components/ChatWidget'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { LanguageProvider, useLanguage } from './context/LanguageContext'
import type { ReservationSelection } from './types'
import './App.css'

function MainApp() {
  const { t } = useLanguage()
  const [reservation, setReservation] = useState<ReservationSelection | null>(null)
  const [isChatOpen, setIsChatOpen] = useState(false)
  const openReservation = (selection: ReservationSelection = { device: 'laptop', symptom: '' }) => setReservation(selection)
  const openChat = () => setIsChatOpen(true)

  return (
    <>
      <a className="skip-link" href="#main">{t.nav.skip}</a>
      <Navbar onOpenReservation={() => openReservation()} onOpenChat={openChat} />
      <main id="main">
        <HeroSection onOpenReservation={() => openReservation()} onOpenChat={openChat} />
        <ServiceSection onSelectCategory={openReservation} />
        <OrderStatusSection />
        <FaqSection />
        <ContactSection onOpenReservation={openReservation} onOpenChat={openChat} />
        <Footer />
      </main>
      <ChatWidget isOpen={isChatOpen} onToggle={() => setIsChatOpen(value => !value)} onBookWithSymptom={openReservation} />
      {reservation && <ReservationModal initialSelection={reservation} onClose={() => setReservation(null)} />}
    </>
  )
}

export default function App() {
  return (
    <BrowserRouter>
      <LanguageProvider>
        <Routes>
          <Route path="/" element={<MainApp />} />
          <Route path="/en" element={<MainApp />} />
          <Route path="/ko" element={<Navigate to="/" replace />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </LanguageProvider>
    </BrowserRouter>
  )
}
