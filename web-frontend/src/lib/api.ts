function getToken(): string | null {
  const match = document.cookie.match(/(^| )jwt=([^;]+)/);
  return match ? decodeURIComponent(match[2]) : null;
}

async function request<T>(url: string, options: RequestInit = {}): Promise<T> {
  const token = getToken();
  console.log("[API]", options.method || "GET", url);
  console.log("[API] Token:", token ? `${token.substring(0, 20)}...` : "NO TOKEN");
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    ...(options.headers as Record<string, string>),
  };
  if (token) {
    headers["Authorization"] = `Bearer ${token}`;
  }

  const res = await fetch(url, { ...options, headers });

  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || `Request failed with status ${res.status}`);
  }

  const contentType = res.headers.get("content-type");
  if (contentType && contentType.includes("application/json")) {
    return res.json();
  }
  return res.text() as unknown as T;
}

export const api = {
  get<T>(url: string): Promise<T> {
    return request<T>(url, { method: "GET" });
  },
  post<T>(url: string, body?: unknown): Promise<T> {
    return request<T>(url, {
      method: "POST",
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  },
  put<T>(url: string, body?: unknown): Promise<T> {
    return request<T>(url, {
      method: "PUT",
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  },
  delete<T>(url: string): Promise<T> {
    return request<T>(url, { method: "DELETE" });
  },
};
