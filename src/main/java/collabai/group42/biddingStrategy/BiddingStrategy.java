package collabai.group42.biddingStrategy;

import collabai.group42.BoaState;
import geniusweb.actions.Action;
import geniusweb.boa.BoaParty;
import geniusweb.inform.Settings;

/**
 * Determines what action to take in which state.
 * <p>
 * Assumed part of a {@link BoaParty}.
 * 
 * <p>
 * <em>MUST</em> have an empty constructor.
 */
public interface BiddingStrategy {

	/**
	 * Recommend a next action.
	 * <p>
	 * <b>Note</b> for implementors: This can extract configuration parameters
	 * from {@link Settings#getParameters} using {@link BoaState#getSettings()}.
	 * Generally we recommend to write implementations such that they can also
	 * be hard-configured, for easier use of the components in hard-coded
	 * settings.
	 * 
	 * @param state the current {@link BoaState}
	 * 
	 * @return the suggested next Action. Should always return a valid action,
	 *         not null.
	 */
	Action getAction(BoaState state);

}
