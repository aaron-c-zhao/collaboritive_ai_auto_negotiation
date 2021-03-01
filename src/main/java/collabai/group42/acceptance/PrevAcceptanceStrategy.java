package collabai.group42.acceptance;

import geniusweb.actions.Action;
import geniusweb.actions.Offer;
import geniusweb.boa.BoaState;
import geniusweb.boa.acceptancestrategy.AcceptanceStrategy;
import geniusweb.issuevalue.Bid;
import geniusweb.profile.utilityspace.UtilitySpace;

/**
 * Class for a acceptance condition called ACprev. Works by comparing the incoming bid with the last bid sent.
 * see: https://homepages.cwi.nl/~baarslag/pub/Acceptance_conditions_in_automated_negotiation.pdf
 *
 */
public class PrevAcceptanceStrategy implements AcceptanceStrategy {
    private final double a = 1.02; // multiplier for utility of bid
    private final double b = 0; // minimal gap
    
    @Override
    public Boolean isAcceptable(Bid bid, BoaState state) {
    	UtilitySpace utilSpace = (UtilitySpace) state.getProfile();
        Bid lastOffer = null;

        // Loop through action history
        for (int i = state.getActionHistory().size() - 1; i >= 0; i--) {
            Action action = state.getActionHistory().get(i);
            
            // if the current action is an offer made by the opponent
            if (action.getActor() == state.getSettings().getID() && action instanceof Offer) {
	            Offer offer = (Offer) action;
	            lastOffer = offer.getBid();
	            break; // last bid found
            }
        }

    	// There is no previous offer by the opponent, probably because it is the first move.
        if (lastOffer == null) return false; 

        // return true if bid is better then our last offer
        return utilSpace.getUtility(bid).doubleValue()*a + b > utilSpace.getUtility(lastOffer).doubleValue();
    }

}
