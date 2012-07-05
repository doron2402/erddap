/* 
 * EDDTableFromMWFS Copyright 2007, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.ByteArray;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.cohort.util.Test;

import gov.noaa.pfel.coastwatch.griddata.FileNameUtility;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.SSR;
import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;

import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.*;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;


/** 
 * THIS CLASS IS INACTIVE AS OF 2009-01-14. 
 * IT USED TO WORK, 
 * THEN THE SERVER STARTED RETURNING DOUBLED DATA,
 * THEN I CHANGED TO PERCENT-ENCODING THE REQUEST AND THEN IT COULDN'T PARSE THE REQUEST.
 * This class represents a table of data from a microWFS source.
 * It is very specific to the microWFS XML.
 * See http://csc-s-ial-p.csc.noaa.gov/DTL/DTLProjects/microwfs/microWFS.html .
 * And see web page form at:
 * http://csc-s-ial-p.csc.noaa.gov/DTL/DTLProjects/microwfs/ .
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2007-08-30
 */
public class EDDTableFromMWFS extends EDDTable{ 


    private final static String timeSeriesTag    = "<gml:FeatureCollection><gml:featureMember><ioos:insituTimeSeries>";
    private final static String timeSeriesEndTag = "<gml:FeatureCollection><gml:featureMember></ioos:insituTimeSeries>";

    /** The first 5 dataVariables are always created automatically: 
     * longitude, latitude, altitude, time, stationID. */
    protected final static int nFixedVariables = 5; 
    protected final static int stationIDIndex = 4; 


    /**
     * This constructs an EDDTableFromMWFS based on the information in an .xml file.
     * 
     * @param xmlReader with the &lt;erddapDatasets&gt;&lt;dataset type="EDDTableFromMWFS"&gt;
     *    having just been read.  
     * @return an EDDTableFromMWFS.
     *    When this returns, xmlReader will have just read &lt;erddapDatasets&gt;&lt;/dataset&gt; .
     * @throws Throwable if trouble
     */
    public static EDDTableFromMWFS fromXml(SimpleXMLReader xmlReader) throws Throwable {

        //data to be obtained (or not)
        if (verbose) String2.log("\n*** constructing EDDTableFromMWFS(xmlReader)...");
        String tDatasetID = xmlReader.attributeValue("datasetID"); 
        Attributes tGlobalAttributes = null;
        double tLongitudeSourceMinimum = Double.NaN;  
        double tLongitudeSourceMaximum = Double.NaN;  
        double tLatitudeSourceMinimum = Double.NaN;  
        double tLatitudeSourceMaximum = Double.NaN;  
        double tAltitudeSourceMinimum = Double.NaN;  
        double tAltitudeSourceMaximum = Double.NaN;  
        String tTimeSourceMinimum = "";  
        String tTimeSourceMaximum = "";  
        ArrayList tDataVariables = new ArrayList();
        int tReloadEveryNMinutes = Integer.MAX_VALUE;
        String tAccessibleTo = null;
        StringArray tOnChange = new StringArray();
        String tFgdcFile = null;
        String tIso19115File = null;
        String tLocalSourceUrl = null;

        //process the tags
        String startOfTags = xmlReader.allTags();
        int startOfTagsN = xmlReader.stackSize();
        int startOfTagsLength = startOfTags.length();
        while (true) {
            xmlReader.nextTag();
            String tags = xmlReader.allTags();
            String content = xmlReader.content();
            //if (reallyVerbose) String2.log("  tags=" + tags + content);
            if (xmlReader.stackSize() == startOfTagsN) 
                break; //the </dataset> tag
            String localTags = tags.substring(startOfTagsLength);

            //try to make the tag names as consistent, descriptive and readable as possible
            if      (localTags.equals("<addAttributes>"))
                tGlobalAttributes = getAttributesFromXml(xmlReader);
            else if (localTags.equals( "<longitudeSourceMinimum>")) {}
            else if (localTags.equals("</longitudeSourceMinimum>")) tLongitudeSourceMinimum = String2.parseDouble(content); 
            else if (localTags.equals( "<longitudeSourceMaximum>")) {}
            else if (localTags.equals("</longitudeSourceMaximum>")) tLongitudeSourceMaximum = String2.parseDouble(content); 
            else if (localTags.equals( "<latitudeSourceMinimum>")) {}
            else if (localTags.equals("</latitudeSourceMinimum>")) tLatitudeSourceMinimum = String2.parseDouble(content); 
            else if (localTags.equals( "<latitudeSourceMaximum>")) {}
            else if (localTags.equals("</latitudeSourceMaximum>")) tLatitudeSourceMaximum = String2.parseDouble(content); 
            else if (localTags.equals( "<altitudeSourceMinimum>")) {}
            else if (localTags.equals("</altitudeSourceMinimum>")) tAltitudeSourceMinimum = String2.parseDouble(content); 
            else if (localTags.equals( "<altitudeSourceMaximum>")) {}
            else if (localTags.equals("</altitudeSourceMaximum>")) tAltitudeSourceMaximum = String2.parseDouble(content); 
            else if (localTags.equals( "<timeSourceMinimum>")) {}
            else if (localTags.equals("</timeSourceMinimum>")) tTimeSourceMinimum = content; 
            else if (localTags.equals( "<timeSourceMaximum>")) {}
            else if (localTags.equals("</timeSourceMaximum>")) tTimeSourceMaximum = content; 
            else if (localTags.equals( "<dataVariable>")) 
                tDataVariables.add(getSDADVariableFromXml(xmlReader));           
            else if (localTags.equals( "<accessibleTo>")) {}
            else if (localTags.equals("</accessibleTo>")) tAccessibleTo = content;
            else if (localTags.equals( "<reloadEveryNMinutes>")) {}
            else if (localTags.equals("</reloadEveryNMinutes>")) tReloadEveryNMinutes = String2.parseInt(content); 
            else if (localTags.equals( "<sourceUrl>")) {}
            else if (localTags.equals("</sourceUrl>")) tLocalSourceUrl = content; 

            //onChange
            else if (localTags.equals( "<onChange>")) {}
            else if (localTags.equals("</onChange>")) 
                tOnChange.add(content); 

            else if (localTags.equals( "<fgdcFile>")) {}
            else if (localTags.equals("</fgdcFile>"))     tFgdcFile = content; 
            else if (localTags.equals( "<iso19115File>")) {}
            else if (localTags.equals("</iso19115File>")) tIso19115File = content; 

            else xmlReader.unexpectedTagException();
        }
        int ndv = tDataVariables.size();
        Object ttDataVariables[][] = new Object[ndv][];
        for (int i = 0; i < tDataVariables.size(); i++)
            ttDataVariables[i] = (Object[])tDataVariables.get(i);

        return new EDDTableFromMWFS(tDatasetID, tAccessibleTo,
            tOnChange, tFgdcFile, tIso19115File, tGlobalAttributes,
            tLongitudeSourceMinimum, tLongitudeSourceMaximum,
            tLatitudeSourceMinimum,  tLatitudeSourceMaximum,
            tAltitudeSourceMinimum,  tAltitudeSourceMaximum, 
            tTimeSourceMinimum,      tTimeSourceMaximum,     
            ttDataVariables,
            tReloadEveryNMinutes, tLocalSourceUrl);
    }

