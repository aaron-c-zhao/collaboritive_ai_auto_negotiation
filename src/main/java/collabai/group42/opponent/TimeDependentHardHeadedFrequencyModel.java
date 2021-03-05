package collabai.group42.opponent;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

import geniusweb.actions.Action;
import geniusweb.actions.Offer;
import geniusweb.issuevalue.Bid;
import geniusweb.issuevalue.DiscreteValue;
import geniusweb.issuevalue.DiscreteValueSet;
import geniusweb.issuevalue.Domain;
import geniusweb.issuevalue.NumberValue;
import geniusweb.issuevalue.NumberValueSet;
import geniusweb.issuevalue.Value;
import geniusweb.issuevalue.ValueSet;
import geniusweb.opponentmodel.OpponentModel;
import geniusweb.profile.utilityspace.DiscreteValueSetUtilities;
import geniusweb.profile.utilityspace.LinearAdditiveUtilitySpace;
import geniusweb.profile.utilityspace.NumberValueSetUtilities;
import geniusweb.profile.utilityspace.ValueSetUtilities;
import geniusweb.progress.Progress;
import org.apache.commons.math3.analysis.function.Gaussian;
import org.apache.commons.math3.distribution.LaplaceDistribution;
import org.apache.commons.math3.distribution.TriangularDistribution;

/**
 * HardHeadedFrequency Model.
 * <p>
 * Default: learning coef l = 0.2; learnValueAddition v = 1.0
 * <p>
 * paper: https://ii.tudelft.nl/sites/default/files/boa.pdf
 */
public class HardHeadedFrequencyModel implements Group42OpponentModel {

    private static final int DECIMALS = 4; // accuracy of our computations.
    private final Map<String, Issue> utilitySpace;
    private final Bid resBid;
    private final Domain domain;
    private final Map<String, ValueSetUtilities> utils;
    private final Map<String, BigDecimal> weights;
    private final Bid previousBid;
    private final BigDecimal learnCoef;
    private final BigDecimal learnValueAddition;
    private final BigDecimal amountOfIssues;
    private final BigDecimal goldenValue;

    public HardHeadedFrequencyModel(Domain domain, Map<String, Issue> utilitySpace, Map<String, ValueSetUtilities> utils, Map<String, BigDecimal> weights, BigDecimal learnCoef, BigDecimal learnValueAddition, BigDecimal amountOfIssues, BigDecimal goldenValue, Bid resBid, Bid previousBid) {
        this.domain = domain;
        this.utilitySpace = utilitySpace;
        this.utils = utils;
        this.weights = weights;
        this.learnCoef = learnCoef;
        this.learnValueAddition = learnValueAddition;
        this.amountOfIssues = amountOfIssues;
        this.goldenValue = goldenValue;
        this.resBid = resBid;
        this.previousBid = previousBid;
    }

    private static Map<String, Issue> cloneMap(Map<String, Issue> utilitySpace) {
        return new HashMap<>(utilitySpace);
        // Map<String, Issue> result = new HashMap<>();
        // for (String issue : utilitySpace.keySet()) {
        //     result.put(issue, utilitySpace.get(issue).clone());
        // }
        // return result;
    }

