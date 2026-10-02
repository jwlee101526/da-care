import { MessageSquare, ClipboardList, CalendarDays, Wrench } from 'lucide-react'
import { Reveal } from './Reveal'
import { useLanguage } from '../context/LanguageContext'
import { cn } from '@/lib/utils'
import { eyebrow, sectionContainer, sectionHeading, sectionHeadingDesc, sectionPadding, sectionTitle, snapCentered } from './landing'

const stepIcons = [MessageSquare, ClipboardList, CalendarDays, Wrench]

// 4단계를 한 줄(좁은 화면은 2줄)로 나누고 칸 사이에 구분선을 둔다. 줄의 첫 칸·끝 칸은 바깥 여백이 없다.
function stepClass(index: number, count: number) {
  return cn(
    'border-l border-[#dce1e7] px-7 max-[600px]:px-5',
    index === 0 && 'border-l-0 pl-0 max-[600px]:pl-0',
    index === count - 1 && 'pr-0 max-[600px]:pr-0',
    index === 1 && 'max-[850px]:pr-0 max-[600px]:pr-0',
    index === 2 && 'max-[850px]:border-l-0 max-[850px]:pl-0 max-[600px]:pl-0',
  )
}

export function OrderStatusSection() {
  const { t } = useLanguage()

  return (
    <section id="status" className={cn(sectionPadding, snapCentered, 'bg-[#f6f7f9]')}>
      <Reveal className={sectionContainer}>
        <div className={sectionHeading}>
          <div>
            <span className={eyebrow}>{t.process.eyebrow}</span>
            <h2 className={sectionTitle}>{t.process.title1}<br />{t.process.title2}</h2>
          </div>
          <p className={sectionHeadingDesc} style={{ whiteSpace: 'pre-line' }}>{t.process.desc}</p>
        </div>
        <ol className="mx-0 mt-12 mb-6 grid list-none grid-cols-4 p-0 max-[850px]:grid-cols-2 max-[850px]:gap-x-0 max-[850px]:gap-y-9 max-[600px]:mt-8 landing-wide:mt-14">
          {t.process.steps.map(({ title, text }, index) => {
            const Icon = stepIcons[index] || Wrench
            return (
              <li key={title} className={stepClass(index, t.process.steps.length)}>
                <div className="mb-[30px] flex items-center justify-between text-brand max-[600px]:mb-5 landing-wide:mb-9">
                  <span className="text-[14px] landing-wide:text-[16px]">0{index + 1}</span>
                  <Icon className="text-[#7b889e] landing-wide:h-[30px] landing-wide:w-[30px]" size={24} strokeWidth={1.5} />
                </div>
                <h3 className="mb-3 text-[20px] font-semibold max-[600px]:text-[18px] landing-wide:text-[24px]">{title}</h3>
                <p className="text-[14px] text-muted max-[600px]:text-[13px] landing-wide:text-[15px]">{text}</p>
              </li>
            )
          })}
        </ol>
        <p className="mt-9 text-[13px] text-muted max-[600px]:text-[12px] landing-wide:text-[15px]">{t.process.note}</p>
      </Reveal>
    </section>
  )
}
