'use client';

import React, { useEffect, useState, Suspense } from 'react';
import Link from 'next/link';
import { useSearchParams } from 'next/navigation';

function UpdateProfileCompleteContent() {
  const searchParams = useSearchParams();
  const [updateType, setUpdateType] = useState<'profile' | 'password'>('profile');

  useEffect(() => {
    const type = searchParams.get('type');
    if (type === 'password') {
      setUpdateType('password');
    }
  }, [searchParams]);

  const getMessage = () => {
    if (updateType === 'password') {
      return {
        title: 'パスワードを更新しました',
        description: 'パスワードの変更が完了しました。次回ログイン時は新しいパスワードをご使用ください。',
      };
    }
    return {
      title: 'プロフィールを更新しました',
      description: 'プロフィール情報の変更が正常に保存されました。',
    };
  };

  const message = getMessage();

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col justify-center py-12 sm:px-6 lg:px-8">
      <div className="sm:mx-auto sm:w-full sm:max-w-md">
        {/* Success Icon */}
        <div className="flex justify-center">
          <div className="rounded-full bg-green-100 p-3">
            <svg 
              className="h-12 w-12 text-green-600" 
              fill="none" 
              stroke="currentColor" 
              viewBox="0 0 24 24"
            >
              <path 
                strokeLinecap="round" 
                strokeLinejoin="round" 
                strokeWidth={2} 
                d="M5 13l4 4L19 7" 
              />
            </svg>
          </div>
        </div>

        {/* Message */}
        <div className="mt-6 text-center">
          <h2 className="text-3xl font-bold text-gray-900">
            {message.title}
          </h2>
          <p className="mt-4 text-base text-gray-600">
            {message.description}
          </p>
        </div>

        {/* Action Buttons */}
        <div className="mt-8 bg-white py-8 px-4 shadow sm:rounded-lg sm:px-10">
          <div className="space-y-4">
            <Link
              href="/update-profile"
              className="w-full flex justify-center py-3 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
            >
              プロフィール編集に戻る
            </Link>

            <Link
              href="/profile"
              className="w-full flex justify-center py-3 px-4 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
            >
              マイページへ
            </Link>

            <Link
              href="/"
              className="w-full flex justify-center py-3 px-4 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
            >
              トップページへ
            </Link>
          </div>
        </div>

        {/* Additional Info */}
        {updateType === 'password' && (
          <div className="mt-6 text-center">
            <p className="text-sm text-gray-500">
              セキュリティのため、次回ログイン時に新しいパスワードが必要になります。
            </p>
          </div>
        )}
      </div>
    </div>
  );
}

export default function UpdateProfileCompletePage() {
  return (
    <Suspense fallback={
      <div className="min-h-screen bg-gray-50 flex flex-col justify-center py-12 sm:px-6 lg:px-8">
        <div className="text-center">
          <div className="inline-block animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
        </div>
      </div>
    }>
      <UpdateProfileCompleteContent />
    </Suspense>
  );
}