    /**
     * The constructor.
     *
     * <p>There is probably a getCapabilities-type of WFS request that
     * has station information. Unfortunately, this class doesn't 
     * utilize it, so it requests min,max lon,lat,alt,time range
     * explicitly.
     *
     * @param tDatasetID is a very short string identifier 
     *   (required: just safe characters: A-Z, a-z, 0-9, _, -, or .)
     *   for this dataset. See EDD.datasetID().
     * @param tAccessibleTo is a comma separated list of 0 or more
     *    roles which will have access to this dataset.
     *    <br>If null, everyone will have access to this dataset (even if not logged in).
     *    <br>If "", no one will have access to this dataset.
     * @param tOnChange 0 or more actions (starting with "http://" or "mailto:")
     *    to be done whenever the dataset changes significantly
     * @param tFgdcFile This should be the fullname of a file with the FGDC
     *    that should be used for this dataset, or "" (to cause ERDDAP not
     *    to try to generate FGDC metadata for this dataset), or null (to allow
     *    ERDDAP to try to generate FGDC metadata for this dataset).
     * @param tIso19115 This is like tFgdcFile, but for the ISO 19119-2/19139 metadata.
     * @param tAddGlobalAttributes are global attributes which will
     *   be added to (and take precedence over) the data source's global attributes.
     *   This may be null if you have nothing to add.
     *   The combined global attributes must include:
     *   <ul>
     *   <li> "title" - the short (&lt; 80 characters) description of the dataset 
     *   <li> "summary" - the longer description of the dataset.
     *      It may have newline characters (usually at &lt;= 72 chars per line). 
     *   <li> "institution" - the source of the data 
     *      (best if &lt; 50 characters so it fits in a graph's legend).
     *   <li> "infoUrl" - the url with information about this data set 
     *   <li> "cdm_data_type" - one of the EDD.CDM_xxx options
     *   </ul>
     *   Special case: value="null" causes that item to be removed from combinedGlobalAttributes.
     *   Special case: if combinedGlobalAttributes name="license", any instance of "[standard]"
     *     will be converted to the EDStatic.standardLicense.
     * @param tLonMin in source units (use Double.NaN if not known).
     *    [I use eddTable.getEmpiricalMinMax("2007-02-01", "2007-02-01", false, true); below to get it.]
     * @param tLonMax see tLonMin description.
     * @param tLatMin see tLonMin description.
     * @param tLatMax see tLonMin description.
     * @param tAltMin see tLonMin description. 
     * @param tAltMax see tLonMin description.
     * @param tTimeMin   in EDVTimeStamp.ISO8601TZ_FORMAT, or "" if not known.
     * @param tTimeMax   in EDVTimeStamp.ISO8601TZ_FORMAT, or "" if not known
     * @param tDataVariables is an Object[nDataVariables][4]: 
     *    <br>[0]=String sourceName (the name of the data variable in the dataset source),
     *    <br>[1]=String destinationName (the name to be presented to the ERDDAP user, 
     *        or null to use the sourceName),
     *    <br>[2]=Attributes addAttributes (at ERD, this must have "ioos_category" - 
     *        a category from EDV.ioosCategories). 
     *        Special case: value="null" causes that item to be removed from combinedAttributes.
     *    <br>[3]=String source dataType (e.g., "int", "float", "String"). 
     *        Some data sources have ambiguous data types, so it needs to be specified here.
     *    <br>The order of variables you define doesn't have to match the
     *       order in the source.
     *    <br>!!!Unique to EDDTableFromMWFS: the longitude, latitude, altitude,  
     *       time and stationID variables are created automatically.
     *    <br>!!!Unique to EDDTableFromMWFS: there can be only 1 tDataVariable.
     * @param tReloadEveryNMinutes indicates how often the source should
     *    be checked for new data.
     * @param tLocalSourceUrl the url to which queries are sent
     * @throws Throwable if trouble
     */
    public EDDTableFromMWFS(String tDatasetID, String tAccessibleTo,
        StringArray tOnChange, String tFgdcFile, String tIso19115File,
        Attributes tAddGlobalAttributes,
        double tLonMin, double tLonMax,
        double tLatMin, double tLatMax,
        double tAltMin, double tAltMax,
        String tTimeMin, String tTimeMax,
        Object[][] tDataVariables,
        int tReloadEveryNMinutes,
        String tLocalSourceUrl) throws Throwable {

        if (verbose) String2.log(
            "\n*** constructing EDDTableFromMWFS " + tDatasetID); 
        long constructionStartMillis = System.currentTimeMillis();
        String errorInMethod = "Error in EDDTableFromMWFS(" + 
            tDatasetID + ") constructor:\n";
            
        //save some of the parameters
        className = "EDDTableFromMWFS"; 
        datasetID = tDatasetID;
        setAccessibleTo(tAccessibleTo);
        onChange = tOnChange;
        fgdcFile = tFgdcFile;
        iso19115File = tIso19115File;
        if (tAddGlobalAttributes == null)
            tAddGlobalAttributes = new Attributes();
        addGlobalAttributes = tAddGlobalAttributes;
        addGlobalAttributes.set("sourceUrl", makePublicSourceUrl(tLocalSourceUrl));
        localSourceUrl = tLocalSourceUrl;
        setReloadEveryNMinutes(tReloadEveryNMinutes);
        Test.ensureNotNothing(sourceUrl, "sourceUrl wasn't specified.");

        sourceCanConstrainNumericData = CONSTRAIN_PARTIAL; //lon, lat, alt, time; others nothing
        sourceCanConstrainStringData  = CONSTRAIN_NO; 
        sourceCanConstrainStringRegex = "";
      
        //set source attributes (none available from source)
        sourceGlobalAttributes = new Attributes();
        combinedGlobalAttributes = new Attributes(addGlobalAttributes, sourceGlobalAttributes); //order is important
        String tLicense = combinedGlobalAttributes.getString("license");
        if (tLicense != null)
            combinedGlobalAttributes.set("license", 
                String2.replaceAll(tLicense, "[standard]", EDStatic.standardLicense));
        combinedGlobalAttributes.removeValue("null");

        //make the fixedVariables
        dataVariables = new EDV[nFixedVariables + tDataVariables.length];
        lonIndex = 0;
        latIndex = 1;
        altIndex = 2;
        timeIndex = 3;
        dataVariables[lonIndex] = new EDVLon(EDV.LON_NAME, null, null, "double", 
            Double.isNaN(tLonMin)? -180 : tLonMin, 
            Double.isNaN(tLonMax)?  180 : tLonMax);
        dataVariables[latIndex] = new EDVLat(EDV.LAT_NAME, null, null, "double", 
            Double.isNaN(tLatMin)? -90 : tLatMin, 
            Double.isNaN(tLatMax)?  90 : tLatMax);
        dataVariables[altIndex] = new EDVAlt(EDV.ALT_NAME, null, null,
            "double", tAltMin, tAltMax,
//!!!altitude values are in meters, but I'm not certain that positive=up!!!
            1); //tAltMetersPerSourceUnit);
        dataVariables[timeIndex] = new EDVTime(EDV.TIME_NAME, null, 
            (new Attributes()).add("units", EDVTimeStamp.ISO8601TZ_FORMAT).
            add("actual_range", new StringArray(new String[]{tTimeMin, tTimeMax})), 
            "String");
        dataVariables[stationIDIndex] = new EDV("station_id", null, 
            null, (new Attributes()) 
                .add("long_name", "Station ID")
                .add("ioos_category", "Identifier"),
            "String"); //the constructor that reads actual_range
        //no need to call setActualRangeFromDestinationMinMax() since they are NaNs

        //create non-fixed dataVariables[]
        Test.ensureEqual(tDataVariables.length, 1,
            errorInMethod + "tDataVariables.length must be 1.");
        for (int dv = 0; dv < tDataVariables.length; dv++) {
            String tSourceName = (String)tDataVariables[dv][0];
            String tDestName = (String)tDataVariables[dv][1];
            if (tDestName == null || tDestName.trim().length() == 0)
                tDestName = tSourceName;
            Attributes tSourceAtt = null; //(none available from source)
            Attributes tAddAtt = (Attributes)tDataVariables[dv][2];
            String tSourceType = (String)tDataVariables[dv][3];

            if (EDVTimeStamp.hasTimeUnits(tSourceAtt, tAddAtt)) {
                dataVariables[nFixedVariables + dv] = new EDVTimeStamp(tSourceName, tDestName, 
                    tSourceAtt, tAddAtt,
                    tSourceType); //the constructor that reads actual_range
                dataVariables[nFixedVariables + dv].setActualRangeFromDestinationMinMax();
            } else {
                dataVariables[nFixedVariables + dv] = new EDV(tSourceName, tDestName, 
                    tSourceAtt, tAddAtt,
                    tSourceType); //the constructor that reads actual_range
                dataVariables[nFixedVariables + dv].setActualRangeFromDestinationMinMax();
            }
        }

        //ensure the setup is valid
        ensureValid();

        //done
        if (verbose) String2.log(
            (reallyVerbose? "\n" + toString() : "") +
            "\n*** EDDTableFromMWFS " + datasetID + 
            " constructor finished. TIME=" + 
            (System.currentTimeMillis() - constructionStartMillis) + "\n"); 

    }


