import engineerImg from '@/assets/carousel1.jpg'
import careImg from '@/assets/carousel2.jpg'
import diagImg from '@/assets/carousel3.png'
import { Carousel, CarouselContent, CarouselItem, CarouselNext, CarouselPagination, CarouselPrevious } from '@/components/ui/carousel'
import { useLanguage } from '../context/LanguageContext'

export function HeroSection() {
  const { t } = useLanguage()

  return (
    <section className="hero" aria-label={t.hero.ariaLabel}>
      <Carousel className="hero-carousel" autoPlayMs={8000} opts={{ align: 'start' }} aria-label={t.hero.ariaLabel}>
        <CarouselContent>
          <CarouselItem>
            <figure className="hero-slide hero-slide--diag">
              <img src={diagImg} alt={t.hero.slide3.alt} />
              <div className="hero-slide-content">
                <h2>{t.hero.slide3.title1}<br />{t.hero.slide3.title2}<span>.</span></h2>
                <p style={{ whiteSpace: 'pre-line' }}>{t.hero.slide3.desc}</p>
              </div>
              <figcaption className="sr-only">{t.hero.slide3.caption}</figcaption>
            </figure>
          </CarouselItem>
          <CarouselItem>
            <figure className="hero-slide hero-slide--service">
              <img src={careImg} alt={t.hero.slide2.alt} />
              <div className="hero-slide-content">
                <h2>{t.hero.slide2.title1}<br />{t.hero.slide2.title2}<span>.</span></h2>
                <p style={{ whiteSpace: 'pre-line' }}>{t.hero.slide2.desc}</p>
              </div>
              <figcaption className="sr-only">{t.hero.slide2.caption}</figcaption>
            </figure>
          </CarouselItem>
          <CarouselItem>
            <figure className="hero-slide hero-slide--engineer">
              <img src={engineerImg} alt={t.hero.slide1.alt} />
              <div className="hero-slide-content">
                <h2>{t.hero.slide1.title1}<br />{t.hero.slide1.title2}<span>.</span></h2>
                <p style={{ whiteSpace: 'pre-line' }}>{t.hero.slide1.desc}</p>
              </div>
              <figcaption className="sr-only">{t.hero.slide1.caption}</figcaption>
            </figure>
          </CarouselItem>
        </CarouselContent>
        <CarouselPrevious />
        <CarouselNext />
        <CarouselPagination labels={t.hero.tabs} />
      </Carousel>
    </section>
  )
}

