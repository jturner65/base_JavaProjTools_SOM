package base_SOM_Objects.som_geom.geom_examples;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.TreeMap;

import base_Math_Objects.MyMathUtils;
import base_Math_Objects.vectorObjs.floats.myPointf;
import base_Math_Objects.vectorObjs.floats.myVectorf;
import base_Render_Interface.IRenderInterface;
import base_SOM_Objects.som_examples.base.SOM_Example;
import base_SOM_Objects.som_examples.enums.SOM_ExDataType;
import base_SOM_Objects.som_geom.SOM_GeomMapManager;
import base_SOM_Objects.som_geom.geom_UI.SOM_AnimWorldWin;
import base_SOM_Objects.som_geom.geom_utils.geom_objs.SOM_GeomObjDrawType;
import base_SOM_Objects.som_geom.geom_utils.geom_objs.SOM_GeomObjSamples;
import base_SOM_Objects.som_geom.geom_utils.geom_objs.SOM_GeomObjTypes;
import base_SOM_Objects.som_geom.geom_utils.geom_objs.SOM_GeomSamplePointf;
import base_UI_Objects.windowUI.base.Base_DispWindow;

/**
 * class to instance the base functionality of a geometric object represented by 
 * some parameters and also by samples for use in training and consuming a SOM
 * 
 * @author john
 */
public abstract class SOM_GeomObj extends SOM_Example  {
    /**
     * Integer object ID specific to instancing class objects
     */
    private int GeomObj_ID;

    /**
     * label to display, based on ID and object type
     */
    protected String dispLabel;
    
    /**
     * location-based and random color arrays, for display
     */
    public int[] locClrAra;
    public int[] rndClrAra;
    public int[] labelClrAra;
    
    /**
     * state flags
     */
    private int[] geomStFlags;                    //state flags for this instance - bits in array holding relevant process info
    public static final int
            debugIDX                         = 0,        //draw this sphere's sample points
            is3dIDX                            = 1,        //this object is in 3d or 2d
            buildSampleObjIDX                = 2,        //should build sample object
            buildVisRepOfObjIDX                = 3;        //should prebuild a physical representation of object, if supported
    public static final int numgeomStFlags = 4;    
        
    /**
     * type of object (geometric)
     */
    private SOM_GeomObjTypes objGeomType;
        
    /**
     * list of original object point samples and their owning objects making up this example - 
     * these will be used to determine the classes for this object, to be passed to bmu map node for this example 
     */
    protected final SOM_GeomSamplePointf[] geomSrcSamples;    
    /**
     * given source points that make up this object
     */
    private final SOM_GeomSamplePointf[] srcPts;

    /**
     * construction managing sample points on the surface of this geom object
     */
    private SOM_GeomObjSamples objSamples;

    /**
     * all class ID's this object belongs to - in this case, the IDs of the geomSrcSamples samples making up this object
     */
    protected HashSet<Integer> classIDs;
    
    /**
     * all category ID's this object belongs to
     */
    protected HashSet<Integer> categoryIDs;
    
    /**
     * tag to denote the beginning/end of a source point record in csv file
     */
    private static final String srcPtTag = "SRCPT,";

    /**
     * distance to draw point label from point
     */
    public static final float lblDist = 2.0f;
    
    /**
     * build a geometry-based training/validation example for the SOM
     * @param _mapMgr : owning map manager
     * @param _exType : example data type (ML) (training, testing, validation, etc)
     * @param _id  : the unique ID info for this training example
     * @param _srcSmpls : the source points and their owning SOM_GeomObj objects that built this sample (if null then these are the points that make this object)
     * @param _worldBounds : bounds in world for valid values for this object
     * @param _GeoType : geometric object type
     * @param _numSmplPts : # of sample points to build for this object
     */    
    public SOM_GeomObj(SOM_GeomMapManager _mapMgr, SOM_ExDataType _exType, String _id, SOM_GeomSamplePointf[] _srcSmpls, SOM_GeomObjTypes _GeoType, int _numSmplPts,  boolean _is3D, boolean _shouldBuildSamples, boolean _shouldBuildVisRep) {
        super(_mapMgr, _exType,_id);
        _ctorInit(incrID(), _GeoType, _is3D, _shouldBuildSamples, _shouldBuildVisRep);
        
        geomSrcSamples = _srcSmpls;        
        srcPts = _initAndBuildSrcPts(geomSrcSamples);
        
        objSamples = (_shouldBuildSamples ? new SOM_GeomObjSamples(this, geomSrcSamples.length, rndClrAra, labelClrAra) : null);        

        //Object specific init
        initObjVals(_numSmplPts);
    }//ctor
    
