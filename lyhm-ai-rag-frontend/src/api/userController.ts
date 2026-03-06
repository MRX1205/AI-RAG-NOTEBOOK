// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** 创建用户 POST /user/add */
export async function addUser(body: API.UserAddRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponseLong>('/user/add', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: body,
    ...(options || {}),
  })
}

/** 删除用户 POST /user/delete */
export async function deleteUser(body: API.DeleteRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean>('/user/delete', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: body,
    ...(options || {}),
  })
}

/** 获取用户（管理员）GET /user/get */
export async function getUserById(params: API.getUserByIdParams, options?: { [key: string]: any }) {
  return request<API.BaseResponseUser>('/user/get', {
    method: 'GET',
    params: { ...params },
    ...(options || {}),
  })
}

/** 获取登录用户信息 GET /user/get/login */
export async function getLoginUser(options?: { [key: string]: any }) {
  return request<API.BaseResponseLoginUserVO>('/user/get/login', {
    method: 'GET',
    ...(options || {}),
  })
}

/** 根据id获取用户VO GET /user/get/vo */
export async function getUserVoById(params: API.getUserVOByIdParams, options?: { [key: string]: any }) {
  return request<API.BaseResponseUserVO>('/user/get/vo', {
    method: 'GET',
    params: { ...params },
    ...(options || {}),
  })
}

/** 分页获取用户列表（管理员）GET /user/list */
export async function listUser(
  params: { pageNum?: number; pageSize?: number; userName?: string },
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageUserVO>('/user/list', {
    method: 'GET',
    params: { ...params },
    ...(options || {}),
  })
}

/** 分页获取用户封装列表（管理员POST版）POST /user/list/page/vo */
export async function listUserVoByPage(body: API.UserQueryRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponsePageUserVO>('/user/list/page/vo', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: body,
    ...(options || {}),
  })
}

/** 用户登录 POST /user/login */
export async function userLogin(body: API.UserLoginRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponseLoginUserVO>('/user/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: body,
    ...(options || {}),
  })
}

/** 用户注销 POST /user/logout */
export async function userLogout(options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean>('/user/logout', {
    method: 'POST',
    ...(options || {}),
  })
}

/** 用户注册 POST /user/register */
export async function userRegister(body: API.UserRegisterRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponseLong>('/user/register', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: body,
    ...(options || {}),
  })
}

/** 更新用户（管理员旧接口）POST /user/update */
export async function updateUser(body: API.UserUpdateRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean>('/user/update', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: body,
    ...(options || {}),
  })
}

/** 管理员修改用户 PUT /user/update/{id} */
export async function adminUpdateUser(id: string | number, body: API.UserAdminUpdateRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean>(`/user/update/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    data: body,
    ...(options || {}),
  })
}

/** 修改自己的用户名 PUT /user/update/username */
export async function updateUsername(body: API.UserUpdateUsernameRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean>('/user/update/username', {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    data: body,
    ...(options || {}),
  })
}

/** 修改自己的密码 PUT /user/update/password */
export async function updatePassword(body: API.UserUpdatePasswordRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean>('/user/update/password', {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    data: body,
    ...(options || {}),
  })
}

/** 上传头像 POST /user/upload/avatar */
export async function uploadAvatar(formData: FormData, options?: { [key: string]: any }) {
  return request<API.BaseResponseString>('/user/upload/avatar', {
    method: 'POST',
    data: formData,
    ...(options || {}),
  })
}

/** 管理员重置用户密码 PUT /user/admin/reset-password/{id} */
export async function adminResetPassword(id: string | number, options?: { [key: string]: any }) {
  return request<API.BaseResponseString>(`/user/admin/reset-password/${id}`, {
    method: 'PUT',
    ...(options || {}),
  })
}

/** 管理员修改用户状态 PUT /user/admin/status/{id} */
export async function adminUpdateUserStatus(id: string | number, body: API.UserStatusRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean>(`/user/admin/status/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    data: body,
    ...(options || {}),
  })
}
