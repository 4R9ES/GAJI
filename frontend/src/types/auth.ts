import type { Me } from './user'
export interface SignupRequest {
  loginId: string
  email: string
  password: string
  name: string
  locationId: string
  bio?: string
  account?: string
}
export interface LoginRequest {
  email: string
  password: string
}
export interface ChangePasswordRequest {
  currentPassword: string
  newPassword: string
}
export type AuthResponse = Me