    /**
     * building objects from CSV string - CURRENTLY ALWAYS BUILDS SAMPLES
     * @param _mapMgr
     * @param _animWin
     * @param _exType
     * @param _oid
     * @param _csvDat
     * @param _worldBounds
     * @param _GeoType
     */
    public SOM_GeomObj(SOM_GeomMapManager _mapMgr, SOM_ExDataType _exType, String _oid, String _csvDat, SOM_GeomObjTypes _GeoType, int _numSrcPts, boolean _is3D) {
        super(_mapMgr, _exType, _oid);
        _ctorInit(Integer.parseInt(_csvDat.trim().split(",")[1].trim()),_GeoType, _is3D, true, true);
        
        //only data needed to be saved
        srcPts = _buildSrcPtsFromCSVString(_numSrcPts, _csvDat);    
        //build geomSrcSamples from srcPts 
        geomSrcSamples = new SOM_GeomSamplePointf[getSrcPts().length];
        for(int i=0;i<geomSrcSamples.length;++i) {geomSrcSamples[i] =  new SOM_GeomSamplePointf(getSrcPts()[i], dispLabel+"_gen_pt_"+i, this);}

        objSamples = new SOM_GeomObjSamples(this, geomSrcSamples.length, rndClrAra, labelClrAra);
        //Object specific init for CSV
        initObjValsFromCSV(_csvDat);
    }
    
    /**
     * ctor to build object corresponding to bmu geometric object
     * @param _mapMgr
     * @param _mapNode Map Node
     */    
    public SOM_GeomObj(SOM_GeomMapManager _mapMgr, SOM_GeomMapNode _mapNode, SOM_GeomObjTypes _GeoType, boolean _is3D) {
        super(_mapMgr, SOM_ExDataType.MapNode,_GeoType.toString()+"_"+_mapNode.OID);
        _ctorInit(incrID(), _GeoType, _is3D, true, true);

        float[] mapFtrsAra = _mapNode.getRawFtrs();    
        srcPts = buildSrcPtsFromBMUMapNodeFtrs(mapFtrsAra, dispLabel);
        //_DBG_DispObjStats(mapFtrsAra,"map node ctor", "Map Node:  "+ _mapNode.OID);
        //build geomSrcSamples from srcPts 
        geomSrcSamples = new SOM_GeomSamplePointf[getSrcPts().length];
        for(int i=0;i<geomSrcSamples.length;++i) {geomSrcSamples[i] =  new SOM_GeomSamplePointf(getSrcPts()[i], dispLabel+"_gen_pt_"+i, this);}
        
        objSamples = new SOM_GeomObjSamples(this, geomSrcSamples.length, rndClrAra, labelClrAra);
        
        //Object type-specific init for map node
        initObjValsForMapNode(_mapNode);
    }
    
    public SOM_GeomObj(SOM_GeomObj _otr) {
        super(_otr);
        GeomObj_ID = _otr.GeomObj_ID;
        dispLabel = _otr.dispLabel;
        //animWin = _otr.animWin;
        //objGeomType = _otr.objGeomType;
        classIDs = _otr.classIDs;
        categoryIDs = _otr.categoryIDs;
        geomSrcSamples = _otr.geomSrcSamples;

        geomStFlags = _otr.geomStFlags;
        srcPts = _otr.getSrcPts();
        rndClrAra = _otr.rndClrAra;
        locClrAra = _otr.locClrAra;
        labelClrAra = _otr.labelClrAra;
        objSamples = _otr.objSamples;
    }//copy ctor

