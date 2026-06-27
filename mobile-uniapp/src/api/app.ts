import { API_BASE_URL, authHeaders, request } from './request'

export interface PageResult<T> {
  records: T[]
  total: number
  pageNo: number
  pageSize: number
}

export function wxLogin(data: Record<string, unknown>) {
  return request<Record<string, unknown>>({
    url: '/app/auth/wx-login',
    method: 'POST',
    data,
  })
}

export function fetchHome() {
  return request<Record<string, unknown>>({ url: '/app/home', method: 'GET' })
}

export function fetchMine() {
  return request<Record<string, unknown>>({ url: '/app/mine', method: 'GET' })
}

export function fetchHouses() {
  return request<PageResult<Record<string, unknown>>>({ url: '/app/houses', method: 'GET' })
}

export function fetchBindProjects() {
  return request<PageResult<Record<string, unknown>>>({ url: '/app/bind-options/projects', method: 'GET' })
}

export function fetchBindBuildings(projectId: string | number) {
  return request<PageResult<Record<string, unknown>>>({
    url: '/app/bind-options/buildings',
    method: 'GET',
    data: { projectId },
  })
}

export function fetchBindUnits(projectId: string | number, buildingId: string | number) {
  return request<PageResult<Record<string, unknown>>>({
    url: '/app/bind-options/units',
    method: 'GET',
    data: { projectId, buildingId },
  })
}

export function fetchBindHouses(projectId: string | number, buildingId: string | number, unitId: string | number) {
  return request<PageResult<Record<string, unknown>>>({
    url: '/app/bind-options/houses',
    method: 'GET',
    data: { projectId, buildingId, unitId },
  })
}

export function applyHouseBinding(data: Record<string, unknown>) {
  return request<Record<string, unknown>>({
    url: '/app/house-bindings',
    method: 'POST',
    data,
  })
}

export function unbindHouse(bindId: string | number, reason: string) {
  return request<void>({
    url: `/app/house-bindings/${bindId}/unbind`,
    method: 'PUT',
    data: { reason },
  })
}

export function fetchBills(params: Record<string, unknown>) {
  return request<PageResult<Record<string, unknown>>>({
    url: '/app/bills',
    method: 'GET',
    data: params,
  })
}

export function fetchBillDetail(billId: string | number) {
  return request<Record<string, unknown>>({
    url: `/app/bills/${billId}`,
    method: 'GET',
  })
}

export function createPayOrder(data: Record<string, unknown>) {
  return request<Record<string, unknown>>({
    url: '/app/pay/orders',
    method: 'POST',
    data,
  })
}

export function confirmDemoPayOrder(orderNo: string) {
  return request<Record<string, unknown>>({
    url: `/app/pay/orders/${orderNo}/demo-confirm`,
    method: 'POST',
  })
}

export function fetchPayOrders(params: Record<string, unknown>) {
  return request<PageResult<Record<string, unknown>>>({
    url: '/app/pay/orders',
    method: 'GET',
    data: params,
  })
}

export function fetchPayOrderDetail(orderNo: string) {
  return request<Record<string, unknown>>({
    url: `/app/pay/orders/${orderNo}`,
    method: 'GET',
  })
}

export function fetchPrepayments(params: Record<string, unknown>) {
  return request<PageResult<Record<string, unknown>>>({
    url: '/app/prepayments',
    method: 'GET',
    data: params,
  })
}

export function fetchPrepaymentSummary() {
  return request<Record<string, unknown>>({
    url: '/app/prepayments/summary',
    method: 'GET',
  })
}

export function fetchNotices(params: Record<string, unknown>) {
  return request<PageResult<Record<string, unknown>>>({
    url: '/app/notices',
    method: 'GET',
    data: params,
  })
}

export function fetchPublicNotices(params: Record<string, unknown>) {
  return request<PageResult<Record<string, unknown>>>({
    url: '/public/notices',
    method: 'GET',
    data: params,
  })
}

export function fetchWorkOrders(params: Record<string, unknown>) {
  return request<PageResult<Record<string, unknown>>>({
    url: '/app/workorders',
    method: 'GET',
    data: params,
  })
}

export function fetchWorkOrderDetail(workOrderId: string | number) {
  return request<Record<string, unknown>>({
    url: `/app/workorders/${workOrderId}`,
    method: 'GET',
  })
}

export function createWorkOrder(data: Record<string, unknown>) {
  return request<Record<string, unknown>>({
    url: '/app/workorders',
    method: 'POST',
    data,
  })
}

export function evaluateWorkOrder(workOrderId: string | number, data: Record<string, unknown>) {
  return request<void>({
    url: `/app/workorders/${workOrderId}/evaluate`,
    method: 'POST',
    data,
  })
}

export function createComplaint(data: Record<string, unknown>) {
  return request<Record<string, unknown>>({
    url: '/app/complaints',
    method: 'POST',
    data,
  })
}

export function uploadAppFile(filePath: string, data: { projectId: number | string, moduleCode: string }) {
  return new Promise<Record<string, unknown>>((resolve, reject) => {
    uni.uploadFile({
      url: `${API_BASE_URL}/app/files`,
      filePath,
      name: 'file',
      formData: {
        projectId: String(data.projectId),
        moduleCode: data.moduleCode,
      },
      header: authHeaders(),
      success: (response) => {
        if (response.statusCode === 401) {
          uni.removeStorageSync('member_token')
          reject(new Error('请先绑定房屋'))
          return
        }
        try {
          const body = JSON.parse(response.data) as { code: number, message: string, data: Record<string, unknown> }
          if (body.code !== 0) {
            reject(new Error(body.message || '上传失败'))
            return
          }
          resolve(body.data)
        } catch {
          reject(new Error('上传响应解析失败'))
        }
      },
      fail: reject,
    })
  })
}

export function createVisitor(data: Record<string, unknown>) {
  return request<Record<string, unknown>>({
    url: '/app/visitors',
    method: 'POST',
    data,
  })
}

export function fetchVehicles(params: Record<string, unknown>) {
  return request<PageResult<Record<string, unknown>>>({
    url: '/app/vehicles',
    method: 'GET',
    data: params,
  })
}

export function fetchLeaseContracts(params: Record<string, unknown>) {
  return request<PageResult<Record<string, unknown>>>({
    url: '/app/lease/contracts',
    method: 'GET',
    data: params,
  })
}
