package collabai.group42.acceptance;

import geniusweb.boa.BoaState;
import geniusweb.boa.acceptancestrategy.AcceptanceStrategy;
import geniusweb.issuevalue.Bid;
import geniusweb.profile.utilityspace.UtilitySpace;


/**
 * Class for a acceptance condition called ACnext. Works by comparing the incoming bid our next bid.
 * see: https://homepages.cwi.nl/~baarslag/pub/Acceptance_conditions_in_automated_negotiation.pdf
 */
public class NextAcceptanceStrategy implements AcceptanceStrategy {
    private final double a = 1.02;
    private final double b = 0;
    private Bid nextBid = null;

    @Override
    public Boolean isAcceptable(Bid bid, BoaState state) {
    	UtilitySpace utilSpace = (UtilitySpace) state.getProfile();

    	// next bid must be set.
        if (nextBid == null) throw new IllegalArgumentException("Next bid is null");
        
        // return true if bid is better then our next bid
        return (utilSpace.getUtility(bid).doubleValue() * a + b > utilSpace.getUtility(nextBid).doubleValue());
    }

    /**
     * setter for our next bid, must be called before isAcceptable
     */
    public void setNextBid(Bid bid) {
        this.nextBid = bid;
    }
}
