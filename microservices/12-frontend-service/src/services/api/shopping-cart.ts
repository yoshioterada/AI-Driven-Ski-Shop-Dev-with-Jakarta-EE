/**
 * Shopping Cart Service API Client
 * 
 * Shopping Cart Serviceへのアクセス用APIクライアント
 */

import axios, { AxiosInstance } from 'axios';
import {
  CartResponse,
  AddToCartRequest,
  UpdateQuantityRequest,
  CartValidationResponse
} from '../../types/cart';

// Shopping Cart Serviceの直接URL
const SHOPPING_CART_BASE_URL = process.env.NEXT_PUBLIC_SHOPPING_CART_URL || 'http://localhost:8088';

/**
 * APIベースURLを取得
 */
const getBaseUrl = (): string => {
  const useApiGateway = process.env.NEXT_PUBLIC_USE_API_GATEWAY === 'true';
  
  if (useApiGateway) {
    // API Gateway経由の場合
    return process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080';
  } else {
    // Shopping Cart Serviceに直接アクセス
    return SHOPPING_CART_BASE_URL;
  }
};

/**
 * Shopping Cart Service専用HTTPクライアント
 */
class ShoppingCartHttpClient {
  private readonly axiosInstance: AxiosInstance;

  constructor(baseURL: string) {
    this.axiosInstance = axios.create({
      baseURL,
      timeout: 10000,
      headers: {
        'Content-Type': 'application/json',
      },
    });

    this.setupInterceptors();
  }

  private setupInterceptors(): void {
    // リクエストインターセプター
    this.axiosInstance.interceptors.request.use(
      (config) => {
        console.log(`[ShoppingCart] ${config.method?.toUpperCase()} ${config.url}`);
        return config;
      },
      (error) => {
        console.error('[ShoppingCart] Request error:', error);
        return Promise.reject(new Error('Request failed'));
      }
    );

    // レスポンスインターセプター
    this.axiosInstance.interceptors.response.use(
      (response) => {
        console.log(`[ShoppingCart] ${response.status} ${response.config.url}`);
        return response;
      },
      (error) => {
        console.error('[ShoppingCart] Response error:', error);
        return Promise.reject(new Error('Response failed'));
      }
    );
  }

  public updateBaseURL(baseURL: string): void {
    this.axiosInstance.defaults.baseURL = baseURL;
  }

  public getAxiosInstance(): AxiosInstance {
    return this.axiosInstance;
  }
}

/**
 * Shopping Cart Service API Client
 */
export class ShoppingCartApi {
  private readonly httpClient: ShoppingCartHttpClient;

  constructor() {
    this.httpClient = new ShoppingCartHttpClient(getBaseUrl());
  }

  /**
   * API Gateway経由に切り替える
   */
  public switchToApiGateway(): void {
    const gatewayUrl = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080';
    this.httpClient.updateBaseURL(gatewayUrl);
  }

  /**
   * Shopping Cart Service直接アクセスに切り替える
   */
  public switchToDirectAccess(): void {
    this.httpClient.updateBaseURL(SHOPPING_CART_BASE_URL);
  }

  // =================
  // カート管理API
  // =================

  /**
   * セッションベースでカートを取得または作成
   */
  async getOrCreateCartBySession(sessionId: string): Promise<CartResponse> {
    const response = await this.httpClient.getAxiosInstance().get<CartResponse>(
      `/api/v1/carts/session/${sessionId}`
    );
    return response.data;
  }

  /**
   * 顧客ベースでカートを取得または作成
   */
  async getOrCreateCartByCustomer(customerId: string): Promise<CartResponse> {
    const response = await this.httpClient.getAxiosInstance().get<CartResponse>(
      `/api/v1/carts/customer/${customerId}`
    );
    return response.data;
  }

  /**
   * カートIDでカート詳細を取得
   */
  async getCartById(cartId: string): Promise<CartResponse> {
    const response = await this.httpClient.getAxiosInstance().get<CartResponse>(
      `/api/v1/carts/${cartId}`
    );
    return response.data;
  }

  // =================
  // カート商品操作API
  // =================

  /**
   * カートに商品を追加
   */
  async addItemToCart(cartId: string, item: AddToCartRequest): Promise<CartResponse> {
    const response = await this.httpClient.getAxiosInstance().post<CartResponse>(
      `/api/v1/carts/${cartId}/items`,
      item
    );
    return response.data;
  }

  /**
   * カート内商品の数量を更新
   */
  async updateItemQuantity(cartId: string, sku: string, request: UpdateQuantityRequest): Promise<CartResponse> {
    const response = await this.httpClient.getAxiosInstance().put<CartResponse>(
      `/api/v1/carts/${cartId}/items/${sku}/quantity`,
      request
    );
    return response.data;
  }

  /**
   * カートから商品を削除
   */
  async removeItemFromCart(cartId: string, sku: string): Promise<CartResponse> {
    const response = await this.httpClient.getAxiosInstance().delete<CartResponse>(
      `/api/v1/carts/${cartId}/items/${sku}`
    );
    return response.data;
  }

  /**
   * カートをクリア（全商品削除）
   */
  async clearCart(cartId: string): Promise<void> {
    try {
      console.log(`Clearing cart: ${cartId}`);
      const response = await this.httpClient.getAxiosInstance().delete(
        `/api/v1/carts/${cartId}/items`
      );
      console.log('Clear cart response:', response.status, response.data);
    } catch (error: unknown) {
      console.error('Clear cart API error:', error);
      if (error && typeof error === 'object' && 'response' in error) {
        const axiosError = error as { response?: { status: number; data: unknown } };
        console.error('Error response:', axiosError.response?.status, axiosError.response?.data);
        throw new Error(`API Error: ${axiosError.response.status} - ${JSON.stringify(axiosError.response.data)}`);
      } else if (error && typeof error === 'object' && 'request' in error) {
        const axiosError = error as { request: unknown };
        console.error('Network error:', axiosError.request);
        throw new Error('Network Error: Unable to connect to cart service');
      } else {
        const errorMessage = error instanceof Error ? error.message : 'Unknown error';
        console.error('Request setup error:', errorMessage);
        throw new Error(`Request Error: ${errorMessage}`);
      }
    }
  }

  // =================
  // 高度な操作API
  // =================

  /**
   * カート検証
   */
  async validateCart(cartId: string): Promise<CartValidationResponse> {
    const response = await this.httpClient.getAxiosInstance().post<CartValidationResponse>(
      `/api/v1/carts/${cartId}/validate`
    );
    return response.data;
  }

  /**
   * ゲストカートを顧客カートにマージ
   */
  async mergeGuestCart(guestCartId: string, customerId: string): Promise<CartResponse> {
    const response = await this.httpClient.getAxiosInstance().post<CartResponse>(
      `/api/v1/carts/${guestCartId}/merge/${customerId}`
    );
    return response.data;
  }

  // =================
  // システム関連API
  // =================

  /**
   * ヘルスチェック
   */
  async getHealth(): Promise<{ status: string; checks: Array<{ name: string; status: string; data?: Record<string, unknown> }> }> {
    const response = await this.httpClient.getAxiosInstance().get('/q/health');
    return response.data;
  }
}

// デフォルトインスタンス
export const shoppingCartApi = new ShoppingCartApi();

// 便利関数
export const switchCartApiToGateway = () => {
  shoppingCartApi.switchToApiGateway();
};

export const switchCartApiToDirectAccess = () => {
  shoppingCartApi.switchToDirectAccess();
};
