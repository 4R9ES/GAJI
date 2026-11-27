import { client } from './client'
import type { Comment } from '../types/comment'
import { toUser } from './users'
interface CommentSummary {
  commentId: number
  userId: string
  userName: string
  content: string
  mine: boolean
}
export const commentsApi = {
  async list(postId: string): Promise<(Comment & { mine: boolean })[]> {
    const rows = (
      await client.get<CommentSummary[]>(`/posts/${encodeURIComponent(postId)}/comments`)
    ).data
    return rows.map((c) => ({
      id: String(c.commentId),
      postId,
      author: toUser(c.userId, c.userName),
      parentId: null, // backend has no replies
      content: c.content,
      createdAt: '', // not provided by backend
      mine: c.mine,
    }))
  },
  async create(postId: string, content: string) {
    await client.post(`/posts/${encodeURIComponent(postId)}/comments`, { content })
  },
  async remove(id: string) {
    await client.delete(`/comments/${encodeURIComponent(id)}`)
  },
}
