import http from './http'

export const login = (payload) => http.post('/auth/login', payload)
export const getMe = () => http.get('/auth/me')
export const changePassword = (payload) => http.post('/auth/change-password', payload)
export const logout = () => http.post('/auth/logout')
