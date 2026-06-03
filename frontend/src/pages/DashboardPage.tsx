import { startTransition, useDeferredValue, useEffect, useState } from 'react'
import {
  Alert,
  Avatar,
  Box,
  Button,
  Chip,
  CircularProgress,
  Container,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Divider,
  FormControl,
  Grid,
  IconButton,
  InputAdornment,
  InputLabel,
  MenuItem,
  Paper,
  Select,
  Stack,
  Tab,
  Tabs,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TablePagination,
  TableRow,
  TextField,
  Typography,
} from '@mui/material'
import AddLinkIcon from '@mui/icons-material/AddLink'
import AnalyticsIcon from '@mui/icons-material/Analytics'
import AutorenewIcon from '@mui/icons-material/Autorenew'
import ContentCopyIcon from '@mui/icons-material/ContentCopy'
import ExitToAppIcon from '@mui/icons-material/ExitToApp'
import LinkOffIcon from '@mui/icons-material/LinkOff'
import QrCode2Icon from '@mui/icons-material/QrCode2'
import SearchIcon from '@mui/icons-material/Search'
import TuneIcon from '@mui/icons-material/Tune'
import {
  Area,
  AreaChart,
  Bar,
  BarChart,
  CartesianGrid,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { api } from '../api'
import { useAuth } from '../state/AuthContext'
import type {
  DashboardSummary,
  PagedResponse,
  ShortUrl,
  UrlAnalytics,
} from '../types'

type CreateMode = 'single' | 'bulk'

const metricMeta = [
  { key: 'totalUrls', label: 'Total URLs', tone: '#0F766E' },
  { key: 'activeUrls', label: 'Active URLs', tone: '#2563EB' },
  { key: 'inactiveUrls', label: 'Inactive URLs', tone: '#D97706' },
  { key: 'expiredUrls', label: 'Expired URLs', tone: '#B45309' },
  { key: 'totalClicks', label: 'Total Clicks', tone: '#7C3AED' },
] as const

export default function DashboardPage() {
  const { user, logout } = useAuth()
  const [dashboard, setDashboard] = useState<DashboardSummary | null>(null)
  const [urls, setUrls] = useState<PagedResponse<ShortUrl> | null>(null)
  const [analytics, setAnalytics] = useState<UrlAnalytics | null>(null)
  const [selectedUrl, setSelectedUrl] = useState<ShortUrl | null>(null)
  const [qrCodeUrl, setQrCodeUrl] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [savePending, setSavePending] = useState(false)
  const [analyticsLoading, setAnalyticsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [status, setStatus] = useState('ALL')
  const [sortBy, setSortBy] = useState('createdAt')
  const [direction, setDirection] = useState('DESC')
  const [page, setPage] = useState(0)
  const [search, setSearch] = useState('')
  const deferredSearch = useDeferredValue(search)
  const [createMode, setCreateMode] = useState<CreateMode>('single')
  const [singleForm, setSingleForm] = useState({
    title: '',
    originalUrl: '',
    customAlias: '',
    expiresAt: '',
  })
  const [bulkPayload, setBulkPayload] = useState('')
  const [editForm, setEditForm] = useState({
    title: '',
    expiresAt: '',
    active: true,
  })

  useEffect(() => {
    void refreshAll()
  }, [page, status, sortBy, direction, deferredSearch])

  async function refreshAll() {
    setLoading(true)
    setError(null)
    try {
      const [dashboardResponse, urlsResponse] = await Promise.all([
        api.get<DashboardSummary>('/api/dashboard/summary'),
        api.get<PagedResponse<ShortUrl>>('/api/urls', {
          params: {
            page,
            size: 8,
            search: deferredSearch || undefined,
            status,
            sortBy,
            direction,
          },
        }),
      ])
      setDashboard(dashboardResponse.data)
      setUrls(urlsResponse.data)
    } catch (err: any) {
      setError(err?.response?.data?.message ?? 'Unable to load the dashboard right now.')
    } finally {
      setLoading(false)
    }
  }

  async function handleCreate() {
    setSavePending(true)
    setError(null)
    try {
      if (createMode === 'single') {
        await api.post('/api/urls', {
          title: singleForm.title || null,
          originalUrl: singleForm.originalUrl,
          customAlias: singleForm.customAlias || null,
          expiresAt: singleForm.expiresAt || null,
        })
        setSingleForm({ title: '', originalUrl: '', customAlias: '', expiresAt: '' })
      } else {
        const urlsPayload = bulkPayload
          .split('\n')
          .map((line) => line.trim())
          .filter(Boolean)
          .map((line) => {
            const [title, originalUrl] = line.includes('|') ? line.split('|', 2) : ['', line]
            return { title: title || null, originalUrl: originalUrl.trim() }
          })
        await api.post('/api/urls/bulk', { urls: urlsPayload })
        setBulkPayload('')
      }
      await refreshAll()
    } catch (err: any) {
      setError(err?.response?.data?.message ?? 'Unable to create short URLs.')
    } finally {
      setSavePending(false)
    }
  }

  async function handleDeactivate(id: string) {
    await api.patch(`/api/urls/${id}/deactivate`)
    await refreshAll()
  }

  async function handleActivate(id: string) {
    await api.patch(`/api/urls/${id}/activate`)
    await refreshAll()
  }

  async function openAnalytics(url: ShortUrl) {
    setSelectedUrl(url)
    setAnalyticsLoading(true)
    try {
      const { data } = await api.get<UrlAnalytics>(`/api/urls/${url.id}/analytics`)
      setAnalytics(data)
    } finally {
      setAnalyticsLoading(false)
    }
  }

  function openEdit(url: ShortUrl) {
    setSelectedUrl(url)
    setEditForm({
      title: url.title ?? '',
      expiresAt: url.expiresAt ? url.expiresAt.slice(0, 16) : '',
      active: url.active,
    })
  }

  async function handleUpdate() {
    if (!selectedUrl) {
      return
    }
    setSavePending(true)
    try {
      await api.put(`/api/urls/${selectedUrl.id}`, {
        title: editForm.title || null,
        expiresAt: editForm.expiresAt ? new Date(editForm.expiresAt).toISOString() : null,
        active: editForm.active,
      })
      setSelectedUrl(null)
      await refreshAll()
    } finally {
      setSavePending(false)
    }
  }

  async function copyToClipboard(value: string) {
    await navigator.clipboard.writeText(value)
  }

  if (loading && !dashboard && !urls) {
    return (
      <Box sx={{ minHeight: '100vh', display: 'grid', placeItems: 'center' }}>
        <CircularProgress />
      </Box>
    )
  }

  return (
    <Box sx={{ minHeight: '100vh', py: 4 }}>
      <Container maxWidth="xl">
        <Stack spacing={3}>
          <Paper sx={{ p: 3 }}>
            <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} justifyContent="space-between" alignItems={{ xs: 'flex-start', md: 'center' }}>
              <Stack spacing={1}>
                <Typography variant="overline" sx={{ letterSpacing: 2 }}>
                  URL OPERATIONS HUB
                </Typography>
                <Typography variant="h3">Welcome back, {user?.fullName}</Typography>
                <Typography color="text.secondary">
                  Manage short links, inspect click trends, and demonstrate distributed-system design decisions from a single dashboard.
                </Typography>
              </Stack>
              <Stack direction="row" spacing={2} alignItems="center">
                <Avatar sx={{ bgcolor: 'primary.main', width: 48, height: 48 }}>
                  {user?.fullName?.[0] ?? 'U'}
                </Avatar>
                <Button variant="outlined" endIcon={<ExitToAppIcon />} onClick={logout}>
                  Logout
                </Button>
              </Stack>
            </Stack>
          </Paper>

          {error && <Alert severity="error">{error}</Alert>}

          <Grid container spacing={2}>
            {metricMeta.map((item) => (
              <Grid key={item.key} size={{ xs: 12, sm: 6, lg: 3 }}>
                <Paper className="metric-card" sx={{ p: 3 }}>
                  <Typography color="text.secondary">{item.label}</Typography>
                  <Typography variant="h3" sx={{ color: item.tone, mt: 1 }}>
                    {dashboard?.[item.key].toLocaleString()}
                  </Typography>
                </Paper>
              </Grid>
            ))}
          </Grid>

          <Grid container spacing={3}>
            <Grid size={{ xs: 12, lg: 5 }}>
              <Paper sx={{ p: 3, height: '100%' }}>
                <Stack spacing={2}>
                  <Stack direction="row" justifyContent="space-between" alignItems="center">
                    <Typography variant="h5">Create short URLs</Typography>
                    <Chip label="Base62 + Redis cache" color="primary" variant="outlined" />
                  </Stack>
                  <Tabs value={createMode} onChange={(_, value) => setCreateMode(value)}>
                    <Tab value="single" label="Single URL" />
                    <Tab value="bulk" label="Bulk upload" />
                  </Tabs>
                  {createMode === 'single' ? (
                    <Stack spacing={2}>
                      <TextField
                        label="Original URL"
                        value={singleForm.originalUrl}
                        onChange={(event) => setSingleForm((current) => ({ ...current, originalUrl: event.target.value }))}
                        placeholder="https://example.com/very/long/path"
                        fullWidth
                      />
                      <TextField
                        label="Title"
                        value={singleForm.title}
                        onChange={(event) => setSingleForm((current) => ({ ...current, title: event.target.value }))}
                      />
                      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
                        <TextField
                          label="Custom alias"
                          value={singleForm.customAlias}
                          onChange={(event) => setSingleForm((current) => ({ ...current, customAlias: event.target.value }))}
                          fullWidth
                        />
                        <TextField
                          label="Expires at"
                          type="datetime-local"
                          value={singleForm.expiresAt}
                          onChange={(event) => setSingleForm((current) => ({ ...current, expiresAt: event.target.value }))}
                          InputLabelProps={{ shrink: true }}
                          fullWidth
                        />
                      </Stack>
                    </Stack>
                  ) : (
                    <TextField
                      multiline
                      minRows={8}
                      label="Bulk URLs"
                      placeholder="Landing page|https://company.com/landing&#10;https://company.com/pricing"
                      value={bulkPayload}
                      onChange={(event) => setBulkPayload(event.target.value)}
                      helperText="Use one URL per line. Optional format: title|url"
                    />
                  )}
                  <Button variant="contained" size="large" startIcon={<AddLinkIcon />} onClick={handleCreate} disabled={savePending}>
                    {savePending ? 'Saving...' : createMode === 'single' ? 'Create short URL' : 'Create batch'}
                  </Button>
                </Stack>
              </Paper>
            </Grid>

            <Grid size={{ xs: 12, lg: 7 }}>
              <Paper sx={{ p: 3 }}>
                <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} justifyContent="space-between" alignItems={{ xs: 'stretch', md: 'center' }} sx={{ mb: 3 }}>
                  <Typography variant="h5">Traffic overview</Typography>
                  <Chip icon={<TuneIcon />} label="Interview-ready analytics" color="secondary" variant="outlined" />
                </Stack>
                <Box sx={{ height: 280 }}>
                  <ResponsiveContainer width="100%" height="100%">
                    <AreaChart data={dashboard?.clickTrend ?? []}>
                      <defs>
                        <linearGradient id="traffic" x1="0" y1="0" x2="0" y2="1">
                          <stop offset="5%" stopColor="#0F766E" stopOpacity={0.85} />
                          <stop offset="95%" stopColor="#0F766E" stopOpacity={0.05} />
                        </linearGradient>
                      </defs>
                      <CartesianGrid strokeDasharray="3 3" stroke="#D7E1DC" />
                      <XAxis dataKey="day" />
                      <YAxis />
                      <Tooltip />
                      <Area type="monotone" dataKey="count" stroke="#0F766E" fill="url(#traffic)" strokeWidth={3} />
                    </AreaChart>
                  </ResponsiveContainer>
                </Box>
              </Paper>
            </Grid>
          </Grid>

          <Paper sx={{ p: 3 }}>
            <Stack direction={{ xs: 'column', lg: 'row' }} spacing={2} justifyContent="space-between" alignItems={{ xs: 'stretch', lg: 'center' }} sx={{ mb: 3 }}>
              <Typography variant="h5">Managed URLs</Typography>
              <Stack direction={{ xs: 'column', md: 'row' }} spacing={2}>
                <TextField
                  value={search}
                  onChange={(event) => {
                    startTransition(() => {
                      setPage(0)
                      setSearch(event.target.value)
                    })
                  }}
                  placeholder="Search title, code, or original URL"
                  InputProps={{
                    startAdornment: (
                      <InputAdornment position="start">
                        <SearchIcon />
                      </InputAdornment>
                    ),
                  }}
                  sx={{ minWidth: 280 }}
                />
                <FormControl sx={{ minWidth: 150 }}>
                  <InputLabel>Status</InputLabel>
                  <Select label="Status" value={status} onChange={(event) => setStatus(event.target.value)}>
                    <MenuItem value="ALL">All</MenuItem>
                    <MenuItem value="ACTIVE">Active</MenuItem>
                    <MenuItem value="EXPIRED">Expired</MenuItem>
                    <MenuItem value="INACTIVE">Inactive</MenuItem>
                  </Select>
                </FormControl>
                <FormControl sx={{ minWidth: 150 }}>
                  <InputLabel>Sort by</InputLabel>
                  <Select label="Sort by" value={sortBy} onChange={(event) => setSortBy(event.target.value)}>
                    <MenuItem value="createdAt">Created</MenuItem>
                    <MenuItem value="clickCount">Clicks</MenuItem>
                    <MenuItem value="title">Title</MenuItem>
                  </Select>
                </FormControl>
                <FormControl sx={{ minWidth: 120 }}>
                  <InputLabel>Direction</InputLabel>
                  <Select label="Direction" value={direction} onChange={(event) => setDirection(event.target.value)}>
                    <MenuItem value="DESC">DESC</MenuItem>
                    <MenuItem value="ASC">ASC</MenuItem>
                  </Select>
                </FormControl>
              </Stack>
            </Stack>

            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Short Link</TableCell>
                    <TableCell>Destination</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell>Clicks</TableCell>
                    <TableCell>Created</TableCell>
                    <TableCell align="right">Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {(urls?.content ?? []).map((url) => (
                    <TableRow key={url.id} hover>
                      <TableCell>
                        <Stack spacing={0.5}>
                          <Typography fontWeight={700}>{url.shortCode}</Typography>
                          <Typography variant="body2" color="text.secondary">
                            {url.shortUrl}
                          </Typography>
                        </Stack>
                      </TableCell>
                      <TableCell sx={{ maxWidth: 320 }}>
                        <Typography noWrap title={url.originalUrl}>
                          {url.originalUrl}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <Chip
                          label={url.status}
                          color={url.status === 'ACTIVE' ? 'success' : url.status === 'INACTIVE' ? 'warning' : 'error'}
                          variant={url.status === 'ACTIVE' ? 'filled' : 'outlined'}
                        />
                      </TableCell>
                      <TableCell>{url.clickCount.toLocaleString()}</TableCell>
                      <TableCell>{new Date(url.createdAt).toLocaleDateString()}</TableCell>
                      <TableCell align="right">
                        <Stack direction="row" justifyContent="flex-end" spacing={1}>
                          <IconButton onClick={() => copyToClipboard(url.shortUrl)}>
                            <ContentCopyIcon />
                          </IconButton>
                          <IconButton onClick={() => setQrCodeUrl(url.qrCodeDataUrl)}>
                            <QrCode2Icon />
                          </IconButton>
                          <IconButton onClick={() => openAnalytics(url)}>
                            <AnalyticsIcon />
                          </IconButton>
                          <IconButton onClick={() => openEdit(url)}>
                            <TuneIcon />
                          </IconButton>
                          {url.status === 'ACTIVE' ? (
                            <IconButton onClick={() => handleDeactivate(url.id)}>
                              <LinkOffIcon />
                            </IconButton>
                          ) : url.status === 'INACTIVE' ? (
                            <IconButton onClick={() => handleActivate(url.id)}>
                              <AutorenewIcon />
                            </IconButton>
                          ) : null}
                        </Stack>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
            <TablePagination
              component="div"
              count={urls?.totalElements ?? 0}
              page={urls?.page ?? 0}
              rowsPerPage={8}
              onPageChange={(_, nextPage) => setPage(nextPage)}
              rowsPerPageOptions={[8]}
              onRowsPerPageChange={() => undefined}
            />
          </Paper>
        </Stack>
      </Container>

      <Dialog open={Boolean(analytics || analyticsLoading)} onClose={() => { setAnalytics(null); setSelectedUrl(null) }} maxWidth="lg" fullWidth>
        <DialogTitle>Analytics for {selectedUrl?.shortCode}</DialogTitle>
        <DialogContent dividers>
          {analyticsLoading ? (
            <Box sx={{ py: 6, display: 'grid', placeItems: 'center' }}>
              <CircularProgress />
            </Box>
          ) : analytics ? (
            <Stack spacing={3}>
              <Typography color="text.secondary">
                Total clicks: <strong>{analytics.totalClicks.toLocaleString()}</strong>
              </Typography>
              <Grid container spacing={2}>
                <Grid size={{ xs: 12, lg: 7 }}>
                  <Paper sx={{ p: 2 }}>
                    <Typography variant="h6" gutterBottom>Daily clicks</Typography>
                    <Box sx={{ height: 260 }}>
                      <ResponsiveContainer width="100%" height="100%">
                        <BarChart data={analytics.dailyClicks}>
                          <CartesianGrid strokeDasharray="3 3" />
                          <XAxis dataKey="day" />
                          <YAxis />
                          <Tooltip />
                          <Bar dataKey="count" fill="#0F766E" radius={[10, 10, 0, 0]} />
                        </BarChart>
                      </ResponsiveContainer>
                    </Box>
                  </Paper>
                </Grid>
                <Grid size={{ xs: 12, lg: 5 }}>
                  <Paper sx={{ p: 2 }}>
                    <Typography variant="h6" gutterBottom>Country split</Typography>
                    <Box sx={{ height: 260 }}>
                      <ResponsiveContainer width="100%" height="100%">
                        <PieChart>
                          <Pie data={analytics.countryBreakdown} dataKey="count" nameKey="label" innerRadius={55} outerRadius={90} fill="#F59E0B" />
                          <Tooltip />
                        </PieChart>
                      </ResponsiveContainer>
                    </Box>
                  </Paper>
                </Grid>
              </Grid>
              <Grid container spacing={2}>
                {[
                  { title: 'Browsers', rows: analytics.browserBreakdown },
                  { title: 'Operating Systems', rows: analytics.operatingSystemBreakdown },
                  { title: 'Devices', rows: analytics.deviceBreakdown },
                ].map((item) => (
                  <Grid key={item.title} size={{ xs: 12, md: 4 }}>
                    <Paper sx={{ p: 2 }}>
                      <Typography variant="h6" gutterBottom>{item.title}</Typography>
                      <Stack spacing={1.25}>
                        {item.rows.map((row) => (
                          <Stack key={row.label} direction="row" justifyContent="space-between">
                            <Typography color="text.secondary">{row.label}</Typography>
                            <Typography fontWeight={700}>{row.count}</Typography>
                          </Stack>
                        ))}
                      </Stack>
                    </Paper>
                  </Grid>
                ))}
              </Grid>
            </Stack>
          ) : null}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => { setAnalytics(null); setSelectedUrl(null) }}>Close</Button>
        </DialogActions>
      </Dialog>

      <Dialog open={Boolean(selectedUrl && analytics === null && !analyticsLoading)} onClose={() => setSelectedUrl(null)} maxWidth="sm" fullWidth>
        <DialogTitle>Edit URL settings</DialogTitle>
        <DialogContent dividers>
          <Stack spacing={2}>
            <TextField
              label="Title"
              value={editForm.title}
              onChange={(event) => setEditForm((current) => ({ ...current, title: event.target.value }))}
            />
            <TextField
              label="Expires at"
              type="datetime-local"
              value={editForm.expiresAt}
              onChange={(event) => setEditForm((current) => ({ ...current, expiresAt: event.target.value }))}
              InputLabelProps={{ shrink: true }}
            />
            <FormControl>
              <InputLabel>Status</InputLabel>
              <Select
                label="Status"
                value={editForm.active ? 'active' : 'inactive'}
                onChange={(event) => setEditForm((current) => ({ ...current, active: event.target.value === 'active' }))}
              >
                <MenuItem value="active">Active</MenuItem>
                <MenuItem value="inactive">Inactive</MenuItem>
              </Select>
            </FormControl>
            <Divider />
            <Stack direction="row" spacing={2} alignItems="center">
              <img src={selectedUrl?.qrCodeDataUrl} width={120} height={120} alt="QR code" />
              <Box>
                <Typography variant="subtitle1">QR code</Typography>
                <Typography color="text.secondary">
                  Share the short link as a scannable asset for print campaigns, events, and mobile journeys.
                </Typography>
              </Box>
            </Stack>
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setSelectedUrl(null)}>Cancel</Button>
          <Button variant="contained" onClick={handleUpdate} disabled={savePending}>Save changes</Button>
        </DialogActions>
      </Dialog>

      <Dialog open={Boolean(qrCodeUrl)} onClose={() => setQrCodeUrl(null)}>
        <DialogTitle>QR code preview</DialogTitle>
        <DialogContent>
          {qrCodeUrl && <img src={qrCodeUrl} width={280} height={280} alt="QR code" />}
        </DialogContent>
      </Dialog>
    </Box>
  )
}
