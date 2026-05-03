import { useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { Button } from "./ui/button";
import { Gamepad2, User, LogOut, Users } from "lucide-react";
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from "./ui/dropdown-menu";
import { useAuthStore } from "../store/authStore";
import { useFriendStore } from "../store/friendStore";
import { FriendsSidebar } from "./friends/FriendsSidebar";

export function Navigation() {
    const location = useLocation();
    const navigate = useNavigate();

    const isLoggedIn = useAuthStore((s) => s.isLoggedIn);
    const user = useAuthStore((s) => s.user);
    const logout = useAuthStore((s) => s.logout);
    const requestCount = useFriendStore((s) => s.requests.length);

    const [friendsOpen, setFriendsOpen] = useState(false);

    const handleLogout = () => {
        logout();
        navigate("/");
    };

    return (
        <>
            <nav className="border-b border-border bg-card">
                <div className="container mx-auto px-4 h-16 flex items-center justify-between">
                    <Link to="/" className="flex items-center gap-2">
                        <Gamepad2 className="w-6 h-6" />
                        <span className="font-medium">Mafia Game</span>
                    </Link>

                    <div className="flex items-center gap-2">
                        <Button
                            asChild
                            variant={location.pathname === "/play" ? "default" : "ghost"}
                        >
                            <Link to="/play">Play</Link>
                        </Button>

                        {isLoggedIn && (
                            <Button
                                variant="ghost"
                                size="icon"
                                className="relative"
                                onClick={() => setFriendsOpen(true)}
                                title="Friends"
                            >
                                <Users className="w-5 h-5" />
                                {requestCount > 0 && (
                                    <span className="absolute -top-1 -right-1 bg-red-500 text-white text-[10px] rounded-full w-4 h-4 flex items-center justify-center font-medium">
                                        {requestCount}
                                    </span>
                                )}
                            </Button>
                        )}

                        {isLoggedIn ? (
                            <DropdownMenu>
                                <DropdownMenuTrigger asChild>
                                    <Button
                                        variant={location.pathname === "/profile" ? "default" : "ghost"}
                                    >
                                        <User className="w-4 h-4 mr-2" />
                                        {user?.username ?? "Profile"}
                                    </Button>
                                </DropdownMenuTrigger>
                                <DropdownMenuContent align="end" className="w-44">
                                    <DropdownMenuItem onClick={() => navigate("/profile")}>
                                        <User className="w-4 h-4 mr-2" />
                                        Profile
                                    </DropdownMenuItem>
                                    <DropdownMenuSeparator />
                                    <DropdownMenuItem onClick={handleLogout} className="text-red-500 focus:text-red-500">
                                        <LogOut className="w-4 h-4 mr-2" />
                                        Log out
                                    </DropdownMenuItem>
                                </DropdownMenuContent>
                            </DropdownMenu>
                        ) : (
                            <Button
                                asChild
                                variant={location.pathname === "/signin" ? "default" : "ghost"}
                            >
                                <Link to="/signin">
                                    <User className="w-4 h-4 mr-2" />
                                    Sign In
                                </Link>
                            </Button>
                        )}
                    </div>
                </div>
            </nav>

            {isLoggedIn && (
                <FriendsSidebar open={friendsOpen} onOpenChange={setFriendsOpen} />
            )}
        </>
    );
}
