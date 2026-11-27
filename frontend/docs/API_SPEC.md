# 가지 API 명세서 — 백엔드 협의용 초안

| 항목        | 내용                                                            |
| ----------- | --------------------------------------------------------------- |
| 문서 버전   | v0.4                                                            |
| 작성·갱신일 | 2026-11-10                                                      |
| 상태        | 백엔드 검토 전 / 협의용                                         |
| 기준        | 사용자 승인 요구사항 + 현재 프론트 API 함수·TypeScript 타입·MSW |
| 목적        | 백엔드 언어와 관계없이 REST 요청·응답 계약 합의                 |
| 실행 환경   | 로컬. 실제 백엔드 서버·DB 연동은 미검증                         |

> 백엔드 팀원에게: 현재 프론트 화면에서 필요한 API 초안입니다. 엔드포인트·JSON은 제안이며, 구현 가능 여부와 ERD·인증·트랜잭션 정책을 검토해 주세요. 다른 형태가 필요하면 양쪽이 합의한 뒤 프론트 타입·API 함수·목 데이터도 함께 수정하겠습니다.

## 1. 이 문서에서 확정과 제안을 구분하는 법

- **서비스 요구사항 확정:** 사용자가 승인한 동작과 입력 제한. 예: 판매글만 존재, 사진 1장, 상품 상태 3종, 댓글 문의, 거래당 구매자 평가 1회.
- **프론트 계약 제안:** 아래 URL·HTTP 메서드·필드명·ID 직렬화·페이지 응답·오류 코드. 현재 프론트와 목이 사용하지만 백엔드 합의는 아직 필요하다.
- **백엔드 협의 필요:** Spring Boot/Node.js, 세션/JWT, ERD 추가 컬럼, 점수 계산, 파일 저장 방식, 금액 상한 등. 이 초안으로 임의 확정하지 않는다.

문서 안의 JSON은 형식을 설명하는 예시 데이터다. 실제 계정·실거래 정보가 아니다.

## 2. 먼저 합의할 항목

| 순서 | 결정할 내용                                        | 현재 프론트 기준                                         | 합의 결과 |
| ---- | -------------------------------------------------- | -------------------------------------------------------- | --------- |
| 1    | 인증 방식·만료·로그아웃·비밀번호 변경 후 세션 정책 | 미정. axios는 쿠키 전송 허용, JWT 전송·갱신은 미구현     | 미정      |
| 2    | API 접두사·서버 포트                               | `/api`, 목 이외 개발 서버 대상은 `http://localhost:8080` | 미정      |
| 3    | 응답 구조                                          | 모델 직접 반환. 별도 `{ "data": ... }` 래퍼 없음         | 미정      |
| 4    | ID·날짜·null 표현                                  | 문자열 ID, UTC ISO 8601, 미평가 `buyerRating: null`      | 미정      |
| 5    | 가입 후 자동 로그인                                | 가입 성공 시 Me를 받고 로그인 상태로 이동                | 미정      |
| 6    | ERD 추가·제약                                      | title, Post/Comment created_at, 거래·평가 유일성         | 미정      |
| 7    | 사진 업로드·접근 URL                               | multipart `image` 1개, 응답 `contentImgUrl`              | 미정      |
| 8    | 금액 범위·이메일 정책·신뢰도 계산                  | 금액 상한·계산식 미정, 목 이메일 최대 254자·소문자 비교  | 미정      |
| 9    | 판매자별 글 조회 URL                               | 화면은 `GET /posts?sellerLoginId=...` 사용               | 미정      |

## 3. 화면별 API와 구현 순서

모든 행이 필수 서비스 범위다. P0/P1/P2는 구현 순서이며 P2를 선택 기능으로 분류하지 않는다.

| 순서 | 화면·업무                 | 필요한 API                                                                                                                 | 완료 기준                                     |
| ---- | ------------------------- | -------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------- |
| P0   | 회원가입·동네             | GET /locations, POST /auth/signup                                                                                          | 동네 선택, 중복·길이 검증, 응답 Me            |
| P0   | 로그인·로그아웃·계정 확인 | POST /auth/login, POST /auth/logout, GET /auth/me                                                                          | 새로고침 후 계정 확인·권한 확인               |
| P0   | 비밀번호 변경             | PATCH /auth/password                                                                                                       | 현재 비밀번호 확인, 새 비밀번호 검증          |
| P0   | 상품 목록·상세            | GET /posts, GET /posts/:postId                                                                                             | 동네 필터, 최신순, 상태·상품 상태·판매자 표시 |
| P0   | 상품 등록·수정·삭제       | POST /posts, PATCH /posts/:postId, DELETE /posts/:postId                                                                   | 사진 1장, 소유권·SOLD 제약                    |
| P1   | 댓글 문의                 | GET/POST /posts/:postId/comments, DELETE /comments/:commentId                                                              | 글별 조회·작성·본인 삭제                      |
| P1   | 상태·거래 완료            | PATCH /posts/:postId/status, GET /posts/:postId/buyer-candidates, POST /posts/:postId/deal                                 | 후보 검증, Deal_log + SOLD 원자적 저장        |
| P1   | 프로필·판매/구매 내역     | GET /users/:loginId, GET /posts?sellerLoginId=..., GET /me/deals?role=...                                                  | 공개·본인 정보 분리, 완료 거래 조회           |
| P1   | 구매자 평가               | POST /deals/:dealId/buyer-rating                                                                                           | 판매자만 1~10점, 거래당 1회                   |
| P2   | 찜·내 찜 목록             | PUT/DELETE /posts/:postId/like, GET /me/likes                                                                              | 중복 없는 찜, likesCount·isLiked·목록 동기화  |
| P2   | 팔로우·관계 목록·피드     | PUT/DELETE /users/:loginId/follow, GET /users/:loginId/followers, GET /users/:loginId/following, GET /posts?feed=following | 자기 팔로우 금지, 수·목록·피드 동기화         |

일반 SNS 게시글, 검색, 카테고리, 채팅, 알림, 거래 후기, 프로필 수정 API는 이번 필수 범위에 포함하지 않는다. 댓글 답장(`parentId`)은 프론트·목에 구현된 추가 제안이며, 백엔드 포함 여부는 팀 검토 후 확정한다. 구매자 점수 평가는 포함한다.

## 3-1. 담당자별 확인 항목

아래 **기능 담당 이름은 팀에서 정한 배정**이다. API 단위의 세부 경계는 그 배정을 기준으로 한 **검토 안내**이며, 찜·팔로우는 회원 기능에 포함하여 김유진이 담당한다. 게시물·상품이 겹치는 API의 세부 경계는 공동으로 합의한다. `함께 확인`은 구현 책임을 떠넘기는 의미가 아니라 연관 데이터·규칙을 맞춰야 한다는 뜻이다.

