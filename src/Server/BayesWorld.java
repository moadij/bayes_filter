package Server;
import java.awt.Color;
import java.awt.Graphics;
import java.lang.*;
import javax.swing.JComponent;
import javax.swing.JFrame;
import java.io.*;
import java.util.Random;

import java.net.*;




class MyCanvas extends JComponent {
    int winWidth, winHeight;
    double squareWidth, squareHeight;
    Color gris = new Color(170,170,170);
    Color myWhite = new Color(220, 220, 220);
    
    int xPosition, yPosition;

    World world;
    
    public MyCanvas(int w, int h, World wld, int _x, int _y) {
        world = wld;
        winWidth = w;
        winHeight = h;
        updatePosition(_x, _y);
        
        squareWidth = (double)w / world.width;
        squareHeight = (double)h / world.height;
    }
    
    public void updatePosition(int _x, int _y) {
        xPosition = _x;
        yPosition = _y;
        
        repaint();
    }
    
    public void paint(Graphics g) {
        for (int y = 0; y < world.height; y++) {
            for (int x = 0; x < world.width; x++) {
                if (world.grid[x][y] == 1) {
                    g.setColor(Color.black);
                    g.fillRect((int)(x * squareWidth), (int)(y * squareHeight), (int) squareWidth, (int) squareHeight);
                }
                else if (world.grid[x][y] == 0) {
                    g.setColor(myWhite);
                    g.fillRect((int)(x * squareWidth), (int)(y * squareHeight), (int) squareWidth, (int) squareHeight);
                }
                else if (world.grid[x][y] == 2) {
                    g.setColor(Color.red);
                    g.fillRect((int)(x * squareWidth), (int)(y * squareHeight), (int) squareWidth, (int) squareHeight);
                }
                else if (world.grid[x][y] == 3) {
                    g.setColor(Color.green);
                    g.fillRect((int)(x * squareWidth), (int)(y * squareHeight), (int) squareWidth, (int) squareHeight);
                }
            }
            if (y != 0) {
                g.setColor(gris);
                g.drawLine(0, (int)(y * squareHeight), (int)winWidth, (int)(y * squareHeight));
            }
        }
        for (int x = 0; x < world.width; x++) {
                g.setColor(gris);
                g.drawLine((int)(x * squareWidth), 0, (int)(x * squareWidth), (int)winHeight);
        }
        
        g.setColor(Color.blue);
        g.fillOval((int)(xPosition * squareWidth)+1, (int)(yPosition * squareHeight)+1, (int)(squareWidth -1.4), (int)(squareHeight -1.4));
    }
}

public class BayesWorld extends JFrame {
    public static final int NORTH = 0;
    public static final int SOUTH = 1;
    public static final int EAST = 2;
    public static final int WEST = 3;
    public static final int STAY = 4;

    Color backgroundColor = new Color(230,230,230);
    static MyCanvas canvas;
    World world;
    int xPosition, yPosition;
    double moveProb, sensorAccuracy;
    
    ServerSocket serverSocket;
    Socket clientSocket;
    PrintWriter sout;
    BufferedReader sin;
    
    Random rand;
    
    public BayesWorld(String f, double _moveProb, double _sensorAccuracy, String _known) {
        rand = new Random();
    
        world = new World(f);
        int width = 500;
        int height = 500;
        moveProb = _moveProb;
        sensorAccuracy = _sensorAccuracy;
        
        initRobotPosition();
    
        int bar = 20;
        setSize(width,height+bar);
        getContentPane().setBackground(backgroundColor);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(0, 0, width, height+bar);
        canvas = new MyCanvas(width, height, world, xPosition, yPosition);
        getContentPane().add(canvas);
        
        setVisible(true);
        setTitle("BayesWorld");
        
        getConnection(3333, f, _known);
        survive();
    }
    
    private void getConnection(int port, String fnombre, String _known) {
        System.out.println("Set up the connection:" + port);
        
        try {
            serverSocket = new ServerSocket(port);
            clientSocket = serverSocket.accept();
            sout = new PrintWriter(clientSocket.getOutputStream(), true);
            sin = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
    
            System.out.println("Connection established.");
        
            sout.println(fnombre);
            sout.println(moveProb);
            sout.println(sensorAccuracy);
            
            if (_known.equals("known")) {
                sout.println("known");
                sout.println(xPosition);
                sout.println(yPosition);
            }
            else {
                sout.println("unknown");
            }
        } catch (IOException e) {
            System.err.println("Caught IOException: " + e.getMessage());
        }
    }
    