    /**
     * Initializes the model. This function must be called first after
     * constructing an instance. It can also be called later, if there is a
     * change in the domain or resBid.
     * <p>
     * This late-initialization is to support boa models that have late
     * initialization.
     *
     * @param domain the domain to work with. Must be not null.
     * @param resBid the reservation bid, or null if no reservationbid is
     *               available.
     * @return OpponentModel that uses given domain and reservationbid.
     */
    @Override
    public OpponentModel with(Domain domain, Bid resBid) {
        BigDecimal learnCoef = new BigDecimal("0.2");
        BigDecimal learnValueAddition = BigDecimal.ONE;
        BigDecimal amountOfIssues = new BigDecimal(domain.getIssues().size());
        BigDecimal goldenValue = learnCoef.divide(amountOfIssues, DECIMALS, BigDecimal.ROUND_HALF_UP);
        BigDecimal commonWeight = BigDecimal.ONE.divide(amountOfIssues, DECIMALS, BigDecimal.ROUND_HALF_UP);

        HashMap<String, Issue> us = new HashMap<>();
        Map<String, ValueSetUtilities> utils = new HashMap<>();
        Map<String, BigDecimal> weights = new HashMap<>();
        for (String issue : domain.getIssues()) {
            // for utils and weights
            weights.put(issue, commonWeight);
            ValueSet vs = domain.getValues(issue);
            if (vs instanceof DiscreteValueSet) {
                DiscreteValueSet dvs = (DiscreteValueSet) vs;
                dvs.getValues();

                Map<DiscreteValue, BigDecimal> discreteEstimation = new HashMap<>();
                for (Value x : domain.getValues(issue)) {
                    discreteEstimation.put((DiscreteValue) x, BigDecimal.ONE);
                }
                utils.put(issue, new DiscreteValueSetUtilities(discreteEstimation));
            }

            if (vs instanceof NumberValueSet) {
                NumberValueSet nvs = (NumberValueSet) vs;
                KDE kde = new KDE(nvs.getRange().getLow(), nvs.getRange().getHigh(), nvs.getRange().getStep());
                utils.put(issue, kde);
            }
            // ----

            Issue newIssue = new Issue();

            for (Value x : domain.getValues(issue)) {
                newIssue.values.put(x, BigDecimal.ONE);
            }
            newIssue.weight = commonWeight;
            us.put(issue, newIssue);
        }

        return new HardHeadedFrequencyModel(domain, us, utils, weights, learnCoef, learnValueAddition, amountOfIssues, goldenValue, resBid, null);
    }

    /**
     * Update this with a new action that was done by the opponent that this
     * model is modeling. {@link #with(Domain, Bid)} must be called before
     * calling this.
     *
     * @param action   the new incoming action.
     * @param progress the current progress of the negotiation. Calls to this
     *                 must be done with increasing progress.
     * @return the updated {@link OpponentModel}
     */
    @Override
    public OpponentModel with(Action action, Progress progress) {
        if (domain == null) {
            throw new IllegalStateException("domain is not initialized");
        }

        if (!(action instanceof Offer)) {
            return this;
        }

        Bid newBid = ((Offer) action).getBid();
        double time = progress.get(System.currentTimeMillis());

        if (previousBid != null) {
            int numberUnchanged = 0;
            // TODO change to boolean
            HashMap<String, Integer> lastDiffSet = determineDifference(previousBid, newBid);
            for (String issue : lastDiffSet.keySet()) {
                if (lastDiffSet.get(issue) == 0) {
                    numberUnchanged++;
                }
            }

            BigDecimal totalSum = BigDecimal.ONE.add(goldenValue.multiply(new BigDecimal(numberUnchanged)));
            BigDecimal maximumWeight = BigDecimal.ONE.subtract(amountOfIssues.multiply(goldenValue).divide(totalSum, DECIMALS, BigDecimal.ROUND_HALF_UP));

            // TODO make utils and weights be final (make clone and edit)
            for (String issue : lastDiffSet.keySet()) {
                BigDecimal weight = weights.get(issue);
                BigDecimal newWeight;

                if (lastDiffSet.get(issue) == 0 && maximumWeight.compareTo(weight) > 0) {
                    newWeight = weight.add(goldenValue).divide(totalSum, DECIMALS, BigDecimal.ROUND_HALF_UP);
                } else {
                    newWeight = weight.divide(totalSum, DECIMALS, BigDecimal.ROUND_HALF_UP);
                }

                weights.put(issue, newWeight);
            }

            newBid.getIssueValues().forEach((issue, value) -> {
                if (value instanceof NumberValue) {
                    ((KDE) utils.get(issue)).addValue(value, weights.get(issue), time);
                } else if (value instanceof DiscreteValue) {
                    ((DiscreteUtilities) utils.get(issue)).addValue(value, learnValueAddition, time);
                }
            });
        }

        // ------ other version
        // Map<String, Issue> newUtilitySpace = cloneMap(utilitySpace);
        //
        // if (previousBid != null) {
        //     int numberUnchanged = 0;
        //     HashMap<String, Integer> lastDiffSet = determineDifference(previousBid, newBid);
        //     for (String issue : lastDiffSet.keySet()) {
        //         if (lastDiffSet.get(issue) == 0) {
        //             numberUnchanged++;
        //         }
        //     }
        //
        //     BigDecimal totalSum = BigDecimal.ONE.add(goldenValue.multiply(new BigDecimal(numberUnchanged)));
        //     BigDecimal maximumWeight = BigDecimal.ONE.subtract(amountOfIssues.multiply(goldenValue).divide(totalSum, DECIMALS, BigDecimal.ROUND_HALF_UP));
        //
        //     for (String issue : lastDiffSet.keySet()) {
        //         BigDecimal weight = newUtilitySpace.get(issue).weight;
        //         BigDecimal newWeight;
        //
        //         if (lastDiffSet.get(issue) == 0 && maximumWeight.compareTo(weight) > 0) {
        //             newWeight = weight.add(goldenValue).divide(totalSum, DECIMALS, BigDecimal.ROUND_HALF_UP);
        //         } else {
        //             newWeight = weight.divide(totalSum, DECIMALS, BigDecimal.ROUND_HALF_UP);
        //         }
        //
        //         newUtilitySpace.get(issue).weight = newWeight;
        //     }
        //
        //     newBid.getIssueValues().forEach((issue, value) -> {
        //         BigDecimal eval = newUtilitySpace.get(issue).values.get(value);
        //         newUtilitySpace.get(issue).values.put(value, eval.add(learnValueAddition));
        //     });
        // }

        return new HardHeadedFrequencyModel(domain, utilitySpace, utils, weights, learnCoef, learnValueAddition, amountOfIssues, goldenValue, resBid, newBid);
    }