    /** 
     * This gets the data (chunk by chunk) from this EDDTable for the 
     * OPeNDAP DAP-style query and writes it to the TableWriter. 
     * See the EDDTable method documentation.
     *
     * @param loggedInAs the user's login name if logged in (or null if not logged in).
     * @param requestUrl the part of the user's request, after EDStatic.baseUrl, before '?'.
     * @param userDapQuery the part of the user's request after the '?', still percentEncoded, may be null.
     * @param tableWriter
     */
    public void getDataForDapQuery(String loggedInAs, String requestUrl, 
        String userDapQuery, TableWriter tableWriter) throws Throwable {
        long getTime = System.currentTimeMillis();

        //get requestedMin,Max
        double requestedDestinationMin[] = new double[4];
        double requestedDestinationMax[] = new double[4];
        getRequestedDestinationMinMax(userDapQuery, true,
            requestedDestinationMin, requestedDestinationMax);        

        //no need to further prune constraints 
        //getRequestedDestinationMinMax handles it
        //sourceCanConstrainNumericData = CONSTRAIN_PARTIAL; //lon, lat, alt, time; others nothing
        //sourceCanConstrainStringData  = CONSTRAIN_NO; 
        //sourceCanConstrainStringRegex = "";

        //make the constraint
        //BBOX order defined at http://csc-s-ial-p.csc.noaa.gov/DTL/DTLProjects/microwfs/
        // as max lon, min lat, min lon, max lat  (!)
        //e.g.,  "&BBOX=-69.00,32.0,-72.00,42.30&TIME=2007-06-01T12:00Z,2007-06-01T14:00Z" + 
        //       "&TYPENAME=waterTemperature";
        String sourceVarName = dataVariables[nFixedVariables].sourceName();
        String constraint = "&BBOX=" + 
            String2.genEFormat6(requestedDestinationMax[0]) + "," + 
            String2.genEFormat6(requestedDestinationMin[1]) + "," + 
            String2.genEFormat6(requestedDestinationMin[0]) + "," + 
            String2.genEFormat6(requestedDestinationMax[1]) +
            "&TIME=" + Calendar2.epochSecondsToIsoStringT(requestedDestinationMin[3]) + 
                "Z," + Calendar2.epochSecondsToIsoStringT(requestedDestinationMax[3]) + "Z" + 
            "&TYPENAME=" + sourceVarName;
        if (reallyVerbose)
            String2.log("  constraint=" + constraint);

        //set up the table
        Table table = makeTable();

        //request the data
//???break the request up into smaller time chunks???
        double tLon = Double.NaN, tLat = Double.NaN, tAltitude = Double.NaN; //values that persist for a while
        String tTime = "", tStationID = "";     //values that persist for a while
        //read the xml properties file
        if (verbose) String2.log("mWFS data request url=\n" + sourceUrl + constraint);
        SimpleXMLReader xmlReader = new SimpleXMLReader(
            SSR.getUrlInputStream(sourceUrl + constraint), "gml:FeatureCollection");
        String tags = xmlReader.allTags();
        TAG_LOOP:
        do {
            //process the tags
            if (reallyVerbose) String2.log("tags=" + tags + xmlReader.content());

            if (tags.startsWith(timeSeriesTag)) {
                String endOfTsTag = tags.substring(timeSeriesTag.length());
                String content = xmlReader.content();
                String error = null;

                if (endOfTsTag.equals("</ioos:sensor>")) {
                    //e.g., 44004
                    tStationID = content;

                } else if (endOfTsTag.equals("</ioos:observationName>")) {
                    //e.g., waterTemperature
                    if (!content.equals(sourceVarName))
                        error = "<ioos:observationName>=" + content + 
                            " should have been " + sourceVarName + ".";

                } else if (endOfTsTag.equals("</ioos:verticalPosition>")) {
                    //e.g., 0
                    tAltitude = String2.parseDouble(content);
                    if (Double.isNaN(tAltitude))
                        error = "Invalid <ioos:verticalPosition>=" + content;

                } else if (endOfTsTag.equals("<ioos:horizontalPosition><gml:Point></gml:pos>")) {
                    //e.g., 38.48 -70.43    order is lat lon
                    int spo = content.indexOf(' ');                
                    if (spo < 0) spo = 0; //push error to handler below 
                    tLat = String2.parseDouble(content.substring(0, spo));
                    tLon = String2.parseDouble(content.substring(spo + 1));
                    if (Double.isNaN(tLon) || Double.isNaN(tLat))
                        error = "Invalid <gml:Point>=" + content;

                } else if (endOfTsTag.equals("<ioos:tsEvent><ioos:TSMeasurement></ioos:obsDateTime>")) {
                    //e.g., 2007-06-01T12:50:00Z
                    tTime = content;

                } else if (endOfTsTag.equals("<ioos:tsEvent><ioos:TSMeasurement></ioos:observation>")) {
                    //e.g., 24.1
                    String invalid = null;
                    if (Double.isNaN(tLon))       invalid = "longitude";
                    if (Double.isNaN(tLat))       invalid = "latitude";
                    if (Double.isNaN(tAltitude))  invalid = "altitude";
                    if (tTime.length() == 0)      invalid = "time";
                    if (tStationID.length() == 0) invalid = "station_id";
                    if (invalid == null) {

                        //add a row of data to table
                        table.getColumn(lonIndex).addDouble(tLon);
                        table.getColumn(latIndex).addDouble(tLat);
                        table.getColumn(altIndex).addDouble(tAltitude);
                        table.getColumn(timeIndex).addString(tTime);
                        table.getColumn(stationIDIndex).addString(tStationID);
                        table.getColumn(nFixedVariables).addString(content);

                        //write to tableWriter?
                        if (writeChunkToTableWriter(requestUrl, userDapQuery, table, tableWriter, false)) {
                            table = makeTable();
                            if (tableWriter.noMoreDataPlease) {
                                tableWriter.logCaughtNoMoreDataPlease(datasetID);
                                break TAG_LOOP;
                            }
                        }


                    } else {
                        error = "The " + invalid + " value wasn't set in the XML response.";
                    }

                //a tag that unsets persistent values
                } else if (endOfTsTag.equals("</ioos:tsEvent>")) {
                    tTime = "";
                }

                //handle the error
                if (error != null)
                    throw new RuntimeException(
                        "Data source error on xml line #" + xmlReader.lineNumber() + 
                        ": " + error);

            //a tag that unsets persistent values
            } else if (tags.equals(timeSeriesEndTag)) {
                tLon = Double.NaN;
                tLat = Double.NaN;
                tAltitude = Double.NaN;
                tTime = "";
                tStationID = "";                
            }            


            //get the next tags
            xmlReader.nextTag();
            tags = xmlReader.allTags();
        } while (!tags.equals("</gml:FeatureCollection>"));

        xmlReader.close();      

        //do the final writeToTableWriter
        writeChunkToTableWriter(requestUrl, userDapQuery, table, tableWriter, true);
        if (verbose) String2.log("  getDataForDapQuery done. TIME=" +
            (System.currentTimeMillis() - getTime)); 
    }

