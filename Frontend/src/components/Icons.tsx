import type { SVGProps } from 'react'

type IconProps = SVGProps<SVGSVGElement>

const base: IconProps = {
  viewBox: '0 0 24 24',
  fill: 'none',
  stroke: 'currentColor',
  strokeWidth: 1.8,
  strokeLinecap: 'round',
  strokeLinejoin: 'round',
  'aria-hidden': true,
}

export function IconGrid(p: IconProps) {
  return (
    <svg {...base} {...p}>
      <rect x="3" y="3" width="7" height="7" rx="1.5" />
      <rect x="14" y="3" width="7" height="7" rx="1.5" />
      <rect x="3" y="14" width="7" height="7" rx="1.5" />
      <rect x="14" y="14" width="7" height="7" rx="1.5" />
    </svg>
  )
}

export function IconChat(p: IconProps) {
  return (
    <svg {...base} {...p}>
      <path d="M21 12a8 8 0 0 1-11.6 7.1L4 20l1.1-4.2A8 8 0 1 1 21 12Z" />
      <path d="M8.5 11h7M8.5 14h4" />
    </svg>
  )
}

export function IconBot(p: IconProps) {
  return (
    <svg {...base} {...p}>
      <rect x="4" y="8" width="16" height="12" rx="3" />
      <path d="M12 8V4M9 4h6" />
      <circle cx="9" cy="14" r="1.2" fill="currentColor" stroke="none" />
      <circle cx="15" cy="14" r="1.2" fill="currentColor" stroke="none" />
      <path d="M2 13v3M22 13v3" />
    </svg>
  )
}

export function IconSettings(p: IconProps) {
  return (
    <svg {...base} {...p}>
      <circle cx="12" cy="12" r="3" />
      <path d="M19.4 15a1.7 1.7 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-1.8-.3 1.7 1.7 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.1a1.7 1.7 0 0 0-1.1-1.5 1.7 1.7 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.7 1.7 0 0 0 .3-1.8 1.7 1.7 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.1a1.7 1.7 0 0 0 1.5-1.1 1.7 1.7 0 0 0-.3-1.8l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 1.8.3H9a1.7 1.7 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.1a1.7 1.7 0 0 0 1 1.5 1.7 1.7 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.8V9a1.7 1.7 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.1a1.7 1.7 0 0 0-1.5 1Z" />
    </svg>
  )
}

export function IconSend(p: IconProps) {
  return (
    <svg {...base} {...p}>
      <path d="M22 2 11 13" />
      <path d="M22 2 15 22l-4-9-9-4 20-7Z" />
    </svg>
  )
}

export function IconPlus(p: IconProps) {
  return (
    <svg {...base} {...p}>
      <path d="M12 5v14M5 12h14" />
    </svg>
  )
}

export function IconTrash(p: IconProps) {
  return (
    <svg {...base} {...p}>
      <path d="M3 6h18M8 6V4h8v2M6 6l1 14h10l1-14" />
    </svg>
  )
}

export function IconSun(p: IconProps) {
  return (
    <svg {...base} {...p}>
      <circle cx="12" cy="12" r="4" />
      <path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4" />
    </svg>
  )
}

export function IconMoon(p: IconProps) {
  return (
    <svg {...base} {...p}>
      <path d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8Z" />
    </svg>
  )
}

export function IconRefresh(p: IconProps) {
  return (
    <svg {...base} {...p}>
      <path d="M21 12a9 9 0 1 1-2.6-6.4" />
      <path d="M21 3v6h-6" />
    </svg>
  )
}

export function IconSpark(p: IconProps) {
  return (
    <svg {...base} {...p}>
      <path d="M12 3l1.8 5.2L19 10l-5.2 1.8L12 17l-1.8-5.2L5 10l5.2-1.8L12 3Z" />
      <path d="M19 16l.8 2.2L22 19l-2.2.8L19 22l-.8-2.2L16 19l2.2-.8L19 16Z" />
    </svg>
  )
}

