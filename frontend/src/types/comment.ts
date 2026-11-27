import type { PublicUser } from './user'
export interface Comment {
  id: string
  postId: string
  author: PublicUser
  parentId: string | null
  content: string
  createdAt: string
}