    void initRobotPosition() {
        while (true) {
            // random initial position
            //xpos = rand.nextInt(mundo.width);
            //ypos = rand.nextInt(mundo.height);
            
            // random initial position in bottom right quadrant
            xPosition = rand.nextInt(world.width / 2) + (world.width/2);
            yPosition = rand.nextInt(world.height / 2) + (world.height/2);
    
            if (world.grid[xPosition][yPosition] == 0)
                break;
        }
    }
    
    void moveIt(int action) {
        int oldXPosition = xPosition, oldYPosition = yPosition;
        
        switch (action) {
            case NORTH: // up
                yPosition--;
                break;
            case SOUTH:
                yPosition++;
                break;
            case WEST:
                xPosition--;
                break;
            case EAST: //
                xPosition++;
                break;
            case 4: // stay
                break;
        }
        
        if (world.grid[xPosition][yPosition] == 1) {
            xPosition = oldXPosition;
            yPosition = oldYPosition;
        }
        canvas.updatePosition(xPosition, yPosition);
    }
    
    void moveRobot(int action) {
        double value = rand.nextInt(1001) / 1001.0;
        
        if (value <= moveProb)
            moveIt(action);
        else { // pick a different move randomly
            int other = rand.nextInt(5);
            while (other == action)
                other = rand.nextInt(5);
            moveIt(other);
        }
    }
    
    // returns a strong with a char specifying north south east west; 1 = wall; 0 = no wall
    String getSonarReadings() {
        double value = rand.nextInt(1001) / 1001.0;
        String reading = "";
        // north
        if (world.grid[xPosition][yPosition -1] == 1) { // it is a wall
            if (value <= sensorAccuracy)
                reading += "1";
            else
                reading += "0";
        }
        else { // it is not a wall
            if (value <= sensorAccuracy)
                reading += "0";
            else
                reading += "1";
        }
        // south
        value = rand.nextInt(1001) / 1001.0;
        if (world.grid[xPosition][yPosition +1] == 1) { // it is a wall
            if (value <= sensorAccuracy)
                reading += "1";
            else
                reading += "0";
        }
        else { // it is not a wall
            if (value <= sensorAccuracy)
                reading += "0";
            else
                reading += "1";
        }
        // east
        value = rand.nextInt(1001) / 1001.0;
        if (world.grid[xPosition +1][yPosition] == 1) { // it is a wall
            if (value <= sensorAccuracy)
                reading += "1";
            else
                reading += "0";
        }
        else { // it is not a wall
            if (value <= sensorAccuracy)
                reading += "0";
            else
                reading += "1";
        }
        // west
        value = rand.nextInt(1001) / 1001.0;
        if (world.grid[xPosition -1][yPosition] == 1) { // it is a wall
            if (value <= sensorAccuracy)
                reading += "1";
            else
                reading += "0";
        }
        else { // it is not a wall
            if (value <= sensorAccuracy)
                reading += "0";
            else
                reading += "1";
        }
    
        return reading;
    }
    
    void survive() {
        int action;
        boolean theEnd = false;
        int numMoves = 0;
        
        while (true) {
            try {
                action = Integer.parseInt(sin.readLine());
                System.out.println("Move the robot: " + action);
                moveRobot(action);
                
                String sonars = getSonarReadings();
                System.out.println(sonars);
                if (world.grid[xPosition][yPosition] == 3) {
                    System.out.println("Winner");
                    //sout.println("win");
                    sonars += "winner";
                    theEnd = true;
                }
                else if (world.grid[xPosition][yPosition] == 2) {
                    System.out.println("Loser");
                    //sout.println("lose");
                    sonars += "loser";
                    theEnd = true;
                }
                sout.println(sonars);
                
                numMoves++;
                
                if (theEnd)
                    break;
            }
            catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
        System.out.println("It took " + numMoves + " moves.");
    }

    public static void main(String[] args) {
        BayesWorld bw = new BayesWorld(args[0], Double.parseDouble(args[1]), Double.parseDouble(args[2]), args[3]);
    }
}