package de.keithpaterson.terracommand;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.transaction.Transactional;

import org.geotools.data.DataUtilities;
import org.geotools.data.shapefile.ShapefileDumper;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.map.Layer;
import org.geotools.map.MapContent;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

import de.keithpaterson.terracommand.entities.Island;
import de.keithpaterson.terracommand.entities.IslandRepository;
import de.keithpaterson.terracommand.entities.OneDegreeBucket;
import de.keithpaterson.terracommand.entities.OneDegreeBucketRepository;
import de.keithpaterson.terracommand.entities.TenDegreeBucket;
import de.keithpaterson.terracommand.entities.TenDegreeBucketRepository;
import de.keithpaterson.terracommand.entities.Tile;
import de.keithpaterson.terracommand.entities.TileRepository;

@Configuration
@EnableBatchProcessing
@Component
public class IslandFinderTasklet implements Runnable, Tasklet, InitializingBean {

	public class LoggingShapefileDumper extends ShapefileDumper {
		Logger logger = Logger.getLogger(LoggingShapefileDumper.class.getName());

		public LoggingShapefileDumper(File targetDirectory) {
			super(targetDirectory);
		}

		@Override
		protected void shapefileDumped(String fileName, SimpleFeatureType remappedSchema) throws IOException {
			logger.info("Dumped to " + fileName);
			super.shapefileDumped(fileName, remappedSchema);
		}
	}

	private static final String TERRAIN_FILE = "([0-9]{6,10})\\.btg\\.gz";
	private static final String AIRPORT_FILE = "([0-9A-Z]{3,4})\\.btg\\.gz";
	private static final String STG_FILE = "([0-9]{6,10})\\.stg";

	Logger logger = Logger.getLogger(IslandFinderTasklet.class.getName());

	private Properties props = new Properties();

	IndexMap indexMap = new IndexMap();

//	ArrayList<Island> buckets = new ArrayList<>();

	@Autowired
	TileRepository tileRepo;

	@Autowired
	IslandRepository islandRepo;

	@Autowired
	OneDegreeBucketRepository oneDegreeBucketRepository;

	@Autowired
	TenDegreeBucketRepository tenDegreeBucketRepository;
	
	@Value("${terrasync.path}")
	private String basePath;

	public void run() {
		loadProperties();

		buildIndex();

		walkDirectory();
		mergeIslands();
		drawBoxes();
	}

