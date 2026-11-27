// @vitest-environment jsdom
import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router'
import { FeedPage } from './FeedPage'
import { AuthPage } from './AuthPage'
import { PostCard } from '../components/posts/PostCard'
import { authApi } from '../api/auth'
import { postsApi } from '../api/posts'
import { getLocations } from '../api/locations'
import type { Post } from '../types/post'
import type { ReactNode } from 'react'
vi.mock('../api/locations', () => ({ getLocations: vi.fn() }))
vi.mock('../api/auth', () => ({
  authApi: { me: vi.fn(), signup: vi.fn(), login: vi.fn() },
}))
vi.mock('../api/posts', () => ({ postsApi: { list: vi.fn() } }))
afterEach(() => {
  cleanup()
  vi.resetAllMocks()
})
function mount(children: ReactNode) {
  const query = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  render(
    <QueryClientProvider client={query}>
      <MemoryRouter>{children}</MemoryRouter>
    </QueryClientProvider>,
  )
}
describe('critical form and display behavior', () => {
  it('does not submit a mismatched signup password', async () => {
    vi.mocked(getLocations).mockResolvedValue([
      { id: 'seoul', name: '서울특별시 마포구' },
    ])
    const user = userEvent.setup()
    mount(<AuthPage signup />)
    await user.type(screen.getByLabelText(/아이디/), 'testuser')
    await user.type(screen.getByLabelText('이메일'), 'test@example.com')
    await user.type(
      screen.getByLabelText(/^비밀번호/, {
        selector: 'input[name="password"]',
      }),
      'Abcd1234',
    )
    await user.type(screen.getByLabelText('비밀번호 확인'), 'Different123')
    await user.type(screen.getByLabelText('이름'), '테스트 이웃')
    await screen.findByRole('option', { name: '서울특별시' })
    await user.selectOptions(
      screen.getByRole('combobox', { name: '시/도' }),
      '서울특별시',
    )
    await user.selectOptions(
      screen.getByRole('combobox', { name: '시/군/구' }),
      'seoul',
    )
    await user.click(screen.getByRole('button', { name: '가지 시작하기' }))
    expect(await screen.findByRole('alert')).toBeTruthy()
    expect(authApi.signup).not.toHaveBeenCalled()
  })
  it('shows the product condition, sale state and local seller on cards', () => {
    const post: Post = {
      id: '1',
      title: '테스트 책상',
      content: '설명',
      price: 45000,
      contentImgUrl: '/images/desk.svg',
      condition: 'LIKE_NEW',
      status: 'RESERVED',
      likesCount: 3,
      isLiked: false,
      createdAt: '2026-11-06T00:00:00Z',
      seller: {
        loginId: 'seller',
        name: '판매 이웃',
        bio: '',
        location: { id: 'seoul', name: '서울특별시 마포구' },
        userRank: 50,
        followerCount: 0,
        followingCount: 0,
        isFollowing: false,
      },
    }
    mount(<PostCard post={post} />)
    expect(screen.getByText('서울특별시 마포구')).toBeTruthy()
    expect(screen.getByText('거의 새것')).toBeTruthy()
    expect(screen.getByText('예약중')).toBeTruthy()
    expect(screen.getByRole('img', { name: '테스트 책상' })).toBeTruthy()
  })
})

describe('feed loading transitions', () => {
  it('waits for the account check, then shows the intro and every neighborhood', async () => {
    let finishAccount!: (value: null) => void
    vi.mocked(authApi.me).mockReturnValue(
      new Promise<null>((resolve) => {
        finishAccount = resolve
      }),
    )
    vi.mocked(getLocations).mockResolvedValue([])
    vi.mocked(postsApi.list).mockResolvedValue({
      items: [],
      page: 1,
      pageSize: 20,
      total: 0,
    })
    mount(<FeedPage />)
    expect(screen.getByRole('status').textContent).toContain('상품 목록')
    expect(screen.queryByText('어느 동네를 둘러볼까요?')).toBeNull()
    expect(postsApi.list).not.toHaveBeenCalled()
    finishAccount(null)
    expect(await screen.findByText('어느 동네를 둘러볼까요?')).toBeTruthy()
    await waitFor(() => expect(postsApi.list).toHaveBeenCalledWith({ page: 1 }))
    expect(await screen.findByText('아직 판매글이 없어요')).toBeTruthy()
    expect(screen.queryByRole('status')).toBeNull()
  })

  it('replaces skeletons with the empty state after posts load', async () => {
    vi.mocked(authApi.me).mockResolvedValue(null)
    vi.mocked(getLocations).mockResolvedValue([
      { id: 'seoul', name: '서울특별시 마포구' },
    ])
    let finishPosts!: (value: Awaited<ReturnType<typeof postsApi.list>>) => void
    vi.mocked(postsApi.list).mockReturnValue(
      new Promise((resolve) => {
        finishPosts = resolve
      }),
    )
    mount(<FeedPage />)
    await waitFor(() => expect(postsApi.list).toHaveBeenCalled())
    expect(screen.getByRole('status').textContent).toContain('상품 목록')
    finishPosts({ items: [], page: 1, pageSize: 20, total: 0 })
    expect(await screen.findByText('아직 판매글이 없어요')).toBeTruthy()
    expect(screen.queryByRole('status')).toBeNull()
  })

  it('shows request errors without leaving a skeleton behind', async () => {
    vi.mocked(authApi.me).mockResolvedValue(null)
    vi.mocked(getLocations).mockResolvedValue([
      { id: 'seoul', name: '서울특별시 마포구' },
    ])
    vi.mocked(postsApi.list).mockRejectedValue(
      new Error('상품을 불러오지 못했습니다.'),
    )
    mount(<FeedPage />)
    expect((await screen.findByRole('alert')).textContent).toContain(
      '상품을 불러오지 못했습니다.',
    )
    expect(screen.queryByRole('status')).toBeNull()
  })
})
