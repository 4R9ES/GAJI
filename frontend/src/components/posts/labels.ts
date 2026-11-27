import type { PostStatus, PostCondition } from '../../types/post'
export const statusLabels: Record<PostStatus, string> = {
  ON_SALE: '판매중',
  RESERVED: '예약중',
  SOLD: '판매완료',
}
export const conditionLabels: Record<PostCondition, string> = {
  NEW: '새 상품',
  LIKE_NEW: '거의 새것',
  USED: '사용감 있음',
}
