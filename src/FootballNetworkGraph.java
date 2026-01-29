import org.graphstream.graph.*;
import org.graphstream.graph.implementations.*;
import org.graphstream.ui.swing_viewer.SwingViewer;
import org.graphstream.ui.swing_viewer.ViewPanel;
import org.graphstream.ui.view.Viewer;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;

public class FootballNetworkGraph extends JFrame {

	private static final long serialVersionUID = 1L;
    private static final Color BG_COLOR = new Color(43, 43, 43);
    private static final Color PANEL_COLOR = new Color(60, 63, 65);
    private static final Color TEXT_COLOR = new Color(230, 230, 230);
    private static final Color ACCENT_COLOR = new Color(75, 110, 175);
    
    private JSONObject matchesJson;
    private JSONObject playersNamesJson;
    
    private int minYear = Integer.MAX_VALUE;
    private int maxYear = Integer.MIN_VALUE;

    private Graph graph;
    private SwingViewer viewer;
    private ViewPanel viewPanel;

    private JSlider yearSlider;
    private JLabel yearLabel;
    
    private JSlider weightMinSlider;
    private JSlider weightMaxSlider;
    private JLabel weightRangeLabel;
    private JCheckBox showLonelyNodesBox;
    private boolean isUpdatingSliders = false;
    
    private JPanel clubsPanel;
    private JPanel playersPanel;
    
    private int selectedYear;
    private Set<String> selectedClubs = new HashSet<>();
    private Set<Integer> selectedPlayers = new HashSet<>();
    
    private Map<String, JCheckBox> clubCheckBoxMap = new HashMap<>();
    private Map<Integer, JCheckBox> playerCheckBoxMap = new HashMap<>();

    public FootballNetworkGraph() {
        setupLookFeel();

        setTitle("Evolution of Football Teams");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1400, 950);
        setLayout(new BorderLayout());
        getContentPane().setBackground(BG_COLOR);

        loadData();
        
        if (maxYear == Integer.MIN_VALUE) {
             minYear = 2000;
             maxYear = 2024;
        }
        selectedYear = maxYear; 

        System.setProperty("org.graphstream.ui", "swing");
        graph = new SingleGraph("FootballNetwork");
        setupGraphStyle();

        viewer = new SwingViewer(graph, Viewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD);
        viewer.enableAutoLayout();
        viewPanel = (ViewPanel) viewer.addDefaultView(false);
        
        for (MouseListener ml : viewPanel.getMouseListeners()) viewPanel.removeMouseListener(ml);

        add(viewPanel, BorderLayout.CENTER);

        JPanel sidePanel = createSidePanel();
        
        JScrollPane sideScroll = new JScrollPane(sidePanel);
        sideScroll.setBorder(null);
        sideScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sideScroll.getVerticalScrollBar().setUnitIncrement(16);
        add(sideScroll, BorderLayout.WEST);

        populateClubList();    
        updatePlayerList();    
        updateGraph(true); 