### 역할 요약

| 담당자                  | 배정된 기능          | 먼저 확인할 부분                                                                     | 함께 확인할 사람                                                                    |
| ----------------------- | -------------------- | ------------------------------------------------------------------------------------ | ----------------------------------------------------------------------------------- |
| 김유진                  | 회원 기능·찜·팔로우  | 가입·인증·프로필·비밀번호 변경, 찜·내 찜 목록, 팔로우·관계 목록·카운트               | 이효선: 회원·관계 제약 / 김성윤·전준혁: 상품·피드 연결 / 수영: 신뢰도 계산·반영     |
| 김성윤                  | 게시물·댓글 기능     | 판매글 제목·본문·생성일, 댓글 작성·조회·본인 삭제, 글과 댓글의 연결                  | 전준혁: 동일 Post의 상품 정보 / 수영: 댓글 작성자에서 구매자 후보 선정              |
| 전준혁                  | 상품 관리 기능       | 가격·사진 1장·상품 상태·판매 상태, 상품 등록·수정·삭제 조건, 목록·상세의 상품 데이터 | 김성윤: Post API 통합 / 수영: 거래 완료와 SOLD 전환                                 |
| 수영                    | 거래 관리 기능       | 구매자 후보·실거래가·거래 완료·판매/구매 내역·구매자 평가 1회                        | 전준혁: 상품 상태 / 김성윤: 댓글 자격 / 김유진: 신뢰도 / 이효선: 트랜잭션·유일 제약 |
| 이효선                  | DB 관리              | ERD 변경·컬럼 타입·PK/FK·UNIQUE·동네 기준 데이터·저장 제약·트랜잭션 검토             | 각 기능 담당자와 실제 조회·저장 방식 확인                                           |
| 프론트엔드 담당자(본인) | 화면·프론트 API 연동 | 필요한 응답 필드, 타입·MSW·실제 요청 일치, 화면 상태·권한 표시                       | 각 API의 담당자와 요청/응답 확정                                                    |

DB 담당이라고 해서 모든 API 구현을 이효선에게 맡기는 것은 아니다. 각 기능 담당자가 자신의 API·검증·쿼리를 확인하고, 이효선은 그 API가 사용하는 DB 구조와 제약을 함께 검토한다.

### 김유진 — 회원 기능 확인

- [ ] 가입 필드 loginId/email/password/name/locationId/bio/account와 길이·중복 규칙 확인
- [ ] 로그인은 email + password, loginId는 가입 후 변경 불가 확인
- [ ] 세션/JWT, 인증 정보 전달, 만료·로그아웃·비밀번호 변경 후 세션 정책 결정
- [ ] GET /locations를 회원가입에서 사용할 수 있도록 응답 형식을 확인. API 구현 담당도 김유진·이효선이 합의
- [ ] PublicUser에는 email/account/password/해시가 없고, Me에만 email/account가 있는지 확인
- [ ] 다른 기능 담당자가 현재 로그인 회원을 식별하고 권한을 검사하는 공통 방법 공유
- [ ] 수영과 userRank 계산 담당·평가 후 갱신 시점 확인. 프로필 API는 최종 계산값을 반환

- [ ] 찜 추가·취소·내 찜 목록과 한 글당 회원별 1회 제한 확인
- [ ] 팔로우·언팔로우·팔로워/팔로잉 목록·관계 수 확인. 자기 팔로우 금지
- [ ] 김성윤·전준혁과 Post 응답의 likesCount/isLiked 및 팔로잉 피드의 관계 필터 연결 확인
- [ ] 이효선과 찜·팔로우 복합 유일 제약 및 삭제 후 카운트 일치 확인

### 김성윤 — 게시물·댓글 기능 확인

- [ ] 전준혁과 하나의 판매글 Post를 함께 사용한다는 전제로 글 등록·수정·삭제·조회 API 경계 결정
- [ ] title 최대 100자, 본문 최대 1000자, Post.createdAt 응답 확인
- [ ] 댓글 본문 최대 300자, Comment.createdAt, 글별 조회·작성·본인 삭제 확인. 댓글 수정은 없음
- [ ] 댓글 작성자는 인증 정보로 결정하고 다른 회원의 댓글 삭제를 서버에서 차단
- [ ] 전준혁과 글 삭제 시 연결 댓글·찜 처리 방식 확인
- [ ] 수영과 구매자 후보 기준 확인: 해당 글의 현재 댓글 작성자, 판매자 제외, 중복 제거
- [ ] 후보 조회 후 댓글이 삭제된 경우 거래 저장 시 자격 재검증이 가능하도록 조회 기준 공유

### 전준혁 — 상품 관리 기능 확인

- [ ] price, image/contentImgUrl, condition의 저장·응답 형식 확인
- [ ] condition = NEW/LIKE_NEW/USED와 status = ON_SALE/RESERVED/SOLD를 구분
- [ ] 사진 한 장·JPEG/PNG/WebP·5MiB 제한·파일 내용 검증·접근 가능한 URL 확인
- [ ] 김성윤과 GET/POST/PATCH/DELETE /posts의 최종 API 구현 담당·호출 경계 확정
- [ ] 동네·판매자별 목록 필터, 최신순·페이지 응답, 카드의 상품 상태·찜 수 확인
- [ ] 예약중 ↔ 판매중 전환과 판매완료 이후 수정·삭제·상태 복귀 차단 확인
- [ ] 수영과 거래 완료 시 status 변경이 별도 요청으로 분리되지 않도록 합의

### 수영 — 거래 관리 기능 확인

- [ ] 구매자는 현재 댓글을 남긴 회원 중 판매자를 제외하여 선택. 후보 목록과 저장 시점 모두 검증
- [ ] money는 0 이상 원 단위 정수, UI 기본값은 price이지만 서버에서도 독립 검증
- [ ] Deal_log 생성과 Post의 SOLD 전환을 하나의 트랜잭션으로 처리. 실패 시 둘 다 롤백
- [ ] 글당 거래 한 건만 허용. 동시 완료·재요청에서도 중복 생성 차단
- [ ] 판매자와 구매자가 같은 거래 ID·실거래가·거래일을 조회하는지 확인
- [ ] 해당 거래 판매자만 구매자를 1~10점으로 한 번 평가. 수정·재평가 차단
- [ ] 김유진과 신뢰도 계산·갱신, 이효선과 평가 저장 위치·유일 제약 확인

### 이효선 — DB 관리 확인

