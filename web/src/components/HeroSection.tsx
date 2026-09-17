import { ArrowRight, ArrowUpRight } from 'lucide-react'
import engineerImg from '@/assets/carousel1.jpg'
import careImg from '@/assets/carousel2.jpg'
import diagImg from '@/assets/carousel3.png'
import { Carousel, CarouselContent, CarouselItem, CarouselNext, CarouselPagination, CarouselPrevious } from '@/components/ui/carousel'
import { useLanguage } from '../context/LanguageContext'

interface HeroSectionProps {
  onOpenReservation: () => void
  onOpenChat: () => void
}

export function HeroSection({ onOpenReservation, onOpenChat }: HeroSectionProps) {
  const { t } = useLanguage()

  return (
    <section className="hero" id="top" aria-labelledby="hero-title">
      <Carousel className="hero-carousel" autoPlayMs={8000} opts={{ align: 'start' }} aria-label={t.hero.ariaLabel}>
        <CarouselContent>
          <CarouselItem>
            <figure className="hero-slide hero-slide--engineer">
              <img src={engineerImg} alt={t.hero.slide1.alt} fetchPriority="high" />
              <div className="hero-slide-content">
                <span className="hero-eyebrow">{t.hero.slide1.eyebrow}</span>
                <h1 id="hero-title">{t.hero.slide1.title1}<br />{t.hero.slide1.title2}<span>.</span></h1>
                <p style={{ whiteSpace: 'pre-line' }}>{t.hero.slide1.desc}</p>
                <div className="hero-actions">
                  <button className="button light" onClick={onOpenReservation}>{t.hero.btnReservation} <ArrowUpRight size={18} /></button>
                  <button className="button hero-secondary" onClick={onOpenChat}>{t.hero.btnChat} <ArrowRight size={18} /></button>
                </div>
                <p className="hero-note">{t.hero.note}</p>
              </div>
              <figcaption className="sr-only">{t.hero.slide1.caption}</figcaption>
            </figure>
          </CarouselItem>
          <CarouselItem>
            <figure className="hero-slide hero-slide--service">
              <img src={careImg} alt={t.hero.slide2.alt} />
              <div className="hero-slide-content">
                <span className="hero-eyebrow">{t.hero.slide2.eyebrow}</span>
                <h2>{t.hero.slide2.title1}<br />{t.hero.slide2.title2}<span>.</span></h2>
                <p style={{ whiteSpace: 'pre-line' }}>{t.hero.slide2.desc}</p>
                <div className="hero-actions">
                  <button className="button light" onClick={onOpenReservation}>{t.hero.btnReservation} <ArrowUpRight size={18} /></button>
                  <button className="button hero-secondary" onClick={onOpenChat}>{t.hero.btnChat} <ArrowRight size={18} /></button>
                </div>
                <p className="hero-note">{t.hero.note}</p>
              </div>
              <figcaption className="sr-only">{t.hero.slide2.caption}</figcaption>
            </figure>
          </CarouselItem>
          <CarouselItem>
            <figure className="hero-slide hero-slide--diag">
              <img src={diagImg} alt={t.hero.slide3.alt} />
              <div className="hero-slide-content">
                <span className="hero-eyebrow">{t.hero.slide3.eyebrow}</span>
                <h2>{t.hero.slide3.title1}<br />{t.hero.slide3.title2}<span>.</span></h2>
                <p style={{ whiteSpace: 'pre-line' }}>{t.hero.slide3.desc}</p>
                <div className="hero-actions">
                  <button className="button light" onClick={onOpenReservation}>{t.hero.btnReservation} <ArrowUpRight size={18} /></button>
                  <button className="button hero-secondary" onClick={onOpenChat}>{t.hero.btnChat} <ArrowRight size={18} /></button>
                </div>
                <p className="hero-note">{t.hero.note}</p>
              </div>
              <figcaption className="sr-only">{t.hero.slide3.caption}</figcaption>
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
