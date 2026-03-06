import request from '@/request'

/**
 * 保存一条 AI 对话笔记
 */
export function saveNote(data: API.SaveNoteRequest) {
  return request.post<API.BaseResponse<number>>('/note/save', data)
}

/**
 * 分页获取当前用户笔记列表（notebookId 可选，传入时只返回该笔记本相关笔记）
 */
export function listMyNotes(params: { pageNum?: number; pageSize?: number; notebookId?: string | number }) {
  return request.get<API.BaseResponsePageSavedNoteVO>('/note/list', { params })
}

/**
 * 修改笔记
 */
export function updateNote(id: number | string, data: API.UpdateNoteRequest) {
  return request.put<API.BaseResponse<boolean>>(`/note/${id}`, data)
}

/**
 * 删除笔记
 */
export function deleteNote(id: number | string) {
  return request.delete<API.BaseResponse<boolean>>(`/note/${id}`)
}