    private HashMap<String, Integer> determineDifference(Bid first, Bid second) {
        HashMap<String, Integer> diff = new HashMap<>();
        for (String issue : domain.getIssues()) {
            Value v1 = first.getValue(issue);
            Value v2 = second.getValue(issue);
            diff.put(issue, (v1.equals(v2) ? 0 : 1));
        }
        return diff;
    }

    /**
     * @param bid the {@link Bid} to be evaluated
     * @return the utility value of this bid. This MUST return a number in the range
     * [0,1]. 0 means preferred the least and 1 means preferred the most.
     */
    @Override
    public BigDecimal getUtility(Bid bid) {
        return getUtilitySpace().getUtility(bid);
    }

    private LinearAdditiveUtilitySpace getUtilitySpace() {
        return new LinearAdditiveUtilitySpace(domain, "Opponent", utils, weights, resBid);
    }

    /**
     * @return the name of this profile. Must be simple name (a-Z, 0-9)
     */
    @Override
    public String getName() {
        return "Hard Headed Frequency Model";
    }

    /**
     * @return the domain in which this profile is defined.
     */
    @Override
    public Domain getDomain() {
        return domain;
    }

    /**
     * @return a (hypothetical) bid that is the best alternative to a
     * non-agreement. Only bids that are equal or better should be
     * accepted. If a negotiation does not reach an agreement, the party
     * can get this offer somewhere else. This replaces the older notion
     * of a "reservation value" and is more general. If null, there is
     * no reservation bid and any agreement is better than no agreement.
     */
    @Override
    public Bid getReservationBid() {
        return resBid;
    }

    private static class Issue implements Cloneable {
        public BigDecimal weight;
        public HashMap<Value, BigDecimal> values;

        @Override
        public Issue clone() {
            Issue newIssue = new Issue();
            newIssue.weight = weight;
            newIssue.values = new HashMap<>();
            for (Value value : values.keySet()) {
                newIssue.values.put(value, values.get(value));
            }
            return newIssue;
        }
    }

    private static class DiscreteUtilities implements ValueSetUtilities {

        private HashMap<DiscreteValue, BigDecimal> valueUtilities = new HashMap<>();

        /**
         * create new object based on the given mapping from values to utilities.
         *
         * @param valueUtils map with key {@link DiscreteValue}s and value a Double
         *                   in the range [0,1].
         * @throws NullPointerException     if one of the args is null
         * @throws IllegalArgumentException if values are not in range [0,1].
         */
        public DiscreteUtilities(
          Map<DiscreteValue, BigDecimal> valueUtils) {
            if (valueUtils == null) {
                throw new NullPointerException("valueUtils==null");
            }

            this.valueUtilities.putAll(valueUtils);
        }

        public void addValue(Value value, BigDecimal amount, double time) {
            if (!(value instanceof DiscreteValue)) {
                return;
            }
            BigDecimal newAmount = getTimeBasedIncrement(amount, time);
            valueUtilities.put((DiscreteValue) value, valueUtilities.get(value).add(newAmount));
            normalize(newAmount);
        }

