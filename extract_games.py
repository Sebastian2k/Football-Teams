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

# Load and filter games where both teams are in club_ids
df_games = load_games()
filtered_game_ids = df_games[
    (df_games['home_club_id'].isin(club_ids)) &
    (df_games['away_club_id'].isin(club_ids))
]['game_id']

# Load appearances and convert date column
df_appearances = load_appearances()
df_appearances['date'] = pd.to_datetime(df_appearances['date'])

# Filter appearances by filtered_game_ids and date range
since_date = pd.to_datetime('2023-01-01')
till_date = pd.to_datetime('2023-12-31')

df_filtered = df_appearances[
    (df_appearances['game_id'].isin(filtered_game_ids)) &
    (df_appearances['date'] >= since_date) &
    (df_appearances['date'] <= till_date)
]

# Group by game_id, list player_ids as a dictionary
players_per_match = df_filtered.groupby('game_id')['player_id'].apply(list).to_dict()

# Save the dictionary to JSON
with open("players_per_match.json", 'w') as json_file:
    json.dump(players_per_match, json_file, indent=4)

print("Players per match successfully saved to players_per_match.json")

# Load player info and filter only relevant players from filtered appearances
df_players = load_players()
unique_player_ids = df_filtered['player_id'].unique()
relevant_players = df_players[df_players['player_id'].isin(unique_player_ids)]

# Create player_id to name map and save
player_name_map = relevant_players.drop_duplicates(subset=['player_id']).set_index('player_id')['name'].to_dict()
with open("player_names.json", 'w') as json_file:
    json.dump(player_name_map, json_file, indent=4, ensure_ascii=False)

print("Player name mapping successfully saved to player_names.json")
