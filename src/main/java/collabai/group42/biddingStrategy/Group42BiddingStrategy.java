package collabai.group42.biddingStrategy;

import geniusweb.actions.Accept;
import geniusweb.actions.Action;
import geniusweb.actions.Offer;
import geniusweb.actions.PartyId;
import geniusweb.boa.BoaState;
import geniusweb.boa.biddingstrategy.BiddingStrategy;
import geniusweb.boa.biddingstrategy.ExtendedUtilSpace;
import geniusweb.issuevalue.Bid;
import geniusweb.profile.Profile;
import geniusweb.profile.utilityspace.LinearAdditive;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import tudelft.utilities.immutablelist.ImmutableList;

import java.lang.Math;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;


public class Group42BiddingStrategy implements BiddingStrategy{

    private ExtendedUtilSpace bidSpace = null;
    private PartyId me;
    private double a = 3.0, b = 0.15;
    private final int maxToughness = 9, minToughness = 4;
    private double prevUtility = 1.0;
    private LinkedList<List<Double>> recentBids = new LinkedList<List<Double>>();

    @Override
    public Action getAction(BoaState boaState) {
        if (bidSpace == null) {
            init(boaState);
        }

        double targetUtility = getTargetUtility(
                boaState.getProgress().get(System.currentTimeMillis()));

        Bid lastBid = getLastBid(boaState.getActionHistory());
        if (!(lastBid == null) && recentBids.size() < 10)
            recentBids.add(new ArrayList<>(Arrays.asList(prevUtility, getUtility(lastBid))));
        else if (recentBids.size() == 10) {
            recentBids.remove();
            recentBids.add(Arrays.asList(prevUtility, getUtility(lastBid))));
        }

        prevUtility = targetUtility;

        ImmutableList<Bid> bidOptions = bidSpace
                .getBids((BigDecimal.valueOf(targetUtility)));

        // TODO: Use opponentmodel to find the most beneficial bid wrt the opponent in the same
        //       utility level
        if (bidOptions.size().intValue() == 0) {
            // should not happen, emergency exit
            boaState.getReporter().log(Level.WARNING,
                    "No viable bids found around current utility target");
            // TODO: replace this behavior with randomizing the utility

            return new Accept(me, lastBid);
        }
        Bid pickedBid = bidOptions.get(ThreadLocalRandom.current()
                .nextInt(bidOptions.size().intValue()));
        return new Offer(me, pickedBid);
    }

    /**
     * Calculate the utility of certain bid wrt the party itself.
     *
     * @param lastBid the opponent's last bid.
     * @return This party's utility gained with opponent's bid.
     * TODO: implement this method
     */
    private double getUtility(Bid lastBid) {
        return 0.0;
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
    private double getTargetUtility(Double progress) {
        if (progress < 0.1) {
            return 1 - progress;
        }
        //in the middle part of the negotiation, the toughness of the opponent affect the inflection point
        //which is when to concede at a faster rate
        else if (progress < 0.6) {
            setB();
            return getTimeDependUtility(progress);
        }
        //in the later half of the negotiation, the toughness of the opponent affect the concede ratio.
        else if (progress < 0.9986){
            setA();
            return getTimeDependUtility(progress);
        }
        //only one round left, set the target utility to reservation value.
        else {
            return getMin();
        }
    }

    /**
     * Get the minimum utility. Default to reservation value(if present). If there's no
     * reservation value, 0 is returned.
     *
     * @return The minimum acceptable utility value.
     * TODO: implement this method
     */
    private double getMin() {
        return 0.0;
    }

    /**
     * Formula for time dependent behavior.
     *
     * U(t) = 0.9 - (t - b)^a
     *
     * <li>b: [0.05 - 0.25]</li>
     * <li>a: [2 - 5][</li>
     *
     * @param progress
     * @return
     */
    private double getTimeDependUtility(Double progress) {
        return 0.9 - Math.pow((progress - this.b), this.a);
    }

    /**
     * Set the coefficient b which affects the conceding rate in phase 2.
     */
    private void setB() {
        double niceness = getNiceness();
        this.b =  0.05 + 0.2 * niceness;
    }

    /**
     * Set the coefficient a which affects the conceding rate in phase 3.
     */
    protected void setA() {
        double niceness = getNiceness();
        this.a = 2 + 3 * niceness;
    }

    /**
     * Calculate the opponent's niceness based on the slop of linear regression of the previous bids.
     * <p>
     *     If the slope is smaller than PI/maxToughness then the opponent is playing tough. Correspondingly,
     *     the agent may consider to play extra tough to punish the opponent and end up breaking the deal.
     * </p>
     * <p>
     *     If the slope is greater than PI/minToughness then the opponent is conceding normally,
     *     then the nicer it plays the tougher the agent becomes. Vice versa.
     * </p>
     *
     * @return the niceness of opponents which should be normalized into a range of [0, 1]
     */
    private double getNiceness() {
        SimpleRegression regression = new SimpleRegression();
        for (List<Double> point: recentBids) {
            regression.addData(point.get(0), point.get(1));
        }
        double slope = regression.getSlope();

        // map the slope into range [0, 1]
        slope = (slope < Math.PI / maxToughness)? 0.0 : slope;
        slope = (slope > Math.PI / minToughness)? 1.0 : slope;
        slope = (slope - minToughness) / (maxToughness - minToughness);
        return slope;
    }

    private void init(BoaState boaState) {
        this.me = boaState.getSettings().getID();
        Profile prof = boaState.getProfile();
        if(!(prof instanceof LinearAdditive)) {
            throw new IllegalArgumentException(
                    "Requires a LinearAdditive space but got " + prof);
        }
        LinearAdditive profile = (LinearAdditive) prof;

        this.bidSpace = getBidSpace(profile);

        boaState.getReporter().log(Level.INFO,
                "Group42 BOA biddingStrategy initialized");
    }

    private ExtendedUtilSpace getBidSpace(LinearAdditive profile) {
        return new ExtendedUtilSpace(profile);
    }

    /**
     * Wrapper method. Enable testing bidding strategy with various opponent behavior.
     *
     * @return Estimated utility value of last bid from opponent model.
     * TODO: call the actual API from opponent model
     */
    private double getOpponentUtility() {
        return 0.0;
    }

    /**
     * @return the most recent bid that was offered, or null if no offer has
     *         been done yet.
     */
    private Bid getLastBid(List<Action> history) {
        for (int n = history.size() - 1; n >= 0; n--) {
            Action action = history.get(n);
            if (action instanceof Offer) {
                return ((Offer) action).getBid();
            }
        }
        return null;
    }


}
