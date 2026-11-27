import { client } from './client'
import type { BuyerCandidate, CreateDealRequest, Deal } from '../types/deal'
import { commentsApi } from './comments'
import { splitContent } from './posts'
interface DealSummary {
  dealId: number
  postId: number
  content: string
  sellerId: string
  buyerId: string
  money: number
  dealDate: string
  buyerScore: number | null
}
export const dealsApi = {
  // No candidates endpoint: commenters on the post other than the viewer (seller).
  async candidates(postId: string): Promise<BuyerCandidate[]> {
    const comments = await commentsApi.list(postId)
    const me = comments.find((c) => c.mine)?.author.loginId
    const users = new Map(comments.map((c) => [c.author.loginId, c.author]))
    if (me) users.delete(me)
    return [...users.values()]
  },
  async complete(postId: string, input: CreateDealRequest) {
    await client.post('/deals', {
      postId: Number(postId),
      buyerId: input.buyerLoginId,
      money: input.money,
    })
  },
  async list(role: 'seller' | 'buyer'): Promise<Deal[]> {
    const rows = (
      await client.get<DealSummary[]>(role === 'seller' ? '/deals/sales' : '/deals/purchases')
    ).data
    return rows.map((d) => ({
      id: String(d.dealId),
      postId: String(d.postId),
      postTitle: splitContent(d.content).title,
      sellerLoginId: d.sellerId,
      buyerLoginId: d.buyerId,
      money: d.money,
      dealDate: d.dealDate,
      buyerRating: d.buyerScore,
    }))
  },
  async rate(id: string, score: number) {
    await client.post(`/deals/${encodeURIComponent(id)}/rating`, { score })
  },
}
