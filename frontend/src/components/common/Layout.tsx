import { Link, NavLink, Outlet, useNavigate } from 'react-router'
import { useQueryClient } from '@tanstack/react-query'
import wordmark from '../../assets/gazi-wordmark.svg'
import { authApi } from '../../api/auth'
import { useAction, useMe } from '../../hooks/useApi'
import { Avatar } from './Avatar'
import { Loading } from './Loading'
import { ErrorNotice } from './Feedback'
import { Icon } from './Icon'
export function Layout() {
  const { data: me, isPending: isCheckingAccount } = useMe()
  const cache = useQueryClient()
  const navigate = useNavigate()
  const logout = useAction(
    () => authApi.logout(),
    () => {
      cache.clear()
      cache.setQueryData(['me'], null)
      navigate('/')
    },
  )
  const nav = [
    { to: '/', label: '우리 동네', icon: 'home' as const },
    { to: '/following', label: '팔로잉', icon: 'users' as const },
    { to: '/posts/new', label: '판매하기', icon: 'plus' as const },
    { to: '/me', label: '나의 가지', icon: 'user' as const },
  ]
  return (
    <>
      <a
        href="#main"
        className="sr-only focus:not-sr-only focus:fixed focus:top-2 focus:left-2 focus:z-50 focus:bg-surface focus:p-3"
      >
        본문 바로가기
      </a>
      <header className="sticky top-0 z-30 border-b border-line bg-surface">
        <div className="mx-auto flex h-18 max-w-7xl items-center gap-8 px-5 lg:px-10">
          <Link
            to="/"
            aria-label="가지 홈"
            className="flex min-h-11 shrink-0 items-center"
          >
            <img
              src={wordmark}
              alt="가지"
              width={80}
              height={40}
              className="h-10 w-20 object-contain"
            />
          </Link>
          <nav aria-label="주 메뉴" className="hidden h-full gap-6 md:flex">
            {nav
              .filter((item) => item.to !== '/posts/new')
              .map((item) => (
                <NavLink key={item.to} to={item.to} end className="top-link">
                  {item.label}
                </NavLink>
              ))}
          </nav>
          <div className="ml-auto flex items-center gap-3">
            <Link to="/posts/new" className="btn hidden md:inline-flex">
              <Icon name="plus" className="size-4" />
              판매하기
            </Link>
            {isCheckingAccount ? (
              <Loading variant="account" />
            ) : me ? (
              <>
                <Link to="/me" className="flex items-center gap-2 text-sm">
                  <Avatar name={me.name} />
                  <span className="hidden lg:block">{me.name}</span>
                </Link>
                <button
                  type="button"
                  onClick={() => logout.mutate()}
                  disabled={logout.isPending}
                  className="text-sm text-muted hover:text-ink"
                >
                  로그아웃
                </button>
              </>
            ) : (
              <>
                <Link
                  to="/login"
                  className="flex min-h-11 items-center text-sm font-medium"
                >
                  로그인
                </Link>
                <Link to="/signup" className="btn-secondary">
                  회원가입
                </Link>
              </>
            )}
          </div>
        </div>
      </header>
      <main
        id="main"
        className="mx-auto max-w-7xl px-5 pt-8 pb-16 md:pt-12 lg:px-10"
      >
        <ErrorNotice error={logout.error} />
        <Outlet />
      </main>
      <footer className="mx-auto mb-24 max-w-7xl px-5 md:mb-0 lg:px-10">
        <div className="flex flex-wrap items-center justify-between gap-x-6 gap-y-2 border-t border-line py-6 text-sm text-muted">
          <p>물건을 보고, 이웃을 알아가요.</p>
          {import.meta.env.VITE_USE_MOCK === 'true' && (
            <>
              <p>로컬 데모용 목 데이터라 새로고침하면 초기화됩니다.</p>
              <p>
                상품 사진은 데모용 이미지입니다.{' '}
                <a
                  href="/photo-credits.html"
                  className="underline underline-offset-4"
                >
                  사진 출처
                </a>
              </p>
            </>
          )}
        </div>
      </footer>
      <nav
        aria-label="하단 메뉴"
        className="fixed inset-x-0 bottom-0 z-30 flex justify-around border-t border-line bg-surface px-2 pt-2 pb-[max(0.5rem,env(safe-area-inset-bottom))] md:hidden"
      >
        {nav.map((item) => (
          <NavLink
            to={item.to}
            key={item.to}
            end
            className={({ isActive }) =>
              `flex min-h-14 flex-1 flex-col items-center justify-center gap-1 text-xs ${isActive ? 'font-semibold text-brand' : 'text-muted'}`
            }
          >
            <Icon name={item.icon} />
            {item.label}
          </NavLink>
        ))}
      </nav>
    </>
  )
}
