package base_SOM_Objects.som_geom.geom_utils.geom_threading.base;

import java.util.concurrent.Callable;

import base_Math_Objects.MyMathUtils;
import base_Math_Objects.vectorObjs.doubles.myPoint;
import base_Math_Objects.vectorObjs.doubles.myVector;
import base_Math_Objects.vectorObjs.floats.myPointf;
import base_Math_Objects.vectorObjs.floats.myVectorf;
import base_SOM_Objects.som_geom.SOM_GeomMapManager;
import base_Utils_Objects.io.messaging.MessageObject;

public abstract class SOM_GeomCallable implements Callable<Boolean> {
    /**
     * owning map manager
     */
    protected final SOM_GeomMapManager mapMgr;

    protected final String dataType;

    /**
     * message object
     */
    protected final MessageObject msgObj;
    /**
     * start and end index in array of data, thread index, # of 
     */
    protected final int stIdx, endIdx, thdIDX;    
    /**
     * monitor progress
     */
    private final int progressBnd;
    private static final float progAmt = .2f;
    protected double progress = -progAmt;

    /**
     * coordinate bounds in world 
     *         first idx : 0 is min; 1 is diff
     *         2nd idx : 0 is x, 1 is y, 2 is z
     */
    protected final float[][] worldBounds;    

    
    public SOM_GeomCallable(SOM_GeomMapManager _mapMgr, int _stIdx, int _endIdx, int _thdIdx) {
        mapMgr=_mapMgr;
        msgObj = MessageObject.getInstance();
        dataType=mapMgr.getGeomObjTypeName();
        worldBounds = mapMgr.getWorldBounds();        
        
        stIdx = _stIdx;
        endIdx = _endIdx;
        thdIDX= _thdIdx;
        int diff = (int) ((endIdx-stIdx) * progAmt);
        progressBnd = diff < 1 ? 1 : diff;
    }

    
    protected final void incrProgress(int idx, String task) {
        if(((idx-stIdx) % progressBnd) == 0) {        
            progress += progAmt;    
            msgObj.dispInfoMessage("SOM_GeomCallable","incrProgress::thdIDX=" + String.format("%02d", thdIDX)+" ", "Progress performing " + task +" at : " + String.format("%.2f",progress));
        }
        if(progress > 1.0) {progress = 1.0;}
    }
    public final double getProgress() {    return progress;}
    
    /**
     * determine how many work elements should be assigned per thread 
     * @param numVals total number of work elements to execute
     * @param numThds total number of threads available
     * @return number of work elements per thread to assign
     */
    public final int calcNumPerThd(int numVals, int numThds) {    return (int) ((numVals -1)/(1.0*numThds)) + 1;    }//calcNumPerThd
    
    protected myPointf getRandPointInBounds_2D() {
        myPointf x = new myPointf( 
                ( MyMathUtils.randomFloat() *worldBounds[1][0])+worldBounds[0][0], 
                ( MyMathUtils.randomFloat() *worldBounds[1][1])+worldBounds[0][1],0);
        return x;
    }
    

    protected myPointf getRandPointInBounds_3D() {
        myPointf x = new myPointf( 
                ( MyMathUtils.randomFloat() *worldBounds[1][0])+worldBounds[0][0], 
                ( MyMathUtils.randomFloat() *worldBounds[1][1])+worldBounds[0][1],
                ( MyMathUtils.randomFloat() *worldBounds[1][2])+worldBounds[0][2]);
        return x;
    }
    
    protected myPointf getRandPointInBounds_3D(float bnd) {
        float tbnd = 2.0f*bnd;
        myPointf x = new myPointf( 
                ( MyMathUtils.randomFloat() *(worldBounds[1][0]-tbnd))+worldBounds[0][0]+bnd, 
                ( MyMathUtils.randomFloat() *(worldBounds[1][1]-tbnd))+worldBounds[0][1]+bnd,
                ( MyMathUtils.randomFloat() *(worldBounds[1][2]-tbnd))+worldBounds[0][2]+bnd);
        return x;
    }
    
    protected myPoint getRandPointInBounds_2D_Double() {
        myPoint x = new myPoint( 
                ( MyMathUtils.randomDouble() *worldBounds[1][0])+worldBounds[0][0], 
                ( MyMathUtils.randomDouble() *worldBounds[1][1])+worldBounds[0][1],0);
        return x;
    }
    

    protected myPoint getRandPointInBounds_3D_Double() {
        myPoint x = new myPoint( 
                ( MyMathUtils.randomDouble() *worldBounds[1][0])+worldBounds[0][0], 
                ( MyMathUtils.randomDouble() *worldBounds[1][1])+worldBounds[0][1],
                ( MyMathUtils.randomDouble() *worldBounds[1][2])+worldBounds[0][2]);
        return x;
    }
    
