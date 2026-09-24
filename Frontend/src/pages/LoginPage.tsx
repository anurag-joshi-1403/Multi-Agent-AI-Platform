import { useEffect, useRef, useState } from 'react'
import type { FormEvent, MouseEvent } from 'react'
import {
  IconArrowRight,
  IconBot,
  IconEye,
  IconEyeOff,
  IconGoogle,
  IconKey,
  IconLock,
  IconMail,
  IconMenu,
  IconMoon,
  IconPanelRight,
  IconPaperclip,
  IconSun,
  IconX,
} from '../components/Icons'
import { loadString, saveString, STORAGE_KEYS } from '../lib/storage'
import '../login.css'
import type { Theme } from '../hooks/useTheme'

interface Props {
  onLogin: (username: string, password: string) => Promise<void>
  /** Shared with the console via `App` — one dark/light preference for the whole app, login screen
   * included, rather than this page defaulting to light regardless of what was chosen inside. */
  theme: Theme
  onToggleTheme: () => void
}

const NAV = [
  { id: 'home', label: 'Home' },
  { id: 'about', label: 'About' },
  { id: 'features', label: 'Features' },
  { id: 'contact', label: 'Contact' },
] as const

const FEATURES = [
  {
    Icon: IconBot,
    title: 'Five specialised agents',
    text: 'Coding, research, summarising, document Q&A and general chat — each one self-describing, with its own options.',
  },
  {
    Icon: IconPaperclip,
    title: 'Attach files to any agent',
    text: 'Drop in a PDF or text file and it stays in context for the whole conversation, not just one message.',
  },
  {
    Icon: IconPanelRight,
    title: 'Inspect every response',
    text: 'Latency, model, token usage and the exact request behind each reply — nothing happens out of sight.',
  },
  {
    Icon: IconKey,
    title: 'Bring your own model',
    text: 'Google Gemini by default; switch to Anthropic Claude or OpenAI with a single environment variable.',
  },
]

const REPO_URL = 'https://github.com/anurag-joshi-1403/Multi-Agent-AI-Platform'

// The backend has no OAuth, self-service sign-up or password reset yet. These controls are part of
// the page design, so rather than dead links they explain the actual state in one line.
const NOTICES = {
  google: 'Google sign-in isn’t set up on this backend yet — sign in with your username and password.',
  signup: 'There’s no self-service sign-up yet — accounts are created by whoever runs this backend.',
  reset: 'Passwords are managed by whoever runs this backend — there’s no self-service reset yet.',
} as const

