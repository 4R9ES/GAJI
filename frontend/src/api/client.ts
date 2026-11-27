import axios from 'axios'
import type { ApiError } from '../types/common'
// Spring session cookie (JSESSIONID) is sent via withCredentials.
export const client = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  withCredentials: true,
  timeout: 15000,
})
// Backend wraps every body in { isSuccess, code, message, result }.
client.interceptors.response.use((response) => {
  if (response.data && typeof response.data === 'object' && 'isSuccess' in response.data)
    response.data = response.data.result
  return response
})
export function errorMessage(error: unknown): string {
  if (axios.isAxiosError<ApiError>(error))
    return (
      error.response?.data?.message ||
      (error.code === 'ECONNABORTED'
        ? '요청 시간이 초과됐습니다. 다시 시도해 주세요.'
        : '서버에 연결할 수 없습니다.')
    )
  return error instanceof Error ? error.message : '요청을 처리하지 못했습니다.'
}