    protected final void _DBG_DispObjStats(float[] mapFtrsAra, String _callingMethod, String _nodeIdDispt) {
        String ftrs = "";
        for(int i=0;i<mapFtrsAra.length;++i) {ftrs += String.format("%.8f, ", mapFtrsAra[i]);}
        String tmpDisp = _nodeIdDispt;
        for(SOM_GeomSamplePointf pt : srcPts) {    tmpDisp += " | " + pt.toStrBrf();    }
        tmpDisp += " | Ftrs :"+ftrs;
        msgObj.dispInfoMessage("SOM_GeomObj::"+GeomObj_ID, _callingMethod, tmpDisp);
    }

    /**
     * Object-type specific ctor init
     * @param _numSmplPts # of sample points to derive
     */
    protected abstract void initObjVals(int _numSmplPts);
    
    /**
     * Object-type-specific ctor init for CSV-derived nodes
     * @param _csvDat string of CSV data
     */
    protected abstract void initObjValsFromCSV(String _csvDat);
    
    /**
     * Object-type-specific ctor init for map node-based object 
     * @param _mapNode
     */
    protected abstract void initObjValsForMapNode(SOM_GeomMapNode _mapNode);
    
    private void _ctorInit(int _geoObj_ID, SOM_GeomObjTypes _GeoType, boolean _is3D, boolean _shouldBuildSamples, boolean _shouldBuildVisRep) {
        initGeomFlags();
        setGeomFlag(is3dIDX, _is3D);
        setGeomFlag(buildSampleObjIDX, _shouldBuildSamples);
        setGeomFlag(buildVisRepOfObjIDX, _shouldBuildVisRep);
        objGeomType = _GeoType;        
        GeomObj_ID = _geoObj_ID;        //sets GeomObj_ID to be count of instancing class objs
        dispLabel = objGeomType.toString() + "_"+GeomObj_ID;
        
        classIDs = new HashSet<Integer>();
        categoryIDs = new HashSet<Integer>();        

        labelClrAra = getGeomFlag(is3dIDX)? new int[] {0,0,0,255} : new int[] {255,255,255,255};
        rndClrAra = MyMathUtils.randomIntClrAra();    
    }//_ctorInit    
    
    /**
     * initialize object's ID
     */
    protected abstract int incrID();
    
    /**
     * initialize object's ID, and build SOM_GeomSamplePointf array from the source samples used to derive this object
     * @param _srcSmpls
     * @return
     */
    private final SOM_GeomSamplePointf[] _initAndBuildSrcPts(SOM_GeomSamplePointf[] _srcSmpls) {
        SOM_GeomSamplePointf[] ptAra = new SOM_GeomSamplePointf[_srcSmpls.length];
        for(int i=0;i<_srcSmpls.length;++i) {
            if(_srcSmpls[i].getObj() == null) {_srcSmpls[i].setObj(this);}
            ptAra[i]=new SOM_GeomSamplePointf(_srcSmpls[i], dispLabel+"_SrcPt_"+i, this);
        }
        return ptAra;
    }//_initAndBuildSrcPts
    
    
    /**
     * build new point location for color, increasing the distance from the origin to provide more diversity
     * @param planeOrigin
     * @param norm
     * @return
     */
    protected myPointf buildLocForColor(myPointf pt, myVectorf norm) {        
        float distFromOrigin = myPointf._dist(pt, myPointf.ZEROPT) + 1.0f;
        float mod = (float) Math.pow(distFromOrigin, 1.2);
        return new myPointf(myPointf.ZEROPT, mod, norm);
    }
    
    /**
     * column names of rawDescrForCSV data (preprocessed data)
     * @return
     */
    @Override
    public final String getRawDescColNamesForCSV() {
        String res = "OID,GeomObj_ID,TAG,";    
        for(int i=0;i<getSrcPts().length;++i) {res += getSrcPts()[i].toCSVHeaderStr()+"TAG,";}
        res +=getRawDescColNamesForCSV_Indiv();    
        if(null!=objSamples) {res += objSamples.getRawDescColNamesForCSV();}
        return res;
    }
    /**
     * instance-class specific format for header column names
     * @return
     */
    protected abstract String getRawDescColNamesForCSV_Indiv();
    
