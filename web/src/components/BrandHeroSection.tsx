import brandHeroWebp from '@/assets/carousel-brand.webp'
import brandHeroJpg from '@/assets/carousel-brand.jpg'
import { cn } from '@/lib/utils'
import { snapCentered } from './landing'

const fillImage = 'block h-full w-full object-cover object-center [image-rendering:-webkit-optimize-contrast] [image-rendering:high-quality] [transform:translateZ(0)]'

export function BrandHeroSection() {
  return (
    <section
      className={cn(
        'relative h-[calc(100dvh-var(--site-header-height))] min-h-[580px] w-full overflow-hidden bg-[#f1f3f6] max-[600px]:h-[clamp(380px,62vh,520px)] max-[600px]:min-h-[380px] max-[600px]:bg-line-subtle',
        snapCentered,
      )}
      id="brand-hero"
      aria-label="DA-CARE Brand Hero"
    >
      <h1 className="sr-only">DA-CARE | 스마트 기기 및 가전 통합 케어 솔루션</h1>
      <div className="absolute inset-0 h-full w-full">
        <picture className={fillImage}>
          <source srcSet={brandHeroWebp} type="image/webp" />
          <img
            className={cn(fillImage, 'max-[600px]:object-[48%_center]')}
            src={brandHeroJpg}
            alt="다케어 디바이스 & 가전 케어"
            fetchPriority="high"
            decoding="sync"
          />
        </picture>
      </div>
      <div className="pointer-events-none absolute inset-x-0 bottom-0 h-12 bg-[linear-gradient(180deg,transparent_0%,var(--surface-subtle)_100%)] max-[600px]:h-10" aria-hidden="true" />
    </section>
  )
}
