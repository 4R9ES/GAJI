# 가지 · gazi

우리 동네 중고거래와 이웃 관계를 연결하는 대학교 DB 팀 프로젝트의 로컬 프론트엔드.

## 실행

Node.js 24 LTS 권장. 현재 개발 환경은 Node 24.16.0, npm 11.13.0.

```bash
npm ci
cp .env.example .env
npm run dev
```

터미널에 출력된 로컬 주소를 연다. 기본 포트는 5173이며 사용 중이면 Vite가 다른 포트를 선택한다.

`.env`가 이미 있으면 복사 대신 값을 확인한다.

- 목 계정: `demo@gazi.local` / `Gazi1234` (마포구, 판매글 3·5 소유)
- 다른 이웃: `neighbor@gazi.local` / `Gazi1234` (마포구, 판매글 1·2·4 소유)
- 다른 동네: `green@gazi.local` / `Gazi1234` (화성시)
- 비회원은 시/도와 시/군/구를 선택해 목록을 볼 수 있다.
- 목 데이터·가입 계정·로그인은 **새로고침하면 초기화**된다. 실제 개인정보와 비밀번호를 입력하지 않는다.
- 판매글 사진은 로컬에 저장한 Pexels 데모 사진이며 ([출처](docs/ASSET_CREDITS.md)), 등록할 때 실제 JPEG/PNG/WebP 한 장을 선택할 수 있다.
- 현재 인증은 MSW의 메모리 세션 시뮬레이션이다. 실제 서비스의 보안·인증 구현이 아니다.

## 확인 명령

```bash
npm run typecheck
npm run lint
npm run format:check
npm test
npm run build
```

`npm run format`으로 코드와 Tailwind 클래스를 정렬한다. `npm run preview`는 빌드 미리보기이며 실제 API용 개발 프록시는 제공하지 않는다. 실제 API 통합 확인은 `npm run dev`에서 진행한다.

## 실제 API 연결

```dotenv
VITE_USE_MOCK=false
VITE_API_BASE_URL=/api
API_PROXY_TARGET=http://localhost:8080
```

`.env` 변경 후 개발 서버를 다시 시작한다. `/api` 접두사는 유지한 채 프록시한다. 백엔드도 `/api/locations`처럼 제공하거나, 합의 후 `vite.config.ts`에 rewrite를 추가한다. 브라우저 요청을 백엔드 절대 URL로 바꾸면 Vite 프록시를 우회한다.

화면 → TanStack Query 훅 → `src/api` 함수 → axios 인스턴스 → HTTP 순서. MSW는 같은 요청을 가로챈다. `VITE_USE_MOCK=false`일 때 MSW 초기화·목 데이터는 실행하지 않는다.

인증 방식은 미정. 현재 axios는 `withCredentials: true`로 쿠키 전송을 허용한다. 백엔드가 세션 또는 JWT를 선택하면 `src/api/client.ts`, `src/api/auth.ts`에 전송·갱신 처리를 구현한다. JWT 저장 위치·CSRF 정책·비밀번호 변경 후 세션 정책은 백엔드와 추가 합의가 필요하다. 화면 보호만으로 서버 권한 검증을 대체하지 않는다.

## 화면

| 경로                                                     | 기능                                           |
| -------------------------------------------------------- | ---------------------------------------------- |
| `/`                                                      | 동네별 최신 판매글, 시/도·시/군/구 선택        |
| `/login`, `/signup`                                      | 로그인, 가입                                   |
| `/following`                                             | 팔로잉한 회원의 전체 동네 판매글               |
| `/posts/new`, `/posts/:postId/edit`                      | 등록·수정, 상품 상태 선택                      |
| `/posts/:postId`                                         | 사진·가격·상태·판매자·댓글·찜·거래 완료        |
| `/users/:loginId`                                        | 공개 프로필, 신뢰도, 전체 판매글·완료 기록     |
| `/users/:loginId/followers`, `/users/:loginId/following` | 관계 목록                                      |
| `/me`                                                    | 내 정보, 판매글·찜·판매/구매 기록, 구매자 평가 |
| `/me/password`                                           | 비밀번호 변경                                  |

모든 글은 판매글이다. 프로필 사진은 가지 마크를 사용한다. 일반 SNS 글·프로필 수정·검색·카테고리·채팅·알림·거래 후기 기능은 범위에서 제외했다. 구매자 평가는 필수로 포함한다.

## 수동 확인 시나리오

1. 비회원으로 서울특별시 → 마포구 선택. 시/도를 경기도로 바꾸면 하위 선택·이전 목록이 초기화되는지 확인.
2. 데모 계정 로그인 → 상품 3 상세 → 예약중 → 판매중 복귀.
3. 상품 3의 댓글 작성자인 `neighbor`를 구매자로 선택 → 실거래가 수정 → 판매완료. 재완료·수정·삭제·상태 복귀가 차단되는지 확인.
4. 나의 가지 → 판매 내역 → 구매자 1~10점 평가. 평가 후 다시 입력할 수 없는지 확인.
5. 로그아웃 후 neighbor 계정 로그인 → 구매 내역에서 동일한 금액·거래 확인. 새로고침하지 말 것.
6. 찜·취소, 팔로우·해제, 댓글 작성·본인 삭제 및 팔로잉 피드 반영 확인.
7. 사진 한 장으로 판매글 등록 → 상품 상태 NEW/LIKE_NEW/USED 변경 → 상세·카드 반영 확인.
8. 비밀번호 변경 → 로그아웃 → 바뀐 비밀번호로 로그인. 새로고침 시 원래 데모 비밀번호로 초기화됨.
9. 360px 모바일 화면에서 가로 스크롤, 하단 내비게이션, 폼·대화상자와 키보드 포커스 확인.

