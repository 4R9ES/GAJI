// @vitest-environment jsdom
import { afterEach, describe, expect, it } from 'vitest'
import { cleanup, render, screen, within } from '@testing-library/react'
import { Loading } from './Loading'

afterEach(cleanup)

describe('loading accessibility', () => {
  it.each([
    ['page', '화면을'],
    ['cards', '상품 목록을'],
    ['detail', '상품 정보를'],
    ['profile', '프로필을'],
    ['comments', '댓글을'],
    ['connections', '이웃 목록을'],
    ['deals', '거래 내역을'],
    ['form', '판매글 수정 화면을'],
    ['fields', '구매자 정보를'],
    ['account', '계정 정보를'],
  ] as const)(
    '%s announces its purpose without exposing fake controls',
    (variant, message) => {
      render(<Loading variant={variant} />)
      const status = screen.getByRole('status')
      expect(status.getAttribute('aria-busy')).toBe('true')
      expect(status.textContent).toContain(message)
      expect(status.querySelector('[aria-hidden="true"]')).toBeTruthy()
      expect(within(status).queryByRole('button')).toBeNull()
      expect(within(status).queryByRole('textbox')).toBeNull()
      expect(within(status).queryByRole('link')).toBeNull()
    },
  )

  it('uses the responsive product grid and animates only when motion is allowed', () => {
    const { container } = render(<Loading variant="cards" />)
    expect(container.querySelector('.product-grid')?.children).toHaveLength(6)
    expect(container.querySelector('.motion-safe\\:animate-pulse')).toBeTruthy()
    expect(container.querySelector('.animate-pulse')).toBeNull()
  })
})
