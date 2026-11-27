import { useLocations } from '../../hooks/useApi'
import { ErrorNotice } from './Feedback'
import { Icon } from './Icon'
export function LocationSelect({
  value,
  province: selectedProvince,
  onChange,
  onProvinceChange,
  required = false,
  bar = false,
}: {
  value: string
  province: string
  onChange: (id: string) => void
  onProvinceChange: (province: string) => void
  required?: boolean
  bar?: boolean
}) {
  const locations = useLocations()
  const provinces = [
    ...new Set(locations.data?.map((location) => location.name.split(' ')[0])),
  ]
  return (
    <div>
      <div className={bar ? 'neighborhood-bar' : 'grid grid-cols-2 gap-2'}>
        {bar && <Icon name="pin" className="size-5 shrink-0 text-brand" />}
        <select
          aria-label="시/도"
          value={selectedProvince}
          required={required}
          disabled={!locations.data}
          onChange={(event) => {
            onProvinceChange(event.target.value)
            onChange('')
          }}
        >
          <option value="">시/도 선택</option>
          {provinces.map((province) => (
            <option key={province}>{province}</option>
          ))}
        </select>
        <select
          aria-label="시/군/구"
          required={required}
          value={value}
          disabled={!selectedProvince || !locations.data}
          onChange={(event) => onChange(event.target.value)}
        >
          <option value="">시/군/구 선택</option>
          {locations.data
            ?.filter(
              (location) => location.name.split(' ')[0] === selectedProvince,
            )
            .map((location) => (
              <option key={location.id} value={location.id}>
                {location.name.split(' ').slice(1).join(' ') || location.name}
              </option>
            ))}
        </select>
      </div>
      <ErrorNotice error={locations.error} />
    </div>
  )
}
