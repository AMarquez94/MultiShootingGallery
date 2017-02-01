/**
 * Author: Alejandro Marquez Ferrer
 */

package mygame;

import com.jme3.math.Vector3f;
import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;
import com.jme3.network.serializing.Serializer;

public class UtNetworking {
    
    public static final int PORT = 6000;
    
    public static void initialiseSerializables(){
        Serializer.registerClass(Connect.class);
        Serializer.registerClass(Disconnect.class);
        Serializer.registerClass(Alive.class);
        Serializer.registerClass(LaserInput.class);
        Serializer.registerClass(RotateInput.class);
        Serializer.registerClass(FireInput.class);
        Serializer.registerClass(NewClientAccepted.class);
        Serializer.registerClass(Reject.class);
        Serializer.registerClass(Disconnected.class);
        Serializer.registerClass(Prepare.class);
        Serializer.registerClass(Start.class);
        Serializer.registerClass(Activate.class);
        Serializer.registerClass(Inactivate.class);
        Serializer.registerClass(Move.class);
        Serializer.registerClass(Change.class);
        Serializer.registerClass(Award.class);
        Serializer.registerClass(Rotate.class);
        Serializer.registerClass(LaserToggled.class);
        Serializer.registerClass(Congratulate.class);
    }
    
    @Serializable
    public static class Connect extends AbstractMessage{
        
        private String n;
        
        public Connect(){
            
        }
        
        public Connect(String n){
            this.n = n;
        }
        
        public String getN(){
            return n;
        }
    }
    
    @Serializable
    public static class Disconnect extends AbstractMessage{
        
        public Disconnect(){
            
        }
    }
    
    @Serializable
    public static class Alive extends AbstractMessage{
        
        public Alive(){
            
        }
    }
    
    @Serializable
    public static class LaserInput extends AbstractMessage{
        
        public LaserInput(){
            
        }
    }
    
    @Serializable
    public static class RotateInput extends AbstractMessage{
        
        private boolean r;
        
        private float a;
        
        public RotateInput(){
            
        }
        
        public RotateInput(boolean r, float a){
            this.r = r;
            this.a = a;
        }
        
        public boolean getR(){
            return r;
        }
        
        public float getA(){
            return a;
        }
    }
    
    @Serializable
    public static class FireInput extends AbstractMessage{
        
        public FireInput(){
            
        }
    }
    
    @Serializable
    public static class NewClientAccepted extends AbstractMessage {
        
        private String n;
        private int c;
        
        public NewClientAccepted(){
            
        }
        
        public NewClientAccepted(String n, int c){
            this.n = n;
            this.c = c;
        }
        
        public String getN() {
            return n;
        }

        public int getC() {
            return c;
        }
    }
    
    @Serializable
    public static class Reject extends AbstractMessage{
        
        private String reason;
        
        public Reject(){
            
        }
        
        public Reject(String reason){
            this.reason = reason;
        }
        
        public String getReason(){
            return this.reason;
        }
    }
    
    @Serializable
    public static class Disconnected extends AbstractMessage{
        
        private int c;
        
        public Disconnected(){
            
        }
        
        public Disconnected(int c){
            this.c = c;
        }
        
        public int getC(){
            return this.c;
        }
    }
    
    @Serializable
    public static class Prepare extends AbstractMessage{
        
        private Vector3f[] pa;
        private float[][] oa;
        private String[] n;
        private boolean[] la;
        
        public Prepare(){
            
        }
        
        public Prepare(Vector3f[] pa, float[][] oa, String[] n, boolean[] la){
            this.pa = pa;
            this.oa = oa;
            this.n = n;
            this.la = la;
        }
        
        public Vector3f[] getPa(){
            return pa;
        }
        
        public float[][] getOa(){
            return oa;
        }
        
        public String[] getN(){
            return n;
        }
        
        public boolean[] getLa(){
            return la;
        }
        
    }
    
    @Serializable
    public static class Start extends AbstractMessage{
        
        public Start(){
            
        }
    }
    
    @Serializable
    public static class Activate extends AbstractMessage{

        private int i;
        private int c;
        private Vector3f p;
        private Vector3f d;
        
        public Activate(){
            
        }
        
        public Activate(int i,int c, Vector3f p, Vector3f d){
            this.i = i;
            this.c = c;
            this.p = p;
            this.d = d;
        }
        
        public int getI() {
            return i;
        }

        public int getC() {
            return c;
        }

        public Vector3f getP() {
            return p;
        }

        public Vector3f getD() {
            return d;
        }
    }
    
    @Serializable
    public static class Inactivate extends AbstractMessage{
        
        private int i;
        private int c;

        public Inactivate(){
            
        }
        
        public Inactivate(int i, int c){
            this.i = i;
            this.c = c;
        }
        
        public int getI() {
            return i;
        }

        public int getC() {
            return c;
        }
        
    }
    
    @Serializable
    public static class Move extends AbstractMessage{
        
        private int c;
        private Vector3f p;
        
        public Move(){
            
        }
        
        public Move(int c,Vector3f p){
            this.c = c;
            this.p = p;
        }

        public int getC() {
            return c;
        }

        public Vector3f getP() {
            return p;
        }
    }
    
    @Serializable
    public static class Change extends AbstractMessage{
        
        private int i;
        private int c;
        private Vector3f p;
        private Vector3f d;
        
        public Change(){
            
        }
        
        public Change(int i, int c, Vector3f p, Vector3f d){
            this.i = i;
            this.c = c;
            this.p = p;
            this.d = d;
        }

        public int getI() {
            return i;
        }

        public int getC() {
            return c;
        }

        public Vector3f getP() {
            return p;
        }

        public Vector3f getD() {
            return d;
        }
    }
    
    @Serializable
    public static class Award extends AbstractMessage{
        
        private int i;
        private int s;
        
        public Award(){
            
        }
        
        public Award(int i, int s){
            this.i = i;
            this.s = s;
        }

        public int getI() {
            return i;
        }

        public int getS() {
            return s;
        }
        
    }
    
    @Serializable
    public static class Rotate extends AbstractMessage{
        
        private int i; 
        private boolean b;
        private float a;
        
        public Rotate(){
            
        }
        
        public Rotate(int i, boolean b, float a){
            this.i = i;
            this.a = a;
            this.b = b;
        }

        public int getI() {
            return i;
        }

        public boolean isB() {
            return b;
        }

        public float getA() {
            return a;
        }
        
    }
    
    @Serializable
    public static class LaserToggled extends AbstractMessage{
        
        private int i;
        private boolean b;
        
        public LaserToggled(){
            
        }
        
        public LaserToggled(int i, boolean b){
            this.i = i;
            this.b = b;
        }

        public int getI() {
            return i;
        }

        public boolean isB() {
            return b;
        }
        
    }
    
    @Serializable
    public static class Congratulate extends AbstractMessage{
        
        private Integer[] w;
        private int n;
        
        public Congratulate(){
            
        }
        public Congratulate(Integer[] w, int n){
            this.w = w;
            this.n = n;
        }

        public Integer[] getW() {
            return w;
        }

        public int getN() {
            return n;
        }
    }
}
