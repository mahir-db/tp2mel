package mines;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Random;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;


public class Board extends JPanel {
    private static final long serialVersionUID = 6195235521361212179L;

    private static final int numImages = 13;
    private static final int cellSize = 15;

    private static final int coverOverForCell = 10;
    private static final int markForCell = 10;
    private static final int EmptyCell = 0;
    private static final int mineCell = 9;
    private static final int coveredMineCell = mineCell + coverOverForCell;
    private static final int markedMineCell = coveredMineCell + markForCell;

    private static final int drawMine = 9;
    private static final int drawCover = 10;
    private final int drawMark = 11;
    private static final int drawWrongMark = 12;

    private int[] field;
    private boolean inGame;
    private int minesLeft;
    private transient Image[] img;
    private int mines = 40;
    private int rows = 16;
    private int cols = 16;
    private int allCells;
    private JLabel statusbar;


    public Board(JLabel statusbar) {

        this.statusbar = statusbar;

        img = new Image[numImages];
        String basePath = "/Users/macbook/Downloads/mineGame copy/images/";

        for (int i = 0; i < numImages; i++) {
            img[i] = new ImageIcon(basePath + i + ".gif").getImage();
        }

        setDoubleBuffered(true);

        addMouseListener(new MinesAdapter());
        newGame();
    }

    public void newGame() {
        Random random = new Random(); // Save and reuse Random
        inGame = true;
        minesLeft = mines;
        allCells = rows * cols;
        field = new int[allCells];

        // Initialize the field
        for (int index = 0; index < allCells; index++) {
            field[index] = coverOverForCell;
        }

        statusbar.setText(Integer.toString(minesLeft));

        int placedMines = 0;
        while (placedMines < mines) {
            int position = random.nextInt(allCells); // Use nextInt() instead of nextDouble

            if (field[position] == coveredMineCell) {
                continue; // Skip if mine is already placed
            }

            field[position] = coveredMineCell;
            placedMines++;

            updateAdjacentCells(position);
        }
    }

    private void updateAdjacentCells(int position) {
        int currentCol = position % cols;

        // Define relative positions for adjacent cells
        int[] directions = {-1 - cols, -cols, 1 - cols, -1, 1, cols - 1, cols, cols + 1};

        for (int direction : directions) {
            int adjacentPosition = position + direction;

            if (isValidCell(adjacentPosition, currentCol, direction)) {
                if (field[adjacentPosition] != coveredMineCell) {
                    field[adjacentPosition]++;
                }
            }
        }
    }

    private boolean isValidCell(int position, int currentCol, int direction) {
        if (position < 0 || position >= allCells) {
            return false; // Out of bounds
        }

        int adjacentCol = position % cols;

        // Prevent wrapping across rows
        if (Math.abs(currentCol - adjacentCol) > 1) {
            return false;
        }

        return true;
    }

    public void findEmptyCells(int index) {
        int currentCol = index % cols;

        // Define relative positions for adjacent cells
        int[] directions = {-1 - cols, -cols, 1 - cols, -1, 1, cols - 1, cols, cols + 1};

        for (int direction : directions) {
            int adjacentIndex = index + direction;

            if (isValidCell(adjacentIndex, currentCol, direction) && field[adjacentIndex] > mineCell) {
                field[adjacentIndex] -= coverOverForCell;
                if (field[adjacentIndex] == EmptyCell) {
                    findEmptyCells(adjacentIndex); // Recursively check adjacent cells
                }
            }
        }
    }

    @Override
    public void paint(Graphics g) {
        int uncover = 0;

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                int index = (i * cols) + j;
                int cell = field[index];

                if (inGame && cell == mineCell) {
                    inGame = false;
                }

                cell = determineCellAppearance(cell);

                if (inGame && cell == drawCover) {
                    uncover++;
                }

                g.drawImage(img[cell], (j * cellSize), (i * cellSize), this);
            }
        }

        updateGameStatus(uncover);
    }

    private int determineCellAppearance(int cell) {
        if (!inGame) {
            if (cell == coveredMineCell) {
                return drawMine;
            } else if (cell == markedMineCell) {
                return drawMark;
            } else if (cell > coveredMineCell) {
                return drawWrongMark;
            } else if (cell > mineCell) {
                return drawCover;
            }
        } else {
            if (cell > coveredMineCell) {
                return drawMark;
            } else if (cell > mineCell) {
                return drawCover;
            }
        }

        return cell;
    }

    private void updateGameStatus(int uncover) {
        if (uncover == 0 && inGame) {
            inGame = false;
            statusbar.setText("Game won");
        } else if (!inGame) {
            statusbar.setText("Game lost");
        }
    }

    class MinesAdapter extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent e) {
            int x = e.getX();
            int y = e.getY();

            int cCol = x / cellSize;
            int cRow = y / cellSize;

            if (!inGame) {
                restartGame();
                return;
            }

            if (x < cols * cellSize && y < rows * cellSize) {
                boolean repaintNeeded = false;

                if (e.getButton() == MouseEvent.BUTTON3) {
                    repaintNeeded = handleRightClick(cRow, cCol);
                } else {
                    repaintNeeded = handleLeftClick(cRow, cCol);
                }

                if (repaintNeeded) {
                    repaint();
                }
            }
        }

        private void restartGame() {
            newGame();
            repaint();
        }

        private boolean handleRightClick(int cRow, int cCol) {
            int cellIndex = (cRow * cols) + cCol;

            if (field[cellIndex] <= mineCell) {
                return false; // No action if the cell is already uncovered or not a valid target.
            }

            if (field[cellIndex] <= coveredMineCell) {
                if (minesLeft > 0) {
                    field[cellIndex] += markForCell;
                    minesLeft--;
                    statusbar.setText(Integer.toString(minesLeft));
                } else {
                    statusbar.setText("No marks left");
                }
            } else {
                field[cellIndex] -= markForCell;
                minesLeft++;
                statusbar.setText(Integer.toString(minesLeft));
            }

            return true;
        }

        private boolean handleLeftClick(int cRow, int cCol) {
            int cellIndex = (cRow * cols) + cCol;

            if (field[cellIndex] > coveredMineCell) {
                return false; // Ignore if the cell is marked.
            }

            if (field[cellIndex] > mineCell && field[cellIndex] < markedMineCell) {
                field[cellIndex] -= coverOverForCell;

                if (field[cellIndex] == mineCell) {
                    inGame = false;
                } else if (field[cellIndex] == EmptyCell) {
                    findEmptyCells(cellIndex);
                }

                return true;
            }

            return false;
        }
    }
}