    /**
     * required info for this example to build feature data - use this so we don't have to reload and rebuilt from data every time
     * @return
     */
    @Override
    public final String getPreProcDescrForCSV() {
        //first have example id, then have geom obj type id
        String res = ""+OID+","+ GeomObj_ID+"," + srcPtTag;
        //only need to save srcPts
        for(int i=0;i<getSrcPts().length;++i) {res += getSrcPts()[i].toCSVStr() + srcPtTag;}            
        res += getPreProcDescrForCSV_Indiv();
        if(null!=objSamples) {res += objSamples.getPreProcDescrForCSV();}
        return res;
    }
    
    /**
     * instance-class specific format for header column names
     * @return
     */
    protected abstract String getPreProcDescrForCSV_Indiv();
    
    /**
     * build a specified # of sample points for this object
     * @param _numSmplPts
     */
    protected final void buildLocClrInitObjAndSamples(myPointf _locForClr, int _numSmplPts) {
        locClrAra = getClrFromWorldLoc(_locForClr);        
        buildSmplSetAndSmplPShapes(_numSmplPts);    
    }//buildLocClrInitObjAndSamples
    
    /**
     * build array of source points from csv data
     * @param _numSrcSmpls
     * @param _csvDat
     * @return
     */
    private final SOM_GeomSamplePointf[] _buildSrcPtsFromCSVString(int _numSrcSmpls, String _csvDat) {
        SOM_GeomSamplePointf[] res = new SOM_GeomSamplePointf[_numSrcSmpls];
        String[] strDatAra = _csvDat.split(srcPtTag),parseStrAra;
        //from idx 1 to end is src point csv strs
        for(int i=0;i<res.length;++i) {    
            parseStrAra = strDatAra[i+1].trim().split(",");            
            res[i] = new SOM_GeomSamplePointf(parseStrAra, this);}
        return res;        
    }//_buildSrcPtsFromCSVString
    
    /**
     * build an array of source points from the characteristic features of the source map node
     */
    protected abstract SOM_GeomSamplePointf[] buildSrcPtsFromBMUMapNodeFtrs(float[] mapFtrs, String _dispLabel);
    
    
    /**
     * build location color for this object and build samples from csv string listing
     * @param _locForClr
     * @param _csvStr
     */
    protected final void buildLocClrAndSamplesFromCSVStr(myPointf _locForClr, String _csvStr) {
        locClrAra = getClrFromWorldLoc(_locForClr);            
        if(null!=objSamples) {objSamples.buildSampleSetAndPShapesFromCSVStr(Base_DispWindow.ri, _csvStr);}
        //else {msgObj.dispWarningMessage("SOM_GeomObj::"+GeomObj_ID, "buildLocClrAndSamplesFromCSVStr", "Attempting to rebuild samples from CSV for obj "+dispLabel +" when no objSamples Object exists");}
    }
    
    /**
     * build pshape to hold samples, to speed up rendering
     * @param _numSmplPts
     */
    public final void buildSmplSetAndSmplPShapes(int _numSmplPts) {
        if(null!=objSamples) {objSamples.buildSampleSetAndPShapes(Base_DispWindow.ri,_numSmplPts);} 
        //else {msgObj.dispWarningMessage("SOM_GeomObj::"+GeomObj_ID, "buildSmplSetAndSmplPShapes", "Attempting to rebuild samples for obj "+dispLabel +" when no objSamples Object exists");}
    }//buildSampleSet
    

