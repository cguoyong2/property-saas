import { http } from './http'

export interface PageQuery {
  pageNo?: number
  pageSize?: number
  [key: string]: string | number | boolean | undefined
}

export function fetchPage(path: string, params: PageQuery = {}) {
  return http.get(path, { params })
}

export function fetchDetail(path: string, id: string | number, idName: string) {
  return http.get(path.replace(`:${idName}`, String(id)))
}

export function createRecord(path: string, data: Record<string, unknown>) {
  return http.post(path, data)
}

export function updateRecord(path: string, id: string | number, idName: string, data: Record<string, unknown>) {
  return http.put(path.replace(`:${idName}`, String(id)), data)
}

export function postAction(path: string, data?: Record<string, unknown>) {
  return http.post(path, data ?? {})
}

export function putAction(path: string, data?: Record<string, unknown>) {
  return http.put(path, data ?? {})
}

export function downloadFile(path: string) {
  return http.get(path, { responseType: 'blob' })
}
