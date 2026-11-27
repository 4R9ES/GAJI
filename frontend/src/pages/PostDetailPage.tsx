import { Loading } from '../components/common/Loading'
import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { useQuery } from '@tanstack/react-query'
import { postsApi } from '../api/posts'
import { commentsApi } from '../api/comments'
import { dealsApi } from '../api/deals'
import { useAction, useMe } from '../hooks/useApi'
import { Avatar } from '../components/common/Avatar'
import { ErrorNotice, Field } from '../components/common/Feedback'
import { Modal } from '../components/common/Modal'
import { Icon } from '../components/common/Icon'
import { conditionLabels, statusLabels } from '../components/posts/labels'
import type { Post } from '../types/post'
import type { Comment } from '../types/comment'
import type { CreateDealRequest } from '../types/deal'
import { validateMoney } from '../types/validation'
export function PostDetailPage() {
  const { postId = '' } = useParams()
  const me = useMe()
  const navigate = useNavigate()
  const [showDeal, setShowDeal] = useState(false)
  const [comment, setComment] = useState('')
  const post = useQuery({
    queryKey: ['post', postId],
    queryFn: () => postsApi.detail(postId),
  })
  const comments = useQuery({
    queryKey: ['comments', postId],
    queryFn: () => commentsApi.list(postId),
  })
  const remove = useAction(
    () => postsApi.remove(postId),
    () => navigate('/'),
  )
  const status = useAction((value: 'ON_SALE' | 'RESERVED') =>
    postsApi.status(postId, value),
  )
  const like = useAction(() =>
    postsApi.like(postId, post.data?.isLiked || false),
  )
  const addComment = useAction(
    (content: string) => commentsApi.create(postId, content),
    () => setComment(''),
  )
  const [replyTo, setReplyTo] = useState<string | null>(null)
  const [reply, setReply] = useState('')
  const addReply = useAction(
    (input: { content: string; parentId: string }) =>
      commentsApi.create(postId, input.content),
    () => {
      setReply('')
      setReplyTo(null)
    },
  )
  const deleteComment = useAction((id: string) => commentsApi.remove(id))
  if (post.isPending) return <Loading variant="detail" />
  if (post.error) return <ErrorNotice error={post.error} />
  const item = post.data!
  const mine = me.data?.loginId === item.seller.loginId
  function submitComment(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (comment.trim()) addComment.mutate(comment.trim())
  }
  return (
    <>
      <Link to="/" className="back-link">
        우리 동네로 돌아가기
      </Link>
      <div className="mt-4 grid gap-x-14 gap-y-10 lg:grid-cols-[minmax(0,7fr)_minmax(0,5fr)]">
        <img
          src={item.contentImgUrl}
          alt={item.title}
          width={800}
          height={800}
          fetchPriority="high"
          className="aspect-square w-full rounded-card bg-paper-2 object-cover"
        />
        <aside className="lg:sticky lg:top-24 lg:row-span-2 lg:self-start">
          <div className="flex gap-2">
            <span className="rounded-lg bg-leaf-soft px-3 py-1 text-sm font-semibold text-leaf-dark">
              {statusLabels[item.status]}
            </span>
            <span className="rounded-lg bg-brand-soft px-3 py-1 text-sm text-brand">
              {conditionLabels[item.condition]}
            </span>
          </div>
          <h1 className="mt-5 text-3xl leading-tight font-bold">
            {item.title}
          </h1>
          <p className="mt-3 flex flex-wrap gap-x-4 text-sm text-muted">
            <span>{item.seller.location.name}</span>
            {item.createdAt && (
              <time dateTime={item.createdAt}>
                {new Date(item.createdAt).toLocaleDateString('ko-KR')}
              </time>
            )}
          </p>
          <p className="mt-6 font-display text-4xl font-bold tabular-nums">
            {item.price.toLocaleString('ko-KR')}
            <span className="ml-1 text-xl">원</span>
          </p>
          <div className="mt-7 flex flex-wrap items-center gap-2">
            {me.data ? (
              <button
                type="button"
                className={item.isLiked ? 'btn' : 'btn-secondary'}
                aria-pressed={item.isLiked}
                disabled={like.isPending}
                onClick={() => like.mutate()}
              >
                <Icon name="heart" />
                {item.isLiked ? '찜했어요' : '찜하기'} {item.likesCount}
              </button>
            ) : (
              <Link to="/login" className="btn-secondary">
                <Icon name="heart" />
                로그인하고 찜하기 {item.likesCount}
              </Link>
            )}
            {mine && item.status !== 'SOLD' && (
              <>
                <Link to={`/posts/${item.id}/edit`} className="btn-secondary">
                  수정
                </Link>
                <button
                  type="button"
                  className="btn-secondary text-danger"
                  disabled={remove.isPending}
                  onClick={() => {
                    if (
                      window.confirm(
                        '판매글을 삭제할까요? 댓글과 찜도 함께 삭제됩니다.',
                      )
                    )
                      remove.mutate()
                  }}
                >
                  삭제
                </button>
              </>
            )}
          </div>
          {mine && item.status !== 'SOLD' && (
            <div className="mt-6 border-t border-line pt-5">
              <p className="mb-3 text-sm font-semibold">판매 상태 관리</p>
              <div className="flex flex-wrap gap-2">
                <button
                  type="button"
                  className="btn-secondary"
                  disabled={status.isPending}
                  onClick={() =>
                    status.mutate(
                      item.status === 'RESERVED' ? 'ON_SALE' : 'RESERVED',
                    )
                  }
                >
                  {item.status === 'RESERVED'
                    ? '판매중으로 변경'
                    : '예약중으로 변경'}
                </button>
                <button
                  type="button"
                  className="btn"
                  onClick={() => setShowDeal(true)}
                >
                  거래 완료
                </button>
              </div>
            </div>
          )}
          {item.status === 'SOLD' && (
            <p className="mt-5 rounded-xl bg-brand-soft p-4 text-sm text-brand">
              이 물건은 새로운 이웃을 만났어요. 거래 기록을 위해 글을
              보관합니다.
            </p>
          )}
          <div className="mt-3 space-y-2">
            <ErrorNotice error={like.error} />
            <ErrorNotice error={remove.error} />
            <ErrorNotice error={status.error} />
          </div>
          <Link
            to={`/users/${encodeURIComponent(item.seller.loginId)}`}
            className="mt-6 flex items-center gap-3 border-t border-line pt-5 hover:[&_.seller-name]:underline"
          >
            <Avatar name={item.seller.name} />
            <div className="min-w-0 flex-1">
              <p className="seller-name truncate font-semibold underline-offset-4">
                {item.seller.name}
              </p>
              <p className="mt-1 text-sm text-muted">
                {item.seller.location.name}
              </p>
            </div>
            <div className="text-right">
              <p className="text-lg font-bold text-brand tabular-nums">
                {item.seller.userRank}
                <span className="text-sm"> / 100</span>
              </p>
              <p className="text-sm text-muted">이웃 신뢰도</p>
            </div>
            <Icon name="chevron" className="size-4 text-muted" />
          </Link>
        </aside>
        <div className="min-w-0">
          <p className="max-w-[65ch] leading-8 whitespace-pre-wrap">
            {item.content}
          </p>
          <section className="mt-12 border-t border-line pt-8">
            <h2 className="text-lg font-bold">
              이웃과 이야기{' '}
              <span className="ml-1 text-brand">
                {comments.data?.length || 0}
              </span>
            </h2>
            <p className="mt-2 text-sm text-muted">
              궁금한 점이나 거래 약속을 댓글로 남겨주세요.
            </p>
            <ErrorNotice error={comments.error} />
            {comments.isPending ? (
              <Loading variant="comments" />
            ) : (
              <div className="mt-5 divide-y divide-line">
                {comments.data
                  ?.filter((c) => !c.parentId)
                  .map((c) => {
                    const replies =
                      comments.data?.filter((r) => r.parentId === c.id) ?? []
                    const commentProps = {
                      mine: me.data?.loginId,
                      canReply: false, // backend has no replies
                      deleting: deleteComment.isPending,
                      onDelete: (id: string, hasReplies: boolean) => {
                        if (
                          window.confirm(
                            hasReplies
                              ? '댓글을 삭제할까요? 달린 답장도 함께 삭제됩니다.'
                              : '댓글을 삭제할까요?',
                          )
                        )
                          deleteComment.mutate(id)
                      },
                      onReply: () => {
                        setReplyTo(c.id)
                        setReply('')
                      },
                    }
                    return (
                      <div key={c.id} className="py-4">
                        <CommentItem
                          comment={c}
                          hasReplies={replies.length > 0}
                          {...commentProps}
                        />
                        {replies.length > 0 && (
                          <ul className="mt-2 space-y-1 pl-4 sm:pl-13">
                            {replies.map((r) => (
                              <li key={r.id} className="flex gap-2 pt-3">
                                <Icon
                                  name="reply"
                                  className="mt-2 size-4 shrink-0 text-muted"
                                />
                                <div className="min-w-0 flex-1">
                                  <CommentItem
                                    comment={r}
                                    hasReplies={false}
                                    {...commentProps}
                                  />
                                </div>
                              </li>
                            ))}
                          </ul>
                        )}
                        {replyTo === c.id && (
                          <form
                            onSubmit={(event) => {
                              event.preventDefault()
                              if (reply.trim())
                                addReply.mutate({
                                  content: reply.trim(),
                                  parentId: c.id,
                                })
                            }}
                            className="mt-4 flex gap-2 pl-4 sm:pl-13"
                          >
                            <Icon
                              name="reply"
                              className="mt-3 size-4 shrink-0 text-brand"
                            />
                            <div className="min-w-0 flex-1 space-y-3">
                              <Field
                                label={`${c.author.name}님에게 답장`}
                                hint={
                                  <span className="block text-right tabular-nums">
                                    {reply.length}/300
                                  </span>
                                }
                              >
                                <textarea
                                  value={reply}
                                  onChange={(e) => setReply(e.target.value)}
                                  maxLength={300}
                                  required
                                  autoFocus
                                  placeholder="답장을 입력하세요."
                                  className="content-field min-h-24"
                                />
                              </Field>
                              <ErrorNotice error={addReply.error} />
                              <div className="flex justify-end gap-2">
                                <button
                                  type="button"
                                  className="btn-secondary"
                                  onClick={() => setReplyTo(null)}
                                >
                                  취소
                                </button>
                                <button
                                  className="btn"
                                  disabled={addReply.isPending || !reply.trim()}
                                >
                                  답장 등록
                                </button>
                              </div>
                            </div>
                          </form>
                        )}
                      </div>
                    )
                  })}
                {comments.data?.length === 0 && (
                  <p className="py-6 text-sm text-muted">
                    첫 이야기를 남겨주세요.
                  </p>
                )}
              </div>
            )}
            <ErrorNotice error={deleteComment.error} />
            {me.data ? (
              <form onSubmit={submitComment} className="mt-4 space-y-3">
                <Field
                  label="댓글 남기기"
                  hint={
                    <span className="block text-right tabular-nums">
                      {comment.length}/300
                    </span>
                  }
                >
                  <textarea
                    value={comment}
                    onChange={(e) => setComment(e.target.value)}
                    maxLength={300}
                    required
                    placeholder="이웃에게 궁금한 점을 물어보세요."
                    className="content-field min-h-32"
                  />
                </Field>
                <ErrorNotice error={addComment.error} />
                <div className="text-right">
                  <button
                    className="btn"
                    disabled={addComment.isPending || !comment.trim()}
                  >
                    댓글 등록
                  </button>
                </div>
              </form>
            ) : (
              <Link to="/login" className="btn-secondary mt-5">
                로그인하고 댓글 남기기
              </Link>
            )}
          </section>
        </div>
      </div>
      {showDeal && (
        <CompleteDeal post={item} onClose={() => setShowDeal(false)} />
      )}
    </>
  )
}
function CommentItem({
  comment: c,
  mine,
  canReply,
  hasReplies,
  deleting,
  onDelete,
  onReply,
}: {
  comment: Comment
  mine?: string
  canReply: boolean
  hasReplies: boolean
  deleting: boolean
  onDelete: (id: string, hasReplies: boolean) => void
  onReply: () => void
}) {
  return (
    <article className="flex gap-3">
      <Avatar name={c.author.name} />
      <div className="min-w-0 flex-1">
        <Link
          to={`/users/${encodeURIComponent(c.author.loginId)}`}
          className="text-sm font-semibold"
        >
          {c.author.name}
        </Link>
        <p className="mt-1 text-sm leading-relaxed break-words whitespace-pre-wrap">
          {c.content}
        </p>
        <div className="mt-1 flex flex-wrap items-center gap-x-3 text-sm text-muted">
          {c.createdAt && (
            <time dateTime={c.createdAt}>
              {new Date(c.createdAt).toLocaleString('ko-KR')}
            </time>
          )}
          {canReply && (
            <button type="button" className="comment-action" onClick={onReply}>
              답장하기
            </button>
          )}
          {mine === c.author.loginId && (
            <button
              type="button"
              className="comment-action"
              disabled={deleting}
              onClick={() => onDelete(c.id, hasReplies)}
            >
              삭제
            </button>
          )}
        </div>
      </div>
    </article>
  )
}
function CompleteDeal({ post, onClose }: { post: Post; onClose: () => void }) {
  const [error, setError] = useState('')
  const candidates = useQuery({
    queryKey: ['candidates', post.id],
    queryFn: () => dealsApi.candidates(post.id),
  })
  const action = useAction(
    (input: CreateDealRequest) => dealsApi.complete(post.id, input),
    onClose,
  )
  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = new FormData(event.currentTarget)
    const money = Number(form.get('money'))
    if (!validateMoney(money)) {
      setError('실거래가는 0 이상의 안전한 정수로 입력해 주세요.')
      return
    }
    setError('')
    action.mutate({ buyerLoginId: String(form.get('buyerLoginId')), money })
  }
  return (
    <Modal title="좋은 거래, 잘 마치셨나요?" onClose={onClose}>
      <p className="mb-5 text-sm leading-relaxed text-muted">
        구매자와 실제 거래 금액을 기록합니다. 완료 후에는 판매글을
        수정·삭제하거나 상태를 되돌릴 수 없습니다.
      </p>
      <ErrorNotice error={candidates.error} />
      {candidates.isPending ? (
        <Loading variant="fields" />
      ) : (
        <form onSubmit={submit} className="space-y-5">
          <Field
            label="구매자"
            hint="이 글에 댓글을 남긴 이웃 중에서 선택합니다."
          >
            <select name="buyerLoginId" required defaultValue="">
              <option value="">구매자 선택</option>
              {candidates.data?.map((user) => (
                <option key={user.loginId} value={user.loginId}>
                  {user.name} (@{user.loginId})
                </option>
              ))}
            </select>
          </Field>
          {candidates.data?.length === 0 && (
            <p className="text-sm text-danger">
              선택할 구매자가 없습니다. 구매자가 먼저 댓글을 남겨야 합니다.
            </p>
          )}
          <Field label="실거래가 (원)">
            <input
              name="money"
              type="number"
              required
              min={0}
              step={1}
              max={Number.MAX_SAFE_INTEGER}
              defaultValue={post.price}
            />
          </Field>
          <ErrorNotice error={error ? new Error(error) : action.error} />
          <button
            className="btn w-full"
            disabled={action.isPending || !candidates.data?.length}
          >
            {action.isPending ? '저장 중…' : '판매완료로 기록하기'}
          </button>
        </form>
      )}
    </Modal>
  )
}
