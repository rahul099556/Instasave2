
import { NextRequest, NextResponse } from "next/server";

function isValidInstagramUrl(value: string) {
  try {
    const url = new URL(value);

    const allowedHosts = ["instagram.com", "www.instagram.com"];

    if (!allowedHosts.includes(url.hostname.toLowerCase())) {
      return false;
    }

    return /^\/(reel|reels|p|tv|stories)\//i.test(url.pathname);
  } catch {
    return false;
  }
}

export async function POST(request: NextRequest) {
  try {
    const body = await request.json();
    const instagramUrl = String(body?.url || "").trim();

    if (!isValidInstagramUrl(instagramUrl)) {
      return NextResponse.json(
        { error: "Please enter a valid public Instagram URL." },
        { status: 400 }
      );
    }

    const resolverUrl = process.env.INSTAGRAM_RESOLVER_URL;
    const apiKey = process.env.INSTAGRAM_RESOLVER_API_KEY;

    if (!resolverUrl) {
      return NextResponse.json(
        {
          error:
            "Instagram resolver is not configured yet. Please configure the resolver service.",
        },
        { status: 503 }
      );
    }

    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 15000);

    try {
      const response = await fetch(resolverUrl, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          ...(apiKey
            ? { Authorization: `Bearer ${apiKey}` }
            : {}),
        },
        body: JSON.stringify({
          url: instagramUrl,
        }),
        signal: controller.signal,
        cache: "no-store",
      });

      if (!response.ok) {
        return NextResponse.json(
          { error: "Unable to resolve this Instagram URL." },
          { status: 502 }
        );
      }

      const data = await response.json();

      if (!data?.mediaUrl) {
        return NextResponse.json(
          { error: "No downloadable media was found." },
          { status: 404 }
        );
      }

      return NextResponse.json({
        success: true,
        data: {
          url: data.mediaUrl,
          imageUrl: data.thumbnailUrl || null,
          videoUrl: data.type === "video" ? data.mediaUrl : null,
          isVideo: data.type === "video",
        },
      });
    } finally {
      clearTimeout(timeout);
    }
  } catch (error) {
    console.error("Instagram resolver error:", error);

    return NextResponse.json(
      { error: "Something went wrong while processing the Instagram URL." },
      { status: 500 }
    );
  }
}
