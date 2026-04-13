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

export const useAuthStore = create<AuthState>((set) => ({
    isLoggedIn: false,
    user: null,

    login: (token: string, user: User) => {
        setCookie('jwt', token, 1);
        set({ isLoggedIn: true, user });
    },

    logout: () => {
        deleteCookie('jwt');
        set({ isLoggedIn: false, user: null });
    },

    initialize: () => {
        const token = getCookie('jwt');
        if (token) {
            try {
                const payload = JSON.parse(atob(token.split('.')[1]));
                const isExpired = payload.exp * 1000 < Date.now();
                if (!isExpired) {
                    set({
                        isLoggedIn: true,
                        user: { userId: Number(payload.sub), username: payload.username },
                    });
                } else {
                    deleteCookie('jwt');
                }
            } catch {
                deleteCookie('jwt');
            }
        }
    },
}))
