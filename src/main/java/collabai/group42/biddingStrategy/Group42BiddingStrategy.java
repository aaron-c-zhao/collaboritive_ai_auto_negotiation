package collabai.group42.biddingStrategy;

import geniusweb.actions.Accept;
import geniusweb.actions.Action;
import geniusweb.actions.EndNegotiation;
import geniusweb.actions.Offer;
import geniusweb.actions.PartyId;
import geniusweb.boa.BoaState;
import geniusweb.boa.biddingstrategy.BiddingStrategy;
import geniusweb.boa.biddingstrategy.ExtendedUtilSpace;
import geniusweb.issuevalue.Bid;
import geniusweb.profile.Profile;
import geniusweb.profile.utilityspace.LinearAdditive;
import tudelft.utilities.immutablelist.ImmutableList;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;


public class Group42BiddingStrategy implements BiddingStrategy{

    private ExtendedUtilSpace bidSpace = null;
    private PartyId me;

    @Override
    public Action getAction(BoaState boaState) {
        if (bidSpace == null) {
            init(boaState);
        }

        double targetUtility = getTargetUtility(
                boaState.getProgress().get(System.currentTimeMillis()));

        ImmutableList<Bid> bidOptions = bidSpace
                .getBids((BigDecimal.valueOf(targetUtility)));

        // TODO: check the logic here
        if (bidOptions.size().intValue() == 0) {
            // should not happen, emergency exit
            boaState.getReporter().log(Level.WARNING,
                    "No viable bids found around current utility target");
            Bid lastBid = getLastBid(boaState.getActionHistory());
            if (lastBid == null)
                return new EndNegotiation(me);
            return new Accept(me, lastBid);
        }
        Bid pickedBid = bidOptions.get(ThreadLocalRandom.current()
                .nextInt(bidOptions.size().intValue()));
        return new Offer(me, pickedBid);
    }

    /**
     * The method where the specific bidding strategy is implemented.
     *
     * @param progress Current progress wrt the total rounds
     * @return The intended utility for the next bid
     */
    private double getTargetUtility(Double progress) {
        return 0.0;
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
