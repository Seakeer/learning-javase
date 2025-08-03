package me.seakeer.learning.javase.other.gui;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;

/**
 * SudokuUi;
 *
 * @author Seakeer;
 * @date 2024/9/14;
 */
public class SudokuUi extends JFrame {

    private static final String EMPTY_STR = "";

    private final SudokuGame SUDOKU_GAME;

    private final JPanel MAIN_PANEL;

    private final JPanel FOOTER_PANEL;

    private JTextField[][] cells;


    public SudokuUi(SudokuGame sudokuGame) {
        init();
        this.SUDOKU_GAME = sudokuGame;
        this.MAIN_PANEL = mainPanel();
        this.FOOTER_PANEL = footerPanel();
    }


    public void fillPuzzle(int[][] puzzle) {
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                if (puzzle[i][j] < 1) {
                    cells[i][j].setEditable(true);
                    cells[i][j].setText(EMPTY_STR);
                    cells[i][j].setForeground(Color.BLACK);
                    cells[i][j].setBackground(Color.WHITE);
                } else {
                    cells[i][j].setEditable(false);
                    cells[i][j].setBackground(Color.LIGHT_GRAY);
                    cells[i][j].setForeground(Color.BLACK);
                    cells[i][j].setText(Integer.toString(puzzle[i][j]));
                }
            }
        }
    }

    public void fillSolution(int[][] solution) {
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                cells[i][j].setEditable(false);
                cells[i][j].setText(Integer.toString(solution[i][j]));
                cells[i][j].setForeground(cellTextColor(i, j));
            }
        }
    }

    private void fillPlayerSolution(int[][] playerSolution, int[][] puzzle) {
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                if (puzzle[i][j] < 1) {
                    cells[i][j].setEditable(true);
                    cells[i][j].setForeground(Color.BLACK);
                    cells[i][j].setBackground(Color.WHITE);
                    cells[i][j].setText(playerSolution[i][j] != 0 ? Integer.toString(playerSolution[i][j]) : EMPTY_STR);
                } else {
                    cells[i][j].setEditable(false);
                    cells[i][j].setBackground(Color.LIGHT_GRAY);
                    cells[i][j].setForeground(Color.BLACK);
                    cells[i][j].setText(Integer.toString(puzzle[i][j]));
                }
            }
        }
    }

    private JPanel footerPanel() {

        JButton checkSolutionButton = new JButton("检查答案");
        checkSolutionButton.addActionListener(e -> checkSolution());

        JButton showSolutionButton = new JButton("显示答案");
        showSolutionButton.addActionListener(e -> {
            SUDOKU_GAME.savePlayerSolution(getCellsValue());
            SUDOKU_GAME.showSolution();
        });

        JButton hideSolutionButton = new JButton("隐藏答案");
        hideSolutionButton.addActionListener(e -> {
            fillPlayerSolution(SUDOKU_GAME.getPlayerSolution(), SUDOKU_GAME.getPuzzle());
        });

        JButton nextButton = new JButton("换一个");
        nextButton.addActionListener(e -> SUDOKU_GAME.nextRound());

        JButton exitButton = new JButton("退出");
        exitButton.addActionListener(e -> SUDOKU_GAME.stop());

        JPanel footerPanel = new JPanel(new GridLayout(1, 5));
        footerPanel.add(checkSolutionButton);
        footerPanel.add(showSolutionButton);
        footerPanel.add(hideSolutionButton);
        footerPanel.add(nextButton);
        footerPanel.add(exitButton);
        add(footerPanel, BorderLayout.SOUTH);
        return footerPanel;
    }

    private JPanel mainPanel() {
        JPanel mainPanel = new JPanel(new GridLayout(9, 9));
        cells = new JTextField[9][9];
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                cells[i][j] = new JTextField(1);
                cells[i][j].setFont(new Font("serilf", Font.BOLD, 40));
                cells[i][j].setHorizontalAlignment(JTextField.CENTER);

                AbstractDocument document = (AbstractDocument) cells[i][j].getDocument();
                document.setDocumentFilter(new DocumentFilter() {
                    @Override
                    public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                        String currentText = fb.getDocument().getText(0, fb.getDocument().getLength());
                        String newText = currentText.substring(0, offset) + text + currentText.substring(offset + length);
                        if (!text.matches("[1-9]")) {
                            text = "";
                        }
                        if (newText.length() > 1) {
                            super.replace(fb, 0, 1, text, attrs);
                        } else {
                            super.replace(fb, offset, length, text, attrs);
                        }
                    }
                });
                mainPanel.add(cells[i][j]);
            }
        }
        add(mainPanel, BorderLayout.CENTER);
        return mainPanel;
    }

    private void init() {
        JFrame.setDefaultLookAndFeelDecorated(true);
        setTitle("Sudoku Game");
        setSize(800, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
    }

    private int[][] getCellsValue() {
        int[][] cellsValue = new int[9][9];
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                String text = cells[i][j].getText();
                if (null == text || text.isEmpty()) {
                    cellsValue[i][j] = 0;
                } else {
                    cellsValue[i][j] = Integer.parseInt(text);
                }
            }
        }
        return cellsValue;
    }

    private Color cellTextColor(int i, int j) {
        switch (i / 3) {
            case 0:
            case 2:
                switch (j / 3) {
                    case 0:
                    case 2:
                        return Color.BLUE;
                    case 1:
                        return Color.GREEN;
                    default:
                        return Color.BLACK;
                }
            case 1:
                switch (j / 3) {
                    case 0:
                    case 2:
                        return Color.GREEN;
                    case 1:
                        return Color.BLUE;
                    default:
                        return Color.BLACK;
                }
            default:
                return Color.BLACK;
        }
    }


    public void checkSolution() {
        if (SudokuHelper.isRight(getCellsValue())) {
            JLabel message = new JLabel("恭喜你挑战成功！");
            ImageIcon icon = new ImageIcon();
            JOptionPane.showMessageDialog(MAIN_PANEL, message, "挑战成功", 3, icon);
        } else {
            JLabel message = new JLabel("再试试吧！");
            ImageIcon icon = new ImageIcon();
            JOptionPane.showMessageDialog(MAIN_PANEL, message, "挑战失败", 3, icon);
        }
    }


}
