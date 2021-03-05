package collabai.group42.acceptance;

import collabai.group42.BoaState;
import geniusweb.issuevalue.Bid;
import geniusweb.profile.utilityspace.UtilitySpace;


//https://homepages.cwi.nl/~baarslag/pub/Acceptance_conditions_in_automated_negotiation.pdf

public class NextAcceptanceStrategy implements AcceptanceStrategy {
    private final double a = 1.02;
    private final double b = 0;
    private Bid nextBid = null;

    @Override
    public Boolean isAcceptable(Bid bid, BoaState state) {
        if (nextBid == null) return false;

        UtilitySpace utilSpace = (UtilitySpace) state.getProfile();
        
//        System.out.println("next offer: " + utilSpace.getUtility(nextBid) + "  this offer: " + utilSpace.getUtility(bid));
        
        return (utilSpace.getUtility(bid).doubleValue() * a + b > utilSpace.getUtility(nextBid).doubleValue());
    }

    public void setNextBid(Bid bid) {
        this.nextBid = bid;
    }
}
