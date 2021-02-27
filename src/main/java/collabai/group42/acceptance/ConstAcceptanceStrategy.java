package collabai.group42.acceptance;

import geniusweb.boa.acceptancestrategy.AcceptanceStrategy;
import geniusweb.issuevalue.Bid;
import geniusweb.profile.utilityspace.UtilitySpace;
import geniusweb.boa.BoaState;

public class ConstAcceptanceStrategy implements AcceptanceStrategy {
    private final double a = 0.9;

    @Override
    public Boolean isAcceptable(Bid bid, BoaSatate state) {


        return ((UtilitySpace) state.getProfile()).getUtility(bid)
                .doubleValue() >= a;
    }
}
