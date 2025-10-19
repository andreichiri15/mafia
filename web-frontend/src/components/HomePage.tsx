import { Link } from "react-router-dom";
import { Button } from "./ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "./ui/card";
import { Users, Shield, Eye } from "lucide-react";

export function HomePage() {
  return (
    <div className="container mx-auto px-4 py-12">
      <div className="max-w-4xl mx-auto text-center space-y-8">
        <div className="space-y-4">
          <h1>Welcome to Mafia</h1>
          <p className="text-muted-foreground">
            The classic social deduction game. Join friends, uncover the mafia, and survive the night.
          </p>
        </div>
        
        <Button asChild size="lg">
          <Link to="/play">Start Playing</Link>
        </Button>
        
        <div className="grid md:grid-cols-3 gap-6 mt-12">
          <Card>
            <CardHeader>
              <Users className="w-8 h-8 mb-2 text-primary" />
              <CardTitle>Social Deduction</CardTitle>
            </CardHeader>
            <CardContent>
              <CardDescription>
                Work with your team to identify the mafia members before it's too late.
              </CardDescription>
            </CardContent>
          </Card>
          
          <Card>
            <CardHeader>
              <Shield className="w-8 h-8 mb-2 text-primary" />
              <CardTitle>Strategic Gameplay</CardTitle>
            </CardHeader>
            <CardContent>
              <CardDescription>
                Use your role's unique abilities to protect the innocent or eliminate threats.
              </CardDescription>
            </CardContent>
          </Card>
          
          <Card>
            <CardHeader>
              <Eye className="w-8 h-8 mb-2 text-primary" />
              <CardTitle>Mystery & Intrigue</CardTitle>
            </CardHeader>
            <CardContent>
              <CardDescription>
                Every game is different. Read between the lines and trust no one.
              </CardDescription>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}
