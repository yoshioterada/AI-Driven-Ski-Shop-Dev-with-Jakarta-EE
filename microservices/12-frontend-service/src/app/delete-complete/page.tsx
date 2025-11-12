'use client';

import React from 'react';
import Link from 'next/link';

export default function DeleteCompletePage() {
  return (
    <div className="min-h-screen bg-gray-50 flex flex-col justify-center py-12 sm:px-6 lg:px-8">
      <div className="sm:mx-auto sm:w-full sm:max-w-md">
        {/* Info Icon */}
        <div className="flex justify-center">
          <div className="rounded-full bg-blue-100 p-3">
            <svg 
              className="h-12 w-12 text-blue-600" 
              fill="none" 
              stroke="currentColor" 
              viewBox="0 0 24 24"
            >
              <path 
                strokeLinecap="round" 
                strokeLinejoin="round" 
                strokeWidth={2} 
                d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" 
              />
            </svg>
          </div>
        </div>

        {/* Message */}
        <div className="mt-6 text-center">
          <h2 className="text-3xl font-bold text-gray-900">
            アカウントを削除しました
          </h2>
          <p className="mt-4 text-base text-gray-600">
            ご利用ありがとうございました。
            <br />
            アカウントに関連するすべてのデータが削除されました。
          </p>
        </div>

        {/* Action Buttons */}
        <div className="mt-8 bg-white py-8 px-4 shadow sm:rounded-lg sm:px-10">
          <div className="space-y-4">
            <Link
              href="/"
              className="w-full flex justify-center py-3 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
            >
              トップページへ
            </Link>

            <Link
              href="/register"
              className="w-full flex justify-center py-3 px-4 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
            >
              新しいアカウントを作成
            </Link>
          </div>
        </div>

        {/* Additional Info */}
        <div className="mt-6 text-center">
          <p className="text-sm text-gray-500">
            再度ご利用いただく場合は、新しいアカウントを作成してください。
            <br />
            削除されたデータの復元はできません。
          </p>
        </div>

        {/* Support Link */}
        <div className="mt-6 text-center">
          <p className="text-xs text-gray-400">
            何かお困りのことがございましたら、
            <Link href="/contact" className="text-blue-600 hover:text-blue-500">
              お問い合わせ
            </Link>
            ください。
          </p>
        </div>
      </div>
    </div>
  );
}
