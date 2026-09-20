import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { ApiError } from '../lib/api'
import { useAuth } from '../context/AuthContext'
import { useLanguage } from '../context/LanguageContext'
import dacareLogo from '../assets/brand/dacare-logo.svg'

export function AuthPage({ signup = false }: { signup?: boolean }) {
  const showDemoAccount = import.meta.env.DEV
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
    setPhone('010-1234-5678')
    setAddress('서울특별시 서초구 방배동 100')
    setError('')
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
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
    <main className="auth-page">
      <div className="auth-container">
        <form className="auth-card" onSubmit={submit}>
          <Link to={homePath} className="auth-logo" title={lang === 'en' ? 'Back to DA-CARE Home' : 'DA-CARE 홈으로'}>
            <img src={dacareLogo} alt="DA-CARE" />
          </Link>
          <div className="auth-header">
            <h1>{signup ? (lang === 'en' ? 'Create Account' : '회원가입') : (lang === 'en' ? 'Sign In' : '로그인')}</h1>
            <p className="auth-subtitle">
              {signup
                ? (lang === 'en' ? 'Create your DA-CARE account to book and manage repairs.' : 'DA-CARE 계정을 생성하고 맞춤형 수리 서비스를 예약하세요.')
                : (lang === 'en' ? 'Please sign in to access DA-CARE repair services.' : 'DA-CARE 서비스 이용을 위해 로그인해 주세요.')}
            </p>
          </div>

          {showDemoAccount && (
            <div className="demo-account-box">
              <div className="demo-account-header">
                <span className="demo-badge">DEMO</span>
                <strong>{lang === 'en' ? 'Quick Demo Login / Autofill' : '체험용 데모 계정 안내'}</strong>
              </div>
              {!signup ? (
                <>
                  <p className="demo-account-desc">
                    {lang === 'en'
                      ? 'Use pre-registered test accounts for instant access.'
                      : '등록된 테스트 계정으로 원클릭 로그인을 하실 수 있습니다.'}
                  </p>
                  <div className="demo-btn-group">
                    <button type="button" className="demo-btn customer" onClick={fillCustomerDemo}>
                      {lang === 'en' ? 'Customer: demo@dacare.com' : '고객 계정 자동 입력 (demo@dacare.com)'}
                    </button>
                    <button type="button" className="demo-btn admin" onClick={fillAdminDemo}>
                      {lang === 'en' ? 'Admin: admin@dacare.com' : '관리자 계정 자동 입력 (admin@dacare.com)'}
                    </button>
                  </div>
                </>
              ) : (
                <>
                  <p className="demo-account-desc">
                    {lang === 'en'
                      ? 'Quickly fill demo registration values for testing.'
                      : '회원가입 테스트를 위해 샘플 고객 정보를 한 번에 채웁니다.'}
                  </p>
                  <button type="button" className="demo-btn customer full" onClick={fillSignupDemo}>
                    {lang === 'en' ? 'Auto-fill Demo Signup Data' : '샘플 회원 정보 자동 채우기'}
                  </button>
                </>
              )}
            </div>
          )}

          {error && (
            <div className="form-error" role="alert">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <circle cx="12" cy="12" r="10"/>
                <line x1="12" y1="8" x2="12" y2="12"/>
                <line x1="12" y1="16" x2="12.01" y2="16"/>
              </svg>
              <span>{error}</span>
            </div>
          )}

          <div className="auth-fields">
            <label className="auth-label">
              <span>{lang === 'en' ? 'Email Address' : '이메일'}</span>
              <input
                required
                name="email"
                type="email"
                placeholder="example@email.com"
                value={email}
                onChange={e => setEmail(e.target.value)}
                autoComplete="email"
              />
            </label>

            <label className="auth-label">
              <span>{lang === 'en' ? 'Password' : '비밀번호'}</span>
              <input
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
                <label className="auth-label">
                  <span>{lang === 'en' ? 'Full Name' : '성함'}</span>
                  <input
                    required
                    maxLength={50}
                    placeholder={lang === 'en' ? 'John Doe' : '홍길동'}
                    value={name}
                    onChange={e => setName(e.target.value)}
                    autoComplete="name"
                  />
                </label>
                <label className="auth-label">
                  <span>{lang === 'en' ? 'Phone Number' : '연락처'}</span>
                  <input
                    required
                    type="tel"
                    placeholder="010-1234-5678"
                    pattern="[0-9-]{9,13}"
                    value={phone}
                    onChange={e => setPhone(e.target.value)}
                    autoComplete="tel"
                  />
                </label>
                <label className="auth-label">
                  <span>{lang === 'en' ? 'Default Address' : '주소'}</span>
                  <input
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
            className="button primary full-width auth-submit-btn"
            disabled={isSubmitting}
          >
            {isSubmitting
              ? (lang === 'en' ? 'Processing...' : '처리 중...')
              : signup
              ? (lang === 'en' ? 'Sign Up' : '회원가입')
              : (lang === 'en' ? 'Sign In' : '로그인')}
          </button>

          <div className="auth-footer-links">
            <span className="auth-switch-text">
              {signup
                ? (lang === 'en' ? 'Already have an account?' : '이미 계정이 있으신가요?')
                : (lang === 'en' ? "Don't have an account?" : '아직 계정이 없으신가요?')}
            </span>
            <Link
              to={switchAuthPath}
              className="auth-switch-link"
              onClick={() => setError('')}
            >
              {signup
                ? (lang === 'en' ? 'Sign In' : '로그인하기')
                : (lang === 'en' ? 'Sign Up' : '회원가입하기')}
            </Link>
          </div>

          <Link to={homePath} className="auth-back-link">
            {lang === 'en' ? '← Back to Home' : '← 메인 홈으로 돌아가기'}
          </Link>
        </form>
      </div>
    </main>
  )
}
