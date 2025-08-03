package me.seakeer.learning.javase.other.gui;

import java.util.Random;

/**
 * SudokuHelper;
 * 工具类：生成数独谜面、解决数独、以及谜底是否正确
 *
 * @author Seakeer;
 * @date 2024/9/14;
 */
public class SudokuHelper {

    private static final int SIZE = 9;

    private static int[][] puzzle;

    /**
     * 生成数独谜面
     */
    public static int[][] genSudokuPuzzle() {
        puzzle = new int[SIZE][SIZE];
        // 填充对角线的 3x3 子网格
        fillDiagonal();
        // 从左上角的 3x3 子网格开始填充
        fillRemaining(0, 3);
        //参数是要掩盖的数字个数
        return generatePuzzleWithHiddenNumbers(60);
    }

    /**
     * 解决数独谜面, 返回谜底
     *
     * @param puzzle
     * @return
     */
    public static int[][] solveSolution(int[][] puzzle) {
        int[][] solution = deepCopyArrData(puzzle);
        solveSudoku(solution);
        return solution;
    }

    /**
     * 判断数独谜面是否可解
     *
     * @param puzzle
     * @return
     */
    public static boolean isSolvable(int[][] puzzle) {
        int[][] solution = deepCopyArrData(puzzle);
        return solveSudoku(solution);
    }

    /**
     * 判断谜底是否正确
     *
     * @param solution
     * @return
     */
    public static boolean isRight(int[][] solution) {
        // 检查每行
        for (int i = 0; i < 9; i++) {
            if (!isValidRow(solution[i])) {
                return false;
            }
        }

        // 检查每列
        for (int j = 0; j < 9; j++) {
            int[] column = getColumn(solution, j);
            if (!isValidRow(column)) {
                return false;
            }
        }

        // 检查每个 3x3 子网格
        for (int rowBlock = 0; rowBlock < 3; rowBlock++) {
            for (int colBlock = 0; colBlock < 3; colBlock++) {
                int[] subGrid = getSubGrid(solution, rowBlock * 3, colBlock * 3);
                if (!isValidRow(subGrid)) {
                    return false;
                }
            }
        }

        return true;
    }


    /***************************************生成谜面相关********************************************/
    private static void fillDiagonal() {
        Random random = new Random();
        for (int i = 0; i < SIZE; i += 3) {
            fillSubGrid(i, i, random);
        }
    }

    private static void fillSubGrid(int row, int col, Random random) {
        int[] nums = {1, 2, 3, 4, 5, 6, 7, 8, 9};
        shuffleArray(nums, random);
        int index = 0;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                puzzle[row + i][col + j] = nums[index++];
            }
        }
    }

    private static void shuffleArray(int[] array, Random random) {
        for (int i = array.length - 1; i > 0; i--) {
            int index = random.nextInt(i + 1);
            int temp = array[index];
            array[index] = array[i];
            array[i] = temp;
        }
    }

    private static boolean isValid(int row, int col, int num) {
        for (int i = 0; i < SIZE; i++) {
            if (puzzle[row][i] == num || puzzle[i][col] == num) {
                return false;
            }
        }
        int subGridStartRow = row - row % 3;
        int subGridStartCol = col - col % 3;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (puzzle[subGridStartRow + i][subGridStartCol + j] == num) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean fillRemaining(int row, int col) {
        if (col == SIZE) {
            col = 0;
            row++;
            if (row == SIZE) {
                return true;
            }
        }
        if (puzzle[row][col] != 0) {
            return fillRemaining(row, col + 1);
        }
        Random random = new Random();
        int[] nums = {1, 2, 3, 4, 5, 6, 7, 8, 9};
        shuffleArray(nums, random);
        for (int i = 0; i < SIZE; i++) {
            if (isValid(row, col, nums[i])) {
                puzzle[row][col] = nums[i];
                if (fillRemaining(row, col + 1)) {
                    return true;
                }
                puzzle[row][col] = 0;
            }
        }
        return false;
    }

    private static int[][] generatePuzzleWithHiddenNumbers(int numToHide) {
        int[][] puzzleWithHiddenNumbers = deepCopyArrData(puzzle);
        Random random = new Random();
        while (numToHide > 0) {
            int row = random.nextInt(SIZE);
            int col = random.nextInt(SIZE);
            if (puzzleWithHiddenNumbers[row][col] != 0) {
                puzzleWithHiddenNumbers[row][col] = 0;
                numToHide--;
            }

        }
        return puzzleWithHiddenNumbers;
    }

    public static int[][] deepCopyArrData(int[][] original) {
        if (original == null) {
            return null;
        }

        int[][] copy = new int[original.length][];
        for (int i = 0; i < original.length; i++) {
            if (original[i] != null) {
                copy[i] = new int[original[i].length];
                System.arraycopy(original[i], 0, copy[i], 0, original[i].length);
            } else {
                copy[i] = null;
            }
        }
        return copy;
    }

    /**************************************解数独相关*********************************************/

    private static boolean solveSudoku(int[][] board) {
        int row = -1;
        int col = -1;
        boolean isEmpty = true;
        // 找到未填充的位置
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                if (board[i][j] == 0) {
                    row = i;
                    col = j;
                    isEmpty = false;
                    break;
                }
            }
            if (!isEmpty) {
                break;
            }
        }
        // 如果没有未填充的位置，数独已解决
        if (isEmpty) {
            return true;
        }
        // 尝试填充数字
        for (int num = 1; num <= 9; num++) {
            if (isSafe(board, row, col, num)) {
                board[row][col] = num;
                if (solveSudoku(board)) {
                    return true;
                }
                board[row][col] = 0; // 回溯
            }
        }
        return false;
    }

    private static boolean isSafe(int[][] board, int row, int col, int num) {
        // 检查行
        for (int i = 0; i < 9; i++) {
            if (board[row][i] == num) {
                return false;
            }
        }

        // 检查列
        for (int i = 0; i < 9; i++) {
            if (board[i][col] == num) {
                return false;
            }
        }

        // 检查小九宫格
        int startRow = row - row % 3;
        int startCol = col - col % 3;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (board[i + startRow][j + startCol] == num) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isValidRow(int[] row) {
        boolean[] seen = new boolean[10];
        for (int num : row) {
            if (num < 1 || num > 9 || seen[num]) {
                return false;
            }
            seen[num] = true;
        }
        return true;
    }

    private static int[] getColumn(int[][] solution, int columnIndex) {
        int[] column = new int[9];
        for (int i = 0; i < 9; i++) {
            column[i] = solution[i][columnIndex];
        }
        return column;
    }

    private static int[] getSubGrid(int[][] solution, int startRow, int startCol) {
        int[] subGrid = new int[9];
        int index = 0;
        for (int i = startRow; i < startRow + 3; i++) {
            for (int j = startCol; j < startCol + 3; j++) {
                subGrid[index++] = solution[i][j];
            }
        }
        return subGrid;
    }
}