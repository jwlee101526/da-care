// 랜딩(#main) 섹션들이 함께 쓰는 Tailwind 클래스.
// landing-snap / landing-wide 조건은 index.css의 @custom-variant에 정의돼 있다.

/** 큰 화면에서 섹션 하나가 화면 하나를 채우고 스크롤이 섹션 단위로 멈춘다. */
export const snapSection = 'landing-snap:min-h-[calc(100dvh-var(--site-header-height))] landing-snap:snap-start landing-snap:snap-always'
/** snapSection + 섹션 내용을 세로 가운데 정렬 */
export const snapCentered = `${snapSection} landing-snap:flex landing-snap:items-center`

export const sectionPadding = 'py-24 max-[850px]:py-[72px] max-[600px]:py-[60px]'
export const sectionContainer = 'container mx-auto w-[min(1120px,calc(100%-64px))] max-[600px]:w-[calc(100%-40px)] landing-wide:w-[min(1360px,calc(100%-48px))]'

export const sectionHeading = 'mb-10 flex items-end justify-between gap-8 max-[600px]:mb-7 max-[600px]:flex-col max-[600px]:items-start max-[600px]:gap-4'
export const sectionHeadingDesc = 'text-[15px] text-muted max-[600px]:text-[14px] landing-wide:text-[16px]'
export const eyebrow = 'mb-3.5 block text-[16px] font-bold tracking-[-.01em] text-brand max-[600px]:mb-3 max-[600px]:text-[15px]'
export const sectionTitle = 'landing-wide:text-[44px]'
