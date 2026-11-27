import type { PublicUser } from './user'
export type PostStatus = 'ON_SALE' | 'RESERVED' | 'SOLD'
export type PostCondition = 'NEW' | 'LIKE_NEW' | 'USED'
export interface Post {
  id: string
  title: string
  price: number
  content: string
  contentImgUrl: string
  condition: PostCondition
  status: PostStatus
  seller: PublicUser
  likesCount: number
  isLiked: boolean
  createdAt: string
}
export interface PostInput {
  title: string
  price: number
  content: string
  condition: PostCondition
  image?: File
}
export interface PostFilters {
  nearby?: boolean
  feed?: 'following'
  sellerLoginId?: string
  page?: number
  pageSize?: number
}