    //makes a table with a column for each variable (with sourceNames) to hold source values
    private Table makeTable() throws Throwable {
        Table table = new Table();
        for (int col = 0; col < dataVariables.length; col++)
            table.addColumn(col, dataVariables[col].sourceName(), 
                PrimitiveArray.factory(dataVariables[col].sourceDataTypeClass(), 128, false)); 
        return table;
    }

    /** 
     * Make a test dataset.
     * @return a test dataset (waterTemperature)
     */
    public static EDDTableFromMWFS testDataset() throws Throwable {
        return new EDDTableFromMWFS(
            "cscWT", //String tDatasetID, 
            null,
            (new Attributes())
                .add("title",         "Buoy Data (Water Temperature) from the NOAA CSC microWFS") //for demo: I emphasize source
                .add("summary",       
"[Normally, the summary describes the dataset. Here, it describes \n" +
"the server.] \n" +
"The mission of the NOAA CSC Data Transport Laboratory (DTL) is to \n" +
"support the employment of data transport technologies that are \n" +
"compatible with Ocean.US Data Management and Communications (DMAC) \n" +
"guidance at the local and regional levels. This is accomplished \n" +
"through the identification, evaluation, and documentation of \n" +
"relevant data transport technology candidates. In following that \n" +
"mission, the DTL is exploring the use of the Open Geospatial \n" +
"Consortium (OGC) Web Feature Service (WFS) and the Geography \n" +
"Markup Language (GML) Simple Feature Profile to transport in-situ \n" +
"time series data.")   //better summary?
                .add("institution",   "NOAA CSC")
                .add("infoUrl",       "http://www.csc.noaa.gov/DTL/dtl_proj4_gmlsfp_wfs.html")
                .add("cdm_data_type", CDM_TIMESERIES)
                .add("standard_name_vocabulary", FileNameUtility.getStandardNameVocabulary())
                .add("license",       "[standard]"),
            //min/max lon lat alt gathered by looking at data for 2007-02-01 
            // via eddTable.getEmpiricalMinMax("2007-02-01", "2007-02-01", false, true); below 
            -97.22, -70.43,
            24.55, 38.48,
            0, 0,
            "2007-01-01T00:00:00Z", "", //tSourceTimeMin Max
            new Object[][]{  //dataVariables: sourceName, destName, type, addAttributes
                {"waterTemperature", "sea_water_temperature", "float", (new Attributes())
                    .add("ioos_category", "Temperature")
                    .add("long_name", "Sea Water Temperature")
                    .add("standard_name", "sea_water_temperature")
                    .add("units", "degree_C")}},
            60, //int tReloadEveryNMinutes,
            "http://csc-s-ial-p.csc.noaa.gov/cgi-bin/microwfs/microWFS.cgi?SERVICENAME=dtlservice" +
                "&REQUEST=getFeature&SERVICE=microWFS&VERSION=1.1.0&OUTPUTFORMAT=text/xml;" +
                "subType=gml/3.1.1/profiles/gmlsf/1.0.0/1");
            //&BBOX=-69.00,32.0,-72.00,42.30&TIME=2007-06-01T12:00Z,2007-06-01T14:00Z&TYPENAME=waterTemperature

    }

