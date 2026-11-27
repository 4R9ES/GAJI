import { Loading } from '../components/common/Loading'
import { useState } from 'react'
import { Link } from 'react-router'
import mark from '../assets/gazi-mark.svg'
import { useQuery } from '@tanstack/react-query'
import { postsApi } from '../api/posts'
import { useMe } from '../hooks/useApi'
import { ErrorNotice } from '../components/common/Feedback'
import { PostGrid, Pagination } from '../components/posts/PostGrid'
export function FeedPage({ following = false }: { following?: boolean }) {
  const me = useMe()
  const [page, setPage] = useState(1)
  // Backend filters by distance from the logged-in user's location, not by picked area.
  const browsingAll = !following && !me.data
  const params = following
    ? { feed: 'following' as const, page }
    : me.data
      ? { nearby: true, page }
      : { page }
  const posts = useQuery({
    queryKey: ['posts', params],
    queryFn: () => postsApi.list(params),
    enabled: following || !me.isPending,
  })
  return (
    <>
      <div className="mb-10 grid gap-x-14 gap-y-8 lg:grid-cols-2">
        <section className="flex max-w-2xl flex-col">
          <h1 className="flex items-center gap-3 text-3xl font-bold sm:text-4xl">
            {following ? '팔로잉한 이웃의 물건' : '우리 동네 중고거래'}
            <img
              src={mark}
              alt=""
              aria-hidden="true"
              width={28}
              height={44}
              className="h-9 w-auto shrink-0 sm:h-11"
            />
          </h1>
          <p className="mt-3 text-muted">
            {following
              ? '팔로우한 이웃의 판매글을 동네 제한 없이 모았어요.'
              : '가까운 이웃의 물건을 보고, 프로필에서 거래 기록을 확인하세요.'}
          </p>
          {!following && me.data && (
            <p className="mt-6 text-sm font-semibold text-brand lg:mt-auto lg:pt-6">
              {me.data.location.name} 근처 5km
            </p>
          )}
        </section>
        {browsingAll && !me.isPending && (
          <aside
            aria-labelledby="feed-intro"
            className="rounded-card bg-paper-2 p-6"
          >
            <h2 id="feed-intro" className="text-lg font-bold">
              어느 동네를 둘러볼까요?
            </h2>
            <p className="mt-1 text-sm text-muted">
              가지는 이웃끼리 물건을 사고파는 동네 중고거래예요.
            </p>
            <ol className="mt-4 space-y-2.5">
              {[
                ['로그인하기', '가입한 동네 근처 물건이 보여요.'],
                [
                  '판매자 확인하기',
                  '프로필에서 이웃 신뢰도와 거래 기록을 봐요.',
                ],
                ['댓글로 거래하기', '궁금한 점과 거래 약속은 댓글로 나눠요.'],
              ].map(([title, text], index) => (
                <li key={title} className="flex items-start gap-3">
                  <span className="flex size-6 shrink-0 items-center justify-center rounded-full bg-surface font-display text-xs font-bold text-brand">
                    {index + 1}
                  </span>
                  <p className="text-sm leading-6">
                    <span className="font-semibold">{title}</span>
                    <span className="text-muted"> {text}</span>
                  </p>
                </li>
              ))}
            </ol>
            {!me.data && (
              <p className="mt-4 border-t border-line pt-3 text-sm">
                로그인하면 내 동네 물건이 바로 보여요.{' '}
                <Link
                  to="/login"
                  className="font-semibold text-brand underline underline-offset-4"
                >
                  로그인
                </Link>
              </p>
            )}
          </aside>
        )}
      </div>
      <div className="mb-6 flex flex-wrap items-baseline justify-between gap-x-4 gap-y-1 border-b border-line pb-3">
        <h2 className="text-lg font-bold whitespace-nowrap">
          {following
            ? '팔로잉 피드'
            : browsingAll
              ? '모든 동네의 최근 물건'
              : '최근 올라온 물건'}
        </h2>
        <p className="text-sm text-muted">
          {browsingAll ? '로그인하면 내 동네 근처 물건만 보여요' : '최신 등록순'}
        </p>
      </div>
      <ErrorNotice error={me.error} />
      <ErrorNotice error={posts.error} />
      {me.isPending ? (
        <Loading variant="cards" />
      ) : posts.isPending ? (
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
  )
}
