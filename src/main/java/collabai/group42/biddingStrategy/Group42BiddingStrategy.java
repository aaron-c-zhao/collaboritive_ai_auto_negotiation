package collabai.group42.biddingStrategy;

import collabai.group42.opponent.Group42OpponentModel;
import geniusweb.actions.AbstractAction;
import geniusweb.actions.Accept;
import geniusweb.actions.Action;
import geniusweb.actions.EndNegotiation;
import geniusweb.actions.Offer;
import geniusweb.actions.PartyId;
import collabai.group42.BoaState;
import geniusweb.issuevalue.Bid;
import geniusweb.opponentmodel.OpponentModel;
import geniusweb.profile.Profile;
import geniusweb.profile.utilityspace.LinearAdditive;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import tudelft.utilities.immutablelist.ImmutableList;

import java.lang.Math;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;


public class Group42BiddingStrategy implements BiddingStrategy {

    protected ExtendedUtilSpace bidSpace = null;
    protected PartyId me;
    private double min, max;
    private double a = 4.0, b = 0.15;
    protected LinkedList<List<Double>> recentBids = new LinkedList<>();

    @Override
    public Action getAction(BoaState boaState) {
        if (bidSpace == null) {
            init(boaState);
        }

        double progress = boaState.getProgress().get(System.currentTimeMillis());

        double targetUtility = getTargetUtility(progress);

        Bid lastBid = getLastBid(boaState.getActionHistory());
        if (!(lastBid == null) && recentBids.size() < 10)
            recentBids.add(Arrays.asList(progress, getUtility(lastBid)));
        else if (recentBids.size() == 10) {
            recentBids.remove();
            recentBids.add(Arrays.asList(progress, getUtility(lastBid)));
        }

        ImmutableList<Bid> bidOptions = bidSpace
                .getBids((BigDecimal.valueOf(targetUtility)));

        if (bidOptions.size().intValue() == 0) {
            // should not happen, emergency exit
            return getAlterAction(boaState, targetUtility, lastBid, bidOptions);
        }

        // pick the most beneficial bid from the opponent's perspective
        return new Offer(me, getNiceBid(bidOptions, boaState));
    }

    /**
     * React when no valid bid is found at target utility.
     *
     * @param boaState      {@link BoaState}
     * @param targetUtility target utility at which no valid bid is found
     * @param lastBid       opponent's last bid
     * @param bidOptions    bid options
     * @return alternative action. Either
     */
    protected AbstractAction getAlterAction(BoaState boaState, double targetUtility, Bid lastBid, ImmutableList<Bid> bidOptions) {
        boaState.getReporter().log(Level.WARNING,
                "No viable bids found at target utility: " + targetUtility);

        // try target utility plus a randomized value in [-0.05, 0.05]
        for (int i = 0; i < 10 && bidOptions.size().intValue() == 0; i++) {
            BigDecimal tUtility = BigDecimal.valueOf(targetUtility
                    + (ThreadLocalRandom.current().nextDouble() - 1) / 10);//.setScale(2, RoundingMode.HALF_UP);
            bidOptions = bidSpace.getBids(tUtility);
//            System.out.println(tUtility.doubleValue());
//            System.out.println(bidOptions.size().intValue());
        }
        if (bidOptions.size().intValue() == 0) {
            boaState.getReporter().log(Level.WARNING,
                    "No viable bids found after 10 attempts.");

            // no success after 10 more attempts then either accept or walk away
            if (lastBid != null)
                return new Accept(me, lastBid);
            return new EndNegotiation(me);
        }
        return new Offer(me, getNiceBid(bidOptions, boaState));
    }

    /**
     * Find the nicest bid wrt the opponent within 10 attempts.
     *
     * @param bidOptions candidate bids
     * @return the nicest bid among 10 random bids at the target utility
     */
    protected Bid getNiceBid(ImmutableList<Bid> bidOptions, BoaState boaState) {
        long maxIndex = 0;
        double maxUtility = 0.0;
        for (int i = 0; i < bidOptions.size().intValue() && i < 20; i++) {
            long index = ThreadLocalRandom.current()
                    .nextInt(bidOptions.size().intValue());
            double utility = getOpponentUtility(bidOptions.get(index), boaState);
//            System.out.println(utility);
            if (maxUtility < utility) {
                maxUtility = utility;
                maxIndex = index;
            }
        }
        return bidOptions.get(maxIndex);
    }

    /**
     * Calculate the utility of certain bid wrt the party itself.
     *
     * @param bid the opponent's last bid.
     * @return This party's utility gained with opponent's bid.
     */
    protected double getUtility(Bid bid) {
        return bidSpace.getUtility(bid);
    }

