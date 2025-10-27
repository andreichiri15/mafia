import { Button } from "./ui/button";
import { Card, CardAction, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "./ui/card";

import { Label } from "./ui/label"
import { Input } from "./ui/input"


export default function SignInPage() {
    return (
        <div className="flex justify-center mt-4 padding-4">
            <Card className="w-2/5">
                <CardHeader>
                    <CardTitle>
                        Sign In
                    </CardTitle>
                </CardHeader>
                <CardContent>
                    <form>
                        <div className="flex flex-col gap-4">
                            <Label htmlFor="email">
                                Email
                            </Label>
                            <Input
                                id="email"
                                type="email"
                                placeholder="email@example.com"
                                required
                            />
                            <Label htmlFor="password">
                                Password
                            </Label>
                            <Input
                                id="password"
                                type="password"
                                required
                            />
                        </div>
                    </form>
                </CardContent>
                <CardFooter className="flex flex-col gap-4">
                    <Button type="submit" className="w-full">
                        Sign In
                    </Button>
                    <Button variant="outline" className="w-full">
                        Log in with Google
                    </Button>
                </CardFooter>
            </Card>
        </div>
    )
}
