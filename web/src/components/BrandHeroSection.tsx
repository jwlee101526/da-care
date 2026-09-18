import brandHeroWebp from '@/assets/carousel-brand.webp'
import brandHeroJpg from '@/assets/carousel-brand.jpg'

export function BrandHeroSection() {
  return (
    <section className="brand-hero" id="brand-hero" aria-label="DA-CARE Brand Hero">
      <h1 className="sr-only">DA-CARE | 스마트 기기 및 가전 통합 케어 솔루션</h1>
      <div className="brand-hero-bg">
        <picture>
          <source srcSet={brandHeroWebp} type="image/webp" />
          <img
            src={brandHeroJpg}
            alt="다케어 디바이스 & 가전 케어"
            fetchPriority="high"
            decoding="sync"
          />
        </picture>
      </div>
      <div className="brand-hero-fade" aria-hidden="true" />
    </section>
  )
}
