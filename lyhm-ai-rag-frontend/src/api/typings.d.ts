declare namespace API {
  type BaseResponseBoolean = {
    code?: number
    data?: boolean
    message?: string
  }

  type BaseResponseLoginUserVO = {
    code?: number
    data?: LoginUserVO
    message?: string
  }

  type BaseResponseLong = {
    code?: number
    data?: number
    message?: string
  }

  type BaseResponsePageUserVO = {
    code?: number
    data?: PageUserVO
    message?: string
  }

  type BaseResponseString = {
    code?: number
    data?: string
    message?: string
  }

  type BaseResponseUser = {
    code?: number
    data?: User
    message?: string
  }

  type BaseResponseUserVO = {
    code?: number
    data?: UserVO
    message?: string
  }

  type DeleteRequest = {
    id?: string | number
  }

  type getUserByIdParams = {
    id: number
  }

  type getUserVOByIdParams = {
    id: number
  }

  type LoginUserVO = {
    id?: string | number
    userAccount?: string
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
    createTime?: string
    updateTime?: string
  }

  type PageUserVO = {
    records?: UserVO[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
    optimizeCountQuery?: boolean
  }

  type User = {
    id?: string | number
    userAccount?: string
    userPassword?: string
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
    editTime?: string
    createTime?: string
    updateTime?: string
    isDelete?: number
  }

  type UserAddRequest = {
    userName?: string
    userAccount?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
    userPassword?: string
  }

  type UserLoginRequest = {
    userAccount?: string
    userPassword?: string
  }

  type UserQueryRequest = {
    pageNum?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    id?: string | number
    userName?: string
    userAccount?: string
    userProfile?: string
    userRole?: string
  }

  type UserRegisterRequest = {
    userAccount?: string
    userPassword?: string
    checkPassword?: string
    role?: string
    adminCode?: string
  }

  type UserUpdateRequest = {
    id?: string | number
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
  }

  type UserUpdateUsernameRequest = {
    newUserName?: string
  }

  type UserUpdatePasswordRequest = {
    oldPassword?: string
    newPassword?: string
  }

  type UserAdminUpdateRequest = {
    userName?: string
    userRole?: string
  }

  type UserVO = {
    id?: string | number
    userAccount?: string
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
    status?: number
    createTime?: string
  }

  type UserStatusRequest = {
    status?: number
  }

  // ========== 笔记本相关类型 ==========

  type NotebookVO = {
    id?: string | number
    title?: string
    description?: string
    sourceCount?: number
    coverImage?: string
    isFeatured?: number
    userName?: string
    createTime?: string
    updateTime?: string
  }

  type FeaturedNotebookVO = {
    id?: string | number
    title?: string
    description?: string
    summary?: string
    coverImage?: string
    userName?: string
    createTime?: string
  }

  type NotebookAddRequest = {
    title?: string
    description?: string
  }

  type NotebookUpdateRequest = {
    id?: string | number
    title?: string
    description?: string
  }

  type NotebookAdminUpdateRequest = {
    title?: string
    description?: string
    summary?: string
    isFeatured?: number
  }

  type NotebookFeaturedRequest = {
    isFeatured?: number
  }

  type BaseResponseNotebookVO = {
    code?: number
    data?: NotebookVO
    message?: string
  }

  type BaseResponseNotebookVOList = {
    code?: number
    data?: NotebookVO[]
    message?: string
  }

  type BaseResponseFeaturedNotebookVOList = {
    code?: number
    data?: FeaturedNotebookVO[]
    message?: string
  }

  type BaseResponsePageNotebookVO = {
    code?: number
    data?: {
      records?: NotebookVO[]
      totalRow?: number
      pageNumber?: number
      pageSize?: number
    }
    message?: string
  }

  // 通用泛型响应类型
  type BaseResponse<T> = {
    code?: number
    data?: T
    message?: string
  }

  // ========== 来源相关类型 ==========

  type SourceVO = {
    id?: string | number
    notebookId?: string | number
    fileName?: string
    fileType?: string
    fileSize?: number
    segmentCount?: number
    status?: string
    errorMessage?: string
    createTime?: string
  }

  type SourceTextAddRequest = {
    notebookId?: string | number
    title?: string
    content?: string
  }

  // ========== 报告相关类型 ==========

  type ReportVO = {
    id?: string | number
    notebookId?: string | number
    title?: string
    reportType?: string
    content?: string
    sourceIds?: string
    createTime?: string
  }

  type ReportGenerateRequest = {
    notebookId?: string | number
    reportType?: string
    sourceIds?: (string | number)[]
    customPrompt?: string
  }

  // ========== 测验相关类型 ==========

  type QuizVO = {
    id?: string | number
    notebookId?: string | number
    title?: string
    questionCount?: number
    difficulty?: string
    questions?: string
    sourceIds?: string
    createTime?: string
  }

  type QuizQuestion = {
    questionId?: number
    question?: string
    options?: QuizOption[]
    correctAnswer?: string
    explanation?: string
  }

  type QuizOption = {
    label?: string
    text?: string
  }

  type QuizGenerateRequest = {
    notebookId?: string | number
    questionCount?: number
    difficulty?: string
    sourceIds?: (string | number)[]
  }

  type QuizSubmitRequest = {
    quizId?: number
    answers?: QuizAnswer[]
    timeCost?: number
  }

  type QuizAnswer = {
    questionId?: number
    answer?: string
  }

  type QuizSubmitResult = {
    recordId?: number
    score?: number
    correctCount?: number
    totalCount?: number
    results?: QuizAnswerResult[]
  }

  type QuizAnswerResult = {
    questionId?: number
    userAnswer?: string
    correctAnswer?: string
    isCorrect?: boolean
    explanation?: string
  }

  // ========== 对话相关类型 ==========

  type ChatRequest = {
    notebookId?: string | number
    message?: string
    sourceIds?: (string | number)[]
  }

  // ========== 保存笔记相关类型 ==========

  type SaveNoteRequest = {
    notebookId?: string | number
    title?: string
    content: string
    sourceQuestion?: string
  }

  type UpdateNoteRequest = {
    title?: string
    content?: string
  }

  type SavedNoteVO = {
    id?: string | number
    userId?: string | number
    notebookId?: string | number
    title?: string
    content?: string
    sourceQuestion?: string
    createTime?: string
    updateTime?: string
  }

  type BaseResponseSavedNoteVO = {
    code?: number
    data?: SavedNoteVO
    message?: string
  }

  type PageSavedNoteVO = {
    records?: SavedNoteVO[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
  }

  type BaseResponsePageSavedNoteVO = {
    code?: number
    data?: PageSavedNoteVO
    message?: string
  }
}
