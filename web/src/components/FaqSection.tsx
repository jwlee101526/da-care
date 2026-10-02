import { Plus, Minus } from 'lucide-react'
import { Reveal } from './Reveal'
import { useLanguage } from '../context/LanguageContext'
import { cn } from '@/lib/utils'
import { eyebrow, sectionContainer, sectionPadding, sectionTitle, snapCentered } from './landing'

export function FaqSection() {
  const { t } = useLanguage()

  return (
    <section id="faq" className={cn(sectionPadding, snapCentered)}>
      <Reveal className={cn(sectionContainer, 'grid grid-cols-[1fr_1.7fr] gap-16 max-[850px]:grid-cols-1 max-[850px]:gap-8')}>
        <div>
          <span className={eyebrow}>{t.faq.eyebrow}</span>
          <h2 className={sectionTitle}>{t.faq.title}</h2>
          <p className="mt-4 text-[15px] text-muted landing-wide:text-[16px]">{t.faq.intro}</p>
        </div>
        <div className="border-t border-[#303946]">
          {t.faq.items.map(({ question, answer }) => (
            <details key={question} className="group border-b border-line">
              <summary className="flex min-h-[76px] cursor-pointer list-none items-center justify-between gap-6 py-5 text-[16px] font-medium max-[600px]:min-h-[72px] max-[600px]:gap-4 max-[600px]:text-[15px] landing-wide:min-h-[88px] landing-wide:text-[18px] [&::-webkit-details-marker]:hidden">
                <span>{question}</span>
                <span className="flex shrink-0 items-center justify-center text-[#7a8493]">
                  <Plus size={20} className="group-open:hidden" aria-hidden="true" />
                  <Minus size={20} className="hidden group-open:block group-open:text-brand" aria-hidden="true" />
                </span>
              </summary>
              <p className="pr-9 pb-6 text-[14px] text-muted max-[600px]:pr-0 landing-wide:text-[15px]">{answer}</p>
            </details>
          ))}
        </div>
      </Reveal>
    </section>
  )
}
