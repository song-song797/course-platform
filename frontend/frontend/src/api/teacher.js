import http from './http'

export const getTeacherCourses = (params) => http.get('/teacher/courses', { params })
export const createTeacherCourse = (payload) => http.post('/teacher/courses', payload)
export const createTeacherAssignment = (courseId, payload) => http.post(`/teacher/courses/${courseId}/assignments`, payload)
export const getTeacherAssignment = (assignmentId) => http.get(`/teacher/assignments/${assignmentId}`)
export const getTeacherGroups = (assignmentId) => http.get(`/teacher/assignments/${assignmentId}/groups`)
export const createTeacherGroup = (assignmentId, payload) => http.post(`/teacher/assignments/${assignmentId}/groups`, payload)
export const updateTeacherGroup = (assignmentId, groupId, payload) => http.put(`/teacher/assignments/${assignmentId}/groups/${groupId}`, payload)
export const deleteTeacherGroup = (assignmentId, groupId) => http.delete(`/teacher/assignments/${assignmentId}/groups/${groupId}`)
export const updateRubric = (assignmentId, payload) => http.put(`/teacher/assignments/${assignmentId}/rubric`, payload)
export const getTeacherSubmissions = (assignmentId, params) => http.get(`/teacher/assignments/${assignmentId}/submissions`, { params })
export const createTeacherScore = (submissionId, payload) => http.post(`/teacher/submissions/${submissionId}/scores`, payload)
export const getTeacherEvaluations = (assignmentId, params) => http.get(`/teacher/assignments/${assignmentId}/evaluations`, { params })
export const reviewEvaluation = (evaluationId, excluded = true) => http.patch(`/teacher/evaluations/${evaluationId}/review`, null, { params: { excluded } })
export const addBlacklist = (assignmentId, evaluatorUserId, targetSubmissionId) =>
  http.post(`/teacher/assignments/${assignmentId}/blacklist`, null, { params: { evaluatorUserId, targetSubmissionId } })
export const removeBlacklist = (assignmentId, evaluatorUserId, targetSubmissionId) =>
  http.delete(`/teacher/assignments/${assignmentId}/blacklist`, { params: { evaluatorUserId, targetSubmissionId } })
export const getTeacherStats = (assignmentId) => http.get(`/teacher/assignments/${assignmentId}/stats`)
