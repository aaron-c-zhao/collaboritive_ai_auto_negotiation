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
import geniusweb.profile.utilityspace.LinearAdditiveUtilitySpace;
import geniusweb.profile.utilityspace.NumberValueSetUtilities;
import geniusweb.profile.utilityspace.ValueSetUtilities;
import geniusweb.progress.Progress;
import org.apache.commons.math3.analysis.function.Gaussian;
import org.apache.commons.math3.distribution.LaplaceDistribution;
import org.apache.commons.math3.distribution.TriangularDistribution;

public class Group42FrequencyModel implements Group42OpponentModel {

    private static final int DECIMALS = 4; // accuracy of our computations.
    private final Domain domain;
    private final Map<String, ValueSetUtilities> utils;
    private final Map<String, BigDecimal> weights;
    private final Bid previousBid;
    private final BigDecimal learnCoef;
    private final BigDecimal learnValueAddition;
    private final BigDecimal amountOfIssues;
    private final BigDecimal goldenValue;
    private final Bid resBid;
    private LinearAdditiveUtilitySpace laus;

    public Group42FrequencyModel() {
        domain = null;
        amountOfIssues = BigDecimal.ZERO;
        goldenValue = BigDecimal.ZERO;
        learnValueAddition = BigDecimal.ZERO;
        learnCoef = BigDecimal.ZERO;
        previousBid = null;
        weights = null;
        utils = null;
        resBid = null;
        laus = null;
    }

    public Group42FrequencyModel(Domain domain, Map<String, ValueSetUtilities> utils, Map<String, BigDecimal> weights, BigDecimal learnCoef, BigDecimal learnValueAddition, BigDecimal amountOfIssues, BigDecimal goldenValue, Bid resBid, Bid previousBid) {
        this.domain = domain;
        this.utils = utils;
        this.weights = weights;
        this.learnCoef = learnCoef;
        this.learnValueAddition = learnValueAddition;
        this.amountOfIssues = amountOfIssues;
        this.goldenValue = goldenValue;
        this.resBid = resBid;
        this.previousBid = previousBid;
        this.laus = null;
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
        BigDecimal goldenValue = learnCoef.divide(amountOfIssues, DECIMALS, BigDecimal.ROUND_HALF_EVEN);
        BigDecimal commonWeight = BigDecimal.ONE.divide(amountOfIssues, DECIMALS, BigDecimal.ROUND_HALF_EVEN);

        // HashMap<String, Issue> us = new HashMap<>();
        Map<String, ValueSetUtilities> utils = new HashMap<>();
        Map<String, BigDecimal> weights = new HashMap<>();
        for (String issue : domain.getIssues()) {
            // for utils and weights
            weights.put(issue, commonWeight);
            ValueSet vs = domain.getValues(issue);

            if (vs instanceof DiscreteValueSet) {
                DiscreteValueSet dvs = (DiscreteValueSet) vs;

                Map<DiscreteValue, BigDecimal> discreteEstimation = new HashMap<>();
                for (DiscreteValue x : dvs.getValues()) {
                    discreteEstimation.put(x, BigDecimal.ONE);
                }
                utils.put(issue, new DiscreteUtilities(discreteEstimation));
            }

            if (vs instanceof NumberValueSet) {
                NumberValueSet nvs = (NumberValueSet) vs;
                KDE kde = new KDE(nvs.getRange().getLow(), nvs.getRange().getHigh(), nvs.getRange().getStep());
                utils.put(issue, kde);
            }
        }

        return new Group42FrequencyModel(domain, utils, weights, learnCoef, learnValueAddition, amountOfIssues, goldenValue, resBid, null);
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
            BigDecimal maximumWeight = BigDecimal.ONE.subtract(amountOfIssues.multiply(goldenValue).divide(totalSum, DECIMALS, BigDecimal.ROUND_HALF_EVEN));

            for (String issue : lastDiffSet.keySet()) {
                BigDecimal weight = weights.get(issue);
                BigDecimal newWeight;

                if (lastDiffSet.get(issue) == 0 && maximumWeight.compareTo(weight) > 0) {
                    newWeight = weight.add(goldenValue).divide(totalSum, DECIMALS, BigDecimal.ROUND_HALF_EVEN);
                } else {
                    newWeight = weight.divide(totalSum, DECIMALS, BigDecimal.ROUND_HALF_EVEN);
                }

                weights.put(issue, newWeight);
            }
        }

        newBid.getIssueValues().forEach((issue, value) -> {
            if (value instanceof NumberValue) {
                ((KDE) utils.get(issue)).addValue(value, weights.get(issue), time);
            } else if (value instanceof DiscreteValue) {
                ((DiscreteUtilities) utils.get(issue)).addValue(value, learnValueAddition, time);
            }
        });

