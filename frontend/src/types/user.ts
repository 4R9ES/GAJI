import type { Location } from './location'
export interface PublicUser {
  loginId: string
  name: string
  bio: string
  location: Location
  userRank: number
  followerCount: number
  followingCount: number
  isFollowing: boolean
}
export interface Me extends PublicUser {
  email: string
  account: string
}