function scrollToSection(id: string) {
  if (id === 'home') {
    window.scrollTo({ top: 0, behavior: 'smooth' })
    return
  }
  document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

/** Login front page. The form is UI only — `onLogin` is where authentication gets wired in. */
export function LoginPage({ onLogin, theme, onToggleTheme }: Props) {
  const remembered = loadString(STORAGE_KEYS.rememberUser)
  const [username, setUsername] = useState(remembered ?? '')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [remember, setRemember] = useState(remembered !== null)
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [menuOpen, setMenuOpen] = useState(false)
  const [activeSection, setActiveSection] = useState<string>('home')
  const [scrolled, setScrolled] = useState(false)
  const usernameRef = useRef<HTMLInputElement>(null)

  // Highlight the nav link for whichever section is in view.
  useEffect(() => {
    const observer = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) if (entry.isIntersecting) setActiveSection(entry.target.id)
      },
      { rootMargin: '-45% 0px -50% 0px' },
    )
    for (const { id } of NAV) {
      const el = document.getElementById(id)
      if (el) observer.observe(el)
    }
    return () => observer.disconnect()
  }, [])

  // Give the sticky nav a solid backing once the hero has scrolled underneath it — the translucent,
  // blurred look reads fine over the hero itself, but a card or section edge sliding under a
  // half-see-through bar looks unfinished.
  useEffect(() => {
    function onScroll() {
      setScrolled(window.scrollY > 8)
    }
    onScroll()
    window.addEventListener('scroll', onScroll, { passive: true })
    return () => window.removeEventListener('scroll', onScroll)
  }, [])

  function go(e: MouseEvent, id: string) {
    e.preventDefault()
    setMenuOpen(false)
    scrollToSection(id)
  }

  function goToLogin(e: MouseEvent) {
    e.preventDefault()
    setMenuOpen(false)
    scrollToSection('home')
    usernameRef.current?.focus()
  }

  function goToSignUp(e: MouseEvent) {
    e.preventDefault()
    setMenuOpen(false)
    scrollToSection('home')
    show('signup')
  }

  function show(kind: keyof typeof NOTICES) {
    setError(null)
    setNotice(NOTICES[kind])
  }

  function toggleRemember(checked: boolean) {
    setRemember(checked)
    if (!checked) saveString(STORAGE_KEYS.rememberUser, null)
  }

  async function submit(e: FormEvent) {
    e.preventDefault()
    if (busy) return
    const user = username.trim()
    if (!user || !password) {
      setNotice(null)
      setError('Enter your email or username and your password.')
      return
    }
    setBusy(true)
    setError(null)
    setNotice(null)
    try {
      await onLogin(user, password)
      saveString(STORAGE_KEYS.rememberUser, remember ? user : null)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Sign-in failed')
      setBusy(false)
    }
  }

  return (
    <div className="auth-page">
      <header className={`ap-nav ${scrolled ? 'ap-nav-scrolled' : ''}`}>
        <div className="ap-nav-inner">
          <a className="ap-brand" href="#home" onClick={(e) => go(e, 'home')} aria-label="Multi-Agent AI Platform home">
            <span className="ap-brand-mark" aria-hidden>
              <IconBot width={16} height={16} />
            </span>
            <span>Multi-Agent AI</span>
          </a>

          <nav id="ap-primary-nav" className={`ap-links ${menuOpen ? 'open' : ''}`} aria-label="Primary">
            {NAV.map(({ id, label }) => (
              <a
                key={id}
                href={`#${id}`}
                className={activeSection === id ? 'active' : ''}
                aria-current={activeSection === id ? 'true' : undefined}
                onClick={(e) => go(e, id)}
              >
                {label}
              </a>
            ))}
            <a href="#home" className="ap-mobile-only" onClick={goToLogin}>
              Login
            </a>
            <a href="#home" className="ap-mobile-only ap-mobile-secondary" onClick={goToSignUp}>
              Sign up
            </a>
          </nav>

          <div className="ap-nav-actions">
            <button
              type="button"
              className="ap-theme-toggle"
              onClick={onToggleTheme}
              aria-label={`Switch to ${theme === 'dark' ? 'light' : 'dark'} theme`}
              title={`Switch to ${theme === 'dark' ? 'light' : 'dark'} theme`}
            >
              {theme === 'dark' ? <IconSun /> : <IconMoon />}
            </button>
            <a href="#home" className="ap-btn ap-btn-outline ap-desktop-only" onClick={goToSignUp}>
              Sign up
            </a>
            <a href="#home" className="ap-btn ap-btn-primary ap-desktop-only" onClick={goToLogin}>
              Login
            </a>
          </div>

          <button
            type="button"
            className="ap-burger"
            onClick={() => setMenuOpen((v) => !v)}
            aria-expanded={menuOpen}
            aria-controls="ap-primary-nav"
            aria-label={menuOpen ? 'Close menu' : 'Open menu'}
          >
            {menuOpen ? <IconX /> : <IconMenu />}
          </button>
        </div>
      </header>

      <main>
        <section id="home" className="ap-hero">
          <span className="ap-blob ap-blob-1" aria-hidden />
          <span className="ap-blob ap-blob-2" aria-hidden />
          <span className="ap-blob ap-blob-3" aria-hidden />

          <form className="ap-card" onSubmit={submit} noValidate>
            <h1>Welcome Back!</h1>
            <p className="ap-sub">Login to continue to your account</p>

            {error && (
              <p className="ap-alert ap-alert-error" role="alert">
                {error}
              </p>
            )}
            {!error && notice && (
              <p className="ap-alert ap-alert-info" role="status">
                {notice}
              </p>
            )}

            <label className="ap-field">
              <span className="ap-label">Username</span>
              <span className="ap-input-wrap">
                <IconMail className="ap-input-icon" />
                <input
                  ref={usernameRef}
                  className="ap-input"
                  type="text"
                  autoComplete="username"
                  autoFocus
                  placeholder="Enter your username"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  disabled={busy}
                />
              </span>
            </label>

            <label className="ap-field">
              <span className="ap-label">Password</span>
              <span className="ap-input-wrap">
                <IconLock className="ap-input-icon" />
                <input
                  className="ap-input"
                  type={showPassword ? 'text' : 'password'}
                  autoComplete="current-password"
                  placeholder="Enter your password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  disabled={busy}
                />
                <button
                  type="button"
                  className="ap-eye"
                  onClick={() => setShowPassword((v) => !v)}
                  aria-label={showPassword ? 'Hide password' : 'Show password'}
                  aria-pressed={showPassword}
                  title={showPassword ? 'Hide password' : 'Show password'}
                >
                  {showPassword ? <IconEyeOff /> : <IconEye />}
                </button>
              </span>
            </label>

            <div className="ap-row">
              <label className="ap-check">
                <input type="checkbox" checked={remember} onChange={(e) => toggleRemember(e.target.checked)} />
                <span>Remember me</span>
              </label>
              <a href="#forgot" className="ap-link" onClick={(e) => { e.preventDefault(); show('reset') }}>
                Forgot Password?
              </a>
            </div>

            <button type="submit" className="ap-btn ap-btn-primary ap-btn-lg" disabled={busy}>
              {busy ? <span className="ap-spinner" aria-hidden /> : null}
              {busy ? 'Signing in…' : 'Login'}
              {!busy && <IconArrowRight />}
            </button>

            <div className="ap-divider" aria-hidden>
              <span>OR</span>
            </div>

            <button type="button" className="ap-btn ap-btn-google" onClick={() => show('google')}>
              <IconGoogle />
              Continue with Google
            </button>

            <p className="ap-foot">
              Don&rsquo;t have an account?{' '}
              <a href="#signup" className="ap-link" onClick={(e) => { e.preventDefault(); show('signup') }}>
                Sign Up
              </a>
            </p>
          </form>
        </section>

        <section id="about" className="ap-section">
          <span className="ap-eyebrow">About</span>
          <h2>One console, five specialist agents</h2>
          <p className="ap-lead">
            Multi-Agent AI Platform is a workspace for talking to a set of focused AI agents — coding,
            research, summarising, document Q&amp;A and general chat. Each agent describes its own
            options, so the console builds the right controls for it automatically. Sign in to pick an
            agent, attach files, tune its settings and inspect exactly what was sent to the model and what
            came back.
          </p>
          <div className="ap-stats" aria-label="Platform at a glance">
            <span className="ap-stat"><b>5</b> agents</span>
            <span className="ap-stat"><b>3</b> model providers</span>
            <span className="ap-stat"><b>62</b> automated tests</span>
          </div>
        </section>

        <div className="ap-band">
          <section id="features" className="ap-section">
            <span className="ap-eyebrow">Features</span>
            <h2>Built for working with AI, not just chatting</h2>
            <p className="ap-lead">
              Everything below is live in the console today.
            </p>
            <div className="ap-grid">
              {FEATURES.map(({ Icon, title, text }) => (
                <article key={title} className="ap-feature">
                  <span className="ap-feature-icon" aria-hidden>
                    <Icon />
                  </span>
                  <h3>{title}</h3>
                  <p>{text}</p>
                </article>
              ))}
            </div>
          </section>
        </div>

        <section id="contact" className="ap-section">
          <span className="ap-eyebrow">Contact</span>
          <h2>Questions, ideas or a bug?</h2>
          <p className="ap-lead">
            The project is open source. The fastest way to reach the maintainer is through the repository —
            issues and pull requests are both welcome.
          </p>
          <div className="ap-contact-actions">
            <a className="ap-btn ap-btn-primary" href={REPO_URL} target="_blank" rel="noreferrer">
              View on GitHub
            </a>
            <a className="ap-btn ap-btn-outline" href={`${REPO_URL}/issues/new`} target="_blank" rel="noreferrer">
              Open an issue
            </a>
          </div>
        </section>
      </main>

      <footer className="ap-footer">© 2026 Multi-Agent AI Platform</footer>
    </div>
  )
}
