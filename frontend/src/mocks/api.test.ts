import { beforeAll, afterAll, beforeEach, describe, expect, it } from 'vitest'
import { setupServer } from 'msw/node'
import { handlers } from './handlers'
import { ready, resetStore, state } from './data'
import { client } from '../api/client'
import { authApi } from '../api/auth'
import { postsApi } from '../api/posts'
import { commentsApi } from '../api/comments'
import { usersApi } from '../api/users'
import { dealsApi } from '../api/deals'
import { getLocations } from '../api/locations'
import { validateMoney, validatePassword } from '../types/validation'
const server = setupServer(...handlers)
beforeAll(async () => {
  await ready
  client.defaults.baseURL = 'http://localhost/api'
  server.listen({ onUnhandledFrame: 'error' })
})
afterAll(() => server.close())
beforeEach(async () => {
  server.resetHandlers()
  await resetStore()
})
const login = (email = 'demo@gazi.local') =>
  authApi.login({ email, password: 'Gazi1234' })
const rejected = (status: number, code?: string) => ({
  response: { status, ...(code ? { data: { code } } : {}) },
})
describe('API contract through axios and MSW', () => {
  it('requires authentication and never exposes credentials on public profiles', async () => {
    expect(await authApi.me()).toBeNull()
    await expect(postsApi.remove('1')).rejects.toMatchObject(rejected(401))
    const publicProfile = await usersApi.detail('gazi_demo')
    expect(publicProfile).not.toHaveProperty('password')
    expect(publicProfile).not.toHaveProperty('passwordHash')
    expect(publicProfile).not.toHaveProperty('email')
    expect(publicProfile).not.toHaveProperty('account')
    const me = await login()
    expect(me.email).toBe('demo@gazi.local')
    expect(me).not.toHaveProperty('passwordHash')
    await authApi.logout()
    expect(await authApi.me()).toBeNull()
  })
  it('filters locations, returns condition, createdAt and paginates', async () => {
    expect(await getLocations()).toHaveLength(56)
    const posts = await postsApi.list({
      locationId: 'seoul-마포구',
      pageSize: 2,
    })
    expect(posts.items).toHaveLength(2)
    expect(posts.total).toBe(5)
    expect(
      posts.items.every((p) => p.seller.location.id === 'seoul-마포구'),
    ).toBe(true)
    expect(posts.items[0].condition).toBe('USED')
    expect(posts.items[0].createdAt).toBeTruthy()
    expect(posts.items[0]).not.toHaveProperty('updatedAt')
    await expect(postsApi.list({ page: 0 })).rejects.toMatchObject(
      rejected(400),
    )
  })
  it('creates a member, rejects duplicates and enforces DB length limits', async () => {
    const input = {
      loginId: 'new_neighbor',
      email: 'new@example.com',
      password: 'Strong123',
      name: '새 이웃',
      locationId: 'gyeonggi-화성시',
      bio: '반가워요',
    }
    expect((await authApi.signup(input)).userRank).toBe(50)
    await expect(authApi.signup(input)).rejects.toMatchObject(rejected(409))
    await expect(
      authApi.signup({
        ...input,
        loginId: 'other',
        email: 'other@example.com',
        bio: '가'.repeat(301),
      }),
    ).rejects.toMatchObject(rejected(400))
    await expect(
      authApi.signup({
        ...input,
        loginId: 'other',
        email: 'other@example.com',
        password: 'abcdefgh',
      }),
    ).rejects.toMatchObject(rejected(400))
  })
  it('changes passwords and rejects the old credential', async () => {
    await login()
    await expect(
      authApi.changePassword({
        currentPassword: 'wrong',
        newPassword: 'Next1234',
      }),
    ).rejects.toMatchObject(rejected(400))
    await authApi.changePassword({
      currentPassword: 'Gazi1234',
      newPassword: 'Next1234',
    })
    await authApi.logout()
    await expect(login()).rejects.toMatchObject(rejected(401))
    expect(
      (await authApi.login({ email: 'demo@gazi.local', password: 'Next1234' }))
        .loginId,
    ).toBe('gazi_demo')
  })
  it('creates, reads, edits and deletes posts with one image', async () => {
    await login()
    const input = {
      title: '새 책상',
      price: 1000,
      content: '깨끗합니다',
      condition: 'NEW' as const,
      image: new File(['test-image'], 'photo.png', { type: 'image/png' }),
    }
    const created = await postsApi.create(input)
    expect(created.condition).toBe('NEW')
    expect(created.contentImgUrl).toMatch(/^blob:/)
    const edited = await postsApi.update(created.id, {
      ...input,
      title: '수정한 책상',
      condition: 'LIKE_NEW',
      image: undefined,
    })
    expect(edited.title).toBe('수정한 책상')
    expect(edited.condition).toBe('LIKE_NEW')
    expect((await postsApi.detail(created.id)).title).toBe('수정한 책상')
    await postsApi.remove(created.id)
    await expect(postsApi.detail(created.id)).rejects.toMatchObject(
      rejected(404),
    )
    await expect(
      postsApi.create({ ...input, content: '가'.repeat(1001) }),
    ).rejects.toMatchObject(rejected(400))
    await expect(
      postsApi.create({
        ...input,
        image: new File(['svg'], 'x.svg', { type: 'image/svg+xml' }),
      }),
    ).rejects.toMatchObject(rejected(400))
  })
  it('rejects mutations by another seller', async () => {
    await login()
    await expect(postsApi.remove('1')).rejects.toMatchObject(rejected(403))
    await expect(postsApi.status('1', 'RESERVED')).rejects.toMatchObject(
      rejected(403),
    )
    await expect(dealsApi.candidates('1')).rejects.toMatchObject(rejected(403))
  })
  it('creates and deletes own comments while protecting others', async () => {
    await login()
    const comment = await commentsApi.create('1', '거래 가능한가요?')
    expect((await commentsApi.list('1')).some((c) => c.id === comment.id)).toBe(
      true,
    )
    await commentsApi.remove(comment.id)
    await expect(commentsApi.remove('seed-comment-1')).rejects.toMatchObject(
      rejected(403),
    )
    await expect(
      commentsApi.create('1', '가'.repeat(301)),
    ).rejects.toMatchObject(rejected(400))
  })
  it('keeps replies one level deep and removes them with their parent', async () => {
    await login()
    const parent = await commentsApi.create('1', '원댓글')
    const reply = await commentsApi.create('1', '답장', parent.id)
    expect(reply.parentId).toBe(parent.id)
    const nested = await commentsApi.create('1', '답장의 답장', reply.id)
    expect(nested.parentId).toBe(parent.id)
    await expect(
      commentsApi.create('2', '다른 글', parent.id),
    ).rejects.toMatchObject(rejected(404))
    await commentsApi.remove(parent.id)
    const ids = (await commentsApi.list('1')).map((c) => c.id)
    expect(ids).not.toContain(reply.id)
    expect(ids).not.toContain(nested.id)
  })
  it('treats repeated likes and follows as idempotent and disallows self-follow', async () => {
    await login()
    await postsApi.like('2', false)
    await postsApi.like('2', false)
    expect((await postsApi.detail('2')).likesCount).toBe(1)
    expect((await postsApi.likes()).items.some((p) => p.id === '2')).toBe(true)
    await postsApi.like('2', true)
    expect((await postsApi.detail('2')).likesCount).toBe(0)
    await expect(usersApi.follow('gazi_demo', false)).rejects.toMatchObject(
      rejected(400),
    )
    await usersApi.follow('green', false)
    await usersApi.follow('green', false)
    expect(
      (await usersApi.connections('gazi_demo', 'following')).filter(
        (u) => u.loginId === 'green',
      ),
    ).toHaveLength(1)
    expect(
      (await postsApi.list({ feed: 'following' })).items.some(
        (p) => p.seller.loginId === 'green',
      ),
    ).toBe(true)
    await usersApi.follow('green', true)
    expect((await usersApi.detail('green')).isFollowing).toBe(false)
  })
  it('excludes the seller and deduplicates buyer candidates', async () => {
    await login()
    await commentsApi.create('3', '판매자 댓글')
    expect((await dealsApi.candidates('3')).map((u) => u.loginId)).toEqual([
      'neighbor',
    ])
    await authApi.logout()
    await login('neighbor@gazi.local')
    await commentsApi.create('3', '구매자 추가 댓글')
    await authApi.logout()
    await login()
    expect(await dealsApi.candidates('3')).toHaveLength(1)
  })
  it('completes one atomic deal, preserves SOLD and allows exactly one rating', async () => {
    await login()
    await postsApi.status('3', 'RESERVED')
    await postsApi.status('3', 'ON_SALE')
    await expect(
      dealsApi.complete('3', { buyerLoginId: 'gazi_demo', money: 80000 }),
    ).rejects.toMatchObject(rejected(400))
    await expect(
      dealsApi.complete('3', { buyerLoginId: 'neighbor', money: -1 }),
    ).rejects.toMatchObject(rejected(400))
    const results = await Promise.allSettled([
      dealsApi.complete('3', { buyerLoginId: 'neighbor', money: 80000 }),
      dealsApi.complete('3', { buyerLoginId: 'neighbor', money: 80000 }),
    ])
    expect(results.filter((r) => r.status === 'fulfilled')).toHaveLength(1)
    expect((await postsApi.detail('3')).status).toBe('SOLD')
    expect(state.deals).toHaveLength(1)
    await expect(postsApi.status('3', 'ON_SALE')).rejects.toMatchObject(
      rejected(409),
    )
    await expect(postsApi.remove('3')).rejects.toMatchObject(rejected(409))
    const deal = (await dealsApi.list('seller'))[0]
    await expect(dealsApi.rate(deal.id, 11)).rejects.toMatchObject(
      rejected(400),
    )
    expect((await dealsApi.rate(deal.id, 9)).buyerRating).toBe(9)
    await expect(dealsApi.rate(deal.id, 8)).rejects.toMatchObject(rejected(409))
    await authApi.logout()
    await login('neighbor@gazi.local')
    expect((await dealsApi.list('buyer'))[0].money).toBe(80000)
    await expect(dealsApi.rate(deal.id, 10)).rejects.toMatchObject(
      rejected(403),
    )
  })
  it('requires a current comment for buyer eligibility', async () => {
    await login()
    await commentsApi.remove('seed-comment-2')
    await authApi.logout()
    await login('neighbor@gazi.local')
    await expect(
      dealsApi.complete('1', { buyerLoginId: 'gazi_demo', money: 45000 }),
    ).rejects.toMatchObject(rejected(400))
    expect((await postsApi.detail('1')).status).toBe('ON_SALE')
  })
  it('rejects malformed request bodies and unknown resources', async () => {
    await expect(
      client.post('/auth/login', 'not json', {
        headers: { 'Content-Type': 'application/json' },
      }),
    ).rejects.toMatchObject(rejected(400))
    await expect(postsApi.detail('missing')).rejects.toMatchObject(
      rejected(404),
    )
    await expect(usersApi.detail('missing')).rejects.toMatchObject(
      rejected(404),
    )
  })
})
describe('form validation boundaries', () => {
  it('requires letters and digits in passwords', () => {
    expect(validatePassword('Abcd1234')).toBe(true)
    expect(validatePassword('12345678')).toBe(false)
    expect(validatePassword('abcdefghi')).toBe(false)
    expect(validatePassword('a1')).toBe(false)
  })
  it('accepts zero but rejects fractions, negatives and unsafe integers', () => {
    expect(validateMoney(0)).toBe(true)
    for (const value of [
      -1,
      1.5,
      NaN,
      Infinity,
      Number.MAX_SAFE_INTEGER + 1,
      '100',
    ])
      expect(validateMoney(value)).toBe(false)
  })
})
