import { client } from './client'
import type { PublicUser } from '../types/user'
// Backend only returns some profile fields per endpoint; fill the rest.
export function toUser(
  loginId: string,
  name: string,
  extra: Partial<PublicUser> = {},
): PublicUser {
  return {
    loginId,
    name,
    bio: '',
    location: { id: '', name: '' },
    userRank: 0,
    followerCount: 0,
    followingCount: 0,
    isFollowing: false,
    ...extra,
  }
}
interface ProfileResponse {
  loginId: string
  name: string
  bio: string | null
  locationName: string
  userRank: number
  followerCount: number
  followingCount: number
  followedByMe: boolean
}
export const usersApi = {
  async detail(id: string) {
    const p = (
      await client.get<ProfileResponse>(`/users/${encodeURIComponent(id)}`)
    ).data
    return toUser(p.loginId, p.name, {
      bio: p.bio ?? '',
      location: { id: '', name: p.locationName },
      userRank: p.userRank,
      followerCount: p.followerCount,
      followingCount: p.followingCount,
      isFollowing: p.followedByMe,
    })
  },
  async connections(id: string, kind: 'followers' | 'following') {
    const rows = (
      await client.get<{ loginId: string; name: string }[]>(
        `/users/${encodeURIComponent(id)}/${kind}`,
      )
    ).data
    return rows.map((u) => toUser(u.loginId, u.name))
  },
  async follow(id: string, following: boolean) {
    const path = `/users/${encodeURIComponent(id)}/follow`
    if (following) await client.delete(path)
    else await client.post(path)
  },
}