## 구조

```text
src/
  api/          HTTP·인증 전송 경계, 도메인별 함수
  types/        요청·응답 계약, 공유 검증 규칙
  mocks/        MSW, 메모리 데이터, API 통합 테스트
  pages/        라우트 화면
  components/   common/, posts/
  hooks/        Query·Mutation 연결
  assets/       원본 로고
  styles/       전역 스타일·브랜드 토큰
public/
  images/       데모 상품 일러스트
  mockServiceWorker.js
```

[API 계약 초안](docs/API_SPEC.md)을 백엔드와 먼저 합의한다. ERD 추가 요청과 인증 미확정 사항을 포함했다.

## 패키지 선택

`package-lock.json`에 실제 해석된 버전을 기록했다. 설치 당시 안정 버전을 사용하되, TypeScript는 최신 typescript-eslint의 peer 범위(`<6.1.0`)에 맞춰 **6.0.3**으로 선택했다. Vite 템플릿의 TypeScript 7을 강제 설치하지 않았다. 템플릿 기본 Oxlint는 요청한 ESLint로 교체했다.

- React 19, Vite 8, React Router 8, TanStack Query 5, axios 1
- Tailwind 4: `@tailwindcss/vite` + CSS `@theme`, 별도 v3 설정 파일 없음
- MSW 3: `msw/http`, `onUnhandledFrame` API 사용
- ESLint 10 flat config, Prettier 3, Tailwind 정렬 플러그인, Vitest 5

확인한 공식 설치 문서:
[Vite](https://vite.dev/guide/), [React Router](https://reactrouter.com/start/declarative/installation), [TanStack Query](https://tanstack.com/query/latest/docs/framework/react/installation), [axios](https://axios-http.com/docs/intro), [Tailwind](https://tailwindcss.com/docs/installation/using-vite), [ESLint](https://eslint.org/docs/latest/use/getting-started), [Prettier](https://prettier.io/docs/install), [Tailwind 정렬 플러그인](https://github.com/tailwindlabs/prettier-plugin-tailwindcss), [MSW](https://mswjs.io/docs/quick-start), [Vitest](https://vitest.dev/guide/).

## 단계별 커밋 메시지 제안

현재 커밋은 생성하지 않았다. 필요하면 파일을 단계별로 검토·스테이징한 뒤 요청한다.

| 단계            | 실행·확인                                                             | 메시지                                             |
| --------------- | --------------------------------------------------------------------- | -------------------------------------------------- |
| 생성·품질 설정  | `npm ci`, `npm run typecheck`, `npm run lint`, `npm run format:check` | `chore: initialize Vite TypeScript tooling`        |
| 라우팅·레이아웃 | `npm run dev`, 모바일 내비게이션·공개/인증 경로                       | `feat: add responsive routes and brand layout`     |
| API·목          | `npm test`, 요청·쓰기 후 조회·권한·중복 제한                          | `feat: add typed API layer and stateful MSW mocks` |
| 화면            | `npm run dev`, 위 수동 시나리오                                       | `feat: implement marketplace and neighbor flows`   |
| 명세·검증       | `npm run build`, 백엔드 요청 사항 검토                                | `docs: document API contract and local workflow`   |

## 알려진 한계

목 데이터는 브라우저 탭의 메모리이며 새로고침·다른 탭 간 공유를 지원하지 않는다. 목 비밀번호 digest는 데모 검증용이며 운영용 암호 저장 구현이 아니다. 사진 형식은 MIME·크기만 검사하므로 실제 서버는 내용 검사와 업로드 보안도 구현해야 한다. 목의 userRank는 고정값이며 평가 후 재계산하지 않는다. 설치 시 npm audit에서 ESLint/typescript-eslint의 minimatch 의존성 관련 moderate 경고 10건을 확인했다. 자동 강제 다운그레이드는 적용하지 않았다. 실제 백엔드 통합은 아직 수행하지 못했다. Hallmark 수정 후에는 5개 공개 화면을 데스크톱·375px 모바일로 자동 렌더링하고 캡처를 확인했다.

## Hallmark 디자인 수정

보라·초록 브랜드와 기존 라우트·폼·API를 유지하고, 홍보 배너·영문 소제목·이모지 장식·카드 확대를 제거했다. 홈은 동네 선택과 상품 목록을 먼저 보여준다. 테마는 `src/styles/tokens.css`에 모았으며 흰 배경과 플랫한 상품 그리드를 사용한다. 구매·판매 데이터와 실제 거래 사진은 별개인 데모 자료다.

[적용 스킬](https://github.com/Nutlope/hallmark) · [사진 출처](docs/ASSET_CREDITS.md). 수정 전 작업 파일은 `/private/tmp/gazi-before-hallmark-20261106`에 백업했다. 자동 커밋은 하지 않았다.
