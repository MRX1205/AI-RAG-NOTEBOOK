import request from '@/request'

/**
 * 测验 API 接口
 */

/** 生成测验 */
export async function generateQuiz(body: API.QuizGenerateRequest) {
    return request<API.BaseResponse<API.QuizVO>>('/quiz/generate', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        data: body,
    })
}

/** 提交测验答案 */
export async function submitQuiz(body: API.QuizSubmitRequest) {
    return request<API.BaseResponse<any>>('/quiz/submit', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        data: body,
    })
}

/** 获取测验列表 */
export async function listQuizzes(notebookId: string | number) {
    return request<API.BaseResponse<API.QuizVO[]>>('/quiz/list', {
        method: 'GET',
        params: { notebookId },
    })
}

/** 获取测验详情 */
export async function getQuiz(id: string | number) {
    return request<API.BaseResponse<API.QuizVO>>('/quiz/get', {
        method: 'GET',
        params: { id },
    })
}

/** 删除测验 */
export async function deleteQuiz(body: API.DeleteRequest) {
    return request<API.BaseResponse<boolean>>('/quiz/delete', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        data: body,
    })
}
