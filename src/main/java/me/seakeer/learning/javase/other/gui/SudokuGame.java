package me.seakeer.learning.javase.other.gui;

/**
 * 9*9 SudokuGame;
 * 使用Java GUI 的编写的数独游戏
 *
 * @author Seakeer;
 * @date 2024/9/15;
 */
public class SudokuGame {

    /**
     * 封装 UI 界面
     */
    private SudokuUi sudokuUi;

    /**
     * 数独谜面
     * 0 表示空白
     */
    private int[][] puzzle;

    /**
     * 数独谜底
     */
    private int[][] solution;

    /**
     * 玩家的答案
     * 0 表示还没填写
     */
    private int[][] playerSolution;

    /**
     * 初始化UI
     */
    private void initUi() {
        this.sudokuUi = new SudokuUi(this);
        this.sudokuUi.setVisible(true);
    }

    /**
     * 生成数据
     */
    public void genData() {
        this.puzzle = SudokuHelper.genSudokuPuzzle();
        this.solution = SudokuHelper.solveSolution(puzzle);
        this.playerSolution = new int[9][9];
    }

    public void showSolution() {
        this.sudokuUi.fillSolution(solution);
    }

    public void savePlayerSolution(int[][] playerSolution) {
        this.playerSolution = playerSolution;
    }

    public int[][] getPlayerSolution() {
        return this.playerSolution;
    }

    public int[][] getPuzzle() {
        return puzzle;
    }

    public void start() {
        initUi();
        genData();
        this.sudokuUi.fillPuzzle(puzzle);
    }

    public void stop() {
        this.sudokuUi.dispose();
    }

    public static void main(String[] args) {
        SudokuGame game = new SudokuGame();
        game.start();
    }

    public void nextRound() {
        genData();
        sudokuUi.fillPuzzle(puzzle);
    }
}