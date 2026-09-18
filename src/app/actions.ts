'use server'

export async function fetchInstagramMedia(url: string) {
  if (!url) {
    return { error: "Please enter a valid Instagram link." };
  }
  
  try {
    const response = await fetch(url, {
      headers: {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
      },
      next: { revalidate: 0 }
    });
    
    if (!response.ok) {
      return { error: "Could not fetch the URL. Instagram may have blocked the request." };
    }
    
    const html = await response.text();
    
    const videoPattern = /<meta property="og:video" content="(.*?)"/;
    const imagePattern = /<meta property="og:image" content="(.*?)"/;
    
    const videoMatch = html.match(videoPattern);
    const imageMatch = html.match(imagePattern);
    
    const videoUrl = videoMatch ? videoMatch[1].replace(/&amp;/g, '&') : null;
    const imageUrl = imageMatch ? imageMatch[1].replace(/&amp;/g, '&') : null;
    
    if (videoUrl || imageUrl) {
      return {
        success: true,
        data: {
          url,
          imageUrl,
          videoUrl,
          isVideo: !!videoUrl
        }
      };
    } else {
      return { error: "Could not extract media. Instagram may have blocked the request or the profile is private." };
    }
  } catch (err: any) {
    return { error: `Error: ${err.message}` };
  }
}
