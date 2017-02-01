/**
 * Author: Alejandro Marquez Ferrer
 */

package mygame;

import com.jme3.app.SimpleApplication;
import com.jme3.app.StatsAppState;
import com.jme3.asset.TextureKey;
import com.jme3.audio.AudioNode;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.network.Filters;
import com.jme3.network.HostedConnection;
import com.jme3.network.Message;
import com.jme3.network.MessageListener;
import com.jme3.network.Network;
import com.jme3.network.Server;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Cylinder;
import com.jme3.scene.shape.Sphere;
import com.jme3.system.JmeContext;
import com.jme3.texture.Texture;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import mygame.UtNetworking.*;

/**
 * test
 *
 * @author normenhansen
 */
public class ServerMain extends SimpleApplication {

    private Server server;
    private float counter = 0;
    private Random random = new Random();
    private Geometry geom;

    public static void main(String[] args) {

        UtNetworking.initialiseSerializables();

        ServerMain app = new ServerMain();
        app.start(JmeContext.Type.Headless);
    }
    //INT Constants
    final int PLAYINGFIELD_RESOLUTION = 100;
    final int CAN_RESOLUTION = 100;
    final int CANNONBALL_NUM = 5;
    final int CANNONBALL_RESOLUTION = 100;
    final int LARGECAN_NUM = 10;
    final int LARGECAN_VALUE = 10;
    final int MEDIUMCAN_NUM = 6;
    final int MEDIUMCAN_VALUE = 20;
    final int SMALLCAN_NUM = 3;
    final int SMALLCAN_VALUE = 40;
    final int CANS_NUM = LARGECAN_NUM + MEDIUMCAN_NUM + SMALLCAN_NUM;
    //FLOAT Constants
    final float DEAD_MARGIN = 1f;
    final float START_TIME = 30f;
    final float PLAYINGFIELD_RADIUS = 200f;
    final float SMALLCAN_RADIUS = 3f;
    final float SMALLCAN_HEIGHT = 10f;
    final float MEDIUMCAN_RADIUS = 4f;
    final float MEDIUMCAN_HEIGHT = 15f;
    final float LARGECAN_RADIUS = 5f;
    final float LARGECAN_HEIGHT = 20f;
    final float MAXIMAL_CAN_RADIUS = LARGECAN_RADIUS;
    final float CANNON_SAFETYDISTANCE = 20f;
    final float SAFETY_MARGIN = 2f * MAXIMAL_CAN_RADIUS + CANNON_SAFETYDISTANCE;
    final float CANNONBALL_RADIUS = 1.1f * MAXIMAL_CAN_RADIUS;
    final float CANNONBALL_SPEED = 80f;
    final float CANNON_BARREL_RADIUS = CANNONBALL_RADIUS;
    final float CANNON_BARREL_LENGTH = MAXIMAL_CAN_RADIUS + CANNON_SAFETYDISTANCE;
    final float CANNON_SUPPORT_RADIUS = 2.1f * CANNON_BARREL_RADIUS;
    final float CANNON_SUPPORT_HEIGHT = 2.4f * CANNON_BARREL_RADIUS;
    final float CANNON_BASE_RADIUS = 3f * CANNON_BARREL_RADIUS;
    final float CANNON_BASE_HEIGHT = 3f * CANNON_BARREL_RADIUS;
    final float CANNON_ROTATION_SPEED = 20f;
    final float LASER_HEIGHT = PLAYINGFIELD_RADIUS;
    final float LASER_RADIUS = 0.1f * CANNON_BARREL_RADIUS;
    //Nodes
    private Node nodeGeom;
    //Server constants
    final float TIMEOUT = 5.0f;
    final int MAX_CHARS = 8;
    final int MAX_PLAYERS = 20;
    private Node[] cans;
    private Node[] cannons;
    private Node[][] cannonballs;
    private Node[] cams;
    private String[] nicknames;
    private int[] points;
    private int players = 0;
    private State currentState;
//    private Vector3f[] positions;
    private float[] timeouts;
    private boolean[] lasers;
    private static JLabel label;
    private float time = 0;
    private boolean finishing = false;
    private int cannonballsNum = 0;

