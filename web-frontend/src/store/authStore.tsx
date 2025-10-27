import {create} from 'zustand'

interface authState {
    isLoggedIn: boolean,
    setIsLoggedIn: (isLoggedIn: boolean) => void; 
}

export const useAuthStore = create<authState>((set) => ({
    isLoggedIn: false,
    setIsLoggedIn: (newLoggedInState: boolean) => set({isLoggedIn: newLoggedInState})
}))