- [ ] 사용자 제공 ERD 기준으로 Post.title VARCHAR(100), Post/Comment.created_at 추가 가능 여부 검토
- [ ] 입력 길이와 컬럼 타입 일치: 본문 1000, 댓글/소개 300, 아이디 20, 이름 50, 계좌 30
- [ ] Location 기준 데이터·ID·name 형식과 User.location_ID FK 확인
- [ ] login_id와 email 중복 방지, Post/Comment/User/Deal_log 연결 FK 확인
- [ ] Deal_log.post_ID 유일성 및 S_ID가 구매자 login_id를 가리키는지 확인
- [ ] 찜(회원, 글)·팔로우(회원, 대상) 유일성, 평가 1회 저장 제약 확인
- [ ] 글 삭제·거래 기록 보존 정책에 맞는 FK/삭제 동작, 상태 변경 트랜잭션 확인
- [ ] 가격·실거래가·평가·신뢰도 타입과 허용 범위, 날짜 타입·기본값 확인

### API별 확인 담당

`공동 확인·최종 구현 담당 합의`인 Post API는 한 사람이 최종 응답을 조립하거나 명확한 내부 호출 경계를 정한다. 프론트가 게시물·상품 정보를 각각 저장한 뒤 조합하는 두 번의 등록 요청으로 바꾸는 안은 현재 계약이 아니다.

| Method | Endpoint                        | 우선 확인 담당          | 함께 확인                   | 확인할 내용                                   |
| ------ | ------------------------------- | ----------------------- | --------------------------- | --------------------------------------------- |
| GET    | /locations                      | 김유진·이효선 공동 확인 | API 구현 담당 합의          | 동네 목록 형식·시드 데이터·회원 FK            |
| POST   | /auth/signup                    | 김유진                  | 이효선                      | 필드·중복·가입 후 로그인                      |
| POST   | /auth/login                     | 김유진                  | 프론트엔드                  | 인증 방식·Me 응답                             |
| POST   | /auth/logout                    | 김유진                  | 프론트엔드                  | 인증 해제·반복 요청 정책                      |
| GET    | /auth/me                        | 김유진                  | 프론트엔드                  | 계정 확인·개인정보·미인증 401                 |
| PATCH  | /auth/password                  | 김유진                  | 이효선                      | 현재 비밀번호 검증·세션 정책                  |
| GET    | /posts                          | 김성윤·전준혁 공동 확인 | 김유진: 팔로잉 관계·찜 정보 | 동네/판매자/팔로잉 필터·페이지·통합 Post 응답 |
| GET    | /posts/:postId                  | 김성윤·전준혁 공동 확인 | 김유진                      | 본문·상품·판매자 공개 정보 통합 응답          |
| POST   | /posts                          | 김성윤·전준혁 공동 확인 | 이효선                      | 제목·본문·사진·가격·상품 상태 한 번에 등록    |
| PATCH  | /posts/:postId                  | 김성윤·전준혁 공동 확인 | 수영                        | 소유권·파일 교체·SOLD 수정 차단               |
| DELETE | /posts/:postId                  | 김성윤·전준혁 공동 확인 | 수영·이효선                 | 댓글/찜 처리·완료 거래 보존                   |
| PATCH  | /posts/:postId/status           | 전준혁                  | 수영                        | ON_SALE/RESERVED만 직접 변경                  |
| GET    | /posts/:postId/comments         | 김성윤                  | 김유진                      | 작성자 공개 정보·생성일·순서                  |
| POST   | /posts/:postId/comments         | 김성윤                  | 김유진·전준혁               | 인증 작성자·글 존재·본문 제한                 |
| DELETE | /comments/:commentId            | 김성윤                  | 수영                        | 본인 삭제·구매자 자격 영향                    |
| PUT    | /posts/:postId/like             | 김유진                  | 전준혁·이효선               | 찜 추가·권한·중복 방지                        |
| DELETE | /posts/:postId/like             | 김유진                  | 전준혁·이효선               | 찜 해제·카운트 일치                           |
| GET    | /me/likes                       | 김유진                  | 김성윤·전준혁               | 본인 찜 목록·Post 응답·페이지                 |
| GET    | /users/:loginId                 | 김유진                  | 수영                        | 공개 필드·신뢰도·관계 수                      |
| GET    | /users/:loginId/posts           | 김성윤·전준혁 공동 확인 | 김유진                      | 선택 URL. /posts?sellerLoginId와 하나만 채택  |
| GET    | /users/:loginId/followers       | 김유진                  | 이효선                      | 팔로워 목록·공개 정보                         |
| GET    | /users/:loginId/following       | 김유진                  | 이효선                      | 팔로잉 목록·공개 정보                         |
| PUT    | /users/:loginId/follow          | 김유진                  | 이효선                      | 자기 팔로우·중복 금지                         |
| DELETE | /users/:loginId/follow          | 김유진                  | 이효선                      | 해제·관계 수 일치                             |
| GET    | /posts/:postId/buyer-candidates | 수영                    | 김성윤·김유진               | 댓글 기준 후보·판매자 제외·공개 회원 정보     |
| POST   | /posts/:postId/deal             | 수영                    | 전준혁·김성윤·이효선        | 자격 재검증·실거래가·거래+SOLD 원자 저장      |
| GET    | /me/deals                       | 수영                    | 김유진                      | role=seller/buyer·현재 회원 기준 거래 내역    |
| POST   | /deals/:dealId/buyer-rating     | 수영                    | 김유진·이효선               | 판매자 권한·1회 평가·신뢰도 반영              |

### 여러 담당자가 반드시 같이 확인할 부분

| 쟁점                  | 같이 확인할 사람                | 합의할 내용                                                                                                     |
| --------------------- | ------------------------------- | --------------------------------------------------------------------------------------------------------------- |
| 하나의 판매글 Post    | 김성윤·전준혁·이효선·프론트엔드 | 제목·본문과 상품 정보를 어떤 API·응답·테이블로 연결할지, 최종 API 담당                                          |
| title·created_at 추가 | 김성윤·전준혁·이효선            | 기존 데이터가 있으면 초기값·적용 방법까지 확인                                                                  |
| 거래 완료             | 수영·전준혁·이효선              | Deal_log 생성과 SOLD 변경을 한 트랜잭션에 묶고 동시 요청 처리                                                   |
| 댓글 기반 구매자      | 김성윤·수영                     | 현재 댓글 기준 후보, 댓글 삭제 후 저장 시 재검증                                                                |
| 평가와 신뢰도         | 수영·김유진·이효선              | 평가 저장 위치, 1회 제약, userRank 계산 주체·갱신 시점                                                          |
| 개인정보·인증         | 김유진·전체 기능 담당           | 현재 회원 식별·권한 검사, 공개 응답에 email/account/password 제외                                               |
| 찜·팔로우·팔로잉 피드 | 김유진·김성윤·전준혁·이효선     | 김유진은 찜·팔로우 관계·목록·카운트 담당. 김성윤·전준혁과 피드 필터 및 Post의 likesCount/isLiked 응답 연결 확인 |

