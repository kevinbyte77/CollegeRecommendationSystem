package com.college.gui;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Collections;

import com.college.model.College;
import com.college.model.Course;
import com.college.service.RecommendationService;
import com.college.service.RecommendationHistoryService;
import com.college.service.BookmarkService;
import com.college.service.CutoffDataService;

public class SearchFrame extends JFrame {

    private final RecommendationService recommendationService;
    private final List<College> colleges;
    private final List<Course> courses;

    public SearchFrame() {
        recommendationService = new RecommendationService();
        colleges = recommendationService.getColleges();
        courses = recommendationService.getCourses();

        setTitle("Search Colleges");
        setSize(750, 500);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridLayout(7, 2, 10, 10));
        panel.setBorder(
                BorderFactory.createEmptyBorder(25, 30, 25, 30)
        );

        JLabel heading = new JLabel("Search Engineering Colleges");
        heading.setFont(new Font("Arial", Font.BOLD, 20));

        JTextField percentileField = new JTextField();

        String[] categories = CutoffDataService.getCategoryCodes().toArray(new String[0]);
        JComboBox<String> categoryBox = new JComboBox<>(categories);
        JTextField cityField = new JTextField();

        DefaultComboBoxModel<String> branchModel =
                new DefaultComboBoxModel<>();

        branchModel.addElement("All Branches");

        for (Course course : courses) {
            String courseName = course.getCourseName();
            boolean exists = false;

            for (int i = 0; i < branchModel.getSize(); i++) {
                if (branchModel.getElementAt(i)
                        .equalsIgnoreCase(courseName)) {
                    exists = true;
                    break;
                }
            }

            if (!exists) {
                branchModel.addElement(courseName);
            }
        }

        JComboBox<String> branchBox = new JComboBox<>(branchModel);

        JButton searchButton = new JButton("Search");
        JButton backButton = new JButton("Back to Home");

        panel.add(heading);
        panel.add(new JLabel(""));

        panel.add(new JLabel("JEE Main Percentile:"));
        panel.add(percentileField);

        panel.add(new JLabel("Category:"));
        panel.add(categoryBox);

        panel.add(new JLabel("Preferred City:"));
        panel.add(cityField);

        panel.add(new JLabel("Preferred Branch:"));
        panel.add(branchBox);

        panel.add(searchButton);
        panel.add(backButton);

        add(panel);

        searchButton.addActionListener(e -> {
            String percentile = percentileField.getText().trim();
            String category = (String) categoryBox.getSelectedItem();
            String city = cityField.getText().trim();
            String branch = (String) branchBox.getSelectedItem();

            if (percentile.isEmpty()) {
                JOptionPane.showMessageDialog(
                        this, "Please enter your percentile."
                );
                return;
            }

            try {
                double value = Double.parseDouble(percentile);

                if (value < 0 || value > 100) {
                    JOptionPane.showMessageDialog(
                            this, "Percentile must be between 0 and 100."
                    );
                    return;
                }

                searchButton.setEnabled(false);
                searchButton.setText("Searching...");

                new SwingWorker<List<String>, Void>() {
                    @Override
                    protected List<String> doInBackground() {
                        // runs on a background thread: database query
                        return recommendationService.recommendColleges(
                                value, category, city, branch
                        );
                    }

                    @Override
                    protected void done() {
                        // runs back on the Event Dispatch Thread: update the UI
                        try {
                            showResults(get(), category, city, branch);
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(
                                    SearchFrame.this,
                                    "Search failed: " + ex.getMessage()
                            );
                        } finally {
                            searchButton.setEnabled(true);
                            searchButton.setText("Search");
                        }
                    }
                }.execute();

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(
                        this,
                        "Please enter a valid numeric percentile."
                );
            }
        });

        backButton.addActionListener(e -> dispose());
    }

    private void showResults(List<String> recommendations,
                             String category, String city, String branch) {

                DefaultListModel<String> listModel =
                        new DefaultListModel<>();

                for (String result : recommendations) {
                    String lowerResult = result.toLowerCase();

                    boolean matchesCity =
                            city.isEmpty()
                            || lowerResult.contains(city.toLowerCase());

                    boolean matchesBranch =
                            branch.equals("All Branches")
                            || lowerResult.contains(
                                    branch.toLowerCase()
                            );

                    if (matchesCity && matchesBranch) {
                        listModel.addElement(result);
                    }
                }

                if (listModel.isEmpty()) {
                    JOptionPane.showMessageDialog(
                            this, "No matching colleges found."
                    );
                    return;
                }

                RecommendationHistoryService.saveRecommendations(
                        Collections.list(listModel.elements())
                );

                JList<String> resultList = new JList<>(listModel);
                resultList.setSelectionMode(
                        ListSelectionModel.SINGLE_SELECTION
                );

                JButton detailsButton =
                        new JButton("View College Details");

                JButton bookmarkButton =
                        new JButton("Bookmark College");

                JPanel resultPanel =
                        new JPanel(new BorderLayout(10, 10));

                JPanel buttonPanel = new JPanel();
                buttonPanel.add(detailsButton);
                buttonPanel.add(bookmarkButton);

                resultPanel.add(
                        new JScrollPane(resultList),
                        BorderLayout.CENTER
                );

                resultPanel.add(buttonPanel, BorderLayout.SOUTH);

                detailsButton.addActionListener(event -> {
                    String selectedResult = resultList.getSelectedValue();

                    if (selectedResult == null) {
                        JOptionPane.showMessageDialog(
                                this, "Please select a result first."
                        );
                        return;
                    }

                    for (Course course : courses) {
                        for (College college : colleges) {
                            String prefix =
                                    college.getCollegeName()
                                    + " | " + college.getCity()
                                    + " | " + course.getCourseName() + " |";

                            if (selectedResult.startsWith(prefix)
                                    && course.getCollegeId()
                                            == college.getCollegeId()) {

                                new CollegeDetailsFrame(
                                        college, courses
                                ).setVisible(true);
                                return;
                            }
                        }
                    }

                    JOptionPane.showMessageDialog(
                            this, "Could not find the selected college."
                    );
                });

                bookmarkButton.addActionListener(event -> {
                    String selectedResult = resultList.getSelectedValue();

                    if (selectedResult == null) {
                        JOptionPane.showMessageDialog(
                                this, "Please select a result first."
                        );
                        return;
                    }

                    for (Course course : courses) {
                        for (College college : colleges) {
                            String prefix =
                                    college.getCollegeName()
                                    + " | " + college.getCity()
                                    + " | " + course.getCourseName() + " |";

                            if (selectedResult.startsWith(prefix)
                                    && course.getCollegeId()
                                            == college.getCollegeId()) {

                                if (BookmarkService.isBookmarked(
                                        college.getCollegeId())) {

                                    JOptionPane.showMessageDialog(
                                            this,
                                            "This college is already bookmarked."
                                    );
                                } else {
                                    BookmarkService.bookmarkCollege(college);

                                    JOptionPane.showMessageDialog(
                                            this,
                                            college.getCollegeName()
                                                    + " bookmarked successfully."
                                    );
                                }

                                return;
                            }
                        }
                    }

                    JOptionPane.showMessageDialog(
                            this, "Could not find the selected college."
                    );
                });

                JOptionPane.showMessageDialog(
                        this,
                        resultPanel,
                        "College Recommendations",
                        JOptionPane.PLAIN_MESSAGE
                );
    }
}
