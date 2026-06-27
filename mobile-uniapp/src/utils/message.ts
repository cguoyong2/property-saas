export type MessageCategory = 'all' | 'payment' | 'refund' | 'workorder' | 'house' | 'notice'

export interface MessageCategoryOption {
  key: MessageCategory
  label: string
}

export interface MessageAction {
  text: string
  url: string
}

export const messageCategories: MessageCategoryOption[] = [
  { key: 'all', label: '全部' },
  { key: 'payment', label: '缴费' },
  { key: 'refund', label: '退款' },
  { key: 'workorder', label: '工单' },
  { key: 'house', label: '房屋' },
  { key: 'notice', label: '公告' },
]

export function messageCategory(message: Record<string, unknown>): MessageCategory {
  const code = String(message.templateCode || message.noticeType || '').toUpperCase()
  if (code.includes('REFUND')) return 'refund'
  if (code.includes('BILL') || code.includes('PAYMENT') || code.includes('PAY')) return 'payment'
  if (code.includes('WORKORDER')) return 'workorder'
  if (code.includes('HOUSE') || code.includes('BINDING')) return 'house'
  return 'notice'
}

export function messageCategoryText(message: Record<string, unknown>) {
  const map: Record<MessageCategory, string> = {
    all: '消息',
    payment: '缴费',
    refund: '退款',
    workorder: '工单',
    house: '房屋',
    notice: message.noticeType ? '公告' : '通知',
  }
  return map[messageCategory(message)]
}

export function messageTemplateText(value: unknown) {
  const map: Record<string, string> = {
    HOUSE_BINDING_AUDIT: '房屋绑定审核',
    BILL_CREATED: '账单已生成',
    BILL_DUE: '账单催缴提醒',
    PAYMENT_SUCCESS: '缴费成功',
    REFUND_APPLY: '退款申请',
    REFUND_AUDIT: '退款审核',
    REFUND_SUCCESS: '退款完成',
    WORKORDER_STATUS: '工单状态更新',
    WORKORDER_DISPATCH: '工单进度',
  }
  return map[String(value || '').toUpperCase()] || '站内通知'
}

export function messageAction(message: Record<string, unknown>): MessageAction | null {
  const category = messageCategory(message)
  if (category === 'payment') {
    return { text: '查看账单', url: '/pages/bill/list' }
  }
  if (category === 'refund') {
    return { text: '查看缴费记录', url: '/pages/payment/history' }
  }
  if (category === 'workorder') {
    return { text: '查看工单', url: '/pages/workorder/list' }
  }
  if (category === 'house') {
    return { text: '查看房屋', url: '/pages/house/list' }
  }
  return null
}

export function formatMessageTime(value: unknown) {
  if (!value) return ''
  return String(value).replace('T', ' ').slice(0, 16)
}

export function shortMessageContent(value: unknown, maxLength = 72) {
  const text = String(value || '').replace(/\s+/g, ' ').trim()
  if (text.length <= maxLength) return text
  return `${text.slice(0, maxLength)}...`
}
