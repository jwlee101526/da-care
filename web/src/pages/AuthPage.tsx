import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { ApiError } from '../lib/api'
import { isValidPhone } from '../lib/phone'
import { PhoneInput } from '../components/PhoneInput'
import { useAuth } from '../context/AuthContext'
import { useLanguage } from '../context/LanguageContext'
import dacareLogo from '../assets/brand/dacare-logo.svg'
import { buttonVariants } from '../components/ui/button'
import { formError } from '../components/formError'
import { cn } from '@/lib/utils'

const fieldLabel = 'flex flex-col gap-1.5 text-left text-[13px] font-semibold text-ink-body'
const fieldInput = 'box-border min-h-[46px] w-full rounded-lg border border-line-strong bg-white px-3.5 py-2.5 text-[14px] text-ink transition-[border-color,box-shadow] duration-150 ease-[ease] placeholder:text-[13px] placeholder:text-ink-faint focus:border-brand focus:shadow-[0_0_0_3px_rgba(36,87,214,0.15)] focus:outline-none'
const demoButton = 'w-full cursor-pointer rounded-lg border border-line-default bg-white px-3 py-2 text-left text-[12px] font-semibold text-ink-body transition-all duration-150 ease-[ease] hover:border-brand hover:bg-[#f0f5ff] hover:text-brand'
const demoDesc = 'mt-0 mb-2.5 text-[12px] leading-[1.45] text-ink-subtle'
const backLink = 'mt-4 inline-flex items-center justify-center text-center text-[13px] text-ink-subtle transition-colors duration-150 ease-[ease] hover:text-navy'

