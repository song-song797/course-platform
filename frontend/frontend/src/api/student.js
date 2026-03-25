import http from './http'

export const getStudentHome = () => http.get('/student/home')
export const getStudentCourses = (params) => http.get('/student/courses', { params })
export const getStudentAssignment = (assignmentId) => http.get(`/student/assignments/${assignmentId}`)
export const getMySubmission = (assignmentId) => http.get(`/student/assignments/${assignmentId}/my-submission`)
export const submitProject = (assignmentId, payload) => http.post(`/student/assignments/${assignmentId}/submit`, payload)
export const updateProject = (submissionId, payload) => http.put(`/student/submissions/${submissionId}`, payload)
export const getProjects = (assignmentId, params) => http.get(`/student/assignments/${assignmentId}/projects`, { params })
export const createEvaluation = (submissionId, payload) => http.post(`/student/projects/${submissionId}/evaluations`, payload)
export const getDashboard = (assignmentId) => http.get(`/student/assignments/${assignmentId}/my-dashboard`)
export const getLeaderboard = (assignmentId, params) => http.get(`/student/assignments/${assignmentId}/leaderboard`, { params })
