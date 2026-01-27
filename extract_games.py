import kagglehub
import pandas as pd
from kagglehub import KaggleDatasetAdapter
import json

# Clubs
club_ids = {
    131,  # FC Barcelona
    418,  # Real Madrid
    281,  # Man City
    985,  # Man United
    27,   # Bayern Munich
    31,   # Liverpool FC
    11,   # Arsenal FC
    631,  # Chelsea FC
    506,  # Juventus
    5,    # AC Milan
    46,   # Inter Milan
    583,  # Paris Saint-Germain (PSG)
    13,   # Atletico Madrid
    16    # Borussia Dortmund
}

def load_games() -> pd.DataFrame:
    return kagglehub.dataset_load(
        KaggleDatasetAdapter.PANDAS,
        "davidcariboo/player-scores",
        path="games.csv"
    )

def load_appearances() -> pd.DataFrame:
    return kagglehub.dataset_load(
        KaggleDatasetAdapter.PANDAS,
        "davidcariboo/player-scores",
        path="appearances.csv"
    )

def load_players() -> pd.DataFrame:
    return kagglehub.dataset_load(
        KaggleDatasetAdapter.PANDAS,
        "davidcariboo/player-scores",
        path="players.csv"
    )

# --- Load Data ---
print("Loading data...")
df_games = load_games()
df_appearances = load_appearances()
df_players = load_players()

# --- Date Processing ---
df_games['date'] = pd.to_datetime(df_games['date'], errors='coerce')
df_appearances['date'] = pd.to_datetime(df_appearances['date'], errors='coerce')

# --- Filter Games ---
# Only matches between chosen clubs
filtered_games = df_games[
    (df_games['home_club_id'].isin(club_ids)) &
    (df_games['away_club_id'].isin(club_ids))
]

# Date range
since_date = pd.to_datetime("2014-01-01")
till_date = pd.to_datetime("2024-12-31")

filtered_games = filtered_games[
    (filtered_games['date'] >= since_date) &
    (filtered_games['date'] <= till_date)
]

filtered_game_ids = filtered_games['game_id'].unique()

print(f"Found {len(filtered_games)} matches between selected clubs.")

# --- Filter Appearances ---
df_app_filtered = df_appearances[df_appearances['game_id'].isin(filtered_game_ids)]

# --- FILE 1: matches.json ---
print("Generating matches.json...")
matches = {}

for _, game in filtered_games.iterrows():
    game_id = int(game['game_id'])
    home_club_id = int(game['home_club_id'])
    away_club_id = int(game['away_club_id'])

    current_game_apps = df_app_filtered[df_app_filtered['game_id'] == game_id]

    home_players = (
        current_game_apps[current_game_apps['player_club_id'] == home_club_id]['player_id']
        .dropna().astype(int).unique().tolist()
    )

    away_players = (
        current_game_apps[current_game_apps['player_club_id'] == away_club_id]['player_id']
        .dropna().astype(int).unique().tolist()
    )

    matches[game_id] = {
        "date": game['date'].strftime("%Y-%m-%d") if pd.notna(game['date']) else None,
        "home_team": {
            "club_id": home_club_id,
            "club_name": game.get('home_club_name'),
            "players": home_players
        },
        "away_team": {
            "club_id": away_club_id,
            "club_name": game.get('away_club_name'),
            "players": away_players
        }
    }

with open("matches.json", "w", encoding="utf-8") as f:
    json.dump(matches, f, indent=4, ensure_ascii=False)

print("Saved matches.json")

# --- FILE 2: players.json (SIMPLE FORMAT) ---
print("Generating players.json...")
relevant_player_ids = df_app_filtered['player_id'].dropna().astype(int).unique()

filtered_players_df = df_players[df_players['player_id'].isin(relevant_player_ids)].drop_duplicates(subset=['player_id'])

# Simple dictionary: "ID": "Name"
players = {}
for _, row in filtered_players_df.iterrows():
    pid_str = str(int(row['player_id'])) # Convert ID to string for JSON key
    players[pid_str] = row['name']

with open("players.json", "w", encoding="utf-8") as f:
    json.dump(players, f, indent=4, ensure_ascii=False)

print("Saved players.json")