import type { PublicUser } from './user'
export interface CreateDealRequest {
  buyerLoginId: string
  money: number
}
export interface Deal {
  id: string
  postId: string
  postTitle: string
  sellerLoginId: string
  buyerLoginId: string
  money: number
  dealDate: string
  buyerRating: number | null
}
export type BuyerCandidate = PublicUser
