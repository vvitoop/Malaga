package clases;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.Timer;

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
    private int playerHits = 0;
    private boolean isGameOver = false;
    private List<Rectangle> projectiles;
    private List<Rectangle> enemyProjectiles;
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
        enemyHealth = ENEMY_HEALTH;

        // Cargar las imágenes del corazón, del jugador, bala del jugador, bala del enemigo y del jefe final
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
                if (moveDirection != 0) {
                    moveSquare(moveDirection);
                }
                if (isShooting) {
                    shootProjectile();
                }
                updateProjectiles();
                updateEnemyProjectiles();
                moveBossEnemy();
                checkCollisions();
                repaint();
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
                shootEnemyProjectile();
            }
        });
        enemyShootTimer.start();

        bossMoveTimer = new Timer(BOSS_MOVE_INTERVAL, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (bossEnemy != null && bossEnemy.health <= ENEMY_HEALTH / 2) {
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
        // Limitar el movimiento a los bordes izquierdo y derecho
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
            // Limitar el movimiento del jefe a los bordes izquierdo y derecho
            if (bossEnemy.x <= 0 || bossEnemy.x + ENEMY_WIDTH >= GAME_WIDTH) {
                bossEnemy.direction *= -1;
            }
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
                    if (bossEnemy.health <= 0) {
                        bossEnemy = null;
                        enemyShootTimer.stop();
                        JOptionPane.showMessageDialog(this, "Felicidades! Has ganado", "Victoria", JOptionPane.INFORMATION_MESSAGE);
                        parentFrame.dispose(); // Cerrar el JFrame
                    }
                }
            }
        }

        for (int i = 0; i < enemyProjectiles.size(); i++) {
            Rectangle projectile = enemyProjectiles.get(i);
            if (projectile.intersects(new Rectangle(squareX, SQUARE_Y_POSITION, WIDTH_SIZE, SQUARE_SIZE))) {
                enemyProjectiles.remove(i);
                i--;
                playerHits++;
                if (playerHits >= MAX_HITS) {
                    gameOver();
                }
            }
        }
    }

    private void gameOver() {
        JOptionPane.showMessageDialog(this, "¡Has perdido!", "Fin del juego", JOptionPane.INFORMATION_MESSAGE);
        parentFrame.dispose(); // Cerrar el JFrame
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(backgroundGif.getImage(), 0, 0, getWidth(), getHeight(), null);
        g.setColor(Color.GREEN);
        g.drawImage(playerImage, squareX, SQUARE_Y_POSITION, WIDTH_SIZE, SQUARE_SIZE, null);

        if (bossEnemy != null) {
            g.drawImage(bossImage, bossEnemy.x, bossEnemy.y, bossEnemy.width, bossEnemy.height, null);
        }

        g.setColor(Color.WHITE);
        for (Rectangle projectile : projectiles) {
            g.drawImage(playerBulletImage, projectile.x, projectile.y, projectile.width, projectile.height, null);
        }

        for (Rectangle projectile : enemyProjectiles) {
            g.drawImage(enemyBulletImage, projectile.x, projectile.y, projectile.width, projectile.height, null);
        }

        // Dibujar las vidas restantes como corazones
        for (int i = 0; i < MAX_HITS - playerHits; i++) {
            int heartX = GAME_WIDTH - (i + 1) * 40;
            int heartY = 10;
            g.drawImage(heartImage, heartX, heartY, 30, 30, null);
        }
    }

    // Clase interna para representar al BossEnemy
    private class BossEnemy extends Rectangle {
        private static final long serialVersionUID = 1L;
        int health;
        int speed;
        int direction;
        boolean hasMovedToMid;

        public BossEnemy(int x, int y, int width, int height, int speed) {
            super(x, y, width, height);
            this.health = ENEMY_HEALTH;
            this.speed = speed;
            this.direction = 1;
            this.hasMovedToMid = false;
        }
    }
}