    public enum State {

        STOPPED, RUNNING
    }
//    private ArrayList<HostedConnection> hostedConnections = new ArrayList<HostedConnection>();
    private HostedConnection[] hostedConnections;

    public ServerMain() {
        super(new StatsAppState());
    }

    @Override
    public void simpleInitApp() {
        try {
            server = Network.createServer(UtNetworking.PORT);
            server.start();
        } catch (IOException ex) {
            Logger.getLogger(ServerMain.class.getName()).log(Level.SEVERE, null, ex);
        }

        //Initialize playingfield
        this.pauseOnFocus = false;

        //Field
        Cylinder field = new Cylinder(30, 30, PLAYINGFIELD_RADIUS, 0f, true);
        field.scaleTextureCoordinates(new Vector2f(1f, 30f));
        Geometry cylinderField = new Geometry("Cylinder", field);
        Material matField = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        Texture tex = assetManager.loadTexture(new TextureKey("Textures/ground.jpg", false));
        tex.setWrap(Texture.WrapMode.Repeat); //This should set the texture to repeat.
        matField.setTexture("ColorMap", tex); // with Unshaded.j3md
        cylinderField.setMaterial(matField);

        nodeGeom = new Node("geoms");
        Node nodeField = new Node("Field");
        nodeField.attachChild(cylinderField);

        cans = new Node[CANS_NUM];
        cannons = new Node[MAX_PLAYERS];
        cannonballs = new Node[MAX_PLAYERS][CANNONBALL_NUM];
        cams = new Node[MAX_PLAYERS];
//        positions = new Vector3f[MAX_PLAYERS];

        for (int i = 0; i < MAX_PLAYERS; i++) {
            //Plate
            Cylinder plate = new Cylinder(30, 30, CANNON_SUPPORT_RADIUS, CANNON_SUPPORT_HEIGHT, true);
            Geometry cylinderPlate = new Geometry("CylBase", plate);
            Material matPlate = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
            matPlate.setColor("Color", ColorRGBA.Black);
            cylinderPlate.setMaterial(matPlate);

            //Base
            Cylinder base = new Cylinder(30, 30, CANNON_BASE_RADIUS, CANNON_BASE_HEIGHT, true);
            Geometry cylinderBase = new Geometry("Cylinder", base);
            Material matBase = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
            matBase.setColor("Color", ColorRGBA.Brown);
            cylinderBase.setMaterial(matBase);

            //Barrel
            Cylinder barrel = new Cylinder(30, 30, CANNON_BARREL_RADIUS, CANNON_BARREL_LENGTH, true);
            Geometry cylinderBarrel = new Geometry("Cylinder", barrel);
            Material matBarrel = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
            matBarrel.setColor("Color", ColorRGBA.DarkGray);
            cylinderBarrel.setMaterial(matBarrel);

            //Laser
            Cylinder claser = new Cylinder(30, 30, LASER_RADIUS, LASER_HEIGHT, true);
            Geometry cylinderLaser = new Geometry("Cylinder", claser);
            Material matLaser = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
            matLaser.setColor("Color", ColorRGBA.Red);
            cylinderLaser.setMaterial(matLaser);

            //Nodes
            Node nodeCannon = new Node("Cannon " + i);
            Node nodePlate = new Node("Plate");
            nodePlate.attachChild(cylinderPlate);
            Node nodeBase = new Node("Base");
            nodeBase.attachChild(cylinderBase);
            Node nodeBarrel = new Node("Barrel");
            nodeBarrel.attachChild(cylinderBarrel);
            Node nodeLaser = new Node("Laser");
            nodeLaser.attachChild(cylinderLaser);
            AudioNode audio = new AudioNode(assetManager, "Sounds/cannon.wav", false);
            audio.setPositional(true);
            audio.setVolume(3);

            Node nodeCam = new Node("Cam " + i);
//            nodeCam.attachChild(gBox);
            rootNode.attachChild(nodeCam);

            for (int j = 0; j < CANNONBALL_NUM; j++) {
                initBullet(i, j, nodeCannon);
            }

            nodeCannon.attachChild(nodePlate);
            nodeCannon.attachChild(nodeBase);
            nodeCannon.attachChild(nodeBarrel);
            nodeCannon.attachChild(nodeLaser);
            nodeCannon.attachChild(audio);
            nodeGeom.attachChild(nodeCannon);

            //Position
            nodePlate.move(0, 0, -CANNON_SUPPORT_HEIGHT / 2 + 0.1f);
            nodeBase.rotate(0, 90 * FastMath.DEG_TO_RAD, 0);
            nodeBarrel.move(0, CANNON_BASE_RADIUS, CANNON_BARREL_RADIUS);
            nodeBarrel.rotate(90 * FastMath.DEG_TO_RAD, 90 * FastMath.DEG_TO_RAD, 0);
            nodeLaser.move(0, LASER_HEIGHT / 2, LASER_RADIUS);
            nodeLaser.rotate(90 * FastMath.DEG_TO_RAD, 90 * FastMath.DEG_TO_RAD, 0);
            nodeLaser.setCullHint(Spatial.CullHint.Always);

            //Position of the cannons
            float angle = 2 * FastMath.PI / (MAX_PLAYERS);
            angle = angle * i;
            nodeCannon.move((float) Math.cos(angle) * PLAYINGFIELD_RADIUS,
                    (float) Math.sin(angle) * PLAYINGFIELD_RADIUS, 0);

            //Cam position and location
            nodeCam.move((float) Math.cos(angle) * PLAYINGFIELD_RADIUS * 1.5f, (float) Math.sin(angle) * PLAYINGFIELD_RADIUS * 1.5f, 1.5f * PLAYINGFIELD_RADIUS);
            nodeCam.rotate(0, 0, angle + FastMath.PI / 2);
            cams[i] = nodeCam;

            cannons[i] = nodeCannon;
        }

        //Cans initialization
        //Small ones
        for (int i = 0; i < SMALLCAN_NUM; i++) {
            initCan(i, "small");
        }
        //Medium ones
        for (int i = SMALLCAN_NUM; i < SMALLCAN_NUM + MEDIUMCAN_NUM; i++) {
            initCan(i, "medium");
        }
        //Large ones
        for (int i = SMALLCAN_NUM + MEDIUMCAN_NUM; i < cans.length; i++) {
            initCan(i, "large");
        }

        nodeGeom.attachChild(nodeField);
        rootNode.attachChild(nodeGeom);

        currentState = State.STOPPED;
        nicknames = new String[MAX_PLAYERS];
        for (int i = 0; i < nicknames.length; i++) {
            nicknames[i] = "";
        }

        timeouts = new float[MAX_PLAYERS];
        lasers = new boolean[MAX_PLAYERS];
        for (int i = 0; i < timeouts.length; i++) {
            timeouts[i] = TIMEOUT;
            lasers[i] = false;
        }

        points = new int[MAX_PLAYERS];
        hostedConnections = new HostedConnection[MAX_PLAYERS];
        server.addMessageListener(new MessageHandler());
        createGUI();
    }