    /**
     * build orthonormal basis from the passed normal (unit)
     * @param tmpNorm : normal 
     * @return ortho basis
     */
    protected myVectorf[] buildBasisVecs(myVectorf tmpNorm) {
        myVectorf[] basisVecs = new myVectorf[3];
        //build basis vectors
        basisVecs[0] = tmpNorm;
        if(basisVecs[0]._dot(myVectorf.FORWARD) == 1.0f) {//if planeNorm is in x direction means plane is y-z, so y axis will work as basis
            basisVecs[1] = new myVectorf(myVectorf.RIGHT);
        } else {
            basisVecs[1] = basisVecs[0]._cross(myVectorf.FORWARD);
            basisVecs[1]._normalize();
        }
        basisVecs[2] = basisVecs[1]._cross(basisVecs[0]);
        basisVecs[2]._normalize();        
        return basisVecs;
    }
        
    /**
     * return 4 points that describe a sphere uniquely - no trio of points can be collinear, and the 4 points cannot be co planar
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
            msgObj.dispInfoMessage("Geom_SphereSOMExample","getRandSpherePoints", "Took Longer than 2 iterations to generate 4 points for sphere : " + iter);
            
        }
        return spherePts;
    }//getRandSpherePoints
    
    /**
     * return a random point on this object
     */
    public abstract myPointf getRandPointOnObj();
    
    @Override
    protected final void buildFeaturesMap() {
        clearFtrMap(unNormFtrMapTypeKey);//
        buildFeaturesMap_Indiv();
        //find magnitude of features
        ftrVecMag = 0;
        Float ftrSqrMag = 0.0f;
        int ftrSize = ftrMaps[unNormFtrMapTypeKey].size();
        for(int i=0;i<ftrSize;++i) {
            Float val = ftrMaps[unNormFtrMapTypeKey].get(i);
            ftrSqrMag += val * val;
            ((SOM_GeomMapManager)mapMgr).trainDatObjBnds.checkValInBnds(i, val);            
        }
        ftrVecMag = (float)Math.sqrt(ftrSqrMag);

    }//buildFeaturesMap
    
    protected abstract void buildFeaturesMap_Indiv();
            
    /**
     * Return all the relevant classes found in this example/that this example belongs 
     * to, for class segment calculation
     * @return class IDs present in this example
     */    
    @Override
    protected final HashSet<Integer> getAllClassIDsForClsSegmentCalc() {        return classIDs;}

    /**
     * Return all the relevant categories found in this example or that this example 
     * belongs to, for category segment membership calculation
     * @return category IDs present in this example
     */    
    @Override
    protected final HashSet<Integer> getAllCategoryIDsForCatSegmentCalc() {        return categoryIDs;}
    
    @Override
    protected final void buildPerFtrNormMap() {    //build standardized features
        calcPerFtrNormVector(ftrMaps[unNormFtrMapTypeKey], ftrMaps[perFtrNormMapTypeKey], mapMgr.getTrainFtrMins(),mapMgr.getTrainFtrDiffs());
        setFlag(perFtrNormBuiltIDX, true);
    }    
    
    @Override
    protected final void buildAllNonZeroFtrIDXs() {
        allNonZeroFtrIDXs = new ArrayList<Integer>();
        //all idxs should be considered "non-zero", even those with zero value, since these examples are dense
        for(int i=0;i<numFtrs;++i) {allNonZeroFtrIDXs.add(i);}        
    }

    @Override
    // any processing that must occur once all constituent data records are added to
    // this example - must be called externally, BEFORE ftr vec is built
    public final void finalizeBuildBeforeFtrCalc() {    }

    @Override
    //called after all features of this kind of object are built
    public final void postFtrVecBuild() {}
    @Override
    //this is called after an individual example's features are built
    protected final void _buildFeatureVectorEnd_Priv() {}

    @Override
    protected final void setIsTrainingDataIDX_Priv() {
        exampleDataType = isTrainingData ? SOM_ExDataType.Training : SOM_ExDataType.Testing;
        setMapNodeClrs(mapMgr.getClrFillStrkTxtAra(exampleDataType));
    }
    
    /**
     * return the appropriate string value for the dense training data - should be numeric key value to save in lrn or csv dense file
     * Strafford will always use sparse data so this doesn't matter
     * @return
     */
    @Override
    protected String getDenseTrainDataKey() {
        return String.format("%09d", testTrainDataIDX);
    }

