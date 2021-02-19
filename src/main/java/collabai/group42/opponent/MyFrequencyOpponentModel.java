package collabai.group42.opponent;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import geniusweb.actions.Action;
import geniusweb.actions.Offer;
import geniusweb.issuevalue.Bid;
import geniusweb.issuevalue.Domain;
import geniusweb.issuevalue.NumberValue;
import geniusweb.issuevalue.Value;
import geniusweb.opponentmodel.OpponentModel;
import geniusweb.profile.utilityspace.NumberValueSetUtilities;
import geniusweb.profile.utilityspace.UtilitySpace;
import geniusweb.progress.Progress;

/**
 * implements an {@link OpponentModel} by counting frequencies of bids placed by
 * the opponent.
 * <p>
 * NOTE: {@link NumberValue}s are also treated as 'discrete', so the frequency
 * of one value does not influence the influence the frequency of nearby values
 * (as you might expect as {@link NumberValueSetUtilities} is only affected by
 * the endpoints).
 * <p>
 * immutable.
 */
public class MyFrequencyOpponentModel implements UtilitySpace, OpponentModel {

    private static final int DECIMALS = 4; // accuracy of our computations.
    private static int serial = 1; // counter for auto name generation

    private final Domain domain;
    private final Map<String, Map<Value, Integer>> bidFrequencies;
    private final BigDecimal totalBids;
    private final Bid resBid;

    public MyFrequencyOpponentModel() {
        this.domain = null;
        this.bidFrequencies = null;
        this.totalBids = BigDecimal.ZERO;
        this.resBid = null;
    }

    /**
     * internal constructor. Assumes the freqs keyset is equal to the available
     * issues.
     *
     * @param domain the domain
     * @param freqs  the observed frequencies for all issue values. This map is
     *               assumed to be a fresh private-access only copy.
     * @param total  the total number of bids contained in the freqs map. This
     *               must be equal to the sum of the Integer values in the
     *               {@link #bidFrequencies} for each issue (this is not
     *               checked).
     * @param resBid the reservation bid. Can be null
     */
    private MyFrequencyOpponentModel(Domain domain,
                                   Map<String, Map<Value, Integer>> freqs, BigDecimal total,
                                   Bid resBid) {
        if (domain == null) {
            throw new IllegalStateException("domain is not initialized");
        }
        this.domain = domain;
        this.bidFrequencies = freqs;
        this.totalBids = total;
        this.resBid = resBid;
    }

    /**
     * @param freqs
     * @return deep copy of freqs map.
     */
    private static Map<String, Map<Value, Integer>> cloneMap(
        Map<String, Map<Value, Integer>> freqs) {
        Map<String, Map<Value, Integer>> map = new HashMap<>();
        for (String issue : freqs.keySet()) {
            map.put(issue, new HashMap<Value, Integer>(freqs.get(issue)));
        }
        return map;
    }

    @Override
    public MyFrequencyOpponentModel with(Domain domain, Bid resBid) {
        if (domain == null) {
            throw new NullPointerException("domain is not initialized");
        }
        // FIXME merge already available frequencies?
        return new MyFrequencyOpponentModel(domain,
            domain.getIssues().stream().collect(
                Collectors.toMap(iss -> iss, iss -> new HashMap<>())),
            BigDecimal.ZERO, resBid);
    }

    @Override
    public BigDecimal getUtility(Bid bid) {
        if (domain == null) {
            throw new IllegalStateException("domain is not initialized");
        }
        String err = domain.isComplete(bid);
        if (err != null) {
            throw new IllegalArgumentException(err);
        }
        if (totalBids == BigDecimal.ZERO) {
            return BigDecimal.ONE;
        }
        BigDecimal sum = BigDecimal.ZERO;
        // Assume all issues have equal weight.
        for (String issue : domain.getIssues()) {
            sum = sum.add(getFraction(issue, bid.getValue(issue)));
        }
        return sum.divide(new BigDecimal(bidFrequencies.size()), DECIMALS,
            BigDecimal.ROUND_HALF_UP);
    }

    @Override
    public String getName() {
        if (domain == null) {
            throw new IllegalStateException("domain is not initialized");
        }
        return "FreqOppModel" + (serial++) + "For" + domain;
    }

    @Override
    public Domain getDomain() {
        return domain;
    }

    @Override
    public MyFrequencyOpponentModel with(Action action, Progress progress) {
        if (domain == null) {
            throw new IllegalStateException("domain is not initialized");
        }

        if (!(action instanceof Offer))
            return this;

        Bid bid = ((Offer) action).getBid();
        Map<String, Map<Value, Integer>> newFreqs = cloneMap(bidFrequencies);
        for (String issue : domain.getIssues()) {
            Map<Value, Integer> freqs = newFreqs.get(issue);
            Value value = bid.getValue(issue);
            if (value != null) {
                Integer oldfreq = freqs.get(value);
                if (oldfreq == null) {
                    oldfreq = 0;
                }
                freqs.put(value, oldfreq + 1);
            }
        }

        return new MyFrequencyOpponentModel(domain, newFreqs,
            totalBids.add(BigDecimal.ONE), resBid);
    }

    /**
     * @param issue the issue to get frequency info for
     * @return a map containing a map of values and the number of times that
     * value was used in previous bids. Values that are possible but not
     * in the map have frequency 0.
     */
    public Map<Value, Integer> getCounts(String issue) {
        if (domain == null) {
            throw new IllegalStateException("domain is not initialized");
        }
        if (!(bidFrequencies.containsKey(issue))) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(bidFrequencies.get(issue));
    }

    /**
     * @param issue the issue to check
     * @param value the value to check
     * @return the fraction of the total cases that bids contained given value
     * for the issue.
     */
    private BigDecimal getFraction(String issue, Value value) {
        if (totalBids == BigDecimal.ZERO) {
            return BigDecimal.ONE;
        }
        Integer freq = bidFrequencies.get(issue).get(value);
        if (freq == null) {
            freq = 0;
        }
        return new BigDecimal(freq).divide(totalBids, DECIMALS,
            BigDecimal.ROUND_HALF_UP);
    }

    @Override
    public Bid getReservationBid() {
        return resBid;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
            + ((bidFrequencies == null) ? 0 : bidFrequencies.hashCode());
        result = prime * result + ((domain == null) ? 0 : domain.hashCode());
        result = prime * result
            + ((totalBids == null) ? 0 : totalBids.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        MyFrequencyOpponentModel other = (MyFrequencyOpponentModel) obj;
        if (bidFrequencies == null) {
            if (other.bidFrequencies != null)
                return false;
        } else if (!bidFrequencies.equals(other.bidFrequencies))
            return false;
        if (domain == null) {
            if (other.domain != null)
                return false;
        } else if (!domain.equals(other.domain))
            return false;
        if (totalBids == null) {
            if (other.totalBids != null)
                return false;
        } else if (!totalBids.equals(other.totalBids))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "MyFrequencyOpponentModel[" + totalBids + "," + bidFrequencies
            + "]";
    }

}
