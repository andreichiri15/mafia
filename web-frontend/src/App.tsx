import { BrowserRouter, Routes, Route } from "react-router-dom";
import { Navigation } from "./components/Navigation";
import { HomePage } from "./components/HomePage";
import { PlayPage } from "./components/PlayPage";
import { LobbyPage } from "./components/LobbyPage";
import { ProfilePage } from "./components/ProfilePage";

export default function App() {
  return (
    <BrowserRouter>
      <div className="size-full flex flex-col">
        <Navigation />
        <div className="flex-1 overflow-auto">
          <Routes>
            <Route path="/" element={<HomePage />} />
            <Route path="/play" element={<PlayPage />} />
            <Route path="/lobby" element={<LobbyPage />} />
            <Route path="/profile" element={<ProfilePage />} />
          </Routes>
        </div>
      </div>
    </BrowserRouter>
  );
}
