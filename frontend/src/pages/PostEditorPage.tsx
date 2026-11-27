import { Loading } from '../components/common/Loading'
import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { useQuery } from '@tanstack/react-query'
import { postsApi } from '../api/posts'
import { useAction, useMe } from '../hooks/useApi'
import { ErrorNotice, Field } from '../components/common/Feedback'
import { conditionLabels } from '../components/posts/labels'
import { Icon } from '../components/common/Icon'
import { IMAGE_TYPES, MAX_IMAGE_SIZE, validateMoney } from '../types/validation'
import type { Post, PostCondition, PostInput } from '../types/post'
export function PostEditorPage() {
  const { postId } = useParams()
  const me = useMe()
  const post = useQuery({
    queryKey: ['post', postId],
    queryFn: () => postsApi.detail(postId!),
    enabled: !!postId,
  })
  if (postId && post.isPending) return <Loading variant="form" />
  if (post.error) return <ErrorNotice error={post.error} />
  if (
    post.data &&
    (post.data.seller.loginId !== me.data?.loginId ||
      post.data.status === 'SOLD')
  )
    return <ErrorNotice error={new Error('수정할 수 없는 판매글입니다.')} />
  return <PostForm key={postId || 'new'} post={post.data} />
}
function PostForm({ post }: { post?: Post }) {
  const navigate = useNavigate()
  const [image, setImage] = useState<File>()
  const [preview, setPreview] = useState(post?.contentImgUrl || '')
  const [error, setError] = useState('')
  const [titleLength, setTitleLength] = useState(post?.title.length || 0)
  const [contentLength, setContentLength] = useState(post?.content.length || 0)
  useEffect(
    () => () => {
      if (preview.startsWith('blob:')) URL.revokeObjectURL(preview)
    },
    [preview],
  )
  const action = useAction(
    (input: PostInput) =>
      post ? postsApi.update(post.id, input) : postsApi.create(input),
    (result) => navigate(`/posts/${result.id}`),
  )
  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    const form = new FormData(event.currentTarget)
    const price = Number(form.get('price'))
    if (!validateMoney(price)) {
      setError('가격은 0 이상의 안전한 정수로 입력해 주세요.')
      return
    }
    if (!post && !image) {
      setError('사진 한 장을 선택해 주세요.')
      return
    }
    action.mutate({
      title: String(form.get('title')).trim(),
      content: String(form.get('content')).trim(),
      price,
      condition: String(form.get('condition')) as PostCondition,
      image,
    })
  }
  return (
    <div className="mx-auto max-w-5xl">
      <Link to={post ? `/posts/${post.id}` : '/'} className="back-link">
        돌아가기
      </Link>
      <h1 className="mt-4 text-3xl font-bold">
        {post ? '판매글 수정' : '판매글 등록'}
      </h1>
      <p className="mt-3 mb-7 text-sm text-muted">
        사진과 상품 상태를 정확히 알려주세요.
      </p>
      <form
        onSubmit={submit}
        className="grid gap-x-12 gap-y-6 lg:grid-cols-[minmax(0,5fr)_minmax(0,7fr)]"
      >
        <div className="space-y-4">
          <Field
            label="상품 사진 1장"
            hint="JPEG·PNG·WebP / 최대 5MB. 새 사진을 선택하면 기존 사진을 교체합니다."
          >
            <div className="photo-drop">
              {preview ? (
                <img
                  src={preview}
                  alt="판매글 사진 미리보기"
                  className="size-full object-contain"
                />
              ) : (
                <span className="flex flex-col items-center gap-3 text-muted">
                  <span className="flex size-14 items-center justify-center rounded-full bg-brand-soft text-brand">
                    <Icon name="plus" className="size-6" />
                  </span>
                  <span className="text-base font-medium text-ink">
                    사진 고르기
                  </span>
                  <span>눌러서 고르거나 여기로 끌어다 놓으세요.</span>
                </span>
              )}
              <input
                type="file"
                accept="image/jpeg,image/png"
                onChange={(event) => {
                  const file = event.target.files?.[0]
                  if (!file) return
                  if (
                    !IMAGE_TYPES.includes(file.type) ||
                    file.size === 0 ||
                    file.size > MAX_IMAGE_SIZE
                  ) {
                    setError('JPEG·PNG·WebP, 5MB 이하의 사진을 선택해 주세요.')
                    event.target.value = ''
                    return
                  }
                  setError('')
                  setImage(file)
                  setPreview(URL.createObjectURL(file))
                }}
              />
            </div>
          </Field>
        </div>
        <div className="space-y-6">
          <Field
            label="제목"
            hint={
              <span className="block text-right tabular-nums">
                {titleLength}/100
              </span>
            }
          >
            <input
              name="title"
              required
              maxLength={100}
              defaultValue={post?.title}
              placeholder="어떤 물건인가요?"
              className="title-field"
              onInput={(event) =>
                setTitleLength(event.currentTarget.value.length)
              }
            />
          </Field>
          <Field label="판매 가격 (원)">
            <span className="price-field">
              <input
                name="price"
                type="number"
                inputMode="numeric"
                min={0}
                step={1}
                max={Number.MAX_SAFE_INTEGER}
                required
                defaultValue={post?.price}
                placeholder="0"
              />
              <span aria-hidden="true">원</span>
            </span>
          </Field>
          <fieldset>
            <legend className="mb-2 text-sm font-medium">상품 상태</legend>
            <div className="segmented">
              {Object.entries(conditionLabels).map(([value, label]) => (
                <label key={value}>
                  <input
                    type="radio"
                    name="condition"
                    value={value}
                    required
                    defaultChecked={(post?.condition || 'USED') === value}
                  />
                  <span>{label}</span>
                </label>
              ))}
            </div>
          </fieldset>
          <Field
            label="상품 설명"
            hint={
              <span className="flex justify-between gap-4">
                <span>사용 기간과 상태, 거래 방법을 알려주세요.</span>
                <span className="shrink-0 tabular-nums">
                  {contentLength}/1000
                </span>
              </span>
            }
          >
            <textarea
              name="content"
              required
              maxLength={1000}
              defaultValue={post?.content}
              placeholder={
                '예) 1년 정도 사용했어요.\n모서리에 작은 흠집이 있어요.\n평일 저녁 동네에서 직접 거래하고 싶어요.'
              }
              onInput={(event) =>
                setContentLength(event.currentTarget.value.length)
              }
              className="content-field"
            />
          </Field>
          <ErrorNotice error={error ? new Error(error) : action.error} />
          <div className="flex justify-end gap-2">
            <Link
              to={post ? `/posts/${post.id}` : '/'}
              className="btn-secondary"
            >
              취소
            </Link>
            <button className="btn" disabled={action.isPending}>
              {action.isPending
                ? '저장 중…'
                : post
                  ? '수정 완료'
                  : '판매글 등록'}
            </button>
          </div>
        </div>
      </form>
    </div>
  )
}
