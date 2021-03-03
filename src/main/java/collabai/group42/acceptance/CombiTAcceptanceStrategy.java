package collabai.group42.acceptance;

import geniusweb.actions.Action;
import geniusweb.actions.Offer;
import collabai.group42.BoaState;
import geniusweb.issuevalue.Bid;
import geniusweb.profile.utilityspace.LinearAdditive;
import geniusweb.profile.utilityspace.UtilitySpace;

/**
 * Class for a acceptance condition called ACcombi(W). combines ACnext and a version of ACtime.
 * After time T it checks all previous bids to check if the bid is better then those.
 * Comparable to CombiWAcceptanceState
 * see: https://homepages.cwi.nl/~baarslag/pub/Acceptance_conditions_in_automated_negotiation.pdf
 */
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
        
        // loop through action history
        for (int i = state.getActionHistory().size() - 1; i >= 0; i--) {
            Action action = state.getActionHistory().get(i);
            
            // if the current action is an offer made by the opponent
            if (action.getActor() != state.getSettings().getID() && action instanceof Offer) {
	            Offer offer = (Offer) action;
	            if (highestUtil < utilSpace.getUtility(offer.getBid()).doubleValue()) highestUtil = utilSpace.getUtility(offer.getBid()).doubleValue();
            }
        }

     // if past T and bid is the best bid we received so far.
        if (state.getProgress().get(System.currentTimeMillis()) > T && utilSpace.getUtility(bid).doubleValue() > highestUtil) {
            return true;
        }

        return false;
    }

    public void setNextBid(Bid bid) {
        this.nextBid = bid;
    }
}
