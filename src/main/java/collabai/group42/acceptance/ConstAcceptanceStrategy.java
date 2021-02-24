package collabai.group42.acceptance;

import geniusweb.boa.BoaState;
import geniusweb.boa.acceptancestrategy.AcceptanceStrategy;
import geniusweb.issuevalue.Bid;
import geniusweb.profile.utilityspace.UtilitySpace;

public class ConstAcceptanceStrategy implements AcceptanceStrategy {
    @Override
    public Boolean isAcceptable(Bid bid, BoaState state) {
        double a = 0.8;

        return ((UtilitySpace) state.getProfile()).getUtility(bid)
                .doubleValue() >= a;
    }
}