### 찜·팔로우 담당 확정

찜·팔로우는 **회원 기능에 포함하며 김유진이 담당**한다. 찜 추가/취소·내 찜 목록, 팔로우/언팔로우·팔로워/팔로잉 목록·관계 수를 포함한다.

팔로잉 피드는 김유진이 관리하는 관계 데이터를 사용하고, 판매글 조회 담당인 김성윤·전준혁과 GET /posts?feed=following의 필터를 연결한다. 찜 정보도 같은 방식으로 상품 목록·상세의 likesCount/isLiked에 반영한다. DB의 관계 유일성은 이효선과 함께 검토한다.

### 각 담당자가 검토 후 남길 답변

1. 본인 API의 URL·요청·응답 형식에 동의하는지
2. ERD/구현 때문에 변경해야 하는 항목과 대체 JSON 예시
3. 공동 API의 최종 구현 담당과 연결할 데이터·호출 경계
4. 구현 예정일·프론트 연동 가능일 — 확인 후 기입, 이 문서에서 임의 지정하지 않음

## 4. 백엔드 요청 사항

| 대상        | 요청                                                             | 프론트 계약                                               |
| ----------- | ---------------------------------------------------------------- | --------------------------------------------------------- |
| Post        | ERD에 없는 `title VARCHAR(100) NOT NULL` 추가                    | `title: string`, 최대 100자. 목록·상세·등록·수정에서 사용 |
| Post        | ERD에 없는 `created_at` 추가, 서버에서 생성 시각 기록            | `createdAt: string`, UTC ISO 8601. 최신순 정렬 기준       |
| Comment     | ERD에 없는 `created_at` 추가, 서버에서 생성 시각 기록            | `createdAt: string`, UTC ISO 8601                         |
| Post        | 상품 상태 필드·응답 확정                                         | `condition: "NEW" \| "LIKE_NEW" \| "USED"` 필수           |
| Deal_log    | 글당 거래 1건 유일성 보장, 거래 생성과 SOLD 전환을 트랜잭션 처리 | 동시 요청에도 Deal_log 중복 금지                          |
| 구매자 평가 | 거래당 한 번 1~10점 저장할 위치·제약 확정                        | `buyerRating: number \| null`, 수정 불가                  |
| 회원 관계   | 찜·팔로우 복합 유일성·자기 팔로우 금지                           | 추가 PUT은 중복 생성 없이 성공                            |

`updatedAt`은 요청·응답에 사용하지 않는다. `createdAt`은 생성 시각이며 수정해도 바뀌지 않는다.

기존 DB `location_ID`는 API `locationId`, 거래 구매자 `S_ID`는 `buyerLoginId`로 매핑한다. 요청·응답은 camelCase. 원본 ERD 파일은 이 폴더에 없어 사용자가 제공한 컬럼 정보만 기준으로 작성했다.

## 5. 공통 규칙

- 개발 base URL: `/api`. 아래 경로에는 `/api`를 생략했다.
- JSON 기본, 사진이 있는 판매글 등록·수정은 `multipart/form-data`.
- ID는 프론트에서 불투명 문자열로 취급한다. DB 숫자 ID를 쓰면 API 직렬화 시 문자열로 통일하거나 프론트와 타입을 함께 조정한다.
- 날짜: 서버 생성 UTC ISO 8601. 프론트에서는 로컬 시간대로 표시.
- 가격·실거래가는 0 이상 원 단위 안전한 정수. 최대 금액은 실제 DB 타입과 합의 필요.
- 목록: 기본 `page=1&pageSize=20`. page >= 1, pageSize 1~100.
- 목록 응답: `{ "items": [], "page": 1, "pageSize": 20, "total": 0 }`.
- Location·댓글·관계 목록·거래 목록은 현재 배열 응답. 규모가 커지면 서버·프론트 계약을 함께 페이지화.
- 성공: 조회/수정 200, 생성 201, 응답 없는 삭제/토글/비밀번호 변경 204.
- 오류: 400 유효성, 401 미인증/로그인 실패, 403 권한, 404 없음, 409 중복·상태 충돌.
- 인증은 세션/JWT 미정. 현재 목은 메모리 세션, axios는 쿠키 전송 허용. 실제 인증·갱신·CSRF·비밀번호 변경 후 세션 정책은 확정 필요.
- 모든 쓰기 권한·길이·금액·중복 제한은 서버에서 재검증한다. TS 타입은 런타임 보안 검증이 아니다.

```json
{
  "code": "VALIDATION_ERROR",
  "message": "댓글은 300자 이내로 입력해 주세요.",
  "fieldErrors": { "content": "최대 300자" }
}
```

`fieldErrors`는 선택 필드다. 목은 주로 code/message로 응답한다.

### 목록 쿼리 파라미터

| 파라미터      | 타입    | 기본/필수          | 의미                                                         |
| ------------- | ------- | ------------------ | ------------------------------------------------------------ |
| locationId    | string  | 선택               | 작성자의 동네 ID로 필터. 동네 목록 화면은 선택한 ID를 보냄   |
| sellerLoginId | string  | 선택               | 해당 판매자의 글만 조회. SOLD 포함                           |
| feed          | string  | 선택               | 현재 허용 의미는 following, 로그인 회원이 팔로우한 회원의 글 |
| page          | integer | 기본 1             | 1부터 시작                                                   |
| pageSize      | integer | 기본 20            | 1~100. 프론트는 20 사용                                      |
| role          | string  | /me/deals에서 필수 | seller 또는 buyer                                            |

페이지 파라미터는 `/posts`, `/me/likes`, 선택 URL인 `/users/:loginId/posts`에 사용한다. `/me/deals`는 현재 페이지가 아닌 배열 응답이다. 여러 글 필터를 함께 보내면 목은 AND 조건으로 처리한다. 잘못된 feed·존재하지 않는 locationId 처리 정책은 백엔드 검토 시 확정한다. 팔로잉 화면은 동네 제한을 걸지 않는다.

페이지를 넘겼는데 항목이 없으면 200과 빈 items를 반환한다. 빈 목록을 404로 처리하지 않는다. 단건 조회 대상이 없으면 404다. `total`은 현재 필터에 일치하는 전체 개수다.

### 응답 형태 주의

