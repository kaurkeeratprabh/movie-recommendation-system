// =====================================================
// Movie Recommendation System
// Author: Keerat Kaur
// AIML Student Project
// =====================================================

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.stream.*;

public class MovieRecommender {

    // Data record
    static class Movie {
        String title, rating, genre, mood, language, description;
        int year;
        double imdb;

        Movie(String[] p) {
            title       = p[0].trim();
            year        = Integer.parseInt(p[1].trim());
            rating      = p[2].trim();
            genre       = p[3].trim().toLowerCase();
            mood        = p[4].trim().toLowerCase();
            language    = p[5].trim();
            imdb        = Double.parseDouble(p[6].trim());
            description = p[7].trim();
        }
    }

    // Load CSV
    static List<Movie> loadMovies(String path) {
        List<Movie> list = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            br.readLine(); // skip header
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split(",", 8);
                if (p.length == 8) list.add(new Movie(p));
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Cannot load " + path);
        }
        return list;
    }

    // Recommend
    static List<Movie> recommend(List<Movie> all, int age, String genre,
                                 String mood, String lang, int limit) {
        String[] allowed = age < 13 ? new String[]{"G","U"}
                : age < 18 ? new String[]{"G","PG","U","UA","U/A","PG-13"}
                  :            new String[]{"G","PG","U","UA","U/A","PG-13","R","A"};

        Set<String> ok = new HashSet<>(Arrays.asList(allowed));

        return all.stream()
                .filter(m -> ok.contains(m.rating))
                .filter(m -> m.genre.equalsIgnoreCase(genre) || m.mood.equalsIgnoreCase(mood))
                .sorted(Comparator.comparingDouble((Movie m) -> {
                    double s = 0;
                    if (m.genre.equalsIgnoreCase(genre))   s += 4;
                    if (m.mood.equalsIgnoreCase(mood))     s += 3;
                    if (lang.equals("All") || m.language.equalsIgnoreCase(lang)) s += 1;
                    s += m.imdb / 10.0;
                    return -s;
                }))
                .limit(limit)
                .collect(Collectors.toList());
    }

    // Main / GUI
    public static void main(String[] args) {
        List<Movie> movies = loadMovies("data/movies.csv");

        JFrame f = new JFrame("Movie Recommender");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setSize(900, 600);
        f.setLocationRelativeTo(null);

        // Left form
        JPanel form = new JPanel(new GridLayout(0, 1, 4, 4));
        form.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        form.setPreferredSize(new Dimension(220, 0));

        JTextField ageField = new JTextField();
        String[] moods  = {"Happy","Sad","Excited","Relaxed","Motivated","Scared"};
        String[] genres = {"Action","Comedy","Drama","Horror","Sci-fi","Romance"};
        String[] langs  = {"All","English","Hindi"};
        JComboBox<String> moodBox  = new JComboBox<>(moods);
        JComboBox<String> genreBox = new JComboBox<>(genres);
        JComboBox<String> langBox  = new JComboBox<>(langs);
        JSpinner limitSpinner = new JSpinner(new SpinnerNumberModel(5, 1, 20, 1));
        JButton btn = new JButton("Find Movies");

        form.add(new JLabel("Age:"));       form.add(ageField);
        form.add(new JLabel("Mood:"));      form.add(moodBox);
        form.add(new JLabel("Genre:"));     form.add(genreBox);
        form.add(new JLabel("Language:")); form.add(langBox);
        form.add(new JLabel("Results:"));  form.add(limitSpinner);
        form.add(new JLabel());             form.add(btn);

        // Right results
        String[] cols = {"Title","Year","Rating","Genre","Mood","Lang","IMDb"};
        javax.swing.table.DefaultTableModel model =
                new javax.swing.table.DefaultTableModel(cols, 0) {
                    public boolean isCellEditable(int r, int c) { return false; }
                };
        JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JTextArea desc = new JTextArea(4, 0);
        desc.setEditable(false);
        desc.setLineWrap(true);
        desc.setWrapStyleWord(true);
        desc.setText("Select a movie to see its description.");

        JPanel right = new JPanel(new BorderLayout(5, 5));
        right.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 10));
        right.add(new JScrollPane(table), BorderLayout.CENTER);
        right.add(new JScrollPane(desc),  BorderLayout.SOUTH);

        // ── Button action ─────────────────────────────────────────────────────
        btn.addActionListener(e -> {
            int age;
            try {
                age = Integer.parseInt(ageField.getText().trim());
                if (age <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(f, "Enter a valid age.");
                return;
            }
            String genre = ((String) genreBox.getSelectedItem()).toLowerCase();
            String mood  = ((String) moodBox .getSelectedItem()).toLowerCase();
            String lang  = (String) langBox.getSelectedItem();
            int    limit = (int) limitSpinner.getValue();

            List<Movie> results = recommend(movies, age, genre, mood, lang, limit);
            model.setRowCount(0);
            for (Movie m : results)
                model.addRow(new Object[]{m.title, m.year, m.rating,
                        m.genre, m.mood, m.language,
                        String.format("%.1f", m.imdb)});

            if (results.isEmpty()) desc.setText("No matches found. Try different filters.");
            else desc.setText("Click a row to see the description.");
        });

        // Row click → description
        table.getSelectionModel().addListSelectionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) return;
            String title = (String) model.getValueAt(row, 0);
            movies.stream()
                    .filter(m -> m.title.equals(title))
                    .findFirst()
                    .ifPresent(m -> desc.setText(m.title + " (" + m.year + ") | " +
                            m.rating + " | IMDb: " + m.imdb + "\n\n" + m.description));
        });

        // Layout
        f.setLayout(new BorderLayout());
        f.add(form,  BorderLayout.WEST);
        f.add(right, BorderLayout.CENTER);
        f.setVisible(true);
    }
}