    /**
     * The method where the specific bidding strategy is implemented.
     * <p>
     * The strategy combines time dependent pattern with behavior dependent pattern. Time decides
     * the lower limit of the demanded utility, and the opponent's selfishness decides the upper limit
     * of the demanded utility. The target utility will be drawn from the range randomly.
     * <p>
     * Once the target utility is decided, the opponent model is used to pick nicer bid wrt the opponent.
     *
     * @param progress Current progress wrt the total rounds
     * @return The intended utility for the next bid
     */
    protected double getTargetUtility(Double progress) {
        if (progress < 0.1) {
            return (1 - progress * 0.1) * this.max;
        }
        //in the middle part of the negotiation, the toughness of the opponent affect the inflection point
        //which is when to concede at a faster rate
        else if (progress < 0.8) {
            setB();
            return (getTimeDependUtility(progress)) * this.max;
        }
        //in the later half of the negotiation, the toughness of the opponent affect the concede ratio.
        else if (progress < 0.99) {
            setA();
            return (getTimeDependUtility(progress)) * this.max;
        }
        //only one round left, set the target utility to reservation value.
        else {
            return this.min;
        }
    }

    /**
     * Get the minimum utility. Default to reservation value(if present). If there's no
     * reservation value, 0 is returned.
     *
     * @return The minimum acceptable utility value.
     */
    protected double getMin() {
        return bidSpace.getMin().doubleValue();
    }

    protected double getMax() {
        return bidSpace.getMax().doubleValue();
    }

    /**
     * Formula for time dependent behavior.
     * <p>
     * U(t) = 0.9 - (t - b)^a
     *
     * <li>b: [0.05 - 0.25]</li>
     * <li>a: [2 - 5][</li>
     *
     * @param progress
     * @return
     */
    private double getTimeDependUtility(Double progress) {
        return 0.99 - Math.pow((progress - getB()), getA());
    }

    /**
     * Set the coefficient b which affects the conceding rate in phase 2.
     */
    protected void setB() {
        double niceness = getNiceness();
        this.b = 0.1 + 0.1 * niceness;
        System.out.println(niceness);
    }

    /**
     * Set the coefficient a which affects the conceding rate in phase 3.
     */
    protected void setA() {
        double niceness = getNiceness();
        this.a = 4.0 + 2.0 * niceness;
    }


    protected double getB() {
        return this.b;
    }

    protected double getA() {
        return this.a;
    }

    /**
     * Calculate the opponent's niceness based on the slop of linear regression of the previous bids.
     * <p>
     * If the slope is smaller than PI/maxToughness then the opponent is playing tough. Correspondingly,
     * the agent may consider to play extra tough to punish the opponent and end up breaking the deal.
     * </p>
     * <p>
     * If the slope is greater than PI/minToughness then the opponent is conceding normally,
     * then the nicer it plays the tougher the agent becomes. Vice versa.
     * </p>
     *
     * @return the niceness of opponents which should be normalized into a range of [0, 1]
     */
    protected double getNiceness() {
        SimpleRegression regression = new SimpleRegression();
        for (List<Double> point : getRecentBids()) {
            regression.addData(point.get(0), point.get(1));
        }
        double slope = regression.getSlope();
        System.out.println("slope" + slope);
        // map the slope into range [0, 1]
        slope = Math.min(slope, 0.0);
        slope = Math.max(slope, -1.0);
        return -slope;
    }

    protected LinkedList<List<Double>> getRecentBids() {
        return this.recentBids;
    }

    protected void init(BoaState boaState) {
        this.me = boaState.getSettings().getID();
        Profile prof = boaState.getProfile();
        if (!(prof instanceof LinearAdditive)) {
            throw new IllegalArgumentException(
                    "Requires a LinearAdditive space but got " + prof);
        }
        LinearAdditive profile = (LinearAdditive) prof;

        this.bidSpace = getBidSpace(profile);

        min = getMin();
        max = getMax();

        boaState.getReporter().log(Level.INFO,
                "Group42 BOA biddingStrategy initialized");
    }

    private ExtendedUtilSpace getBidSpace(LinearAdditive profile) {
        return new ExtendedUtilSpace(profile);
    }

    /**
     * Wrapper method. Enable testing bidding strategy with various opponent behavior.
     *
     * @param pickedBid bid that the opponent model will evaluate against
     * @return Estimated utility value of last bid from opponent model.
     * TODO: call the actual API from opponent model
     */
    protected double getOpponentUtility(Bid pickedBid, BoaState boaState) {
        Map<PartyId, OpponentModel> oms = boaState.getOpponentModels();
        if (oms == null) return 0.0;
        for (PartyId id : oms.keySet()) {
            if (id == me) continue;
            Group42OpponentModel om = (Group42OpponentModel) oms.get(id);
            return om.getUtility(pickedBid).doubleValue();
        }
        return 0.0;
    }

    /**
     * @return the most recent bid that was offered, or null if no offer has
     * been done yet.
     */
    protected Bid getLastBid(List<Action> history) {
        for (int n = history.size() - 1; n >= 0; n--) {
            Action action = history.get(n);
            if (action instanceof Offer) {
                return ((Offer) action).getBid();
            }
        }
        return null;
    }

    /**
     * For testing purpose.
     *
     * @return the bidspace.
     */
    protected ExtendedUtilSpace getBidSpace() {
        return this.bidSpace;
    }

}
