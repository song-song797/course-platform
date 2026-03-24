export function parseDateTime(value) {
  if (!value) {
    return null
  }

  const normalized = String(value).trim().replace(' ', 'T')
  const parsed = new Date(normalized)
  return Number.isNaN(parsed.getTime()) ? null : parsed
}

export function getSubmissionCloseAt(target) {
  return target?.submissionCloseAt || target?.deadline || ''
}

export function hasLateWindow(target) {
  const closeAt = getSubmissionCloseAt(target)
  return Boolean(target?.deadline && closeAt && target.deadline !== closeAt)
}

export function isInLateWindow(target, now = new Date()) {
  const deadline = parseDateTime(target?.deadline)
  const closeAt = parseDateTime(getSubmissionCloseAt(target))
  return Boolean(deadline && closeAt && now > deadline && now <= closeAt)
}

export function isSubmissionClosed(target, now = new Date()) {
  const closeAt = parseDateTime(getSubmissionCloseAt(target))
  return Boolean(closeAt && now > closeAt)
}

export function submissionTimeChips(target) {
  const closeAt = getSubmissionCloseAt(target)

  if (!closeAt) {
    return ['未设置提交关闭时间']
  }

  if (!target?.deadline) {
    return [`提交关闭 ${closeAt}`]
  }

  if (hasLateWindow(target)) {
    return [`正常截止 ${target.deadline}`, `迟交截止 ${closeAt}`]
  }

  return [`截止 ${closeAt}`]
}

export function submissionTimeSummary(target) {
  return submissionTimeChips(target).join(' · ')
}

const ineligibleReasonLabels = {
  SELF: '自己的项目',
  ALREADY_EVALUATED: '已完成评分',
  BLACKLISTED: '已回避该项目',
  REVIEW_CLOSED: '当前不在互评阶段',
}

export function ineligibleReasonLabel(reason) {
  return ineligibleReasonLabels[reason] || '当前不可评价'
}

export function ineligibleReasonTagType(reason) {
  if (reason === 'ALREADY_EVALUATED') {
    return 'success'
  }
  if (reason === 'REVIEW_CLOSED') {
    return 'warning'
  }
  return 'info'
}
