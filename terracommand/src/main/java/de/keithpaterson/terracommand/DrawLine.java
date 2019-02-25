package de.keithpaterson.terracommand;
import java.awt.Color;
import java.util.ArrayList;

import org.geotools.data.DataUtilities;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.map.MapContent;
import org.geotools.styling.SLD;
import org.geotools.styling.Style;
import org.geotools.swing.JMapFrame;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

public class DrawLine {
    public Layer getLayerLineByCoord(Coordinate[] coords) throws SchemaException {
        GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
        LineString line = geometryFactory.createLineString(coords);
        SimpleFeatureType TYPE = DataUtilities.createType("test", "line", "the_geom:LineString");
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder((SimpleFeatureType) TYPE);
        featureBuilder.add(line);
        SimpleFeature feature = featureBuilder.buildFeature("LineString_Sample");

        DefaultFeatureCollection lineCollection = new DefaultFeatureCollection();
        lineCollection.add(feature);

        Style style = SLD.createLineStyle(Color.BLUE, 1);
        return new FeatureLayer(lineCollection, style);
    }

    public static void main(String[] args) throws SchemaException {
      MapContent map = new MapContent();
      map.setTitle("IslandFinder");

      DrawLine line = new DrawLine();
      LineString ls = createRandomLineString(10);
      Layer layer = line.getLayerLineByCoord(ls.getCoordinates());
      map.addLayer(layer);


      // Now display the map
      JMapFrame.showMap(map);

    }
    
    /**
     * @return
     */
    public static LineString createRandomLineString(int n) {
      double latitude = (Math.random() * 180.0) - 90.0;
      double longitude = (Math.random() * 360.0) - 180.0;
      GeometryFactory geometryFactory = new GeometryFactory();
      /* Longitude (= x coord) first ! */
      ArrayList<Coordinate> points = new ArrayList<Coordinate>();
      points.add(new Coordinate(longitude, latitude));
      for (int i = 1; i < n; i++) {
        double deltaX = (Math.random() * 10.0) - 5.0;
        double deltaY = (Math.random() * 10.0) - 5.0;
        longitude += deltaX;
        latitude += deltaY;
        points.add(new Coordinate(longitude, latitude));
      }
      LineString line = geometryFactory.createLineString((Coordinate[]) points.toArray(new Coordinate[] {}));
      return line;
    }
}