    /**
     *  this will build the comparison feature vector array that is used as the comparison vector 
     *  in distance measurements - for most cases this will just be a copy of the ftr vector array
     *  but in some instances, there might be an alternate vector to be used to handle when, for 
     *  example, an example has ftrs that do not appear on the map
     * @param _ignored : ignored this is ignored for this kind of example
     */
    @Override
    public final void buildCompFtrVector(float _ignored) {compFtrMaps = ftrMaps;}
    
    @Override
    protected final String dispFtrVal(TreeMap<Integer, Float> ftrs, Integer idx) {
        Float ftr = ftrs.get(idx);
        return "idx : " + idx + " | val : " + String.format("%1.4g",  ftr) + " || ";
    }
        
    ////////////////////////
    // draw functions
    protected static final int lBnd = 40, uBnd = 255, rndBnd = uBnd - lBnd;    
    /**
     * convert a world location within the bounded cube region to be a 4-int color array. Override for 2d objects
     */
    public int[] getClrFromWorldLoc(myPointf pt){return getClrFromWorldLoc_3D(pt,((SOM_GeomMapManager) mapMgr).getWorldBounds());}//getClrFromWorldLoc
    
    /**
     * 2 d get world loc color
     * @param pt
     * @param _worldBounds
     * @return
     */
    public final int[] getClrFromWorldLoc_2D(myPointf pt, myVectorf _dir, float[][] _worldBounds) {        //for 2d world bounds is idx 0 == 0,1 for min, diff, idx 1 == 0,1 for x,y
        //use x/y value as base color value for point
        float rs = getBoundedClrVal(pt.x, 0, _worldBounds), gs = getBoundedClrVal(pt.y, 1, _worldBounds);//(pt.y-_worldBounds[0][1])/_worldBounds[1][1];        
        //dir is normalized, find angle for blue value
        float bs = (_dir.x + 1)/2.0f;
        return new int[]{(int)(rndBnd*rs)+lBnd,(int)(rndBnd*gs)+lBnd,(int)(rndBnd*bs)+lBnd,255};
    }
    
    private float getBoundedClrVal(float val, int boundIDX,float[][] _worldBounds) {
        return (val -_worldBounds[0][boundIDX])/_worldBounds[1][boundIDX];
        //return (rs > 1.0 ? 1.0f : rs < 0.0f  ? 0.0f : rs);
    }
    
    /**
     * 3 d get world loc color
     * @param pt point
     * @param _worldBounds
     * @return
     */
    public final int[] getClrFromWorldLoc_3D(myPointf pt,float[][] _worldBounds) {        
        return new int[]{(int)(255*getBoundedClrVal(pt.x, 0, _worldBounds)),(int)(255*getBoundedClrVal(pt.y, 1, _worldBounds)),(int)(255*getBoundedClrVal(pt.z, 2, _worldBounds)),255};
    }

    /**
     * draw entire object this class represents, using location as color or using randomly assigned color
     * @param ri
     */    
        
    public final void drawMeClrRnd(IRenderInterface ri) {
        ri.pushMatState();        
        ri.setFill(rndClrAra,255);
        ri.setStroke(rndClrAra,255);
        _drawMe_Geom(ri,SOM_GeomObjDrawType.rndClr);
        ri.popMatState();    
    }    
    
    
    public final void drawMeClrLoc(IRenderInterface ri) {
        ri.pushMatState();        
        ri.setFill(locClrAra,255);
        ri.setStroke(locClrAra,255);
        _drawMe_Geom(ri,SOM_GeomObjDrawType.locClr);
        ri.popMatState();    
    }
    
    /**
     * draw entire object this class represents, using location as color or using randomly assigned color
     * @param ri
     */    
    public final void drawMeClrRnd_WF(IRenderInterface ri) {
        ri.pushMatState();        
        ri.noFill();
        ri.setStroke(rndClrAra,255);
        _drawMe_Geom(ri,SOM_GeomObjDrawType.noFillRndClr);
        ri.popMatState();    
    }    
    