export function IconClock(p: IconProps) {
  return (
    <svg {...base} {...p}>
      <circle cx="12" cy="12" r="9" />
      <path d="M12 7v5l3 2" />
    </svg>
  )
}

export function IconLayers(p: IconProps) {
  return (
    <svg {...base} {...p}>
      <path d="m12 3 9 5-9 5-9-5 9-5Z" />
      <path d="m3 12 9 5 9-5M3 16l9 5 9-5" />
    </svg>
  )
}

export function IconActivity(p: IconProps) {
  return (
    <svg {...base} {...p}>
      <path d="M3 12h4l3-8 4 16 3-8h4" />
    </svg>
  )
}

export function IconArrowRight(p: IconProps) {
  return (
    <svg {...base} {...p}>
      <path d="M5 12h14M13 6l6 6-6 6" />
    </svg>
  )
}

export function IconPanelLeft(p: IconProps) {
  return (
    <svg {...base} {...p}>
      <rect x="3" y="4" width="18" height="16" rx="2.5" />
      <path d="M9 4v16" />
    </svg>
  )
}

export function IconChevronLeft(p: IconProps) {
  return (
    <svg {...base} {...p}>
      <path d="m15 6-6 6 6 6" />
    </svg>
  )
}

export function IconCheck(p: IconProps) {
  return (
    <svg {...base} {...p} strokeWidth={2.4}>
      <path d="m5 12 4.5 4.5L19 7" />
    </svg>
  )
}

export function IconPanelRight(p: IconProps) {
  return (
    <svg {...base} {...p}>
      <rect x="3" y="4" width="18" height="16" rx="2.5" />
      <path d="M15 4v16" />
    </svg>
  )
}

export function IconCode(p: IconProps) {
  return (
    <svg {...base} {...p}>
      <path d="m8 8-4 4 4 4M16 8l4 4-4 4M14 5l-4 14" />
    </svg>
  )
}

export function IconCopy(p: IconProps) {
  return (
    <svg {...base} {...p}>
      <rect x="9" y="9" width="11" height="11" rx="2" />
      <path d="M5 15V6a2 2 0 0 1 2-2h9" />
    </svg>
  )
}

export function IconArrowDown(p: IconProps) {
  return (
    <svg {...base} {...p}>
      <path d="M12 5v14M6 13l6 6 6-6" />
    </svg>
  )
}

export function IconUpload(p: IconProps) {
  return (
    <svg {...base} {...p}>
      <path d="M12 16V4M6 10l6-6 6 6" />
      <path d="M4 20h16" />
    </svg>
  )
}

export function IconKey(p: IconProps) {
  return (
    <svg {...base} {...p}>
      <circle cx="8" cy="15" r="4" />
      <path d="M10.8 12.2 20 3M15 8l3 3M17 6l3 3" />
    </svg>
  )
}

export function IconSliders(p: IconProps) {
  return (
    <svg {...base} {...p}>
      <path d="M4 6h10M18 6h2M4 12h4M12 12h8M4 18h12M20 18h0" />
      <circle cx="16" cy="6" r="2" />
      <circle cx="10" cy="12" r="2" />
      <circle cx="18" cy="18" r="2" />
    </svg>
  )
}

export function IconEdit(p: IconProps) {
  return (
    <svg {...base} {...p}>
      <path d="M12 20h9" />
      <path d="M16.5 3.5a2.12 2.12 0 0 1 3 3L7 19l-4 1 1-4Z" />
    </svg>
  )
}

export function IconX(p: IconProps) {
  return (
    <svg {...base} {...p}>
      <path d="M18 6 6 18M6 6l12 12" />
    </svg>
  )
}

export function IconInfo(p: IconProps) {
  return (
    <svg {...base} {...p}>
      <circle cx="12" cy="12" r="9" />
      <path d="M12 11v5M12 8h.01" />
    </svg>
  )
}