    /**
     * This tests the methods in this class.
     *
     * @param doLongTest
     * @throws Throwable if trouble
     */
    public static void test(boolean doLongTest) throws Throwable {
        String2.log("\n****************** EDDTableFromMWFS.test() *****************\n");
        verbose = true;
        reallyVerbose = true;
        SSR.verbose = true;
        SSR.reallyVerbose = true;
        String today = Calendar2.getCurrentISODateTimeStringLocal().substring(0, 10);
        try {

/*
    <dataset type="EDDTableFromMWFS" datasetID="cscWT">        
        <sourceUrl>http://csc-s-ial-p.csc.noaa.gov/cgi-bin/microwfs/microWFS.cgi?SERVICENAME=dtlservice&amp;REQUEST=getFeature&amp;SERVICE=microWFS&amp;VERSION=1.1.0&amp;OUTPUTFORMAT=text/xml;subType=gml/3.1.1/profiles/gmlsf/1.0.0/1</sourceUrl>
        <addAttributes> 
            <att name="title">Buoy Data (Water Temperature) from the NOAA CSC microWFS</att>
            <att name="summary">       
[Normally, the summary describes the dataset. Here, it describes 
the server.] 
The mission of the NOAA CSC Data Transport Laboratory (DTL) is to 
support the employment of data transport technologies that are 
compatible with Ocean.US Data Management and Communications (DMAC) 
guidance at the local and regional levels. This is accomplished 
through the identification, evaluation, and documentation of 
relevant data transport technology candidates. In following that 
mission, the DTL is exploring the use of the Open Geospatial 
Consortium (OGC) Web Feature Service (WFS) and the Geography 
Markup Language (GML) Simple Feature Profile to transport in-situ 
time series data.</att>
            <att name="cdm_data_type">Station</att>
            <att name="Conventions">COARDS, CF-1.6, Unidata Dataset Discovery v1.0</att>  
            <att name="infoUrl">http://www.csc.noaa.gov/DTL/dtl_proj4_gmlsfp_wfs.html</att>
            <att name="institution">NOAA CSC</att>
            <att name="license">[standard]</att>
            <att name="Metadata_Conventions">COARDS, CF-1.6, Unidata Dataset Discovery v1.0</att>  
            <att name="standard_name_vocabulary">CF-12</att>
        </addAttributes> 
        <longitudeSourceMinimum>-97.22</longitudeSourceMinimum>
        <longitudeSourceMaximum>-70.43</longitudeSourceMaximum>
        <latitudeSourceMinimum>24.55</latitudeSourceMinimum>
        <latitudeSourceMaximum>38.48</latitudeSourceMaximum>
        <altitudeSourceMinimum>0</altitudeSourceMinimum>
        <altitudeSourceMaximum>0</altitudeSourceMaximum>
        <timeSourceMinimum>2007-01-01T00:00:00Z</timeSourceMinimum>
        <timeSourceMaximum></timeSourceMaximum>
        <dataVariable>
            <sourceName>waterTemperature</sourceName>
            <destinationName>sea_water_temperature</destinationName>
            <dataType>float</dataType>
            <addAttributes>
                <att name="colorBarMinimum" type="double">0.0</att>
                <att name="colorBarMaximum" type="double">32.0</att>
                <att name="ioos_category">Temperature</att>
                <att name="long_name">Sea Water Temperature</att>
                <att name="standard_name">sea_water_temperature</att>
                <att name="units">degree_C</att>
            </addAttributes>
        </dataVariable>
    </dataset>

*/
        //EDDTable mwfs = testDataset(); //should work
        EDDTable mwfs = (EDDTable)oneFromDatasetXml("cscWT"); //should work

        double tLon, tLat;
        String name, tName, results, expected, userDapQuery;
        String error = "";

        //getEmpiricalMinMax   just do once
        //mwfs.getEmpiricalMinMax("2007-02-01", "2007-02-01", false, true);
        //if (true) System.exit(1);

        //a test based on their web page's example
        //http://csc-s-ial-p.csc.noaa.gov/DTL/DTLProjects/microwfs/
        TableWriterAllInMemory tw = new TableWriterAllInMemory();
        //BBOX is max lon, min lat, min lon, max lat  (!)
        //e.g.,  "&BBOX=-69.00,32.0,-72.00,42.30&TIME=2007-06-01T12:00Z,2007-06-01T14:00Z" + 
        //       "&TYPENAME=waterTemperature";
        mwfs.getDataForDapQuery(null, "", "longitude,latitude,altitude,time,station_id,sea_water_temperature" +
            "&longitude>=-72&longitude<=-69&latitude>=32&latitude<=42.3" +
            "&time>=2007-06-01T12:00&time<=2007-06-01T14:00", tw);
        Table table = tw.cumulativeTable();
        String2.log("table=" + table.toString("row", 10));
        //   Row       longitude       latitude       altitude           time     station_id sea_water_temp
        //     0          -70.43          38.48              0     1180702200          44004           24.1
        //     1          -70.43          38.48              0     1180705800          44004           24.1
        Test.ensureEqual(table.getFloatData(0, 0), -70.43f, "");
        Test.ensureEqual(table.getFloatData(0, 1), -70.43f, "");
        Test.ensureEqual(table.getFloatData(1, 0), 38.48f, "");
        Test.ensureEqual(table.getFloatData(1, 1), 38.48f, "");
        Test.ensureEqual(table.getFloatData(2, 0), 0f, "");
        Test.ensureEqual(table.getFloatData(2, 1), 0f, "");
        Test.ensureEqual(table.getDoubleData(3, 0), 1180702200, "");
        Test.ensureEqual(table.getDoubleData(3, 1), 1180705800, "");  
        Test.ensureEqual(table.getStringData(4, 0), "44004", "");
        Test.ensureEqual(table.getStringData(4, 1), "44004", "");
        Test.ensureEqual(table.getFloatData(5, 0), 24.1f, "");
        Test.ensureEqual(table.getFloatData(5, 1), 24.1f, "");


        //*** test getting das for entire dataset
        String2.log("\n****************** EDDTableFromMWFS.test das dds for entire dataset\n");
        tName = mwfs.makeNewFileForDapQuery(null, "", EDStatic.fullTestCacheDirectory, className + "_Entire", ".das"); 
        results = String2.annotatedString(new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray()));
        //String2.log(results);
        expected = //see OpendapHelper.EOL for comments
"Attributes {[10]\n" +
" s {[10]\n" +
"  longitude {[10]\n" +
"    String _CoordinateAxisType \"Lon\";[10]\n" +
"    Float64 actual_range -97.22, -70.43;[10]\n" +
"    String axis \"X\";[10]\n" +
"    String ioos_category \"Location\";[10]\n" +
"    String long_name \"Longitude\";[10]\n" +
"    String standard_name \"longitude\";[10]\n" +
"    String units \"degrees_east\";[10]\n" +
"  }[10]\n" +
"  latitude {[10]\n" +
"    String _CoordinateAxisType \"Lat\";[10]\n" +
"    Float64 actual_range 24.55, 38.48;[10]\n" +
"    String axis \"Y\";[10]\n" +
"    String ioos_category \"Location\";[10]\n" +
"    String long_name \"Latitude\";[10]\n" +
"    String standard_name \"latitude\";[10]\n" +
"    String units \"degrees_north\";[10]\n" +
"  }[10]\n" +
"  altitude {[10]\n" +
"    String _CoordinateAxisType \"Height\";[10]\n" +
"    String _CoordinateZisPositive \"up\";[10]\n" +
"    Float64 actual_range 0.0, 0.0;[10]\n" +
"    String axis \"Z\";[10]\n" +
"    String ioos_category \"Location\";[10]\n" +
"    String long_name \"Altitude\";[10]\n" +
"    String positive \"up\";[10]\n"+
"    String standard_name \"altitude\";[10]\n" +
"    String units \"m\";[10]\n" +
"  }[10]\n" +
"  time {[10]\n" +
"    String _CoordinateAxisType \"Time\";[10]\n" +
"    Float64 actual_range 1.1676096e+9, NaN;[10]\n" +
"    String axis \"T\";[10]\n" +
"    String ioos_category \"Time\";[10]\n" +
"    String long_name \"Time\";[10]\n" +
"    String standard_name \"time\";[10]\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";[10]\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";[10]\n" +
"  }[10]\n" +
"  station_id {[10]\n" +
"    String ioos_category \"Identifier\";[10]\n" +
"    String long_name \"Station ID\";[10]\n" +
"  }[10]\n" +
"  sea_water_temperature {[10]\n" +
"    String ioos_category \"Temperature\";[10]\n" +
"    String long_name \"Sea Water Temperature\";[10]\n" +
"    String standard_name \"sea_water_temperature\";[10]\n" +
"    String units \"degree_C\";[10]\n" +
"  }[10]\n" +
" }[10]\n" +
"  NC_GLOBAL {[10]\n" +
"    String cdm_data_type \"TimeSeries\";[10]\n" +
"    String cdm_timeseries_variables \"???\";[10]\n" +
"    String Conventions \"COARDS, CF-1.6, Unidata Dataset Discovery v1.0\";[10]\n" +
"    Float64 Easternmost_Easting -70.43;[10]\n" +
"    Float64 geospatial_lat_max 38.48;[10]\n" +
"    Float64 geospatial_lat_min 24.55;[10]\n" +
"    String geospatial_lat_units \"degrees_north\";[10]\n" +
"    Float64 geospatial_lon_max -70.43;[10]\n" +
"    Float64 geospatial_lon_min -97.22;[10]\n" +
"    String geospatial_lon_units \"degrees_east\";[10]\n" +
"    Float64 geospatial_vertical_max 0.0;[10]\n" +
"    Float64 geospatial_vertical_min 0.0;[10]\n" +
"    String geospatial_vertical_positive \"up\";[10]\n" +
"    String geospatial_vertical_units \"m\";[10]\n" +
"    String history \"" + today + " http://csc-s-ial-p.csc.noaa.gov/cgi-bin/microwfs/microWFS.cgi?SERVICENAME=dtlservicesubType=gml/3.1.1/profiles/gmlsf/1.0.0/1[10]\n" +
today + " " + EDStatic.erddapUrl + //in tests, always use non-https url
                "/tabledap/cscWT.das\";[10]\n" + //note no '?' after .das
"    String infoUrl \"http://www.csc.noaa.gov/DTL/dtl_proj4_gmlsfp_wfs.html\";[10]\n" +
"    String institution \"NOAA CSC\";[10]\n" +
"    String license \"The data may be used and redistributed for free but is not intended[10]\n" +
"for legal use, since it may contain inaccuracies. Neither the data[10]\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any[10]\n" +
"of their employees or contractors, makes any warranty, express or[10]\n" +
"implied, including warranties of merchantability and fitness for a[10]\n" +
"particular purpose, or assumes any legal liability for the accuracy,[10]\n" +
"completeness, or usefulness, of this information.\";[10]\n" +
"    String Metadata_Conventions \"COARDS, CF-1.6, Unidata Dataset Discovery v1.0\";[10]\n" +
"    Float64 Northernmost_Northing 38.48;[10]\n" +
"    String sourceUrl \"http://csc-s-ial-p.csc.noaa.gov/cgi-bin/microwfs/microWFS.cgi?SERVICENAME=dtlservicesubType=gml/3.1.1/profiles/gmlsf/1.0.0/1\";[10]\n" +
"    Float64 Southernmost_Northing 24.55;[10]\n" +
"    String standard_name_vocabulary \"CF-12\";[10]\n" +
"    String summary \"[Normally, the summary describes the dataset. Here, it describes [10]\n" +
"the server.][10]\n" +
"The mission of the NOAA CSC Data Transport Laboratory (DTL) is to[10]\n" +
"support the employment of data transport technologies that are[10]\n" +
"compatible with Ocean.US Data Management and Communications (DMAC)[10]\n" +
"guidance at the local and regional levels. This is accomplished[10]\n" +
"through the identification, evaluation, and documentation of[10]\n" +
"relevant data transport technology candidates. In following that[10]\n" +
"mission, the DTL is exploring the use of the Open Geospatial[10]\n" +
"Consortium (OGC) Web Feature Service (WFS) and the Geography[10]\n" +
"Markup Language (GML) Simple Feature Profile to transport in-situ[10]\n" +
"time series data.\";[10]\n" +
"    String time_coverage_start \"2007-01-01T00:00:00Z\";[10]\n" +
"    String title \"Buoy Data (Water Temperature) from the NOAA CSC microWFS\";[10]\n" +
"    Float64 Westernmost_Easting -97.22;[10]\n" +
"  }[10]\n" +
"}[10]\n" +
"[end]";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);
        
