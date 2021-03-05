package collabai.group42.acceptance;

import geniusweb.issuevalue.Bid;
import geniusweb.profile.utilityspace.LinearAdditive;
import geniusweb.profile.utilityspace.UtilitySpace;
import collabai.group42.BoaState;

/**
 * Class for a Constant acceptance condition with target utility a
 *
 */
public class ConstAcceptanceStrategy implements AcceptanceStrategy {
    private final double a = 0.8; // accept bid when utility is above this value

    @Override
    public Boolean isAcceptable(Bid bid, BoaState state) {
    	UtilitySpace utilSpace = (LinearAdditive) state.getProfile();
    	
//    	System.out.println("a: " + a + "  this offer: " + utilSpace.getUtility(bid));

        return utilSpace.getUtility(bid).doubleValue() >= a;
    }
}