    public final void drawMeClrLoc_WF(IRenderInterface ri) {
        ri.pushMatState();        
        ri.noFill();
        ri.setStroke(locClrAra,255);
        _drawMe_Geom(ri,SOM_GeomObjDrawType.noFillLocClr);
        ri.popMatState();    
    }
    
        
    /**
     * Draw this object's label
     * @param ri
     */
    public abstract void drawMyLabel(IRenderInterface ri, SOM_AnimWorldWin animWin);
    
    protected void _drawLabelAtLoc_3D(IRenderInterface ri, myPointf pt, SOM_AnimWorldWin animWin, String label, float _scl, float _off) {
        ri.pushMatState();        
        ri.translate(pt.x,pt.y,pt.z); 
        animWin.unSetCamOrient();
        ri.scale(_scl);
        ri.showText(label, _off,-_off,0); 
        ri.popMatState();
    }
    protected final void _drawPointAtLoc_3D(IRenderInterface ri, myPointf pt, float r) {
        ri.pushMatState(); 
        ri.drawSphere(pt, r, 4);        
        ri.popMatState();
    } // render sphere of radius r and center P)    
    
    /**
     * show a point in 2d
     * @param ri
     * @param P
     * @param r
     * @param s
     * @param _off
     */
    protected final void _drawLabelAtLoc_2D(IRenderInterface ri, myPointf pt, float r, String label, float _off) {
        ri.pushMatState(); 
        //ri.translate(pt.x,pt.y,0); 
        ri.drawEllipse2D(pt.x,pt.y,r,r);                
        ri.showText(label, _off, _off);
        ri.popMatState();
    } // render sphere of radius r and center P)    
    
    protected final void _drawPointAtLoc_2D(IRenderInterface ri, myPointf pt, float r) {
        ri.pushMatState(); 
        //ri.translate(pt.x,pt.y,0); 
        ri.drawEllipse2D(pt.x,pt.y,r,r);//ri.circle(0,0,r,r);                        
        ri.popMatState();
    } // render sphere of radius r and center P)    
    
    /**
     * draw this object
     * @param ri
     */
    protected abstract void _drawMe_Geom(IRenderInterface ri, SOM_GeomObjDrawType drawType);
    
    /**
     * draw this object with appropriate selected highlight/cue
     * @param ri
     * @param animTimeMod
     */
    public final void drawMeSelected_ClrLoc(IRenderInterface ri, float animTimeMod, boolean drawSamples) {
        drawMeClrLoc(ri);
        _drawMeSelected(ri,animTimeMod);
        if(drawSamples && null!=objSamples) {objSamples.drawMeSmplsSelected(ri);}    
    };
    /**
     * draw this object with appropriate selected highlight/cue
     * @param ri
     * @param animTmMod
     */
    public void drawMeSelected_ClrRnd(IRenderInterface ri, float animTmMod, boolean drawSamples) {
        drawMeClrRnd(ri);
        _drawMeSelected(ri,animTmMod);
        if(drawSamples && null!=objSamples) {objSamples.drawMeSmplsSelected(ri);}
    };
    protected abstract void _drawMeSelected(IRenderInterface ri, float animTmMod);
    
    /**
     * draw this object's samples, using the random color
     * @param ri
     */
    public final void drawMySmplsLabel(IRenderInterface ri, SOM_AnimWorldWin animWin){    if(null!=objSamples) {if(getGeomFlag(is3dIDX)){objSamples.drawMySmplsLabel_3D(ri, animWin); } else { objSamples.drawMySmplsLabel_2D(ri);}}}//
    
    /**
     * draw this object's samples, using the random color
     * @param ri
     */
    public final void drawMeSmpls_ClrRnd(IRenderInterface ri){    if(null!=objSamples) {objSamples.drawMeSmpls_ClrRnd(ri);}}//
    
    /**
     * draw this object's samples, using the location-based color
     * @param ri
     */
    public final void drawMeSmpls_ClrLoc(IRenderInterface ri){    if(null!=objSamples) {objSamples.drawMeSmpls_ClrLoc(ri);}}//
    