현재 프론트는 axios 응답의 `response.data` 자체를 모델로 사용한다. 따라서 아래처럼 별도 래퍼를 추가하면 프론트 수정이 필요하다.

```json
{ "data": { "id": "1" } }
```

위 블록은 **현재 계약에 맞지 않는 래퍼 예시**다. 실제 성공 응답은 아래 모델·배열·PageResponse를 직접 반환한다. 204 응답에는 JSON 본문을 넣지 않는다. 인증 실패인 `/auth/me`는 HTTP 401이며, 프론트 API 함수 내부에서만 null로 변환한다.

## 6. 공개·인증 경계

공개: locations, 판매글 목록/상세/댓글 목록, 공개 프로필/판매글/팔로워/팔로잉 목록.
인증 필요: 등록·수정·삭제·상태 변경·거래·평가·댓글 쓰기·찜·팔로우·팔로잉 피드·내 정보/거래/찜.
비회원은 동네를 직접 선택하고 회원은 기본으로 본인 locationId를 사용한다.

공개 User에는 비밀번호·비밀번호 해시·email·account를 포함하지 않는다. Me에만 email/account 포함. 비밀번호는 요청에만 존재한다. 작성자·판매자·현재 회원 ID는 서버가 인증 정보에서 결정한다. 등록 요청의 sellerLoginId나 평가 요청의 평가자 ID를 신뢰하지 않는다. userRank·관계 수·찜 수·거래일·생성일은 서버가 관리하며 클라이언트가 임의 지정하지 않는다.

## 7. 입력 제한

| 필드               | 규칙                                                                                                                                    |
| ------------------ | --------------------------------------------------------------------------------------------------------------------------------------- |
| loginId            | 필수, 최대 20자, 중복 불가, 가입 후 변경 불가                                                                                           |
| email              | 필수, 이메일 형식, 중복 불가, 로그인에 사용                                                                                             |
| password           | 8자 이상, 영문·숫자 포함. 비밀번호 확인란은 프론트 일치 검사 전용                                                                       |
| name               | 필수, 최대 50자                                                                                                                         |
| locationId         | 서버 locations 중 하나, 시/도와 시/군/구 둘 다 선택                                                                                     |
| bio                | 선택, 최대 300자                                                                                                                        |
| account            | 선택, 최대 30자, 본인만 조회                                                                                                            |
| title              | 필수, 최대 100자. DB 추가 요청                                                                                                          |
| content (Post)     | 필수, 최대 1000자                                                                                                                       |
| content (Comment)  | 필수, 최대 300자, 수정 없음                                                                                                             |
| parentId (Comment) | 선택. 같은 글의 댓글 id, 없으면 404 COMMENT_NOT_FOUND. 답장의 id를 주면 그 원댓글 아래에 저장(한 단계). 원댓글 삭제 시 답장도 함께 삭제 |
| condition          | NEW / LIKE_NEW / USED                                                                                                                   |
| image              | 등록 시 1장 필수, JPEG/PNG/WebP, 5 MiB 이하, 빈 파일 불가                                                                               |
| score              | 정수 1~10, 거래당 1회                                                                                                                   |

상품 상태 condition과 판매 상태 status는 별개다.

## 8. 응답 모델·타입·JSON 예시

### 모델별 필드 타입

| 모델            | 필드와 타입                                                                                                                                                    |
| --------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Location        | id: string, name: string                                                                                                                                       |
| PublicUser      | loginId/name/bio: string, location: Location, userRank/followerCount/followingCount: number, isFollowing: boolean                                              |
| Me              | PublicUser 전체 + email/account: string                                                                                                                        |
| Post            | id/title/content/contentImgUrl/createdAt: string, price/likesCount: number, condition: PostCondition, status: PostStatus, seller: PublicUser, isLiked: boolean |
| Comment         | id/postId/content/createdAt: string, author: PublicUser                                                                                                        |
| Deal            | id/postId/postTitle/sellerLoginId/buyerLoginId/dealDate: string, money: number, buyerRating: number 또는 null                                                  |
| PageResponse<T> | items: T[], page/pageSize/total: number                                                                                                                        |
| ApiError        | code/message: string, fieldErrors?: Record<string, string>                                                                                                     |

모든 응답 필드는 위 타입대로 존재한다. `fieldErrors`만 선택이다. PostCondition은 NEW/LIKE_NEW/USED, PostStatus는 ON_SALE/RESERVED/SOLD다. 날짜 문자열은 ISO 8601이어야 한다.

### Location

```json
{ "id": "seoul-마포구", "name": "서울특별시 마포구" }
```

목 ID는 데모용이다. 운영 서버의 Location 기본키 값을 그대로 받아 사용한다. 서울특별시 25구·경기도 31시군을 목에 제공한다. 시/도를 바꾸면 하위 선택은 초기화한다.

### PublicUser

```json
{
  "loginId": "neighbor",
  "name": "연남의 오후",
  "bio": "취향이 담긴 물건을 다음 이웃에게 전합니다.",
  "location": { "id": "seoul-마포구", "name": "서울특별시 마포구" },
  "userRank": 92,
  "followerCount": 1,
  "followingCount": 1,
  "isFollowing": true
}
```

userRank는 0~100, 서버 계산 전담. 기본값 50. 카운트 기본값 0. 비회원 isFollowing은 false.
프로필 사진 컬럼 없음. 프론트 기본 가지 마크 사용.

### Me

본인 계정 응답 전체 예시. 로그인·가입·내 계정 조회에서 같은 모델을 사용한다.

```json
{
  "loginId": "gazi_demo",
  "name": "가지로운 생활",
  "bio": "이웃과 필요한 물건을 나눕니다.",
  "location": { "id": "seoul-마포구", "name": "서울특별시 마포구" },
  "userRank": 78,
  "followerCount": 1,
  "followingCount": 1,
  "isFollowing": false,
  "email": "demo@gazi.local",
  "account": ""
}
```

선택 입력 bio/account를 등록하지 않았으면 현재 프론트 계약은 빈 문자열이다. null을 반환하려면 타입·목·화면을 함께 조정한다. 본인에 대한 isFollowing은 false다. 인증 토큰이나 비밀번호를 이 모델에 임의로 추가하지 말고, 인증 방식을 먼저 합의한다.

### Post

```json
{
  "id": "1",
  "title": "함께 집중하던 원목 책상",
  "price": 45000,
  "content": "아끼던 책상을 내놓아요. 생활 흠집이 조금 있습니다.",
  "contentImgUrl": "/uploads/desk.jpg",
  "condition": "USED",
  "status": "ON_SALE",
  "seller": {
    "loginId": "neighbor",
    "name": "연남의 오후",
    "bio": "취향이 담긴 물건을 다음 이웃에게 전합니다.",
    "location": { "id": "seoul-마포구", "name": "서울특별시 마포구" },
    "userRank": 92,
    "followerCount": 1,
    "followingCount": 1,
    "isFollowing": false
  },
  "likesCount": 1,
  "isLiked": false,
  "createdAt": "2026-11-06T00:00:00.000Z"
}
```

