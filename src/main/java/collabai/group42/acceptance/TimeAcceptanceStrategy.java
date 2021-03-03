package collabai.group42.acceptance;

import collabai.group42.BoaState;
import geniusweb.issuevalue.Bid;

/**
 * Class for a acceptance condition called ACtime. Works by accepting the bid if the time is later then T.
 * see: https://homepages.cwi.nl/~baarslag/pub/Acceptance_conditions_in_automated_negotiation.pdf
 *
 */
public class TimeAcceptanceStrategy implements AcceptanceStrategy {
    private final double T = 0.92; // time after which to definitely accept.
	@Override
	public Boolean isAcceptable(Bid bid, BoaState state) {
        double progress = state.getProgress().get(System.currentTimeMillis());
        return (progress > T - 0.000001);
	}
}
