import request from '@/request'

/**
 * 来源 API 接口
 */

/** 上传文件来源 */
export async function uploadSource(notebookId: string | number, file: File) {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('notebookId', String(notebookId))
    return request<API.BaseResponse<API.SourceVO>>('/source/upload', {
        method: 'POST',
        data: formData,
        // 不设置 Content-Type，让浏览器自动设置 multipart/form-data
    })
}

/** 添加文本来源 */
export async function addTextSource(body: API.SourceTextAddRequest) {
    return request<API.BaseResponse<API.SourceVO>>('/source/add/text', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        data: body,
    })
}

/** 获取来源列表 */
export async function listSources(notebookId: string | number) {
    return request<API.BaseResponse<API.SourceVO[]>>('/source/list', {
        method: 'GET',
        params: { notebookId },
    })
}

/** 删除来源 */
export async function deleteSource(body: API.DeleteRequest) {
    return request<API.BaseResponse<boolean>>('/source/delete', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        data: body,
    })
}
