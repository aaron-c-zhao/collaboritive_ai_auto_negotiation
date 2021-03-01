package collabai.group42.acceptance;

import geniusweb.boa.acceptancestrategy.AcceptanceStrategy;
import geniusweb.issuevalue.Bid;
import geniusweb.profile.utilityspace.UtilitySpace;
import geniusweb.boa.BoaState;

/**
 * Class for a Constant acceptance condition with target utility a
 *
 */
public class ConstAcceptanceStrategy implements AcceptanceStrategy {
    private final double a = 0.8; // accept bid when utility is above this value

    @Override
    public Boolean isAcceptable(Bid bid, BoaState state) {

        return ((UtilitySpace) state.getProfile()).getUtility(bid)
                .doubleValue() >= a;
    }
}

