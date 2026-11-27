import type { Post } from '../../types/post'
import { PostCard } from './PostCard'
import { Empty } from '../common/Feedback'
export function PostGrid({ posts }: { posts: Post[] }) {
  return posts.length ? (
    <div className="product-grid">
      {posts.map((post) => (
        <PostCard key={post.id} post={post} />
      ))}
    </div>
  ) : (
    <Empty title="아직 판매글이 없어요">
      다른 동네를 둘러보거나 첫 판매글을 올려 보세요.
    </Empty>
  )
}
export function Pagination({
  page,
  total,
  pageSize = 20,
  onChange,
}: {
  page: number
  total: number
  pageSize?: number
  onChange: (page: number) => void
}) {
  return total > pageSize ? (
    <nav
      aria-label="페이지 이동"
      className="mt-6 flex items-center justify-center gap-4"
    >
      <button
        className="btn-secondary"
        disabled={page <= 1}
        onClick={() => onChange(page - 1)}
      >
        이전
      </button>
      <span className="text-sm">
        {page} / {Math.ceil(total / pageSize)}
      </span>
      <button
        className="btn-secondary"
        disabled={page * pageSize >= total}
        onClick={() => onChange(page + 1)}
      >
        다음
      </button>
    </nav>
  ) : null
}