        //*** test getting dds for entire dataset
        tName = mwfs.makeNewFileForDapQuery(null, "", EDStatic.fullTestCacheDirectory, className + "_Entire", ".dds"); 
        results = String2.annotatedString(new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray()));
        //String2.log(results);
        expected = 
"Dataset {[10]\n" +
"  Sequence {[10]\n" +
"    Float64 longitude;[10]\n" +
"    Float64 latitude;[10]\n" +
"    Float64 altitude;[10]\n" +
"    Float64 time;[10]\n" +
"    String station_id;[10]\n" +
"    Float32 sea_water_temperature;[10]\n" +
"  } s;[10]\n" +
"} s;[10]\n" +
"[end]";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


        //*** test make data files
        String2.log("\n****************** EDDTableFromMWFS.test make DATA FILES\n");       

        //.asc test of just one station, FOR 1 WEEK
        tLon = -70.43;
        tLat = 38.48;
        userDapQuery = "longitude,latitude,station_id,time,sea_water_temperature" +
            "&longitude=" + tLon + "&latitude=" + tLat +
            "&time>=2007-05-01&time<=2007-05-08"; 
        //2007-05-01 -> 1.1779776E9
        //2007-05-08 -> 1.1785824E9
        tName = mwfs.makeNewFileForDapQuery(null, userDapQuery, EDStatic.fullTestCacheDirectory, className + "_station1", ".asc"); 
        results = String2.annotatedString(new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray()));
        //String2.log(results);
        expected = 
