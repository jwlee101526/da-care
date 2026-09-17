import { MessageSquare, ClipboardList, CalendarDays, Wrench } from 'lucide-react'
import { Reveal } from './Reveal'
import { useLanguage } from '../context/LanguageContext'

const stepIcons = [MessageSquare, ClipboardList, CalendarDays, Wrench]

export function OrderStatusSection() {
  const { t } = useLanguage()

  return (
    <section id="status" className="section process-section">
      <Reveal className="container">
        <div className="section-heading">
          <div>
            <span className="eyebrow">{t.process.eyebrow}</span>
            <h2>{t.process.title1}<br />{t.process.title2}</h2>
          </div>
          <p style={{ whiteSpace: 'pre-line' }}>{t.process.desc}</p>
        </div>
        <ol className="process-grid">
          {t.process.steps.map(({ title, text }, index) => {
            const Icon = stepIcons[index] || Wrench
            return (
              <li key={title}>
                <div className="process-top">
                  <span>0{index + 1}</span>
                  <Icon size={24} strokeWidth={1.5} />
                </div>
                <h3>{title}</h3>
                <p>{text}</p>
              </li>
            )
          })}
        </ol>
        <p className="section-note">{t.process.note}</p>
      </Reveal>
    </section>
  )
}