사진 URL은 브라우저가 열 수 있어야 한다. 별도 업로드 호스트 또는 백엔드 경로 프록시는 백엔드 결정 후 맞춘다. MSW는 로컬에 저장한 Pexels 데모 사진 또는 업로드 Blob URL을 사용한다.

### Comment

```json
{
  "id": "comment-1",
  "postId": "1",
  "author": {
    "loginId": "gazi_demo",
    "name": "가지로운 생활",
    "bio": "반가워요.",
    "location": { "id": "seoul-마포구", "name": "서울특별시 마포구" },
    "userRank": 78,
    "followerCount": 1,
    "followingCount": 1,
    "isFollowing": false
  },
  "parentId": null,
  "content": "책상 가로 길이가 궁금해요.",
  "createdAt": "2026-11-06T00:10:00.000Z"
}
```

`parentId`는 답장(대댓글)이 달린 원댓글의 id, 원댓글이면 `null`. 답장은 한 단계만 허용한다. DB 추가 요청: `Comment.parent_id` (NULL 허용, 같은 Comment FK).

### Deal

```json
{
  "id": "deal-1",
  "postId": "1",
  "postTitle": "함께 집중하던 원목 책상",
  "sellerLoginId": "neighbor",
  "buyerLoginId": "gazi_demo",
  "money": 40000,
  "dealDate": "2026-11-06T01:00:00.000Z",
  "buyerRating": null
}
```

## 9. API 목록·요청·응답

응답 모델 이름은 위 JSON 구조를 가리킨다. 배열·페이지 구조는 공통 규칙을 따른다.

### 인증·동네

| Method | Endpoint       | 권한 | 요청                       | 성공 응답                |
| ------ | -------------- | ---- | -------------------------- | ------------------------ |
| GET    | /locations     | 공개 | 없음                       | 200 Location[]           |
| POST   | /auth/signup   | 공개 | 아래 SignupRequest         | 201 Me, 로그인 상태 생성 |
| POST   | /auth/login    | 공개 | 아래 LoginRequest          | 200 Me, 로그인 상태 생성 |
| POST   | /auth/logout   | 인증 | 없음                       | 204                      |
| GET    | /auth/me       | 인증 | 없음                       | 200 Me, 미인증 401       |
| PATCH  | /auth/password | 인증 | 아래 ChangePasswordRequest | 204                      |

SignupRequest:

```json
{
  "loginId": "new_neighbor",
  "email": "new@example.com",
  "password": "Example1234",
  "name": "새 이웃",
  "locationId": "seoul-마포구",
  "bio": "작은 거래부터 시작해요.",
  "account": ""
}
```

LoginRequest:

```json
{ "email": "new@example.com", "password": "Example1234" }
```

ChangePasswordRequest:

```json
{ "currentPassword": "Example1234", "newPassword": "Changed1234" }
```

로그아웃은 현재 목에서 미인증 재호출도 204로 처리한다. 실제 서버가 미인증 시 401을 반환할지, 반복 로그아웃을 204로 허용할지는 인증 계약에서 확정한다.

아이디/이메일 중복은 409 DUPLICATE_USER. 회원가입 자동 로그인은 현재 프론트·목 계약이므로 백엔드와 합의 필요.

### 판매글

| Method | Endpoint              | 권한              | 요청                        | 성공 응답              |
| ------ | --------------------- | ----------------- | --------------------------- | ---------------------- |
| GET    | /posts                | 공개              | 김유진: 팔로잉 관계·찜 정보 | 200 PageResponse<Post> |
| GET    | /posts?feed=following | 인증              | page?, pageSize?            | 200 PageResponse<Post> |
| GET    | /posts/:postId        | 공개              | 없음                        | 200 Post               |
| POST   | /posts                | 인증              | multipart 아래              | 201 Post               |
| PATCH  | /posts/:postId        | 소유자, SOLD 아님 | multipart 아래              | 200 Post               |
| DELETE | /posts/:postId        | 소유자, SOLD 아님 | 없음                        | 204                    |
| PATCH  | /posts/:postId/status | 소유자, SOLD 아님 | 아래 상태 요청              | 200 Post               |

등록·수정 multipart 필드:

```text
title = 함께 집중하던 원목 책상
price = 45000
content = 생활 흠집이 조금 있습니다.
condition = USED
image = (JPEG/PNG/WebP 파일 한 장)
```

multipart의 price는 숫자를 표현하는 문자열로 전달하고 서버에서 검증·변환한다. 거래 완료 JSON의 money는 문자열이 아닌 number다.

수정은 title/price/content/condition 전체 전송. image 생략 시 기존 사진 유지, 파일 전송 시 교체. multipart Content-Type은 브라우저가 boundary와 함께 설정하므로 직접 고정하지 않는다.

동네 필터는 판매자의 locationId를 기준으로 한다. 팔로잉 피드는 기본으로 동네 제한 없음. 최신순 정렬은 createdAt DESC, 동률일 때 안정적인 보조 정렬 키를 백엔드에서 제공한다.

상태 변경:

```json
{ "status": "RESERVED" }
```

ON_SALE ↔ RESERVED, ON_SALE/RESERVED → SOLD 가능. SOLD는 아래 거래 생성으로만 진입. SOLD 이후 수정·삭제·상태 복귀 불가. 없는 글 404, 타인 조작 403, 완료 글 조작 409 POST_SOLD. 삭제 시 해당 댓글·찜도 함께 제거.

### 댓글·찜

| Method | Endpoint                | 권한        | 요청             | 성공 응답              |
| ------ | ----------------------- | ----------- | ---------------- | ---------------------- |
| GET    | /posts/:postId/comments | 공개        | 없음             | 200 Comment[], 생성순  |
| POST   | /posts/:postId/comments | 인증        | 아래             | 201 Comment            |
| DELETE | /comments/:commentId    | 댓글 작성자 | 없음             | 204                    |
| PUT    | /posts/:postId/like     | 김유진      | 없음             | 204                    |
| DELETE | /posts/:postId/like     | 김유진      | 없음             | 204                    |
| GET    | /me/likes               | 김유진      | page?, pageSize? | 200 PageResponse<Post> |

```json
{ "content": "이번 주말에 거래할 수 있을까요?" }
{ "content": "가로 120cm예요.", "parentId": "comment-1" }
```

