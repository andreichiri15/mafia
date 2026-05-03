import { useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { Button } from "./ui/button";
import { Card, CardContent, CardFooter, CardHeader, CardTitle } from "./ui/card";
import { Label } from "./ui/label";
import { Input } from "./ui/input";
import { useAuthStore } from "../store/authStore";

export default function SignInPage() {
    const [isRegister, setIsRegister] = useState(false);
    const [identifier, setIdentifier] = useState("");
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [username, setUsername] = useState("");
    const [error, setError] = useState("");
    const [loading, setLoading] = useState(false);

    const login = useAuthStore((s) => s.login);
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const redirectTo = searchParams.get("redirect");

    async function handleSubmit(e: React.FormEvent) {
        e.preventDefault();
        setError("");
        setLoading(true);

        const url = isRegister ? "/api/auth/register" : "/api/auth/login";
        const body = isRegister
            ? { username, email, password }
            : { identifier, password };

        try {
            const res = await fetch(url, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(body),
            });

            if (!res.ok) {
                const text = await res.text();
                setError(text || "Something went wrong");
                return;
            }

            const data = await res.json();
            login(data.token, { userId: data.userId, username: data.username });
            // Only honor relative paths to avoid open-redirect issues
            const target = redirectTo && redirectTo.startsWith("/") ? redirectTo : "/";
            navigate(target);
        } catch {
            setError("Could not connect to server");
        } finally {
            setLoading(false);
        }
    }

    return (
        <div className="flex justify-center mt-4 padding-4">
            <Card className="w-2/5">
                <CardHeader>
                    <CardTitle>{isRegister ? "Register" : "Sign In"}</CardTitle>
                </CardHeader>
                <CardContent>
                    <form id="auth-form" onSubmit={handleSubmit}>
                        <div className="flex flex-col gap-4">
                            {isRegister ? (
                                <>
                                    <Label htmlFor="username">Username</Label>
                                    <Input
                                        id="username"
                                        type="text"
                                        placeholder="your username"
                                        value={username}
                                        onChange={(e) => setUsername(e.target.value)}
                                        required
                                    />
                                    <Label htmlFor="email">Email</Label>
                                    <Input
                                        id="email"
                                        type="email"
                                        placeholder="email@example.com"
                                        value={email}
                                        onChange={(e) => setEmail(e.target.value)}
                                        required
                                    />
                                </>
                            ) : (
                                <>
                                    <Label htmlFor="identifier">Email or Username</Label>
                                    <Input
                                        id="identifier"
                                        type="text"
                                        placeholder="email@example.com or username"
                                        value={identifier}
                                        onChange={(e) => setIdentifier(e.target.value)}
                                        required
                                    />
                                </>
                            )}
                            <Label htmlFor="password">Password</Label>
                            <Input
                                id="password"
                                type="password"
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                required
                            />
                        </div>
                    </form>
                    {error && (
                        <p className="text-red-500 text-sm mt-2">{error}</p>
                    )}
                </CardContent>
                <CardFooter className="flex flex-col gap-4">
                    <Button
                        type="submit"
                        form="auth-form"
                        className="w-full"
                        disabled={loading}
                    >
                        {loading ? "Loading..." : isRegister ? "Register" : "Sign In"}
                    </Button>
                    <Button
                        variant="link"
                        className="w-full"
                        type="button"
                        onClick={() => {
                            setIsRegister(!isRegister);
                            setError("");
                        }}
                    >
                        {isRegister
                            ? "Already have an account? Sign in"
                            : "Don't have an account? Register"}
                    </Button>
                </CardFooter>
            </Card>
        </div>
    );
}