        /**
         * As time goes e^(-5x)
         */
        private BigDecimal getTimeBasedIncrement(BigDecimal amount, double time) {
            return amount.multiply(BigDecimal.valueOf(Math.exp(-5 * time)));
        }

        // TODO make amount time dependent
        private void normalize(BigDecimal amount) {
            BigDecimal div = BigDecimal.ONE.add(amount);
            valueUtilities.replaceAll((d, v) -> valueUtilities.get(d).divide(div, DECIMALS, BigDecimal.ROUND_HALF_UP));
        }

        @Override
        public BigDecimal getUtility(Value value) {
            if (!valueUtilities.containsKey(value)) {
                return BigDecimal.ZERO;
            }
            return valueUtilities.get(value);
        }

        /**
         * @return copy of the value-utility pair map.
         */
        public Map<DiscreteValue, BigDecimal> getUtilities() {
            return Collections.unmodifiableMap(valueUtilities);
        }

        @Override
        public String isFitting(ValueSet valueset) {
            if (!(valueset instanceof DiscreteValueSet)) {
                return "The utilities are for a discrete valueset but the given values are "
                  + valueset;
            }
            DiscreteValueSet discvalueset = (DiscreteValueSet) valueset;
            if (!valueUtilities.keySet()
              .equals(new HashSet<>(discvalueset.getValues())))
                return "The values in the set " + valueset
                  + " do not match the values mapped to utilities "
                  + valueUtilities.keySet();
            return null;
        }
    }

    /**
     * Use KDE to estimate number value set utility.
     * 1. choose appropriate kernel
     * 2. calculate appropriate bandwidth and amplitude
     * 3. perform KDE each time we receive a value
     * 4. apply time dependent decreasing function to change amplitude f
     * 5. use lowest estimate as lowerUtility and highest estimate as upperUtility
     */
    private static class KDE implements ValueSetUtilities {
        private static final double MININTENSITY = 0.75;
        private BigDecimal lowValue, highValue, step;
        private BigDecimal numValues, totalArea;
        private BigDecimal lowerUtility, upperUtility;
        private BigDecimal triangularNormalRange, triangularVariation;
        private LinkedHashMap<NumberValue, BigDecimal> values;

        public KDE(BigDecimal lowerBound, BigDecimal upperBound, BigDecimal step) {
            this.lowValue = lowerBound;
            this.highValue = upperBound;
            this.step = step;
            this.numValues = highValue.subtract(lowValue).divideToIntegralValue(step);
            this.triangularNormalRange = highValue.subtract(lowValue).divide(BigDecimal.valueOf(2), DECIMALS, BigDecimal.ROUND_HALF_UP);
            this.triangularVariation = highValue.subtract(lowValue).divide(BigDecimal.valueOf(4), DECIMALS, BigDecimal.ROUND_HALF_UP);
            this.values = new LinkedHashMap<>(numValues.intValue());

            // set uniform distribution
            totalArea = BigDecimal.ONE;
            BigDecimal weight = BigDecimal.ONE.divide(numValues, DECIMALS, BigDecimal.ROUND_HALF_UP);
            for (BigDecimal i = lowValue; i.compareTo(highValue) < 0; i = i.add(step)) {
                values.put(new NumberValue(i), weight);
            }
            lowerUtility = weight;
            upperUtility = weight;
        }

