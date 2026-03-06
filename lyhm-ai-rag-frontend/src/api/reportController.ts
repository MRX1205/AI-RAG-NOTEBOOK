import request from '@/request'

/**
 * 报告 API 接口
 */

/** 获取报告列表 */
export async function listReports(notebookId: string | number) {
    return request<API.BaseResponse<API.ReportVO[]>>('/report/list', {
        method: 'GET',
        params: { notebookId },
    })
}

/** 获取报告详情 */
export async function getReport(id: string | number) {
    return request<API.BaseResponse<API.ReportVO>>('/report/get', {
        method: 'GET',
        params: { id },
    })
}

/** 删除报告 */
export async function deleteReport(body: API.DeleteRequest) {
    return request<API.BaseResponse<boolean>>('/report/delete', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        data: body,
    })
}
