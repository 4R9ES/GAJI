import { http, HttpResponse } from 'msw/http'
import { hashPassword, pair, locations, publicUser, ready, state } from './data'
import type { MockPost, MockUser } from './data'
import type { PostCondition } from '../types/post'
import {
  validatePassword,
  validateMoney,
  IMAGE_TYPES,
  MAX_IMAGE_SIZE,
} from '../types/validation'
const base = `*${new URL(import.meta.env.VITE_API_BASE_URL || '/api', 'http://localhost').pathname.replace(/\/$/, '')}`
class Failure extends Error {
  status: number
  code: string
  constructor(status: number, code: string, message: string) {
    super(message)
    this.status = status
    this.code = code
  }
}
function fail(status: number, code: string, message: string): never {
  throw new Failure(status, code, message)
}
type Params = Record<string, string | readonly string[] | undefined>
function handle(
  fn: (request: Request, params: Params) => Response | Promise<Response>,
) {
  return async ({ request, params }: { request: Request; params: Params }) => {
    await ready
    try {
      return await fn(request, params)
    } catch (error) {
      if (error instanceof Failure)
        return HttpResponse.json(
          { code: error.code, message: error.message },
          { status: error.status },
        )
      if (error instanceof SyntaxError || error instanceof TypeError)
        return HttpResponse.json(
          { code: 'INVALID_REQUEST', message: '요청 형식을 확인해 주세요.' },
          { status: 400 },
        )
      console.error(error)
      return HttpResponse.json(
        { code: 'INTERNAL_ERROR', message: '목 서버 오류입니다.' },
        { status: 500 },
      )
    }
  }
}
async function body(request: Request): Promise<Record<string, unknown>> {
  const value: unknown = await request.json()
  if (!value || typeof value !== 'object' || Array.isArray(value))
    fail(400, 'INVALID_REQUEST', '요청 형식이 올바르지 않습니다.')
  return value as Record<string, unknown>
}
function text(value: unknown, label: string, max: number, optional = false) {
  if (optional && (value === undefined || value === '')) return ''
  if (typeof value !== 'string' || !value.trim() || value.length > max)
    fail(400, 'VALIDATION_ERROR', `${label}: 1~${max}자로 입력해 주세요.`)
  return value.trim()
}
function session(): MockUser {
  return (
    state.users.find((u) => u.loginId === state.session) ||
    fail(401, 'UNAUTHENTICATED', '로그인이 필요합니다.')
  )
}
function me(user: MockUser) {
  return {
    ...publicUser(user.loginId),
    email: user.email,
    account: user.account,
  }
}
function user(id: unknown) {
  return (
    state.users.find((u) => u.loginId === id) ||
    fail(404, 'USER_NOT_FOUND', '회원을 찾을 수 없습니다.')
  )
}
function post(id: unknown) {
  return (
    state.posts.find((p) => p.id === id) ||
    fail(404, 'POST_NOT_FOUND', '판매글을 찾을 수 없습니다.')
  )
}
function owner(p: MockPost) {
  if (session().loginId !== p.sellerLoginId)
    fail(403, 'FORBIDDEN', '판매자만 변경할 수 있습니다.')
}
function editable(p: MockPost) {
  owner(p)
  if (p.status === 'SOLD')
    fail(409, 'POST_SOLD', '판매완료 글은 수정·삭제할 수 없습니다.')
}
function serialize(p: MockPost) {
  const { sellerLoginId, ...rest } = p
  return {
    ...rest,
    seller: publicUser(sellerLoginId),
    likesCount: [...state.likes].filter((k) => k.endsWith(`:${p.id}`)).length,
    isLiked: state.likes.has(pair(state.session, p.id)),
  }
}
function paginate<T>(items: T[], request: Request) {
  const params = new URL(request.url).searchParams
  const page = Number(params.get('page') || 1)
  const pageSize = Number(params.get('pageSize') || 20)
  if (
    !Number.isInteger(page) ||
    page < 1 ||
    !Number.isInteger(pageSize) ||
    pageSize < 1 ||
    pageSize > 100
  )
    fail(400, 'INVALID_PAGINATION', '페이지 값을 확인해 주세요.')
  return {
    items: items.slice((page - 1) * pageSize, page * pageSize),
    page,
    pageSize,
    total: items.length,
  }
}
function candidates(p: MockPost) {
  return [
    ...new Set(
      state.comments
        .filter((c) => c.postId === p.id && c.authorLoginId !== p.sellerLoginId)
        .map((c) => c.authorLoginId),
    ),
  ].map(publicUser)
}
async function postInput(request: Request, required: boolean) {
  const form = await request.formData()
  const title = text(form.get('title'), '제목', 100)
  const content = text(form.get('content'), '본문', 1000)
  const rawPrice = form.get('price')
  const price =
    typeof rawPrice === 'string' && rawPrice.trim() ? Number(rawPrice) : NaN
  if (!validateMoney(price))
    fail(400, 'INVALID_PRICE', '가격은 0 이상의 안전한 정수로 입력해 주세요.')
  const condition = form.get('condition')
  if (!['NEW', 'LIKE_NEW', 'USED'].includes(String(condition)))
    fail(400, 'INVALID_CONDITION', '상품 상태를 선택해 주세요.')
  const image = form.get('image')
  if (required && !(image instanceof File))
    fail(400, 'IMAGE_REQUIRED', '사진 한 장을 선택해 주세요.')
  if (
    image !== null &&
    (!(image instanceof File) ||
      !IMAGE_TYPES.includes(image.type) ||
      image.size === 0 ||
      image.size > MAX_IMAGE_SIZE)
  )
    fail(400, 'INVALID_IMAGE', 'JPEG·PNG·WebP, 5MB 이하의 사진만 가능합니다.')
  return {
    title,
    price,
    content,
    condition: condition as PostCondition,
    image: image instanceof File ? image : null,
  }
}
export const handlers = [
  http.get(
    `${base}/locations`,
    handle(() => HttpResponse.json(locations)),
  ),
  http.get(
    `${base}/auth/me`,
    handle(() => HttpResponse.json(me(session()))),
  ),
  http.post(
    `${base}/auth/signup`,
    handle(async (request) => {
      const b = await body(request)
      const loginId = text(b.loginId, '아이디', 20)
      const email = text(b.email, '이메일', 254).toLowerCase()
      if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email))
        fail(400, 'INVALID_EMAIL', '이메일 형식을 확인해 주세요.')
      if (state.users.some((u) => u.loginId === loginId || u.email === email))
        fail(409, 'DUPLICATE_USER', '이미 사용 중인 아이디 또는 이메일입니다.')
      if (!validatePassword(b.password))
        fail(
          400,
          'INVALID_PASSWORD',
          '비밀번호는 8자 이상 영문·숫자를 포함해야 합니다.',
        )
      const name = text(b.name, '이름', 50)
      const bio = text(b.bio, '소개', 300, true)
      const account = text(b.account, '계좌', 30, true)
      if (!locations.some((l) => l.id === b.locationId))
        fail(400, 'INVALID_LOCATION', '동네를 선택해 주세요.')
      const passwordHash = await hashPassword(b.password as string)
      if (state.users.some((u) => u.loginId === loginId || u.email === email))
        fail(409, 'DUPLICATE_USER', '이미 사용 중인 아이디 또는 이메일입니다.')
      const created: MockUser = {
        loginId,
        email,
        name,
        bio,
        account,
        locationId: b.locationId as string,
        userRank: 50,
        passwordHash,
      }
      state.users.push(created)
      state.session = loginId
      return HttpResponse.json(me(created), { status: 201 })
    }),
  ),
  http.post(
    `${base}/auth/login`,
    handle(async (request) => {
      const b = await body(request)
      const found = state.users.find(
        (u) =>
          typeof b.email === 'string' &&
          u.email === b.email.trim().toLowerCase(),
      )
      if (
        !found ||
        typeof b.password !== 'string' ||
        found.passwordHash !== (await hashPassword(b.password))
      )
        fail(
          401,
          'INVALID_CREDENTIALS',
          '이메일 또는 비밀번호를 확인해 주세요.',
        )
      state.session = found.loginId
      return HttpResponse.json(me(found))
    }),
  ),
  http.post(
    `${base}/auth/logout`,
    handle(() => {
      state.session = null
      return new HttpResponse(null, { status: 204 })
    }),
  ),
  http.patch(
    `${base}/auth/password`,
    handle(async (request) => {
      const current = session()
      const b = await body(request)
      if (
        typeof b.currentPassword !== 'string' ||
        current.passwordHash !== (await hashPassword(b.currentPassword))
      )
        fail(400, 'INVALID_PASSWORD', '현재 비밀번호가 일치하지 않습니다.')
      if (!validatePassword(b.newPassword))
        fail(
          400,
          'INVALID_PASSWORD',
          '새 비밀번호는 8자 이상 영문·숫자를 포함해야 합니다.',
        )
      current.passwordHash = await hashPassword(b.newPassword as string)
      return new HttpResponse(null, { status: 204 })
    }),
  ),
  http.get(
    `${base}/posts`,
    handle((request) => {
      const q = new URL(request.url).searchParams
      let items = state.posts
      if (q.get('locationId'))
        items = items.filter(
          (p) => user(p.sellerLoginId).locationId === q.get('locationId'),
        )
      if (q.get('sellerLoginId'))
        items = items.filter((p) => p.sellerLoginId === q.get('sellerLoginId'))
      if (q.get('feed') === 'following') {
        const current = session()
        items = items.filter((p) =>
          state.follows.has(pair(current.loginId, p.sellerLoginId)),
        )
      }
      return HttpResponse.json(
        paginate(
          [...items]
            .sort((a, b) => b.createdAt.localeCompare(a.createdAt))
            .map(serialize),
          request,
        ),
      )
    }),
  ),
  http.post(
    `${base}/posts`,
    handle(async (request) => {
      const current = session()
      const { image, ...input } = await postInput(request, true)
      const created: MockPost = {
        ...input,
        id: crypto.randomUUID(),
        sellerLoginId: current.loginId,
        contentImgUrl: URL.createObjectURL(image!),
        status: 'ON_SALE',
        createdAt: new Date().toISOString(),
      }
      state.posts.push(created)
      return HttpResponse.json(serialize(created), { status: 201 })
    }),
  ),
  http.get(
    `${base}/posts/:postId`,
    handle((_r, p) => HttpResponse.json(serialize(post(p.postId)))),
  ),
  http.patch(
    `${base}/posts/:postId`,
    handle(async (request, p) => {
      const found = post(p.postId)
      editable(found)
      const { image, ...input } = await postInput(request, false)
      editable(found)
      Object.assign(found, input)
      if (image) {
        if (found.contentImgUrl.startsWith('blob:'))
          URL.revokeObjectURL(found.contentImgUrl)
        found.contentImgUrl = URL.createObjectURL(image)
      }
      return HttpResponse.json(serialize(found))
    }),
  ),
  http.delete(
    `${base}/posts/:postId`,
    handle((_r, p) => {
      const found = post(p.postId)
      editable(found)
      state.posts = state.posts.filter((item) => item.id !== found.id)
      state.comments = state.comments.filter((c) => c.postId !== found.id)
      state.likes = new Set(
        [...state.likes].filter((k) => !k.endsWith(`:${found.id}`)),
      )
      if (found.contentImgUrl.startsWith('blob:'))
        URL.revokeObjectURL(found.contentImgUrl)
      return new HttpResponse(null, { status: 204 })
    }),
  ),
  http.patch(
    `${base}/posts/:postId/status`,
    handle(async (request, p) => {
      const b = await body(request)
      const found = post(p.postId)
      editable(found)
      if (b.status !== 'ON_SALE' && b.status !== 'RESERVED')
        fail(
          400,
          'INVALID_STATUS',
          '판매완료는 거래 완료 요청을 사용해 주세요.',
        )
      found.status = b.status
      return HttpResponse.json(serialize(found))
    }),
  ),
  http.get(
    `${base}/posts/:postId/comments`,
    handle((_r, p) => {
      const found = post(p.postId)
      return HttpResponse.json(
        state.comments
          .filter((c) => c.postId === found.id)
          .map(({ authorLoginId, ...c }) => ({
            ...c,
            author: publicUser(authorLoginId),
          })),
      )
    }),
  ),
  http.post(
    `${base}/posts/:postId/comments`,
    handle(async (request, p) => {
      const current = session()
      const found = post(p.postId)
      const b = await body(request)
      const content = text(b.content, '댓글', 300)
      let parentId: string | null = null
      if (b.parentId !== undefined && b.parentId !== null) {
        const parent =
          state.comments.find(
            (c) => c.id === String(b.parentId) && c.postId === found.id,
          ) || fail(404, 'COMMENT_NOT_FOUND', '답장할 댓글이 없습니다.')
        // 답장은 한 단계만: 답장에 답장하면 같은 원댓글 아래에 붙는다.
        parentId = parent.parentId ?? parent.id
      }
      const created = {
        id: crypto.randomUUID(),
        postId: found.id,
        authorLoginId: current.loginId,
        parentId,
        content,
        createdAt: new Date().toISOString(),
      }
      state.comments.push(created)
      const { authorLoginId, ...rest } = created
      return HttpResponse.json(
        { ...rest, author: publicUser(authorLoginId) },
        { status: 201 },
      )
    }),
  ),
  http.delete(
    `${base}/comments/:commentId`,
    handle((_r, p) => {
      const current = session()
      const found =
        state.comments.find((c) => c.id === p.commentId) ||
        fail(404, 'COMMENT_NOT_FOUND', '댓글이 없습니다.')
      if (found.authorLoginId !== current.loginId)
        fail(403, 'FORBIDDEN', '본인 댓글만 삭제할 수 있습니다.')
      state.comments = state.comments.filter(
        (c) => c.id !== found.id && c.parentId !== found.id,
      )
      return new HttpResponse(null, { status: 204 })
    }),
  ),
  http.put(
    `${base}/posts/:postId/like`,
    handle((_r, p) => {
      const current = session()
      const found = post(p.postId)
      state.likes.add(pair(current.loginId, found.id))
      return new HttpResponse(null, { status: 204 })
    }),
  ),
  http.delete(
    `${base}/posts/:postId/like`,
    handle((_r, p) => {
      const current = session()
      const found = post(p.postId)
      state.likes.delete(pair(current.loginId, found.id))
      return new HttpResponse(null, { status: 204 })
    }),
  ),
  http.get(
    `${base}/me/likes`,
    handle((request) => {
      const current = session()
      return HttpResponse.json(
        paginate(
          state.posts
            .filter((p) => state.likes.has(pair(current.loginId, p.id)))
            .map(serialize),
          request,
        ),
      )
    }),
  ),
  http.get(
    `${base}/users/:loginId`,
    handle((_r, p) => HttpResponse.json(publicUser(user(p.loginId).loginId))),
  ),
  http.get(
    `${base}/users/:loginId/posts`,
    handle((request, p) => {
      const found = user(p.loginId)
      return HttpResponse.json(
        paginate(
          state.posts
            .filter((post) => post.sellerLoginId === found.loginId)
            .sort((a, b) => b.createdAt.localeCompare(a.createdAt))
            .map(serialize),
          request,
        ),
      )
    }),
  ),
  http.get(
    `${base}/users/:loginId/followers`,
    handle((_r, p) => {
      const found = user(p.loginId)
      return HttpResponse.json(
        state.users
          .filter((u) => state.follows.has(pair(u.loginId, found.loginId)))
          .map((u) => publicUser(u.loginId)),
      )
    }),
  ),
  http.get(
    `${base}/users/:loginId/following`,
    handle((_r, p) => {
      const found = user(p.loginId)
      return HttpResponse.json(
        state.users
          .filter((u) => state.follows.has(pair(found.loginId, u.loginId)))
          .map((u) => publicUser(u.loginId)),
      )
    }),
  ),
  http.put(
    `${base}/users/:loginId/follow`,
    handle((_r, p) => {
      const current = session()
      const found = user(p.loginId)
      if (current.loginId === found.loginId)
        fail(400, 'SELF_FOLLOW', '자신을 팔로우할 수 없습니다.')
      state.follows.add(pair(current.loginId, found.loginId))
      return new HttpResponse(null, { status: 204 })
    }),
  ),
  http.delete(
    `${base}/users/:loginId/follow`,
    handle((_r, p) => {
      const current = session()
      const found = user(p.loginId)
      if (current.loginId === found.loginId)
        fail(400, 'SELF_FOLLOW', '자신을 팔로우할 수 없습니다.')
      state.follows.delete(pair(current.loginId, found.loginId))
      return new HttpResponse(null, { status: 204 })
    }),
  ),
  http.get(
    `${base}/posts/:postId/buyer-candidates`,
    handle((_r, p) => {
      const found = post(p.postId)
      owner(found)
      return HttpResponse.json(candidates(found))
    }),
  ),
  http.post(
    `${base}/posts/:postId/deal`,
    handle(async (request, p) => {
      const b = await body(request)
      const found = post(p.postId)
      owner(found)
      if (
        found.status === 'SOLD' ||
        state.deals.some((d) => d.postId === found.id)
      )
        fail(409, 'DEAL_EXISTS', '이미 완료된 거래입니다.')
      if (!candidates(found).some((u) => u.loginId === b.buyerLoginId))
        fail(400, 'INVALID_BUYER', '댓글을 단 다른 회원을 선택해 주세요.')
      if (!validateMoney(b.money))
        fail(
          400,
          'INVALID_MONEY',
          '실거래가는 0 이상의 안전한 정수여야 합니다.',
        )
      const deal = {
        id: crypto.randomUUID(),
        postId: found.id,
        postTitle: found.title,
        sellerLoginId: found.sellerLoginId,
        buyerLoginId: b.buyerLoginId as string,
        money: b.money as number,
        dealDate: new Date().toISOString(),
        buyerRating: null,
      }
      state.deals.push(deal)
      found.status = 'SOLD'
      return HttpResponse.json(deal, { status: 201 })
    }),
  ),
  http.get(
    `${base}/me/deals`,
    handle((request) => {
      const current = session()
      const role = new URL(request.url).searchParams.get('role')
      if (role !== 'seller' && role !== 'buyer')
        fail(400, 'INVALID_ROLE', '거래 역할을 확인해 주세요.')
      return HttpResponse.json(
        state.deals
          .filter(
            (d) =>
              (role === 'seller' ? d.sellerLoginId : d.buyerLoginId) ===
              current.loginId,
          )
          .sort((a, b) => b.dealDate.localeCompare(a.dealDate)),
      )
    }),
  ),
  http.post(
    `${base}/deals/:dealId/buyer-rating`,
    handle(async (request, p) => {
      const b = await body(request)
      const current = session()
      const deal =
        state.deals.find((d) => d.id === p.dealId) ||
        fail(404, 'DEAL_NOT_FOUND', '거래를 찾을 수 없습니다.')
      if (deal.sellerLoginId !== current.loginId)
        fail(403, 'FORBIDDEN', '판매자만 평가할 수 있습니다.')
      if (deal.buyerRating !== null)
        fail(409, 'ALREADY_RATED', '평가는 한 번만 가능합니다.')
      if (
        typeof b.score !== 'number' ||
        !Number.isInteger(b.score) ||
        b.score < 1 ||
        b.score > 10
      )
        fail(400, 'INVALID_SCORE', '평가는 정수 1~10점입니다.')
      deal.buyerRating = b.score
      return HttpResponse.json(deal, { status: 201 })
    }),
  ),
]