"Dataset {[10]\n" +
"  Sequence {[10]\n" +
"    Float64 longitude;[10]\n" +
"    Float64 latitude;[10]\n" +
"    String station_id;[10]\n" +
"    Float64 time;[10]\n" +
"    Float32 sea_water_temperature;[10]\n" +
"  } s;[10]\n" +
"} s;[10]\n" +
"---------------------------------------------[10]\n" +
"s.longitude, s.latitude, s.station_id, s.time, s.sea_water_temperature[10]\n" +
"-70.43, 38.48, \"44004\", 1.1779806E9, 8.9[10]\n";
        Test.ensureTrue(results.startsWith(expected), "\nresults=\n" + results.substring(0, 600));
        expected = 
"-70.43, 38.48, \"44004\", 1.1785818E9, 20.3[10]\n[end]";  //last row
        Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results.substring(results.length() - 200));

        //.asc test of just one station, FOR ALL TIME
        if (doLongTest) {
            userDapQuery = "longitude,latitude,station_id,time,sea_water_temperature" +
                "&longitude=" + tLon + "&latitude=" + tLat; 
            tName = mwfs.makeNewFileForDapQuery(null, userDapQuery, EDStatic.fullTestCacheDirectory, className + "_station1", ".asc"); 
            results = String2.annotatedString(new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray()));
            //String2.log(results);
            expected = 
