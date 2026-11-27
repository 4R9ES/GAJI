export const MAX_IMAGE_SIZE = 5 * 1024 * 1024
export const IMAGE_TYPES = ['image/jpeg', 'image/png']
export function validatePassword(value: unknown): value is string {
  return (
    typeof value === 'string' &&
    value.length >= 8 &&
    /[a-zA-Z]/.test(value) &&
    /[0-9]/.test(value)
  )
}
export function validateMoney(value: unknown): value is number {
  return typeof value === 'number' && Number.isSafeInteger(value) && value >= 0
}