        return new Group42FrequencyModel(domain, utils, weights, learnCoef, learnValueAddition, amountOfIssues, goldenValue, resBid, newBid);
    }

    /**
     * Returns if there were any changes to previous bid.
     */
    private HashMap<String, Integer> determineDifference(Bid first, Bid second) {
        HashMap<String, Integer> diff = new HashMap<>();
        for (String issue : domain.getIssues()) {
            Value v1 = first.getValue(issue);
            Value v2 = second.getValue(issue);
            diff.put(issue, (v1.equals(v2) ? 0 : 1));
        }
        return diff;
    }

    @Override
    public BigDecimal getUtility(Bid bid) {
        if (laus == null) {
            normaliseWeights();
            laus = getUtilitySpace();
        }
        return laus.getUtility(bid);
    }

    private LinearAdditiveUtilitySpace getUtilitySpace() {
        return new LinearAdditiveUtilitySpace(domain, "Opponent", utils, weights, resBid);
    }

    @Override
    public String getName() {
        return "Time Dependent Hard Headed Frequency Model";
    }

    @Override
    public Domain getDomain() {
        return domain;
    }

    @Override
    public Bid getReservationBid() {
        return resBid;
    }

    /**
     * Makes sure that the sum of weights are 1, at the expense of the first issue.
     */
    private void normaliseWeights() {
        BigDecimal extra = BigDecimal.ONE.subtract(weights.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add));
        for (String issue : weights.keySet()) {
            weights.computeIfPresent(issue, (s, bigDecimal) -> bigDecimal.add(extra));
            return;
        }
    }

    private static class DiscreteUtilities implements ValueSetUtilities {

        private static final double DECAY_COEFFICIENT = 0.2d;
        private final HashMap<DiscreteValue, BigDecimal> valueFrequencies = new HashMap<>();
        private BigDecimal count;

        public DiscreteUtilities(Map<DiscreteValue, BigDecimal> valueUtils) {
            if (valueUtils == null) {
                throw new NullPointerException("valueUtils==null");
            }

            this.valueFrequencies.putAll(valueUtils);
            count = BigDecimal.valueOf(valueFrequencies.size());
        }

        public void addValue(Value value, BigDecimal amount, double time) {
            if (!(value instanceof DiscreteValue)) {
                return;
            }

            BigDecimal newAmount = getTimeBasedIncrement(amount, time);
            // BigDecimal newAmount = amount;
            valueFrequencies.put((DiscreteValue) value, valueFrequencies.get(value).add(newAmount));
            count = count.add(newAmount);
        }

        /**
         * y=a*(1-ct^3)
         */
        private BigDecimal getTimeBasedIncrement(BigDecimal amount, double time) {
            // return amount.multiply(BigDecimal.valueOf(Math.exp(-DECAY_COEFFICIENT * time)));
            return amount.multiply(BigDecimal.ONE.subtract(BigDecimal.valueOf(time * time * time * DECAY_COEFFICIENT)));
        }

        @Override
        public BigDecimal getUtility(Value value) {
            if ((value instanceof DiscreteValue) && valueFrequencies.containsKey(value)) {
                return valueFrequencies.get(value).divide(count, DECIMALS, BigDecimal.ROUND_HALF_EVEN);
            }
            return BigDecimal.ZERO;
        }

        /**
         * @return copy of the value-utility pair map.
         */
        public Map<DiscreteValue, BigDecimal> getUtilities() {
            return Collections.unmodifiableMap(valueFrequencies);
        }

        @Override
        public String isFitting(ValueSet valueset) {
            if (!(valueset instanceof DiscreteValueSet)) {
                return "The utilities are for a discrete valueset but the given values are "
                  + valueset;
            }
            DiscreteValueSet discvalueset = (DiscreteValueSet) valueset;
            if (!valueFrequencies.keySet()
              .equals(new HashSet<>(discvalueset.getValues())))
                return "The values in the set " + valueset
                  + " do not match the values mapped to utilities "
                  + valueFrequencies.keySet();
            return null;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("DiscreteUtilities\n");
            for (DiscreteValue value : valueFrequencies.keySet()) {
                sb.append(value).append(": ").append(getUtility(value)).append("\n");
            }
            return sb.toString();
        }
    }

    /**
     * Use KDE to estimate number value set utility.
     */
    private static class KDE implements ValueSetUtilities {
        private static final double MIN_INTENSITY = 0.8;
        private final BigDecimal lowValue, highValue, step;
        private final BigDecimal numValues;
        private final BigDecimal triangularNormalRange, triangularVariation;
        private final LinkedHashMap<BigDecimal, BigDecimal> values;
        private BigDecimal totalArea;
        private NumberValueSetUtilities nvsu;

        public KDE(BigDecimal lowerBound, BigDecimal upperBound, BigDecimal step) {
            this.lowValue = lowerBound;
            this.highValue = upperBound;
            this.step = step;
            this.numValues = highValue.subtract(lowValue).divideToIntegralValue(step);
            this.triangularNormalRange = highValue.subtract(lowValue).divide(BigDecimal.valueOf(2), DECIMALS, BigDecimal.ROUND_HALF_EVEN);
            this.triangularVariation = highValue.subtract(lowValue).divide(BigDecimal.valueOf(4), DECIMALS, BigDecimal.ROUND_HALF_EVEN);
            this.values = new LinkedHashMap<>(numValues.intValue());

            // set uniform distribution
            totalArea = BigDecimal.ZERO;
            // BigDecimal weight = BigDecimal.ONE.divide(numValues, DECIMALS, BigDecimal.ROUND_HALF_EVEN);
            for (BigDecimal i = lowValue; i.compareTo(highValue) <= 0; i = i.add(step)) {
                values.put(i, BigDecimal.ZERO);
            }

            nvsu = null;
        }

        public void addValue(Value value, BigDecimal issueWeight, double progress) {
            if (!(value instanceof NumberValue)) {
                return;
            }

            BigDecimal center = ((NumberValue) value).getValue();
            // BigDecimal bandwidth = mapIssueWeight(issueWeight);
            BigDecimal bandwidth = triangularNormalRange;
            BigDecimal factor = mapProgressToBandwidth(progress);
            TriangularDistribution td = getTriangular(center, bandwidth);

            // add probability for center (only once)
            BigDecimal probCenter = factor.multiply(getProbability(td, center));
            values.computeIfPresent(center, (numberValue, bigDecimal) -> {
                totalArea = totalArea.add(probCenter);
                BigDecimal result = bigDecimal.add(probCenter);
                return result;
            });

            // add probabilities to both sides of center
            for (BigDecimal i = step; i.compareTo(bandwidth) < 0; i = i.add(step)) {
                BigDecimal bucket = center.subtract(i);
                BigDecimal prob = factor.multiply(getProbability(td, bucket));

                // add to left of center
                values.computeIfPresent(bucket, (numberValue, bigDecimal) -> {
                    // totalArea = totalArea.add(prob);
                    BigDecimal result = bigDecimal.add(prob);
                    return result;
                });

                // add to right of center
                values.computeIfPresent(center.add(i), (numberValue, bigDecimal) -> {
                    // totalArea = totalArea.add(prob);
                    BigDecimal result = bigDecimal.add(prob);
                    return result;
                });
            }

            nvsu = null;
        }

        private BigDecimal getProbability(TriangularDistribution td, BigDecimal center) {
            return BigDecimal.valueOf(td.probability(
              center.subtract(step.divide(BigDecimal.valueOf(2))).doubleValue(),
              center.add(step.divide(BigDecimal.valueOf(2))).doubleValue()));
        }

        public NumberValueSetUtilities getSetUtilities() {
            return new NumberValueSetUtilities(lowValue, values.get(lowValue).divide(totalArea, DECIMALS, BigDecimal.ROUND_HALF_EVEN), highValue, values.get(highValue).divide(totalArea, DECIMALS, BigDecimal.ROUND_HALF_EVEN));
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
         * (y = (1-m)(1-x)+m) old
         * y = 1-cp^3
         */
        public BigDecimal mapProgressToBandwidth(double progress) {
            // return BigDecimal.valueOf((1 - MIN_INTENSITY) * (1 - progress) + MIN_INTENSITY);
            return BigDecimal.ONE.subtract(BigDecimal.valueOf(progress * progress * progress * (1d - MIN_INTENSITY)));
        }

        /**
         * Returns a (symmetrical) triangle distribution (KDE kernel)
         */
        public TriangularDistribution getTriangular(BigDecimal center, BigDecimal width) {
            return new TriangularDistribution(center.subtract(width).doubleValue(), center.doubleValue(), center.add(width).doubleValue());
        }

        /**
         * Returns a gaussian curve
         */
        public Gaussian getGaussian(BigDecimal center, BigDecimal variance) {
            return new Gaussian(center.doubleValue(), variance.doubleValue());
        }

        /**
         * Returns a laplacian curve
         */
        public LaplaceDistribution getLaplace(BigDecimal center, BigDecimal beta) {
            return new LaplaceDistribution(center.doubleValue(), beta.doubleValue());
        }

        @Override
        public BigDecimal getUtility(Value value) {
            if (nvsu == null) {
                nvsu = getSetUtilities();
            }
            return nvsu.getUtility(value);
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

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("KDE{\n");
            for (BigDecimal value : values.keySet()) {
                sb.append(value.toPlainString()).append(": ").append(getUtility(new NumberValue(value))).append("\n");
            }
            return sb.toString();
        }
    }

}