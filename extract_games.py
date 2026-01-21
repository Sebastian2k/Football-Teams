import kagglehub
import pandas as pd
from kagglehub import KaggleDatasetAdapter
import json

club_ids = {131, 418, 281, 985}  # Barcelona, Real Madrid, Man City, Man United

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

# Load data
df_games = load_games()
df_appearances = load_appearances()
df_players = load_players()

# Date parsing
df_games['date'] = pd.to_datetime(df_games['date'], errors='coerce')
df_appearances['date'] = pd.to_datetime(df_appearances['date'], errors='coerce')

# Filter games (only matches between chosen clubs)
filtered_games = df_games[
    (df_games['home_club_id'].isin(club_ids)) &
    (df_games['away_club_id'].isin(club_ids))
]

# Filter by date
since_date = pd.to_datetime("2014-01-01")
till_date = pd.to_datetime("2024-12-31")

filtered_games = filtered_games[
    (filtered_games['date'] >= since_date) &
    (filtered_games['date'] <= till_date)
]

filtered_game_ids = filtered_games['game_id'].unique()

# Filter appearances to those games
df_app_filtered = df_appearances[df_appearances['game_id'].isin(filtered_game_ids)]

# FILE 1: matches.json
matches = {}

for _, game in filtered_games.iterrows():
    game_id = int(game['game_id'])

    players_in_match = (
        df_app_filtered[df_app_filtered['game_id'] == game_id]['player_id']
        .dropna()
        .astype(int)
        .unique()
        .tolist()
    )

    matches[game_id] = {
        "date": game['date'].strftime("%Y-%m-%d") if pd.notna(game['date']) else None,
        "teams": [
            {
                "club_id": int(game['home_club_id']),
                "club_name": game.get('home_club_name')
            },
            {
                "club_id": int(game['away_club_id']),
                "club_name": game.get('away_club_name')
            }
        ],
        "players": players_in_match
    }

with open("matches.json", "w", encoding="utf-8") as f:
    json.dump(matches, f, indent=4, ensure_ascii=False)

print("Saved matches.json")

# FILE 2: players.json
relevant_player_ids = df_app_filtered['player_id'].dropna().astype(int).unique()

players = (
    df_players[df_players['player_id'].isin(relevant_player_ids)]
    .drop_duplicates(subset=['player_id'])
    .set_index('player_id')['name']
    .to_dict()
)

with open("players.json", "w", encoding="utf-8") as f:
    json.dump(players, f, indent=4, ensure_ascii=False)

print("Saved players.json")
