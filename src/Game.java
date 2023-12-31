import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class Game extends JPanel implements ActionListener, KeyListener {

    private enum GameState {
        PLAYING, PAUSED, OVER, COUNTDOWN
    }

    private enum Movement {
        UP, RIGHT, DOWN, LEFT
    }

    private static final double COUNTDOWN = 3.9;
    private static final int CELL_SIZE = 20;

    private static final int STARTING_SNAKE_SIZE = 4;

    private int tickRate;
    private GameState state;
    private Timer ticker;
    private double score;
    private double elapsed;
    private double countdown;
    private boolean updated;

    private int rows, cols;
    private ArrayList<Point> food;
    private ArrayList<Point> snake;
    private Movement movement;

    public Game() {
        setOpaque(true);
        setFocusable(true);

        addKeyListener(this);

        tickRate = Preferences.getTickRate();

        ticker = new Timer(1000 / tickRate, this);

        if (Preferences.isFocusPause()) {
            Router.getFrame().addWindowFocusListener(new WindowAdapter() {
                @Override
                public void windowLostFocus(WindowEvent e) {
                    pause();
                }
            });
        }

        // Game being added to the frame counts as a resize so setup is called
        addComponentListener(new ComponentAdapter() {

            @Override
            public void componentResized(ComponentEvent e) {
                setup();
            }

        });
    }

    public void setup() {
        state = GameState.PAUSED;
        ticker.stop();

        // Reset game info
        updated = false;
        score = 0;
        elapsed = 0;

        // Calculate map size
        cols = getWidth() / CELL_SIZE;
        rows = getHeight() / CELL_SIZE;

        // Reset snake
        snake = new ArrayList<>();
        int startX = cols / 2 + STARTING_SNAKE_SIZE / 2;
        int startY = rows / 2;
        for (int i = 0; i < STARTING_SNAKE_SIZE; i++) {
            snake.add(new Point(startX - i, startY));
        }
        movement = Movement.RIGHT;

        // Regenerate food
        food = new ArrayList<>();
        while (food.size() < 1) {
            Random random = new Random();
            int x = random.nextInt(cols);
            int y = random.nextInt(rows);
            boolean isSnakeBody = false;
            for (Point snakeBody : snake) {
                if (snakeBody.x == x && snakeBody.y == y) {
                    isSnakeBody = true;
                    break;
                }
            }
            if (isSnakeBody) {
                continue;
            }
            food.add(new Point(x, y));
        }

        if (Savegame.getGamesPlayed() == 0) {
            String info = "It seems this is your first game!\nUse the arrow keys to move, press ESC to pause.";
            Utils.dialog(getParent(), info, new ArrayList<String>(
                    Arrays.asList("OK")));
        }

        countdown = COUNTDOWN;
        state = GameState.COUNTDOWN;
        ticker.start();
        requestFocus();
    }

    private void update() {
        // Run countdown if game is starting
        if (state == GameState.COUNTDOWN) {
            countdown -= ticker.getDelay() / 1000.0;

            if (countdown < 1) {
                state = GameState.PLAYING;
            }
        }

        // Finish game if user is in an end condition
        if (state == GameState.OVER) {
            ticker.stop();

            boolean newHighScore = false;
            Savegame.setGamesPlayed(Savegame.getGamesPlayed() + 1);
            if (score > Savegame.getHighScore()) {
                Savegame.setHighScore((int) score);
                newHighScore = true;
            }

            if (elapsed > Savegame.getLongestGame()) {
                Savegame.setLongestGame((int) elapsed);
            }

            String info;
            if (score == (rows * cols) - STARTING_SNAKE_SIZE) {
                info = "GAME OVER, YOU WIN!";
                Savegame.setGamesWon(Savegame.getGamesWon() + 1);
            } else {
                info = "GAME OVER!";
            }

            if (newHighScore) {
                info += "\nNEW HIGH SCORE!";
            }

            ArrayList<String> buttons = new ArrayList<>(Arrays.asList("RESTART", "MENU"));
            int choice = Utils.dialog(getParent(), info, buttons);

            if (choice == 0) {
                setup();
            } else {
                Router.goBack();
            }
        }

        // Do nothing if game is not in play state
        if (state != GameState.PLAYING) {
            return;
        }

        // Increase elapsed time
        elapsed += ticker.getDelay() / 1000.0;

        // Move the snake body forward
        Point previous = snake.get(0);
        Point head;

        boolean wallsLoop = Preferences.isWallsLoop();

        switch (movement) {
            case UP:
                if (wallsLoop && previous.y == 0) {
                    head = new Point(previous.x, rows - 1);
                } else {
                    head = new Point(previous.x, previous.y - 1);
                }
                break;
            case RIGHT:
                if (wallsLoop && previous.x == cols - 1) {
                    head = new Point(0, previous.y);
                } else {
                    head = new Point(previous.x + 1, previous.y);
                }
                break;
            case DOWN:
                if (wallsLoop && previous.y == rows - 1) {
                    head = new Point(previous.x, 0);
                } else {
                    head = new Point(previous.x, previous.y + 1);
                }
                break;
            case LEFT:
                if (wallsLoop && previous.x == 0) {
                    head = new Point(cols - 1, previous.y);
                } else {
                    head = new Point(previous.x - 1, previous.y);
                }
                break;
            default:
                throw new IllegalStateException();
        }
        snake.add(0, head);

        // Check if the snake has eaten food
        boolean ate = false;
        for (Point point : food) {
            if (head.equals(point)) {
                ate = true;
                score += 1;
                // Regenerate food
                food = new ArrayList<>();
                while (food.size() < 1) {
                    Random random = new Random();
                    int x = random.nextInt(cols);
                    int y = random.nextInt(rows);
                    boolean isSnakeBody = false;
                    for (Point snakeBody : snake) {
                        if (snakeBody.x == x && snakeBody.y == y) {
                            isSnakeBody = true;
                            break;
                        }
                    }
                    if (isSnakeBody) {
                        continue;
                    }
                    food.add(new Point(x, y));
                }
            }
        }

        if (!ate) {
            snake.remove(snake.size() - 1);
        }

        // Check if the snake has collided with itself
        for (int i = 1; i < snake.size(); i++) {
            if (head.equals(snake.get(i))) {
                state = GameState.OVER;
            }
        }

        // Check if the snake has collided with map borders
        if (head.x < 0 || head.x >= cols || head.y < 0 || head.y >= rows) {
            state = GameState.OVER;
        }

        updated = true;
    }

    private void pause() {
        if (state != GameState.PLAYING) {
            return;
        }

        state = GameState.PAUSED;

        String info = "GAME PAUSED!";
        ArrayList<String> buttons = new ArrayList<String>(Arrays.asList("CONTINUE", "RESTART", "MENU"));
        int choice = Utils.dialog(getParent(), info, buttons);

        if (choice == 0) {
            // Unpause
            countdown = COUNTDOWN;
            state = GameState.COUNTDOWN;
        } else if (choice == 1) {
            // Restart
            Savegame.setGamesPlayed(Savegame.getGamesPlayed() + 1);
            setup();
        } else {
            // Back to menu
            Router.goBack();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());

        if (state == null) {
            return;
        }

        g.setColor(Theme.getSecondary(Theme.getCurrent()));
        g.setFont(Preferences.BODY);
        g.drawString("Time: " + (int) elapsed + "s", 10, 20);
        g.drawString("Score: " + (int) score, 10, 40);

        if (state == GameState.COUNTDOWN) {
            g.setFont(Preferences.TITLE);
            g.drawString(String.valueOf((int) countdown), getWidth() / 2 - 20, getHeight() / 3);
        }

        // Draw snake
        g.setColor(Color.GREEN);
        for (int i = 0; i < snake.size(); i++) {
            Point point = snake.get(i);

            // Calculate the size of the body segment based on its position
            int size = (int) (CELL_SIZE - i * ((double) CELL_SIZE / (2 * snake.size())));

            // Draw body segment
            g.fillRect(point.x * CELL_SIZE + (CELL_SIZE - size) / 2,
                    point.y * CELL_SIZE + (CELL_SIZE - size) / 2,
                    size, size);
        }

        // Draw food
        g.setColor(Color.RED);
        for (Point blob : food) {
            g.fillOval(blob.x * CELL_SIZE, blob.y * CELL_SIZE, CELL_SIZE, CELL_SIZE);
        }

    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (state != GameState.PLAYING || !updated) {
            return;
        }

        updated = false;
        int key = e.getKeyCode();

        switch (key) {
            case KeyEvent.VK_UP:
                if (movement != Movement.DOWN)
                    movement = Movement.UP;
                break;
            case KeyEvent.VK_RIGHT:
                if (movement != Movement.LEFT)
                    movement = Movement.RIGHT;
                break;
            case KeyEvent.VK_DOWN:
                if (movement != Movement.UP)
                    movement = Movement.DOWN;
                break;
            case KeyEvent.VK_LEFT:
                if (movement != Movement.RIGHT)
                    movement = Movement.LEFT;
                break;
            case KeyEvent.VK_ESCAPE:
                pause();
                break;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        update();
        repaint();
    }
}
