export interface User {
  id: string
  email: string
  fullName: string
  role: 'USER' | 'ADMIN'
  createdAt: string
}

export interface AuthResponse {
  accessToken: string
  tokenType: string
  expiresInMinutes: number
  user: User
}

export interface ShortUrl {
  id: string
  shortCode: string
  shortUrl: string
  originalUrl: string
  title: string | null
  qrCodeDataUrl: string
  customAlias: boolean
  active: boolean
  clickCount: number
  expiresAt: string | null
  createdAt: string
  lastAccessedAt: string | null
}

export interface PagedResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}

export interface DailyClickPoint {
  day: string
  count: number
}

export interface MetricBreakdown {
  label: string
  count: number
}

export interface UrlAnalytics {
  shortCode: string
  totalClicks: number
  dailyClicks: DailyClickPoint[]
  browserBreakdown: MetricBreakdown[]
  operatingSystemBreakdown: MetricBreakdown[]
  deviceBreakdown: MetricBreakdown[]
  countryBreakdown: MetricBreakdown[]
}

export interface DashboardSummary {
  totalUrls: number
  activeUrls: number
  expiredUrls: number
  totalClicks: number
  clickTrend: DailyClickPoint[]
  topUrls: MetricBreakdown[]
}
