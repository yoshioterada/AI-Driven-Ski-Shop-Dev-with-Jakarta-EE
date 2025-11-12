import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios';
import { API_BASE_URL, API_TIMEOUT, STORAGE_KEYS } from '@/utils/constants';
import type { ApiResponse, ApiError } from '@/types/api';

/**
 * HTTPクライアントクラス
 */
class HttpClient {
  private instance: AxiosInstance;

  constructor() {
    this.instance = axios.create({
      baseURL: API_BASE_URL,
      timeout: API_TIMEOUT,
      headers: {
        'Content-Type': 'application/json',
      },
    });

    this.setupInterceptors();
  }

  /**
   * リクエスト/レスポンスインターセプターを設定
   */
  private setupInterceptors(): void {
    // リクエストインターセプター
    this.instance.interceptors.request.use(
      (config) => {
        // 認証トークンを自動で追加
        if (typeof window !== 'undefined') {
          const token = localStorage.getItem(STORAGE_KEYS.AUTH_TOKEN);
          if (token) {
            config.headers.Authorization = `Bearer ${token}`;
          }
        }

        // リクエストログ
        console.log(`[HTTP] ${config.method?.toUpperCase()} ${config.url}`);
        
        return config;
      },
      (error) => {
        console.error('[HTTP] Request error:', error);
        return Promise.reject(error);
      }
    );

    // レスポンスインターセプター
    this.instance.interceptors.response.use(
      (response: AxiosResponse<ApiResponse>) => {
        // レスポンスログ
        console.log(`[HTTP] ${response.status} ${response.config.url}`);
        
        return response;
      },
      async (error) => {
        console.error('[HTTP] Response error:', error);

        // 401エラーの場合、トークンをリフレッシュを試行
        if (error.response?.status === 401) {
          try {
            await this.refreshToken();
            // 元のリクエストを再試行
            return this.instance.request(error.config);
          } catch {
            // リフレッシュに失敗した場合、ログアウト処理
            this.logout();
            window.location.href = '/login';
          }
        }

        // APIエラーレスポンスを標準化
        const apiError: ApiError = {
          code: error.response?.data?.code || 'UNKNOWN_ERROR',
          message: error.response?.data?.message || error.message,
          details: error.response?.data?.details,
          timestamp: new Date().toISOString(),
          requestId: error.response?.headers?.['x-request-id'],
        };

        return Promise.reject(apiError);
      }
    );
  }

  /**
   * トークンをリフレッシュ
   */
  private async refreshToken(): Promise<void> {
    if (typeof window === 'undefined') return;

    const refreshToken = localStorage.getItem(STORAGE_KEYS.REFRESH_TOKEN);
    if (!refreshToken) {
      throw new Error('No refresh token available');
    }

    const response = await this.instance.post('/auth/refresh', {
      refreshToken,
    });

    const { token, refreshToken: newRefreshToken } = response.data.data;
    localStorage.setItem(STORAGE_KEYS.AUTH_TOKEN, token);
    localStorage.setItem(STORAGE_KEYS.REFRESH_TOKEN, newRefreshToken);
  }

  /**
   * ログアウト処理
   */
  private logout(): void {
    if (typeof window === 'undefined') return;

    localStorage.removeItem(STORAGE_KEYS.AUTH_TOKEN);
    localStorage.removeItem(STORAGE_KEYS.REFRESH_TOKEN);
  }

  /**
   * GETリクエスト
   */
  public async get<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
    const response = await this.instance.get<ApiResponse<T>>(url, config);
    return response.data.data;
  }

  /**
   * POSTリクエスト
   */
  public async post<T>(
    url: string,
    data?: unknown,
    config?: AxiosRequestConfig
  ): Promise<T> {
    const response = await this.instance.post<ApiResponse<T>>(url, data, config);
    return response.data.data;
  }

  /**
   * PUTリクエスト
   */
  public async put<T>(
    url: string,
    data?: unknown,
    config?: AxiosRequestConfig
  ): Promise<T> {
    const response = await this.instance.put<ApiResponse<T>>(url, data, config);
    return response.data.data;
  }

  /**
   * PATCHリクエスト
   */
  public async patch<T>(
    url: string,
    data?: unknown,
    config?: AxiosRequestConfig
  ): Promise<T> {
    const response = await this.instance.patch<ApiResponse<T>>(url, data, config);
    return response.data.data;
  }

  /**
   * DELETEリクエスト
   */
  public async delete<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
    const response = await this.instance.delete<ApiResponse<T>>(url, config);
    return response.data.data;
  }

  /**
   * RAW GETリクエスト（レスポンスをラップしない）
   */
  public async getRaw<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
    const response = await this.instance.get<T>(url, config);
    return response.data;
  }

  /**
   * RAW POSTリクエスト（レスポンスをラップしない）
   */
  public async postRaw<T>(
    url: string,
    data?: unknown,
    config?: AxiosRequestConfig
  ): Promise<T> {
    const response = await this.instance.post<T>(url, data, config);
    return response.data;
  }

  /**
   * RAW PUTリクエスト（レスポンスをラップしない）
   */
  public async putRaw<T>(
    url: string,
    data?: unknown,
    config?: AxiosRequestConfig
  ): Promise<T> {
    const response = await this.instance.put<T>(url, data, config);
    return response.data;
  }

  /**
   * RAW DELETEリクエスト（レスポンスをラップしない、204の場合はvoidを返す）
   */
  public async deleteRaw(url: string, config?: AxiosRequestConfig): Promise<void> {
    await this.instance.delete(url, config);
  }

  /**
   * ファイルアップロード
   */
  public async upload<T>(
    url: string,
    formData: FormData,
    onProgress?: (progress: number) => void
  ): Promise<T> {
    const config: AxiosRequestConfig = {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
      onUploadProgress: (progressEvent) => {
        if (onProgress && progressEvent.total) {
          const progress = Math.round(
            (progressEvent.loaded * 100) / progressEvent.total
          );
          onProgress(progress);
        }
      },
    };

    const response = await this.instance.post<ApiResponse<T>>(url, formData, config);
    return response.data.data;
  }

  /**
   * ダウンロード
   */
  public async download(url: string, filename: string): Promise<void> {
    const response = await this.instance.get(url, {
      responseType: 'blob',
    });

    const blob = new Blob([response.data]);
    const downloadUrl = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = downloadUrl;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(downloadUrl);
  }

  /**
   * リクエストをキャンセル
   */
  public createCancelToken() {
    return axios.CancelToken.source();
  }

  /**
   * Axiosインスタンスを取得（直接操作が必要な場合）
   */
  public getInstance(): AxiosInstance {
    return this.instance;
  }
}

// シングルトンインスタンスをエクスポート
export const httpClient = new HttpClient();
export default httpClient;