    protected myPoint getRandPointInBounds_3D_Double(double bnd) {
        double tbnd = 2.0*bnd;
        myPoint x = new myPoint( 
                ( MyMathUtils.randomDouble() *(worldBounds[1][0]-tbnd))+worldBounds[0][0]+bnd, 
                ( MyMathUtils.randomDouble() *(worldBounds[1][1]-tbnd))+worldBounds[0][1]+bnd,
                ( MyMathUtils.randomDouble() *(worldBounds[1][2]-tbnd))+worldBounds[0][2]+bnd);
        return x;
    }
    
    /**
     * find an array of 3 non-colinear points 
     * @return array of 3 non-colinear points
     */
    protected myPointf[] getRandPlanePoints() {
        myPointf a = getRandPointInBounds_3D(),b;
        do { b = getRandPointInBounds_3D();} while (a.equals(b));        //make sure a != b
        myPointf c;
        myVectorf ab = new myVectorf(a,b), ac;
        ab._normalize();
        boolean eqFail = false, dotProdFail = false;
        do {
            c = getRandPointInBounds_3D();
            eqFail = (a.equals(c)) || (b.equals(c));
            if(eqFail) {continue;}
            ac = new myVectorf(a,c);
            ac._normalize();
            dotProdFail = (Math.abs(ab._dot(ac))==1.0f);
        } while (eqFail || dotProdFail);
        myPointf[] planePts = new myPointf[] {a,b,c};        
        return planePts;
    }
    
    /**
     * find an array of 3 non-colinear points
     * @return array of 3 non-colinear points
     */
    protected myPoint[] getRandPlanePoints_Double() {
        myPoint a = getRandPointInBounds_3D_Double(), b;
        do{b= getRandPointInBounds_3D_Double();} while (a.equals(b)); //make sure a != b
        myPoint c;
        myVector ab = new myVector(a,b), ac;
        ab._normalize();
        boolean eqFail = false, dotProdFail = false;
        do {
            c = getRandPointInBounds_3D_Double();
            eqFail = (a.equals(c)) || (b.equals(c));
            if(eqFail) {continue;}
            ac = new myVector(a,c);
            ac._normalize();
            dotProdFail = (Math.abs(ab._dot(ac))==1.0f);
        } while (eqFail || dotProdFail);
        myPoint[] planePts = new myPoint[] {a,b,c};        
        return planePts;
    }
    
    /**
     * get random unit vector
     * @return
     */
    protected myVectorf getRandNormal_3D() {
        myVectorf x = new myVectorf( 
                ( MyMathUtils.randomFloat() *2.0f)-1.0f, 
                ( MyMathUtils.randomFloat() *2.0f)-1.0f,
                ( MyMathUtils.randomFloat() *2.0f)-1.0f);        
        return x._normalized();
    }
    
    protected myVector getRandNormal_3D_Double() {
        myVector x = new myVector( 
                ( MyMathUtils.randomDouble() *2.0f)-1.0f, 
                ( MyMathUtils.randomDouble() *2.0f)-1.0f,
                ( MyMathUtils.randomDouble() *2.0f)-1.0f);        
        return x._normalized();
    }
    
//    /**
//     * get uniformly random position on sphere surface with passed radius and center
//     * @param rad
//     * @param ctr
//     * @return
//     */
//    protected final myPointf getRandPosOnSphere(double rad, myPointf ctr){
//        myPointf pos = new myPointf();
//        double     cosTheta = ThreadLocalRandom.current().nextDouble(-1,1), sinTheta =  Math.sin(Math.acos(cosTheta)),
//                phi = ThreadLocalRandom.current().nextDouble(0,MyMathUtils.TWO_PI_F);
//        pos.set(sinTheta * Math.cos(phi), sinTheta * Math.sin(phi),cosTheta);
//        pos._mult((float) rad);
//        pos._add(ctr);
//        return pos;
//    }//getRandPosOnSphere
//    
//    protected final myPoint getRandPosOnSphere_Double(double rad, myPoint ctr){
//        myPoint pos = new myPoint();
//        double     cosTheta = ThreadLocalRandom.current().nextDouble(-1,1), sinTheta =  Math.sin(Math.acos(cosTheta)),
//                phi = ThreadLocalRandom.current().nextDouble(0,MyMathUtils.TWO_PI_F);
//        pos.set(sinTheta * Math.cos(phi), sinTheta * Math.sin(phi),cosTheta);
//        pos._mult((float) rad);
//        pos._add(ctr);
//        return pos;
//    }//getRandPosOnSphere
    
