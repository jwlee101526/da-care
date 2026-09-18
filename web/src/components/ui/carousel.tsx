import * as React from 'react'
import useEmblaCarousel, { type UseEmblaCarouselType } from 'embla-carousel-react'
import { ArrowLeft, ArrowRight } from 'lucide-react'
import { cn } from '@/lib/utils'

type CarouselApi = UseEmblaCarouselType[1]

interface CarouselContextValue {
  carouselRef: UseEmblaCarouselType[0]
  api: CarouselApi
  canScrollPrev: boolean
  canScrollNext: boolean
  selectedIndex: number
  scrollPrev: () => void
  scrollNext: () => void
  scrollTo: (index: number) => void
}

const CarouselContext = React.createContext<CarouselContextValue | null>(null)

function useCarousel() {
  const context = React.useContext(CarouselContext)
  if (!context) throw new Error('Carousel components must be used inside Carousel.')
  return context
}

interface CarouselProps extends React.HTMLAttributes<HTMLDivElement> {
  autoPlayMs?: number
  opts?: Parameters<typeof useEmblaCarousel>[0]
}

function Carousel({ className, children, opts, autoPlayMs, ...props }: CarouselProps) {
  const [carouselRef, api] = useEmblaCarousel({ loop: true, ...opts })
  const [canScrollPrev, setCanScrollPrev] = React.useState(true)
  const [canScrollNext, setCanScrollNext] = React.useState(true)
  const [selectedIndex, setSelectedIndex] = React.useState(0)
  const autoPlayTimer = React.useRef<number | undefined>(undefined)

  const onSelect = React.useCallback((carouselApi: NonNullable<CarouselApi>) => {
    setCanScrollPrev(carouselApi.canScrollPrev())
    setCanScrollNext(carouselApi.canScrollNext())
    setSelectedIndex(carouselApi.selectedScrollSnap())
  }, [])

  const scrollPrev = React.useCallback(() => api?.scrollPrev(), [api])
  const scrollNext = React.useCallback(() => api?.scrollNext(), [api])
  const scrollTo = React.useCallback((index: number) => api?.scrollTo(index), [api])

  const stopAutoPlay = React.useCallback(() => {
    if (autoPlayTimer.current) window.clearInterval(autoPlayTimer.current)
  }, [])

  const startAutoPlay = React.useCallback(() => {
    if (!api || !autoPlayMs || window.matchMedia('(prefers-reduced-motion: reduce)').matches) return
    stopAutoPlay()
    autoPlayTimer.current = window.setInterval(() => api.scrollNext(), autoPlayMs)
  }, [api, autoPlayMs, stopAutoPlay])

  React.useEffect(() => {
    if (!api) return
    api.on('select', onSelect)
    api.on('reInit', onSelect)
    return () => {
      api.off('select', onSelect)
      api.off('reInit', onSelect)
    }
  }, [api, onSelect])

  React.useEffect(() => {
    startAutoPlay()
    return stopAutoPlay
  }, [startAutoPlay, stopAutoPlay])

  return (
    <CarouselContext.Provider value={{ carouselRef, api, canScrollPrev, canScrollNext, selectedIndex, scrollPrev, scrollNext, scrollTo }}>
      <div className={cn('relative', className)} role="region" aria-roledescription="carousel" onMouseEnter={stopAutoPlay} onMouseLeave={startAutoPlay} onFocus={stopAutoPlay} onBlur={startAutoPlay} {...props}>
        {children}
      </div>
    </CarouselContext.Provider>
  )
}

function CarouselContent({ className, ...props }: React.HTMLAttributes<HTMLDivElement>) {
  const { carouselRef } = useCarousel()
  return <div ref={carouselRef} className="overflow-hidden"><div className={cn('flex', className)} {...props} /></div>
}

function CarouselItem({ className, ...props }: React.HTMLAttributes<HTMLDivElement>) {
  return <div role="group" aria-roledescription="slide" className={cn('min-w-0 shrink-0 grow-0 basis-full', className)} {...props} />
}

function CarouselPrevious({ className, ...props }: React.ComponentProps<'button'>) {
  const { scrollPrev, canScrollPrev } = useCarousel()
  return <button type="button" className={cn('carousel-control carousel-previous', className)} disabled={!canScrollPrev} onClick={scrollPrev} aria-label="이전 슬라이드" {...props}><ArrowLeft size={20} /></button>
}

function CarouselNext({ className, ...props }: React.ComponentProps<'button'>) {
  const { scrollNext, canScrollNext } = useCarousel()
  return <button type="button" className={cn('carousel-control carousel-next', className)} disabled={!canScrollNext} onClick={scrollNext} aria-label="다음 슬라이드" {...props}><ArrowRight size={20} /></button>
}

function CarouselPagination({ className, labels }: { className?: string; labels: string[] }) {
  const { selectedIndex, scrollTo } = useCarousel()
  return (
    <div className={cn('carousel-pagination', className)} role="tablist" aria-label="슬라이드 선택">
      {labels.map((label, index) => (
        <button
          key={label}
          type="button"
          role="tab"
          aria-selected={selectedIndex === index}
          aria-label={label}
          onClick={() => scrollTo(index)}
        >
          <span className="sr-only">{label}</span>
          <span className="carousel-pagination-bar" />
        </button>
      ))}
    </div>
  )
}

export { Carousel, CarouselContent, CarouselItem, CarouselPrevious, CarouselNext, CarouselPagination }
