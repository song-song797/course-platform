import http from './http'

export const getAdminCourses = (params) => http.get('/admin/courses', { params })
export const createCourse = (payload) => http.post('/admin/courses', payload)
export const getCourseMembers = (courseId) => http.get(`/admin/courses/${courseId}/members`)
export const addCourseMember = (courseId, payload) => http.post(`/admin/courses/${courseId}/members`, payload)
export const removeCourseMember = (courseId, userId) => http.delete(`/admin/courses/${courseId}/members/${userId}`)
export const importUsers = (formData) => http.post('/admin/users/import', formData, {
  headers: { 'Content-Type': 'multipart/form-data' },
})
