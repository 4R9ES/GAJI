export interface PageResponse<T> {
  items: T[]
  page: number
  pageSize: number
  total: number
}
export interface ApiError {
  code: string
  message: string
  fieldErrors?: Record<string, string>
}
