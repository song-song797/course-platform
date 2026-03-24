import http from './http'

export const uploadFile = (formData) =>
  http.post('/files/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
