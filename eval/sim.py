import csv
import numpy as np
import matplotlib
matplotlib.use("Agg")  # backend headless, ca să ruleze fără display
import matplotlib.pyplot as plt
from collections import defaultdict

rng = np.random.default_rng(42)

# ----------------------- Parametri din EloService -----------------------
DEFAULT_ELO = 1000
K_FACTOR = {"VILLAGER": 16, "MAFIA": 24, "MUTILATOR": 24, "JESTER": 24,
            "SHERIFF": 32, "DOCTOR": 32}
EXPECTED_FALLBACK = {"MAFIA": 0.45, "MUTILATOR": 0.45, "VILLAGER": 0.50,
                     "SHERIFF": 0.50, "DOCTOR": 0.50, "JESTER": 0.10}
MAFIA_SIDE = {"MAFIA", "MUTILATOR"}
VILLAGE_SIDE = {"VILLAGER", "SHERIFF", "DOCTOR"}


def round_weight(r):
    r = min(max(r, 1), 5)
    return 0.5 + 0.5 * (r / 5.0)


# ----------------------- Configurarea partidei --------------------------
N_PLAYERS = 8  # jucători per partidă


def assign_roles():
    roles = ["MAFIA", "MAFIA", "SHERIFF", "DOCTOR"]
    if rng.random() < 0.4:
        roles.append("JESTER")
    if rng.random() < 0.3:
        roles.append("MUTILATOR")
    while len(roles) < N_PLAYERS:
        roles.append("VILLAGER")
    rng.shuffle(roles)
    return roles[:N_PLAYERS]


# ----------------------- Model generativ de partidă --------------------
def skill_to_p(theta):
    """Probabilitatea ca o decizie a jucătorului să fie bună."""
    return 0.2 + 0.6 * theta


def simulate_game(players, thetas, roles):
    """Întoarce (winner, info-per-jucător, max_round) pe baza abilităților."""
    max_round = int(rng.integers(2, 7))
    village_strength = np.mean([thetas[p] for p, r in zip(players, roles) if r in VILLAGE_SIDE])
    mafia_strength = np.mean([thetas[p] for p, r in zip(players, roles) if r in MAFIA_SIDE])

    # P(satul câștigă) crește cu diferența de abilitate
    p_village = 1 / (1 + np.exp(-3.0 * (village_strength - mafia_strength)))
    # Jesterul poate fura victoria, mai des dacă e priceput
    jester_theta = next((thetas[p] for p, r in zip(players, roles) if r == "JESTER"), None)
    if jester_theta is not None and rng.random() < 0.06 + 0.10 * jester_theta:
        winner = "JESTER_WIN"
    else:
        winner = "VILLAGER_WIN" if rng.random() < p_village else "MAFIA_WIN"

    info = {}
    for p, role in zip(players, roles):
        theta = thetas[p]
        # mafioții pricepuți sunt votați afară mai rar; satul priceput îi prinde mai des
        voted_out = (role in MAFIA_SIDE and
                     rng.random() < 0.35 * (1 - theta) * (0.5 + village_strength))
        alive = not voted_out
        info[p] = dict(role=role, alive=alive, voted_out=voted_out, max_round=max_round)
    return winner, info, max_round


def personal_score(role, theta, alive, voted_out, max_round):
    """Reproduce structura din computePersonalScore, condusă de θ."""
    s = 0.0
    p = skill_to_p(theta)
    fw = round_weight(max_round)
    if role in MAFIA_SIDE:
        if alive:
            s += 0.25 * fw
        if voted_out:
            s -= 0.30 * fw
    if role == "SHERIFF":
        for r in range(1, max_round + 1):
            w = round_weight(r)
            s += (0.30 * w) if rng.random() < p else (-0.05 * w)
    elif role == "DOCTOR":
        for r in range(1, max_round + 1):
            if rng.random() < p:
                s += 0.30 * round_weight(r)
    elif role == "VILLAGER":
        for r in range(1, max_round + 1):
            w = round_weight(r)
            s += (0.20 * w) if rng.random() < p else (-0.05 * w)
    elif role == "MAFIA":
        for r in range(1, max_round + 1):
            if rng.random() < 0.4 * p:
                s += 0.20 * round_weight(r)  # a ucis un rol de putere
    return float(np.clip(s, -1.0, 1.0))


# ----------------------- Scor așteptat dinamic --------------------------
class ExpectedScore:
    """Winrate per rol, cu fallback până la >= 20 partide."""

    def __init__(self):
        self.played = defaultdict(int)
        self.won = defaultdict(int)

    def value(self, role):
        if self.played[role] >= 20:
            return self.won[role] / self.played[role]
        return EXPECTED_FALLBACK.get(role, 0.5)

    def record(self, role, won):
        self.played[role] += 1
        if won:
            self.won[role] += 1


def did_win(role, winner):
    if role in MAFIA_SIDE:
        return winner == "MAFIA_WIN"
    if role in VILLAGE_SIDE:
        return winner == "VILLAGER_WIN"
    if role == "JESTER":
        return winner == "JESTER_WIN"
    return False


def compute_delta(role, won, expected, pscore, hybrid=True):
    k = K_FACTOR[role]
    team_delta = k * ((1.0 if won else 0.0) - expected)
    if role == "JESTER" or not hybrid:
        return round(team_delta)
    personal_delta = k * pscore
    return round(0.8 * team_delta + 0.2 * personal_delta)


# ----------------------- Pool de jucători și matchmaking ----------------
N_POOL = 200
GAMES_PER_PLAYER = 80
BAND = 0.10

