import mark from '../../assets/gazi-mark.svg'
export function Avatar({
  name,
  large = false,
}: {
  name: string
  large?: boolean
}) {
  return (
    <span
      className={`flex shrink-0 items-center justify-center rounded-full bg-brand-soft ${large ? 'size-20' : 'size-10'}`}
    >
      <img
        src={mark}
        alt={`${name}의 기본 프로필`}
        className={large ? 'h-12 w-9' : 'h-7 w-5'}
      />
    </span>
  )
}
