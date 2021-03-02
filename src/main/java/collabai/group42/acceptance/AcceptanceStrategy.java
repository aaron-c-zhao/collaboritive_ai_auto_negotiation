package collabai.group42.acceptance;

import collabai.group42.BoaState;
import geniusweb.boa.BoaParty;

import geniusweb.inform.Settings;
import geniusweb.issuevalue.Bid;

/**
 * Determines in which states a bid is acceptable. Typically used as one of the
 * components of a {@link BoaParty}
 * <p>
 * <em>MUST</em> have an empty constructor.
 */
public interface AcceptanceStrategy {

	/**
	 * determine acceptability of a bid.
	 * <p>
	 * <b>Note</b> for implementors: The AcceptanceStrategy can extract
	 * configuration parameters from {@link Settings#getParameters} using
	 * {@link BoaState#getSettings()}. Generally we recommend to write
	 * implementations such that they can also be hard-configured, for easier
	 * use of the components in hard-coded settings.
	 * 
	 * @param bid       the bid to be checked
	 * @param negoState the current {@link BoaState}.
	 * 
	 * 
	 * @return true iff current bid is acceptable at this moment. Notice that in
	 *         theory a protocol might allow accepting other bids than the
	 *         latest. But at this moment only SAOP is supported by BOA.
	 */
	Boolean isAcceptable(Bid bid, BoaState negoState);
}