댓글 수정 없음. 댓글 삭제는 타인 요청 403. 찜은 (회원, 글) 유일하며 반복 PUT/DELETE가 중복 행이나 음수 카운트를 만들지 않는다.

### 프로필·팔로우

| Method | Endpoint                  | 권한   | 요청             | 성공 응답                         |
| ------ | ------------------------- | ------ | ---------------- | --------------------------------- |
| GET    | /users/:loginId           | 공개   | 없음             | 200 PublicUser                    |
| GET    | /users/:loginId/posts     | 공개   | page?, pageSize? | 200 PageResponse<Post>, SOLD 포함 |
| GET    | /users/:loginId/followers | 김유진 | 없음             | 200 PublicUser[]                  |
| GET    | /users/:loginId/following | 김유진 | 없음             | 200 PublicUser[]                  |
| PUT    | /users/:loginId/follow    | 김유진 | 없음             | 204                               |
| DELETE | /users/:loginId/follow    | 김유진 | 없음             | 204                               |

**판매자별 글 조회 URL 협의:** 현재 화면은 `GET /posts?sellerLoginId=:loginId`를 호출한다. `/users/:loginId/posts`는 목에만 추가로 존재하며 프론트 API 함수에서는 호출하지 않는다. 백엔드가 두 URL을 모두 구현할 필요는 없다. 하나로 합의하고 문서·목을 함께 정리한다. 나머지 표의 경로는 현재 프론트에서 사용한다.

자기 팔로우/해제 요청은 400 SELF_FOLLOW. 관계는 (팔로워, 대상) 유일. 반복 PUT/DELETE는 같은 최종 상태를 유지. 공개 프로필의 SOLD 게시글은 완료 기록으로 사용하며, 비공개 구매자·실거래가·계좌까지 공개하지 않는다.

### 거래·구매자 평가

| Method | Endpoint                        | 권한             | 요청                   | 성공 응답          |
| ------ | ------------------------------- | ---------------- | ---------------------- | ------------------ |
| GET    | /posts/:postId/buyer-candidates | 판매자           | 없음                   | 200 PublicUser[]   |
| POST   | /posts/:postId/deal             | 판매자           | 아래 CreateDealRequest | 201 Deal, 글 SOLD  |
| GET    | /me/deals?role=seller           | 인증             | 없음                   | 200 Deal[], 최신순 |
| GET    | /me/deals?role=buyer            | 인증             | 없음                   | 200 Deal[], 최신순 |
| POST   | /deals/:dealId/buyer-rating     | 해당 거래 판매자 | 아래                   | 201 Deal           |

CreateDealRequest:

```json
{ "buyerLoginId": "gazi_demo", "money": 40000 }
```

- 구매자는 이 글에 현재 남아 있는 댓글의 작성자 중 판매자를 제외하고 중복 제거한 회원.
- 후보 목록 조회 후에도 거래 저장 시 자격을 다시 검증. 삭제된 댓글만 남긴 회원은 제외.
- UI 실거래가 기본값은 price, 수정 가능. 서버는 클라이언트 기본값을 신뢰하지 않음.
- DB: Deal_log(post_ID, S_ID=구매자 login_id, money, deal_date). 판매자는 Post 작성자로 유도.
- post_ID 유일 제약과 트랜잭션/동시성 제어로 글당 한 번만 완료. 재요청은 409 DEAL_EXISTS.
- 판매/구매 내역은 인증 회원 기준. 요청으로 다른 사용자의 역할을 사칭할 수 없음.

평가 요청:

```json
{ "score": 9 }
```

판매자만, 거래당 한 번, 정수 1~10. 중복 409 ALREADY_RATED, 타인 403, 범위 위반 400. buyerRating이 null이 아니면 수정할 수 없음. 점수를 통한 userRank 계산은 백엔드 전담이며 목은 고정 신뢰도를 유지한다.

## 10. 오류 응답·중복 요청 처리

아래는 현재 목이 사용하는 오류 코드다. 백엔드는 동일 의미·형태를 제안 기준으로 검토한다. `message`는 사람이 읽을 수 있게 작성하고, SQL·스택·비밀번호·토큰을 노출하지 않는다.

| HTTP | code                                                                 | 대표 상황                                         |
| ---- | -------------------------------------------------------------------- | ------------------------------------------------- |
| 400  | INVALID_REQUEST                                                      | JSON 또는 요청 본문 형식 오류                     |
| 400  | VALIDATION_ERROR                                                     | 필수 값 누락·공백·길이 초과                       |
| 400  | INVALID_EMAIL / INVALID_PASSWORD / INVALID_LOCATION                  | 가입·비밀번호 변경·동네 값 검증 실패              |
| 400  | INVALID_PRICE / INVALID_MONEY                                        | 가격·실거래가가 음수·소수·안전하지 않은 정수      |
| 400  | INVALID_CONDITION / INVALID_STATUS                                   | 허용하지 않는 상태 또는 일반 상태 API로 SOLD 설정 |
| 400  | IMAGE_REQUIRED / INVALID_IMAGE                                       | 등록 사진 누락·MIME/크기/빈 파일 검증 실패        |
| 400  | INVALID_PAGINATION / INVALID_ROLE                                    | 페이지 값 또는 거래 조회 역할 오류                |
| 400  | SELF_FOLLOW / INVALID_BUYER / INVALID_SCORE                          | 자기 팔로우, 구매자 자격 없음, 평가 범위 위반     |
| 401  | UNAUTHENTICATED                                                      | 로그인 필요                                       |
| 401  | INVALID_CREDENTIALS                                                  | 로그인 이메일 또는 비밀번호 불일치                |
| 403  | FORBIDDEN                                                            | 타인 글·댓글 조작, 타인 거래 완료·평가            |
| 404  | POST_NOT_FOUND / USER_NOT_FOUND / COMMENT_NOT_FOUND / DEAL_NOT_FOUND | 해당 리소스 없음                                  |
| 409  | DUPLICATE_USER                                                       | 아이디 또는 이메일 중복                           |
| 409  | POST_SOLD                                                            | 완료 글 수정·삭제·상태 복귀 시도                  |
| 409  | DEAL_EXISTS                                                          | 이미 거래가 완료된 글에 거래 생성 요청            |
| 409  | ALREADY_RATED                                                        | 이미 평가한 거래 재평가                           |
| 500  | INTERNAL_ERROR                                                       | 예상하지 못한 서버 오류                           |

중복 완료 예시:

```json
{
  "code": "DEAL_EXISTS",
  "message": "이미 완료된 거래입니다."
}
```

