import { client } from './client'
import axios from 'axios'
import type {
  SignupRequest,
  LoginRequest,
  ChangePasswordRequest,
} from '../types/auth'
import type { Me } from '../types/user'
import { toUser } from './users'
interface MeResponse {
  loginId: string
  name: string
  email: string
  bio: string | null
  location: { locationId: number; name: string }
  account: string
  userRank: number
  followerCount: number
  followingCount: number
}
function toMe(m: MeResponse): Me {
  return {
    ...toUser(m.loginId, m.name, {
      bio: m.bio ?? '',
      location: { id: String(m.location.locationId), name: m.location.name },
      userRank: m.userRank,
      followerCount: m.followerCount,
      followingCount: m.followingCount,
    }),
    email: m.email,
    account: m.account,
  }
}
export const authApi = {
  async me(): Promise<Me | null> {
    try {
      return toMe((await client.get<MeResponse>('/auth/me')).data)
    } catch (error) {
      if (axios.isAxiosError(error) && error.response?.status === 401)
        return null
      throw error
    }
  },
  async signup(input: SignupRequest) {
    await client.post('/users', {
      ...input,
      locationId: Number(input.locationId),
    })
    // Backend signup does not start a session; log in right after.
    return authApi.login({ email: input.email, password: input.password })
  },
  async login(input: LoginRequest) {
    return toMe((await client.post<MeResponse>('/auth/login', input)).data)
  },
  async logout() {
    await client.post('/auth/logout')
  },
  async changePassword(input: ChangePasswordRequest) {
    await client.patch('/users/me/password', input)
  },
}