    /**
     * return 4 points that describe a sphere uniquely - no trio of points can be colinear, and the 4 points cannot be co planar
     * get 3 non-colinear points, find 4th by finding normal of plane 3 points describe
     * @param rad radius of desired sphere
     * @param ctr center of desired sphere
     */    
    protected final myPointf[] getRandSpherePoints(float rad, myPointf ctr){
        myPointf a = MyMathUtils.getRandPosOnSphere(rad, ctr),b;
        do { b = MyMathUtils.getRandPosOnSphere(rad, ctr);} while (a.equals(b));
        myPointf c,d;
        myVectorf ab = new myVectorf(a,b), ac = myVectorf.ZEROVEC, ad;
        ab._normalize();
        int iter = 0;
        boolean eqFail = false, dotProdFail = false;
        do {
            ++iter;
            c = MyMathUtils.getRandPosOnSphere(rad, ctr);
            eqFail = (a.equals(c)) || (b.equals(c));
            if(eqFail) {continue;}
            ac = new myVectorf(a,c);
            ac._normalize();
            dotProdFail = (Math.abs(ab._dot(ac))==1.0f);
        } while (eqFail || dotProdFail);
        //4th point needs to be non-coplanar - will guarantee that 
        //it is also not collinear with any pair of existing points
        //normal to abc plane
        myVectorf planeNorm = ab._cross(ac)._normalize();
        //now find d so that it does not line in plane of abc - vector from ab
        eqFail = false; dotProdFail = false;
        do {
            ++iter;
            d = MyMathUtils.getRandPosOnSphere(rad, ctr);
            eqFail = a.equals(d) || b.equals(d) || c.equals(d);
            if(eqFail) {continue;}
            ad = new myVectorf(a,d);
            ad._normalize();
            dotProdFail = (ad._dot(planeNorm) == 0.0f);
        } while (eqFail || dotProdFail);//if 0 then in plane (ortho to normal)
        
        myPointf[] spherePts = new myPointf[] {a,b,c,d};        
        if(iter>2) {//check this doesn't take too long - most of the time should never take more than a single iteration through each do loop
            msgObj.dispInfoMessage("SOM_GeomObjBuilder","getRandSpherePoints::thdIDX=" + String.format("%02d", thdIDX)+" ", "Took Longer than 2 iterations to generate 4 points for sphere : " + iter);
            
        }
        return spherePts;
    }
    
    protected final myPoint[] getRandSpherePoints_Double(double rad, myPoint ctr){
        myPoint a = MyMathUtils.getRandPosOnSphereDouble(rad, ctr), b;
        do{b= MyMathUtils.getRandPosOnSphereDouble(rad, ctr);} while (a.equals(b));
        myPoint c,d;
        myVector ab = new myVector(a,b), ac = myVector.ZEROVEC, ad;
        ab._normalize();
        int iter = 0;
        boolean eqFail = false, dotProdFail = false;
        do {
            ++iter;
            c = MyMathUtils.getRandPosOnSphereDouble(rad, ctr);
            eqFail = (a.equals(c)) || (b.equals(c));
            if(eqFail) {continue;}
            ac = new myVector(a,c);
            ac._normalize();
            dotProdFail = (Math.abs(ab._dot(ac))==1.0f);
        } while (eqFail || dotProdFail);
        //4th point needs to be non-coplanar - will guarantee that 
        //it is also not collinear with any pair of existing points
        //normal to abc plane
        myVector planeNorm = ab._cross(ac)._normalize();
        //now find d so that it does not line in plane of abc - vector from ab
        eqFail = false; dotProdFail = false;
        do {
            ++iter;
            d = MyMathUtils.getRandPosOnSphereDouble(rad, ctr);
            eqFail = a.equals(d) || b.equals(d) || c.equals(d);
            if(eqFail) {continue;}
            ad = new myVector(a,d);
            ad._normalize();
            dotProdFail = (ad._dot(planeNorm) == 0.0f);
        } while (eqFail || dotProdFail);//if 0 then in plane (ortho to normal)
        
        myPoint[] spherePts = new myPoint[] {a,b,c,d};        
        if(iter>2) {
            msgObj.dispInfoMessage("SOM_GeomObjBuilder","getRandSpherePoints_Double::thdIDX=" + String.format("%02d", thdIDX)+" ", "Took Longer than 2 iterations to generate 4 points for sphere : " + iter);            
        }
        return spherePts;
    }
    
    

}// class SOM_GeomCallable 