    private class MessageHandler implements MessageListener<HostedConnection> {

        public void messageReceived(HostedConnection source, Message m) {


            if (m instanceof Connect) {
                Connect message = (Connect) m;
                String nickname = message.getN();
                if (currentState != State.RUNNING) {
                    if (players != MAX_PLAYERS) {
                        if (nickname.length() > 0 && nickname.length() <= 8) {
                            if (!repeatedNickname(nickname)) {
                                int idNew = connectPlayer(nickname, source);
                                server.broadcast(Filters.in(arrayToBroadCast()),
                                        new NewClientAccepted(nickname, idNew));
                                server.broadcast(Filters.equalTo(source),
                                        new Prepare(canPositions(), cannonsRotation(), nicknames, lasers));
                            } else {
                                server.broadcast(Filters.equalTo(source), new Reject("Nickname already in use"));
                            }
                        } else {
                            server.broadcast(Filters.equalTo(source),
                                    new Reject("Bad nickname"));
                        }
                    } else {
                        server.broadcast(Filters.equalTo(source),
                                new Reject("Maximal number of clients already connected"));
                    }
                } else {
                    server.broadcast(Filters.equalTo(source),
                            new Reject("Game already running"));
                }
            }

            final int id = findId(source);
            timeouts[id] = TIMEOUT;


            if (m instanceof Disconnect) {

                disconnectPlayer(id);

            }

            if (m instanceof Alive) {
                timeouts[id] = TIMEOUT;
            }

            if (m instanceof LaserInput) {
                timeouts[id] = TIMEOUT;
                if (!lasers[id]) {
                    server.broadcast(Filters.in(arrayToBroadCast()), new LaserToggled(id, true));
                    lasers[id] = true;
                } else {
                    server.broadcast(Filters.in(arrayToBroadCast()), new LaserToggled(id, false));
                    lasers[id] = false;
                }
            }

            if (m instanceof RotateInput) {
                RotateInput message = (RotateInput) m;
                timeouts[id] = TIMEOUT;
                final boolean right = message.getR();
                final float rotation = message.getA();
                ServerMain.this.enqueue(new Callable() {
                    public Object call() throws Exception {
                        if (right) {

                            cannons[id].rotate(0, 0, -rotation);
                        } else {
                            cannons[id].rotate(0, 0, rotation);
                        }
                        server.broadcast(Filters.in(arrayToBroadCast()), new Rotate(id, right, rotation));
                        return null;
                    }
                });

            }

            if (m instanceof FireInput) {
                if(currentState.equals(currentState.RUNNING)){
                    ServerMain.this.enqueue(new Callable() {
                        public Object call() throws Exception {

                            boolean find = false;
                            int i = 0;
                            Vector3f position = new Vector3f();
                            Vector3f direction = cannons[id].getLocalRotation().getRotationColumn(1);
                            while (i < CANNONBALL_NUM && !find) {
                                if (!((Boolean) cannonballs[id][i].getUserData("shooted"))) {
                                    position = cannonballs[id][i].getWorldTranslation();
                                    find = true;
                                } else {
                                    //Ball shooted
                                    i++;
                                }
                            }
                            if (find) {
                                /* We can shoot */
                                shootBall(id, i, direction, position);
                                cannonballsNum++;
                                server.broadcast(Filters.in(arrayToBroadCast()), new Activate(id, i, position, direction));
                            }
                            return null;
                        }
                    });
                }
            }
        }
    }

