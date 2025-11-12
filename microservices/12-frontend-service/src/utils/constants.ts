// アプリケーション定数
export const APP_NAME = 'Ski Resort Shop';
export const APP_VERSION = '1.0.0';
export const APP_DESCRIPTION = 'スキー用品専門オンラインショップ';

// API設定
export const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080';
export const USER_MANAGEMENT_BASE_URL = process.env.NEXT_PUBLIC_USER_MANAGEMENT_URL || 
  `${API_BASE_URL}/user-management-service/api`;
export const API_TIMEOUT = 30000; // 30秒
export const API_RETRY_COUNT = 3;
export const API_RETRY_DELAY = 1000; // 1秒

// ページネーション
export const DEFAULT_PAGE_SIZE = 20;
export const MAX_PAGE_SIZE = 100;
export const PAGE_SIZE_OPTIONS = [10, 20, 50, 100];

// 検索設定
export const SEARCH_DEBOUNCE_DELAY = 300; // 300ms
export const MIN_SEARCH_LENGTH = 2;
export const MAX_SEARCH_SUGGESTIONS = 10;

// キャッシュ設定
export const CACHE_TTL = {
  SHORT: 5 * 60 * 1000,      // 5分
  MEDIUM: 30 * 60 * 1000,    // 30分
  LONG: 2 * 60 * 60 * 1000,  // 2時間
  VERY_LONG: 24 * 60 * 60 * 1000 // 24時間
};

// ローカルストレージキー
export const STORAGE_KEYS = {
  AUTH_TOKEN: 'auth_token',
  REFRESH_TOKEN: 'refresh_token',
  USER_PREFERENCES: 'user_preferences',
  CART: 'shopping_cart',
  RECENT_SEARCHES: 'recent_searches',
  WISHLIST: 'wishlist',
  THEME: 'theme_preference',
  LANGUAGE: 'language_preference'
} as const;

// テーマ設定
export const THEMES = {
  LIGHT: 'light',
  DARK: 'dark',
  AUTO: 'auto'
} as const;

// 言語設定
export const LANGUAGES = {
  JA: 'ja',
  EN: 'en'
} as const;

// 通貨設定
export const CURRENCIES = {
  JPY: 'JPY',
  USD: 'USD',
  EUR: 'EUR'
} as const;

// 商品カテゴリ
export const PRODUCT_CATEGORIES = {
  SKIS: 'skis',
  SNOWBOARDS: 'snowboards',
  BOOTS: 'boots',
  BINDINGS: 'bindings',
  POLES: 'poles',
  HELMETS: 'helmets',
  GOGGLES: 'goggles',
  GLOVES: 'gloves',
  APPAREL: 'apparel',
  ACCESSORIES: 'accessories',
  BAGS: 'bags',
  MAINTENANCE: 'maintenance'
} as const;

// 商品ソート順
export const PRODUCT_SORT_OPTIONS = [
  { value: 'relevance', label: '関連度順' },
  { value: 'price_asc', label: '価格の安い順' },
  { value: 'price_desc', label: '価格の高い順' },
  { value: 'rating', label: '評価の高い順' },
  { value: 'newest', label: '新着順' },
  { value: 'popular', label: '人気順' }
] as const;

// 注文ステータス
export const ORDER_STATUS = {
  PENDING: 'pending',
  CONFIRMED: 'confirmed',
  PROCESSING: 'processing',
  SHIPPED: 'shipped',
  DELIVERED: 'delivered',
  CANCELLED: 'cancelled',
  REFUNDED: 'refunded'
} as const;

// 注文ステータスラベル
export const ORDER_STATUS_LABELS = {
  [ORDER_STATUS.PENDING]: '注文確認中',
  [ORDER_STATUS.CONFIRMED]: '注文確定',
  [ORDER_STATUS.PROCESSING]: '処理中',
  [ORDER_STATUS.SHIPPED]: '発送済み',
  [ORDER_STATUS.DELIVERED]: '配達完了',
  [ORDER_STATUS.CANCELLED]: 'キャンセル',
  [ORDER_STATUS.REFUNDED]: '返金済み'
} as const;