export function AuthPage({ signup = false }: { signup?: boolean }) {
  const showDemoAccount = import.meta.env.DEV || import.meta.env.VITE_SHOW_DEMO_ACCOUNT === 'true'
  const auth = useAuth()
  const { lang } = useLanguage()
  const navigate = useNavigate()
  const location = useLocation()
  const [error, setError] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  const stateData = (location.state as { from?: string; selection?: unknown; name?: string; phone?: string; address?: string } | null)
  const [name, setName] = useState(stateData?.name || '')
  const [phone, setPhone] = useState(stateData?.phone || '')
  const [address, setAddress] = useState(stateData?.address || '')

  const homePath = lang === 'en' ? '/en' : '/'
  const switchAuthPath = lang === 'en' ? (signup ? '/en/login' : '/en/signup') : (signup ? '/login' : '/signup')
  const defaultTarget = lang === 'en' ? '/en/reservations' : '/reservations'

  const fillCustomerDemo = () => {
    setEmail('demo@dacare.com')
    setPassword('password1234')
    setError('')
  }

  const fillAdminDemo = () => {
    setEmail('admin@dacare.com')
    setPassword('admin1234')
    setError('')
  }

  const fillSignupDemo = () => {
    const randomId = Math.floor(100 + Math.random() * 900)
    setEmail(`user${randomId}@dacare.com`)
    setPassword('password1234')
    setName('홍길동')
    setPhone('+821012345678')
    setAddress('서울특별시 서초구 방배동 100')
    setError('')
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    if (signup && !isValidPhone(phone)) {
      setError(lang === 'en' ? 'Enter a valid mobile number.' : '올바른 휴대전화 번호를 입력해 주세요.')
      return
    }
    setIsSubmitting(true)
    try {
      const role = signup ? await auth.signup({ email, password, name, phone, address }) : await auth.login(email, password)
      const target = stateData?.from ?? (role === 'ADMIN' ? '/admin' : defaultTarget)
      navigate(target, { state: stateData?.selection })
    } catch (e) {
      setError(e instanceof ApiError ? e.message : (lang === 'en' ? 'An error occurred during request.' : '요청 처리 중 오류가 발생했습니다.'))
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <main className="box-border flex min-h-[calc(100dvh-var(--site-header-height))] items-center justify-center overflow-y-auto bg-[linear-gradient(180deg,var(--surface-subtle)_0%,#edf2f8_100%)] px-5 pt-12 pb-16 max-[480px]:items-start max-[480px]:px-4 max-[480px]:pt-6 max-[480px]:pb-12">
      <div className="m-auto w-full max-w-[440px]">
        <form className="box-border flex w-full flex-col rounded-2xl border border-line-default bg-white px-9 py-10 shadow-[0_10px_30px_-4px_rgba(24,41,70,0.08),0_4px_12px_-2px_rgba(24,41,70,0.04)] max-[480px]:px-5 max-[480px]:py-7" onSubmit={submit}>
          <Link to={homePath} className="mx-auto mt-0 mb-5 inline-flex items-center justify-center" title={lang === 'en' ? 'Back to DA-CARE Home' : 'DA-CARE 홈으로'}>
            <img className="block h-8 w-auto" src={dacareLogo} alt="DA-CARE" />
          </Link>
          <div className="mb-6 text-center">
            <h1 className="mb-2 text-[24px] font-bold tracking-[-0.03em] text-navy">{signup ? (lang === 'en' ? 'Create Account' : '회원가입') : (lang === 'en' ? 'Sign In' : '로그인')}</h1>
            <p className="text-[14px] leading-[1.5] text-ink-subtle">
              {signup
                ? (lang === 'en' ? 'Create your DA-CARE account to book and manage repairs.' : 'DA-CARE 계정을 생성하고 맞춤형 수리 서비스를 예약하세요.')
                : (lang === 'en' ? 'Please sign in to access DA-CARE repair services.' : 'DA-CARE 서비스 이용을 위해 로그인해 주세요.')}
            </p>
          </div>

          {showDemoAccount && (
            <div className="mb-5 rounded-xl border border-dashed border-line-strong bg-surface-subtle px-4 py-3.5">
              <div className="mb-1.5 flex items-center gap-2">
                <span className="inline-block rounded-[4px] bg-brand px-1.5 py-0.5 text-[10px] font-extrabold tracking-[0.05em] text-white">DEMO</span>
                <strong className="text-[13px] font-bold text-ink">{lang === 'en' ? 'Quick Demo Login / Autofill' : '체험용 데모 계정 안내'}</strong>
              </div>
              {!signup ? (
                <>
                  <p className={demoDesc}>
                    {lang === 'en'
                      ? 'Use pre-registered test accounts for instant access.'
                      : '등록된 테스트 계정으로 원클릭 로그인을 하실 수 있습니다.'}
                  </p>
                  <div className="flex flex-col gap-1.5">
                    <button type="button" className={demoButton} onClick={fillCustomerDemo}>
                      {lang === 'en' ? 'Customer: demo@dacare.com' : '고객 계정 자동 입력 (demo@dacare.com)'}
                    </button>
                    <button type="button" className={demoButton} onClick={fillAdminDemo}>
                      {lang === 'en' ? 'Admin: admin@dacare.com' : '관리자 계정 자동 입력 (admin@dacare.com)'}
                    </button>
                  </div>
                </>
              ) : (
                <>
                  <p className={demoDesc}>
                    {lang === 'en'
                      ? 'Quickly fill demo registration values for testing.'
                      : '회원가입 테스트를 위해 샘플 고객 정보를 한 번에 채웁니다.'}
                  </p>
                  <button type="button" className={cn(demoButton, 'justify-center text-center')} onClick={fillSignupDemo}>
                    {lang === 'en' ? 'Auto-fill Demo Signup Data' : '샘플 회원 정보 자동 채우기'}
                  </button>
                </>
              )}
            </div>
          )}

          {error && (
            <div className={formError} role="alert">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <circle cx="12" cy="12" r="10"/>
                <line x1="12" y1="8" x2="12" y2="12"/>
                <line x1="12" y1="16" x2="12.01" y2="16"/>
              </svg>
              <span>{error}</span>
            </div>
          )}

          <div className="mb-6 flex flex-col gap-4">
            <label className={fieldLabel}>
              <span className="block">{lang === 'en' ? 'Email Address' : '이메일'}</span>
              <input
                className={fieldInput}
                required
                name="email"
                type="email"
                placeholder="example@email.com"
                value={email}
                onChange={e => setEmail(e.target.value)}
                autoComplete="email"
              />
            </label>

            <label className={fieldLabel}>
              <span className="block">{lang === 'en' ? 'Password' : '비밀번호'}</span>
              <input
                className={fieldInput}
                required
                name="password"
                type="password"
                placeholder={signup ? (lang === 'en' ? '8+ characters' : '8자리 이상 입력') : (lang === 'en' ? 'Enter password' : '비밀번호를 입력하세요')}
                value={password}
                onChange={e => setPassword(e.target.value)}
                minLength={signup ? 8 : undefined}
                autoComplete={signup ? 'new-password' : 'current-password'}
              />
            </label>

            {signup && (
              <>
                <label className={fieldLabel}>
                  <span className="block">{lang === 'en' ? 'Full Name' : '성함'}</span>
                  <input
                    className={fieldInput}
                    required
                    maxLength={50}
                    placeholder={lang === 'en' ? 'John Doe' : '홍길동'}
                    value={name}
                    onChange={e => setName(e.target.value)}
                    autoComplete="name"
                  />
                </label>
                <label className={fieldLabel}>
                  <span className="block">{lang === 'en' ? 'Mobile Number' : '휴대전화 번호'}</span>
                  <PhoneInput
                    required
                    className={fieldInput}
                    value={phone}
                    onChange={setPhone}
                  />
                </label>
                <label className={fieldLabel}>
                  <span className="block">{lang === 'en' ? 'Default Address' : '주소'}</span>
                  <input
                    className={fieldInput}
                    required
                    maxLength={200}
                    placeholder={lang === 'en' ? 'Address for on-site repair visits' : '방문 수리를 받으실 기본 주소'}
                    value={address}
                    onChange={e => setAddress(e.target.value)}
                    autoComplete="street-address"
                  />
                </label>
              </>
            )}
          </div>

          <button
            type="submit"
            className={buttonVariants({
              variant: 'primary',
              className: 'mt-1 mb-5 h-12 w-full cursor-pointer rounded-lg text-[15px] leading-[1.4] font-semibold shadow-[0_2px_8px_rgba(36,87,214,0.25)] transition-all duration-200 ease-[ease] enabled:hover:-translate-y-px enabled:hover:bg-brand-hover enabled:hover:shadow-[0_4px_12px_rgba(36,87,214,0.35)]',
            })}
            disabled={isSubmitting}
          >
            {isSubmitting
              ? (lang === 'en' ? 'Processing...' : '처리 중...')
              : signup
              ? (lang === 'en' ? 'Sign Up' : '회원가입')
              : (lang === 'en' ? 'Sign In' : '로그인')}
          </button>

          <div className="flex items-center justify-center gap-2 border-t border-surface-muted pt-4 text-[13px]">
            <span className="text-ink-subtle">
              {signup
                ? (lang === 'en' ? 'Already have an account?' : '이미 계정이 있으신가요?')
                : (lang === 'en' ? "Don't have an account?" : '아직 계정이 없으신가요?')}
            </span>
            <Link
              to={switchAuthPath}
              className="font-semibold text-brand underline underline-offset-[3px] transition-colors duration-150 ease-[ease] hover:text-brand-hover"
              onClick={() => setError('')}
            >
              {signup
                ? (lang === 'en' ? 'Sign In' : '로그인하기')
                : (lang === 'en' ? 'Sign Up' : '회원가입하기')}
            </Link>
          </div>

          {!signup && (
            <Link to={lang === 'en' ? '/en/reservations/lookup' : '/reservations/lookup'} className={backLink}>
              {lang === 'en' ? 'Booked without an account? Look up your booking' : '비회원으로 예약하셨나요? 비회원 예약 조회'}
            </Link>
          )}

          <Link to={homePath} className={backLink}>
            {lang === 'en' ? '← Back to Home' : '← 메인 홈으로 돌아가기'}
          </Link>
        </form>
      </div>
    </main>
  )
}
