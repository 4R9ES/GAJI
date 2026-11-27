import { Loading } from '../components/common/Loading'
import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useParams } from 'react-router'
import { useQuery } from '@tanstack/react-query'
import { usersApi } from '../api/users'
import { postsApi } from '../api/posts'
import { dealsApi } from '../api/deals'
import { authApi } from '../api/auth'
import { useAction, useMe } from '../hooks/useApi'
import { Avatar } from '../components/common/Avatar'
import { Empty, ErrorNotice, Field } from '../components/common/Feedback'
import { PostGrid, Pagination } from '../components/posts/PostGrid'
import type { PublicUser } from '../types/user'
import type { Deal } from '../types/deal'
import type { ChangePasswordRequest } from '../types/auth'
import { validatePassword } from '../types/validation'
function ProfileSummary({ user }: { user: PublicUser }) {
  const me = useMe()
  const follow = useAction(() =>
    usersApi.follow(user.loginId, user.isFollowing),
  )
  const mine = me.data?.loginId === user.loginId
  return (
    <section>
      <Avatar name={user.name} large />
      <p className="mt-5 text-sm text-muted">{user.location.name}</p>
      <h1 className="mt-1 text-3xl font-bold break-words">{user.name}</h1>
      <p className="mt-1 text-sm break-all text-muted">@{user.loginId}</p>
      <p className="mt-5 leading-7 break-words whitespace-pre-wrap">
        {user.bio || '아직 소개를 남기지 않았어요.'}
      </p>
      <dl className="stat-row mt-6">
        <div>
          <dt>이웃 신뢰도</dt>
          <dd className="text-brand">
            {user.userRank}
            <span className="text-sm font-normal text-muted"> / 100</span>
          </dd>
        </div>
        <div>
          <dt>팔로워</dt>
          <dd>
            <Link
              to={`/users/${encodeURIComponent(user.loginId)}/followers`}
              className="inline-flex min-h-11 items-center underline-offset-4 hover:underline"
            >
              {user.followerCount}
            </Link>
          </dd>
        </div>
        <div>
          <dt>팔로잉</dt>
          <dd>
            <Link
              to={`/users/${encodeURIComponent(user.loginId)}/following`}
              className="inline-flex min-h-11 items-center underline-offset-4 hover:underline"
            >
              {user.followingCount}
            </Link>
          </dd>
        </div>
      </dl>
      {!mine &&
        (me.data ? (
          <button
            type="button"
            className={`${user.isFollowing ? 'btn-secondary' : 'btn'} mt-6 w-full`}
            onClick={() => follow.mutate()}
            disabled={follow.isPending}
            aria-pressed={user.isFollowing}
          >
            {user.isFollowing ? '팔로우 취소' : '팔로우'}
          </button>
        ) : (
          <Link to="/login" className="btn mt-6 w-full">
            팔로우
          </Link>
        ))}
      <ErrorNotice error={follow.error} />
    </section>
  )
}
export function ProfilePage() {
  const { loginId = '' } = useParams()
  const [page, setPage] = useState(1)
  const user = useQuery({
    queryKey: ['user', loginId],
    queryFn: () => usersApi.detail(loginId),
  })
  const posts = useQuery({
    queryKey: ['posts', { sellerLoginId: loginId, page }],
    queryFn: () => postsApi.list({ sellerLoginId: loginId, page }),
  })
  if (user.isPending) return <Loading variant="profile" />
  if (user.error) return <ErrorNotice error={user.error} />
  return (
    <div className="split-aside">
      <aside className="lg:sticky lg:top-24 lg:self-start">
        <ProfileSummary user={user.data!} />
      </aside>
      <div className="min-w-0">
        <div className="mb-6 border-b border-line pb-3">
          <h2 className="text-xl font-bold">
            이웃의 판매 물건{' '}
            <span className="text-brand tabular-nums">
              {posts.data?.total || 0}
            </span>
          </h2>
          <p className="mt-1 text-sm text-muted">
            판매완료 글에서 거래 기록도 확인할 수 있어요.
          </p>
        </div>
        <ErrorNotice error={posts.error} />
        {posts.isPending ? (
          <Loading variant="cards" />
        ) : (
          posts.data && (
            <>
              <PostGrid posts={posts.data.items} />
              <Pagination
                page={page}
                total={posts.data.total}
                onChange={setPage}
              />
            </>
          )
        )}
      </div>
    </div>
  )
}
export function ConnectionsPage({ kind }: { kind: 'followers' | 'following' }) {
  const { loginId = '' } = useParams()
  const users = useQuery({
    queryKey: ['connections', loginId, kind],
    queryFn: () => usersApi.connections(loginId, kind),
  })
  return (
    <>
      <Link to={`/users/${encodeURIComponent(loginId)}`} className="back-link">
        프로필로 돌아가기
      </Link>
      <h1 className="mt-5 mb-6 text-2xl font-bold">
        {kind === 'followers' ? '팔로워' : '팔로잉'}
      </h1>
      <ErrorNotice error={users.error} />
      {users.isPending ? (
        <Loading variant="connections" />
      ) : users.data?.length ? (
        <div className="max-w-2xl divide-y divide-line border-y border-line">
          {users.data.map((user) => (
            <Link
              key={user.loginId}
              to={`/users/${encodeURIComponent(user.loginId)}`}
              className="flex items-center gap-3 py-4"
            >
              <Avatar name={user.name} />
              <div>
                <p className="text-sm font-semibold">{user.name}</p>
                <p className="mt-1 text-sm text-muted">{user.location.name}</p>
              </div>
              <span className="ml-auto text-sm text-brand tabular-nums">
                신뢰도 {user.userRank}
              </span>
            </Link>
          ))}
        </div>
      ) : (
        <Empty title="아직 연결된 이웃이 없어요" />
      )}
    </>
  )
}
export function MyPage() {
  const me = useMe()
  const [tab, setTab] = useState<'posts' | 'likes' | 'seller' | 'buyer'>(
    'posts',
  )
  const [page, setPage] = useState(1)
  const posts = useQuery({
    queryKey: ['my-posts', me.data?.loginId, tab, page],
    queryFn: () =>
      tab === 'likes'
        ? postsApi.likes(page)
        : postsApi.list({ sellerLoginId: me.data!.loginId, page }),
    enabled: !!me.data && (tab === 'posts' || tab === 'likes'),
  })
  const deals = useQuery({
    queryKey: ['deals', tab],
    queryFn: () => dealsApi.list(tab === 'buyer' ? 'buyer' : 'seller'),
    enabled: tab === 'seller' || tab === 'buyer',
  })
  if (!me.data) return <Loading variant="profile" />
  return (
    <>
      <div className="mb-8 flex items-center justify-between gap-3">
        <h1 className="text-2xl font-bold">나의 가지</h1>
        <Link to="/me/password" className="btn-secondary">
          비밀번호 변경
        </Link>
      </div>
      <div className="split-aside">
        <aside className="lg:sticky lg:top-24 lg:self-start">
          <ProfileSummary user={me.data} />
          <dl className="mt-6 space-y-3 text-sm">
            <div>
              <dt className="text-muted">이메일</dt>
              <dd className="mt-0.5 break-all">{me.data.email}</dd>
            </div>
            <div>
              <dt className="text-muted">계좌</dt>
              <dd className="mt-0.5">{me.data.account || '미등록'}</dd>
            </div>
          </dl>
          <p className="mt-3 text-sm text-muted">
            본인에게만 보이는 정보입니다.
          </p>
        </aside>
        <div className="min-w-0">
          <div
            role="group"
            aria-label="내 활동 선택"
            className="activity-tabs mb-6"
          >
            {(
              [
                { id: 'posts', label: '내 판매글' },
                { id: 'likes', label: '찜한 물건' },
                { id: 'seller', label: '판매 내역' },
                { id: 'buyer', label: '구매 내역' },
              ] as const
            ).map((item) => (
              <button
                key={item.id}
                aria-pressed={tab === item.id}
                className="activity-tab"
                onClick={() => {
                  setTab(item.id)
                  setPage(1)
                }}
              >
                {item.label}
              </button>
            ))}
          </div>
          {tab === 'posts' || tab === 'likes' ? (
            <>
              <ErrorNotice error={posts.error} />
              {posts.isPending ? (
                <Loading variant="cards" />
              ) : (
                posts.data && (
                  <>
                    <PostGrid posts={posts.data.items} />
                    <Pagination
                      page={page}
                      total={posts.data.total}
                      onChange={setPage}
                    />
                  </>
                )
              )}
            </>
          ) : (
            <>
              <ErrorNotice error={deals.error} />
              {deals.isPending ? (
                <Loading variant="deals" />
              ) : deals.data?.length ? (
                <div className="border-t border-line">
                  {deals.data.map((deal) => (
                    <DealCard
                      key={deal.id}
                      deal={deal}
                      seller={tab === 'seller'}
                    />
                  ))}
                </div>
              ) : (
                <Empty title="아직 완료된 거래가 없어요" />
              )}
            </>
          )}
        </div>
      </div>
    </>
  )
}
function DealCard({ deal, seller }: { deal: Deal; seller: boolean }) {
  const [score, setScore] = useState('')
  const action = useAction((score: number) => dealsApi.rate(deal.id, score))
  return (
    <article className="border-b border-line py-5">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <Link
            to={`/posts/${deal.postId}`}
            className="font-semibold text-brand"
          >
            {deal.postTitle}
          </Link>
          <p className="mt-2 text-sm">
            실거래가 {deal.money.toLocaleString('ko-KR')}원
          </p>
          <p className="mt-2 text-sm text-muted">
            {new Date(deal.dealDate).toLocaleString('ko-KR')}
          </p>
          <Link
            className="mt-2 inline-block text-sm text-muted underline"
            to={`/users/${encodeURIComponent(seller ? deal.buyerLoginId : deal.sellerLoginId)}`}
          >
            {seller ? '구매자' : '판매자'} @
            {seller ? deal.buyerLoginId : deal.sellerLoginId}
          </Link>
        </div>
        <span className="rounded-lg bg-leaf-soft px-3 py-1 text-xs text-leaf-dark">
          판매완료
        </span>
      </div>
      {!seller && deal.buyerRating === null ? (
        <form
          className="mt-4 space-y-3 border-t border-line pt-4"
          onSubmit={(event) => {
            event.preventDefault()
            if (
              window.confirm(
                `${score}점으로 평가할까요? 저장 후 수정할 수 없습니다.`,
              )
            )
              action.mutate(Number(score))
          }}
        >
          <Field
            label="판매자 평가"
            hint="거래당 한 번만 입력할 수 있으며, 이후 수정할 수 없습니다."
          >
            <select
              value={score}
              onChange={(e) => setScore(e.target.value)}
              required
            >
              <option value="">점수 선택 (0~10)</option>
              {Array.from({ length: 11 }, (_, i) => (
                <option key={i} value={i}>
                  {i}점
                </option>
              ))}
            </select>
          </Field>
          <button className="btn" disabled={action.isPending || !score}>
            평가 저장
          </button>
          <ErrorNotice error={action.error} />
        </form>
      ) : (
        <p className="mt-4 text-sm text-muted">
          판매자 평가:{' '}
          {deal.buyerRating === null
            ? '아직 평가되지 않았어요'
            : `${deal.buyerRating} / 10점`}
        </p>
      )}
    </article>
  )
}
export function PasswordPage() {
  const [error, setError] = useState('')
  const [success, setSuccess] = useState(false)
  const action = useAction(
    (input: ChangePasswordRequest) => authApi.changePassword(input),
    () => setSuccess(true),
  )
  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = new FormData(event.currentTarget)
    const newPassword = String(form.get('newPassword'))
    setSuccess(false)
    setError('')
    if (
      !validatePassword(newPassword) ||
      newPassword !== form.get('confirmPassword')
    ) {
      setError(
        '새 비밀번호는 영문·숫자 포함 8자 이상이며 확인란과 일치해야 합니다.',
      )
      return
    }
    const element = event.currentTarget
    action.mutate(
      { currentPassword: String(form.get('currentPassword')), newPassword },
      { onSuccess: () => element.reset() },
    )
  }
  return (
    <div className="mx-auto max-w-md">
      <Link to="/me" className="back-link">
        나의 가지로 돌아가기
      </Link>
      <h1 className="mt-5 mb-7 text-2xl font-bold">비밀번호 변경</h1>
      <form onSubmit={submit} className="space-y-5">
        <Field label="현재 비밀번호">
          <input
            type="password"
            name="currentPassword"
            required
            autoComplete="current-password"
          />
        </Field>
        <Field label="새 비밀번호" hint="영문·숫자 포함 8자 이상">
          <input
            type="password"
            name="newPassword"
            required
            minLength={8}
            autoComplete="new-password"
          />
        </Field>
        <Field label="새 비밀번호 확인">
          <input
            type="password"
            name="confirmPassword"
            required
            minLength={8}
            autoComplete="new-password"
          />
        </Field>
        <ErrorNotice error={error ? new Error(error) : action.error} />
        {success && (
          <p
            role="status"
            className="rounded-xl bg-leaf-soft p-4 text-sm text-leaf-dark"
          >
            비밀번호를 변경했습니다.
          </p>
        )}
        <button className="btn w-full" disabled={action.isPending}>
          비밀번호 변경
        </button>
      </form>
    </div>
  )
}
