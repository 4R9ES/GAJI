import { Loading } from './components/common/Loading'
import { Navigate, Outlet, Route, Routes, useLocation } from 'react-router'
import { Layout } from './components/common/Layout'
import { Empty, ErrorNotice } from './components/common/Feedback'
import { useMe } from './hooks/useApi'
import { FeedPage } from './pages/FeedPage'
import { AuthPage } from './pages/AuthPage'
import { PostDetailPage } from './pages/PostDetailPage'
import { PostEditorPage } from './pages/PostEditorPage'
import {
  ProfilePage,
  ConnectionsPage,
  MyPage,
  PasswordPage,
} from './pages/ProfilePage'
function Protected() {
  const me = useMe()
  const location = useLocation()
  if (me.isPending) return <Loading />
  if (me.error) return <ErrorNotice error={me.error} />
  return me.data ? (
    <Outlet />
  ) : (
    <Navigate to="/login" replace state={{ from: location.pathname }} />
  )
}
export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<FeedPage />} />
        <Route path="login" element={<AuthPage key="login" />} />
        <Route path="signup" element={<AuthPage key="signup" signup />} />
        <Route path="posts/:postId" element={<PostDetailPage />} />
        <Route path="users/:loginId" element={<ProfilePage />} />
        <Route
          path="users/:loginId/followers"
          element={<ConnectionsPage kind="followers" />}
        />
        <Route
          path="users/:loginId/following"
          element={<ConnectionsPage kind="following" />}
        />
        <Route element={<Protected />}>
          <Route
            path="following"
            element={<FeedPage key="following" following />}
          />
          <Route path="posts/new" element={<PostEditorPage key="new" />} />
          <Route path="posts/:postId/edit" element={<PostEditorPage />} />
          <Route path="me" element={<MyPage />} />
          <Route path="me/password" element={<PasswordPage />} />
        </Route>
        <Route
          path="*"
          element={
            <Empty title="페이지를 찾을 수 없어요">
              주소를 확인하거나 메뉴에서 이동해 주세요.
            </Empty>
          }
        />
      </Route>
    </Routes>
  )
}
