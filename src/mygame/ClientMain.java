/**
 * Author: Alejandro Marquez Ferrer
 */

package mygame;

import com.jme3.app.SimpleApplication;
import com.jme3.app.StatsAppState;
import com.jme3.asset.TextureKey;
import com.jme3.audio.AudioNode;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.event.TouchEvent;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.network.Client;
import com.jme3.network.Message;
import com.jme3.network.MessageListener;
import com.jme3.network.Network;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Cylinder;
import com.jme3.scene.shape.Sphere;
import com.jme3.texture.Texture;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import mygame.ServerMain.State;
import mygame.UtNetworking.*;

/**
 * test
 * @author normenhansen
 */
public class ClientMain extends SimpleApplication {

    private Client client;
    private BlockingQueue<String> messageQueue;
    private Geometry geom;
    private int id;
    private String nickname;
    private boolean connected;
    
    public static void main(String[] args) {
        
        UtNetworking.initialiseSerializables();
        
        ClientMain app = new ClientMain();
        app.start();
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
    private BitmapText timerHud;
    private BitmapText pointsHud;
    private BitmapText nickNameHud;
    private BitmapText statusHud;
    
    //Server constants
    final float TIMEOUT = 5.0f;
    final int MAX_CHARS = 8;
    final int MAX_PLAYERS = 20;
    
    final float SENDERTIME = 0.03f;
    
    private Node[] cans;
    private Node[] cannons;
    private Node[][] cannonballs;
    
    //Nickname Constants
    private boolean goodNickname;
    private float counter;
    private MyListenerClass initialListener;
    private boolean waiting;
    
    private float time = START_TIME;
    private int[] points;
    
    private boolean laser = false;
    boolean playing = false;
    private String[] nicknames;
    
    private BitmapText[] otherPoints;
    private Vector3f[] positions;
    private AudioNode[] audios;
    
    private ServerMain.State currentState;
    
    public ClientMain(){
        super(new StatsAppState());
    }

    @Override
    public void simpleInitApp() {
        try {
            client = Network.connectToServer("127.0.0.1", UtNetworking.PORT);
            client.start();
        } catch (IOException ex) {
            Logger.getLogger(ClientMain.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        messageQueue = new LinkedBlockingQueue<String>() {};
        client.addMessageListener(new NetworkMessageListener());
        
        //Field
        Cylinder field = new Cylinder(30,30,PLAYINGFIELD_RADIUS,0f,true);
        field.scaleTextureCoordinates(new Vector2f(1f, 30f));
        Geometry cylinderField = new Geometry("Cylinder",field);
        Material matField = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        Texture tex = assetManager.loadTexture( new TextureKey("Textures/ground.jpg", false) );
        tex.setWrap(Texture.WrapMode.Repeat); //This should set the texture to repeat.
        matField.setTexture("ColorMap", tex); // with Unshaded.j3md
        cylinderField.setMaterial(matField);
        
        nodeGeom = new Node("geoms");
        Node nodeField = new Node("Field");
        nodeField.attachChild(cylinderField);
        
        cans = new Node[CANS_NUM];
        cannons = new Node[MAX_PLAYERS];
        cannonballs = new Node[MAX_PLAYERS][CANS_NUM];
        points = new int[MAX_PLAYERS];
        positions = new Vector3f[MAX_PLAYERS];
        
        for (int i = 0; i < MAX_PLAYERS; i++) {
            //Position of the cannons
            float angle = 2 * FastMath.PI / (MAX_PLAYERS);
            angle = angle * i;
            positions[i] = new Vector3f((float) Math.cos(angle) * PLAYINGFIELD_RADIUS,
                    (float) Math.sin(angle) * PLAYINGFIELD_RADIUS, 0);
        }
        //Cans initialization
        //Small ones
        for (int i = 0; i < SMALLCAN_NUM; i++){
            initCan(i,"small");
        }
        //Medium ones
        for (int i = SMALLCAN_NUM; i < SMALLCAN_NUM + MEDIUMCAN_NUM; i++){
            initCan(i,"medium");
        }
        //Large ones
        for (int i = SMALLCAN_NUM + MEDIUMCAN_NUM; i < cans.length; i++){
            initCan(i,"large");
        }
        
        for (int i = 0; i < MAX_PLAYERS; i++) {
            for (int j = 0; j < CANNONBALL_NUM; j++) {
                cannonballs[i][j] = new Node();
                cannonballs[i][j].setUserData("shooted", false);
            }
            
        }
        
        nodeGeom.attachChild(nodeField);
        
        /* We initialize the first dialogue to choose nickname */
        goodNickname = false;
        initialListener = new MyListenerClass();
        inputManager.addRawInputListener(initialListener);
        nickNameHud = new BitmapText(guiFont, false);          
        nickNameHud.setSize(guiFont.getCharSet().getRenderedSize()+10);      // font size
        nickNameHud.setColor(ColorRGBA.White);                             // font color
        nickNameHud.setText("Insert nickname: ");
        nickNameHud.setLocalTranslation(    // position
         settings.getWidth()/2 - (guiFont.getLineWidth(nickNameHud.getText() + "    ")),
         settings.getHeight()/2 + (guiFont.getCharSet().getRenderedSize()+10)/2, 0); 
        guiNode.attachChild(nickNameHud);
        nickname = "";
        counter = 0;
        this.pauseOnFocus = false;
        
        connected = true;
        waiting = true;
        otherPoints = new BitmapText[MAX_PLAYERS];
        audios = new AudioNode[MAX_PLAYERS];
        
        currentState = State.STOPPED;
        
        setDisplayFps(false);
        setDisplayStatView(false);
    }

    @Override
    public void simpleUpdate(float tpf) {
        if (currentState.equals(State.RUNNING)){
            time = time - tpf;
            timerHud.setText(timeToString());
            if(time < 0){
                currentState = State.STOPPED;
                String status = statusHud.getText();
                if(!status.equals("")){
                    statusHud.setText("Waiting...");
                }
                time = 0;
                timerHud.setText("00:00");
            }
        }
        if (!goodNickname){
            counter += tpf;
            if(counter > 0.5f){
                nickNameHud.setText("Insert nickname: " + nickname + "|");
                if(counter > 1f){
                    counter = 0;
                }
            }
            else{
                nickNameHud.setText("Insert nickname: " + nickname);
            }
        }
        else if(playing){
            for(int i = 0; i < MAX_PLAYERS; i++){
                for(int j = 0; j < CANNONBALL_NUM; j++){
                    if(((Boolean)cannonballs[i][j].getUserData("shooted"))){

                        Vector3f direction = (Vector3f)cannonballs[i][j].getUserData("direction");

                        /* If a ball has been shooted -> moves it */
                        cannonballs[i][j].move(direction.mult(tpf*CANNONBALL_SPEED));

                        cannonballs[i][j].rotate(-(tpf*CANNONBALL_SPEED/CANNONBALL_RADIUS),0, 0);

                    }
                }
            }
        }
       
    }

    @Override
    public void simpleRender(RenderManager rm) {
        //TODO: add render code
    }
    
    private class NetworkMessageListener implements MessageListener<Client>{

        public void messageReceived(Client source, Message m) {
            
            if(m instanceof NewClientAccepted){
                NewClientAccepted message = (NewClientAccepted) m;
                
                final String n = message.getN();
                final int c = message.getC();
                
                if(n.equals(nickname)){
                    
                    ClientMain.this.enqueue(new Callable(){
                        public Object call() throws Exception{
                            id = c;
                            initField();
                            cannons[id] = createCannon(id,n,true,false);
                            return null;
                        }
                     });
                    
                   
                }
                else{
                    ClientMain.this.enqueue(new Callable(){
                        public Object call() throws Exception{
                            cannons[c] = createCannon(c,n,false,false);
                            nicknames[c] = n; 
                            return null;
                        }
                     });
                }
            }
            
            if(m instanceof Reject){
                Reject message = (Reject)m;
                
                String reason = "Connection refused: " + message.getReason();
                
                nickNameHud.setLocalTranslation(    // position
                settings.getWidth()/2 - (guiFont.getLineWidth(reason))/2,
                settings.getHeight()/2 + (guiFont.getCharSet().getRenderedSize()+10)/2, 0); 
                nickNameHud.setText(reason);
            }
            
            if(m instanceof Prepare){
                Prepare message = (Prepare)m;
                
                final Vector3f[] posCans = message.getPa();
                final float[][] orCannons = message.getOa();
                final String[] nicks = message.getN();
                final boolean[] lasers = message.getLa();
                nicknames=nicks;
                
                ClientMain.this.enqueue(new Callable(){
                    public Object call() throws Exception{

                        for(int i = 0; i < MAX_PLAYERS; i++){
                            if(!nicknames[i].equals("") && i != id){
                                if(cannons[i]==null){
                                    cannons[i] = createCannon(i, nicks[i],false,lasers[i]);
                                }
                                Quaternion q = new Quaternion(orCannons[i][0],orCannons[i][1],orCannons[i][2],orCannons[i][3]);
                                cannons[i].setLocalRotation(q);
                            }
                        }
                        for(int i = 0; i < CANS_NUM; i++){
                            cans[i].setCullHint(Spatial.CullHint.Inherit);
                            Vector3f p = new Vector3f();
                            nodeGeom.worldToLocal(posCans[i], p);
                            cans[i].setLocalTranslation(p);
                        }
                        
                        prepareHUD();
                        return null;
                    }
                });
            }
            
            if(m instanceof Disconnected){
                Disconnected message = (Disconnected) m;
                
                final int idDisconnected = message.getC();
                
                ClientMain.this.enqueue(new Callable(){
                        public Object call() throws Exception{
                            cannons[idDisconnected].removeFromParent();
                            nicknames[idDisconnected] = "";
                            points[idDisconnected] = 0;
                            otherPoints[idDisconnected].setText("");
                            return null;
                        }
               });
            }
            
            if(m instanceof LaserToggled){
                LaserToggled message = (LaserToggled) m;
                
                final int idLaser = message.getI();
                final boolean b = message.isB();
                
                if(id != idLaser){
                    if(b){
                        ClientMain.this.enqueue(new Callable(){
                            public Object call() throws Exception{
                                cannons[idLaser].getChild("Laser").setCullHint(Spatial.CullHint.Never);
                                return null;
                            }
                        });
                    }
                    else{
                        ClientMain.this.enqueue(new Callable(){
                            public Object call() throws Exception{
                                cannons[idLaser].getChild("Laser").setCullHint(Spatial.CullHint.Always);
                                return null;
                            }
                        });
                    }
                }
            }
            if(m instanceof Rotate){
                Rotate message = (Rotate) m;
                
                final int idRotation = message.getI();
                final boolean right = message.isB();
                final float rotation = message.getA();
                
                if(id!=idRotation){
                    if(right){
                        ClientMain.this.enqueue(new Callable(){
                            public Object call() throws Exception{
                                cannons[idRotation].rotate(0,0,-rotation);
                                return null;
                            }
                        });
                    }
                    else{
                        ClientMain.this.enqueue(new Callable(){
                            public Object call() throws Exception{
                                cannons[idRotation].rotate(0,0,rotation);
                                return null;
                            }
                        });
                    }
                }
            }
            
            if (m instanceof Activate){
                Activate message = (Activate) m;
                
                final Vector3f position = message.getP();
                final Vector3f direction = message.getD();
                final int cIndex = message.getC();
                final int idFired = message.getI();
                
                ClientMain.this.enqueue(new Callable(){
                    public Object call() throws Exception{
                        shootBall(direction,position,cIndex,idFired);
                        return null;
                    }
                });
            }
            
            if(m instanceof Inactivate){
                Inactivate message = (Inactivate) m;
                
                final int cId = message.getC();
                final int pId = message.getI();
                
                ClientMain.this.enqueue(new Callable(){
                    public Object call() throws Exception{
                        cannonballs[pId][cId].removeFromParent();
                        initBullet(pId,cId);
                        return null;
                    }
                });
            }
            
            if (m instanceof Move){
                Move message = (Move) m;
                
                final int canId = message.getC();
                final Vector3f position = message.getP();
                
                ClientMain.this.enqueue(new Callable(){
                    public Object call() throws Exception{
                        cans[canId].setLocalTranslation(position);
                        return null;
                    }
                });
            }
            
            if (m instanceof Award){
                Award message = (Award) m;
                
                final int pointsId = message.getI();
                final int point = message.getS();
                
                ClientMain.this.enqueue(new Callable(){
                   public Object call() throws Exception{
                       points[pointsId] = point;
                       
                       if(pointsId == id){
                           pointsHud.setText(point + "");
                       }
                       else{
                          otherPoints[pointsId].setText(nicknames[pointsId] + "\n" + point);
                       }
                       
                       return null;
                   } 
                });
                
            }
            
            if (m instanceof Change){
                Change message = (Change) m;
                
                final int i = message.getI();
                final int j = message.getC();
                final Vector3f position = message.getP();
                final Vector3f direction = message.getD();
                
                Vector3f olddir = cannonballs[i][j].getUserData("direction");
                float c = 0;
                if(olddir!= null){
                    c = FastMath.atan2(direction.y,direction.x) - FastMath.atan2(olddir.y,olddir.x);
                }
                final float change = c;
                
                ClientMain.this.enqueue(new Callable(){
                   public Object call() throws Exception{
                       cannonballs[i][j].setLocalTranslation(position);
                       cannonballs[i][j].setLocalRotation((Quaternion)cannonballs[i][j].getUserData("rot"));
                       cannonballs[i][j].rotate(0,0,change)/*0, 0, change)*/;
                       cannonballs[i][j].setUserData("direction", direction);
                       return null;
                   } 
                });

            }
            
            if (m instanceof Start){
                
                ClientMain.this.enqueue(new Callable(){
                   public Object call() throws Exception{
                       time = START_TIME;
                       currentState = State.RUNNING;
                       statusHud.setText("");
                       return null;
                   } 
                });
            }
            
            if(m instanceof Congratulate){
                
                Congratulate message = (Congratulate)m;
                
                final Integer[] congratulate = message.getW();
                
                ClientMain.this.enqueue(new Callable(){
                   public Object call() throws Exception{
                       String winners = "Winner(s):" + "\n";
                       for(int i = 0; i < congratulate.length; i++){
                           winners = winners + " -" + nicknames[congratulate[i]] + "\n"; 
                       }
                       winners = winners + "Waiting...";
                       statusHud.setText(winners);
                       return null;
                   } 
                });
            }
        }
    }
    
    @Override
    public void destroy() {
        client.close();
        super.destroy();
    }
    
    private void initCan(int i, String type){
        cans[i] = new Node(i + "");
        cans[i].setUserData("type", type);
        Geometry can;
        if(type.equals("small")){
            Cylinder c = new Cylinder(30,30,SMALLCAN_RADIUS,SMALLCAN_HEIGHT,true);
            can = new Geometry("Can",c);
            Texture tex = assetManager.loadTexture( new TextureKey("Textures/metalT.png", false) );
            tex.setWrap(Texture.WrapMode.Repeat); //This should set the texture to repeat.
            Material mark_mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
            mark_mat.setTexture("ColorMap", tex);
            mark_mat.setColor("Color", ColorRGBA.Red);
            can.setMaterial(mark_mat);
        }
        else if(type.equals("medium")){
            Cylinder c = new Cylinder(30,30,MEDIUMCAN_RADIUS,MEDIUMCAN_HEIGHT,true);
            can = new Geometry("Can",c);
            Texture tex = assetManager.loadTexture( new TextureKey("Textures/metalT.png", false) );
            tex.setWrap(Texture.WrapMode.Repeat); //This should set the texture to repeat.
            Material mark_mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
            mark_mat.setTexture("ColorMap", tex);
            mark_mat.setColor("Color", ColorRGBA.Orange);
            can.setMaterial(mark_mat);
        }
        else{
            Cylinder c = new Cylinder(30,30,LARGECAN_RADIUS,LARGECAN_HEIGHT,true);
            can = new Geometry("Can",c);
            Texture tex = assetManager.loadTexture( new TextureKey("Textures/metalT.png", false) );
            tex.setWrap(Texture.WrapMode.Repeat); //This should set the texture to repeat.
            Material mark_mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
            mark_mat.setTexture("ColorMap", tex);
            mark_mat.setColor("Color", ColorRGBA.Yellow);
            can.setMaterial(mark_mat);         
        }
        cans[i].attachChild(can);
        cans[i].setCullHint(Spatial.CullHint.Always);
        nodeGeom.attachChild(cans[i]);
    }
    
    public class MyListenerClass implements RawInputListener{
        public void beginInput() {
        }

        public void endInput() {
        }

        public void onJoyAxisEvent(JoyAxisEvent evt) {
        }

        public void onJoyButtonEvent(JoyButtonEvent evt) {
        }

        public void onMouseMotionEvent(MouseMotionEvent evt) {
        }

        public void onMouseButtonEvent(MouseButtonEvent evt) {
        }

        public void onKeyEvent(KeyInputEvent evt) {
            if(evt.isPressed()){
                if(evt.getKeyCode() == KeyInput.KEY_RETURN){
                    try {
                        goodNickname = true;
                        waiting = true;
                        sendConnectMsg(nickname);
                    } catch (Throwable ex) {
                        Logger.getLogger(ClientMain.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                else if(evt.getKeyCode() == KeyInput.KEY_BACK){
                    if(nickname.length()>0){
                        nickname = nickname.substring(0, nickname.length()-1);
                    }
                    nickNameHud.setText("Insert nickname: " + nickname + "|");
                }
                else{
                    nickname = nickname + evt.getKeyChar();
                    nickNameHud.setText("Insert nickname: " + nickname + "|");
                }
            }
        }

        public void onTouchEvent(TouchEvent evt) {
        
        }
        
        
    }
    public void initField(){
            
        inputManager.removeRawInputListener(initialListener);
        nickNameHud.removeFromParent();
        
        //Show PlayingField
        rootNode.attachChild(nodeGeom);
        Thread t = new Thread(new Sender());
        t.start();
        initKeys();
        initHUD();
        Vector3f camPosition = new Vector3f(positions[id].getX()*1.75f,positions[id].getY()*1.75f,2f* PLAYINGFIELD_RADIUS);
        cam.setLocation(camPosition);
        cam.lookAt(new Vector3f(0,0,0), new Vector3f(0,0,1));
    }
    
    private class Sender implements Runnable {

        
        public Sender(){
            
        }
        
        public void run() {
           while (connected){
               try {
                   Thread.sleep((long) SENDERTIME*1000);
//                   String s = messageQueue.poll((long)TIMEOUT, TimeUnit.SECONDS
                   String s = messageQueue.poll((long)(TIMEOUT - SENDERTIME - 1),TimeUnit.SECONDS);
                   if(s == null){
                       client.send(new Alive());
                   }
                   else{
                       boolean rotate = false;
                       float rotation = 0;
                       boolean laser = false;
                       ArrayList<String> l = new ArrayList();
                       l.add(s);
                       int n = messageQueue.drainTo(l);
                       for (int i = 0; i < n+1; i++) {
                           s = l.get(i);
                           Scanner scan = new Scanner(s);
                           String operation = scan.next();
                           if(operation.equals("Rotation")){
                               rotate = true;
                               boolean right = scan.nextBoolean();
                               if(right){
                                   rotation = rotation + Float.parseFloat(scan.nextLine());
                               }
                               else{
                                   rotation = rotation - Float.parseFloat(scan.nextLine());
                               }
                           }
                           else if(operation.equals("Laser")){
                               laser = !laser;
                           }
                           
                           else if(operation.equals("Fire")){
                               client.send(new FireInput());
                           }
                       }
                       if(rotate){
                           client.send(new RotateInput((rotation>0),Math.abs(rotation)));
                       }
                       if(laser){
                           client.send(new LaserInput());
                       }
                   }
                   
               } catch (InterruptedException ex) {
                   Logger.getLogger(ClientMain.class.getName()).log(Level.SEVERE, null, ex);
               }
           }
           connected = true;
        }
    }
    
    public void sendConnectMsg(String nickname){
        client.send(new Connect(nickname));
    }
    
    public Node createCannon(final int id, String nickname, final boolean me, final boolean laser){
        
        final Node nodeCannon = new Node("Cannon " + id);
        nodeCannon.setUserData("nickname", nickname);
        nodeCannon.setUserData("points", points[id]);
      
        ClientMain.this.enqueue(new Callable(){
            public Object call() throws Exception{

            //Plate
            Cylinder plate = new Cylinder(30,30,CANNON_SUPPORT_RADIUS,CANNON_SUPPORT_HEIGHT,true);
            Geometry cylinderPlate = new Geometry("CylBase",plate);
            Material matPlate = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
            matPlate.setColor("Color", ColorRGBA.Black);
            cylinderPlate.setMaterial(matPlate);

            //Base
            Cylinder base = new Cylinder(30,30,CANNON_BASE_RADIUS,CANNON_BASE_HEIGHT,true);
            Geometry cylinderBase = new Geometry("Cylinder",base);
            Material matBase = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
            matBase.setColor("Color", ColorRGBA.Brown);
            cylinderBase.setMaterial(matBase);

            //Barrel
            Cylinder barrel = new Cylinder(30,30,CANNON_BARREL_RADIUS,CANNON_BARREL_LENGTH,true);
            Geometry cylinderBarrel = new Geometry("Cylinder",barrel);
            Material matBarrel = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
            matBarrel.setColor("Color", ColorRGBA.DarkGray);
            cylinderBarrel.setMaterial(matBarrel);

            //Laser
            Cylinder claser = new Cylinder(30,30,LASER_RADIUS,LASER_HEIGHT,true);
            Geometry cylinderLaser = new Geometry("Cylinder",claser);
            Material matLaser = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
            matLaser.setColor("Color", ColorRGBA.Red);
            cylinderLaser.setMaterial(matLaser);

//            if(!me){
            /** Write text on the screen (HUD) */
            otherPoints[id] = new BitmapText(guiFont, false);
            if(!me){
                otherPoints[id].setName("Nickname " + id);
                otherPoints[id].setSize(guiFont.getCharSet().getRenderedSize());
                otherPoints[id].setText((String)nodeCannon.getUserData("nickname")+ "\n" + nodeCannon.getUserData("points"));
                otherPoints[id].setLocalTranslation(cam.getScreenCoordinates(
                        new Vector3f(positions[id].x/* - guiFont.getLineWidth(cannonText.getText())/2*/,
                        positions[id].y/* + CANNON_BASE_RADIUS*3f */, positions[id].z)));
            }

            //Nodes
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

            for (int j = 0; j < CANNONBALL_NUM; j++) {
                initBullet(id,j);
            }

            nodeCannon.attachChild(nodePlate);
            nodeCannon.attachChild(nodeBase);
            nodeCannon.attachChild(nodeBarrel);
            nodeCannon.attachChild(nodeLaser);
            nodeCannon.attachChild(audio);
            
            if(!me){
                guiNode.attachChild(otherPoints[id]);
            }
            nodeGeom.attachChild(nodeCannon);

            //Position
            nodePlate.move(0,0,-CANNON_SUPPORT_HEIGHT/2 + 0.1f);
            nodeBase.rotate(0, 90*FastMath.DEG_TO_RAD, 0);
            nodeBarrel.move(0, CANNON_BASE_RADIUS,CANNON_BARREL_RADIUS);
            nodeBarrel.rotate(90*FastMath.DEG_TO_RAD, 90*FastMath.DEG_TO_RAD, 0);
            nodeLaser.move(0, LASER_HEIGHT/2, LASER_RADIUS);
            nodeLaser.rotate(90*FastMath.DEG_TO_RAD, 90*FastMath.DEG_TO_RAD, 0);
            if(!laser){
                nodeLaser.setCullHint(Spatial.CullHint.Always);
            }

            //We transform the world coordinates to local coordinates
            nodeCannon.setLocalTranslation(positions[id]);
            audios[id] = audio;
            playing = true;
            return null;
            }
        });
        return nodeCannon;
    }
    
    /**
     * Init the HUD (time and points
     */
    private void initHUD(){
        time = START_TIME;
        timerHud = new BitmapText(guiFont, false);          
        timerHud.setSize(guiFont.getCharSet().getRenderedSize()+20);      // font size
        timerHud.setColor(ColorRGBA.White);                             // font color
        timerHud.setText(timeToString());             // the text
        timerHud.setLocalTranslation(20, settings.getHeight()- 20, 0); // position
        guiNode.attachChild(timerHud);
        
        pointsHud = new BitmapText(guiFont, false);          
        pointsHud.setSize(guiFont.getCharSet().getRenderedSize()+20);      // font size
        pointsHud.setColor(ColorRGBA.White);                             // font color
        pointsHud.setText("0");             // the text
        pointsHud.setLocalTranslation(20, settings.getHeight() 
                - timerHud.getHeight() -15, 0); // position
        guiNode.attachChild(pointsHud);
        points = new int[MAX_PLAYERS];
        
        statusHud = new BitmapText(guiFont, false);          
        statusHud.setSize(guiFont.getCharSet().getRenderedSize()+20);      // font size
        statusHud.setColor(ColorRGBA.White);                             // font color
        statusHud.setText("Waiting...");             // the text
        statusHud.setLocalTranslation(settings.getWidth()/2 - (guiFont.getLineWidth("Waiting...")),
         settings.getHeight()/2 + (guiFont.getCharSet().getRenderedSize()+10)/2, 0); // position
        guiNode.attachChild(statusHud);
    }
    
    private void prepareHUD(){
        time = START_TIME;
        timerHud.setText(timeToString());
        pointsHud.setText("0");
        points = new int[MAX_PLAYERS];
    }
    
    /**
     * Transforms the time from float to String (format: s:hs)
     */
    private String timeToString(){
        String seconds = (int)time + "";
        String ms = ((int)(time*100))%100 + "";
        return seconds + ":" + ms;
    }
    
    private void initKeys() {
        inputManager.addMapping("Left",  new KeyTrigger(KeyInput.KEY_LEFT));
        inputManager.addMapping("Right",  new KeyTrigger(KeyInput.KEY_RIGHT));
        inputManager.addMapping("L",  new KeyTrigger(KeyInput.KEY_L));
        inputManager.addMapping("R",  new KeyTrigger(KeyInput.KEY_R));
        inputManager.addMapping("SHOOT",  new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        // Add the names to the action listener.
        inputManager.addListener(actionListener,"L","SHOOT","R");
        inputManager.addListener(analogListener,"Left", "Right");
    }
    
    private ActionListener actionListener = new ActionListener() {
        public void onAction(String name, boolean keyPressed, float tpf) {
          if (name.equals("L") && !keyPressed) {
              
              /* Turns the laser on/off */
              if (!laser){
                  cannons[id].getChild("Laser").setCullHint(Spatial.CullHint.Never);
              }
              else{
                  cannons[id].getChild("Laser").setCullHint(Spatial.CullHint.Always);
              }
              messageQueue.add("Laser");
              laser = !laser;
          }
          if (name.equals("SHOOT") && !keyPressed){
              /* If the game has started, shoots */
              
              messageQueue.add("Fire");
          }
        }
        
    };
    
    
     private AnalogListener analogListener = new AnalogListener() {
        public void onAnalog(String name, float value, float tpf) {
          if (name.equals("Left")) {
            float[] angles = new float[3];
            cannons[id].getLocalRotation().toAngles(angles);
                
            /* Can't make a full rotation */
            float r = value*CANNON_ROTATION_SPEED/CANNON_SUPPORT_RADIUS;
            cannons[id].rotate(0,0,r);
            messageQueue.add("Rotation " + false + " " + r);
          }
          if (name.equals("Right")) {
            float[] angles = new float[3];
            cannons[id].getLocalRotation().toAngles(angles);
                
            /* Can't make a full rotation */
            float r = value*CANNON_ROTATION_SPEED/CANNON_SUPPORT_RADIUS;
            cannons[id].rotate(0,0,-r);
            messageQueue.add("Rotation " + true + " " + r);
          }
        }
     };
     
     private void shootBall(Vector3f direction, Vector3f position, int idBall, int idPlayer){
         cannonballs[idPlayer][idBall].setCullHint(Spatial.CullHint.Never);
         cannons[idPlayer].detachChild(cannonballs[idPlayer][idBall]);
         nodeGeom.attachChild(cannonballs[idPlayer][idBall]);
         cannonballs[idPlayer][idBall].setLocalTranslation(position);
         cannonballs[idPlayer][idBall].setLocalRotation(cannons[idPlayer].getWorldRotation());
         cannonballs[idPlayer][idBall].setUserData("rot",cannons[idPlayer].getWorldRotation());
         cannonballs[idPlayer][idBall].setUserData("direction", direction);
         cannonballs[idPlayer][idBall].setUserData("shooted", true);
         audios[idPlayer].playInstance();
     }
     
     private void initBullet(int i, int j){
    
        cannonballs[i][j] = new Node("Cannonball " + i + " " + j);
        cannonballs[i][j].setUserData("shooted", false);
        Sphere sphere = new Sphere(30, 30, CANNONBALL_RADIUS);
        Geometry mark = new Geometry("BOOM!", sphere);
        
        Texture tex = assetManager.loadTexture( new TextureKey("Textures/rockT.jpg", false) );
        tex.setWrap(Texture.WrapMode.Repeat); //This should set the texture to repeat.    
        Material mark_mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mark_mat.setTexture("ColorMap", tex);
        mark.setMaterial(mark_mat);
        cannonballs[i][j].attachChild(mark);
        cannonballs[i][j].setCullHint(Spatial.CullHint.Always);
        cannons[i].attachChild(cannonballs[i][j]);
        cannonballs[i][j].move(0,CANNON_BARREL_LENGTH + CANNONBALL_RADIUS,CANNONBALL_RADIUS);
    }
     
}
