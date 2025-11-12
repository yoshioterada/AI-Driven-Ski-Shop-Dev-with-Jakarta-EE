'use client';

import React, { useState, useEffect } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { updatePassword } from '@/services/api/userManagement';
import { useAuthStore } from '@/stores/authStore';
import { validatePassword } from '@/utils/validators';

export default function UpdatePasswordPage() {
  const [formData, setFormData] = useState({
    currentPassword: '',
    newPassword: '',
    confirmPassword: '',
  });
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  
  const router = useRouter();
  const { user, isAuthenticated } = useAuthStore();

  // Redirect if not authenticated
  useEffect(() => {
    if (!isAuthenticated || !user) {
      router.push('/login');
    }
  }, [isAuthenticated, user, router]);

  const handleChange = (field: string, value: string) => {
    setFormData(prev => ({
      ...prev,
      [field]: value
    }));
    // Clear field error when user starts typing
    if (fieldErrors[field]) {
      setFieldErrors(prev => {
        const newErrors = { ...prev };
        delete newErrors[field];
        return newErrors;
      });
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setError('');
    setFieldErrors({});

    // Validation
    const errors: Record<string, string> = {};

    if (!formData.currentPassword) {
      errors.currentPassword = '現在のパスワードを入力してください';
    }

    const passwordValidation = validatePassword(formData.newPassword);
    if (!passwordValidation.isValid) {
      errors.newPassword = passwordValidation.errors[0];
    }

    if (formData.newPassword !== formData.confirmPassword) {
      errors.confirmPassword = '新しいパスワードが一致しません';
    }

    if (formData.currentPassword === formData.newPassword) {
      errors.newPassword = '現在のパスワードと同じパスワードは使用できません';
    }

    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors);
      setIsLoading(false);
      return;
    }

    try {
      if (!user?.id) {
        throw new Error('ユーザー情報が取得できません');
      }

      await updatePassword({
        id: parseInt(user.id),
        currentPassword: formData.currentPassword,
        newPassword: formData.newPassword,
      });

      // Reset form
      setFormData({
        currentPassword: '',
        newPassword: '',
        confirmPassword: '',
      });

      // Redirect to completion page
      router.push('/update-profile-complete?type=password');
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'パスワードの更新に失敗しました';
      setError(errorMessage);
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col justify-center py-12 sm:px-6 lg:px-8">
      <div className="sm:mx-auto sm:w-full sm:max-w-md">
        {/* Header */}
        <div className="text-center">
          <h2 className="text-3xl font-bold text-gray-900">
            パスワード変更
          </h2>
          <p className="mt-2 text-sm text-gray-600">
            セキュリティのため、定期的にパスワードを変更することをお勧めします
          </p>
        </div>

        {/* Password Form */}
        <div className="mt-8 bg-white py-8 px-4 shadow sm:rounded-lg sm:px-10">
          <form onSubmit={handleSubmit} className="space-y-6">
            {error && (
              <div className="bg-red-50 border border-red-200 rounded-md p-4">
                <p className="text-sm text-red-600">{error}</p>
              </div>
            )}

            <div>
              <label htmlFor="currentPassword" className="block text-sm font-medium text-gray-700">
                現在のパスワード <span className="text-red-500">*</span>
              </label>
              <input
                id="currentPassword"
                type="password"
                value={formData.currentPassword}
                onChange={(e) => handleChange('currentPassword', e.target.value)}
                required
                className={`mt-1 block w-full px-3 py-2 border rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 ${
                  fieldErrors.currentPassword ? 'border-red-300' : 'border-gray-300'
                }`}
              />
              {fieldErrors.currentPassword && (
                <p className="mt-1 text-xs text-red-600">{fieldErrors.currentPassword}</p>
              )}
            </div>

            <div>
              <label htmlFor="newPassword" className="block text-sm font-medium text-gray-700">
                新しいパスワード <span className="text-red-500">*</span>
              </label>
              <input
                id="newPassword"
                type="password"
                value={formData.newPassword}
                onChange={(e) => handleChange('newPassword', e.target.value)}
                required
                className={`mt-1 block w-full px-3 py-2 border rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 ${
                  fieldErrors.newPassword ? 'border-red-300' : 'border-gray-300'
                }`}
              />
              {fieldErrors.newPassword && (
                <p className="mt-1 text-xs text-red-600">{fieldErrors.newPassword}</p>
              )}
              <p className="mt-1 text-xs text-gray-500">
                8文字以上、大文字・小文字・数字・特殊文字を含む
              </p>
            </div>

            <div>
              <label htmlFor="confirmPassword" className="block text-sm font-medium text-gray-700">
                新しいパスワード（確認） <span className="text-red-500">*</span>
              </label>
              <input
                id="confirmPassword"
                type="password"
                value={formData.confirmPassword}
                onChange={(e) => handleChange('confirmPassword', e.target.value)}
                required
                className={`mt-1 block w-full px-3 py-2 border rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 ${
                  fieldErrors.confirmPassword ? 'border-red-300' : 'border-gray-300'
                }`}
              />
              {fieldErrors.confirmPassword && (
                <p className="mt-1 text-xs text-red-600">{fieldErrors.confirmPassword}</p>
              )}
            </div>

            <div className="flex items-center justify-between pt-4">
              <Link
                href="/update-profile"
                className="text-sm text-blue-600 hover:text-blue-500"
              >
                ← プロフィール編集に戻る
              </Link>
              
              <button
                type="submit"
                disabled={isLoading}
                className="flex justify-center py-2 px-6 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50"
              >
                {isLoading ? '変更中...' : 'パスワードを変更'}
              </button>
            </div>
          </form>

          {/* Security Tips */}
          <div className="mt-6 pt-6 border-t border-gray-200">
            <h3 className="text-sm font-medium text-gray-900 mb-2">
              安全なパスワードのヒント
            </h3>
            <ul className="text-xs text-gray-600 space-y-1">
              <li>• 他のサイトで使用していないパスワードを使用する</li>
              <li>• 定期的にパスワードを変更する</li>
              <li>• 推測されやすい個人情報（名前、誕生日など）を含めない</li>
              <li>• 辞書に載っている単語を避ける</li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  );
}