찜·팔로우 PUT/DELETE 반복은 같은 최종 상태를 유지하는 204다. 거래 생성·평가는 같은 데이터로 재요청해도 새 행을 만들거나 기존 값을 덮어쓰지 않고 409로 응답하는 제안이다. 거래 완료 응답을 받기 전에 연결이 끊겼다면 프론트는 상태·내역을 다시 조회해 확인한다. 사용자에게 변경 요청을 무조건 자동 재전송하도록 요구하지 않는다.

## 11. 통합 확인 시나리오

| 시나리오                           | 확인할 결과                                                     |
| ---------------------------------- | --------------------------------------------------------------- |
| 가입 → 계정 확인 → 로그아웃        | Me 반환, 공개 모델에 비밀번호 없음, 로그아웃 후 /auth/me 401    |
| 중복 아이디·이메일로 가입          | 409, 중복 회원 미생성                                           |
| 비밀번호 변경                      | 현재 비밀번호 검증, 이전 비밀번호 로그인 실패, 새 비밀번호 성공 |
| 동네 목록 → 선택 → 상품 조회       | Location ID 사용, 다른 동네 글 제외, 최신순·페이지 개수 일치    |
| 글 등록 → 상세 → 수정              | 사진 1장, title/condition/가격/설명 반영, createdAt 유지        |
| 타인 글·댓글 수정/삭제 시도        | 서버 403. UI 버튼 숨김만으로 검증을 대신하지 않음               |
| 찜·팔로우 반복 추가/해제           | 중복 행·카운트 음수 없음, 목록·카운트·조회 플래그 일치          |
| 팔로잉 피드                        | 로그인 회원이 팔로우한 회원의 글, 동네 제한 없음                |
| 예약중 → 판매중 복귀               | RESERVED → ON_SALE 허용                                         |
| 구매자 후보 조회                   | 현재 댓글 작성자 중 판매자 제외·중복 제거                       |
| 구매자 댓글 삭제 후 거래 완료 시도 | 후보 자격 재검증, 부적격 거래 생성 차단                         |
| 같은 글에 거래 완료 동시 요청      | Deal_log 한 건, SOLD 일치. 나머지 요청은 충돌                   |
| 거래 완료 중 서버 저장 실패        | 글 상태만 SOLD이거나 거래 행만 남는 부분 저장 없음              |
| 판매/구매 내역                     | 판매자와 구매자 양쪽에서 같은 거래 ID·실거래가 확인             |
| 완료 글 수정·삭제·상태 복귀        | 409, 거래 기록 보존                                             |
| 구매자 평가 → 재평가               | 판매자만 첫 평가 성공, 재평가 409, 타인 403                     |
| 0원·음수·소수·숫자가 아닌 값       | 0원 허용. 나머지는 규칙대로 검증                                |
| 공개 프로필·글·댓글 조회           | 계좌·이메일·비밀번호·해시 미노출                                |

실제 DB 유일 제약·롤백·인증 만료·업로드 내용 검증은 백엔드가 실서버에서 확인해야 한다. 목의 동작만으로 이 보장을 검증했다고 간주하지 않는다.

## 12. 백엔드 검토·합의 체크리스트

- [ ] 위 담당자별 확인 항목 검토, Post 공동 API 최종 담당 및 김유진의 찜·팔로우 연동 경계 확인
- [ ] 백엔드 언어·담당 도메인·서버 포트 합의
- [ ] URL, camelCase, ID 문자열, 응답 래퍼 없음, 날짜·빈 값 표현 합의
- [ ] 세션/JWT 및 쿠키/토큰 전송·만료·갱신·CSRF·로그아웃 정책 합의
- [ ] 비밀번호 변경 후 기존 로그인 세션 유지·만료 정책 합의
- [ ] 가입 후 자동 로그인 또는 가입 후 로그인 화면 이동 중 선택
- [ ] Post.title VARCHAR(100), Post/Comment.created_at 추가 확인
- [ ] condition 저장 컬럼·enum·기본값 확인
- [ ] 구매자 평가 저장 위치·1~10·거래당 1회 제약 확인
- [ ] Deal_log.post_ID 유일 제약·SOLD 전환 트랜잭션·동시성 처리 확인
- [ ] 회원 아이디/이메일·찜·팔로우 중복 방지 DB 제약 확인
- [ ] userRank 계산식과 평가 후 갱신 시점 합의
- [ ] 사진 image 필드·1장·5MiB·내용 검증·저장·접근 URL 합의
- [ ] DB 금액 상한·이메일 길이/대소문자·문자 길이 계산 기준 확인
- [ ] 판매자별 글 조회 URL 하나로 정리
- [ ] 오류 코드·권한·페이지/배열 응답·목록 정렬 합의
- [ ] 위 통합 시나리오를 실제 백엔드와 확인

합의 후에는 이 문서를 확정본으로 갱신하고, 변경된 항목은 프론트 `src/types`, `src/api`, `src/mocks`와 함께 반영한다. 별도 메신저 대화만으로 계약이 달라지지 않도록 한다. 현재 체크되지 않은 항목은 백엔드가 승인한 것으로 간주하지 않는다.

## 13. 프론트 연결 방법과 검증 범위

`npm test`는 실제 axios API 함수를 MSW Node 인터셉터와 연결해 요청·응답, 쓰기 후 조회, 인증 경계·중복 거래·평가·조건 필드를 검증한다. 실제 Spring/Node 서버·DB 제약·네트워크 인증·트랜잭션은 검증하지 않았다. 브라우저용 MSW도 같은 handlers를 사용한다.

### 로컬 연결 설정 예시

```dotenv
VITE_USE_MOCK=false
VITE_API_BASE_URL=/api
API_PROXY_TARGET=http://localhost:8080
```

개발 서버를 재시작하면 브라우저의 `/api` 요청을 Vite가 백엔드로 전달한다. 현재 프록시는 `/api` 접두사를 제거하지 않는다. 백엔드가 `/locations`처럼 접두사 없이 구현하면 합의 후 rewrite 설정을 추가한다. 이 프록시는 개발 서버용이므로 배포 환경 설정으로 간주하지 않는다.

### 검토 기준 파일

- `src/api/*.ts`: 화면에서 실제로 보내는 HTTP 요청과 multipart 필드
- `src/types/*.ts`: 요청·응답 타입
- `src/mocks/handlers.ts`: 목의 권한·검증·상태 처리
- `.env.example`, `vite.config.ts`: 로컬 API 주소와 프록시

문서 작업에서는 실제 백엔드를 호출하거나 배포·외부 공유·Git 커밋을 하지 않는다. 백엔드 구현 여부는 문서 작성만으로 확인할 수 없다.
