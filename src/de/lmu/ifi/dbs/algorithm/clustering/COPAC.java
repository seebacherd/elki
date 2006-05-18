package de.lmu.ifi.dbs.algorithm.clustering;

import de.lmu.ifi.dbs.algorithm.Algorithm;
import de.lmu.ifi.dbs.algorithm.result.PartitionResults;
import de.lmu.ifi.dbs.algorithm.result.clustering.ClusteringResult;
import de.lmu.ifi.dbs.algorithm.result.clustering.PartitionClusteringResults;
import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.properties.Properties;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Algorithm to partition a database according to the correlation dimension of
 * its objects and to then perform an arbitrary algorithm over the partitions.
 *
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class COPAC extends COPAA implements Clustering<RealVector> {
  /**
   * Holds the class specific debug status.
   */
  @SuppressWarnings({"unused", "UNUSED_SYMBOL"})
  private static final boolean DEBUG = LoggingConfiguration.DEBUG;

  /**
   * The logger of this class.
   */
  private Logger logger = Logger.getLogger(this.getClass().getName());

  /**
   * Description for parameter partitioning algorithm
   */
  public static final String PARTITION_ALGORITHM_D = "<class>algorithm to apply to each partition " +
                                                     Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(Clustering.class) +
                                                     ".";

  /**
   * Sets the specific parameters additionally to the parameters set by the
   * super-class.
   */
  public COPAC() {
    super();
    // put in the right description
    parameterToDescription.remove(COPAA.PARTITION_ALGORITHM_P + OptionHandler.EXPECTS_VALUE);
    parameterToDescription.put(COPAA.PARTITION_ALGORITHM_P + OptionHandler.EXPECTS_VALUE, PARTITION_ALGORITHM_D);
    optionHandler = new OptionHandler(parameterToDescription, this.getClass().getName());
  }

  /**
   * Passes remaining parameters first to the partition algorithm, then to the
   * preprocessor.
   *
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  @Override
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);
    if (!(getPartitionAlgorithm() instanceof Clustering)) {
      throw new WrongParameterValueException(PARTITION_ALGORITHM_P,
                                             optionHandler.getOptionValue(PARTITION_ALGORITHM_P),
                                             PARTITION_ALGORITHM_D);
    }
    setParameters(args, remainingParameters);
    return remainingParameters;
  }

  /**
   * @see Clustering#getResult()
   */
  public ClusteringResult<RealVector> getResult() {
    return (ClusteringResult<RealVector>) super.getResult();
  }

  /**
   * @see Algorithm#getDescription()
   */
  public Description getDescription() {
    return new Description(
    "COPAC",
    "COrrelation PArtition Clustering",
    "Partitions a database according to the correlation dimension of its objects and performs a clustering algorithm over the partitions.",
    "unpublished");
  }

  /**
   * Runs the partition algorithm and creates the result.
   *
   * @param database     the database to run this algorithm on
   * @param partitionMap the map of partition IDs to object ids
   */
  protected PartitionResults<RealVector> runPartitionAlgorithm(
  Database<RealVector> database,
  Map<Integer, List<Integer>> partitionMap) {
    try {
      Map<Integer, Database<RealVector>> databasePartitions = database
      .partition(partitionMap, partitionDatabase,
                 partitionDatabaseParameters);
      Map<Integer, ClusteringResult<RealVector>> results = new Hashtable<Integer, ClusteringResult<RealVector>>();
      Clustering<RealVector> partitionAlgorithm = (Clustering<RealVector>) getPartitionAlgorithm();
      for (Integer partitionID : databasePartitions.keySet()) {
        if (isVerbose()) {
          logger.info("\nRunning "
                      + partitionAlgorithm.getDescription()
          .getShortTitle() + " on partition "
                           + partitionID + "\n");
        }
        partitionAlgorithm.run(databasePartitions.get(partitionID));
        results.put(partitionID, partitionAlgorithm.getResult());
      }
      return new PartitionClusteringResults<RealVector>(database,
                                                        results, database.dimensionality());
    }
    catch (UnableToComplyException e) {
      throw new IllegalStateException(e);
    }
  }

}
