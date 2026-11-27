import { client } from './client'
import type { Post, PostInput, PostFilters } from '../types/post'
import type { PageResponse } from '../types/common'
import { toUser } from './users'
// Backend has no title column: the first line of content is the title.
export function splitContent(text: string) {
  const [title = '', ...rest] = text.split('\n')
  return { title, content: rest.join('\n').trim() }
}
interface PostSummary {
  postId: number
  sellerId: string
  sellerName: string
  content: string
  contentImg: string | null
  price: number
  condition: Post['condition']
  dealStatus: Post['status']
  likesCount: number
}
interface PostDetail extends Omit<PostSummary, 'sellerId' | 'sellerName'> {
  seller: { loginId: string; name: string; userRank: number; locationName: string }
  likedByMe: boolean
}
interface Page<T> {
  items: T[]
  page: number
  size: number
  hasNext: boolean
}
function base(p: PostSummary | PostDetail) {
  return {
    id: String(p.postId),
    ...splitContent(p.content),
    price: p.price,
    contentImgUrl: p.contentImg ?? '',
    condition: p.condition,
    status: p.dealStatus,
    likesCount: p.likesCount,
    createdAt: '', // not provided by backend
  }
}
function fromSummary(p: PostSummary, isLiked = false): Post {
  return { ...base(p), seller: toUser(p.sellerId, p.sellerName), isLiked }
}
function fromDetail(p: PostDetail): Post {
  const { seller } = p
  return {
    ...base(p),
    seller: toUser(seller.loginId, seller.name, {
      userRank: seller.userRank,
      location: { id: '', name: seller.locationName },
    }),
    isLiked: p.likedByMe,
  }
}
// ponytail: backend gives hasNext, not total; fake a total so Pagination enables "next".
function toPage<T>(data: Page<PostSummary>, map: (p: PostSummary) => T): PageResponse<T> {
  const page = data.page + 1
  return {
    items: data.items.map(map),
    page,
    pageSize: data.size,
    total: data.hasNext ? page * data.size + 1 : data.page * data.size + data.items.length,
  }
}
function form(input: PostInput) {
  const body = new FormData()
  body.set('content', input.content ? `${input.title}\n${input.content}` : input.title)
  body.set('price', String(input.price))
  body.set('condition', input.condition)
  if (input.image) body.set('image', input.image)
  return body
}
export const postsApi = {
  async list({ feed, nearby, sellerLoginId, page = 1, pageSize = 20 }: PostFilters = {}) {
    const path = feed === 'following' ? '/feed' : nearby ? '/posts/nearby' : '/posts'
    const params = {
      page: page - 1,
      size: pageSize,
      ...(sellerLoginId ? { userId: sellerLoginId } : {}),
      ...(nearby ? { includeSold: true, sort: 'latest' } : {}),
    }
    const data = (await client.get<Page<PostSummary>>(path, { params })).data
    return toPage(data, (p) => fromSummary(p))
  },
  async detail(id: string) {
    return fromDetail((await client.get<PostDetail>(`/posts/${encodeURIComponent(id)}`)).data)
  },
  async create(input: PostInput) {
    const { postId } = (await client.post<{ postId: number }>('/posts', form(input))).data
    return { id: String(postId) }
  },
  async update(id: string, input: PostInput) {
    return fromDetail(
      (await client.patch<PostDetail>(`/posts/${encodeURIComponent(id)}`, form(input))).data,
    )
  },
  async remove(id: string) {
    await client.delete(`/posts/${encodeURIComponent(id)}`)
  },
  async status(id: string, status: 'ON_SALE' | 'RESERVED') {
    const body = new FormData()
    body.set('dealStatus', status)
    return fromDetail(
      (await client.patch<PostDetail>(`/posts/${encodeURIComponent(id)}`, body)).data,
    )
  },
  async like(id: string, liked: boolean) {
    const path = `/posts/${encodeURIComponent(id)}/likes`
    if (liked) await client.delete(path)
    else await client.post(path)
  },
  async likes(page = 1) {
    const data = (
      await client.get<Page<PostSummary>>('/users/me/likes', {
        params: { page: page - 1, size: 20 },
      })
    ).data
    return toPage(data, (p) => fromSummary(p, true))
  },
}