        /**
         * -3sigma ~ 3sigma
         *
         * @param value
         */
        public void addValue(Value value, BigDecimal issueWeight, double progress) {
            if (!(value instanceof NumberValue)) {
                return;
            }

            BigDecimal center = ((NumberValue) value).getValue();
            BigDecimal bandwidth = mapIssueWeight(issueWeight);
            BigDecimal factor = mapProgressToBandwidth(progress);
            TriangularDistribution td = getTriangular(center, bandwidth);

            // add probability for center (only once)
            BigDecimal probCenter = factor.multiply(BigDecimal.valueOf(td.probability(center.doubleValue())));
            values.computeIfPresent(new NumberValue(center), (numberValue, bigDecimal) -> {
                totalArea = totalArea.add(probCenter);
                BigDecimal result = bigDecimal.add(probCenter); 
                if(result.compareTo(upperUtility) > 0) {
                    upperUtility = result;
                }
                return result;
            });

            // add probabilities to both sides of center
            for (BigDecimal i = step; i.compareTo(bandwidth) < 0; i = i.add(step)) {
                BigDecimal bucket = center.subtract(i);
                BigDecimal prob = factor.multiply(BigDecimal.valueOf(td.probability(bucket.doubleValue())));
                // td.probability(bucket.subtract(step).doubleValue(), bucket.doubleValue());

                // add to left of center
                values.computeIfPresent(new NumberValue(bucket), (numberValue, bigDecimal) -> {
                    totalArea = totalArea.add(prob);
                    BigDecimal result = bigDecimal.add(prob);
                    if(result.compareTo(upperUtility) > 0) {
                        upperUtility = result;
                    }
                    return result;
                });

                // add to right of center
                values.computeIfPresent(new NumberValue(center.add(i)), (numberValue, bigDecimal) -> {
                    totalArea = totalArea.add(prob);
                    BigDecimal result = bigDecimal.add(prob);
                    if(result.compareTo(upperUtility) > 0) {
                        upperUtility = result;
                    }
                    return result;
                });
            }
            // Update lowerUtility
            lowerUtility = values.values().stream().min(BigDecimal::compareTo).orElse(lowerUtility);
        }

        public NumberValueSetUtilities getSetUtilities() {
            return new NumberValueSetUtilities(lowValue, lowerUtility.divide(totalArea, DECIMALS, BigDecimal.ROUND_HALF_UP), highValue, upperUtility.divide(totalArea, DECIMALS, BigDecimal.ROUND_HALF_UP));
        }

        /**
         * w = weight of issue [0, 1]
         * tr = {@link #triangularNormalRange}
         * tv = {@link #triangularVariation}
         * Lower issue weight returns wider bandwidth
         * Higher issue weight returns narrower bandwidth
         * Returns maps weight issue [0, 1] to [tr+tv, tr-tv];
         * y = -(2tv)*x+tr+tv
         */
        public BigDecimal mapIssueWeight(BigDecimal weight) {
            BigDecimal slope = BigDecimal.valueOf(-2).multiply(triangularVariation);
            return weight.multiply(slope).add(triangularNormalRange).add(triangularVariation);
        }

        /**
         * Linear decreasing mapping function that maps [0, 1] of progress to [1, m].
         * p=progress, m = minimum mapped intensity
         * y = (1-m)(1-x)+m
         */
        public BigDecimal mapProgressToBandwidth(double progress) {
            return BigDecimal.valueOf((1 - MININTENSITY) * (1 - progress) - MININTENSITY);
        }

        /**
         * Returns a gaussian curve
         */
        public Gaussian getGaussian(BigDecimal center, BigDecimal variance) {
            return new Gaussian(center.doubleValue(), variance.doubleValue());
        }

        /**
         * Returns a (symmetrical) triangle distribution
         */
        public TriangularDistribution getTriangular(BigDecimal center, BigDecimal width) {
            return new TriangularDistribution(center.subtract(width).doubleValue(), center.doubleValue(), center.add(width).doubleValue());
        }

        /**
         * returns a laplacian curve
         *
         * @param center
         * @param beta
         * @return
         */
        public LaplaceDistribution getLaplace(BigDecimal center, BigDecimal beta) {
            return new LaplaceDistribution(center.doubleValue(), beta.doubleValue());
        }

        @Override
        public BigDecimal getUtility(Value value) {
            return getSetUtilities().getUtility(value);
        }

        /**
         * @param valueset the valueset that is supposed match with this
         * @return null if the ValueSetUtilities fits the given set of values , that
         * means it can give utilities for all possible values in valueset.
         * Or a string containing an explanation why not.
         */
        @Override
        public String isFitting(ValueSet valueset) {

            if (!(valueset instanceof NumberValueSet)) {
                return "The utilities are for a number valueset but the given values are "
                  + valueset;
            }
            NumberValueSet numvalset = (NumberValueSet) valueset;
            if (numvalset.getRange().getLow().compareTo(lowValue) != 0) {
                return "the utilities are specified down to " + lowValue
                  + " but the valueset starts at "
                  + numvalset.getRange().getLow();
            }
            if (numvalset.getRange().getHigh().compareTo(highValue) != 0) {
                return "the utilities are specified up to " + highValue
                  + " but the valueset ends at "
                  + numvalset.getRange().getHigh();
            }

            return null;
        }
    }

}