const DEFAULT_CHART_THEME = Object.freeze({
  tooltipBackground: '#29244f',
  tooltipText: '#f8fbff',
  axisText: '#66769a',
  axisMuted: '#8994b3',
  gridLine: '#d9e1f8',
  shadow: 'rgba(142, 141, 243, 0.08)',
  primary: '#847cf0',
  secondary: '#98b7f6',
  tertiary: '#a6e4dd',
  mutedBar: '#d6dcfb',
  primaryArea: 'rgba(132, 124, 240, 0.24)',
  radarSplitA: 'rgba(132, 124, 240, 0.04)',
  radarSplitB: 'rgba(166, 228, 221, 0.14)',
  labelStrong: '#22304c',
})

function readToken(styles, name, fallback) {
  const value = styles.getPropertyValue(name).trim()
  return value || fallback
}

export function readChartTheme(element) {
  if (typeof window === 'undefined' || !element) {
    return DEFAULT_CHART_THEME
  }

  const styles = window.getComputedStyle(element)
  return {
    tooltipBackground: readToken(styles, '--detail-tooltip-bg', DEFAULT_CHART_THEME.tooltipBackground),
    tooltipText: readToken(styles, '--detail-tooltip-text', DEFAULT_CHART_THEME.tooltipText),
    axisText: readToken(styles, '--detail-chart-axis', DEFAULT_CHART_THEME.axisText),
    axisMuted: readToken(styles, '--detail-chart-axis-muted', DEFAULT_CHART_THEME.axisMuted),
    gridLine: readToken(styles, '--detail-chart-grid', DEFAULT_CHART_THEME.gridLine),
    shadow: readToken(styles, '--detail-chart-shadow', DEFAULT_CHART_THEME.shadow),
    primary: readToken(styles, '--detail-chart-primary', DEFAULT_CHART_THEME.primary),
    secondary: readToken(styles, '--detail-chart-secondary', DEFAULT_CHART_THEME.secondary),
    tertiary: readToken(styles, '--detail-chart-tertiary', DEFAULT_CHART_THEME.tertiary),
    mutedBar: readToken(styles, '--detail-chart-muted', DEFAULT_CHART_THEME.mutedBar),
    primaryArea: readToken(styles, '--detail-chart-primary-area', DEFAULT_CHART_THEME.primaryArea),
    radarSplitA: readToken(styles, '--detail-chart-radar-surface-a', DEFAULT_CHART_THEME.radarSplitA),
    radarSplitB: readToken(styles, '--detail-chart-radar-surface-b', DEFAULT_CHART_THEME.radarSplitB),
    labelStrong: readToken(styles, '--detail-chart-label', DEFAULT_CHART_THEME.labelStrong),
  }
}
