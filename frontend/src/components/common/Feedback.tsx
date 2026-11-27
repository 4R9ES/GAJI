import type { ReactNode } from 'react'
import mark from '../../assets/gazi-mark.svg'
import { errorMessage } from '../../api/client'
export function ErrorNotice({ error }: { error: unknown }) {
  return error ? (
    <p
      role="alert"
      className="rounded-xl bg-danger-soft p-3 text-sm text-danger"
    >
      {errorMessage(error)}
    </p>
  ) : null
}
export function Empty({
  title = '아직 비어 있어요',
  children,
}: {
  title?: string
  children?: ReactNode
}) {
  return (
    <div className="flex min-h-52 flex-col items-center justify-center gap-3 rounded-card bg-paper-2 px-5 py-8 text-center">
      <img src={mark} alt="" width={28} height={44} className="mb-1 h-11 w-7" />
      <h2 className="font-semibold">{title}</h2>
      <div className="text-sm text-muted">
        {children || '이웃과 새로운 연결을 시작해 보세요.'}
      </div>
    </div>
  )
}
export function Field({
  label,
  children,
  hint,
}: {
  label: string
  children: ReactNode
  hint?: ReactNode
}) {
  return (
    <label className="flex flex-col gap-2 text-sm font-medium">
      <span>{label}</span>
      {children}
      {hint && (
        <span className="min-h-lh text-sm font-normal text-muted">{hint}</span>
      )}
    </label>
  )
}
