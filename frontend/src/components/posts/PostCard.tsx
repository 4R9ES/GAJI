import { Link } from 'react-router'
import type { Post } from '../../types/post'
import { statusLabels, conditionLabels } from './labels'
import { Icon } from '../common/Icon'
import { postsApi } from '../../api/posts'
import { useAction, useMe } from '../../hooks/useApi'
export function PostCard({ post }: { post: Post }) {
  const me = useMe()
  const like = useAction(() => postsApi.like(post.id, post.isLiked))
  const heart = (
    <>
      <Icon
        name="heart"
        className={`size-4 ${post.isLiked ? 'fill-current' : ''}`}
      />
      <span className="tabular-nums">{post.likesCount}</span>
    </>
  )
  return (
    <div className="product-item">
      <Link to={`/posts/${post.id}`} className="block">
        <div className="relative aspect-square overflow-hidden rounded-card bg-paper-2">
          {post.contentImgUrl ? (
            <img
              src={post.contentImgUrl}
              alt={post.title}
              width={640}
              height={640}
              className={`size-full object-cover ${post.status === 'SOLD' ? 'opacity-60' : ''}`}
            />
          ) : (
            <div className="flex size-full items-center justify-center text-sm text-muted">
              사진 없음
            </div>
          )}
          <span
            className={`absolute top-3 left-3 rounded px-2 py-1 text-xs font-semibold ${post.status === 'ON_SALE' ? 'bg-surface text-brand' : post.status === 'RESERVED' ? 'bg-warm text-warm-ink' : 'bg-ink text-white'}`}
          >
            {statusLabels[post.status]}
          </span>
        </div>
        <div className="pt-3">
          <h3 className="line-clamp-2 text-base leading-relaxed font-medium">
            {post.title}
          </h3>
          <p className="product-meta">{post.seller.location.name}</p>
          <p className="mt-0.5 mb-1 text-sm font-medium text-ink">
            {conditionLabels[post.condition]}
          </p>
          <p className="mt-1 font-display text-lg font-bold tabular-nums">
            {post.price.toLocaleString('ko-KR')}
            <span className="ml-0.5 text-sm">원</span>
          </p>
        </div>
      </Link>
      <div className="flex items-center justify-between gap-2 text-sm text-muted">
        <span className="truncate">{post.seller.name}</span>
        {me.data ? (
          <button
            type="button"
            className={`card-like ${post.isLiked ? 'text-brand' : ''}`}
            aria-pressed={post.isLiked}
            aria-label={`${post.title} ${post.isLiked ? '찜 취소' : '찜하기'}, 찜 ${post.likesCount}`}
            disabled={like.isPending}
            onClick={() => like.mutate()}
          >
            {heart}
          </button>
        ) : (
          <Link
            to="/login"
            className="card-like"
            aria-label={`로그인하고 ${post.title} 찜하기, 찜 ${post.likesCount}`}
          >
            {heart}
          </Link>
        )}
      </div>
    </div>
  )
}
