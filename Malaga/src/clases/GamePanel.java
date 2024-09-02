package clases;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class GamePanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final int SQUARE_SIZE = 32;
    private static final int WIDTH_SIZE = 60;
    private static final int GAME_WIDTH = 500;
    private static final int GAME_HEIGHT = 700;
    private static final int SQUARE_Y_POSITION = GAME_HEIGHT - SQUARE_SIZE - 20;
    private static final int PROJECTILE_SIZE = 30;
    private static final int ENEMY_PROJECTILE_SIZE = 30;
    private static final int PROJECTILE_SPEED = 10;
    private static final int ENEMY_PROJECTILE_SPEED = 15;
    private static final int ENEMY_HEALTH = 50;
    private static final int ENEMY_SPEED = 5;
    private static final int MAX_HITS = 3;
    private static final int ENEMY_SHOOT_INTERVAL = 500;
    private static final int BOSS_MOVE_INTERVAL = 100;
    private static final int ENEMY_WIDTH = 150;
    private static final int ENEMY_HEIGHT = 100;
    private static final int HEART_SIZE = 30;
    private static final int HEART_FALL_SPEED = 5;

    private ImageIcon backgroundGif;
    private BufferedImage heartImage;
    private BufferedImage playerImage;
    private BufferedImage playerBulletImage;
    private BufferedImage enemyBulletImage;
    private BufferedImage bossImage;
    private int squareX;
    private Timer gameTimer;
    private Timer shootTimer;
    private Timer enemyShootTimer;
    private Timer bossMoveTimer;
    private int moveDirection;
    private boolean canShoot;
    private boolean isShooting;
    private int enemyHealth;
    private int playerLives = 3;
    private int enemyHitCount = 0;
    private boolean isGameOver = false;
    private boolean hasWon = false;
    private List<Rectangle> projectiles;
    private List<Rectangle> enemyProjectiles;
    private List<Rectangle> hearts;
    private BossEnemy bossEnemy;
    private JFrame parentFrame;

    public GamePanel(JFrame frame) {
        this.parentFrame = frame;
        backgroundGif = new ImageIcon(getClass().getClassLoader().getResource("resources/bg.gif"));
        squareX = (GAME_WIDTH - SQUARE_SIZE) / 2;
        moveDirection = 0;
        canShoot = true;
        isShooting = false;
        projectiles = new ArrayList<>();
        enemyProjectiles = new ArrayList<>();
        hearts = new ArrayList<>();
        enemyHealth = ENEMY_HEALTH;

        // Cargar las imágenes
        try {
            heartImage = ImageIO.read(getClass().getClassLoader().getResource("resources/corazon.png"));
            playerImage = ImageIO.read(getClass().getClassLoader().getResource("resources/player.png"));
            playerBulletImage = ImageIO.read(getClass().getClassLoader().getResource("resources/bullet.png"));
            enemyBulletImage = ImageIO.read(getClass().getClassLoader().getResource("resources/enemy_bullet.png"));
            bossImage = ImageIO.read(getClass().getClassLoader().getResource("resources/finalboss.png"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        setFocusable(true);
        requestFocusInWindow();

        initializeBossEnemy();

        gameTimer = new Timer(10, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!isGameOver && !hasWon) {
                    if (moveDirection != 0) {
                        moveSquare(moveDirection);
                    }
                    if (isShooting) {
                        shootProjectile();
                    }
                    updateProjectiles();
                    updateEnemyProjectiles();
                    if (bossEnemy != null) {
                        moveBossEnemy();
                        checkCollisions();
                    }
                    updateHearts();
                    repaint();
                }
            }
        });
        gameTimer.start();

        shootTimer = new Timer(500, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                canShoot = true;
            }
        });

        enemyShootTimer = new Timer(ENEMY_SHOOT_INTERVAL, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!isGameOver && !hasWon && bossEnemy != null) {
                    shootEnemyProjectile();
                }
            }
        });
        enemyShootTimer.start();

        bossMoveTimer = new Timer(BOSS_MOVE_INTERVAL, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (bossEnemy != null && bossEnemy.health <= ENEMY_HEALTH / 2) {
                    bossMoveTimer.start();
                    bossEnemy.y += bossEnemy.speed;
                    if (bossEnemy.y > GAME_HEIGHT / 2 - bossEnemy.height / 2) {
                        bossEnemy.y = GAME_HEIGHT / 2 - bossEnemy.height / 2;
                        bossMoveTimer.stop();
                    }
                }
            }
        });

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int key = e.getKeyCode();
                if (key == KeyEvent.VK_LEFT) {
                    moveDirection = -5;
                } else if (key == KeyEvent.VK_RIGHT) {
                    moveDirection = 5;
                } else if (key == KeyEvent.VK_SPACE) {
                    isShooting = true;
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                int key = e.getKeyCode();
                if ((key == KeyEvent.VK_LEFT && moveDirection == -5) ||
                        (key == KeyEvent.VK_RIGHT && moveDirection == 5)) {
                    moveDirection = 0;
                } else if (key == KeyEvent.VK_SPACE) {
                    isShooting = false;
                }
            }
        });
    }

    private void initializeBossEnemy() {
        int bossX = (GAME_WIDTH - ENEMY_WIDTH) / 2;
        int bossY = 50;
        bossEnemy = new BossEnemy(bossX, bossY, ENEMY_WIDTH, ENEMY_HEIGHT, ENEMY_SPEED);
    }

    private void moveSquare(int dx) {
        squareX += dx;
        if (squareX < 0) {
            squareX = 0;
        } else if (squareX > GAME_WIDTH - WIDTH_SIZE) {
            squareX = GAME_WIDTH - WIDTH_SIZE;
        }
    }

    private void shootProjectile() {
        if (canShoot) {
            int projectileX = squareX + (SQUARE_SIZE - PROJECTILE_SIZE) / 2;
            int projectileY = SQUARE_Y_POSITION;
            projectiles.add(new Rectangle(projectileX, projectileY, PROJECTILE_SIZE, PROJECTILE_SIZE));
            canShoot = false;
            shootTimer.restart();
        }
    }

    private void updateProjectiles() {
        for (int i = 0; i < projectiles.size(); i++) {
            Rectangle projectile = projectiles.get(i);
            projectile.y -= PROJECTILE_SPEED;
            if (projectile.y + PROJECTILE_SIZE < 0) {
                projectiles.remove(i);
                i--;
            }
        }
    }

    private void shootEnemyProjectile() {
        if (bossEnemy != null) {
            int projectileX = bossEnemy.x + (ENEMY_WIDTH - ENEMY_PROJECTILE_SIZE) / 2;
            int projectileY = bossEnemy.y + ENEMY_HEIGHT;
            enemyProjectiles.add(new Rectangle(projectileX, projectileY, ENEMY_PROJECTILE_SIZE, ENEMY_PROJECTILE_SIZE));
        }
    }

    private void updateEnemyProjectiles() {
        for (int i = 0; i < enemyProjectiles.size(); i++) {
            Rectangle projectile = enemyProjectiles.get(i);
            projectile.y += ENEMY_PROJECTILE_SPEED;
            if (projectile.y > GAME_HEIGHT) {
                enemyProjectiles.remove(i);
                i--;
            }
        }
    }

    private void moveBossEnemy() {
        if (bossEnemy != null) {
            if (bossEnemy.health <= ENEMY_HEALTH / 2 && !bossEnemy.hasMovedToMid) {
                bossMoveTimer.start();
                bossEnemy.hasMovedToMid = true;
            }

            bossEnemy.x += bossEnemy.direction * ENEMY_SPEED;
            if (bossEnemy.x <= 0 || bossEnemy.x + ENEMY_WIDTH >= GAME_WIDTH) {
                bossEnemy.direction *= -1;
            }
        }
    }

    private void updateHearts() {
        for (Rectangle heart : hearts) {
            heart.y += HEART_FALL_SPEED;
        }
    }

    private void checkCollisions() {
        if (bossEnemy != null) {
            for (int i = 0; i < projectiles.size(); i++) {
                Rectangle projectile = projectiles.get(i);
                if (projectile.intersects(bossEnemy)) {
                    projectiles.remove(i);
                    i--;
                    bossEnemy.health--;
                    enemyHitCount++;
                    if (enemyHitCount % 15 == 0) {
                        int heartX = bossEnemy.x + (ENEMY_WIDTH - HEART_SIZE) / 2;
                        int heartY = bossEnemy.y + ENEMY_HEIGHT;
                        hearts.add(new Rectangle(heartX, heartY, HEART_SIZE, HEART_SIZE));
                    }
                    if (bossEnemy.health <= 0) {
                        bossEnemy = null;
                        hasWon = true; // Fin del juego
                        break;
                    }
                }
            }

            for (int i = 0; i < enemyProjectiles.size(); i++) {
                Rectangle projectile = enemyProjectiles.get(i);
                if (projectile.intersects(new Rectangle(squareX, SQUARE_Y_POSITION, WIDTH_SIZE, WIDTH_SIZE))) {
                    enemyProjectiles.remove(i);
                    i--;
                    playerLives--;
                    if (playerLives <= 0) {
                        isGameOver = true; // Fin del juego
                        break;
                    }
                }
            }

            for (int i = 0; i < hearts.size(); i++) {
                Rectangle heart = hearts.get(i);
                if (heart.intersects(new Rectangle(squareX, SQUARE_Y_POSITION, WIDTH_SIZE, WIDTH_SIZE))) {
                    hearts.remove(i);
                    i--;
                    playerLives++;
                }
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (backgroundGif != null) {
            g.drawImage(backgroundGif.getImage(), 0, 0, GAME_WIDTH, GAME_HEIGHT, this);
        }
        g.drawImage(playerImage, squareX, SQUARE_Y_POSITION, SQUARE_SIZE, SQUARE_SIZE, this);

        for (Rectangle projectile : projectiles) {
            g.drawImage(playerBulletImage, projectile.x, projectile.y, PROJECTILE_SIZE, PROJECTILE_SIZE, this);
        }

        if (bossEnemy != null) {
            g.drawImage(bossImage, bossEnemy.x, bossEnemy.y, ENEMY_WIDTH, ENEMY_HEIGHT, this);
        }

        for (Rectangle projectile : enemyProjectiles) {
            g.drawImage(enemyBulletImage, projectile.x, projectile.y, ENEMY_PROJECTILE_SIZE, ENEMY_PROJECTILE_SIZE, this);
        }

        for (Rectangle heart : hearts) {
            g.drawImage(heartImage, heart.x, heart.y, HEART_SIZE, HEART_SIZE, this);
        }

        // Mostrar vidas como corazones en la esquina superior izquierda
        g.setColor(Color.WHITE);
        for (int i = 0; i < playerLives; i++) {
            g.drawImage(heartImage, 10 + i * (HEART_SIZE + 5), 10, HEART_SIZE, HEART_SIZE, this);
        }

        if (isGameOver) {
            g.setColor(Color.RED);
            g.setFont(new Font("Arial", Font.BOLD, 50));
            g.drawString("GAME OVER", GAME_WIDTH / 4, GAME_HEIGHT / 2);
        }

        if (hasWon) {
            g.setColor(Color.GREEN);
            g.setFont(new Font("Arial", Font.BOLD, 50));
            g.drawString("¡HAS GANADO!", GAME_WIDTH / 4, GAME_HEIGHT / 2);
        }
    }

    private class BossEnemy extends Rectangle {
        private static final long serialVersionUID = 1L;
        private int health;
        private int speed;
        private int direction = 1;
        private boolean hasMovedToMid = false;

        public BossEnemy(int x, int y, int width, int height, int speed) {
            super(x, y, width, height);
            this.health = ENEMY_HEALTH;
            this.speed = speed;
        }
    }
}