thetas = {i: float(rng.random()) for i in range(N_POOL)}
elo_h = {i: DEFAULT_ELO for i in range(N_POOL)}  # hibrid
elo_t = {i: DEFAULT_ELO for i in range(N_POOL)}  # team-only
games_played = {i: 0 for i in range(N_POOL)}
exp_h, exp_t = ExpectedScore(), ExpectedScore()

corr_history = {"hybrid": [], "team": [], "g": []}
delta_sum = defaultdict(float)   # echilibrul rolurilor (sistemul hibrid)
delta_count = defaultdict(int)


def form_match():
    """Anchor = jucător cu cele mai puține partide; bandă pe ELO hibrid."""
    anchor = min(range(N_POOL), key=lambda i: games_played[i])
    lo, hi = elo_h[anchor] * (1 - BAND), elo_h[anchor] * (1 + BAND)
    pool = [i for i in range(N_POOL) if i != anchor and lo <= elo_h[i] <= hi]
    if len(pool) < N_PLAYERS - 1:
        pool = [i for i in range(N_POOL) if i != anchor]  # relaxare
    chosen = list(rng.choice(pool, N_PLAYERS - 1, replace=False))
    return [anchor] + chosen


total_games = (N_POOL * GAMES_PER_PLAYER) // N_PLAYERS
for g in range(total_games):
    players = form_match()
    roles = assign_roles()
    winner, info, max_round = simulate_game(players, thetas, roles)
    for p, role in zip(players, roles):
        won = did_win(role, winner)
        ps = personal_score(role, thetas[p], info[p]["alive"],
                            info[p]["voted_out"], max_round)
        dh = compute_delta(role, won, exp_h.value(role), ps, True)
        dt = compute_delta(role, won, exp_t.value(role), ps, False)
        elo_h[p] = max(0, elo_h[p] + dh)
        elo_t[p] = max(0, elo_t[p] + dt)
        exp_h.record(role, won)
        exp_t.record(role, won)
        games_played[p] += 1
        delta_sum[role] += dh
        delta_count[role] += 1
    if g % 200 == 0:
        th = np.array([thetas[i] for i in range(N_POOL)])
        corr_history["g"].append(np.mean(list(games_played.values())))
        corr_history["hybrid"].append(np.corrcoef([elo_h[i] for i in range(N_POOL)], th)[0, 1])
        corr_history["team"].append(np.corrcoef([elo_t[i] for i in range(N_POOL)], th)[0, 1])

# ----------------------- Metrici și grafice -----------------------------
th = np.array([thetas[i] for i in range(N_POOL)])
final_h = np.array([elo_h[i] for i in range(N_POOL)])
final_t = np.array([elo_t[i] for i in range(N_POOL)])


def spearman(a, b):
    ra = np.argsort(np.argsort(a))
    rb = np.argsort(np.argsort(b))
    return np.corrcoef(ra, rb)[0, 1]


print("=== Corelația dintre rating și abilitatea adevărată ===")
print(f"Hibrid     Pearson={np.corrcoef(final_h, th)[0,1]:.3f}  Spearman={spearman(final_h, th):.3f}")
print(f"Team-only  Pearson={np.corrcoef(final_t, th)[0,1]:.3f}  Spearman={spearman(final_t, th):.3f}")

# Varianța ratingului pentru jucători cu abilitate similară (zgomot)
print("\n=== Zgomot: deviația standard a ratingului pe intervale de θ ===")
bins = np.linspace(0, 1, 11)
idx = np.digitize(th, bins) - 1
std_h, std_t = [], []
for b in range(10):
    mask = idx == b
    if mask.sum() >= 3:
        std_h.append(np.std(final_h[mask]))
        std_t.append(np.std(final_t[mask]))
print(f"Hibrid     std medie pe interval = {np.mean(std_h):.1f}")
print(f"Team-only  std medie pe interval = {np.mean(std_t):.1f}")

# Echilibrul rolurilor (delta medie per rol, sistemul hibrid)
print("\n=== Echilibrul rolurilor: delta medie per rol (hibrid) ===")
for role in K_FACTOR:
    if delta_count[role]:
        print(f"{role:10s} {delta_sum[role]/delta_count[role]:+.3f}")

# CSV cu rezultatele per jucător
with open("eval/rezultate.csv", "w", newline="") as f:
    w = csv.writer(f)
    w.writerow(["player", "theta", "elo_hybrid", "elo_team", "games"])
    for i in range(N_POOL):
        w.writerow([i, f"{thetas[i]:.4f}", elo_h[i], elo_t[i], games_played[i]])

# Grafic 1: convergența corelației
plt.figure()
plt.plot(corr_history["g"], corr_history["hybrid"], label="Hibrid (echipă + individual)")
plt.plot(corr_history["g"], corr_history["team"], label="Team-only", linestyle="--")
plt.xlabel("Număr mediu de partide jucate")
plt.ylabel("Corelație rating–abilitate")
plt.legend()
plt.grid(True)
plt.title("Convergența ratingului")
plt.savefig("eval/convergenta.png", dpi=150, bbox_inches="tight")

# Grafic 2: rating final vs. abilitate
plt.figure()
plt.scatter(th, final_h, s=12, alpha=0.6, label="Hibrid")
plt.scatter(th, final_t, s=12, alpha=0.6, label="Team-only", marker="x")
plt.xlabel("Abilitate adevărată θ")
plt.ylabel("Rating final")
plt.legend()
plt.grid(True)
plt.title("Rating final vs. abilitate")
plt.savefig("eval/rating_vs_skill.png", dpi=150, bbox_inches="tight")

print("\nFișiere salvate: eval/convergenta.png, eval/rating_vs_skill.png, eval/rezultate.csv")
