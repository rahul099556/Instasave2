'use client'

import { useState } from 'react';
import { fetchInstagramMedia } from './actions';

type MediaInfo = {
  url: string;
  imageUrl: string | null;
  videoUrl: string | null;
  isVideo: boolean;
};

export default function Home() {
  const [linkText, setLinkText] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [media, setMedia] = useState<MediaInfo | null>(null);

  const handlePaste = async () => {
    try {
      const text = await navigator.clipboard.readText();
      if (text) setLinkText(text);
    } catch (err) {
      console.error('Failed to read clipboard contents: ', err);
    }
  };

  const handleFetch = async () => {
    if (!linkText.trim()) return;
    setLoading(true);
    setError(null);
    setMedia(null);
    
    try {
      const res = await fetchInstagramMedia(linkText);
      if (res.error) {
        setError(res.error);
      } else if (res.data) {
        setMedia(res.data);
      }
    } catch (err: any) {
      setError(err.message || 'An unexpected error occurred.');
    } finally {
      setLoading(false);
    }
  };

  const handleDownload = async () => {
    if (!media) return;
    const downloadUrl = media.videoUrl || media.imageUrl;
    if (!downloadUrl) return;

    try {
      const response = await fetch(downloadUrl);
      const blob = await response.blob();
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.style.display = 'none';
      a.href = url;
      a.download = `InstaSave_${Date.now()}.${media.isVideo ? 'mp4' : 'jpg'}`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      alert("Failed to download file directly. You can right-click the file and choose 'Save Video As...' or 'Save Image As...'.");
    }
  };

  return (
    <main className="min-h-screen p-6 max-w-lg mx-auto flex flex-col items-center">
      <div className="mt-8 mb-2 flex items-center justify-center">
        <h1 className="text-4xl font-bold text-transparent bg-clip-text bg-insta-gradient">
          InstaSave
        </h1>
      </div>
      
      <p className="text-gray-500 mb-8 text-center">
        Instagram Reels & Stories Downloader
      </p>

      <div className="w-full relative">
        <input
          type="url"
          className="block w-full px-4 py-4 rounded-xl border border-gray-300 focus:ring-instaRed focus:border-instaRed dark:bg-gray-800 dark:border-gray-600 dark:text-white"
          placeholder="Paste Instagram link here"
          value={linkText}
          onChange={(e) => setLinkText(e.target.value)}
        />
        <div className="absolute inset-y-0 right-2 flex items-center">
          <button
            onClick={handlePaste}
            className="flex items-center space-x-1 px-3 py-1 text-sm font-bold text-instaRed hover:bg-gray-100 dark:hover:bg-gray-700 rounded-md transition-colors"
          >
            <span>Paste</span>
          </button>
        </div>
      </div>

      <button
        onClick={handleFetch}
        disabled={loading || !linkText.trim()}
        className="mt-4 w-full h-14 rounded-xl bg-insta-gradient text-white font-bold text-lg disabled:opacity-50 flex items-center justify-center"
      >
        {loading ? "Processing..." : "Get Preview"}
      </button>

      <div className="w-full mt-8">
        {loading && (
          <div className="flex flex-col items-center justify-center py-8">
            <p className="mt-4 text-gray-500">Processing link...</p>
          </div>
        )}

        {error && (
          <div className="w-full bg-red-50 text-red-700 dark:bg-red-900/30 dark:text-red-400 p-4 rounded-xl text-center">
            {error}
          </div>
        )}

        {media && !loading && (
          <div className="w-full bg-white dark:bg-gray-800 rounded-xl shadow-md overflow-hidden p-4">
            <div className="relative w-full aspect-[4/5] bg-black rounded-lg overflow-hidden flex items-center justify-center">
              {media.imageUrl && (
                // eslint-disable-next-line @next/next/no-img-element
                <img src={media.imageUrl} alt="Media Preview" className="absolute inset-0 w-full h-full object-cover opacity-80" />
              )}
            </div>
            
            <button
              onClick={handleDownload}
              className="mt-4 w-full h-12 flex items-center justify-center space-x-2 bg-purple-100 dark:bg-purple-900/30 text-instaPurple font-bold rounded-lg hover:bg-purple-200 dark:hover:bg-purple-900/50 transition-colors"
            >
              <span>Save to Device</span>
            </button>
          </div>
        )}
      </div>

      <div className="mt-auto pt-12 w-full">
        <h3 className="font-bold text-lg mb-2">How to copy a link?</h3>
        <ol className="text-gray-600 dark:text-gray-400 space-y-1 text-sm mb-8">
          <li>1. Open the Instagram app.</li>
          <li>2. Find the Reel, Story, or Post you want to download.</li>
          <li>3. Tap the Share icon (paper airplane).</li>
          <li>4. Tap 'Copy link'.</li>
          <li>5. Return here and paste the link!</li>
        </ol>

        <p className="text-xs text-gray-400 text-center border-t border-gray-200 dark:border-gray-800 pt-6">
          This tool is for downloading your own content or content you have permission to use. Please respect copyright and Instagram's terms of service.
        </p>
      </div>
    </main>
  );
}