    //////////////////////////////
    // bmu drawing
    
    /**
     * draw entire object this class represents at location dictated by BMU, using location as color or using randomly assigned color
     * @param ri
     */    
    public final void drawMeClrRnd_BMU(IRenderInterface ri) {
        if(!isBmuNull()) {        ((SOM_GeomMapNode) getBmu()).getVisObj().drawMeClrRnd(ri);}
    }    
    
    
    public final void drawMeClrLoc_BMU(IRenderInterface ri) {
        if(!isBmuNull()) {        ((SOM_GeomMapNode) getBmu()).getVisObj().drawMeClrLoc(ri);}
    }
    
    /**
     * draw wireframe rep of object's bmu
     * @param ri
     */    
    public final void drawMeClrRnd_WF_BMU(IRenderInterface ri) {
        if(!isBmuNull()) {        ((SOM_GeomMapNode) getBmu()).getVisObj().drawMeClrRnd_WF(ri);}
    }        
    
    public final void drawMeClrLoc_WF_BMU(IRenderInterface ri) {
        if(!isBmuNull()) {        ((SOM_GeomMapNode) getBmu()).getVisObj().drawMeClrLoc_WF(ri);}
    }
    
    public final void drawMyLabel_BMU(IRenderInterface ri, SOM_AnimWorldWin animWin) {    
        if(!isBmuNull()) {        ((SOM_GeomMapNode) getBmu()).getVisObj().drawMyLabel(ri, animWin);    }
    }
    
    public final void drawMeSelected_ClrLoc_BMU(IRenderInterface ri,float animTmMod, boolean drawSamples) {
        if(!isBmuNull()) {        ((SOM_GeomMapNode) getBmu()).getVisObj().drawMeSelected_ClrLoc(ri, animTmMod,drawSamples);}
    }
    public void drawMeSelected_ClrRnd_BMU(IRenderInterface ri,float animTmMod, boolean drawSamples) { 
        if(!isBmuNull()) {        ((SOM_GeomMapNode) getBmu()).getVisObj().drawMeSelected_ClrRnd(ri, animTmMod,drawSamples);}
    }    
    
    public final void drawMeSmpls_ClrRnd_BMU(IRenderInterface ri){
        if(!isBmuNull()) {        ((SOM_GeomMapNode) getBmu()).getVisObj().drawMeSmpls_ClrRnd(ri);}
    }//
    
    public final void drawMeSmpls_ClrLoc_BMU(IRenderInterface ri){
        if(!isBmuNull()) {        ((SOM_GeomMapNode) getBmu()).getVisObj().drawMeSmpls_ClrLoc(ri);}
    }//    


    ////////////////////////
    // end draw functions

    ////////////////////////
    // data accessor functions
    
    public int getNumSamples() {if(null!=objSamples) {return objSamples.getNumSamples();}else{return 0;}}
    public SOM_GeomSamplePointf getSamplePt(int idx) {if(null!=objSamples) {return objSamples.getSamplePt(idx);}else{return null;}}
    public SOM_GeomSamplePointf[] getAllSamplePts() {if(null!=objSamples) {return objSamples.getAllSamplePts();}else{return new SOM_GeomSamplePointf[0];}}
    
    private void initGeomFlags(){geomStFlags = new int[1 + numFlags/32]; for(int i = 0; i<numFlags; ++i){setFlag(i,false);}}
    public final void setGeomFlag(int idx, boolean val){
        int flIDX = idx/32, mask = 1<<(idx%32);
        geomStFlags[flIDX] = (val ?  geomStFlags[flIDX] | mask : geomStFlags[flIDX] & ~mask);
        switch (idx) {//special actions for each flag
            case debugIDX : {break;}            
        }
    }//setFlag    
    public final boolean getGeomFlag(int idx){int bitLoc = 1<<(idx%32);return (geomStFlags[idx/32] & bitLoc) == bitLoc;}

    public SOM_GeomSamplePointf[] getSrcPts() {    return srcPts;}

    public String getDispLabel() {return dispLabel;}

}//class SOM_GeomObj
