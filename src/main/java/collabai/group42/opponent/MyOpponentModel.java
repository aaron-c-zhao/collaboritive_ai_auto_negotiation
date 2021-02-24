package collabai.group42.opponent;

import geniusweb.actions.Action;
import geniusweb.actions.Offer;
import geniusweb.issuevalue.Bid;
import geniusweb.issuevalue.Domain;
import geniusweb.opponentmodel.OpponentModel;
import geniusweb.profile.utilityspace.UtilitySpace;
import geniusweb.progress.Progress;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class MyOpponentModel implements UtilitySpace, OpponentModel {

    private static final int DECIMALS = 4; // accuracy of our computations.
    private static int serial = 1; // counter for auto name generation

    private final Domain domain;
    private final Bid resBid;

    private final List<BidWrapper> bidHistory;

    public MyOpponentModel() {
        this.domain = null;
        this.resBid = null;

        // TODO do something... 
        this.bidHistory = null;
    }


    /**
     * internal constructor. Assumes the freqs keyset is equal to the available
     * issues.
     *
     * @param domain the domain
     * @param resBid the reservation bid. Can be null
     */
    private MyOpponentModel(Domain domain, List<BidWrapper> bidHistory, Bid resBid) {
        if (domain == null) {
            throw new IllegalStateException("domain is not initialized");
        }
        this.domain = domain;
        this.resBid = resBid;

        // TODO do something...        
        this.bidHistory = bidHistory;
    }

    /**
     * @param bidHistory
     * @return deep copy of bidHistory list.
     */
    private static List<BidWrapper> cloneList(List<BidWrapper> bidHistory) {
        return new ArrayList<>(bidHistory);
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
        if (domain == null) {
            throw new NullPointerException("domain is not initialized");
        }

        // TODO do something...

        return new MyOpponentModel(domain, new ArrayList<>(), resBid);
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

        if (!(action instanceof Offer))
            return this;

        final int maxRounds = 200;
        double p = progress.get(System.currentTimeMillis());
        int roundNum = (int) Math.round(p * maxRounds);

        Bid bid = ((Offer) action).getBid();

        // TODO do something...
        List<BidWrapper> newBids = cloneList(bidHistory);
        newBids.add(new BidWrapper(bid, roundNum));

        return new MyOpponentModel(domain, newBids, resBid);
    }

    /**
     * @param bid the {@link Bid} to be evaluated
     * @return the utility value of this bid. This MUST return a number in the range
     * [0,1]. 0 means preferred the least and 1 means preferred the most.
     */
    @Override
    public BigDecimal getUtility(Bid bid) {
        if (domain == null) {
            throw new IllegalStateException("domain is not initialized");
        }
        String err = domain.isComplete(bid);
        if (err != null) {
            throw new IllegalArgumentException(err);
        }

        // TODO do something...

        return BigDecimal.ZERO;
    }

    /**
     * @return the name of this profile. Must be simple name (a-Z, 0-9)
     */
    @Override
    public String getName() {
        if (domain == null) {
            throw new IllegalStateException("domain is not initialized");
        }
        return "MyOppModel" + (serial++) + "For" + domain;
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

    private class BidWrapper {
        Bid bid;
        int round;

        public BidWrapper(Bid bid, int round) {
            this.bid = bid;
            this.round = round;
        }
    }
}
