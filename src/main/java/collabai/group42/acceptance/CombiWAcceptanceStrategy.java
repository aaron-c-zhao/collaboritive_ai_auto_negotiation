package collabai.group42.acceptance;

import geniusweb.actions.Action;
import geniusweb.actions.Offer;
import geniusweb.boa.BoaState;
import geniusweb.boa.acceptancestrategy.AcceptanceStrategy;
import geniusweb.issuevalue.Bid;
import geniusweb.profile.utilityspace.LinearAdditive;
import geniusweb.profile.utilityspace.UtilitySpace;

/**
 * Class for a acceptance condition called ACcombi(W). combines ACnext and a version of ACtime.
 * After time T it checks the time window to see if the next bid is better then all the bids in the time window.
 * see: https://homepages.cwi.nl/~baarslag/pub/Acceptance_conditions_in_automated_negotiation.pdf
 */
public class CombiWAcceptanceStrategy implements AcceptanceStrategy {
    private final double a = 1;
    private final double b = 0;
    private final double T = 0.92;
    private Bid nextBid = null;

    @Override
    public Boolean isAcceptable(Bid bid, BoaState state) {
        UtilitySpace utilSpace = (LinearAdditive) state.getProfile();

        // Check if bid better is better then our next bid, if so return true
        if (nextBid != null && utilSpace.getUtility(bid).doubleValue() * a + b > utilSpace.getUtility(nextBid).doubleValue()) {
            return true;
        }
        
        


        double progress = state.getProgress().get(System.currentTimeMillis());
        int window = (int) (1-progress)* 200; // how many previous bids are within the time window
        
        double highestUtil = 0; // bid with highest util we have found so far in the time window
        int count = 0; // how many bids of the opponent we have taken into account
        
        // loop through action history
        for (int i = state.getActionHistory().size() - 1; i >= 0; i--) {
            Action action = state.getActionHistory().get(i);
            if (action.getActor() != state.getSettings().getID() && action instanceof Offer) {
            	Offer offer = (Offer) action;
                count++; // action is opponent bid

                 // if current bid is not within the time window, break
                if (progress > 0.5 && count > window) break;

                if (highestUtil < utilSpace.getUtility(offer.getBid()).doubleValue()) highestUtil = utilSpace.getUtility(offer.getBid()).doubleValue();
            }
            
        }

        // if past T and bid is the best bid we received so far
        if (progress > T && utilSpace.getUtility(bid).doubleValue() > highestUtil) {
            return true;
        }

        return false;
    }

    public void setNextBid(Bid bid) {
        this.nextBid = bid;
    }
}
