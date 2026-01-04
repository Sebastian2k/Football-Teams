import org.graphstream.graph.*;
import org.graphstream.graph.implementations.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class FootballNetworkGraph {

    public static void main(String[] args) {
        System.setProperty("org.graphstream.ui", "swing");

        try {
            String matchesContent = new String(Files.readAllBytes(Paths.get("players_per_match.json")));
            String namesContent = new String(Files.readAllBytes(Paths.get("player_names.json")));

            JSONObject matchesJson = new JSONObject(matchesContent);
            JSONObject namesJson = new JSONObject(namesContent);

            Map<String, Integer> pairCounts = new HashMap<>();
            Set<Integer> activePlayerIds = new HashSet<>();

            for (String matchId : matchesJson.keySet()) {
                JSONArray playersInMatch = matchesJson.getJSONArray(matchId);
                List<Integer> playerList = new ArrayList<>();
                
                for (int i = 0; i < playersInMatch.length(); i++) {
                    int pid = playersInMatch.getInt(i);
                    playerList.add(pid);
                    activePlayerIds.add(pid);
                }

                for (int i = 0; i < playerList.size(); i++) {
                    for (int j = i + 1; j < playerList.size(); j++) {
                        int p1 = playerList.get(i);
                        int p2 = playerList.get(j);

                        String key = (p1 < p2) ? p1 + "_" + p2 : p2 + "_" + p1;
                        
                        pairCounts.put(key, pairCounts.getOrDefault(key, 0) + 1);
                    }
                }
            }

            Graph graph = new SingleGraph("FootballConnections");

            String styleSheet =
                    "node {" +
                    "   fill-color: black;" +
                    "   size: 10px;" +
                    "   text-alignment: above;" +
                    "   text-size: 14;" +
                    "}" +
                    "edge {" +
                    "   fill-color: #555;" +
                    "   size: 2px;" +
                    "   text-size: 14;" +
                    "   text-color: red;" +
                    "   text-background-mode: plain;" +
                    "   text-background-color: white;" +
                    "}";
            
            graph.setAttribute("ui.stylesheet", styleSheet);
            graph.setAttribute("ui.quality");
            graph.setAttribute("ui.antialias");

            for (Integer pid : activePlayerIds) {
                String pidStr = String.valueOf(pid);
                String playerName = namesJson.optString(pidStr, "Unknown (" + pidStr + ")");
                
                Node node = graph.addNode(pidStr);
                node.setAttribute("ui.label", playerName);
            }

            for (Map.Entry<String, Integer> entry : pairCounts.entrySet()) {
                String[] ids = entry.getKey().split("_");
                String id1 = ids[0];
                String id2 = ids[1];
                int weight = entry.getValue();

                if (weight > 0) {
                    Edge edge = graph.addEdge(entry.getKey(), id1, id2);
                    edge.setAttribute("ui.label", weight);
                }
            }

            graph.display();

        } catch (IOException e) {
            System.err.println("Błąd odczytu plików: " + e.getMessage());
        }
    }
}
