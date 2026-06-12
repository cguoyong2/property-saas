import { http } from './http'

export interface LoginRequest {
  username: string
  password: string
  captchaCode?: string
}

export function login(data: LoginRequest) {
  return http.post('/auth/login', data)
}

export function fetchMenus() {
  return http.get('/auth/menus')
}
