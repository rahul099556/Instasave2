'use server'

import { headers } from 'next/headers'

export async function fetchInstagramMedia(url: string) {
  if (!url?.trim()) {
    return { error: 'Please enter a valid Instagram URL.' }
  }

  try {
    const headerList = await headers()

    const protocol =
      headerList.get('x-forwarded-proto') || 'https'

    const host = headerList.get('host')

    if (!host) {
      return { error: 'Could not determine server address.' }
    }

    const response = await fetch(
      `${protocol}://${host}/api/resolve`,
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          url: url.trim(),
        }),
        cache: 'no-store',
      }
    )

    const data = await response.json()

    if (!response.ok) {
      return {
        error:
          data?.error ||
          'Could not extract media from this Instagram URL.',
      }
    }

    return data
  } catch (error) {
    console.error('Instagram media error:', error)

    return {
      error:
        'Something went wrong while processing the Instagram URL.',
    }
  }
}
