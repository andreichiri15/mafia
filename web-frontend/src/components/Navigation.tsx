import { Link, useLocation } from "react-router-dom";
import { Button } from "./ui/button";
import { Gamepad2, User } from "lucide-react";

export function Navigation() {
  const location = useLocation();
  
  return (
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
          
          <Button 
            asChild 
            variant={location.pathname === "/profile" ? "default" : "ghost"}
          >
            <Link to="/profile">
              <User className="w-4 h-4 mr-2" />
              Profile
            </Link>
          </Button>
        </div>
      </div>
    </nav>
  );
}
