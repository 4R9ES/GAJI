import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useNavigate } from 'react-router'
import { useQueryClient } from '@tanstack/react-query'
import { authApi } from '../api/auth'
import { useAction } from '../hooks/useApi'
import { ErrorNotice, Field } from '../components/common/Feedback'
import { LocationSelect } from '../components/common/LocationSelect'
import { validatePassword } from '../types/validation'
import type { Me } from '../types/user'
export function AuthPage({ signup = false }: { signup?: boolean }) {
  const [province, setProvince] = useState('')
  const [locationId, setLocationId] = useState('')
  const [error, setError] = useState('')
  const navigate = useNavigate()
  const cache = useQueryClient()
  const action = useAction(
    async (form: FormData): Promise<Me> => {
      const email = String(form.get('email'))
      const password = String(form.get('password'))
      return signup
        ? authApi.signup({
            email,
            password,
            loginId: String(form.get('loginId')),
            name: String(form.get('name')),
            locationId,
            bio: String(form.get('bio') || ''),
            account: String(form.get('account') || ''),
          })
        : authApi.login({ email, password })
    },
    (me) => {
      cache.clear()
      cache.setQueryData(['me'], me)
      navigate('/')
    },
  )
  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    const form = new FormData(event.currentTarget)
    if (
      signup &&
      (!validatePassword(form.get('password')) ||
        form.get('password') !== form.get('confirmPassword'))
    ) {
      setError(
        '비밀번호는 영문·숫자 포함 8자 이상이며 확인란과 일치해야 합니다.',
      )
      return
    }
    action.mutate(form)
  }
  return (
    <div className="mx-auto max-w-md pt-4 sm:pt-8">
      <h1 className="mt-3 text-3xl font-bold">
        {signup ? '가지 회원가입' : '가지 로그인'}
      </h1>
      <p className="mt-3 mb-7 text-sm text-muted">
        {signup
          ? '가입하고 우리 동네 물건을 거래해 보세요.'
          : '이메일과 비밀번호로 로그인하세요.'}
      </p>
      <form onSubmit={submit} className="space-y-5">
        {signup && (
          <Field label="아이디" hint="가입 후 변경할 수 없습니다. 최대 20자.">
            <input
              name="loginId"
              required
              maxLength={20}
              autoComplete="username"
            />
          </Field>
        )}
        <Field label="이메일">
          <input
            name="email"
            type="email"
            required
            maxLength={254}
            autoComplete={signup ? 'email' : 'username'}
            placeholder="hello@example.com"
          />
        </Field>
        <Field
          label="비밀번호"
          hint={signup ? '영문·숫자 포함 8자 이상' : undefined}
        >
          <input
            name="password"
            type="password"
            required
            minLength={signup ? 8 : undefined}
            autoComplete={signup ? 'new-password' : 'current-password'}
          />
        </Field>
        {signup && (
          <>
            <Field label="비밀번호 확인">
              <input
                name="confirmPassword"
                type="password"
                required
                autoComplete="new-password"
              />
            </Field>
            <Field label="이름">
              <input name="name" required maxLength={50} autoComplete="name" />
            </Field>
            <fieldset>
              <legend className="mb-2 text-sm font-medium">내 동네</legend>
              <LocationSelect
                value={locationId}
                province={province}
                onChange={setLocationId}
                onProvinceChange={setProvince}
                required
              />
            </fieldset>
            <Field label="소개 (선택)">
              <textarea
                name="bio"
                maxLength={300}
                placeholder="이웃에게 나를 소개해 주세요."
              />
            </Field>
            <Field label="계좌 (선택)" hint="본인 정보 화면에만 표시됩니다.">
              <input name="account" maxLength={30} />
            </Field>
          </>
        )}
        <ErrorNotice error={error ? new Error(error) : action.error} />
        <button className="btn w-full" disabled={action.isPending}>
          {action.isPending ? '처리 중…' : signup ? '가지 시작하기' : '로그인'}
        </button>
        <p className="text-center text-sm text-muted">
          {signup ? '이미 이웃이신가요?' : '아직 가지 계정이 없나요?'}{' '}
          <Link
            className="font-semibold text-brand underline"
            to={signup ? '/login' : '/signup'}
          >
            {signup ? '로그인' : '회원가입'}
          </Link>
        </p>
      </form>
      {!signup && import.meta.env.VITE_USE_MOCK === 'true' && (
        <div className="mt-5 border-t border-line py-5 text-sm">
          <p className="font-semibold text-ink">먼저 둘러보고 싶다면</p>
          <p className="mt-2 text-muted">demo@gazi.local / Gazi1234</p>
          <p className="mt-2 text-sm text-muted">
            목 전용 계정입니다. 새로고침하면 데이터가 초기화됩니다. 실제
            비밀번호는 입력하지 마세요.
          </p>
        </div>
      )}
    </div>
  )
}
