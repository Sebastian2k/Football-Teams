import json
from itertools import combinations
from collections import Counter


def generate_edges():
    # 1. Wczytanie danych wygenerowanych przez extract_games.py
    try:
        with open("players_per_match.json", "r", encoding="utf-8") as f:
            matches = json.load(f)
        with open("player_names.json", "r", encoding="utf-8") as f:
            names = json.load(f)
    except FileNotFoundError:
        print("Błąd: Brak plików JSON. Uruchom najpierw extract_games.py!")
        return

    # 2. Zliczanie par graczy (krawędzi grafu)
    edges_counter = Counter()

    print("Przetwarzanie meczów i tworzenie powiązań między graczami...")
    for players in matches.values():
        # Mapujemy ID na nazwiska i usuwamy spacje (wymóg GraphStream dla ID węzłów)
        player_names = [names.get(str(p), str(p)).replace(" ", "_") for p in players]

        # Tworzymy unikalne pary graczy, którzy wystąpili w tym samym meczu
        for p1, p2 in combinations(sorted(player_names), 2):
            edges_counter[(p1, p2)] += 1

    # 3. Zapis do pliku tekstowego dla Javy
    # Format: Gracz1 Gracz2 Waga
    output_file = "graph_data.txt"
    with open(output_file, "w", encoding="utf-8") as f:
        for (p1, p2), weight in edges_counter.items():
            f.write(f"{p1} {p2} {weight}\n")

    print(f"Sukces! Plik '{output_file}' został utworzony.")
    print(f"Liczba unikalnych krawędzi: {len(edges_counter)}")


if __name__ == "__main__":
    generate_edges()