    @Override
    public void simpleUpdate(float tpf) {
        if (currentState.equals(State.RUNNING)){
            time = time - tpf;
            label.setText("   Current Status: " + currentState.name()+ ", Time: " + timeToString());
            if(time < 0){
                currentState = State.STOPPED;
                finishing = true;
                label.setText("   Current Status: " + currentState.name()+ ", Time: 0");
                time = 0;
            }
        }
        if(finishing && cannonballsNum == 0){
            finishing = false;
            Integer[] winners = getWinners();
            server.broadcast(Filters.in(hostedConnections), new Congratulate(winners,winners.length));
            label.setText("   Current Status: " + currentState.name());
        }
        for (int i = 0; i < timeouts.length; i++) {
            if (!nicknames[i].equals("")) {
                timeouts[i] = timeouts[i] - tpf;
                if (timeouts[i] < 0) {
                    disconnectPlayer(i);
                }
            }
        }
        for (int i = 0; i < MAX_PLAYERS; i++) {
            for (int j = 0; j < CANNONBALL_NUM; j++) {
                if (((Boolean) cannonballs[i][j].getUserData("shooted"))) {
                    Vector3f direction = (Vector3f) cannonballs[i][j].getUserData("direction");
                    if (!(cannonballs[i][j].getWorldTranslation().distance(new Vector3f(0, 0, 0))
                            > PLAYINGFIELD_RADIUS + DEAD_MARGIN)) {

                        /* If a ball has been shooted -> moves it */
                        cannonballs[i][j].move(direction.mult(tpf * CANNONBALL_SPEED));

                    } else {

                        /* If a ball has reached the limit, removes it */
                        cannonballs[i][j].removeFromParent();
                        initBullet(i, j, cannons[i]);
                        cannonballsNum--;
                        server.broadcast(Filters.in(arrayToBroadCast()), new Inactivate(i, j));
                    }
                    for (int z = 0; z < cans.length; z++) {
                        String type = cans[z].getUserData("type");
                        float radius = (Float) cans[z].getUserData("radius");
                        if (cannonballs[i][j].getWorldTranslation().
                                distance(cans[z].getWorldTranslation())
                                < (radius + CANNONBALL_RADIUS)) {


                            /* If a ball has reached a can, respawn the can in a
                             different position and add points */
                            cannonballs[i][j].removeFromParent();
                            initBullet(i, j, cannons[i]);
                            cannonballsNum--;
                            server.broadcast(Filters.in(arrayToBroadCast()), new Inactivate(i, j));
                            points[i] = points[i] + (Integer) cans[z].getUserData("value");
                            server.broadcast(Filters.in(arrayToBroadCast()), new Award(i, points[i]));
                            cans[z].removeFromParent();
                            Vector3f pos = initCan(z, type);
                            server.broadcast(Filters.in(arrayToBroadCast()), new Move(z, pos));
//                            pointsHud.setText(points+"");
                        }
                    }
                    for (int i1 = 0; i1 < MAX_PLAYERS; i1++) {
                        for (int j1 = 0; j1 < CANNONBALL_NUM; j1++) {
                            if (!(i == i1 && j == j1)
                                    && (Boolean) cannonballs[i1][j1].getUserData("shooted")
                                    && cannonballs[i1][j1].getUserData("direction") != null
                                    && cannonballs[i][j].getUserData("direction") != null
                                    && (!collidedWith(i, j, i1, j1))) {
                                if (cannonballs[i][j].getWorldTranslation()
                                        .distance(cannonballs[i1][j1]
                                        .getWorldTranslation())
                                        <= 2 * CANNONBALL_RADIUS) {
                                    Vector3f d1 = cannonballs[i][j].getUserData("direction");
                                    Vector3f x1 = cannonballs[i][j].getWorldTranslation();

                                    Vector3f d2 = cannonballs[i1][j1].getUserData("direction");
                                    Vector3f x2 = cannonballs[i1][j1].getWorldTranslation();
                                    
                                    Vector3f newd1 = calculateCollision(d1, d2, x1, x2);
                                    Vector3f newd2 = calculateCollision(d2, d1, x2, x1);

                                    cannonballs[i][j].setUserData("collided", true);
                                    cannonballs[i][j].setUserData("collided with", i1 + " " + j1);

                                    cannonballs[i1][j1].setUserData("collided", true);
                                    cannonballs[i1][j1].setUserData("collided with", i + " " + j);

                                    cannonballs[i][j].setUserData("direction", newd1);
                                    cannonballs[i1][j1].setUserData("direction", newd2);
//                                    System.out.println("Cannon " + i + " new direction " + newd1);
//                                    System.out.println("Cannon " + i1 + " new direction " + newd2);
                                    server.broadcast(Filters.in(hostedConnections), new Change(i, j, x1, newd1));
                                    server.broadcast(Filters.in(hostedConnections), new Change(i1, j1, x2, newd2));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void simpleRender(RenderManager rm) {
        //TODO: add render code
    }

    @Override
    public void destroy() {
        server.close();
        super.destroy();
    }

    private Vector3f initCan(int i, String type) {
        cans[i] = new Node(i + "");
        cans[i].setUserData("type", type);
        Geometry can;
        float height = 0;
        if (type.equals("small")) {
            Cylinder c = new Cylinder(30, 30, SMALLCAN_RADIUS, SMALLCAN_HEIGHT, true);
            can = new Geometry("Can", c);
            Texture tex = assetManager.loadTexture(new TextureKey("Textures/metalT.png", false));
            tex.setWrap(Texture.WrapMode.Repeat); //This should set the texture to repeat.
            Material mark_mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
            mark_mat.setTexture("ColorMap", tex);
            mark_mat.setColor("Color", ColorRGBA.Red);
            can.setMaterial(mark_mat);
            height = height + SMALLCAN_HEIGHT / 2;
            cans[i].setUserData("value", SMALLCAN_VALUE);
            cans[i].setUserData("radius", SMALLCAN_RADIUS);
        } else if (type.equals("medium")) {
            Cylinder c = new Cylinder(30, 30, MEDIUMCAN_RADIUS, MEDIUMCAN_HEIGHT, true);
            can = new Geometry("Can", c);
            Texture tex = assetManager.loadTexture(new TextureKey("Textures/metalT.png", false));
            tex.setWrap(Texture.WrapMode.Repeat); //This should set the texture to repeat.
            Material mark_mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
            mark_mat.setTexture("ColorMap", tex);
            mark_mat.setColor("Color", ColorRGBA.Orange);
            can.setMaterial(mark_mat);
            height = height + MEDIUMCAN_HEIGHT / 2;
            cans[i].setUserData("value", MEDIUMCAN_VALUE);
            cans[i].setUserData("radius", MEDIUMCAN_RADIUS);
        } else {
            Cylinder c = new Cylinder(30, 30, LARGECAN_RADIUS, LARGECAN_HEIGHT, true);
            can = new Geometry("Can", c);
            Texture tex = assetManager.loadTexture(new TextureKey("Textures/metalT.png", false));
            tex.setWrap(Texture.WrapMode.Repeat); //This should set the texture to repeat.
            Material mark_mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
            mark_mat.setTexture("ColorMap", tex);
            mark_mat.setColor("Color", ColorRGBA.Yellow);
            can.setMaterial(mark_mat);
            height = height + LARGECAN_HEIGHT / 2;
            cans[i].setUserData("value", LARGECAN_VALUE);
            cans[i].setUserData("radius", LARGECAN_RADIUS);
        }
        cans[i].attachChild(can);

        boolean put = false;
        while (!put) {
            put = true;
            double angle = Math.random() * Math.PI * 2;

            /* Random position */
            cans[i].move((float) Math.cos(angle) * randomDistance(),
                    (float) Math.sin(angle) * randomDistance(), height);

            for (int j = 0; j < cans.length; j++) {
                if (cans[j] != null && cans[j].getChildren() != null
                        && cans[j].getChildren().size() > 0) {
                    if (!cans[i].getName().equals(cans[j].getName())
                            && colision(cans[i], cans[j])) {

                        /* Two cans in conflict -> we respawn the can again*/
                        cans[i].setLocalTranslation(0, 0, 0);
                        put = false;
                    }
                }
            }
        }
        nodeGeom.attachChild(cans[i]);
        return cans[i].getWorldTranslation();
    }

    /**
     * Calculates a random position in the playfield (margin applied)
     */
    private float randomDistance() {
        float minX = -(PLAYINGFIELD_RADIUS - SAFETY_MARGIN);
        float maxX = (PLAYINGFIELD_RADIUS - SAFETY_MARGIN);
        Random rand = new Random();
        return rand.nextFloat() * (maxX - minX) + minX;
    }

    private boolean colision(Node can1, Node can2) {
        return (can1.getWorldTranslation().distance(can2.getWorldTranslation())
                < (Float) can1.getUserData("radius") + (Float) can2.getUserData("radius"));
    }

    private void initBullet(int i, int j, Node nodeCannon) {

        cannonballs[i][j] = new Node("Cannonball " + i + " " + j);
        cannonballs[i][j].setUserData("shooted", false);
        Sphere sphere = new Sphere(30, 30, CANNONBALL_RADIUS);
        Geometry mark = new Geometry("BOOM!", sphere);

        Texture tex = assetManager.loadTexture(new TextureKey("Textures/rockT.jpg", false));
        tex.setWrap(Texture.WrapMode.Repeat); //This should set the texture to repeat.    
        Material mark_mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mark_mat.setTexture("ColorMap", tex);
        mark.setMaterial(mark_mat);
        cannonballs[i][j].attachChild(mark);
        cannonballs[i][j].setCullHint(Spatial.CullHint.Always);
        nodeCannon.attachChild(cannonballs[i][j]);
        cannonballs[i][j].move(0, CANNON_BARREL_LENGTH + CANNONBALL_RADIUS, CANNONBALL_RADIUS);
    }

    public boolean repeatedNickname(String nickname) {
        int i = 0;
        boolean rep = false;
        while (i < nicknames.length && !rep) {
            if (nicknames[i] != null && nicknames[i].equals(nickname)) {
                rep = true;
            } else {
                i++;
            }
        }
        return rep;
    }

    public int connectPlayer(String nickname, HostedConnection s) {
        int i = 0;
        boolean find = false;
        while (i < nicknames.length && !find) {
            if (nicknames[i].equals("")) {
                nicknames[i] = nickname;
//                cannons[i].setCullHint(Spatial.CullHint.Inherit);
//                hostedConnectionsIDs.add(i,s.getId());
                hostedConnections[i] = s;
                players++;
                find = true;
            } else {
                i++;
            }
        }
        return i;
    }

    public Vector3f[] canPositions() {
        Vector3f[] pa = new Vector3f[CANS_NUM];
        for (int i = 0; i < pa.length; i++) {
            pa[i] = cans[i].getWorldTranslation();
        }
        return pa;
    }

    public void disconnectPlayer(int id) {
//        hostedConnectionsIDs.remove(id);
        System.out.println("Player disconnected");
        hostedConnections[id] = null;
        nicknames[id] = "";
        timeouts[id] = TIMEOUT;
        points[id] = 0;
        cannons[id].setLocalRotation(Quaternion.IDENTITY);
        players--;
        lasers[id] = false;
//        cannons[id].setCullHint(Spatial.CullHint.Always);
        server.broadcast(Filters.in(arrayToBroadCast()), new Disconnected(id));
    }

    private void shootBall(int id, int ball, Vector3f direction, Vector3f position) {
//        System.out.println("Cannon " + id + " direction " + direction);
        cannonballs[id][ball].setCullHint(Spatial.CullHint.Never);
        cannons[id].detachChild(cannonballs[id][ball]);
        nodeGeom.attachChild(cannonballs[id][ball]);
        cannonballs[id][ball].setLocalTranslation(position);
        cannonballs[id][ball].setUserData("direction", direction);
        cannonballs[id][ball].setUserData("shooted", true);
    }

    private Vector3f calculateCollision(Vector3f d1, Vector3f d2, Vector3f x1, Vector3f x2) {
        
        float angle = (float) Math.atan2((x1.y - x2.y), (x1.x - x2.x));

        double dir1 = Math.atan2(d1.y, d1.x);
        double dir2 = Math.atan2(d2.y, d2.x);

        double newxspeed1 = Math.cos(dir1 - angle);
        double newyspeed1 = Math.sin(dir1 - angle);
        double newxspeed2 = Math.cos(dir2 - angle);
        double newyspeed2 = Math.sin(dir2 - angle);

        double finalxspeed1 = newxspeed2;
        double finalxspeed2 = newxspeed1;
        double finalyspeed1 = newyspeed1;
        double finalyspeed2 = newyspeed2;

        double finaldirx = Math.cos(angle) * finalxspeed1 + Math.cos(angle + Math.PI / 2) * finalyspeed1;
        double finaldiry = Math.sin(angle) * finalxspeed1 + Math.sin(angle + Math.PI / 2) * finalyspeed1;
        double finaldirx1 = Math.cos(angle) * finalxspeed2 + Math.cos(angle + Math.PI / 2) * finalyspeed2;
        double finaldiry1 = Math.sin(angle) * finalxspeed2 + Math.sin(angle + Math.PI / 2) * finalyspeed2;

        return new Vector3f((float) finaldirx, (float) finaldiry, 0).normalize();
    }

    private void change(int i, int j, Vector3f direction, Vector3f position, int i1, int j1, Vector3f dir1, Vector3f pos1) {
        cannonballs[i][j].setLocalTranslation(position);
        cannonballs[i][j].setUserData("direction", direction);
        cannonballs[i][j].setUserData("shooted", true);

        cannonballs[i1][j1].setLocalTranslation(pos1);
        cannonballs[i1][j1].setUserData("direction", dir1);
        cannonballs[i1][j1].setUserData("shooted", true);


    }

    private float[][] cannonsRotation() {
        float[][] q = new float[MAX_PLAYERS][4];
        for (int i = 0; i < MAX_PLAYERS; i++) {
            Quaternion or = cannons[i].getLocalRotation();
            q[i][0] = or.getX();
            q[i][1] = or.getY();
            q[i][2] = or.getZ();
            q[i][3] = or.getW();
        }
        return q;
    }

    private Vector3f[] cannonsPosition() {
        Vector3f[] q = new Vector3f[MAX_PLAYERS];
        for (int i = 0; i < MAX_PLAYERS; i++) {
            q[i] = cannons[i].getWorldTranslation();
        }
        return q;
    }

    private int findId(HostedConnection source) {
        int i = 0;
        boolean find = false;
        while (i < hostedConnections.length && !find) {
            if (hostedConnections[i] != null && hostedConnections[i].equals(source)) {
                find = true;
            } else {
                i++;
            }
        }
        if (i < hostedConnections.length) {
            return i;
        } else {
            return -1;
        }
    }

    private ArrayList<HostedConnection> arrayToBroadCast() {
        ArrayList<HostedConnection> hs = new ArrayList<HostedConnection>();
        for (int i = 0; i < hostedConnections.length; i++) {
            if (hostedConnections[i] != null) {
                hs.add(hostedConnections[i]);
            }
        }
        return hs;
    }

    private boolean collidedWith(int i, int j, int i1, int j1) {
        boolean canCollide = false;
        if (cannonballs[i][j].getUserData("collided") != null) {
            String ballCollided = cannonballs[i][j].getUserData("collided with");
            Scanner s = new Scanner(ballCollided);
            int balli = s.nextInt();
            int ballj = s.nextInt();
            canCollide = (balli == i1 && ballj == j1);
        }
        return canCollide;
    }

    private void createGUI() {
        JFrame frame = new JFrame("Control Panel");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(300, 200);

        addComponentsToPane(frame.getContentPane());

        frame.pack();
        frame.setVisible(true);

    }

    public void addComponentsToPane(Container pane) {

        if (!(pane.getLayout() instanceof BorderLayout)) {
            pane.add(new JLabel("Container doesn't use BorderLayout!"));
            return;
        }

        //Make the center component big, since that's the
        //typical usage of BorderLayout.
        JButton button = new JButton("Start");
        button.setPreferredSize(new Dimension(150, 80));
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ServerMain.this.enqueue(new Callable() {
                    public Object call() throws Exception {
                        System.out.println("Running");
                        for(int i = 0; i < MAX_PLAYERS; i++){
                            points[i] = 0;
                        }
                        server.broadcast(Filters.in(hostedConnections),
                                        new Prepare(canPositions(), cannonsRotation(),
                                nicknames, lasers));
                        server.broadcast(Filters.in(hostedConnections),
                                new Start());
                        time = START_TIME;
                        currentState = State.RUNNING;
                        return null;
                    }
                });
            }
        });
        pane.add(button, BorderLayout.LINE_START);

        label = new JLabel("   Current Status: " + currentState.name());
        pane.add(label, BorderLayout.PAGE_END);

        button = new JButton("Stop");
        button.setPreferredSize(new Dimension(150, 80));
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ServerMain.this.enqueue(new Callable() {
                    public Object call() throws Exception {
                        System.out.println("Stopped");
                        currentState = State.STOPPED;
                        return null;
                    }
                });
            }
        });
        pane.add(button, BorderLayout.LINE_END);
    }
    
    private Integer[] getWinners(){
        ArrayList<Integer> l = new ArrayList<Integer>();
        int max = 0;
        
        for (int i = 0; i < MAX_PLAYERS; i++) {
            if(points[i] != 0 && !nicknames[i].equals("")){
                if(points[i] > max){
                    max = points[i];
                }
            }
        }
        for (int i = 0; i < MAX_PLAYERS; i++) {
            if(points[i] != 0 && !nicknames[i].equals("")){
                if(points[i] == max){
                    l.add(i);
                }
            }
        }
        
        Integer[] winners = new Integer[l.size()];
        l.toArray(winners);
        return winners;
    }
    
    /**
     * Transforms the time from float to String (format: s:hs)
     */
    private String timeToString(){
        String seconds = (int)time + "";
        String ms = ((int)(time*100))%100 + "";
        return seconds + ":" + ms;
    }
}
