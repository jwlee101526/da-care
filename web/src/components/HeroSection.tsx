import engineerImg from '@/assets/carousel1.jpg'
import careImg from '@/assets/carousel2.jpg'
import diagImg from '@/assets/carousel3.png'
import { Carousel, CarouselContent, CarouselItem, CarouselNext, CarouselPagination, CarouselPrevious } from '@/components/ui/carousel'
import { cn } from '@/lib/utils'
import { useLanguage } from '../context/LanguageContext'
import { snapSection } from './landing'

const heroBackground = 'bg-[linear-gradient(108deg,var(--surface-subtle)_0%,#f1f3f6_36%,#f3f3f3_64%,#f3f3f3_100%)]'
// 히어로 영역 높이. 큰 화면에서는 랜딩 스냅에 맞춰 화면 높이를 채운다.
const heroHeight = 'min-h-[580px] max-[1050px]:min-h-[550px] max-[850px]:min-h-[520px] landing-snap:min-h-[calc(100dvh-var(--site-header-height))]'

const slideImage = 'absolute top-0 right-[min(5vw,72px)] bottom-0 left-auto h-full w-[min(44vw,600px)] object-contain object-right opacity-100 [mask-image:linear-gradient(90deg,transparent_0%,#000_20%,#000_100%)] max-[600px]:inset-0 max-[600px]:w-full max-[600px]:object-cover max-[600px]:object-[center_45%] max-[600px]:opacity-20 max-[600px]:[mask-image:none]'
// 슬라이드마다 사진 성격이 달라 크기와 마스크를 따로 맞춘다.
const slideImageVariant = {
  // 현장 점검: 세탁기 점검 실사 사진의 자연스러운 대비
  engineer: 'w-[min(45vw,600px)] opacity-95 [filter:contrast(1.02)] [mask-image:linear-gradient(90deg,transparent_0%,#000_22%,#000_100%)]',
  // 서비스 안내: carousel2.jpg 스튜디오 배경(#f3f3f3)과 맞춘다
  service: 'w-[min(50vw,700px)] [mask-image:linear-gradient(90deg,transparent_0%,#000_16%,#000_100%)]',
  // 스마트 진단: 선명한 앱 UI와 부드러운 입체 그림자
  diag: 'w-[min(44vw,590px)] [filter:drop-shadow(0_20px_40px_rgba(24,41,70,0.08))] [mask-image:none]',
}

function HeroSlide({ image, kind, title1, title2, desc, alt, caption }: { image: string; kind: keyof typeof slideImageVariant; title1: string; title2: string; desc: string; alt: string; caption: string }) {
  return (
    <figure className={cn('relative m-0 overflow-hidden after:pointer-events-none after:absolute after:inset-0 after:bg-[linear-gradient(90deg,var(--surface-subtle)_0%,#f8fafcdd_34%,#f8fafc00_68%)] after:content-[""] max-[600px]:after:bg-[#f8fafce0] max-[600px]:after:bg-none', heroBackground, heroHeight)}>
      <img className={cn(slideImage, slideImageVariant[kind])} src={image} alt={alt} />
      <div className={cn('relative z-[1] mx-auto flex w-[min(1120px,calc(100%-64px))] flex-col items-start justify-center text-navy landing-wide:w-[min(1360px,calc(100%-48px))]', heroHeight)}>
        <h2 className="mt-0 mb-5 max-w-[680px] text-[length:clamp(38px,3.8vw,56px)] leading-[1.2] font-[650] tracking-[-.05em] max-[1050px]:text-[48px] max-[850px]:text-[42px] max-[600px]:text-[36px] max-[600px]:leading-[1.25] [&>span]:text-brand">
          {title1}<br />{title2}<span>.</span>
        </h2>
        <p className="text-[16px] text-[#4c607f] max-[850px]:text-[14px]" style={{ whiteSpace: 'pre-line' }}>{desc}</p>
      </div>
      <figcaption className="sr-only">{caption}</figcaption>
    </figure>
  )
}

export function HeroSection() {
  const { t } = useLanguage()

  return (
    <section className={cn(heroHeight, snapSection)} aria-label={t.hero.ariaLabel}>
      <Carousel className={cn('min-w-0', heroBackground, heroHeight)} autoPlayMs={8000} opts={{ align: 'start' }} aria-label={t.hero.ariaLabel}>
        <CarouselContent>
          <CarouselItem>
            <HeroSlide image={diagImg} kind="diag" {...t.hero.slide3} />
          </CarouselItem>
          <CarouselItem>
            <HeroSlide image={careImg} kind="service" {...t.hero.slide2} />
          </CarouselItem>
          <CarouselItem>
            <HeroSlide image={engineerImg} kind="engineer" {...t.hero.slide1} />
          </CarouselItem>
        </CarouselContent>
        <CarouselPrevious />
        <CarouselNext />
        <CarouselPagination labels={t.hero.tabs} />
      </Carousel>
    </section>
  )
}
