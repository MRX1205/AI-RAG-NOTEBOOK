// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** 创建笔记本 POST /notebook/add */
export async function addNotebook(
    body: API.NotebookAddRequest,
    options?: { [key: string]: any },
) {
    return request<API.BaseResponseLong>('/notebook/add', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        data: body,
        ...(options || {}),
    })
}

/** 获取笔记本列表 GET /notebook/list */
export async function listNotebook(options?: { [key: string]: any }) {
    return request<API.BaseResponseNotebookVOList>('/notebook/list', {
        method: 'GET',
        ...(options || {}),
    })
}

/** 获取精选笔记本列表 GET /notebook/featured */
export async function getFeaturedNotebooks(options?: { [key: string]: any }) {
    return request<API.BaseResponseFeaturedNotebookVOList>('/notebook/featured', {
        method: 'GET',
        ...(options || {}),
    })
}

/** 获取笔记本详情 GET /notebook/get */
export async function getNotebookById(
    params: { id: string | number },
    options?: { [key: string]: any },
) {
    return request<API.BaseResponseNotebookVO>('/notebook/get', {
        method: 'GET',
        params: { ...params },
        ...(options || {}),
    })
}

/** 更新笔记本 POST /notebook/update */
export async function updateNotebook(
    body: API.NotebookUpdateRequest,
    options?: { [key: string]: any },
) {
    return request<API.BaseResponseBoolean>('/notebook/update', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        data: body,
        ...(options || {}),
    })
}

/** 删除笔记本 POST /notebook/delete */
export async function deleteNotebook(
    body: API.DeleteRequest,
    options?: { [key: string]: any },
) {
    return request<API.BaseResponseBoolean>('/notebook/delete', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        data: body,
        ...(options || {}),
    })
}

/** 上传笔记本封面 POST /notebook/upload/cover/{notebookId} */
export async function uploadNotebookCover(
    notebookId: string | number,
    formData: FormData,
    options?: { [key: string]: any },
) {
    return request<API.BaseResponseString>(`/notebook/upload/cover/${notebookId}`, {
        method: 'POST',
        data: formData,
        ...(options || {}),
    })
}

/** 管理员分页获取笔记本列表 GET /notebook/admin/list */
export async function adminListNotebooks(
    params: { pageNum?: number; pageSize?: number; title?: string; userName?: string },
    options?: { [key: string]: any },
) {
    return request<API.BaseResponsePageNotebookVO>('/notebook/admin/list', {
        method: 'GET',
        params: { ...params },
        ...(options || {}),
    })
}

/** 管理员修改笔记本 PUT /notebook/admin/update/{id} */
export async function adminUpdateNotebook(
    id: string | number,
    body: API.NotebookAdminUpdateRequest,
    options?: { [key: string]: any },
) {
    return request<API.BaseResponseBoolean>(`/notebook/admin/update/${id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        data: body,
        ...(options || {}),
    })
}

/** 管理员软删除笔记本 DELETE /notebook/admin/delete/{id} */
export async function adminDeleteNotebook(
    id: string | number,
    options?: { [key: string]: any },
) {
    return request<API.BaseResponseBoolean>(`/notebook/admin/delete/${id}`, {
        method: 'DELETE',
        ...(options || {}),
    })
}

/** 管理员切换精选状态 PUT /notebook/admin/featured/{id} */
export async function adminToggleFeatured(
    id: string | number,
    isFeatured: number,
    options?: { [key: string]: any },
) {
    return request<API.BaseResponseBoolean>(`/notebook/admin/featured/${id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        data: { isFeatured },
        ...(options || {}),
    })
}

/** 获取公开精选笔记本列表（无需登录）GET /public/notebooks/featured */
export async function getPublicFeaturedNotebooks(options?: { [key: string]: any }) {
    return request<API.BaseResponseFeaturedNotebookVOList>('/public/notebooks/featured', {
        method: 'GET',
        ...(options || {}),
    })
}

/** 获取单个精选笔记本公开详情（含来源列表，无需登录）GET /public/notebooks/{id} */
export async function getPublicNotebookDetail(
    id: string | number,
    options?: { [key: string]: any },
) {
    return request<API.BaseResponsePublicNotebookDetailVO>(`/public/notebooks/${id}`, {
        method: 'GET',
        ...(options || {}),
    })
}

/** 克隆精选笔记本为个人副本（需登录）POST /notebook/{id}/clone */
export async function cloneNotebook(
    id: string | number,
    options?: { [key: string]: any },
) {
    return request<API.BaseResponseLong>(`/notebook/${id}/clone`, {
        method: 'POST',
        ...(options || {}),
    })
}
