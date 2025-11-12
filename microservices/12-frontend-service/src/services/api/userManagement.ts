import axios, { AxiosInstance, AxiosRequestConfig } from 'axios';
import { USER_MANAGEMENT_BASE_URL, API_TIMEOUT, STORAGE_KEYS } from '@/utils/constants';

/**
 * User Management Service API Client
 */

// Base User Payload
export interface BaseUserPayload {
  firstName: string;
  lastName: string;
  email: string;
  role?: 'CUSTOMER' | 'INSTRUCTOR' | 'STAFF' | 'ADMIN';
  phoneNumber?: string;
}

// Create User Payload
export interface CreateUserPayload extends BaseUserPayload {
  username: string;
  password: string;
}

// Update User Payload
export interface UpdateUserPayload extends BaseUserPayload {
  id: number;
}

// Update Password Payload
export interface UpdatePasswordPayload {
  id: number;
  currentPassword: string;
  newPassword: string;
}

// User DTO
export interface UserDto {
  id: number;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
  phoneNumber?: string;
  active: boolean;
  emailVerified: boolean;
  createdAt: string;
  updatedAt: string;
}

/**
 * Axios instance for User Management Service
 */
class UserManagementClient {
  private instance: AxiosInstance;

  constructor() {
    this.instance = axios.create({
      baseURL: USER_MANAGEMENT_BASE_URL,
      timeout: API_TIMEOUT,
      headers: {
        'Content-Type': 'application/json',
      },
    });

    this.setupInterceptors();
  }

  private setupInterceptors(): void {
    // Request interceptor - add auth token
    this.instance.interceptors.request.use(
      (config) => {
        // Add authentication token if available (except for create user endpoint)
        if (typeof window !== 'undefined' && !config.url?.includes('/users') || config.method !== 'post') {
          const token = localStorage.getItem(STORAGE_KEYS.AUTH_TOKEN);
          if (token) {
            config.headers.Authorization = `Bearer ${token}`;
          }
        }
        
        console.log(`[UserManagement] ${config.method?.toUpperCase()} ${config.url}`);
        return config;
      },
      (error) => {
        console.error('[UserManagement] Request error:', error);
        return Promise.reject(error);
      }
    );

    // Response interceptor
    this.instance.interceptors.response.use(
      (response) => {
        console.log(`[UserManagement] ${response.status} ${response.config.url}`);
        return response;
      },
      (error) => {
        console.error('[UserManagement] Response error:', error);
        
        // Handle 401 - unauthorized
        if (error.response?.status === 401) {
          // Redirect to login
          if (typeof window !== 'undefined') {
            window.location.href = '/login';
          }
        }
        
        return Promise.reject(error);
      }
    );
  }

  getInstance(): AxiosInstance {
    return this.instance;
  }
}

const userManagementClient = new UserManagementClient();

/**
 * Create a new user
 */
export async function createUser(payload: CreateUserPayload): Promise<UserDto> {
  try {
    // Transform password field to passwordHash as expected by backend
    const requestBody = {
      username: payload.username,
      email: payload.email,
      passwordHash: payload.password, // Backend expects passwordHash
      firstName: payload.firstName,
      lastName: payload.lastName,
      role: payload.role || 'CUSTOMER',
      phoneNumber: payload.phoneNumber,
    };

    const response = await userManagementClient.getInstance().post<UserDto>('/users', requestBody);
    return response.data;
  } catch (error) {
    if (axios.isAxiosError(error)) {
      const errorData = error.response?.data;
      if (errorData?.code === 'USER_CREATION_FAILED') {
        throw new Error(errorData.message || 'ユーザー作成に失敗しました');
      }
    }
    throw error;
  }
}

/**
 * Fetch user by ID
 */
export async function fetchUser(id: number): Promise<UserDto> {
  try {
    const response = await userManagementClient.getInstance().get<UserDto>(`/users/${id}`);
    return response.data;
  } catch (error) {
    if (axios.isAxiosError(error)) {
      const errorData = error.response?.data;
      if (error.response?.status === 404) {
        throw new Error('ユーザーが見つかりません');
      }
      if (errorData?.message) {
        throw new Error(errorData.message);
      }
    }
    throw error;
  }
}

/**
 * Update user profile
 */
export async function updateUser(payload: UpdateUserPayload): Promise<UserDto> {
  try {
    const { id, ...updateData } = payload;
    
    const requestBody = {
      firstName: updateData.firstName,
      lastName: updateData.lastName,
      email: updateData.email,
      phoneNumber: updateData.phoneNumber,
      role: updateData.role,
    };

    const response = await userManagementClient.getInstance().put<UserDto>(`/users/${id}`, requestBody);
    return response.data;
  } catch (error) {
    if (axios.isAxiosError(error)) {
      const errorData = error.response?.data;
      if (error.response?.status === 404) {
        throw new Error('ユーザーが見つかりません');
      }
      if (errorData?.message) {
        throw new Error(errorData.message);
      }
    }
    throw error;
  }
}

/**
 * Update user password
 */
export async function updatePassword(payload: UpdatePasswordPayload): Promise<void> {
  try {
    const { id, currentPassword, newPassword } = payload;
    
    const requestBody = {
      currentPassword,
      newPassword,
    };

    await userManagementClient.getInstance().put(`/users/${id}/password`, requestBody);
  } catch (error) {
    if (axios.isAxiosError(error)) {
      const errorData = error.response?.data;
      if (error.response?.status === 404) {
        throw new Error('ユーザーが見つかりません');
      }
      if (errorData?.code === 'PASSWORD_UPDATE_FAILED') {
        throw new Error(errorData.message || 'パスワードの更新に失敗しました');
      }
      if (errorData?.message) {
        throw new Error(errorData.message);
      }
    }
    throw error;
  }
}

/**
 * Delete user account
 */
export async function deleteUser(id: number): Promise<void> {
  try {
    await userManagementClient.getInstance().delete(`/users/${id}`);
  } catch (error) {
    if (axios.isAxiosError(error)) {
      const errorData = error.response?.data;
      if (error.response?.status === 404) {
        throw new Error('ユーザーが見つかりません');
      }
      if (errorData?.message) {
        throw new Error(errorData.message);
      }
    }
    throw error;
  }
}
