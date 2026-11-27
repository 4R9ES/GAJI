type LoadingVariant =
  | 'page'
  | 'cards'
  | 'detail'
  | 'profile'
  | 'comments'
  | 'connections'
  | 'deals'
  | 'form'
  | 'fields'
  | 'account'

const labels: Record<LoadingVariant, string> = {
  page: '화면을 불러오는 중입니다.',
  cards: '상품 목록을 불러오는 중입니다.',
  detail: '상품 정보를 불러오는 중입니다.',
  profile: '프로필을 불러오는 중입니다.',
  comments: '댓글을 불러오는 중입니다.',
  connections: '이웃 목록을 불러오는 중입니다.',
  deals: '거래 내역을 불러오는 중입니다.',
  form: '판매글 수정 화면을 불러오는 중입니다.',
  fields: '구매자 정보를 불러오는 중입니다.',
  account: '계정 정보를 확인하는 중입니다.',
}

function Bone({ className }: { className: string }) {
  return <div className={`bg-paper-3 ${className}`} />
}

function Cards() {
  return (
    <div className="product-grid">
      {Array.from({ length: 6 }, (_, index) => (
        <div key={index} className="min-w-0">
          <Bone className="aspect-square w-full rounded-card" />
          <Bone className="mt-4 h-5 w-4/5 rounded" />
          <Bone className="mt-3 h-3 w-3/5 rounded" />
          <Bone className="mt-3 h-5 w-2/5 rounded" />
          <div className="mt-3 flex justify-between gap-3">
            <Bone className="h-3 w-1/3 rounded" />
            <Bone className="h-3 w-8 rounded" />
          </div>
        </div>
      ))}
    </div>
  )
}

function Person() {
  return (
    <div className="flex items-center gap-3 py-4">
      <Bone className="size-10 shrink-0 rounded-full" />
      <div className="min-w-0 flex-1 space-y-3">
        <Bone className="h-4 w-28 max-w-full rounded" />
        <Bone className="h-3 w-40 max-w-full rounded" />
      </div>
    </div>
  )
}

function Fields() {
  return (
    <div className="space-y-6">
      {[0, 1].map((index) => (
        <div key={index} className="space-y-2">
          <Bone className="h-4 w-24 rounded" />
          <Bone className="h-11 w-full rounded-control" />
        </div>
      ))}
      <Bone className="h-11 w-full rounded-control" />
    </div>
  )
}

function Shape({ variant }: { variant: LoadingVariant }) {
  switch (variant) {
    case 'cards':
      return <Cards />
    case 'account':
      return (
        <div className="flex h-11 items-center gap-3">
          <Bone className="size-10 rounded-full" />
          <Bone className="h-4 w-20 rounded" />
        </div>
      )
    case 'profile':
      return (
        <div>
          <div className="flex items-center gap-5 py-6">
            <Bone className="size-20 shrink-0 rounded-full" />
            <div className="min-w-0 flex-1 space-y-3">
              <Bone className="h-3 w-32 rounded" />
              <Bone className="h-7 w-44 max-w-full rounded" />
              <Bone className="h-3 w-24 rounded" />
            </div>
          </div>
          <Bone className="mt-3 h-4 w-3/4 rounded" />
          <Bone className="mt-7 h-5 w-2/3 rounded" />
          <div className="mt-12">
            <Cards />
          </div>
        </div>
      )
    case 'detail':
      return (
        <div>
          <Bone className="mb-5 h-4 w-36 rounded" />
          <div className="grid gap-7 lg:grid-cols-[1.1fr_1fr]">
            <div>
              <Bone className="aspect-square w-full rounded-card" />
              <Person />
            </div>
            <div className="space-y-6 py-2">
              <Bone className="h-6 w-36 rounded" />
              <Bone className="h-8 w-4/5 rounded" />
              <Bone className="h-3 w-1/2 rounded" />
              <Bone className="h-9 w-36 rounded" />
              <div className="space-y-3 border-t border-line pt-6">
                <Bone className="h-4 w-full rounded" />
                <Bone className="h-4 w-full rounded" />
                <Bone className="h-4 w-2/3 rounded" />
              </div>
              <Bone className="h-11 w-32 rounded-control" />
            </div>
          </div>
        </div>
      )
    case 'comments':
    case 'connections':
      return (
        <div className="divide-y divide-line">
          {[0, 1, 2].map((index) => (
            <div key={index}>
              <Person />
              {variant === 'comments' && (
                <Bone className="mb-5 ml-13 h-4 w-2/3 rounded" />
              )}
            </div>
          ))}
        </div>
      )
    case 'deals':
      return (
        <div className="divide-y divide-line">
          {[0, 1].map((index) => (
            <div key={index} className="space-y-4 py-6">
              <Bone className="h-5 w-2/3 rounded" />
              <Bone className="h-4 w-36 rounded" />
              <Bone className="h-3 w-48 rounded" />
              <Bone className="h-3 w-32 rounded" />
            </div>
          ))}
        </div>
      )
    case 'form':
      return (
        <div className="mx-auto max-w-2xl space-y-6">
          <Bone className="h-4 w-24 rounded" />
          <Bone className="h-8 w-40 rounded" />
          <Bone className="h-44 w-full rounded-card" />
          <Fields />
          <Bone className="h-44 w-full rounded-control" />
        </div>
      )
    case 'fields':
      return <Fields />
    default:
      return (
        <div className="space-y-6 py-6">
          <Bone className="h-8 w-48 rounded" />
          <Bone className="h-4 w-3/4 rounded" />
          <Bone className="h-4 w-1/2 rounded" />
          <Bone className="h-40 w-full rounded-card" />
        </div>
      )
  }
}

export function Loading({ variant = 'page' }: { variant?: LoadingVariant }) {
  return (
    <div role="status" aria-live="polite" aria-busy="true">
      <span className="sr-only">{labels[variant]}</span>
      <div aria-hidden="true" className="motion-safe:animate-pulse">
        <Shape variant={variant} />
      </div>
    </div>
  )
}
