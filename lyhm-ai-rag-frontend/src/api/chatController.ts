import request from '@/request'

/**
 * 对话 API 接口
 */

/**
 * 获取 SSE 流式对话的 URL（前端直接使用 EventSource）
 */
export function getChatStreamUrl(notebookId: string | number, message: string, sourceIds?: (string | number)[]) {
    const baseUrl = `${window.location.origin}/api`
    let url = `${baseUrl}/chat/stream?notebookId=${notebookId}&message=${encodeURIComponent(message)}`
    if (sourceIds && sourceIds.length > 0) {
        url += `&sourceIds=${sourceIds.join(',')}`
    }
    return url
}

/** 获取推荐问题 */
export async function getChatSuggestions(notebookId: string | number) {
    return request<API.BaseResponse<string[]>>('/chat/suggestions', {
        method: 'GET',
        params: { notebookId },
    })
}
