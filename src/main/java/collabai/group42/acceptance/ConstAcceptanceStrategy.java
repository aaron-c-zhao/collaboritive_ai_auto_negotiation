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

//stream
//        // specify the number of elements to skip
//        .skip(fromIndex)
//        // specify the no. of elements the stream should be limited to
//        .limit(toIndex - fromIndex + 1);

// list.stream().skip(fromIndex).limit(toIndex - fromIndex + 1)
// convert back to list
