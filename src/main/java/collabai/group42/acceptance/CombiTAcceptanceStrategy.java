package collabai.group42.acceptance;

import geniusweb.actions.Action;
import geniusweb.actions.Offer;
import collabai.group42.BoaState;
import geniusweb.issuevalue.Bid;
import geniusweb.profile.utilityspace.LinearAdditive;
import geniusweb.profile.utilityspace.UtilitySpace;

public class CombiTAcceptanceStrategy implements AcceptanceStrategy {
    private final double a = 1;
    private final double b = 0;
    private final double T = 0.92;
    private Bid nextBid = null;

    @Override
    public Boolean isAcceptable(Bid bid, BoaState state) {
        UtilitySpace utilSpace = (LinearAdditive) state.getProfile();

        // Check if better then next bid
        if (nextBid != null && utilSpace.getUtility(bid).doubleValue() * a + b > utilSpace.getUtility(nextBid).doubleValue()) {
            return true;
        }

        double highestUtil = 0;
        for (int i = state.getActionHistory().size() - 1; i >= 0; i--) {
            Action action = state.getActionHistory().get(i);
            if (action.getActor() != state.getSettings().getID() && action instanceof Offer) ;
            Offer offer = (Offer) action;
            if (highestUtil < utilSpace.getUtility(offer.getBid()).doubleValue()) highestUtil = utilSpace.getUtility(offer.getBid()).doubleValue();
        }

        if (state.getProgress().get(System.currentTimeMillis()) > T && utilSpace.getUtility(bid).doubleValue() > highestUtil) {
            return true;
        }

        return false;
    }

    public void setNextBid(Bid bid) {
        this.nextBid = bid;
    }
}