	public void loadProperties() {
		try {
			getProps().load(new FileReader("terramaster.properties"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public ExitStatus buildIndex() {
		Point2D.Double d = new Point2D.Double(179, 89);
		while (d.y >= -90) {
			d.x = 179;
			while (d.x >= -180) {
				Tile tile = new Tile(d);
				indexMap.put(tile.getTileIndex(), tile);

				if (tileRepo.findByTileIndex(tile.getTileIndex()) == null) {
					tile = tileRepo.save(tile);
				}
				logger.info(d.toString() + "\t" + tile + "\t" + tile.getTileIndex());
				d.x -= tile.getTileWidth();

			}
			d.y -= 0.125;
		}
		return ExitStatus.COMPLETED;
	}

	@Transactional
	public Path walkDirectory() {
		String path = basePath + File.separator + "Terrain";

		Path p = Paths.get(path);
		try {
			Files.walk(p).forEach((p1) -> {
				add(p1);
			});

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return p;
	}

	@Transactional
	public void mergeIslands() {
		int progress = 0;
		boolean clean = false;
		while (!clean) {
			Island newIsland = null;
			int currentProgress = 0;
			for (Tile tileIndex : tileRepo.findAll()) {
				currentProgress += 1;
				newIsland = null;
				ArrayList<Integer> neighbours = tileIndex.getNeighbours();
				for (Integer neighbourIndex : neighbours) {
					Tile neighbourTile = tileRepo.findByTileIndex(neighbourIndex);
					// Tile neighbourTile = new Tile(neighbourIndex);
					if (neighbourTile.getIsland() != null) {
						if (tileIndex.getIsland() != null && !tileIndex.equals(neighbourTile.getIsland())
								&& tileIndex.getIsland().hasTiles() && neighbourTile.getIsland().hasTiles()) {
							newIsland = stitch(tileIndex.getIsland(), neighbourTile.getIsland());
							break;
						}
					}
				}
				if (newIsland != null)
					break;
			}
			if (newIsland == null || currentProgress == progress)
				clean = true;
			progress = currentProgress;
		}
	}

	public void drawBoxes() {

		try {
			File targetDirectory = new File("target/demo");
			File[] files = targetDirectory.listFiles();

			if (files != null) {
				for (int i = 0; i < files.length; i++) {
					files[i].delete();
				}
			}

			MapContent map = new MapContent();
			map.setTitle("IslandFinder");

			DrawLine line = new DrawLine();

			for (Island bucket : islandRepo.findAll()) {
				if (!bucket.hasTiles())
					continue;
				ArrayList<LineString> lines = new ArrayList<>();
				for (Tile tileIndex : bucket.getTiles()) {
					LineString ls = drawTile(tileIndex);
					lines.add(ls);
					Layer layer = line.getLayerLineByCoord(ls.getCoordinates());
					map.addLayer(layer);
				}
				if (lines.isEmpty())
					continue;
				SimpleFeatureType schema = null;
				try {
					schema = DataUtilities.createType("Island_" + bucket.getIslandId(),
							"centerline:LineString,name:\"\",id:0");
				} catch (SchemaException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return;
				}

				writeFile(extractFeatureCollection(bucket.getTiles(), schema));
			}

//			MapViewport viewport = map.getViewport();
//			viewport.setBounds(map.getMaxBounds());
//			map.setViewport(viewport);
//
//			// Now display the map
//			JMapFrame.showMap(map);
		} catch (SchemaException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @param list2
	 * @param schema
	 * @return
	 */

	private SimpleFeatureCollection extractFeatureCollection(List<Tile> list2, SimpleFeatureType schema) {
		long id = 0;
		ArrayList<SimpleFeature> list = new ArrayList<>();
		for (Tile tileIndex : list2) {
			LineString lineString = drawTile(tileIndex);

			SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(schema);
			featureBuilder.add(lineString);
			String name = "Tile : " + tileIndex.getTileIndex();
			int number = (int) Math.round(Math.random() * 10.0);

			featureBuilder.add(name);
			featureBuilder.add(number);
			SimpleFeature feature = featureBuilder.buildFeature("" + (id++));
			list.add(feature);
		}
		SimpleFeatureCollection collection = DataUtilities.collection(list);

		return collection;
	}

	private void writeFile(SimpleFeatureCollection fc) throws IOException, SchemaException {
		File targetDirectory = new File("target/demo");
		targetDirectory.mkdirs();
		ShapefileDumper dumper = new LoggingShapefileDumper(targetDirectory);
		// optiona, set a target charset (ISO-8859-1 is the default)
		dumper.setCharset(Charset.forName("ISO-8859-15"));
		// split when shp or dbf reaches 100MB
		int maxSize = 100 * 1024 * 1024;
		dumper.setMaxDbfSize(maxSize);
		dumper.setMaxDbfSize(maxSize);
		// actually dump data
		dumper.dump(fc);

	}

	private LineString drawTile(Tile tileIndex) {
		double width = tileIndex.getTileWidth();
		double latitude = tileIndex.getLatitude();
		double longitude = tileIndex.getLongitude();
		GeometryFactory geometryFactory = new GeometryFactory();
		/* Longitude (= x coord) first ! */
		ArrayList<Coordinate> points = new ArrayList<Coordinate>();
		points.add(new Coordinate(longitude, latitude));
		points.add(new Coordinate(longitude + width, latitude));
		points.add(new Coordinate(longitude + width, latitude + 0.125));
		points.add(new Coordinate(longitude, latitude + 0.125));
		points.add(new Coordinate(longitude, latitude));
		LineString line = geometryFactory.createLineString((Coordinate[]) points.toArray(new Coordinate[] {}));
		return line;
	}

	private void add(Path p) {
		if (!Files.isDirectory(p)) {
			Matcher m = Pattern.compile(TERRAIN_FILE).matcher(p.getFileName().toString());
			if (m.find()) {
				addTerrainFile(p, m);
				return;
			}
			m = Pattern.compile(AIRPORT_FILE).matcher(p.getFileName().toString());
			if (m.find()) {
				airportFile(p, m);
				return;
			}
			m = Pattern.compile(STG_FILE).matcher(p.getFileName().toString());
			if (m.find()) {
				return;
			}
			if (p.getFileName().toString().equals(".dirindex"))
				return;
			logger.warning(p.toString() + " not recognised");
		}
	}

	private void airportFile(Path p, Matcher m) {
		// TODO Auto-generated method stub

	}

	private void addTerrainFile(Path p, Matcher m) {
		int index = Integer.parseInt(m.group(1));
		logger.info("Adding " + index);
		Tile tileIndex = getOrCreateTileIndex(index);

		tileIndex.setTerrain(true);
		ArrayList<Integer> neighbours = tileIndex.getNeighbours();
		Island island = null;
		for (Integer neighbourIndex : neighbours) {
			Tile neighbourTile = getOrCreateTileIndex(neighbourIndex);
			if (neighbourTile.getIsland() != null) {
				if (island != null && !island.equals(neighbourTile.getIsland())) {
					island = stitch(island, neighbourTile.getIsland());
				} else {
					island = neighbourTile.getIsland();
				}
			}
		}
		if (island != null) {
			island.add(tileIndex);
			tileIndex.setIsland(island);
			island = islandRepo.save(island);
		} else {
			island = new Island();
			logger.info("Found a new island : " + island);
			tileIndex.setIsland(island);
			tileIndex = tileRepo.save(tileIndex);
			Island bucket = island;
			bucket.add(tileIndex);

			for (Integer neighbourIndex : neighbours) {
				Tile neighbourTile = getOrCreateTileIndex(neighbourIndex);
				neighbourTile.setIsland(island);
				neighbourTile = tileRepo.save(neighbourTile);
				bucket.add(neighbourTile);
			}
			bucket = islandRepo.save(bucket);
		}
	}

	private Tile getOrCreateTileIndex(Integer tileIndex) {
		Tile neighbourTile = tileRepo.findByTileIndex(tileIndex);
		if (neighbourTile == null) {
			neighbourTile = tileRepo.save(new Tile(tileIndex));
		}
		if (true || neighbourTile.getTenDegreeBucket() == null) {
			TenDegreeBucket bucket = tenDegreeBucketRepository
					.getByName(TenDegreeBucket.calcName(neighbourTile.getLongitude(), neighbourTile.getLatitude()));
			if (bucket == null) {
				bucket = new TenDegreeBucket(
						TenDegreeBucket.calcName(neighbourTile.getLongitude(), neighbourTile.getLatitude()));
				bucket = tenDegreeBucketRepository.save(bucket);
			}
			neighbourTile.setTenDegreeBucket(bucket);
			tileRepo.save(neighbourTile);
		}
		if (true || neighbourTile.getOneDegreeBucket() == null) {
			OneDegreeBucket bucket = oneDegreeBucketRepository
					.getByName(OneDegreeBucket.calcName(neighbourTile.getLongitude(), neighbourTile.getLatitude()));
			if (bucket == null) {
				bucket = new OneDegreeBucket(
						OneDegreeBucket.calcName(neighbourTile.getLongitude(), neighbourTile.getLatitude()));
				bucket = oneDegreeBucketRepository.save(bucket);
			}
			neighbourTile.setOneDegreeBucket(bucket);
			tileRepo.save(neighbourTile);
		}
		return neighbourTile;
	}

	private Island stitch(Island islandIndex, Island island) {
		logger.info("Merging IslandIndex " + islandIndex + " and " + island);
		Island merged = new Island();
		for (Tile tile : islandIndex.getTiles()) {
			merged.add(tile);
		}
		for (Tile tile : island.getTiles()) {
			merged.add(tile);
		}
		merged = islandRepo.save(merged);
		return merged;
	}

	private Properties getProps() {
		return props;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

}
