import { create } from 'zustand'

interface User {
    userId: number;
    username: string;
}

interface AuthState {
    isLoggedIn: boolean;
    user: User | null;
    login: (token: string, user: User) => void;
    logout: () => void;
    initialize: () => void;
}

function setCookie(name: string, value: string, days: number) {
    const expires = new Date(Date.now() + days * 864e5).toUTCString();
    document.cookie = `${name}=${encodeURIComponent(value)}; expires=${expires}; path=/; SameSite=Strict`;
}

function getCookie(name: string): string | null {
    const match = document.cookie.match(new RegExp('(^| )' + name + '=([^;]+)'));
    return match ? decodeURIComponent(match[2]) : null;
}

function deleteCookie(name: string) {
    document.cookie = `${name}=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/`;
}

function readUserFromCookie(): User | null {
    const token = getCookie('jwt');
    if (!token) return null;
    try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        const isExpired = payload.exp * 1000 < Date.now();
        if (isExpired) {
            deleteCookie('jwt');
            return null;
        }
        return { userId: Number(payload.sub), username: payload.username };
    } catch {
        deleteCookie('jwt');
        return null;
    }
}

// Resolve auth state synchronously at module load, so the first render of
// route guards already knows whether the user is signed in (no flicker / no
// false redirect on page refresh).
const initialUser = readUserFromCookie();

export const useAuthStore = create<AuthState>((set) => ({
    isLoggedIn: initialUser !== null,
    user: initialUser,

    login: (token: string, user: User) => {
        setCookie('jwt', token, 1);
        set({ isLoggedIn: true, user });
    },

    logout: () => {
        deleteCookie('jwt');
        set({ isLoggedIn: false, user: null });
    },

    initialize: () => {
        // Kept for compatibility / re-sync after manual cookie changes.
        const u = readUserFromCookie();
        set({ isLoggedIn: u !== null, user: u });
    },
}))