        setVisible(true);
    }

    private void setupLookFeel() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
            UIManager.put("Control", BG_COLOR);
            UIManager.put("nimbusBase", new Color(18, 30, 49));
            UIManager.put("nimbusBlueGrey", PANEL_COLOR);
            UIManager.put("text", TEXT_COLOR);
            UIManager.put("Label.foreground", TEXT_COLOR);
            UIManager.put("CheckBox.foreground", TEXT_COLOR);
            UIManager.put("TitledBorder.titleColor", new Color(100, 180, 255));
            UIManager.put("Slider.tickColor", TEXT_COLOR);
        } catch (Exception e) { }
    }

    private void setupGraphStyle() {
        graph.setAttribute("ui.stylesheet",
                "graph { fill-color: #2B2B2B; }" +
                "node { " +
                "   size: 15px; " +
                "   fill-color: #4B6EAF; " +
                "   stroke-mode: plain; " +
                "   stroke-color: #EEE; " +
                "   text-color: #EEE; " +
                "   text-size: 12; " +
                "   text-alignment: above; " +
                "}" +
                "node.highlight { fill-color: #E74C3C; size: 20px; }" +
                "edge { " +
                "   fill-color: #555; " +
                "   size: 1px; " +
                "   text-color: #AAA; " +
                "   text-size: 10; " +
                "}");
    }

    private JPanel createSidePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(PANEL_COLOR);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        
        JPanel yearPanel = createSectionPanel("Year");
        yearLabel = new JLabel(String.valueOf(selectedYear));
        yearLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        yearLabel.setForeground(new Color(100, 180, 255));
        yearLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        yearSlider = new JSlider(minYear, maxYear, selectedYear);
        yearSlider.setBackground(PANEL_COLOR);
        yearSlider.setMajorTickSpacing(1);
        yearSlider.setPaintTicks(true);
        yearSlider.addChangeListener(e -> {
            if (!yearSlider.getValueIsAdjusting()) {
                selectedYear = yearSlider.getValue();
                yearLabel.setText(String.valueOf(selectedYear));
                updatePlayerList(); 
                updateGraph(true); 
            }
        });
        
        yearPanel.add(yearLabel);
        yearPanel.add(Box.createVerticalStrut(5));
        yearPanel.add(yearSlider);
        panel.add(yearPanel);
        panel.add(Box.createVerticalStrut(15));

        JPanel weightPanel = createSectionPanel("Matches played together (same or opposing team)");
        weightRangeLabel = new JLabel("Range");
        weightRangeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        weightRangeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        JLabel minLbl = new JLabel("Min:");
        minLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        weightMinSlider = new JSlider(0, 10, 0);
        weightMinSlider.setBackground(PANEL_COLOR);
        
        JLabel maxLbl = new JLabel("Max:");
        maxLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        weightMaxSlider = new JSlider(0, 10, 10);
        weightMaxSlider.setBackground(PANEL_COLOR);

        weightMinSlider.addChangeListener(e -> {
            if (isUpdatingSliders) return;
            if (weightMinSlider.getValue() > weightMaxSlider.getValue()) {
                weightMaxSlider.setValue(weightMinSlider.getValue());
            }
            updateWeightLabel();
            if (!weightMinSlider.getValueIsAdjusting()) updateGraph(false);
        });

        weightMaxSlider.addChangeListener(e -> {
            if (isUpdatingSliders) return;
            if (weightMaxSlider.getValue() < weightMinSlider.getValue()) {
                weightMinSlider.setValue(weightMaxSlider.getValue());
            }
            updateWeightLabel();
            if (!weightMaxSlider.getValueIsAdjusting()) updateGraph(false);
        });

        showLonelyNodesBox = new JCheckBox("Show players without connections");
        showLonelyNodesBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        showLonelyNodesBox.setBackground(PANEL_COLOR);
        showLonelyNodesBox.setForeground(TEXT_COLOR);
        showLonelyNodesBox.setFocusPainted(false);
        showLonelyNodesBox.setSelected(false);
        showLonelyNodesBox.addActionListener(e -> updateGraph(false));

        weightPanel.add(weightRangeLabel);
        weightPanel.add(Box.createVerticalStrut(5));
        weightPanel.add(minLbl);
        weightPanel.add(weightMinSlider);
        weightPanel.add(maxLbl);
        weightPanel.add(weightMaxSlider);
        
        weightPanel.add(Box.createVerticalStrut(15)); 
        weightPanel.add(showLonelyNodesBox);
        
        panel.add(weightPanel);
        panel.add(Box.createVerticalStrut(15));

        JPanel clubsContainer = createSectionPanel("Teams");
        JButton toggleClubs = new JButton("Select/Deselect all");
        styleButton(toggleClubs);
        toggleClubs.addActionListener(e -> toggleAllClubs());
        
        clubsPanel = new JPanel();
        clubsPanel.setLayout(new BoxLayout(clubsPanel, BoxLayout.Y_AXIS));
        clubsPanel.setBackground(PANEL_COLOR);
        
        JScrollPane clubsScroll = new JScrollPane(clubsPanel);
        clubsScroll.setPreferredSize(new Dimension(300, 200));
        clubsScroll.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        
        clubsContainer.add(toggleClubs);
        clubsContainer.add(Box.createVerticalStrut(5));
        clubsContainer.add(clubsScroll);
        panel.add(clubsContainer);
        panel.add(Box.createVerticalStrut(15));

        JPanel playersContainer = createSectionPanel("Football players");
        JButton togglePlayers = new JButton("Select/Deselect visible");
        styleButton(togglePlayers);
        togglePlayers.addActionListener(e -> toggleAllPlayers());

        playersPanel = new JPanel();
        playersPanel.setLayout(new BoxLayout(playersPanel, BoxLayout.Y_AXIS));
        playersPanel.setBackground(PANEL_COLOR);

        JScrollPane playersScroll = new JScrollPane(playersPanel);
        playersScroll.setPreferredSize(new Dimension(300, 300));
        playersScroll.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        playersContainer.add(togglePlayers);
        playersContainer.add(Box.createVerticalStrut(5));
        playersContainer.add(playersScroll);
        panel.add(playersContainer);

        return panel;
    }

    private void updateWeightLabel() {
        weightRangeLabel.setText("Range: " + weightMinSlider.getValue() + " - " + weightMaxSlider.getValue());
    }


    private void loadData() {
        try {
            matchesJson = new JSONObject(new String(Files.readAllBytes(Paths.get("matches.json"))));
            playersNamesJson = new JSONObject(new String(Files.readAllBytes(Paths.get("players.json"))));

            for (String key : matchesJson.keySet()) {
                String date = matchesJson.getJSONObject(key).getString("date");
                int year = Integer.parseInt(date.substring(0, 4));
                if (year < minYear) minYear = year;
                if (year > maxYear) maxYear = year;
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "JSON files error: " + e.getMessage());
        }
    }

    private void populateClubList() {
        Set<String> allClubs = new TreeSet<>();
        for (String matchId : matchesJson.keySet()) {
            JSONObject match = matchesJson.getJSONObject(matchId);
            allClubs.add(match.getJSONObject("home_team").getString("club_name"));
            allClubs.add(match.getJSONObject("away_team").getString("club_name"));
        }

        clubsPanel.removeAll();
        clubCheckBoxMap.clear();

        for (String clubName : allClubs) {
            JCheckBox cb = new JCheckBox(clubName);
            cb.setBackground(PANEL_COLOR);
            cb.setForeground(TEXT_COLOR);
            cb.setSelected(true);
            selectedClubs.add(clubName);
            
            cb.addItemListener(e -> {
                if (cb.isSelected()) selectedClubs.add(clubName);
                else selectedClubs.remove(clubName);
                updatePlayerList();
                updateGraph(true);
            });
            clubsPanel.add(cb);
            clubCheckBoxMap.put(clubName, cb);
        }
        clubsPanel.revalidate();
        clubsPanel.repaint();
    }

    private void updatePlayerList() {
        Set<Integer> validPlayerIds = new HashSet<>();

        for (String matchId : matchesJson.keySet()) {
            JSONObject match = matchesJson.getJSONObject(matchId);
            
            if (!match.getString("date").startsWith(String.valueOf(selectedYear))) continue;

            processTeamForList(match.getJSONObject("home_team"), validPlayerIds);
            processTeamForList(match.getJSONObject("away_team"), validPlayerIds);
        }

        playersPanel.removeAll();
        playerCheckBoxMap.clear();
        selectedPlayers.clear();

        List<Integer> sortedIds = new ArrayList<>(validPlayerIds);
        sortedIds.sort((id1, id2) -> {
            String name1 = playersNamesJson.optString(String.valueOf(id1), "");
            String name2 = playersNamesJson.optString(String.valueOf(id2), "");
            return name1.compareToIgnoreCase(name2);
        });

        for (Integer pid : sortedIds) {
            String name = playersNamesJson.optString(String.valueOf(pid), "ID: " + pid);
            JCheckBox cb = new JCheckBox(name);
            cb.setBackground(PANEL_COLOR);
            cb.setForeground(TEXT_COLOR);
            cb.setSelected(true);
            selectedPlayers.add(pid);

            cb.addItemListener(e -> {
                if (cb.isSelected()) selectedPlayers.add(pid);
                else selectedPlayers.remove(pid);
                updateGraph(false); 
            });
            playersPanel.add(cb);
            playerCheckBoxMap.put(pid, cb);
        }
        playersPanel.revalidate();
        playersPanel.repaint();
    }

    private void processTeamForList(JSONObject teamObj, Set<Integer> validIds) {
        String clubName = teamObj.getString("club_name");
        if (selectedClubs.contains(clubName)) {
            JSONArray players = teamObj.getJSONArray("players");
            for (int i = 0; i < players.length(); i++) {
                validIds.add(players.getInt(i));
            }
        }
    }

    private void updateGraph(boolean recalculateRange) {
        graph.clear();
        setupGraphStyle();

        if (selectedPlayers.isEmpty()) return;

        Map<String, Integer> pairCounts = new HashMap<>();

        for (String matchId : matchesJson.keySet()) {
            JSONObject match = matchesJson.getJSONObject(matchId);
            if (!match.getString("date").startsWith(String.valueOf(selectedYear))) continue;

            List<Integer> matchPlayers = new ArrayList<>();
            collectValidPlayers(match.getJSONObject("home_team"), matchPlayers);
            collectValidPlayers(match.getJSONObject("away_team"), matchPlayers);

            for (int i = 0; i < matchPlayers.size(); i++) {
                for (int j = i + 1; j < matchPlayers.size(); j++) {
                    int p1 = matchPlayers.get(i);
                    int p2 = matchPlayers.get(j);
                    String key = (p1 < p2) ? p1 + "_" + p2 : p2 + "_" + p1;
                    pairCounts.put(key, pairCounts.getOrDefault(key, 0) + 1);
                }
            }
        }

        if (recalculateRange) {
            int maxWeightFound = 0;
            for (int w : pairCounts.values()) {
                if (w > maxWeightFound) maxWeightFound = w;
            }
            if (maxWeightFound == 0) maxWeightFound = 1;

            isUpdatingSliders = true;
            weightMinSlider.setMinimum(0);
            weightMinSlider.setMaximum(maxWeightFound);
            weightMinSlider.setValue(0);

            weightMaxSlider.setMinimum(0);
            weightMaxSlider.setMaximum(maxWeightFound);
            weightMaxSlider.setValue(maxWeightFound);

            int spacing = Math.max(1, maxWeightFound / 5);
            weightMinSlider.setMajorTickSpacing(spacing);
            weightMaxSlider.setMajorTickSpacing(spacing);
            
            updateWeightLabel();
            isUpdatingSliders = false;
        }

        int filterMin = weightMinSlider.getValue();
        int filterMax = weightMaxSlider.getValue();

        Set<String> addedNodes = new HashSet<>();

        for (Map.Entry<String, Integer> entry : pairCounts.entrySet()) {
            int weight = entry.getValue();

            if (weight < filterMin || weight > filterMax) {
                continue; 
            }

            String[] ids = entry.getKey().split("_");
            String p1 = ids[0];
            String p2 = ids[1];

            if (graph.getNode(p1) == null) addNodeToGraph(p1);
            if (graph.getNode(p2) == null) addNodeToGraph(p2);
            
            addedNodes.add(p1);
            addedNodes.add(p2);

            Edge e = graph.addEdge(entry.getKey(), p1, p2);
            if (e != null) {
                if (weight > 0) {
                    e.setAttribute("ui.label", weight);
                    double thickness = 1 + (Math.min(10, weight) * 0.5);
                    e.setAttribute("ui.style", "size: " + thickness + "px;");
                }
            }
        }
        
        if (showLonelyNodesBox.isSelected()) {
            for (Integer pid : selectedPlayers) {
                String pidStr = String.valueOf(pid);
                if (!addedNodes.contains(pidStr)) {
                    if (graph.getNode(pidStr) == null) {
                        addNodeToGraph(pidStr);
                    }
                }
            }
        }
    }

    private void addNodeToGraph(String pidStr) {
        String name = playersNamesJson.optString(pidStr, pidStr);
        Node n = graph.addNode(pidStr);
        n.setAttribute("ui.label", name);
    }

    private void collectValidPlayers(JSONObject teamObj, List<Integer> targetList) {
        String clubName = teamObj.getString("club_name");
        if (selectedClubs.contains(clubName)) {
            JSONArray players = teamObj.getJSONArray("players");
            for (int i = 0; i < players.length(); i++) {
                int pid = players.getInt(i);
                if (selectedPlayers.contains(pid)) {
                    targetList.add(pid);
                }
            }
        }
    }

    private JPanel createSectionPanel(String title) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(PANEL_COLOR);
        TitledBorder border = BorderFactory.createTitledBorder(title);
        border.setTitleFont(new Font("Segoe UI", Font.BOLD, 14));
        border.setTitleColor(new Color(100, 180, 255));
        p.setBorder(border);
        return p;
    }

    private void styleButton(JButton btn) {
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setBackground(ACCENT_COLOR);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
    }

    private void toggleAllClubs() {
        boolean select = !clubCheckBoxMap.values().stream().allMatch(JCheckBox::isSelected);
        for (JCheckBox cb : clubCheckBoxMap.values()) cb.setSelected(select);
    }

    private void toggleAllPlayers() {
        boolean select = !playerCheckBoxMap.values().stream().allMatch(JCheckBox::isSelected);
        for (JCheckBox cb : playerCheckBoxMap.values()) cb.setSelected(select);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(FootballNetworkGraph::new);
    }
}
