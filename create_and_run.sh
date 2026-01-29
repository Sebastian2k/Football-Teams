#!/bin/bash

python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
python3 extract_games.py
javac -cp "lib/*:." src/FootballNetworkGraph.java
java -cp "lib/*:src" FootballNetworkGraph
