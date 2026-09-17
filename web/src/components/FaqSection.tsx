import { Plus, Minus } from 'lucide-react'
import { Reveal } from './Reveal'
import { useLanguage } from '../context/LanguageContext'

export function FaqSection() {
  const { t } = useLanguage()

  return (
    <section id="faq" className="section faq-section">
      <Reveal className="container faq-layout">
        <div>
          <span className="eyebrow">{t.faq.eyebrow}</span>
          <h2>{t.faq.title}</h2>
          <p className="muted faq-intro">{t.faq.intro}</p>
        </div>
        <div className="faq-list">
          {t.faq.items.map(({ question, answer }) => (
            <details key={question}>
              <summary>
                <span>{question}</span>
                <span className="faq-toggle">
                  <Plus size={20} className="faq-plus" aria-hidden="true" />
                  <Minus size={20} className="faq-minus" aria-hidden="true" />
                </span>
              </summary>
              <p>{answer}</p>
            </details>
          ))}
        </div>
      </Reveal>
    </section>
  )
}