"Dataset {[10]\n" +
"  Sequence {[10]\n" +
"    Float64 longitude;[10]\n" +
"    Float64 latitude;[10]\n" +
"    String station_id;[10]\n" +
"    Float64 time;[10]\n" +
"    Float32 sea_water_temperature;[10]\n" +
"  } s;[10]\n" +
"} s;[10]\n" +
"---------------------------------------------[10]\n" +
"s.longitude, s.latitude, s.station_id, s.time, s.sea_water_temperature[10]\n" +
"-70.43, 38.48, \"44004\", 1.1676132E9, 18.2[10]\n";
            Test.ensureTrue(results.startsWith(expected), "\nresults=\n" + results.substring(0, 600));
            expected = 
"-70.43, 38.48, \"44004\", 1.1885934E9, 25.5[10]\n[end]";  //last row
            Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results.substring(results.length() - 200));
        }
        
        //.asc test of ALL STATIONS   one time point
        userDapQuery = "longitude,latitude,time,station_id,sea_water_temperature" +
            "&time>=2007-05-01&time<=2007-05-01T00:59:00";
        //2007-05-01 -> 1.1779776E9
        //2007-05-01T00:59:00 -> 1.17798114E9
        tName = mwfs.makeNewFileForDapQuery(null, userDapQuery, EDStatic.fullTestCacheDirectory, className + "_stations", ".asc"); 
        results = String2.annotatedString(new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray()));
        //SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
        //String2.log(results);
        expected = 
"Dataset {[10]\n" +
"  Sequence {[10]\n" +
"    Float64 longitude;[10]\n" +
"    Float64 latitude;[10]\n" +
"    Float64 time;[10]\n" +
"    String station_id;[10]\n" +
"    Float32 sea_water_temperature;[10]\n" +
"  } s;[10]\n" +
"} s;[10]\n" +
"---------------------------------------------[10]\n" +
"s.longitude, s.latitude, s.time, s.station_id, s.sea_water_temperature[10]\n" +
"-76.95, 34.21, 1.1779788E9, \"41036\", 21.0[10]\n";
      Test.ensureTrue(results.indexOf(expected) == 0, "\nresults=\n" + results.substring(0, 1000));
        expected = 
"-81.11, 24.63, 1.1779776E9, \"SMKF1\", 29.0[10]\n";  //middle row
        Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results.substring(results.length() - 3000));
        expected = 
"-80.22, 27.55, 1.17798084E9, \"41114\", 24.2[10]\n[end]";  //last row
        Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results.substring(results.length() - 300));

        //data for mapExample
        tName = mwfs.makeNewFileForDapQuery(null, "longitude,latitude&time>=2007-12-01&time<=2007-12-01T00:01:00", EDStatic.fullTestCacheDirectory, className + "Map", ".csv");
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"longitude, latitude\n" +
"degrees_east, degrees_north\n" +
"-79.7, 28.85\n" +
"-79.7, 28.85\n" +
"-79.7, 28.85\n" +
"-81.9, 32.03\n" +
"-81.9, 32.03\n" +
"-81.9, 32.03\n" +
"-96.4, 28.45\n" +
"-76.67, 34.72\n" +
"-93.87, 29.73\n" +
"-80.1, 25.59\n" +
"-80.86, 24.84\n" +
"-90.67, 27.8\n" +
"-82.45, 27.07\n" +
"-81.81, 26.13\n" +
"-75.75, 36.18\n" +
"-78.51, 33.87\n" +
"-80.41, 32.28\n" +
"-80.41, 32.28\n" +
"-80.41, 32.28\n" +
"-97.05, 27.83\n" +
"-95.31, 28.95\n" +
"-80.38, 25.01\n" +
"-75.55, 35.8\n" +
"-81.81, 24.55\n" +
"-81.47, 30.67\n" +
"-81.26, 29.86\n" +
"-78.92, 33.66\n" +
"-81.11, 24.63\n" +
"-77.8, 34.21\n" +
"-97.22, 27.58\n" +
"-85.88, 30.21\n" +
"-84.86, 29.41\n" +
"-81.87, 26.65\n" +
"-97.22, 26.06\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);  

        //expected error didn't occur!
        String2.getStringFromSystemIn("\n" + 
            MustBe.getStackTrace() + 
            "An expected error didn't occur at the above location.\n" + 
            "Press ^C to stop or Enter to continue..."); 

        } catch (Throwable t) {
            String2.getStringFromSystemIn(
                "*** EXPECTED ERROR first appeared 2008-09-04: " + MustBe.throwableToString(t) + 
                "\nPress ^C to stop or Enter to continue..."); 
        }

    }

}
