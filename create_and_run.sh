#!/bin/bash

python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
python3 extract_games.py
echo "Compiling FootballNetworkGraph.java..."
javac -cp "lib/*:." src/FootballNetworkGraph.java
echo "Starting app.."
java -cp "lib/*:src" FootballNetworkGraph
