import type { Location } from '../types/location'
import type { PublicUser } from '../types/user'
import type { PostCondition, PostStatus } from '../types/post'
import type { Deal } from '../types/deal'
export interface MockUser {
  loginId: string
  email: string
  name: string
  bio: string
  account: string
  locationId: string
  userRank: number
  passwordHash: string
}
export interface MockPost {
  id: string
  title: string
  price: number
  content: string
  contentImgUrl: string
  condition: PostCondition
  status: PostStatus
  sellerLoginId: string
  createdAt: string
}
export interface MockComment {
  id: string
  postId: string
  authorLoginId: string
  parentId: string | null
  content: string
  createdAt: string
}
const seoul =
  '강남구 강동구 강북구 강서구 관악구 광진구 구로구 금천구 노원구 도봉구 동대문구 동작구 마포구 서대문구 서초구 성동구 성북구 송파구 양천구 영등포구 용산구 은평구 종로구 중구 중랑구'.split(
    ' ',
  )
const gyeonggi =
  '수원시 성남시 의정부시 안양시 부천시 광명시 평택시 동두천시 안산시 고양시 과천시 구리시 남양주시 오산시 시흥시 군포시 의왕시 하남시 용인시 파주시 이천시 안성시 김포시 화성시 광주시 양주시 포천시 여주시 연천군 가평군 양평군'.split(
    ' ',
  )
export const locations: Location[] = [
  ...seoul.map((name) => ({ id: `seoul-${name}`, name: `서울특별시 ${name}` })),
  ...gyeonggi.map((name) => ({
    id: `gyeonggi-${name}`,
    name: `경기도 ${name}`,
  })),
]
export const state: {
  users: MockUser[]
  posts: MockPost[]
  comments: MockComment[]
  deals: Deal[]
  likes: Set<string>
  follows: Set<string>
  session: string | null
} = {
  users: [],
  posts: [],
  comments: [],
  deals: [],
  likes: new Set(),
  follows: new Set(),
  session: null,
}
// shortcut: demo-only password digest, real password storage and sessions belong to the backend.
export async function hashPassword(password: string) {
  const hash = await crypto.subtle.digest(
    'SHA-256',
    new TextEncoder().encode(password),
  )
  return Array.from(new Uint8Array(hash), (byte) =>
    byte.toString(16).padStart(2, '0'),
  ).join('')
}
export function pair(first: string | null, second: string) {
  return `${encodeURIComponent(first ?? '')}:${encodeURIComponent(second)}`
}
export function publicUser(loginId: string): PublicUser {
  const user = state.users.find((u) => u.loginId === loginId)!
  return {
    loginId: user.loginId,
    name: user.name,
    bio: user.bio,
    location: locations.find((l) => l.id === user.locationId)!,
    userRank: user.userRank,
    followerCount: [...state.follows].filter((key) =>
      key.endsWith(`:${encodeURIComponent(loginId)}`),
    ).length,
    followingCount: [...state.follows].filter((key) =>
      key.startsWith(`${encodeURIComponent(loginId)}:`),
    ).length,
    isFollowing: state.follows.has(pair(state.session, loginId)),
  }
}
export async function resetStore() {
  state.session = null
  state.likes = new Set()
  state.follows = new Set(['gazi_demo:neighbor', 'neighbor:gazi_demo'])
  const passwordHash = await hashPassword('Gazi1234')
  state.users = [
    {
      loginId: 'gazi_demo',
      email: 'demo@gazi.local',
      name: '가지로운 생활',
      bio: '필요한 물건은 나누고, 좋은 이웃은 만나고. 작은 거래부터 시작해요.',
      account: '',
      locationId: 'seoul-마포구',
      userRank: 78,
      passwordHash,
    },
    {
      loginId: 'neighbor',
      email: 'neighbor@gazi.local',
      name: '연남의 오후',
      bio: '취향이 담긴 물건을 다음 이웃에게 전합니다.',
      account: '',
      locationId: 'seoul-마포구',
      userRank: 92,
      passwordHash,
    },
    {
      loginId: 'green',
      email: 'green@gazi.local',
      name: '초록 한 조각',
      bio: '식물과 책을 좋아하는 이웃입니다.',
      account: '',
      locationId: 'gyeonggi-화성시',
      userRank: 65,
      passwordHash,
    },
  ]
  const items: [string, number, string, PostCondition, string, string][] = [
    [
      '함께 집중하던 원목 책상',
      45000,
      'desk',
      'USED',
      'neighbor',
      '이사를 준비하면서 아끼던 책상을 내놓아요. 튼튼하고 생활 흠집이 조금 있습니다. 직접 가져가실 수 있는 이웃을 찾아요.',
    ],
    [
      '공간을 따뜻하게, 테이블 조명',
      18000,
      'lamp',
      'LIKE_NEW',
      'neighbor',
      '한 달 정도 사용한 조명입니다. 전구도 함께 드려요. 댓글로 편하게 문의해 주세요.',
    ],
    [
      '주말 산책의 친구, 미니 자전거',
      85000,
      'bike',
      'USED',
      'gazi_demo',
      '가볍게 동네를 돌아다니기 좋은 자전거예요. 타이어 상태 좋고 브레이크 점검했습니다.',
    ],
    [
      '다음 이야기를 기다리는 책 세 권',
      12000,
      'books',
      'LIKE_NEW',
      'neighbor',
      '깨끗하게 읽은 책 세 권을 함께 판매해요. 밑줄이나 낙서 없습니다.',
    ],
    [
      '새 주인을 기다리는 머그 세트',
      15000,
      'mug',
      'NEW',
      'gazi_demo',
      '선물로 받은 미사용 머그 두 개입니다. 상자와 함께 드려요.',
    ],
    [
      '창가를 닮은 작은 초록 화분',
      8000,
      'plant',
      'USED',
      'green',
      '키우기 쉬운 식물이에요. 화분까지 같이 드립니다.',
    ],
  ]
  state.posts = items.map(
    ([title, price, image, condition, sellerLoginId, content], i) => ({
      id: String(i + 1),
      title,
      price,
      content,
      condition,
      sellerLoginId,
      contentImgUrl: `/images/${image}.webp`,
      status: i === 4 ? 'RESERVED' : 'ON_SALE',
      createdAt: new Date(Date.now() - i * 3600000).toISOString(),
    }),
  )
  state.comments = [
    {
      id: 'seed-comment-1',
      postId: '3',
      authorLoginId: 'neighbor',
      parentId: null,
      content: '안녕하세요! 이번 주말에 거래할 수 있을까요?',
      createdAt: new Date().toISOString(),
    },
    {
      id: 'seed-comment-2',
      postId: '1',
      authorLoginId: 'gazi_demo',
      parentId: null,
      content: '책상 가로 길이가 궁금해요.',
      createdAt: new Date().toISOString(),
    },
    {
      id: 'seed-comment-3',
      postId: '1',
      authorLoginId: 'neighbor',
      parentId: 'seed-comment-2',
      content: '가로 120cm, 세로 60cm예요.',
      createdAt: new Date().toISOString(),
    },
  ]
  state.deals = []
  state.likes.add('gazi_demo:1')
  state.likes.add('neighbor:3')
}
export const ready = resetStore()
