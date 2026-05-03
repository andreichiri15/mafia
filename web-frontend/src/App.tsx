import { useEffect } from "react";
import { BrowserRouter, Routes, Route } from "react-router-dom";
import { Navigation } from "./components/Navigation";
import { HomePage } from "./components/HomePage";
import { PlayPage } from "./components/PlayPage";
import { LobbyPage } from "./components/LobbyPage";
import { GamePage } from "./components/GamePage";
import { ProfilePage } from "./components/ProfilePage";
import { InvitePage } from "./components/InvitePage";
import { InviteListener } from "./components/InviteListener";
import SignInPage from "./components/SignInPage";
import { Toaster } from "./components/ui/sonner";
import { useAuthStore } from "./store/authStore";
import { useFriendStore } from "./store/friendStore";
import { useDmStore } from "./store/dmStore";
import { acquireStomp, releaseStomp } from "./lib/websocket";

export default function App() {
	const initialize = useAuthStore((s) => s.initialize);
	const isLoggedIn = useAuthStore((s) => s.isLoggedIn);
	const user = useAuthStore((s) => s.user);

	useEffect(() => {
		initialize();
	}, [initialize]);

	// While logged in, hold one STOMP connection at app level so friend +
	// DM subscriptions survive across page navigations.
	useEffect(() => {
		if (!isLoggedIn || !user) return;

		let cancelled = false;
		acquireStomp().then(() => {
			if (cancelled) return;
			useFriendStore.getState().start(user.userId);
			useDmStore.getState().start(user.userId);
		});

		return () => {
			cancelled = true;
			useFriendStore.getState().stop();
			useDmStore.getState().stop();
			releaseStomp();
		};
	}, [isLoggedIn, user]);

	return (
		<BrowserRouter>
			<div className="size-full flex flex-col">
				<Navigation />
				{isLoggedIn && <InviteListener />}
				<Toaster position="top-right" richColors closeButton />
				<div className="flex-1 overflow-auto">
					<Routes>
						<Route path="/" element={<HomePage />} />
						<Route path="/play" element={<PlayPage />} />
						<Route path="/lobby/:id" element={<LobbyPage />} />
						<Route path="/game/:id" element={<GamePage />} />
						<Route path="/invite/:token" element={<InvitePage />} />
						<Route path="/profile" element={<ProfilePage />} />
						<Route path="/signin" element={<SignInPage />} />
					</Routes>
				</div>
			</div>
		</BrowserRouter>
	);
}