// 支払い方法
export const PAYMENT_METHODS = {
  CREDIT_CARD: 'credit_card',
  DEBIT_CARD: 'debit_card',
  PAYPAL: 'paypal',
  APPLE_PAY: 'apple_pay',
  GOOGLE_PAY: 'google_pay',
  BANK_TRANSFER: 'bank_transfer',
  CONVENIENCE_STORE: 'convenience_store'
} as const;

// 配送方法
export const SHIPPING_METHODS = {
  STANDARD: 'standard',
  EXPRESS: 'express',
  OVERNIGHT: 'overnight',
  PICKUP: 'pickup'
} as const;

// ファイルアップロード設定
export const FILE_UPLOAD = {
  MAX_SIZE: 10 * 1024 * 1024, // 10MB
  ALLOWED_TYPES: [
    'image/jpeg',
    'image/jpg',
    'image/png',
    'image/gif',
    'image/webp'
  ],
  MAX_FILES: 5
} as const;

// バリデーション設定
export const VALIDATION = {
  PASSWORD_MIN_LENGTH: 8,
  USERNAME_MIN_LENGTH: 3,
  USERNAME_MAX_LENGTH: 20,
  EMAIL_PATTERN: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
  PHONE_PATTERN: /^[0-9-+().\s]+$/,
  POSTAL_CODE_PATTERN: /^\d{3}-?\d{4}$/,
  CREDIT_CARD_PATTERN: /^\d{4}\s?\d{4}\s?\d{4}\s?\d{4}$/
} as const;

// レスポンシブブレークポイント
export const BREAKPOINTS = {
  SM: '640px',
  MD: '768px',
  LG: '1024px',
  XL: '1280px',
  '2XL': '1536px'
} as const;

// アニメーション設定
export const ANIMATIONS = {
  DURATION: {
    FAST: 150,
    NORMAL: 300,
    SLOW: 500
  },
  EASING: {
    EASE_IN: 'cubic-bezier(0.4, 0, 1, 1)',
    EASE_OUT: 'cubic-bezier(0, 0, 0.2, 1)',
    EASE_IN_OUT: 'cubic-bezier(0.4, 0, 0.2, 1)'
  }
} as const;

// エラーメッセージ
export const ERROR_MESSAGES = {
  NETWORK_ERROR: 'ネットワークエラーが発生しました',
  SERVER_ERROR: 'サーバーエラーが発生しました',
  VALIDATION_ERROR: '入力内容に誤りがあります',
  AUTHENTICATION_ERROR: '認証に失敗しました',
  AUTHORIZATION_ERROR: 'アクセス権限がありません',
  NOT_FOUND: 'ページが見つかりません',
  TIMEOUT_ERROR: 'タイムアウトしました',
  UNKNOWN_ERROR: '予期しないエラーが発生しました'
} as const;

// 成功メッセージ
export const SUCCESS_MESSAGES = {
  LOGIN_SUCCESS: 'ログインしました',
  LOGOUT_SUCCESS: 'ログアウトしました',
  REGISTER_SUCCESS: 'アカウントを作成しました',
  UPDATE_SUCCESS: '更新しました',
  DELETE_SUCCESS: '削除しました',
  SAVE_SUCCESS: '保存しました',
  SEND_SUCCESS: '送信しました',
  CART_ADD_SUCCESS: 'カートに追加しました',
  ORDER_SUCCESS: '注文を確定しました'
} as const;

// ソーシャルメディア
export const SOCIAL_LINKS = {
  TWITTER: 'https://twitter.com/skiresortshop',
  FACEBOOK: 'https://facebook.com/skiresortshop',
  INSTAGRAM: 'https://instagram.com/skiresortshop',
  YOUTUBE: 'https://youtube.com/skiresortshop'
} as const;

// 連絡先情報
export const CONTACT_INFO = {
  EMAIL: 'support@skiresortshop.com',
  PHONE: '03-1234-5678',
  ADDRESS: '東京都渋谷区神南1-2-3',
  BUSINESS_HOURS: '平日 9:00-18:00'
} as const;
